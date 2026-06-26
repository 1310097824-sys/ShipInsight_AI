package com.gsmv.ais;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.luben.zstd.ZstdInputStream;
import com.gsmv.ais.dto.AisBatchDeleteRequest;
import com.gsmv.ais.dto.AisBatchOperationResult;
import com.gsmv.ais.dto.AisBatchUpdateRequest;
import com.gsmv.ais.dto.AisConvertedCsvSaveResult;
import com.gsmv.ais.dto.AisDatasetDateStat;
import com.gsmv.ais.dto.AisImportProgress;
import com.gsmv.ais.dto.AisImportResult;
import com.gsmv.ais.dto.AisRankingStat;
import com.gsmv.ais.dto.AisRecordView;
import com.gsmv.ais.dto.AisRiskSummary;
import com.gsmv.ais.dto.AisVesselDraftCandidate;
import com.gsmv.ais.dto.AisVesselSummaryView;
import com.gsmv.common.ErrorCode;
import com.gsmv.common.PageResponse;
import com.gsmv.common.exception.BusinessException;
import com.gsmv.security.CurrentUser;
import com.gsmv.security.SecurityUtils;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AisService {

    private static final byte[] ZSTD_MAGIC = {(byte) 0x28, (byte) 0xB5, (byte) 0x2F, (byte) 0xFD};
    private static final DateTimeFormatter CLICKHOUSE_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter CSV_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_BATCH_IDS = 5000;
    private static final int IMPORT_BATCH_SIZE = 1000;
    private static final Map<String, UpdateColumn> UPDATE_COLUMNS = Map.ofEntries(
            Map.entry("vesselName", new UpdateColumn("vessel_name", UpdateType.STRING, false)),
            Map.entry("imo", new UpdateColumn("imo", UpdateType.STRING, false)),
            Map.entry("callSign", new UpdateColumn("call_sign", UpdateType.STRING, false)),
            Map.entry("vesselType", new UpdateColumn("vessel_type", UpdateType.UINT16, true)),
            Map.entry("status", new UpdateColumn("status", UpdateType.UINT16, true)),
            Map.entry("sog", new UpdateColumn("sog", UpdateType.FLOAT, true)),
            Map.entry("cog", new UpdateColumn("cog", UpdateType.FLOAT, true)),
            Map.entry("heading", new UpdateColumn("heading", UpdateType.UINT16, true)),
            Map.entry("length", new UpdateColumn("length", UpdateType.FLOAT, true)),
            Map.entry("width", new UpdateColumn("width", UpdateType.FLOAT, true)),
            Map.entry("draft", new UpdateColumn("draft", UpdateType.FLOAT, true)),
            Map.entry("cargo", new UpdateColumn("cargo", UpdateType.UINT16, true)),
            Map.entry("transceiver", new UpdateColumn("transceiver", UpdateType.STRING, false)),
            Map.entry("note", new UpdateColumn("note", UpdateType.STRING, false))
    );

    private final AisClickHouseProperties properties;
    private final ObjectMapper objectMapper;
    private final AisVesselLinkService vesselLinkService;
    private final HttpClient httpClient;
    private final ConcurrentMap<String, ImportProgressState> importProgresses = new ConcurrentHashMap<>();
    private volatile boolean schemaReady;

    public AisService(AisClickHouseProperties properties, ObjectMapper objectMapper, AisVesselLinkService vesselLinkService) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.vesselLinkService = vesselLinkService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMillis()))
                .build();
    }

    public PageResponse<AisRecordView> list(
            String keyword,
            LocalDateTime observedFrom,
            LocalDateTime observedTo,
            int page,
            int size
    ) {
        validateDateRange(observedFrom, observedTo);
        ensureSchema();

        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        String where = buildWhere(keyword, observedFrom, observedTo);

        String listSql = """
                SELECT
                  record_id AS id,
                  mmsi,
                  base_date_time AS baseDateTime,
                  longitude,
                  latitude,
                  sog,
                  cog,
                  heading,
                  vessel_name AS vesselName,
                  imo,
                  call_sign AS callSign,
                  vessel_type AS vesselType,
                  status,
                  length,
                  width,
                  draft,
                  cargo,
                  transceiver,
                  note,
                  source_file AS sourceFile,
                  imported_by_user_id AS importedByUserId,
                  imported_by_name AS importedByName,
                  imported_at AS importedAt
                FROM %s FINAL
                %s
                ORDER BY base_date_time DESC, imported_at DESC, mmsi ASC
                LIMIT %d OFFSET %d
                FORMAT JSONEachRow
                """.formatted(tableName(), where, safeSize, offset);
        String countSql = """
                SELECT count() AS total
                FROM %s FINAL
                %s
                FORMAT JSONEachRow
                """.formatted(tableName(), where);

        List<AisRecordView> items = vesselLinkService.enrich(parseRecords(postQuery(listSql)));
        long total = parseTotal(postQuery(countSql));
        return new PageResponse<>(items, total, safePage, safeSize);
    }

    public PageResponse<AisRecordView> mapLatest(
            String keyword,
            LocalDateTime observedFrom,
            LocalDateTime observedTo,
            LocalDate datasetDate,
            int limit
    ) {
        validateDateRange(observedFrom, observedTo);
        ensureSchema();

        int safeLimit = Math.min(Math.max(limit, 1), 100000);
        String latestDay = datasetDate == null ? null : datasetDate.toString();
        if (!StringUtils.hasText(latestDay)) {
            String latestDaySql = """
                    SELECT toDate(max(base_date_time)) AS latestDay
                    FROM %s FINAL
                    %s
                    FORMAT JSONEachRow
                    """.formatted(tableName(), buildWhere(keyword, observedFrom, observedTo));
            latestDay = parseTextField(postQuery(latestDaySql), "latestDay");
        }
        if (!StringUtils.hasText(latestDay)) {
            return new PageResponse<>(List.of(), 0, 1, safeLimit);
        }
        String where = buildWhere(keyword, observedFrom, observedTo, List.of("toDate(base_date_time) = toDate('" + escapeSqlString(latestDay) + "')"));
        String listSql = """
                SELECT
                  argMax(record_id, base_date_time) AS id,
                  mmsi,
                  max(base_date_time) AS baseDateTime,
                  argMax(longitude, base_date_time) AS longitude,
                  argMax(latitude, base_date_time) AS latitude,
                  argMax(sog, base_date_time) AS sog,
                  argMax(cog, base_date_time) AS cog,
                  argMax(heading, base_date_time) AS heading,
                  argMax(vessel_name, base_date_time) AS vesselName,
                  argMax(imo, base_date_time) AS imo,
                  argMax(call_sign, base_date_time) AS callSign,
                  argMax(vessel_type, base_date_time) AS vesselType,
                  argMax(status, base_date_time) AS status,
                  argMax(length, base_date_time) AS length,
                  argMax(width, base_date_time) AS width,
                  argMax(draft, base_date_time) AS draft,
                  argMax(cargo, base_date_time) AS cargo,
                  argMax(transceiver, base_date_time) AS transceiver,
                  argMax(note, base_date_time) AS note,
                  argMax(source_file, base_date_time) AS sourceFile,
                  argMax(imported_by_user_id, base_date_time) AS importedByUserId,
                  argMax(imported_by_name, base_date_time) AS importedByName,
                  argMax(imported_at, base_date_time) AS importedAt
                FROM (
                  SELECT
                    record_id,
                    mmsi,
                    base_date_time,
                    longitude,
                    latitude,
                    sog,
                    cog,
                    heading,
                    vessel_name,
                    imo,
                    call_sign,
                    vessel_type,
                    status,
                    length,
                    width,
                    draft,
                    cargo,
                    transceiver,
                    note,
                    source_file,
                    imported_by_user_id,
                    imported_by_name,
                    imported_at
                  FROM %s FINAL
                  %s
                )
                GROUP BY mmsi
                ORDER BY baseDateTime DESC
                LIMIT %d
                FORMAT JSONEachRow
                """.formatted(tableName(), where, safeLimit);
        String countSql = """
                SELECT uniqExact(mmsi) AS total
                FROM (
                  SELECT mmsi
                  FROM %s FINAL
                  %s
                )
                FORMAT JSONEachRow
                """.formatted(tableName(), where);

        List<AisRecordView> items = parseRecords(postQuery(listSql));
        long total = parseTotal(postQuery(countSql));
        return new PageResponse<>(items, total, 1, safeLimit);
    }

    public List<String> datasetDates() {
        ensureSchema();
        String sql = """
                SELECT toString(toDate(base_date_time)) AS datasetDate
                FROM %s FINAL
                GROUP BY datasetDate
                ORDER BY datasetDate DESC
                FORMAT JSONEachRow
                """.formatted(tableName());
        return parseTextFields(postQuery(sql), "datasetDate");
    }

    public List<AisDatasetDateStat> datasetDateStats(
            String keyword,
            LocalDateTime observedFrom,
            LocalDateTime observedTo
    ) {
        validateDateRange(observedFrom, observedTo);
        ensureSchema();
        String sql = """
                SELECT
                  toString(toDate(base_date_time)) AS datasetDate,
                  count() AS recordCount
                FROM %s FINAL
                %s
                GROUP BY datasetDate
                ORDER BY datasetDate DESC
                FORMAT JSONEachRow
                """.formatted(tableName(), buildWhere(keyword, observedFrom, observedTo));
        return parseDatasetDateStats(postQuery(sql));
    }

    public List<AisRankingStat> importerStats(
            String keyword,
            LocalDateTime observedFrom,
            LocalDateTime observedTo,
            int limit
    ) {
        validateDateRange(observedFrom, observedTo);
        ensureSchema();
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        String sql = """
                SELECT
                  if(trim(imported_by_name) = '', '未记录录入人', imported_by_name) AS label,
                  count() AS recordCount
                FROM %s FINAL
                %s
                GROUP BY label
                ORDER BY recordCount DESC
                LIMIT %d
                FORMAT JSONEachRow
                """.formatted(tableName(), buildWhere(keyword, observedFrom, observedTo), safeLimit);
        return parseRankingStats(postQuery(sql));
    }


    public AisRiskSummary riskSummary(
            String keyword,
            LocalDateTime observedFrom,
            LocalDateTime observedTo
    ) {
        validateDateRange(observedFrom, observedTo);
        ensureSchema();
        String sql = """
                SELECT
                  count() AS total,
                  countIf(sog IS NOT NULL AND sog < 1) AS lowSpeedCount,
                  countIf(status IN (1, 5) OR (sog IS NOT NULL AND sog < 0.5)) AS stoppedCount,
                  countIf(note != '' AND (
                    positionCaseInsensitiveUTF8(note, '\u5f02\u5e38') > 0
                    OR positionCaseInsensitiveUTF8(note, '\u98ce\u9669') > 0
                    OR positionCaseInsensitiveUTF8(note, '\u544a\u8b66') > 0
                    OR positionCaseInsensitiveUTF8(note, '\u53ef\u7591') > 0
                    OR positionCaseInsensitiveUTF8(note, 'abnormal') > 0
                    OR positionCaseInsensitiveUTF8(note, 'risk') > 0
                    OR positionCaseInsensitiveUTF8(note, 'warning') > 0
                  )) AS abnormalNoteCount,
                  uniqExact(mmsi) AS uniqueVesselCount
                FROM %s FINAL
                %s
                FORMAT JSONEachRow
                """.formatted(tableName(), buildWhere(keyword, observedFrom, observedTo));
        return parseRiskSummary(postQuery(sql));
    }

    public PageResponse<AisRecordView> vesselTrack(String mmsi, int limit) {
        ensureSchema();
        if (!StringUtils.hasText(mmsi)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择船只编号", HttpStatus.BAD_REQUEST);
        }
        int safeLimit = Math.min(Math.max(limit, 1), 20000);
        String escapedMmsi = escapeSqlString(mmsi.trim());
        String listSql = """
                SELECT
                  record_id AS id,
                  mmsi,
                  base_date_time AS baseDateTime,
                  longitude,
                  latitude,
                  sog,
                  cog,
                  heading,
                  vessel_name AS vesselName,
                  imo,
                  call_sign AS callSign,
                  vessel_type AS vesselType,
                  status,
                  length,
                  width,
                  draft,
                  cargo,
                  transceiver,
                  note,
                  source_file AS sourceFile,
                  imported_by_user_id AS importedByUserId,
                  imported_by_name AS importedByName,
                  imported_at AS importedAt
                FROM %s FINAL
                WHERE mmsi = '%s'
                ORDER BY base_date_time ASC
                LIMIT %d
                FORMAT JSONEachRow
                """.formatted(tableName(), escapedMmsi, safeLimit);
        String countSql = """
                SELECT count() AS total
                FROM %s FINAL
                WHERE mmsi = '%s'
                FORMAT JSONEachRow
                """.formatted(tableName(), escapedMmsi);

        List<AisRecordView> items = vesselLinkService.enrich(parseRecords(postQuery(listSql)));
        long total = parseTotal(postQuery(countSql));
        return new PageResponse<>(items, total, 1, safeLimit);
    }

    public PageResponse<AisRecordView> vesselTrackByKeyword(String keyword, int limit) {
        ensureSchema();
        if (!StringUtils.hasText(keyword)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择船舶编号、IMO 或船名", HttpStatus.BAD_REQUEST);
        }
        int safeLimit = Math.min(Math.max(limit, 1), 20000);
        String identityCondition = buildTrackIdentityCondition(keyword.trim());
        String listSql = """
                SELECT
                  record_id AS id,
                  mmsi,
                  base_date_time AS baseDateTime,
                  longitude,
                  latitude,
                  sog,
                  cog,
                  heading,
                  vessel_name AS vesselName,
                  imo,
                  call_sign AS callSign,
                  vessel_type AS vesselType,
                  status,
                  length,
                  width,
                  draft,
                  cargo,
                  transceiver,
                  note,
                  source_file AS sourceFile,
                  imported_by_user_id AS importedByUserId,
                  imported_by_name AS importedByName,
                  imported_at AS importedAt
                FROM %s FINAL
                WHERE %s
                ORDER BY base_date_time ASC
                LIMIT %d
                FORMAT JSONEachRow
                """.formatted(tableName(), identityCondition, safeLimit);
        String countSql = """
                SELECT count() AS total
                FROM %s FINAL
                WHERE %s
                FORMAT JSONEachRow
                """.formatted(tableName(), identityCondition);

        List<AisRecordView> items = vesselLinkService.enrich(parseRecords(postQuery(listSql)));
        long total = parseTotal(postQuery(countSql));
        return new PageResponse<>(items, total, 1, safeLimit);
    }
    public AisVesselSummaryView vesselSummary(String mmsi, String imo) {
        ensureSchema();
        String identityCondition = buildVesselIdentityCondition(mmsi, imo);
        if (!StringUtils.hasText(identityCondition)) {
            return AisVesselSummaryView.empty();
        }

        String aggregateSql = """
                SELECT
                  count() AS total,
                  min(base_date_time) AS firstBaseDateTime,
                  max(base_date_time) AS latestBaseDateTime
                FROM %s FINAL
                WHERE %s
                FORMAT JSONEachRow
                """.formatted(tableName(), identityCondition);
        AisSummaryAggregate aggregate = parseSummaryAggregate(postQuery(aggregateSql));
        if (aggregate.total() == 0) {
            return AisVesselSummaryView.empty();
        }

        String latestSql = """
                SELECT
                  record_id AS id,
                  mmsi,
                  base_date_time AS baseDateTime,
                  longitude,
                  latitude,
                  sog,
                  cog,
                  heading,
                  vessel_name AS vesselName,
                  imo,
                  call_sign AS callSign,
                  vessel_type AS vesselType,
                  status,
                  length,
                  width,
                  draft,
                  cargo,
                  transceiver,
                  note,
                  source_file AS sourceFile,
                  imported_by_user_id AS importedByUserId,
                  imported_by_name AS importedByName,
                  imported_at AS importedAt
                FROM %s FINAL
                WHERE %s
                ORDER BY base_date_time DESC, imported_at DESC
                LIMIT 1
                FORMAT JSONEachRow
                """.formatted(tableName(), identityCondition);
        AisRecordView latestRecord = vesselLinkService.enrich(parseRecords(postQuery(latestSql))).stream()
                .findFirst()
                .orElse(null);
        return new AisVesselSummaryView(
                aggregate.total(),
                aggregate.firstBaseDateTime(),
                aggregate.latestBaseDateTime(),
                latestRecord
        );
    }

    public PageResponse<AisRecordView> listForVessel(String mmsi, String imo, int page, int size) {
        ensureSchema();
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String identityCondition = buildVesselIdentityCondition(mmsi, imo);
        if (!StringUtils.hasText(identityCondition)) {
            return new PageResponse<>(List.of(), 0, safePage, safeSize);
        }

        int offset = (safePage - 1) * safeSize;
        String listSql = """
                SELECT
                  record_id AS id,
                  mmsi,
                  base_date_time AS baseDateTime,
                  longitude,
                  latitude,
                  sog,
                  cog,
                  heading,
                  vessel_name AS vesselName,
                  imo,
                  call_sign AS callSign,
                  vessel_type AS vesselType,
                  status,
                  length,
                  width,
                  draft,
                  cargo,
                  transceiver,
                  note,
                  source_file AS sourceFile,
                  imported_by_user_id AS importedByUserId,
                  imported_by_name AS importedByName,
                  imported_at AS importedAt
                FROM %s FINAL
                WHERE %s
                ORDER BY base_date_time DESC, imported_at DESC
                LIMIT %d OFFSET %d
                FORMAT JSONEachRow
                """.formatted(tableName(), identityCondition, safeSize, offset);
        String countSql = """
                SELECT count() AS total
                FROM %s FINAL
                WHERE %s
                FORMAT JSONEachRow
                """.formatted(tableName(), identityCondition);

        List<AisRecordView> items = vesselLinkService.enrich(parseRecords(postQuery(listSql)));
        long total = parseTotal(postQuery(countSql));
        return new PageResponse<>(items, total, safePage, safeSize);
    }

    public List<AisVesselDraftCandidate> vesselDraftCandidates(
            String keyword,
            LocalDateTime observedFrom,
            LocalDateTime observedTo,
            int limit
    ) {
        return vesselDraftCandidates(keyword, observedFrom, observedTo, limit, 0);
    }

    public List<AisVesselDraftCandidate> vesselDraftCandidates(
            String keyword,
            LocalDateTime observedFrom,
            LocalDateTime observedTo,
            int limit,
            int offset
    ) {
        validateDateRange(observedFrom, observedTo);
        ensureSchema();
        int safeLimit = Math.min(Math.max(limit, 1), 1000);
        int safeOffset = Math.max(offset, 0);
        String where = buildWhere(keyword, observedFrom, observedTo, List.of("(mmsi != '' OR imo != '')"));
        String sql = """
                SELECT
                  argMax(record_id, base_date_time) AS recordId,
                  argMax(rawMmsi, base_date_time) AS mmsi,
                  argMax(rawImo, base_date_time) AS imo,
                  argMax(vessel_name, base_date_time) AS vesselName,
                  argMax(call_sign, base_date_time) AS callSign,
                  argMax(length, base_date_time) AS length,
                  argMax(width, base_date_time) AS width,
                  argMax(draft, base_date_time) AS draft,
                  argMax(source_file, base_date_time) AS sourceFile,
                  max(base_date_time) AS baseDateTime
                FROM (
                  SELECT
                    record_id,
                    mmsi AS rawMmsi,
                    imo AS rawImo,
                    vessel_name,
                    call_sign,
                    length,
                    width,
                    draft,
                    source_file,
                    base_date_time,
                    if(mmsi != '', concat('MMSI:', mmsi), concat('IMO:', imo)) AS identityKey
                  FROM %s FINAL
                  %s
                ) filtered
                GROUP BY identityKey
                ORDER BY baseDateTime DESC
                LIMIT %d OFFSET %d
                FORMAT JSONEachRow
                """.formatted(tableName(), where, safeLimit, safeOffset);
        return parseVesselDraftCandidates(postQuery(sql));
    }

    public AisImportProgress importProgress(String taskId) {
        if (!StringUtils.hasText(taskId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "missing import task id", HttpStatus.BAD_REQUEST);
        }
        ImportProgressState state = importProgresses.get(taskId.trim());
        if (state == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "import task not found", HttpStatus.NOT_FOUND);
        }
        return state.snapshot();
    }

    public AisImportResult importFile(MultipartFile file, Integer limit, String taskId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择要导入的 AIS 文件", HttpStatus.BAD_REQUEST);
        }

        ensureSchema();
        CurrentUser currentUser = SecurityUtils.requireCurrentUser();
        Integer safeLimit = limit == null || limit <= 0 ? null : Math.min(limit, 1000);
        String uploadName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "local-upload";
        ImportProgressState progress = startImportProgress(taskId, uploadName, file.getSize(), safeLimit);
        int skipped = 0;
        int imported = 0;
        List<Map<String, Object>> insertRows = new ArrayList<>();

        try (
                CountingInputStream rawInput = new CountingInputStream(file.getInputStream(), progress);
                InputStream input = openPossiblyCompressed(rawInput);
                BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))
        ) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "AIS 文件没有表头", HttpStatus.BAD_REQUEST);
            }
            List<String> headers = parseCsvLine(stripBom(headerLine));
            Map<String, Integer> columns = buildColumnIndex(headers);

            String line;
            while ((line = reader.readLine()) != null && (safeLimit == null || imported + insertRows.size() < safeLimit)) {
                if (line.isBlank()) {
                    continue;
                }
                List<String> values = parseCsvLine(line);
                ParsedAisRow parsed = parseAisRow(values, columns, uploadName);
                if (parsed == null) {
                    skipped++;
                    progress.update(imported, skipped, "导入中");
                    continue;
                }
                insertRows.add(toClickHouseRow(parsed, currentUser));
                if (insertRows.size() >= IMPORT_BATCH_SIZE) {
                    insertRows(insertRows);
                    imported += insertRows.size();
                    insertRows.clear();
                    progress.update(imported, skipped, "导入中");
                }
            }
        } catch (BusinessException ex) {
            progress.fail(ex.getMessage());
            throw ex;
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "AIS 文件读取失败：" + ex.getMessage(), HttpStatus.BAD_REQUEST);
        }

        if (!insertRows.isEmpty()) {
            insertRows(insertRows);
            imported += insertRows.size();
            progress.update(imported, skipped, "导入中");
        }
        progress.complete(imported, skipped);
        return new AisImportResult(uploadName, imported, skipped, safeLimit == null ? 0 : safeLimit);
    }

    public AisConvertedCsvSaveResult saveConvertedCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择要保存的 CSV 文件", HttpStatus.BAD_REQUEST);
        }
        String originalName = StringUtils.hasText(file.getOriginalFilename()) ? Path.of(file.getOriginalFilename()).getFileName().toString() : "converted_ais.csv";
        String safeName = sanitizeCsvFileName(originalName);
        Path rootDir = resolveConvertedCsvOutputDir();
        Path outputFile = rootDir.resolve(safeName).normalize();
        if (!outputFile.startsWith(rootDir)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "保存路径不合法", HttpStatus.BAD_REQUEST);
        }
        try {
            Files.createDirectories(rootDir);
            byte[] content = file.getBytes();
            validateCsvContent(content, safeName);
            Files.write(outputFile, content);
            return new AisConvertedCsvSaveResult(safeName, outputFile.toString(), Files.size(outputFile));
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "CSV 文件保存失败：" + ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    public AisBatchOperationResult deleteBatch(AisBatchDeleteRequest request) {
        ensureSchema();
        String condition = buildTargetCondition(
                request == null ? null : request.ids(),
                request != null && Boolean.TRUE.equals(request.allMatched()),
                request == null ? null : request.keyword(),
                request == null ? null : request.observedFrom(),
                request == null ? null : request.observedTo()
        );
        long matched = countByCondition(condition);
        if (matched == 0) {
            return new AisBatchOperationResult(0, "delete");
        }
        postMutation("ALTER TABLE " + tableName() + " DELETE WHERE " + condition + " SETTINGS mutations_sync = 1");
        return new AisBatchOperationResult(matched, "delete");
    }

    public AisBatchOperationResult updateBatch(AisBatchUpdateRequest request) {
        ensureSchema();
        String condition = buildTargetCondition(
                request == null ? null : request.ids(),
                request != null && Boolean.TRUE.equals(request.allMatched()),
                request == null ? null : request.keyword(),
                request == null ? null : request.observedFrom(),
                request == null ? null : request.observedTo()
        );
        List<String> assignments = buildAssignments(request == null ? null : request.fields());
        long matched = countByCondition(condition);
        if (matched == 0) {
            return new AisBatchOperationResult(0, "update");
        }
        postMutation("ALTER TABLE " + tableName() + " UPDATE "
                + String.join(", ", assignments)
                + " WHERE " + condition
                + " SETTINGS mutations_sync = 1");
        return new AisBatchOperationResult(matched, "update");
    }

    private void ensureSchema() {
        if (schemaReady) {
            return;
        }
        synchronized (this) {
            if (schemaReady) {
                return;
            }
            String database = safeIdentifier(properties.getDatabase());
            postQuery("CREATE DATABASE IF NOT EXISTS " + database);
            postQuery("""
                    CREATE TABLE IF NOT EXISTS %s (
                      record_id String,
                      mmsi String,
                      base_date_time DateTime,
                      longitude Float64,
                      latitude Float64,
                      sog Nullable(Float64),
                      cog Nullable(Float64),
                      heading Nullable(UInt16),
                      vessel_name String,
                      imo String,
                      call_sign String,
                      vessel_type Nullable(UInt16),
                      status Nullable(UInt16),
                      length Nullable(Float64),
                      width Nullable(Float64),
                      draft Nullable(Float64),
                      cargo Nullable(UInt16),
                      transceiver String,
                      note String DEFAULT '',
                      source_file String,
                      imported_by_user_id Nullable(UInt64),
                      imported_by_name String,
                      imported_at DateTime DEFAULT now()
                    )
                    ENGINE = ReplacingMergeTree(imported_at)
                    ORDER BY (base_date_time, mmsi, record_id)
                    """.formatted(tableName()));
            postQuery("ALTER TABLE " + tableName() + " ADD COLUMN IF NOT EXISTS note String DEFAULT '' AFTER transceiver");
            schemaReady = true;
        }
    }

    private Path resolveConvertedCsvOutputDir() {
        Path root = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        if (root.getFileName() != null && "system".equalsIgnoreCase(root.getFileName().toString())) {
            root = root.getParent();
        }
        return root.resolve("handle_DATA_clean").normalize();
    }

    private String sanitizeCsvFileName(String fileName) {
        String trimmed = fileName.trim();
        String normalized = trimmed.replace('\\', '_').replace('/', '_').replace(':', '_');
        if (!normalized.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            normalized = normalized + ".csv";
        }
        return normalized.isBlank() ? "converted_ais.csv" : normalized;
    }

    private void validateCsvContent(byte[] content, String fileName) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "CSV 文件为空：" + fileName, HttpStatus.BAD_REQUEST);
            }
            List<String> headers = parseCsvLine(stripBom(headerLine));
            Map<String, Integer> columns = buildColumnIndex(headers);
            if (columns.isEmpty()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "CSV 文件缺少表头：" + fileName, HttpStatus.BAD_REQUEST);
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                ParsedAisRow parsed = parseAisRow(parseCsvLine(line), columns, fileName);
                if (parsed == null) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "CSV 文件包含无效 AIS 记录：" + fileName, HttpStatus.BAD_REQUEST);
                }
            }
        }
    }

    private void insertRows(List<Map<String, Object>> rows) {
        StringBuilder body = new StringBuilder("INSERT INTO ")
                .append(tableName())
                .append(" FORMAT JSONEachRow\n");
        for (Map<String, Object> row : rows) {
            try {
                body.append(objectMapper.writeValueAsString(row)).append('\n');
            } catch (JsonProcessingException ex) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "AIS 记录序列化失败", HttpStatus.BAD_REQUEST);
            }
        }
        postQuery(body.toString());
    }

    private Map<String, Object> toClickHouseRow(ParsedAisRow row, CurrentUser currentUser) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("record_id", row.recordId());
        data.put("mmsi", row.mmsi());
        data.put("base_date_time", row.baseDateTime().format(CLICKHOUSE_DATE_TIME));
        data.put("longitude", row.longitude());
        data.put("latitude", row.latitude());
        data.put("sog", row.sog());
        data.put("cog", row.cog());
        data.put("heading", row.heading());
        data.put("vessel_name", blankToEmpty(row.vesselName()));
        data.put("imo", blankToEmpty(row.imo()));
        data.put("call_sign", blankToEmpty(row.callSign()));
        data.put("vessel_type", row.vesselType());
        data.put("status", row.status());
        data.put("length", row.length());
        data.put("width", row.width());
        data.put("draft", row.draft());
        data.put("cargo", row.cargo());
        data.put("transceiver", blankToEmpty(row.transceiver()));
        data.put("note", "");
        data.put("source_file", blankToEmpty(row.sourceFile()));
        data.put("imported_by_user_id", currentUser.userId());
        data.put("imported_by_name", repairMojibake(firstNonBlank(currentUser.displayName(), currentUser.username(), "系统导入")));
        data.put("imported_at", LocalDateTime.now().format(CLICKHOUSE_DATE_TIME));
        return data;
    }

    private ParsedAisRow parseAisRow(List<String> values, Map<String, Integer> columns, String uploadName) {
        String mmsi = value(values, columns, "mmsi");
        LocalDateTime baseDateTime = parseDateTime(value(values, columns, "basedatetime", "base_date_time", "timestamp"));
        BigDecimal longitude = decimal(value(values, columns, "longitude", "lon", "x"));
        BigDecimal latitude = decimal(value(values, columns, "latitude", "lat", "y"));
        if (!StringUtils.hasText(mmsi) || baseDateTime == null || longitude == null || latitude == null) {
            return null;
        }

        String sourceFile = firstNonBlank(value(values, columns, "sourcefile", "source_file"), uploadName);
        String recordId = sha256Hex(String.join("|",
                mmsi.trim(),
                baseDateTime.format(CLICKHOUSE_DATE_TIME),
                longitude.stripTrailingZeros().toPlainString(),
                latitude.stripTrailingZeros().toPlainString(),
                sourceFile
        ));

        return new ParsedAisRow(
                recordId,
                mmsi.trim(),
                baseDateTime,
                longitude,
                latitude,
                decimal(value(values, columns, "sog")),
                decimal(value(values, columns, "cog")),
                integer(value(values, columns, "heading")),
                normalizeText(value(values, columns, "vesselname", "vessel_name")),
                normalizeText(value(values, columns, "imo")),
                normalizeText(value(values, columns, "callsign", "call_sign")),
                integer(value(values, columns, "vesseltype", "vessel_type")),
                integer(value(values, columns, "status")),
                decimal(value(values, columns, "length")),
                decimal(value(values, columns, "width")),
                decimal(value(values, columns, "draft")),
                integer(value(values, columns, "cargo")),
                normalizeText(value(values, columns, "transceiver")),
                sourceFile
        );
    }

    private InputStream openPossiblyCompressed(InputStream source) throws IOException {
        PushbackInputStream input = new PushbackInputStream(source, 8);
        byte[] header = input.readNBytes(4);
        if (header.length > 0) {
            input.unread(header);
        }
        if (header.length >= 4 && matches(header, ZSTD_MAGIC)) {
            return new ZstdInputStream(input);
        }
        if (header.length >= 2 && (header[0] & 0xff) == 0x1f && (header[1] & 0xff) == 0x8b) {
            return new GZIPInputStream(input);
        }
        return input;
    }

    private boolean matches(byte[] header, byte[] expected) {
        for (int index = 0; index < expected.length; index++) {
            if (header[index] != expected[index]) {
                return false;
            }
        }
        return true;
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char ch = line.charAt(index);
            if (ch == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString());
        return values;
    }

    private Map<String, Integer> buildColumnIndex(List<String> headers) {
        Map<String, Integer> columns = new LinkedHashMap<>();
        for (int index = 0; index < headers.size(); index++) {
            columns.put(normalizeHeader(headers.get(index)), index);
        }
        return columns;
    }

    private String value(List<String> values, Map<String, Integer> columns, String... names) {
        for (String name : names) {
            Integer index = columns.get(normalizeHeader(name));
            if (index != null && index >= 0 && index < values.size()) {
                return values.get(index);
            }
        }
        return null;
    }

    private String normalizeHeader(String value) {
        return value == null ? "" : value.trim()
                .toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "");
    }

    private String stripBom(String value) {
        return value != null && value.startsWith("\uFEFF") ? value.substring(1) : value;
    }

    private BigDecimal decimal(String value) {
        String normalized = normalizeText(value);
        if (!StringUtils.hasText(normalized) || "nan".equalsIgnoreCase(normalized)) {
            return null;
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer integer(String value) {
        BigDecimal decimal = decimal(value);
        return decimal == null ? null : decimal.intValue();
    }

    private LocalDateTime parseDateTime(String value) {
        String normalized = normalizeText(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        try {
            return LocalDateTime.parse(normalized);
        } catch (DateTimeParseException ignored) {
            // Try common AIS CSV formats below.
        }
        try {
            return OffsetDateTime.parse(normalized).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            // Try space-separated timestamps below.
        }
        try {
            String compact = normalized.replace('T', ' ');
            if (compact.endsWith("Z")) {
                Instant instant = Instant.parse(normalized);
                return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
            }
            return LocalDateTime.parse(compact.substring(0, Math.min(19, compact.length())), CSV_DATE_TIME);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String buildTrackIdentityCondition(String keyword) {
        String normalizedKeyword = normalizeNullable(keyword);
        if (normalizedKeyword == null) {
            return null;
        }
        String escapedKeyword = escapeSqlString(normalizedKeyword);
        return """
                (
                  mmsi = '%1$s'
                  OR imo = '%1$s'
                  OR positionCaseInsensitiveUTF8(vessel_name, '%1$s') > 0
                )
                """.formatted(escapedKeyword).trim();
    }
    private String buildWhere(String keyword, LocalDateTime observedFrom, LocalDateTime observedTo) {
        return buildWhere(keyword, observedFrom, observedTo, List.of());
    }

    private String buildVesselIdentityCondition(String mmsi, String imo) {
        String normalizedMmsi = normalizeNullable(mmsi);
        if (normalizedMmsi != null) {
            return "mmsi = '" + escapeSqlString(normalizedMmsi) + "'";
        }
        String normalizedImo = normalizeNullable(imo);
        if (normalizedImo != null) {
            return "imo = '" + escapeSqlString(normalizedImo) + "'";
        }
        return null;
    }

    private String buildWhere(
            String keyword,
            LocalDateTime observedFrom,
            LocalDateTime observedTo,
            List<String> extraClauses
    ) {
        List<String> clauses = new ArrayList<>(buildFilterClauses(keyword, observedFrom, observedTo));
        if (extraClauses != null) {
            extraClauses.stream().filter(StringUtils::hasText).forEach(clauses::add);
        }
        if (clauses.isEmpty()) {
            return "";
        }
        StringJoiner joiner = new StringJoiner(" AND ", "WHERE ", "");
        clauses.forEach(joiner::add);
        return joiner.toString();
    }

    private List<String> buildFilterClauses(String keyword, LocalDateTime observedFrom, LocalDateTime observedTo) {
        List<String> clauses = new ArrayList<>();
        if (StringUtils.hasText(keyword)) {
            String escaped = escapeSqlString(keyword.trim());
            clauses.add("""
                    (
                      positionCaseInsensitiveUTF8(mmsi, '%1$s') > 0
                      OR positionCaseInsensitiveUTF8(vessel_name, '%1$s') > 0
                      OR positionCaseInsensitiveUTF8(imo, '%1$s') > 0
                      OR positionCaseInsensitiveUTF8(call_sign, '%1$s') > 0
                      OR positionCaseInsensitiveUTF8(source_file, '%1$s') > 0
                      OR positionCaseInsensitiveUTF8(imported_by_name, '%1$s') > 0
                      OR positionCaseInsensitiveUTF8(note, '%1$s') > 0
                      OR positionCaseInsensitiveUTF8(toString(longitude), '%1$s') > 0
                      OR positionCaseInsensitiveUTF8(toString(latitude), '%1$s') > 0
                    )
                    """.formatted(escaped));
        }
        if (observedFrom != null) {
            clauses.add("base_date_time >= toDateTime('" + observedFrom.format(CLICKHOUSE_DATE_TIME) + "')");
        }
        if (observedTo != null) {
            clauses.add("base_date_time <= toDateTime('" + observedTo.format(CLICKHOUSE_DATE_TIME) + "')");
        }
        return clauses;
    }

    private String buildFilterCondition(String keyword, LocalDateTime observedFrom, LocalDateTime observedTo) {
        List<String> clauses = buildFilterClauses(keyword, observedFrom, observedTo);
        if (clauses.isEmpty()) {
            return "1 = 1";
        }
        return String.join(" AND ", clauses);
    }

    private String buildTargetCondition(
            List<String> ids,
            boolean allMatched,
            String keyword,
            LocalDateTime observedFrom,
            LocalDateTime observedTo
    ) {
        validateDateRange(observedFrom, observedTo);
        if (allMatched) {
            return buildFilterCondition(keyword, observedFrom, observedTo);
        }
        List<String> safeIds = normalizeRecordIds(ids);
        if (safeIds.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择要操作的 AIS 记录", HttpStatus.BAD_REQUEST);
        }
        String joinedIds = safeIds.stream()
                .map(this::escapeSqlString)
                .map(value -> "'" + value + "'")
                .collect(Collectors.joining(", "));
        return "record_id IN (" + joinedIds + ")";
    }

    private List<String> normalizeRecordIds(List<String> ids) {
        if (ids == null) {
            return List.of();
        }
        Set<String> uniqueIds = ids.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (uniqueIds.size() > MAX_BATCH_IDS) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "单次按勾选记录操作最多支持 " + MAX_BATCH_IDS + " 条", HttpStatus.BAD_REQUEST);
        }
        return new ArrayList<>(uniqueIds);
    }

    private List<String> buildAssignments(Map<String, Object> fields) {
        if (fields == null || fields.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择要修改的字段", HttpStatus.BAD_REQUEST);
        }
        List<String> assignments = new ArrayList<>();
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            UpdateColumn column = UPDATE_COLUMNS.get(entry.getKey());
            if (column == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持修改字段：" + entry.getKey(), HttpStatus.BAD_REQUEST);
            }
            assignments.add(column.column() + " = " + sqlLiteral(entry.getValue(), column));
        }
        if (assignments.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择要修改的字段", HttpStatus.BAD_REQUEST);
        }
        return assignments;
    }

    private String sqlLiteral(Object value, UpdateColumn column) {
        if (value == null) {
            return column.nullable() ? "NULL" : "''";
        }
        return switch (column.type()) {
            case STRING -> "'" + escapeSqlString(limitText(Objects.toString(value, ""), column.column())) + "'";
            case FLOAT -> floatLiteral(value, column.nullable());
            case UINT16 -> uint16Literal(value, column.nullable());
        };
    }

    private String floatLiteral(Object value, boolean nullable) {
        String text = Objects.toString(value, "").trim();
        if (!StringUtils.hasText(text)) {
            return nullable ? "NULL" : "0";
        }
        try {
            return new BigDecimal(text).stripTrailingZeros().toPlainString();
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "数值字段格式不正确：" + text, HttpStatus.BAD_REQUEST);
        }
    }

    private String uint16Literal(Object value, boolean nullable) {
        String text = Objects.toString(value, "").trim();
        if (!StringUtils.hasText(text)) {
            return nullable ? "NULL" : "0";
        }
        try {
            int parsed = new BigDecimal(text).intValueExact();
            if (parsed < 0 || parsed > 65535) {
                throw new ArithmeticException("out of UInt16 range");
            }
            return String.valueOf(parsed);
        } catch (ArithmeticException | NumberFormatException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "整数字段需在 0-65535 之间：" + text, HttpStatus.BAD_REQUEST);
        }
    }

    private String limitText(String value, String column) {
        String trimmed = value == null ? "" : value.trim();
        int maxLength = "note".equals(column) ? 1000 : 255;
        if (trimmed.length() > maxLength) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "字段内容过长：" + column, HttpStatus.BAD_REQUEST);
        }
        return trimmed;
    }

    private long countByCondition(String condition) {
        String countSql = """
                SELECT count() AS total
                FROM %s FINAL
                WHERE %s
                FORMAT JSONEachRow
                """.formatted(tableName(), condition);
        return parseTotal(postQuery(countSql));
    }

    private void postMutation(String query) {
        postQuery(query);
    }

    private String postQuery(String query) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(clickHouseUri())
                    .timeout(Duration.ofMillis(properties.getRequestTimeoutMillis()))
                    .header("Content-Type", "text/plain; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(query, StandardCharsets.UTF_8));
            applyAuthentication(requestBuilder);
            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                throw clickHouseException(response.body());
            }
            return response.body();
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "ClickHouse 暂不可用，请先启动本地 ClickHouse 服务：" + ex.getMessage(),
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        }
    }

    private URI clickHouseUri() {
        return URI.create(properties.getUrl());
    }

    private void applyAuthentication(HttpRequest.Builder requestBuilder) {
        if (StringUtils.hasText(properties.getUsername())) {
            requestBuilder.header("X-ClickHouse-User", properties.getUsername());
            requestBuilder.header("X-ClickHouse-Key", Objects.toString(properties.getPassword(), ""));
        }
    }

    private BusinessException clickHouseException(String body) {
        String message = StringUtils.hasText(body) ? body.strip().lines().findFirst().orElse(body.strip()) : "ClickHouse 执行失败";
        return new BusinessException(ErrorCode.BAD_REQUEST, message, HttpStatus.BAD_REQUEST);
    }

    private List<AisRecordView> parseRecords(String body) {
        List<AisRecordView> records = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(body))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                JsonNode node = objectMapper.readTree(line);
                records.add(new AisRecordView(
                        text(node, "id"),
                        text(node, "mmsi"),
                        localDateTime(node, "baseDateTime"),
                        decimal(node, "longitude"),
                        decimal(node, "latitude"),
                        decimal(node, "sog"),
                        decimal(node, "cog"),
                        integer(node, "heading"),
                        text(node, "vesselName"),
                        text(node, "imo"),
                        text(node, "callSign"),
                        integer(node, "vesselType"),
                        integer(node, "status"),
                        decimal(node, "length"),
                        decimal(node, "width"),
                        decimal(node, "draft"),
                        integer(node, "cargo"),
                        text(node, "transceiver"),
                        text(node, "note"),
                        text(node, "sourceFile"),
                        longValue(node, "importedByUserId"),
                        text(node, "importedByName"),
                        localDateTime(node, "importedAt"),
                        null
                ));
            }
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "ClickHouse 查询结果解析失败", HttpStatus.BAD_REQUEST);
        }
        return records;
    }

    private List<AisVesselDraftCandidate> parseVesselDraftCandidates(String body) {
        List<AisVesselDraftCandidate> candidates = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(body))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!StringUtils.hasText(line)) {
                    continue;
                }
                JsonNode node = objectMapper.readTree(line);
                candidates.add(new AisVesselDraftCandidate(
                        text(node, "recordId"),
                        text(node, "mmsi"),
                        text(node, "imo"),
                        text(node, "vesselName"),
                        text(node, "callSign"),
                        decimal(node, "length"),
                        decimal(node, "width"),
                        decimal(node, "draft"),
                        text(node, "sourceFile"),
                        localDateTime(node, "baseDateTime")
                ));
            }
            return candidates;
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "ClickHouse 船舶候选结果解析失败", HttpStatus.BAD_REQUEST);
        }
    }

    private long parseTotal(String body) {
        try (BufferedReader reader = new BufferedReader(new StringReader(body))) {
            String line = reader.readLine();
            if (!StringUtils.hasText(line)) {
                return 0;
            }
            return objectMapper.readTree(line).path("total").asLong(0);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "ClickHouse 统计结果解析失败", HttpStatus.BAD_REQUEST);
        }
    }

    private AisSummaryAggregate parseSummaryAggregate(String body) {
        try (BufferedReader reader = new BufferedReader(new StringReader(body))) {
            String line = reader.readLine();
            if (!StringUtils.hasText(line)) {
                return AisSummaryAggregate.empty();
            }
            JsonNode node = objectMapper.readTree(line);
            long total = node.path("total").asLong(0);
            if (total == 0) {
                return AisSummaryAggregate.empty();
            }
            return new AisSummaryAggregate(
                    total,
                    localDateTime(node, "firstBaseDateTime"),
                    localDateTime(node, "latestBaseDateTime")
            );
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "ClickHouse AIS summary parse failed", HttpStatus.BAD_REQUEST);
        }
    }


    private AisRiskSummary parseRiskSummary(String body) {
        try (BufferedReader reader = new BufferedReader(new StringReader(body))) {
            String line = reader.readLine();
            if (!StringUtils.hasText(line)) {
                return AisRiskSummary.empty();
            }
            JsonNode node = objectMapper.readTree(line);
            return new AisRiskSummary(
                    node.path("total").asLong(0),
                    node.path("lowSpeedCount").asLong(0),
                    node.path("stoppedCount").asLong(0),
                    node.path("abnormalNoteCount").asLong(0),
                    node.path("uniqueVesselCount").asLong(0)
            );
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "ClickHouse AIS risk summary parse failed", HttpStatus.BAD_REQUEST);
        }
    }

    private String parseTextField(String body, String field) {
        try (BufferedReader reader = new BufferedReader(new StringReader(body))) {
            String line = reader.readLine();
            if (!StringUtils.hasText(line)) {
                return "";
            }
            return text(objectMapper.readTree(line), field);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "ClickHouse 查询结果解析失败", HttpStatus.BAD_REQUEST);
        }
    }

    private List<String> parseTextFields(String body, String field) {
        List<String> values = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(body))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!StringUtils.hasText(line)) {
                    continue;
                }
                String value = text(objectMapper.readTree(line), field);
                if (StringUtils.hasText(value)) {
                    values.add(value);
                }
            }
            return values;
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "ClickHouse result parse failed", HttpStatus.BAD_REQUEST);
        }
    }

    private List<AisDatasetDateStat> parseDatasetDateStats(String body) {
        List<AisDatasetDateStat> stats = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(body))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!StringUtils.hasText(line)) {
                    continue;
                }
                JsonNode node = objectMapper.readTree(line);
                String datasetDate = text(node, "datasetDate");
                if (StringUtils.hasText(datasetDate)) {
                    stats.add(new AisDatasetDateStat(datasetDate, node.path("recordCount").asLong(0)));
                }
            }
            return stats;
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "ClickHouse date stats parse failed", HttpStatus.BAD_REQUEST);
        }
    }

    private List<AisRankingStat> parseRankingStats(String body) {
        List<AisRankingStat> stats = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(body))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!StringUtils.hasText(line)) {
                    continue;
                }
                JsonNode node = objectMapper.readTree(line);
                String label = text(node, "label");
                if (StringUtils.hasText(label)) {
                    stats.add(new AisRankingStat(label, node.path("recordCount").asLong(0)));
                }
            }
            return stats;
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "ClickHouse ranking stats parse failed", HttpStatus.BAD_REQUEST);
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return value.isNumber() ? value.decimalValue() : decimal(value.asText());
    }

    private Integer integer(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return value.asInt();
    }

    private Long longValue(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return value.asLong();
    }

    private LocalDateTime localDateTime(JsonNode node, String field) {
        return parseDateTime(text(node, field));
    }

    private void validateDateRange(LocalDateTime observedFrom, LocalDateTime observedTo) {
        if (observedFrom != null && observedTo != null && observedFrom.isAfter(observedTo)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "开始时间不能晚于结束时间", HttpStatus.BAD_REQUEST);
        }
    }

    private String safeIdentifier(String value) {
        if (!StringUtils.hasText(value) || !value.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "ClickHouse database 配置不合法", HttpStatus.BAD_REQUEST);
        }
        return value;
    }

    private String tableName() {
        return safeIdentifier(properties.getDatabase()) + ".ais_records";
    }

    private String escapeSqlString(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String blankToEmpty(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String repairMojibake(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        if (!(value.contains("Ã") || value.contains("Â") || value.contains("ç"))) {
            return value;
        }
        try {
            return new String(value.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        } catch (RuntimeException ex) {
            return value;
        }
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private ImportProgressState startImportProgress(String taskId, String sourceFile, long totalBytes, Integer limit) {
        String safeTaskId = StringUtils.hasText(taskId) ? taskId.trim() : sha256Hex(sourceFile + "|" + Instant.now());
        ImportProgressState state = new ImportProgressState(
                safeTaskId,
                sourceFile,
                Math.max(totalBytes, 0),
                limit == null ? 0 : limit
        );
        importProgresses.put(safeTaskId, state);
        return state;
    }

    private static class CountingInputStream extends FilterInputStream {
        private final ImportProgressState progress;

        CountingInputStream(InputStream input, ImportProgressState progress) {
            super(input);
            this.progress = progress;
        }

        @Override
        public int read() throws IOException {
            int value = super.read();
            if (value >= 0) {
                progress.addBytes(1);
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int read = super.read(buffer, offset, length);
            if (read > 0) {
                progress.addBytes(read);
            }
            return read;
        }
    }

    private static class ImportProgressState {
        private final String taskId;
        private final String sourceFile;
        private final long totalBytes;
        private final int limit;
        private final LocalDateTime startedAt = LocalDateTime.now();
        private long bytesRead;
        private int imported;
        private int skipped;
        private String status = "running";
        private String message = "准备导入";
        private LocalDateTime updatedAt = startedAt;

        ImportProgressState(String taskId, String sourceFile, long totalBytes, int limit) {
            this.taskId = taskId;
            this.sourceFile = sourceFile;
            this.totalBytes = totalBytes;
            this.limit = limit;
        }

        synchronized void addBytes(long bytes) {
            bytesRead += bytes;
            touch();
        }

        synchronized void update(int imported, int skipped, String message) {
            this.imported = imported;
            this.skipped = skipped;
            this.message = message;
            touch();
        }

        synchronized void complete(int imported, int skipped) {
            this.imported = imported;
            this.skipped = skipped;
            this.bytesRead = Math.max(bytesRead, totalBytes);
            this.status = "completed";
            this.message = "导入完成";
            touch();
        }

        synchronized void fail(String message) {
            this.status = "failed";
            this.message = StringUtils.hasText(message) ? message : "导入失败";
            touch();
        }

        synchronized AisImportProgress snapshot() {
            return new AisImportProgress(
                    taskId,
                    sourceFile,
                    status,
                    bytesRead,
                    totalBytes,
                    imported,
                    skipped,
                    limit,
                    progress(),
                    message,
                    startedAt,
                    updatedAt
            );
        }

        private int progress() {
            if ("completed".equals(status)) {
                return 100;
            }
            if (totalBytes <= 0) {
                return imported > 0 ? 95 : 5;
            }
            long boundedBytes = Math.max(0, Math.min(bytesRead, totalBytes));
            int percentage = (int) Math.floor((boundedBytes * 100.0) / totalBytes);
            return Math.max(1, Math.min(99, percentage));
        }

        private void touch() {
            updatedAt = LocalDateTime.now();
        }
    }

    private enum UpdateType {
        STRING,
        FLOAT,
        UINT16
    }

    private record UpdateColumn(
            String column,
            UpdateType type,
            boolean nullable
    ) {
    }

    private record ParsedAisRow(
            String recordId,
            String mmsi,
            LocalDateTime baseDateTime,
            BigDecimal longitude,
            BigDecimal latitude,
            BigDecimal sog,
            BigDecimal cog,
            Integer heading,
            String vesselName,
            String imo,
            String callSign,
            Integer vesselType,
            Integer status,
            BigDecimal length,
            BigDecimal width,
            BigDecimal draft,
            Integer cargo,
            String transceiver,
            String sourceFile
    ) {
    }

    private record AisSummaryAggregate(
            long total,
            LocalDateTime firstBaseDateTime,
            LocalDateTime latestBaseDateTime
    ) {
        static AisSummaryAggregate empty() {
            return new AisSummaryAggregate(0, null, null);
        }
    }
}
