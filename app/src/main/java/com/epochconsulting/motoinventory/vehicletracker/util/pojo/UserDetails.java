package com.epochconsulting.motoinventory.vehicletracker.util.pojo;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by pragnya on 25/10/17.
 */

public class UserDetails {
    @SerializedName("message")
    @Expose
    private UserData userData ;

    public UserData getUserData() {
        return userData;
    }

    public void setUserData(UserData userData) {
        this.userData = userData;
    }
}
