package com.ringly.ringly.config;


import com.ringly.ringly.bluetooth.Rubric;

import java.util.UUID;

@SuppressWarnings("MagicNumber")
public final class Utilities {
    private Utilities() {}

    public static Rubric getNotificationRubric(
            final Color color, final Vibration vibration, final Color contactColor,
            final boolean shortColor
    ) {
        final Rubric.Builder rubric = new Rubric.Builder();

        if (vibration != Vibration.NONE) {
            rubric.vibrationCount(vibration.count)
                    .vibrationIntensity(173)
                    .vibrationDurationOn(5)
                    .vibrationDurationOff(5);
        }

        // XXX: omitting these seems to send a command which causes the Ring to hang for 20 seconds or so.
        rubric.ledDelay(50 * vibration.count).ledCount(1);
        rubric.ledDurationOn(1).ledDurationOff(1); // setting some default values

        if (color != Color.NONE || contactColor != Color.NONE) {
            rubric.ledDelay(50 * vibration.count).ledCount(1);
            if (shortColor) rubric.ledDurationOn(5).ledDurationOff(5);
            else rubric.ledDurationOn(25).ledDurationOff(25);

            if (color != Color.NONE) rubric.notificationColor(color.ledColor);
            if (contactColor != Color.NONE) rubric.contactColor(contactColor.ledColor);
        }

        return rubric.build();
    }
}
