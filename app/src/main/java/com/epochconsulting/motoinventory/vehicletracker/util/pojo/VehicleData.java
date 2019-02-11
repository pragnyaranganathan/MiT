package com.epochconsulting.motoinventory.vehicletracker.util.pojo;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by pragnya on 20/6/17.
 */

public class VehicleData {
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("vehicle_status")
    @Expose
    private String vehicle_status;
    @SerializedName("item_code")
    @Expose
    private String item_code;
    @SerializedName("warehouse")
    @Expose
    private String warehouse;
    @SerializedName("delivery_required_on")
    @Expose
    private String delivery_required_on;
    @SerializedName("delivery_required_at")
    @Expose
    private String delivery_required_at;
    @SerializedName("booking_reference_number")
    @Expose
    private String booking_reference_number;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVehicle_status() {
        return vehicle_status;
    }

    public void setVehicle_status(String vehicle_status) {
        this.vehicle_status = vehicle_status;
    }

    public String getItem_code() {
        return item_code;
    }

    public void setItem_code(String item_code) {
        this.item_code = item_code;
    }

    public String getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(String warehouse) {
        this.warehouse = warehouse;
    }

    public String getDelivery_required_on() {
        return delivery_required_on;
    }

    public void setDelivery_required_on(String delivery_required_on) {
        this.delivery_required_on = delivery_required_on;
    }

    public String getDelivery_required_at() {
        return delivery_required_at;
    }

    public void setDelivery_required_at(String delivery_required_at) {
        this.delivery_required_at = delivery_required_at;
    }

    public String getBooking_reference_number() {
        return booking_reference_number;
    }

    public void setBooking_reference_number(String booking_reference_number) {
        this.booking_reference_number = booking_reference_number;
    }
}
