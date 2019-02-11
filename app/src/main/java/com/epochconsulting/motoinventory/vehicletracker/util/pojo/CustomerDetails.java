package com.epochconsulting.motoinventory.vehicletracker.util.pojo;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by pragnya on 20/7/17.
 */

public class CustomerDetails {
    @SerializedName("data")
    @Expose
    private List<CustomerData> customerDataList = null;

    public List<CustomerData> getCustomerDataList() {
        return customerDataList;
    }

    public void setCustomerDataList(List<CustomerData> customerDataList) {
        this.customerDataList = customerDataList;
    }
}
