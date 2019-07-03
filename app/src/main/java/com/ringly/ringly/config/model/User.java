package com.ringly.ringly.config.model;

import com.google.gson.annotations.SerializedName;

public class User {
    public final String email;
    public final int id;

    @SerializedName(value="first_name")
    public final String firstName;

    @SerializedName(value="last_name")
    public final String lastName;

    @SerializedName(value="receive_updates")
    public final boolean receiveUpdates;

    public User(String email, int id, String firstName, String lastName, boolean receiveUpdates) {
        this.email = email;
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.receiveUpdates = receiveUpdates;
    }
}
