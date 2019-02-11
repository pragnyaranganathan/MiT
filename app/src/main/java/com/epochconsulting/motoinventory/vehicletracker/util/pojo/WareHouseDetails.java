package com.epochconsulting.motoinventory.vehicletracker.util.pojo;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by pragnya on 6/7/17.
 */

public class WareHouseDetails {
    @SerializedName("data")
    @Expose
    private List<WarehouseAddress> warehouseData = null;

    public List<WarehouseAddress> getWarehouseData() {
        return warehouseData;
    }

    public void setWarehouseData(List<WarehouseAddress> warehouseData) {
        this.warehouseData = warehouseData;
    }
}
