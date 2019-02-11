package com.epochconsulting.motoinventory.vehicletracker.util.pojo;

/**
 * Created by pragnya on 20/6/17.
 */

public class SingletonTemp{
    private static SingletonTemp singleton;
    private SerialNumberTableEntries serialNumberTableEntries = null;
    private VehicleDetails serialNumberedVehicle = null;
    private WareHouseDetails warehouse= null;
    private CustomerDetails customer=null;
    private ItemCodeTableDetails itemTableEntries;
    private UserDetails loggedInUser = null;
    private SingletonTemp(){
    }
    public static SingletonTemp getInstance(){
        if(singleton==null){
            singleton= new SingletonTemp();

        }
        return singleton;
    }


    public SerialNumberTableEntries getSerialNumberTableEntries() {
        return serialNumberTableEntries;
    }

    public void setSerialNumberTableEntries(SerialNumberTableEntries serialNumberTableEntries) {
        this.serialNumberTableEntries = serialNumberTableEntries;
    }

    public void setSerialNumberedVehicle(VehicleDetails response) {
        this.serialNumberedVehicle = response;
    }
    public VehicleDetails getSerialNumberedVehicle(){
        return serialNumberedVehicle;
    }

    public WareHouseDetails getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(WareHouseDetails warehouse) {
        this.warehouse = warehouse;
    }

    public CustomerDetails getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerDetails customer) {
        this.customer = customer;
    }

    public ItemCodeTableDetails getItemTableEntries() {
        return itemTableEntries;
    }

    public void setItemTableEntries(ItemCodeTableDetails itemTableEntries) {
        this.itemTableEntries = itemTableEntries;
    }

    public UserDetails getLoggedInUser() {
        return loggedInUser;
    }

    public void setLoggedInUser(UserDetails loggedInUser) {
        this.loggedInUser = loggedInUser;
    }
}
