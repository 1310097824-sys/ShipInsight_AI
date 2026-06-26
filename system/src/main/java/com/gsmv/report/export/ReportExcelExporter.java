package com.gsmv.report.export;

import com.gsmv.report.dto.DashboardSummary;
import com.gsmv.report.dto.EcosystemAnalyticsPoint;
import com.gsmv.report.dto.NameValuePoint;
import com.gsmv.report.dto.AisRecordMapPoint;
import com.gsmv.report.dto.ReportExportSnapshot;
import com.gsmv.report.dto.VesselDistributionPoint;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public final class ReportExcelExporter {

    private ReportExcelExporter() {
    }

    public static byte[] export(ReportExportSnapshot snapshot) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            CellStyle headerStyle = createHeaderStyle(workbook);

            writeSummarySheet(workbook, headerStyle, snapshot.summary());
            writeNameValueSheet(workbook, headerStyle, "RiskDistribution", "风险等级", snapshot.riskDistribution());
            writeNameValueSheet(workbook, headerStyle, "OperationalStatus", "航行状态", snapshot.operationalStatusDistribution());
            writeNameValueSheet(workbook, headerStyle, "VesselCategoryL1", "大类", snapshot.speciesPhylumDistribution());
            writeNameValueSheet(workbook, headerStyle, "VesselCategoryL2", "子类", snapshot.speciesClassDistribution());
            writeNameValueSheet(workbook, headerStyle, "TrafficTrend", "日期", snapshot.aisRecordTrend());
            writeNameValueSheet(workbook, headerStyle, "RecorderActivity", "记录人员", snapshot.aisRecordActivityByUser());
            writeEcosystemSheet(workbook, headerStyle, snapshot.shippingZoneStats());
            writeSpeciesMapSheet(workbook, headerStyle, snapshot.vesselDistributionPoints());
            writeObservationMapSheet(workbook, headerStyle, snapshot.aisRecordMapPoints());

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("导出 Excel 失败", exception);
        }
    }

    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle cellStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        cellStyle.setFont(font);
        return cellStyle;
    }

    private static void writeSummarySheet(Workbook workbook, CellStyle headerStyle, DashboardSummary summary) {
        Sheet sheet = workbook.createSheet("Summary");
        createHeader(sheet, headerStyle, "指标", "值");
        writePair(sheet, 1, "船舶档案数", summary.totalVesselProfiles());
        writePair(sheet, 2, "手动记录数", summary.totalAisRecords());
        writePair(sheet, 3, "航运区域数", summary.totalShippingZones());
        writePair(sheet, 4, "活跃用户数", summary.totalUsers());
        writePair(sheet, 5, "近 7 天观测次数", summary.recentAisRecordCount());
        autoSize(sheet, 2);
    }

    private static void writeNameValueSheet(
            Workbook workbook,
            CellStyle headerStyle,
            String sheetName,
            String nameHeader,
            List<NameValuePoint> rows
    ) {
        Sheet sheet = workbook.createSheet(sheetName);
        createHeader(sheet, headerStyle, nameHeader, "数量");
        for (int index = 0; index < rows.size(); index++) {
            NameValuePoint row = rows.get(index);
            writePair(sheet, index + 1, row.name(), row.value());
        }
        autoSize(sheet, 2);
    }

    private static void writeEcosystemSheet(Workbook workbook, CellStyle headerStyle, List<EcosystemAnalyticsPoint> rows) {
        Sheet sheet = workbook.createSheet("ShippingZoneStats");
        createHeader(sheet, headerStyle, "区域名", "类型", "记录数", "关联船舶数");
        for (int index = 0; index < rows.size(); index++) {
            EcosystemAnalyticsPoint row = rows.get(index);
            Row excelRow = sheet.createRow(index + 1);
            excelRow.createCell(0).setCellValue(nullSafe(row.zoneName()));
            excelRow.createCell(1).setCellValue(nullSafe(row.zoneType()));
            excelRow.createCell(2).setCellValue(row.recordCount());
            excelRow.createCell(3).setCellValue(row.linkedVesselCount());
        }
        autoSize(sheet, 4);
    }

    private static void writeSpeciesMapSheet(Workbook workbook, CellStyle headerStyle, List<VesselDistributionPoint> rows) {
        Sheet sheet = workbook.createSheet("VesselDistribution");
        createHeader(sheet, headerStyle, "船名", "显示名", "长(m)", "宽(m)", "航线范围", "风险等级", "航行状态");
        for (int index = 0; index < rows.size(); index++) {
            VesselDistributionPoint row = rows.get(index);
            Row excelRow = sheet.createRow(index + 1);
            excelRow.createCell(0).setCellValue(nullSafe(row.displayName()));
            excelRow.createCell(1).setCellValue(nullSafe(row.profileName()));
            excelRow.createCell(2).setCellValue(row.locationLat() == null ? "" : row.locationLat().toPlainString());
            excelRow.createCell(3).setCellValue(row.locationLng() == null ? "" : row.locationLng().toPlainString());
            excelRow.createCell(4).setCellValue(nullSafe(row.routeDescription()));
            excelRow.createCell(5).setCellValue(nullSafe(row.riskLevel()));
            excelRow.createCell(6).setCellValue(nullSafe(row.operationalStatus()));
        }
        autoSize(sheet, 7);
    }

    private static void writeObservationMapSheet(Workbook workbook, CellStyle headerStyle, List<AisRecordMapPoint> rows) {
        Sheet sheet = workbook.createSheet("AisRecordMap");
        createHeader(sheet, headerStyle, "地点", "航运区域", "记录人员", "记录时间", "纬度", "经度", "关联船舶数", "备注");
        for (int index = 0; index < rows.size(); index++) {
            AisRecordMapPoint row = rows.get(index);
            Row excelRow = sheet.createRow(index + 1);
            excelRow.createCell(0).setCellValue(nullSafe(row.locationName()));
            excelRow.createCell(1).setCellValue(nullSafe(row.shippingZoneName()));
            excelRow.createCell(2).setCellValue(nullSafe(row.recorderName()));
            excelRow.createCell(3).setCellValue(row.recordedAt() == null ? "" : row.recordedAt().toString());
            excelRow.createCell(4).setCellValue(row.locationLat() == null ? "" : row.locationLat().toPlainString());
            excelRow.createCell(5).setCellValue(row.locationLng() == null ? "" : row.locationLng().toPlainString());
            excelRow.createCell(6).setCellValue(row.linkedVesselCount());
            excelRow.createCell(7).setCellValue(nullSafe(row.note()));
        }
        autoSize(sheet, 8);
    }

    private static void createHeader(Sheet sheet, CellStyle headerStyle, String... headers) {
        Row row = sheet.createRow(0);
        for (int index = 0; index < headers.length; index++) {
            Cell cell = row.createCell(index);
            cell.setCellValue(headers[index]);
            cell.setCellStyle(headerStyle);
        }
    }

    private static void writePair(Sheet sheet, int rowIndex, String name, long value) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(nullSafe(name));
        row.createCell(1).setCellValue(value);
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private static void autoSize(Sheet sheet, int columnCount) {
        for (int index = 0; index < columnCount; index++) {
            sheet.autoSizeColumn(index);
        }
    }
}
