package com.ringly.ringly.bluetooth;

import android.graphics.Color;
import android.support.annotation.ColorInt;

import com.google.common.primitives.Bytes;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Calendar;
import java.util.TimeZone;

@SuppressWarnings({"MagicNumber", "NumericCastThatLosesPrecision",
        "InstanceVariableNamingConvention", "AccessingNonPublicFieldOfAnotherObject"})
public final class Rubric {

    ////
    //// protocol specifics
    ////

    @SuppressWarnings("InnerClassFieldHidesOuterClassField")
    public enum Type {
        // TODO separate classes for each type
        MOTOR_LED(1),
        ENTER_DFU(3),
        MOBILE_OS(7),
        DATE_TIME(8),
        SLEEP_MODE(10),
        DISCONNECTION_BUZZ(13),
        CONNECTION_LIGHT(14),
        ADVERTISING_NAME(6),
        ;

        private final byte id; // actually a short, but so far we only use the LSB
        Type(final int id) { this.id = (byte)id; }
    }

    public enum MobileOs {
        IOS(1),
        ANDROID(2),
        ;

        private final byte id;
        MobileOs(final int id) { this.id = (byte)id; }
    }


    public byte[] serialize() {
        if (type == Type.MOTOR_LED) {
            return new byte[] {
                    0, type.id,
                    14, // length
                    (byte) Color.red(notificationColor),
                    (byte) Color.green(notificationColor),
                    (byte) Color.blue(notificationColor),
                    (byte) Color.red(contactColor),
                    (byte) Color.green(contactColor),
                    (byte) Color.blue(contactColor),
                    ledDelay,
                    ledDurationOn,
                    ledDurationOff,
                    ledCount,
                    vibrationIntensity,
                    vibrationDurationOn,
                    vibrationDurationOff,
                    vibrationCount
            };
        } else if (type == Type.ENTER_DFU) {
            return new byte[] {
                    0, type.id,
                    1, // length
                    4 // 20 seconds
            };
        } else if (type == Type.MOBILE_OS) {
            return new byte[] {
                    0, type.id,
                    2, // length
                    mobileOs.id,
                    0 // don't want "factory mode"
            };
        } else if (type == Type.DATE_TIME) {
            return Bytes.concat(new byte[] { 0, 8, 14 }, currentTime);
        } else if (type == Type.SLEEP_MODE) {
            return new byte[] {
                    0, type.id,
                    1, // length
                    sleepDuration
            };
        } else if (type == Type.DISCONNECTION_BUZZ) {
            return new byte[] {
                    0, type.id,
                    6, // length
                    disconnectionTime,
                    vibrationCount, // different order from MOTOR_LED :'(
                    vibrationDurationOn,
                    vibrationDurationOff,
                    vibrationIntensity,
                    backoffTime
            };
        } else if (type == Type.CONNECTION_LIGHT) {
            return new byte[] {
                    0, type.id,
                    1, // length
                    (byte) (connectionLight ? 1 : 2)
            };
        } else if (type == Type.ADVERTISING_NAME) {
            String customerName = (diamondClub ? "*" : "-") + " " + advertisingName + "\0";
            int byteLength = 3 + customerName.length();
            byte[] advertisingNameByte = new byte[byteLength];
            advertisingNameByte[0] = 0;
            advertisingNameByte[1] = type.id;
            advertisingNameByte[2] = (byte) (customerName.length());
            for(int i = 0; i<customerName.length();i++) {
                advertisingNameByte[i+3] = customerName.getBytes()[i];
            }
            return  advertisingNameByte;
        }

        else throw new IllegalStateException("unknown rubric type: " + type);
    }


    ////
    //// simple immutable data container
    ////

    private final Type type;

    @ColorInt private final int notificationColor;
    @ColorInt private final int contactColor;
    private final byte ledDelay;
    private final byte ledDurationOn;
    private final byte ledDurationOff;
    private final byte ledCount;
    private final byte vibrationIntensity;
    private final byte vibrationDurationOn;
    private final byte vibrationDurationOff;
    private final byte vibrationCount;

    private final MobileOs mobileOs;

    private final byte[] currentTime;

    private final byte sleepDuration;

    private final byte disconnectionTime;
    private final byte backoffTime;

    private final boolean connectionLight;

    private final String advertisingName;

    private final boolean diamondClub;

