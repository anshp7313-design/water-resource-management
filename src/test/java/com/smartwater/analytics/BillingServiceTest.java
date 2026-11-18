package com.smartwater.analytics;

import com.smartwater.model.BillEstimate;
import com.smartwater.model.TariffPlan;
import com.smartwater.model.WaterRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BillingServiceTest {
    @Test
    void estimateBillAppliesAllowanceAndFixedCharge() {
        BillingService service = BillingService.getInstance();
        TariffPlan plan = new TariffPlan("Test", 25.0, 0.10, 100.0);
        List<WaterRecord> records = List.of(
                new WaterRecord(LocalDate.of(2026, 5, 1), 80, ""),
                new WaterRecord(LocalDate.of(2026, 5, 2), 70, ""),
                new WaterRecord(LocalDate.of(2026, 4, 30), 999, ""));

        BillEstimate estimate = service.estimateBill(records, plan,
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));

        assertEquals(150.0, estimate.getUsageLiters(), 0.001);
        assertEquals(50.0, estimate.getBillableLiters(), 0.001);
        assertEquals(5.0, estimate.getVariableCharge(), 0.001);
        assertEquals(30.0, estimate.getTotal(), 0.001);
        assertEquals(0.10, estimate.getRatePerLiter(), 0.001);
        assertEquals(0.0, estimate.getRemainingAllowanceLiters(), 0.001);
    }

    @Test
    void estimateCurrentMonthBillProjectsFromUsageToDate() {
        BillingService service = BillingService.getInstance();
        TariffPlan plan = new TariffPlan("Forecast", 20.0, 0.05, 100.0);
        List<WaterRecord> records = List.of(
                new WaterRecord(LocalDate.of(2026, 5, 1), 50, ""),
                new WaterRecord(LocalDate.of(2026, 5, 2), 50, ""),
                new WaterRecord(LocalDate.of(2026, 5, 3), 50, ""));

        BillEstimate estimate = service.estimateCurrentMonthBill(records, plan, LocalDate.of(2026, 5, 3));

        assertEquals(150.0, estimate.getUsageLiters(), 0.001);
        assertEquals(50.0, estimate.getBillableLiters(), 0.001);
        assertEquals(22.5, estimate.getTotal(), 0.001);
        assertEquals(1550.0, estimate.getProjectedUsageLiters(), 0.001);
        assertEquals(1450.0, estimate.getProjectedBillableLiters(), 0.001);
        assertEquals(92.5, estimate.getProjectedTotal(), 0.001);
    }
}
