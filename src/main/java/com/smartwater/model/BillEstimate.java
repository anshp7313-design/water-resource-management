package com.smartwater.model;

/**
 * Estimated bill for a usage period.
 */
public class BillEstimate {
    private final double usageLiters;
    private final double billableLiters;
    private final double fixedCharge;
    private final double variableCharge;
    private final double total;
    private final double freeAllowanceLiters;
    private final double ratePerLiter;
    private final double projectedUsageLiters;
    private final double projectedBillableLiters;
    private final double projectedVariableCharge;
    private final double projectedTotal;

    public BillEstimate(double usageLiters, double billableLiters, double fixedCharge, double variableCharge) {
        this(usageLiters, billableLiters, fixedCharge, variableCharge, 0, 0,
                usageLiters, billableLiters, variableCharge);
    }

    public BillEstimate(double usageLiters, double billableLiters, double fixedCharge, double variableCharge,
            double freeAllowanceLiters, double ratePerLiter, double projectedUsageLiters,
            double projectedBillableLiters, double projectedVariableCharge) {
        this.usageLiters = usageLiters;
        this.billableLiters = billableLiters;
        this.fixedCharge = fixedCharge;
        this.variableCharge = variableCharge;
        this.total = fixedCharge + variableCharge;
        this.freeAllowanceLiters = freeAllowanceLiters;
        this.ratePerLiter = ratePerLiter;
        this.projectedUsageLiters = projectedUsageLiters;
        this.projectedBillableLiters = projectedBillableLiters;
        this.projectedVariableCharge = projectedVariableCharge;
        this.projectedTotal = fixedCharge + projectedVariableCharge;
    }

    public double getUsageLiters() {
        return usageLiters;
    }

    public double getBillableLiters() {
        return billableLiters;
    }

    public double getFixedCharge() {
        return fixedCharge;
    }

    public double getVariableCharge() {
        return variableCharge;
    }

    public double getTotal() {
        return total;
    }

    public double getFreeAllowanceLiters() {
        return freeAllowanceLiters;
    }

    public double getRatePerLiter() {
        return ratePerLiter;
    }

    public double getRemainingAllowanceLiters() {
        return Math.max(0, freeAllowanceLiters - usageLiters);
    }

    public double getProjectedUsageLiters() {
        return projectedUsageLiters;
    }

    public double getProjectedBillableLiters() {
        return projectedBillableLiters;
    }

    public double getProjectedVariableCharge() {
        return projectedVariableCharge;
    }

    public double getProjectedTotal() {
        return projectedTotal;
    }
}
