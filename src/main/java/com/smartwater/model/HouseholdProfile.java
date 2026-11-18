package com.smartwater.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Represents household profile configuration for water monitoring.
 * Uses JavaFX Properties for Reactive Data Binding.
 */
public class HouseholdProfile {
    private final StringProperty householdName;
    private final IntegerProperty memberCount;
    private final DoubleProperty dailyLimit;
    private final StringProperty city;
    private final BooleanProperty alertAt70;
    private final BooleanProperty alertAt90;
    private final BooleanProperty darkMode;

    public HouseholdProfile(String householdName, int memberCount, double dailyLimit,
            String city, boolean alertAt70, boolean alertAt90, boolean darkMode) {
        this.householdName = new SimpleStringProperty(householdName);
        this.memberCount = new SimpleIntegerProperty(memberCount);
        this.dailyLimit = new SimpleDoubleProperty(dailyLimit);
        this.city = new SimpleStringProperty(city);
        this.alertAt70 = new SimpleBooleanProperty(alertAt70);
        this.alertAt90 = new SimpleBooleanProperty(alertAt90);
        this.darkMode = new SimpleBooleanProperty(darkMode);
    }

    public String getHouseholdName() {
        return householdName.get();
    }

    public StringProperty householdNameProperty() {
        return householdName;
    }

    public void setHouseholdName(String householdName) {
        this.householdName.set(householdName);
    }

    public int getMemberCount() {
        return memberCount.get();
    }

    public IntegerProperty memberCountProperty() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount.set(memberCount);
    }

    public double getDailyLimit() {
        return dailyLimit.get();
    }

    public DoubleProperty dailyLimitProperty() {
        return dailyLimit;
    }

    public void setDailyLimit(double dailyLimit) {
        this.dailyLimit.set(dailyLimit);
    }

    public String getCity() {
        return city.get();
    }

    public StringProperty cityProperty() {
        return city;
    }

    public void setCity(String city) {
        this.city.set(city);
    }

    public boolean isAlertAt70() {
        return alertAt70.get();
    }

    public BooleanProperty alertAt70Property() {
        return alertAt70;
    }

    public void setAlertAt70(boolean alertAt70) {
        this.alertAt70.set(alertAt70);
    }

    public boolean isAlertAt90() {
        return alertAt90.get();
    }

    public BooleanProperty alertAt90Property() {
        return alertAt90;
    }

    public void setAlertAt90(boolean alertAt90) {
        this.alertAt90.set(alertAt90);
    }

    public boolean isDarkMode() {
        return darkMode.get();
    }

    public BooleanProperty darkModeProperty() {
        return darkMode;
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode.set(darkMode);
    }

    public static double suggestLimit(int members) {
        return members * 50.0;
    }

    @Override
    public String toString() {
        return "HouseholdProfile{" +
                "householdName='" + getHouseholdName() + '\'' +
                ", memberCount=" + getMemberCount() +
                ", dailyLimit=" + getDailyLimit() +
                ", city='" + getCity() + '\'' +
                ", alertAt70=" + isAlertAt70() +
                ", alertAt90=" + isAlertAt90() +
                ", darkMode=" + isDarkMode() +
                '}';
    }
}