    private Rubric(final Builder builder) {
        type = builder.type;

        notificationColor = builder.notificationColor;
        contactColor = builder.contactColor;
        ledDelay = builder.ledDelay;
        ledDurationOn = builder.ledDurationOn;
        ledDurationOff = builder.ledDurationOff;
        ledCount = builder.ledCount;
        vibrationIntensity = builder.vibrationIntensity;
        vibrationDurationOn = builder.vibrationDurationOn;
        vibrationDurationOff = builder.vibrationDurationOff;
        vibrationCount = builder.vibrationCount;

        mobileOs = builder.mobileOs;

        currentTime = builder.currentTime;

        sleepDuration = builder.sleepDuration;

        disconnectionTime = builder.disconnectionTime;
        backoffTime = builder.backoffTime;

        connectionLight = builder.connectionLight;

        advertisingName = builder.advertisingName;
        diamondClub = builder.diamondClub;
    }

    public static final Rubric ENTER_DFU
            = new Builder().type(Type.ENTER_DFU).build();

    public static final Rubric ANDROID_OS
            = new Builder().type(Type.MOBILE_OS).mobileOsType(MobileOs.ANDROID).build();

    @SuppressWarnings("ParameterHidesMemberVariable")
    public static Rubric sleepMode(final int sleepDuration) {
        return new Builder().type(Type.SLEEP_MODE).sleepDuration(sleepDuration).build();
    }

    public static Rubric connectionLight(final boolean val) {
        return new Builder().type(Type.CONNECTION_LIGHT).connectionLight(val).build();
    }

    public static final class Builder {
        private Type type = Type.MOTOR_LED;

        @ColorInt private int notificationColor;
        @ColorInt private int contactColor;
        private byte ledDelay;
        private byte ledDurationOn;
        private byte ledDurationOff;
        private byte ledCount;
        private byte vibrationIntensity;
        private byte vibrationDurationOn;
        private byte vibrationDurationOff;
        private byte vibrationCount;

        private MobileOs mobileOs;

        private byte[] currentTime;

        private byte sleepDuration;

        private byte disconnectionTime;
        private byte backoffTime;

        private boolean connectionLight;

        private String advertisingName;
        private boolean diamondClub;
        public Builder type(final Type val) {
            type = val; return this;
        }

        public Builder notificationColor(@ColorInt final int val) {
            notificationColor = val; return this;
        }
        @SuppressWarnings("UnusedDeclaration")
        public Builder contactColor(@ColorInt final int val) {
            contactColor = val; return this;
        }
        public Builder ledDelay(final int val) {
            ledDelay = asByte(val); return this;
        }
        public Builder ledDurationOn(final int val) {
            ledDurationOn = asByte(val); return this;
        }
        public Builder ledDurationOff(final int val) {
            ledDurationOff = asByte(val); return this;
        }
        public Builder ledCount(final int val) {
            ledCount = asByte(val); return this;
        }
        public Builder vibrationIntensity(final int val) {
            vibrationIntensity = asByte(val); return this;
        }
        public Builder vibrationDurationOn(final int val) {
            vibrationDurationOn = asByte(val); return this;
        }
        public Builder vibrationDurationOff(final int val) {
            vibrationDurationOff = asByte(val); return this;
        }
        public Builder vibrationCount(final int val) {
            vibrationCount = asByte(val); return this;
        }

        public Builder mobileOsType(final MobileOs val) {
            mobileOs = val; return this;
        }

        public Builder currentTime(long millis) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(millis);
            cal.setTimeZone(TimeZone.getTimeZone("UTC"));
            currentTime = ArrayUtils
                    .add(String.format("%1$tY%1$tm%1$tdT%1$tH%1$tM", cal).getBytes(), (byte) 0);
            return this;
        }

        public Builder sleepDuration(final int val) {
            sleepDuration = asByte(val); return this;
        }

        public Builder disconnectionTime(final int val) {
            disconnectionTime = asByte(val); return this;
        }
        public Builder backoffTime(final int val) {
            backoffTime = asByte(val); return this;
        }

        public Builder connectionLight(final boolean val) {
            connectionLight = val; return this;
        }

        public Builder advertisingName(final String val) {
            advertisingName = val; return this;
        }
        public Builder diamondClub(final boolean val) {
            diamondClub = val; return this;
        }

        private static byte asByte(final int x) {
            if (x < 0 || x > 255) {
                throw new IllegalArgumentException("value must be in the range 0 to 255");
            }
            return (byte) x;
        }


        public Rubric build() {
            return new Rubric(this);
        }
    }

}
