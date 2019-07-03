package com.ringly.ringly.config.model;

public class Height {

    public enum HeightUnit { IN, CM }

    public static final double CM_TO_MI = 0.00000621371;
    public static final double IN_TO_MI = 0.00001578;
    public static final double IN_TO_CM = 2.54;


    public final int value;
    public final HeightUnit unit;

    public Height(float value, HeightUnit unit) {
        this(value < 0 ? -1 : (int) value, unit);
    }

    public Height(int value, HeightUnit unit) {
        this.unit = unit;
        this.value = value < 0 ? -1 : value;
    }

    public double getMiles() {
        return this.unit == HeightUnit.CM ? this.value * CM_TO_MI : this.value * IN_TO_MI;
    }

    public double getCm() {
        return this.unit == HeightUnit.CM ? this.value : this.value * IN_TO_CM;
    }

    public String toString() {
        if(value < 0) {
            return "";
        }

        switch(unit) {
            case IN:
                return (value / 12) + " ft " + (value % 12) + " in";
            case CM:
                return value + " cm";
        }

        return "";
    }
}
