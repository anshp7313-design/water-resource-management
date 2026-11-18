package com.smartwater.model;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDate;

/**
 * Represents a water consumption record for a specific date.
 * Uses JavaFX Properties for Reactive Data Binding.
 */
public class WaterRecord {
    private final ObjectProperty<LocalDate> date;
    private final DoubleProperty litersUsed;
    private final StringProperty note;

    public WaterRecord(LocalDate date, double litersUsed, String note) {
        this.date = new SimpleObjectProperty<>(date);
        this.litersUsed = new SimpleDoubleProperty(litersUsed);
        this.note = new SimpleStringProperty(note);
    }

    public LocalDate getDate() {
        return date.get();
    }

    public ObjectProperty<LocalDate> dateProperty() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date.set(date);
    }

    public double getLitersUsed() {
        return litersUsed.get();
    }

    public DoubleProperty litersUsedProperty() {
        return litersUsed;
    }

    public void setLitersUsed(double litersUsed) {
        this.litersUsed.set(litersUsed);
    }

    public String getNote() {
        return note.get();
    }

    public StringProperty noteProperty() {
        return note;
    }

    public void setNote(String note) {
        this.note.set(note);
    }

    @Override
    public String toString() {
        return "WaterRecord{" +
                "date=" + getDate() +
                ", litersUsed=" + getLitersUsed() +
                ", note='" + getNote() + '\'' +
                '}';
    }
}
