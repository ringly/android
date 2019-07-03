package com.ringly.ringly.config;

import com.ringly.ringly.bluetooth.Rubric;

@SuppressWarnings("MagicNumber")
public enum Command {

    BUZZ(Utilities.getNotificationRubric(Color.BLUE, Vibration.ONE, Color.NONE, false)),

    ENABLE_SLEEP_MODE(Rubric.sleepMode(15)),
    DISABLE_SLEEP_MODE(Rubric.sleepMode(0)),

    ENABLE_DISCONNECTION_BUZZ(new Rubric.Builder()
            .type(Rubric.Type.DISCONNECTION_BUZZ)
            .disconnectionTime(9)
            .vibrationCount(7)
            .vibrationIntensity(233)
            .vibrationDurationOn(13)
            .vibrationDurationOff(13)
            .backoffTime(10)
            .build()),
    DISABLE_DISCONNECTION_BUZZ(new Rubric.Builder()
            .type(Rubric.Type.DISCONNECTION_BUZZ)
            .disconnectionTime(0)
            .build()),

    ENABLE_CONNECTION_LIGHT(Rubric.connectionLight(true)),
    DISABLE_CONNECTION_LIGHT(Rubric.connectionLight(false)),

    ENTER_DFU(Rubric.ENTER_DFU),

    VALENTINES(new Rubric.Builder()
            .vibrationCount(3)
            .vibrationIntensity(173)
            .vibrationDurationOn(3)
            .vibrationDurationOff(3)
            .ledDelay(0)
            .ledCount(1)
            .ledDurationOn(8)
            .ledDurationOff(1)
            .notificationColor(android.graphics.Color.rgb(255, 128, 128)) // pink
            .contactColor(android.graphics.Color.WHITE) // white
            .build())
    ;


    @SuppressWarnings({"PublicField", "InstanceVariableNamingConvention"})
    public final Rubric rubric;
    Command(final Rubric rubric) {
        this.rubric = rubric;
    }

}
