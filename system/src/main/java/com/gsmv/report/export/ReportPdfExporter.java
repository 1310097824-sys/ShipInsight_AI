package com.gsmv.report.export;

import com.gsmv.report.dto.DashboardSummary;
import com.gsmv.report.dto.EcosystemAnalyticsPoint;
import com.gsmv.report.dto.NameValuePoint;
import com.gsmv.report.dto.AisRecordMapPoint;
import com.gsmv.report.dto.ReportExportSnapshot;
import com.gsmv.report.dto.VesselDistributionPoint;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

public final class ReportPdfExporter {

    private static final float PAGE_MARGIN = 32F;
    private static final float DEFAULT_FONT_SIZE = 10F;
    private static final float TITLE_FONT_SIZE = 18F;
    private static final float SECTION_FONT_SIZE = 13F;
    private static final float LINE_SPACING = 14F;

    private ReportPdfExporter() {
    }

    public static byte[] export(ReportExportSnapshot snapshot) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDFont font = loadFont(document);
            PdfLineWriter writer = new PdfLineWriter(document, font);

            writer.writeTitle("ShipInsight 船舶态势分析报表");
            writeSummary(writer, snapshot.summary());
            writeNameValueSection(writer, "风险等级分布", "风险等级", snapshot.riskDistribution());
            writeNameValueSection(writer, "航行状态分布", "航行状态", snapshot.operationalStatusDistribution());
            writeNameValueSection(writer, "船舶大类分布", "大类", snapshot.speciesPhylumDistribution());
            writeNameValueSection(writer, "船舶子类分布", "子类", snapshot.speciesClassDistribution());
            writeNameValueSection(writer, "近 30 天交通趋势", "日期", snapshot.aisRecordTrend());
            writeNameValueSection(writer, "记录人员活跃度", "记录人员", snapshot.aisRecordActivityByUser());
            writeEcosystemSection(writer, snapshot.shippingZoneStats());
            writeSpeciesMapSection(writer, snapshot.vesselDistributionPoints());
            writeObservationMapSection(writer, snapshot.aisRecordMapPoints());

