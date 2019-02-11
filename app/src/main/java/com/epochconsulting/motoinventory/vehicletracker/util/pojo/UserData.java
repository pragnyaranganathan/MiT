package com.epochconsulting.motoinventory.vehicletracker.util.pojo;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by pragnya on 25/10/17.
 */

public class UserData {
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("full_name")
    @Expose
    private String full_name;
    @SerializedName("base_warehouse_location")
    @Expose
    private String base_warehouse_location;

    public String getBase_warehouse_location() {
        return base_warehouse_location;
    }

    public void setBase_warehouse_location(String base_warehouse_location) {
        this.base_warehouse_location = base_warehouse_location;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFull_name() {
        return full_name;
    }

    public void setFull_name(String full_name) {
        this.full_name = full_name;
    }
}
