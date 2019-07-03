package com.ringly.ringly.bluetooth;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.ringly.ringly.config.Bluetooth;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("InstanceVariableNamingConvention")
public final class RingName {

    @SuppressWarnings("HardCodedStringLiteral")
    private static final Pattern NON_WORD = Pattern.compile("\\W");
    @SuppressWarnings("HardCodedStringLiteral")
    private static final Pattern PATTERN
            = Pattern.compile('^' + Bluetooth.RINGLY + " (\\S*) (\\S*) \\((.*)\\)$", Pattern.CASE_INSENSITIVE);

    private static final Set<String> STYLES_RING = ImmutableSet.of(
            "DAYD",
            "DIVE",
            "WOOD",
            "STAR",
            "WINE",
            "2SEA",
            "DAYB",
            "NITE",
            "LUST",
            "GDIS",
            "DISR",
            "DATE",
            "HOUR",
            "MOON",
            "TIDE",
            "DAY2"
    );

    private static final Set<String> STYLES_BRACELET = ImmutableSet.of(
            "LAKE",
            "FOTO",
            "VOUS",
            "BACK",
            "WALK",
            "ROAD",
            "LOVE",
            "GO01",
            "GO02",
            "ROSE",
            "JETS",
            "RIDE",
            "BONV"
    );


    public enum ProductType {
        RING(1000, 500),
        BRACELET(850, 375),
        ;

        // values for activity tracking depending on product type:
        // https://docs.google.com/document/d/1scM3sOb78GJ7QYA_qR-Bxo5l6YoBx_YkW3f7Fko3GIo/edit
        public final int minimumPeakIntensity;
        public final int minimumPeakHeight;

        ProductType(final int minimumPeakIntensity, final int minimumPeakHeight) {
            this.minimumPeakIntensity = minimumPeakIntensity;
            this.minimumPeakHeight = minimumPeakHeight;
        }

        public static ProductType fromStyle(String style) {
            if (style == null) {
                return BRACELET;
            }

            String upperStyle = style.toUpperCase();

            if (STYLES_BRACELET.contains(upperStyle)) {
                return BRACELET;
            } else if (STYLES_RING.contains(upperStyle)) {
                return RING;
            }
            return BRACELET;
        }
    }

    //
    // fields
    //

    public final String modifier; // "-" for CZ, "*" for RDC
    public final String type; // 4-letter bluetooth style name, e.g., "VOUS", "DIVE", "LAKE"
    public final String address; // last 4 chars of mac address
    public final ProductType productType; // derived from type

    public RingName(final String modifier, final String type, String address) {
        address = NON_WORD.matcher(address).replaceAll("").toLowerCase();

        this.modifier = modifier;
        this.type = type;
        this.address = address.length() <= 4
                ? address : address.substring(address.length() - 4, address.length());
        this.productType = ProductType.fromStyle(type);
    }

    @SuppressWarnings("UnusedDeclaration")
    public static Optional<RingName> fromString(final CharSequence name) {
        final Matcher m = PATTERN.matcher(name);
        if (!m.find()) return Optional.absent();
        return Optional.of(new RingName(m.group(1), m.group(2), m.group(3)));
    }

    /**
     * This compare the advertising name from a complete name with another advertising name
     * @param deviceName A complete name ex:RINGLY - LOVE (a301)
     * @param advertisingName ex: LOVE
     * @return true if both advertising name are equals
     */
    public static boolean isEqualToAdvertisingName(final CharSequence deviceName, String advertisingName) {
        final Matcher m = PATTERN.matcher(deviceName);
        if (!m.find()) return false;
        return m.group(2).equalsIgnoreCase(advertisingName);
    }

    public static Optional<String> replaceName(final CharSequence name, String newName) {
        final Matcher m = PATTERN.matcher(name);
        if (!m.find()) return Optional.absent();
        return Optional.of(name.toString().replace(m.group(2),newName));
    }

    /*
     * This method is used for display by ArrayListAdapter<RingName>.
     */
    @Override
    public String toString() {
        return String.format("RINGLY %s %s (%s)", modifier, type, address);
    }

    @SuppressWarnings("UnnecessaryThis")
    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof RingName)) return false;
        final RingName that = (RingName) o;
        return this.modifier.equals(that.modifier)
                && this.type.equals(that.type)
                && this.address.equals(that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(modifier, type, address);
    }
}
