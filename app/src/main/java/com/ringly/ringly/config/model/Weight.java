package com.ringly.ringly.config.model;

public class Weight {
    public static final double LB_TO_KG = 0.4536;

    public enum WeightUnit { KG, LB }

    public final float value;
    public final WeightUnit unit;

    public Weight(float value, WeightUnit unit) {
        this.unit = unit;
        this.value = value;
    }

    public double getKg() {
        return unit == WeightUnit.KG ? value : value * LB_TO_KG;
    }

    public String toString() {
        return value < 0 ? "" : value + " " + unit.toString().toLowerCase();
    }
}
