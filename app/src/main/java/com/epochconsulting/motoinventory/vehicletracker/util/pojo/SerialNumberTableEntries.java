package com.epochconsulting.motoinventory.vehicletracker.util.pojo;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by pragnya on 20/6/17.
 */

public class SerialNumberTableEntries {

    @SerializedName("data")
    @Expose
    private List<VehicleData> serialNoTableList = null;

    public List<VehicleData> getSerialNoTableList() {
        return serialNoTableList;
    }

    public void setSerialNoTableList(List<VehicleData> serialNoTableList) {
        this.serialNoTableList = serialNoTableList;
    }
}
