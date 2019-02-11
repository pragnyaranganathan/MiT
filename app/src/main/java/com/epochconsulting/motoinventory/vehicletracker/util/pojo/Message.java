package com.epochconsulting.motoinventory.vehicletracker.util.pojo;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by pragnya on 13/12/18.
 */

public class Message {
    @SerializedName("retval")
    @Expose
    private Integer retval;
    @SerializedName("level")
    @Expose
    private Integer level;

    public Integer getRetval() {
        return retval;
    }

    public void setRetval(Integer retval) {
        this.retval = retval;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

}
