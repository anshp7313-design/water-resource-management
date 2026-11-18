package com.smartwater.analytics;

import com.smartwater.model.BillEstimate;
import com.smartwater.model.TariffPlan;
import com.smartwater.model.WaterRecord;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Estimates bills from usage and tariff settings.
 */
public class BillingService {
    private static BillingService instance;

    private BillingService() {
    }

    public static synchronized BillingService getInstance() {
        if (instance == null) {
            instance = new BillingService();
        }
        return instance;
    }

    public BillEstimate estimateBill(List<WaterRecord> records, TariffPlan plan, LocalDate from, LocalDate to) {
        double usage = usageForPeriod(records, from, to);
        return buildEstimate(usage, usage, plan);
    }

    public BillEstimate estimateCurrentMonthBill(List<WaterRecord> records, TariffPlan plan, LocalDate asOf) {
        YearMonth month = YearMonth.from(asOf);
        LocalDate from = month.atDay(1);
        LocalDate cappedAsOf = asOf.isAfter(month.atEndOfMonth()) ? month.atEndOfMonth() : asOf;
        double usageToDate = usageForPeriod(records, from, cappedAsOf);
        long elapsedDays = Math.max(1, ChronoUnit.DAYS.between(from, cappedAsOf) + 1);
        double projectedUsage = usageToDate / elapsedDays * month.lengthOfMonth();
        return buildEstimate(usageToDate, projectedUsage, plan);
    }

    private double usageForPeriod(List<WaterRecord> records, LocalDate from, LocalDate to) {
        if (records == null || from == null || to == null) {
            return 0;
        }
        LocalDate start = from.isAfter(to) ? to : from;
        LocalDate end = from.isAfter(to) ? from : to;
        return records.stream()
                .filter(r -> r != null && !r.getDate().isBefore(start) && !r.getDate().isAfter(end))
                .mapToDouble(WaterRecord::getLitersUsed)
                .sum();
    }

    private BillEstimate buildEstimate(double usage, double projectedUsage, TariffPlan plan) {
        TariffPlan effectivePlan = plan == null ? TariffPlan.defaultPlan() : plan;
        double allowance = effectivePlan.getFreeAllowanceLiters();
        double rate = effectivePlan.getRatePerLiter();
        double billable = Math.max(0, usage - allowance);
        double variable = billable * rate;
        double projectedBillable = Math.max(0, projectedUsage - allowance);
        double projectedVariable = projectedBillable * rate;
        return new BillEstimate(usage, billable, effectivePlan.getFixedMonthlyCharge(), variable,
                allowance, rate, projectedUsage, projectedBillable, projectedVariable);
    }
}
