package com.smartwater.export;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import com.smartwater.analytics.BillingService;
import com.smartwater.analytics.LeakDetectionService;
import com.smartwater.model.BillEstimate;
import com.smartwater.model.HouseholdProfile;
import com.smartwater.model.LeakAlert;
import com.smartwater.model.TariffPlan;
import com.smartwater.model.WaterRecord;
import com.smartwater.utils.AppClock;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Generates printable water usage reports.
 */
public class ReportGenerator {
    private final BillingService billingService = BillingService.getInstance();
    private final LeakDetectionService leakDetectionService = LeakDetectionService.getInstance();

    public void generatePdfReport(List<WaterRecord> records, HouseholdProfile profile, TariffPlan tariffPlan,
            String outputPath) throws IOException {
        try (PdfWriter writer = new PdfWriter(outputPath);
                PdfDocument pdf = new PdfDocument(writer);
                Document document = new Document(pdf)) {

            document.add(new Paragraph("Water Consumption Report").setBold().setFontSize(18));
            document.add(new Paragraph("Household: " + profile.getHouseholdName()));
            document.add(new Paragraph("Members: " + profile.getMemberCount()));
            document.add(new Paragraph("Daily Limit: " + String.format("%.1f L", profile.getDailyLimit())));
            document.add(new Paragraph("Generated: " + AppClock.today()));
            document.add(new Paragraph(" "));

            addSummary(document, records, profile, tariffPlan);
            addRecordsTable(document, records, profile);
        }
    }

    private void addSummary(Document document, List<WaterRecord> records, HouseholdProfile profile,
            TariffPlan tariffPlan) {
        document.add(new Paragraph("Summary").setBold().setFontSize(14));
        if (records.isEmpty()) {
            document.add(new Paragraph("No records available for the selected period."));
            return;
        }

        double total = records.stream().mapToDouble(WaterRecord::getLitersUsed).sum();
        double average = total / records.size();
        long exceededDays = records.stream().filter(r -> r.getLitersUsed() > profile.getDailyLimit()).count();
        LocalDate from = records.stream().map(WaterRecord::getDate).min(Comparator.naturalOrder()).orElse(AppClock.today());
        LocalDate to = records.stream().map(WaterRecord::getDate).max(Comparator.naturalOrder()).orElse(AppClock.today());
        BillEstimate bill = billingService.estimateBill(records, tariffPlan, from, to);
        LeakAlert leakAlert = leakDetectionService.analyzeRecentUsage(records, profile.getDailyLimit());

        document.add(new Paragraph("Total usage: " + String.format("%.1f L", total)));
        document.add(new Paragraph("Average daily usage: " + String.format("%.1f L", average)));
        document.add(new Paragraph("Days exceeding limit: " + exceededDays));
        document.add(new Paragraph("Estimated bill: " + formatCurrency(bill.getTotal())));
        document.add(new Paragraph("Leak watch: " + leakAlert.getMessage()));
        document.add(new Paragraph(" "));
    }

    private String formatCurrency(double amount) {
        return "Rs. " + String.format("%.2f", amount);
    }

    private void addRecordsTable(Document document, List<WaterRecord> records, HouseholdProfile profile) {
        document.add(new Paragraph("Records").setBold().setFontSize(14));

        Table table = new Table(UnitValue.createPercentArray(new float[] { 2, 2, 2, 4 }));
        table.setWidth(UnitValue.createPercentValue(100));
        table.addHeaderCell(new Cell().add(new Paragraph("Date").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Liters").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Status").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Note").setBold()));

        records.stream()
                .sorted(Comparator.comparing(WaterRecord::getDate))
                .forEach(record -> {
                    table.addCell(record.getDate().toString());
                    table.addCell(String.format("%.1f", record.getLitersUsed()));
                    table.addCell(record.getLitersUsed() > profile.getDailyLimit() ? "Exceeded" : "Safe");
                    table.addCell(record.getNote() == null ? "" : record.getNote());
                });

        document.add(table);
    }
}
