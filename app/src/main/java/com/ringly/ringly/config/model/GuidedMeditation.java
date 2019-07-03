package com.ringly.ringly.config.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Created by Monica on 5/29/2017.
 */

public class GuidedMeditation implements Serializable{

    public static final String GUIDED_MEDITATION_ID = "guidedMeditation";

    @SerializedName("title")
    public final String title;
    @SerializedName("subtitle")
    public final String subtitle;
    @SerializedName("description")
    public final String sessionDescription;
    @SerializedName("audio_file")
    public final String audioFile;
    @SerializedName("icon_image_1x")
    public final String iconImage1x;
    @SerializedName("icon_image_2x")
    public final String iconImage2x;
    @SerializedName("icon_image_3x")
    public final String iconImage3x;
    @SerializedName("length_seconds")
    public final int lengthSeconds;
    @SerializedName("author")
    public final GuidedAudioAuthor author;

//    public var iconUrl: URL {
//        if UIScreen.main.scale == 1 {
//            return self.iconImage1x
//        } else if UIScreen.main.scale == 2 {
//            return self.iconImage2x
//        } else if UIScreen.main.scale == 3 {
//            return self.iconImage3x
//        } else {
//            return self.iconImage2x
//        }
//    }

    public GuidedMeditation(String title, String subtitle, String sessionDescription, String audioFile,
                            String iconImage1x, String iconImage2x, String iconImage3x, int lengthSeconds, GuidedAudioAuthor author) {
        super();
        this.title = title;
        this.subtitle = subtitle;
        this.sessionDescription = sessionDescription;
        this.audioFile = audioFile;
        this.iconImage1x = iconImage1x;
        this.iconImage2x = iconImage2x;
        this.iconImage3x = iconImage3x;
        this.lengthSeconds = lengthSeconds;
        this.author = author;
    }

}
