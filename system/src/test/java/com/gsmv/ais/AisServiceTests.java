package com.gsmv.ais;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.gsmv.ais.dto.AisRankingStat;
import com.gsmv.ais.dto.AisRiskSummary;
import com.gsmv.ais.dto.AisVesselDraftCandidate;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class AisServiceTests {

    @Test
    void vesselDraftCandidatesQueryUsesFilteredSubqueryBeforeAggregation() throws Exception {
        List<String> requests = Collections.synchronizedList(new ArrayList<>());
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> handleRequest(exchange, requests));
        server.start();

        try {
            AisClickHouseProperties properties = new AisClickHouseProperties();
            properties.setUrl("http://localhost:" + server.getAddress().getPort() + "/");
            AisService service = new AisService(properties, new ObjectMapper(), mock(AisVesselLinkService.class));

            List<AisVesselDraftCandidate> candidates = service.vesselDraftCandidates("3677", null, null, 10);

            assertThat(candidates).hasSize(1);
            assertThat(candidates.get(0).mmsi()).isEqualTo("367793980");
            assertThat(candidates.get(0).imo()).isEqualTo("IMO1234567");

            String query = requests.stream()
                    .filter(body -> body.contains("GROUP BY identityKey"))
                    .findFirst()
                    .orElseThrow();
            assertThat(query).contains("mmsi AS rawMmsi");
            assertThat(query).contains("imo AS rawImo");
            assertThat(query).contains("GROUP BY identityKey");
            assertThat(query).contains("FROM (");
            assertThat(query).contains("positionCaseInsensitiveUTF8(mmsi, '3677')");
            assertThat(query).doesNotContain("GROUP BY if(mmsi != ''");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void vesselTrackByKeywordQueryMatchesMmsiImoAndVesselName() throws Exception {
        List<String> requests = Collections.synchronizedList(new ArrayList<>());
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> handleRequest(exchange, requests));
        server.start();

        try {
            AisClickHouseProperties properties = new AisClickHouseProperties();
            properties.setUrl("http://localhost:" + server.getAddress().getPort() + "/");
            AisService service = new AisService(properties, new ObjectMapper(), mock(AisVesselLinkService.class));

            service.vesselTrackByKeyword("CLEVELAND", 20);

            String query = requests.stream()
                    .filter(body -> body.contains("ORDER BY base_date_time ASC"))
                    .filter(body -> body.contains("positionCaseInsensitiveUTF8(vessel_name, 'CLEVELAND') > 0"))
                    .findFirst()
                    .orElseThrow();
            assertThat(query).contains("mmsi = 'CLEVELAND'");
            assertThat(query).contains("imo = 'CLEVELAND'");
            assertThat(query).contains("positionCaseInsensitiveUTF8(vessel_name, 'CLEVELAND') > 0");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void riskSummaryQueryAggregatesRiskSignalsAndParsesResult() throws Exception {
        List<String> requests = Collections.synchronizedList(new ArrayList<>());
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> handleRequest(exchange, requests));
        server.start();

        try {
            AisClickHouseProperties properties = new AisClickHouseProperties();
            properties.setUrl("http://localhost:" + server.getAddress().getPort() + "/");
            AisService service = new AisService(properties, new ObjectMapper(), mock(AisVesselLinkService.class));

            AisRiskSummary summary = service.riskSummary("CLEVELAND", null, null);

            assertThat(summary.total()).isEqualTo(10);
            assertThat(summary.lowSpeedCount()).isEqualTo(3);
            assertThat(summary.stoppedCount()).isEqualTo(2);
            assertThat(summary.abnormalNoteCount()).isEqualTo(1);
            assertThat(summary.uniqueVesselCount()).isEqualTo(4);

            String query = requests.stream()
                    .filter(body -> body.contains("lowSpeedCount"))
                    .findFirst()
                    .orElseThrow();
            assertThat(query).contains("countIf(sog IS NOT NULL AND sog < 1) AS lowSpeedCount");
            assertThat(query).contains("countIf(status IN (1, 5) OR (sog IS NOT NULL AND sog < 0.5)) AS stoppedCount");
            assertThat(query).contains("abnormalNoteCount");
            assertThat(query).contains("uniqExact(mmsi) AS uniqueVesselCount");
            assertThat(query).contains("positionCaseInsensitiveUTF8(vessel_name, 'CLEVELAND')");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void importerStatsQueryGroupsByImportedByNameAndParsesRanking() throws Exception {
        List<String> requests = Collections.synchronizedList(new ArrayList<>());
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> handleRequest(exchange, requests));
        server.start();

        try {
            AisClickHouseProperties properties = new AisClickHouseProperties();
            properties.setUrl("http://localhost:" + server.getAddress().getPort() + "/");
            AisService service = new AisService(properties, new ObjectMapper(), mock(AisVesselLinkService.class));

            List<AisRankingStat> stats = service.importerStats(null, null, null, 10);

            assertThat(stats).hasSize(2);
            assertThat(stats.get(0).label()).isEqualTo("tester");
            assertThat(stats.get(0).recordCount()).isEqualTo(120);

            String query = requests.stream()
                    .filter(body -> body.contains("imported_by_name"))
                    .filter(body -> body.contains("GROUP BY label"))
                    .findFirst()
                    .orElseThrow();
            assertThat(query).contains("if(trim(imported_by_name) = ''");
            assertThat(query).contains("ORDER BY recordCount DESC");
        } finally {
            server.stop(0);
        }
    }

    private static void handleRequest(HttpExchange exchange, List<String> requests) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        requests.add(body);

        String response = "";
        if (body.contains("lowSpeedCount")) {
            response = """
                    {"total":10,"lowSpeedCount":3,"stoppedCount":2,"abnormalNoteCount":1,"uniqueVesselCount":4}
                    """;
        } else if (body.contains("GROUP BY identityKey")) {
            response = """
                    {"recordId":"rec-1","mmsi":"367793980","imo":"IMO1234567","vesselName":"CLEVELAND","callSign":"CALL123","length":200.5,"width":32.2,"draft":10.1,"sourceFile":"ais.csv","baseDateTime":"2025-03-01 00:00:01"}
                    """;
        } else if (body.contains("ORDER BY base_date_time ASC")) {
            response = """
                    {"id":"track-1","mmsi":"367793980","baseDateTime":"2025-03-01 00:00:01","longitude":110.1,"latitude":21.2,"sog":10.5,"cog":12.3,"heading":13,"vesselName":"CLEVELAND","imo":"IMO1234567","callSign":"CALL123","vesselType":70,"status":5,"length":200.5,"width":32.2,"draft":10.1,"cargo":1,"transceiver":"A","note":"","sourceFile":"ais.csv","importedByUserId":1,"importedByName":"tester","importedAt":"2025-03-01 00:10:00"}
                    """;
        } else if (body.contains("GROUP BY label")) {
            response = """
                    {"label":"tester","recordCount":120}
                    {"label":"ops","recordCount":60}
                    """;
        } else if (body.contains("count() AS total")) {
            response = """
                    {"total":1}
                    """;
        }

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
