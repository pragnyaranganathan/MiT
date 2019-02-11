package com.epochconsulting.motoinventory.vehicletracker.util.pojo;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by pragnya on 24/7/17.
 */

public class ItemCodeTableDetails {
    @SerializedName("data")
    @Expose
    private List<ItemCodeData> itemCodeDataList = null;

    public List<ItemCodeData> getItemCodeDataList() {
        return itemCodeDataList;
    }

    public void setItemCodeDataList(List<ItemCodeData> itemCodeDatas) {
        this.itemCodeDataList = itemCodeDatas;
    }
}
