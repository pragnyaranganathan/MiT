package com.epochconsulting.motoinventory.vehicletracker.util;

/**
 * Created by pragnya on 9/6/17.
 */

public class Url {




    Url(){
        //do nothing here, default constructor
    }


    private static String SERVER_ADDRESS ;

    public static final String URL_SCHEME = "http";
    public static final String API = "api";
    public static final String API_RESOURCE = "resource";
    public static final String API_METHOD = "method";

    //added on 27Oct 2017 to reflect changes made in the app name (new app name is motoinventory_tracker, old one renfield)
    //begin change
    private static final String ERPNEXT_APPNAME = "motoinventory_tracker";
    private static final String DOT_STRING_IN_PATH_NAME = ".";
    private static final String API_PATH_NAME = ERPNEXT_APPNAME+DOT_STRING_IN_PATH_NAME+API+DOT_STRING_IN_PATH_NAME;
    public static final String COMPANY_NAME = "HSR Services";
    public static final String COMPANY_ABBR = "HSR";
    //end change


    public static final String LOGIN_URL = "login";
    public static final String LOGOUT_URL = "logout";

    //all the tables I am accessing in the backend
    public static final String WAREHOUSE_TABLE="Warehouse";
    public static final String SERIAL_NO = "Serial No";
    public static final String ITEM_TABLE = "Item";
    public static final String CUSTOMER_TABLE ="Customer" ;
    //added on 25th Oct to accommodate the requirement to tie the user to a particulr warehouse
    public static final String USER_TABLE="User";


   //all the api method calls to login/reset password, login stuff
    public static final String GET_LOGGED_USER = "frappe.auth.get_logged_user";
    public static final String RESENDPASSWORDURL = "frappe.core.doctype.user.user.reset_password";

    public static final String USER_WH_LOCATION = API_PATH_NAME+"getUserDetails";

    //all api method calls related to receive vehicle from RE

    public static final String VALIDATE_SERIAL_NO = API_PATH_NAME+"validate_serial_no";
    public static final String SUBMIT_STOCK_ENTRY = API_PATH_NAME+"submit_stock_entry";
    public static final String MAKE_STOCK_ENTRY = API_PATH_NAME+"make_stock_entry";
    public static final String SEND_MAIL = API_PATH_NAME+"send_IBNR_mail";
    public static final String MAKE_NEW_SERIAL_NO_ENTRY = API_PATH_NAME+"make_new_serial_no_entry";

    //all api method calls related to allocating a vehicle
    public static final String ALLOCATE_VEHICLE = API_PATH_NAME+"change_status";
    public static final String ALLOCATE_VEHICLE_NEW = API_PATH_NAME+"allocate_vehicle";
    public static final String ALLOCATE_VEHICLE_LOWMEDIUM =API_PATH_NAME+"change_status_low_medium" ;

    //all api method calls related to the loading and unloading of stock
    public static final String MAKE_MOVEMENT_STOCK_ENTRY = API_PATH_NAME+"make_movement_stock_entry";
    public static final String MAKE_UNLOADVEHICLE_STOCK_ENTRY = API_PATH_NAME+"make_unloadvehicle_stock_entry";

    public static final String MAKE_DELIVERY_NOTE = API_PATH_NAME+"make_delivery_note";
    public static final String SUBMIT_DELIVERY_NOTE = API_PATH_NAME+"submit_delivery_note";

   //all api method calls related to delivering the vehicle
    public static final String MAKE_SALES_INVOICE = API_PATH_NAME+ "make_sales_invoice";
    public static final String SUBMIT_SALES_INVOICE = API_PATH_NAME+"submit_sales_invoice";
    public static final String CANCEL_SALES_INVOICE = API_PATH_NAME+"cancel_sales_invoice";

    //default WH string..using this in ReceiveFromRE createStockEntry method, have to change this hard coded value, replace with a dummy string, let server throw an error
    public static final String DEFAULT_WH = "Finished Goods - "+COMPANY_ABBR;



    public static String getServerAddress() {
        return SERVER_ADDRESS;
    }

    public static void setServerAddress(String serverAddress) {
        SERVER_ADDRESS = serverAddress;
    }
}