            writer.close();
            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("导出 PDF 失败", exception);
        }
    }

    private static PDFont loadFont(PDDocument document) throws IOException {
        List<Path> candidates = List.of(
                Path.of("C:/Windows/Fonts/simhei.ttf"),
                Path.of("C:/Windows/Fonts/NotoSansSC-VF.ttf"),
                Path.of("C:/Windows/Fonts/simsunb.ttf")
        );

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return PDType0Font.load(document, candidate.toFile());
            }
        }
        return PDType1Font.HELVETICA;
    }

    private static void writeSummary(PdfLineWriter writer, DashboardSummary summary) throws IOException {
        writer.writeSection("系统概览");
        writer.writeKeyValue("船舶档案数", String.valueOf(summary.totalVesselProfiles()));
        writer.writeKeyValue("手动记录数", String.valueOf(summary.totalAisRecords()));
        writer.writeKeyValue("航运区域数", String.valueOf(summary.totalShippingZones()));
        writer.writeKeyValue("活跃用户数", String.valueOf(summary.totalUsers()));
        writer.writeKeyValue("近 7 天观测次数", String.valueOf(summary.recentAisRecordCount()));
        writer.writeBlankLine();
    }

    private static void writeNameValueSection(
            PdfLineWriter writer,
            String title,
            String nameHeader,
            List<NameValuePoint> rows
    ) throws IOException {
        writer.writeSection(title);
        writer.writeLine(nameHeader + " | 数量");
        for (NameValuePoint row : rows) {
            writer.writeLine(nullSafe(row.name()) + " | " + row.value());
        }
        writer.writeBlankLine();
    }

    private static void writeEcosystemSection(PdfLineWriter writer, List<EcosystemAnalyticsPoint> rows) throws IOException {
        writer.writeSection("航运区域统计");
        writer.writeLine("区域名 | 类型 | 记录数 | 关联船舶数");
        for (EcosystemAnalyticsPoint row : rows) {
            writer.writeLine(
                    nullSafe(row.zoneName()) + " | " +
                    nullSafe(row.zoneType()) + " | " +
                    row.recordCount() + " | " +
                    row.linkedVesselCount()
            );
        }
        writer.writeBlankLine();
    }

    private static void writeSpeciesMapSection(PdfLineWriter writer, List<VesselDistributionPoint> rows) throws IOException {
        writer.writeSection("船舶分布点位");
        writer.writeLine("船名 | 显示名 | 坐标 | 航线范围 | 风险等级 | 航行状态");
        for (VesselDistributionPoint row : rows) {
            writer.writeLine(
                    nullSafe(row.displayName()) + " | " +
                    nullSafe(row.profileName()) + " | " +
                    toCoordinate(row.locationLat(), row.locationLng()) + " | " +
                    nullSafe(row.routeDescription()) + " | " +
                    nullSafe(row.riskLevel()) + " | " +
                    nullSafe(row.operationalStatus())
            );
        }
        writer.writeBlankLine();
    }

    private static void writeObservationMapSection(PdfLineWriter writer, List<AisRecordMapPoint> rows) throws IOException {
        writer.writeSection("AIS 记录概要");
        writer.writeLine("地点 | 航运区域 | 记录人员 | 记录时间 | 船舶数 | 备注");
        for (AisRecordMapPoint row : rows) {
            writer.writeLine(
                    nullSafe(row.locationName()) + " | " +
                    nullSafe(row.shippingZoneName()) + " | " +
                    nullSafe(row.recorderName()) + " | " +
                    toText(row.recordedAt()) + " | " +
                    row.linkedVesselCount() + " | " +
                    nullSafe(row.note())
            );
        }
        writer.writeBlankLine();
    }

    private static String toCoordinate(BigDecimal lat, BigDecimal lng) {
        return (lat == null ? "" : lat.toPlainString()) + ", " + (lng == null ? "" : lng.toPlainString());
    }

    private static String toText(LocalDateTime value) {
        return value == null ? "" : value.toString();
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private static final class PdfLineWriter {

        private final PDDocument document;
        private final PDFont font;
        private PDPage page;
        private PDPageContentStream contentStream;
        private float y;

        private PdfLineWriter(PDDocument document, PDFont font) throws IOException {
            this.document = document;
            this.font = font;
            startPage();
        }

        private void writeTitle(String text) throws IOException {
            writeWrapped(text, TITLE_FONT_SIZE, true);
            writeBlankLine();
        }

        private void writeSection(String text) throws IOException {
            writeWrapped(text, SECTION_FONT_SIZE, true);
        }

        private void writeKeyValue(String key, String value) throws IOException {
            writeWrapped(key + ": " + value, DEFAULT_FONT_SIZE, false);
        }

        private void writeLine(String text) throws IOException {
            writeWrapped(text, DEFAULT_FONT_SIZE, false);
        }

        private void writeBlankLine() throws IOException {
            ensureSpace(LINE_SPACING);
            y -= LINE_SPACING;
        }

        private void writeWrapped(String text, float fontSize, boolean emphasize) throws IOException {
            float maxWidth = PDRectangle.A4.getHeight() - PAGE_MARGIN * 2;
            List<String> lines = wrapText(text, fontSize, maxWidth);
            for (String line : lines) {
                ensureSpace(fontSize + 4);
                contentStream.beginText();
                contentStream.setFont(font, fontSize);
                contentStream.newLineAtOffset(PAGE_MARGIN, y);
                contentStream.showText(line);
                contentStream.endText();
                y -= fontSize + (emphasize ? 6 : 4);
            }
        }

        private List<String> wrapText(String text, float fontSize, float maxWidth) throws IOException {
            List<String> lines = new ArrayList<>();
            if (text == null || text.isEmpty()) {
                lines.add("");
                return lines;
            }

            StringBuilder current = new StringBuilder();
            for (int index = 0; index < text.length(); index++) {
                char currentChar = text.charAt(index);
                current.append(currentChar);
                float width = font.getStringWidth(current.toString()) / 1000 * fontSize;
                if (width > maxWidth && current.length() > 1) {
                    current.deleteCharAt(current.length() - 1);
                    lines.add(current.toString());
                    current = new StringBuilder().append(currentChar);
                }
            }

            if (!current.isEmpty()) {
                lines.add(current.toString());
            }

            return lines;
        }

        private void ensureSpace(float requiredHeight) throws IOException {
            if (y - requiredHeight >= PAGE_MARGIN) {
                return;
            }
            startPage();
        }

        private void startPage() throws IOException {
            closeCurrentStream();
            page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
            document.addPage(page);
            contentStream = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - PAGE_MARGIN;
        }

        private void close() throws IOException {
            closeCurrentStream();
        }

        private void closeCurrentStream() throws IOException {
            if (contentStream != null) {
                contentStream.close();
                contentStream = null;
            }
        }
    }
}
