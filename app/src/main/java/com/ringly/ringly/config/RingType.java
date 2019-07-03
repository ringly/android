package com.ringly.ringly.config;

import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;

import com.ringly.ringly.R;

@SuppressWarnings("InstanceVariableNamingConvention")

public enum RingType {

    // Rings
    _2SEA(R.drawable.ring_out_to_sea, R.drawable.stone_sea, R.drawable.base_gold, R.string.out_to_sea, DeviceType.RING),
    DAYB(R.drawable.ring_daybreak, R.drawable.stone_daybreak, R.drawable.base_gunmetal, R.string.daybreak, DeviceType.RING),
    DAYD(R.drawable.ring_daydream, R.drawable.stone_daydream, R.drawable.base_gold, R.string.daydream, DeviceType.RING),
    DIVE(R.drawable.ring_dive_bar, R.drawable.stone_divebar, R.drawable.base_gunmetal, R.string.dive_bar, DeviceType.RING),
    LUST(R.drawable.ring_wanderlust, R.drawable.stone_wanderlust, R.drawable.base_gunmetal, R.string.wanderlust, DeviceType.RING),
    NITE(R.drawable.ring_opening_night, R.drawable.stone_opening_night, R.drawable.base_gunmetal, R.string.opening_night, DeviceType.RING),
    STAR(R.drawable.ring_stargaze, R.drawable.stone_stargaze, R.drawable.base_gold, R.string.stargaze, DeviceType.RING),
    WINE(R.drawable.ring_wine_bar, R.drawable.stone_winebar, R.drawable.base_gold, R.string.wine_bar, DeviceType.RING),
    WOOD(R.drawable.ring_into_the_woods, R.drawable.stone_woods, R.drawable.base_gold, R.string.into_the_woods, DeviceType.RING),
    GDIS(R.drawable.ring_disrupt_gold, R.drawable.stone_divebar, R.drawable.base_gold, R.string.disrupt, DeviceType.RING),
    DISR(R.drawable.ring_dive_bar, R.drawable.stone_divebar, R.drawable.base_gunmetal, R.string.disrupt, DeviceType.RING),
    DATE(R.drawable.ring_first_date, R.drawable.stone_daydream, R.drawable.base_rose, R.string.first_date,DeviceType.RING),
    HOUR(R.drawable.ring_after_hours, R.drawable.stone_afterhours, R.drawable.base_gold, R.string.after_hours, DeviceType.RING),
    MOON(R.drawable.ring_full_moon, R.drawable.stone_daydream, R.drawable.base_silver, R.string.full_moon, DeviceType.RING),
    TIDE(R.drawable.ring_high_tide, R.drawable.ring_into_the_woods  ,R.drawable.base_gold, R.string.high_tide, DeviceType.RING),
    DAY2(R.drawable.ring_daydream2, R.drawable.stone_daydream, R.drawable.base_gold, R.string.daydream, DeviceType.RING),

    // Bracelets
    BACK(R.drawable.bracelet_backstage, R.drawable.stone_divebar, R.drawable.base_gold, R.string.backstage, DeviceType.BRACELET),
    WALK(R.drawable.bracelet_boardwalk, R.drawable.stone_sea, R.drawable.base_silver, R.string.boardwalk, DeviceType.BRACELET),
    FOTO(R.drawable.bracelet_photo_booth, R.drawable.stone_daydream, R.drawable.base_gold, R.string.photobooth, DeviceType.BRACELET),
    VOUS(R.drawable.bracelet_rendezvous, R.drawable.stone_wanderlust, R.drawable.base_gold, R.string.rendezvous, DeviceType.BRACELET),
    ROAD(R.drawable.bracelet_roadtrip, R.drawable.stone_stargaze, R.drawable.base_silver, R.string.roadtrip, DeviceType.BRACELET),
    LAKE(R.drawable.bracelet_lakeside, R.drawable.stone_sea, R.drawable.base_gold, R.string.lakeside, DeviceType.BRACELET),
    LOVE(R.drawable.bracelet_love, R.drawable.stone_daydream, R.drawable.base_rose, R.string.ringly_go, DeviceType.RINGLY_GO),
    GO01(R.drawable.bracelet_love, R.drawable.stone_daydream, R.drawable.base_rose, R.string.ringly_go, DeviceType.RINGLY_GO),
    GO02(R.drawable.bracelet_tbd, R.drawable.stone_stargaze, R.drawable.base_gunmetal, R.string.ringly_go, DeviceType.RINGLY_GO),
    ROSE(R.drawable.bracelet_rose_all_day, R.drawable.stone_daydream, R.drawable.base_rose, R.string.rose, DeviceType.BRACELET),
    JETS(R.drawable.bracelet_jet_set, R.drawable.stone_afterhours, R.drawable.base_gold, R.string.jet_set, DeviceType.BRACELET),
    RIDE(R.drawable.bracelet_joy_ride, R.drawable.stone_daydream, R.drawable.base_silver, R.string.joy_ride, DeviceType.BRACELET),
    BONV(R.drawable.bracelet_bon_voyage, R.drawable.stone_woods, R.drawable.base_gold, R.string.bon_voyage, DeviceType.BRACELET)
    ;

    @DrawableRes public final int photoId;
    @DrawableRes public final int stoneId;
    @DrawableRes public final int baseId;
    @StringRes public final int nameId;
    public final DeviceType deviceType;
    RingType(@DrawableRes final int photoId, @DrawableRes final int stoneId,
             @DrawableRes final int baseId, @StringRes final int nameId, final DeviceType deviceType) {
        this.photoId = photoId;
        this.stoneId = stoneId;
        this.baseId = baseId;
        this.nameId = nameId;
        this.deviceType = deviceType;
    }

    public int getBaseId() {
        if (baseId != -1) {
            return baseId;
        } else {
            return R.drawable.base_gold;
        }
    }

    public int getStoneId() {
        if (stoneId != -1) {
            return stoneId;
        } else {
            return R.drawable.stone_wanderlust;
        }

    }

    public enum DeviceType {
        BRACELET,
        RING,
        RINGLY_GO;
    };
}
