package com.smartwater.export;

import com.smartwater.model.WaterRecord;
import com.smartwater.model.HouseholdProfile;
import com.smartwater.model.TariffPlan;
import com.smartwater.utils.AppClock;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Export service for generating reports in multiple formats (CSV, Excel, PDF).
 */
public class ExportService {
    private static final Logger logger = LoggerFactory.getLogger(ExportService.class);
    private static ExportService instance;
    private final ReportGenerator reportGenerator = new ReportGenerator();

    /**
     * Private constructor for singleton pattern.
     */
    private ExportService() {
        logger.info("ExportService initialized");
    }

    /**
     * Gets the singleton instance.
     *
     * @return the ExportService instance
     */
    public static synchronized ExportService getInstance() {
        if (instance == null) {
            instance = new ExportService();
        }
        return instance;
    }

    /**
     * Exports water records to CSV format.
     *
     * @param records    the records to export
     * @param outputPath the file path to save to
     * @throws IOException if export fails
     */
    public void exportToCSV(List<WaterRecord> records, String outputPath) throws IOException {
        try {
            logger.info("Exporting {} records to CSV: {}", records.size(), outputPath);

            // Create output directory if it doesn't exist
            Files.createDirectories(Paths.get(outputPath).getParent());

            try (FileWriter writer = new FileWriter(outputPath);
                    CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                            .withHeader("Date", "Liters Used", "Note"))) {

                for (WaterRecord record : records) {
                    csvPrinter.printRecord(
                            record.getDate(),
                            String.format("%.2f", record.getLitersUsed()),
                            record.getNote() != null ? record.getNote() : "");
                }

                csvPrinter.flush();
                logger.info("CSV export completed successfully");
            }

        } catch (IOException e) {
            logger.error("Error exporting to CSV: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Exports water records to Excel format.
     *
     * @param records    the records to export
     * @param profile    the household profile
     * @param outputPath the file path to save to
     * @throws IOException if export fails
     */
    public void exportToExcel(List<WaterRecord> records, HouseholdProfile profile, String outputPath)
            throws IOException {
        try {
            logger.info("Exporting {} records to Excel: {}", records.size(), outputPath);

            // Create output directory if it doesn't exist
            Files.createDirectories(Paths.get(outputPath).getParent());

            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Water Usage");

                // Create header style
                CellStyle headerStyle = workbook.createCellStyle();
                Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerStyle.setFont(headerFont);
                headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
                headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                // Create data style
                CellStyle dataStyle = workbook.createCellStyle();
                dataStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00"));

                // Write headers
                Row headerRow = sheet.createRow(0);
                String[] headers = { "Date", "Liters Used", "Daily Limit", "Percentage", "Note" };
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                    cell.setCellStyle(headerStyle);
                }

                // Write data
                int rowNum = 1;
                for (WaterRecord record : records) {
                    Row row = sheet.createRow(rowNum++);

                    Cell dateCell = row.createCell(0);
                    dateCell.setCellValue(record.getDate().format(DateTimeFormatter.ISO_LOCAL_DATE));

                    Cell litersCell = row.createCell(1);
                    litersCell.setCellValue(record.getLitersUsed());
                    litersCell.setCellStyle(dataStyle);

                    Cell limitCell = row.createCell(2);
                    limitCell.setCellValue(profile.getDailyLimit());
                    limitCell.setCellStyle(dataStyle);

                    Cell percentageCell = row.createCell(3);
                    double percentage = (record.getLitersUsed() / profile.getDailyLimit()) * 100;
                    percentageCell.setCellValue(percentage);
                    percentageCell.setCellStyle(dataStyle);

                    Cell noteCell = row.createCell(4);
                    noteCell.setCellValue(record.getNote() != null ? record.getNote() : "");
                }

                // Auto-fit columns
                for (int i = 0; i < headers.length; i++) {
                    sheet.autoSizeColumn(i);
                }

                // Save workbook
                try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                    workbook.write(fos);
                }

                logger.info("Excel export completed successfully");
            }

        } catch (IOException e) {
            logger.error("Error exporting to Excel: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Exports water records to JSON format.
     *
     * @param records    the records to export
     * @param profile    the household profile
     * @param outputPath the file path to save to
     * @throws IOException if export fails
     */
    public void exportToJSON(List<WaterRecord> records, HouseholdProfile profile, String outputPath)
            throws IOException {
        try {
            logger.info("Exporting {} records to JSON: {}", records.size(), outputPath);

            // Create output directory if it doesn't exist
            Files.createDirectories(Paths.get(outputPath).getParent());

            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"exportDate\": \"").append(AppClock.today()).append("\",\n");
            json.append("  \"profile\": {\n");
            json.append("    \"householdName\": \"").append(escapeJson(profile.getHouseholdName())).append("\",\n");
            json.append("    \"members\": ").append(profile.getMemberCount()).append(",\n");
            json.append("    \"dailyLimit\": ").append(profile.getDailyLimit()).append("\n");
            json.append("  },\n");
            json.append("  \"records\": [\n");

            for (int i = 0; i < records.size(); i++) {
                WaterRecord record = records.get(i);
                json.append("    {\n");
                json.append("      \"date\": \"").append(record.getDate()).append("\",\n");
                json.append("      \"litersUsed\": ").append(record.getLitersUsed()).append(",\n");
                json.append("      \"note\": \"").append(escapeJson(record.getNote() != null ? record.getNote() : ""))
                        .append("\"\n");
                json.append("    }");
                if (i < records.size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }

            json.append("  ]\n");
            json.append("}\n");

            Files.write(Paths.get(outputPath), json.toString().getBytes());
            logger.info("JSON export completed successfully");

        } catch (IOException e) {
            logger.error("Error exporting to JSON: {}", e.getMessage(), e);
            throw e;
        }
    }

    public void exportToPDF(List<WaterRecord> records, HouseholdProfile profile, TariffPlan tariffPlan,
            String outputPath) throws IOException {
        try {
            logger.info("Exporting {} records to PDF: {}", records.size(), outputPath);
            Files.createDirectories(Paths.get(outputPath).getParent());
            reportGenerator.generatePdfReport(records, profile, tariffPlan, outputPath);
            logger.info("PDF export completed successfully");
        } catch (IOException e) {
            logger.error("Error exporting to PDF: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Generates a summary report as text.
     *
     * @param records the records to summarize
     * @param profile the household profile
     * @return summary report as string
     */
    public String generateSummaryReport(List<WaterRecord> records, HouseholdProfile profile) {
        StringBuilder report = new StringBuilder();

        report.append("=== Water Consumption Report ===\n\n");
        report.append("Household: ").append(profile.getHouseholdName()).append("\n");
        report.append("Members: ").append(profile.getMemberCount()).append("\n");
        report.append("Daily Limit: ").append(profile.getDailyLimit()).append(" L\n");
        report.append("Report Generated: ").append(AppClock.today()).append("\n\n");

        if (records.isEmpty()) {
            report.append("No data available.\n");
            return report.toString();
        }

        double totalUsage = records.stream().mapToDouble(WaterRecord::getLitersUsed).sum();
        double averageUsage = totalUsage / records.size();
        double maxUsage = records.stream().mapToDouble(WaterRecord::getLitersUsed).max().orElse(0);
        double minUsage = records.stream().mapToDouble(WaterRecord::getLitersUsed).min().orElse(0);

        report.append("Statistics:\n");
        report.append("  Total Usage: ").append(String.format("%.2f", totalUsage)).append(" L\n");
        report.append("  Average Usage: ").append(String.format("%.2f", averageUsage)).append(" L/day\n");
        report.append("  Maximum Usage: ").append(String.format("%.2f", maxUsage)).append(" L\n");
        report.append("  Minimum Usage: ").append(String.format("%.2f", minUsage)).append(" L\n");

        long exceedingDays = records.stream()
                .filter(r -> r.getLitersUsed() > profile.getDailyLimit())
                .count();
        report.append("  Days Exceeding Limit: ").append(exceedingDays).append("\n");

        return report.toString();
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
