package com.smartwater.model;

import com.smartwater.policy.MunicipalityPolicy;

/**
 * Simple billing configuration for estimating monthly water cost.
 */
public class TariffPlan {
    private final int id;
    private final String name;
    private final double fixedMonthlyCharge;
    private final double ratePerLiter;
    private final double freeAllowanceLiters;

    public TariffPlan(int id, String name, double fixedMonthlyCharge, double ratePerLiter, double freeAllowanceLiters) {
        this.id = id;
        this.name = name;
        this.fixedMonthlyCharge = fixedMonthlyCharge;
        this.ratePerLiter = ratePerLiter;
        this.freeAllowanceLiters = freeAllowanceLiters;
    }

    public TariffPlan(String name, double fixedMonthlyCharge, double ratePerLiter, double freeAllowanceLiters) {
        this(0, name, fixedMonthlyCharge, ratePerLiter, freeAllowanceLiters);
    }

    public static TariffPlan defaultPlan() {
        return MunicipalityPolicy.defaultTariffPlan("");
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getFixedMonthlyCharge() {
        return fixedMonthlyCharge;
    }

    public double getRatePerLiter() {
        return ratePerLiter;
    }

    public double getFreeAllowanceLiters() {
        return freeAllowanceLiters;
    }
}
