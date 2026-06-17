package com.gsmv.ais;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.luben.zstd.ZstdInputStream;
import com.gsmv.ais.dto.AisBatchDeleteRequest;
import com.gsmv.ais.dto.AisBatchOperationResult;
import com.gsmv.ais.dto.AisBatchUpdateRequest;
import com.gsmv.ais.dto.AisImportResult;
import com.gsmv.ais.dto.AisRecordView;
import com.gsmv.common.ErrorCode;
import com.gsmv.common.PageResponse;
import com.gsmv.common.exception.BusinessException;
import com.gsmv.security.CurrentUser;
import com.gsmv.security.SecurityUtils;
import java.io.BufferedReader;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
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
    private final HttpClient httpClient;
    private volatile boolean schemaReady;

    public AisService(AisClickHouseProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
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

        List<AisRecordView> items = parseRecords(postQuery(listSql));
        long total = parseTotal(postQuery(countSql));
        return new PageResponse<>(items, total, safePage, safeSize);
    }

    public AisImportResult importFile(MultipartFile file, int limit) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择要导入的 AIS 文件", HttpStatus.BAD_REQUEST);
        }

        ensureSchema();
        CurrentUser currentUser = SecurityUtils.requireCurrentUser();
        int safeLimit = Math.min(Math.max(limit, 1), 1000);
        String uploadName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "local-upload";
        int skipped = 0;
        List<Map<String, Object>> insertRows = new ArrayList<>();

        try (
                InputStream input = openPossiblyCompressed(file);
                BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))
        ) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "AIS 文件没有表头", HttpStatus.BAD_REQUEST);
            }
            List<String> headers = parseCsvLine(stripBom(headerLine));
            Map<String, Integer> columns = buildColumnIndex(headers);

            String line;
            while ((line = reader.readLine()) != null && insertRows.size() < safeLimit) {
                if (line.isBlank()) {
                    continue;
                }
                List<String> values = parseCsvLine(line);
                ParsedAisRow parsed = parseAisRow(values, columns, uploadName);
                if (parsed == null) {
                    skipped++;
                    continue;
                }
                insertRows.add(toClickHouseRow(parsed, currentUser));
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "AIS 文件读取失败：" + ex.getMessage(), HttpStatus.BAD_REQUEST);
        }

        if (!insertRows.isEmpty()) {
            insertRows(insertRows);
        }
        return new AisImportResult(uploadName, insertRows.size(), skipped, safeLimit);
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

    private InputStream openPossiblyCompressed(MultipartFile file) throws IOException {
        PushbackInputStream input = new PushbackInputStream(file.getInputStream(), 8);
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

    private String buildWhere(String keyword, LocalDateTime observedFrom, LocalDateTime observedTo) {
        List<String> clauses = buildFilterClauses(keyword, observedFrom, observedTo);
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
                        localDateTime(node, "importedAt")
                ));
            }
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "ClickHouse 查询结果解析失败", HttpStatus.BAD_REQUEST);
        }
        return records;
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
}
