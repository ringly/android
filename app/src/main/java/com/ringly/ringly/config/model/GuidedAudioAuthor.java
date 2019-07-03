package com.ringly.ringly.config.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Created by Monica on 5/29/2017.
 */

public class GuidedAudioAuthor implements Serializable {

    @SerializedName("name")
    public final String name;
    @SerializedName("image")
    public final String image;


    public GuidedAudioAuthor(String name, String image) {
        this.name = name;
        this.image = image;
    }
}
