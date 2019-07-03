package com.ringly.ringly.ui.screens.activity;

import com.google.common.base.Optional;
import com.ringly.ringly.config.model.Height;
import com.ringly.ringly.config.model.Weight;
import com.ringly.ringly.db.StepEvent;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class StepsData implements Comparable<StepsData> {
    private static final long MILLIS_PER_YEAR = (long) 3.155693e+10;

    final Date startDate;
    final Date endDate;
    final int steps;
    final int cals;
    final double distance; // In miles
    final int minutes;

    public StepsData(Date startDate, Date endDate, int steps, int cals, double distance, int minutes) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.steps = steps;
        this.cals = cals;
        this.distance = Math.round(distance * 100) / 100.0;
        this.minutes = minutes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StepsData stepsData = (StepsData) o;

        if (steps != stepsData.steps) return false;
        if (cals != stepsData.cals) return false;
        if (minutes != stepsData.minutes) return false;
        if (Double.compare(stepsData.distance, distance) != 0) return false;
        if (startDate != null ? !startDate.equals(stepsData.startDate) : stepsData.startDate != null)
            return false;
        return endDate != null ? endDate.equals(stepsData.endDate) : stepsData.endDate == null;

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = startDate != null ? startDate.hashCode() : 0;
        result = 31 * result + (endDate != null ? endDate.hashCode() : 0);
        result = 31 * result + steps;
        result = 31 * result + cals;
        result = 31 * result + minutes;
        temp = Double.doubleToLongBits(distance);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public int compareTo(StepsData o) {
        return startDate.compareTo(o.startDate);
    }

    public static final class Builder {
        Date startDate;
        Date endDate;
        int walking;
        int running;
        int count;

        public Builder() {
            walking = 0;
            running = 0;
        }

        public Builder addStepEvent(StepEvent se) {
            walking += se.walking;
            running += se.running;
            if (startDate == null || se.date.getTime() < startDate.getTime()) {
                startDate = se.date;
            }
            if (endDate == null || se.date.getTime() > endDate.getTime()) {
                endDate = se.date;
            }
            count++;
            return this;
        }

        public StepsData build(Height height, Weight weight, Optional<Long> birthday, boolean isFemale, int field) {
            double walkingDist = height.value == -1 ?
                -1 : height.getMiles() * (isFemale ? 0.413 : 0.415) * walking;
            double runningDist = height.value == -1 ?
                -1 : height.getMiles() * (isFemale ? 0.413 : 0.415) * running;
            int cals = weight.value == -1 || height.value == -1 || !birthday.isPresent() ?
                -1 : (int) ((1.2 * walkingDist + 1.5 * runningDist) * weight.getKg());

            // Add BMR calories to calorie count
            if (cals >= 0 && birthday.isPresent()) {
                int adjust = isFemale ? -161 : 5;
                float age = (startDate.getTime() - birthday.get()) / MILLIS_PER_YEAR;
                Calendar cal = Calendar.getInstance();

                Calendar start = Calendar.getInstance();
                start.setTime(startDate);
                start = com.ringly.ringly.ui.Utilities.truncateCal(start, field);
                Calendar end = (Calendar) start.clone();
                end.add(field, 1);
                end.setTimeInMillis(Math.min(cal.getTimeInMillis(), end.getTimeInMillis()));

                float percent = (float) (end.getTimeInMillis() - start.getTimeInMillis()) /
                    (float) TimeUnit.DAYS.toMillis(1);

                cals += Math.max(0, (10 * weight.getKg() + 6.25 * height.getCm() - 5 * age + adjust) * percent);
            }

            return new StepsData(startDate, endDate, walking + running, cals, walkingDist + runningDist, count);
        }
    }
}
