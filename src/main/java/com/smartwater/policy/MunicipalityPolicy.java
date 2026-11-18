package com.smartwater.policy;

import com.smartwater.model.TariffPlan;

/**
 * Central municipal policy used for household limits and tariff estimation.
 * Users can view these values, but should not directly edit them.
 */
public final class MunicipalityPolicy {
    private static final double LITERS_PER_PERSON_PER_DAY = 25.0;
    private static final double DEFAULT_FIXED_MONTHLY_CHARGE = 50.0;
    private static final double DEFAULT_FREE_ALLOWANCE_LITERS = 6000.0;
    private static final double DEFAULT_RATE_PER_LITER = 0.015;

    private MunicipalityPolicy() {
    }

    public static double calculateDailyLimit(int memberCount, String city) {
        int safeMemberCount = Math.max(1, memberCount);
        return safeMemberCount * LITERS_PER_PERSON_PER_DAY;
    }

    public static TariffPlan defaultTariffPlan(String city) {
        String name = city == null || city.isBlank()
                ? "Municipal household tariff"
                : city.trim() + " municipal tariff";
        return new TariffPlan(name, DEFAULT_FIXED_MONTHLY_CHARGE, DEFAULT_RATE_PER_LITER,
                DEFAULT_FREE_ALLOWANCE_LITERS);
    }

    public static String describeDailyLimit(int memberCount, String city) {
        return String.format("%.1f L/day municipal limit", calculateDailyLimit(memberCount, city));
    }
}
