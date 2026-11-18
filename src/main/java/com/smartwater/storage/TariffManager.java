package com.smartwater.storage;

import com.smartwater.database.DatabaseConnection;
import com.smartwater.database.DatabaseSchema;
import com.smartwater.model.TariffPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Persists tariff plans used for bill estimation.
 */
public class TariffManager {
    private static final Logger logger = LoggerFactory.getLogger(TariffManager.class);
    private static TariffManager instance;

    private final DatabaseConnection dbConnection;

    private TariffManager() {
        this.dbConnection = DatabaseConnection.getInstance();
        new DatabaseSchema(dbConnection).initialize();
    }

    public static synchronized TariffManager getInstance() {
        if (instance == null) {
            instance = new TariffManager();
        }
        return instance;
    }

    public TariffPlan getActiveTariffPlan() {
        return getActiveTariffPlan(0);
    }

    public TariffPlan getActiveTariffPlan(int userId) {
        return getActiveTariffPlan(userId, TariffPlan.defaultPlan());
    }

    public TariffPlan getActiveTariffPlan(int userId, TariffPlan fallbackPlan) {
        try {
            String sql = """
                        SELECT id, name, fixed_monthly_charge, rate_per_liter, free_allowance_liters
                        FROM tariff_plans
                        WHERE user_id = ? AND active = 1
                        ORDER BY id DESC
                        LIMIT 1
                    """;
            try (ResultSet rs = dbConnection.executeQuery(sql, userId)) {
                if (rs.next()) {
                    return extractTariffPlan(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to load active tariff plan: {}", e.getMessage(), e);
        }

        TariffPlan defaultPlan = fallbackPlan == null ? TariffPlan.defaultPlan() : fallbackPlan;
        saveTariffPlan(defaultPlan, userId);
        return defaultPlan;
    }

    public boolean saveTariffPlan(TariffPlan plan) {
        return saveTariffPlan(plan, 0);
    }

    public boolean saveTariffPlan(TariffPlan plan, int userId) {
        try {
            dbConnection.executeUpdate("UPDATE tariff_plans SET active = 0 WHERE user_id = ? AND active = 1", userId);
            String sql = """
                        INSERT INTO tariff_plans (user_id, name, fixed_monthly_charge, rate_per_liter, free_allowance_liters, active)
                        VALUES (?, ?, ?, ?, ?, 1)
                    """;
            dbConnection.executeUpdate(sql, userId, plan.getName(), plan.getFixedMonthlyCharge(), plan.getRatePerLiter(),
                    plan.getFreeAllowanceLiters());
            logger.info("Saved tariff plan: {}", plan.getName());
            return true;
        } catch (SQLException e) {
            logger.error("Failed to save tariff plan: {}", e.getMessage(), e);
            return false;
        }
    }

    private TariffPlan extractTariffPlan(ResultSet rs) throws SQLException {
        return new TariffPlan(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getDouble("fixed_monthly_charge"),
                rs.getDouble("rate_per_liter"),
                rs.getDouble("free_allowance_liters"));
    }
}
