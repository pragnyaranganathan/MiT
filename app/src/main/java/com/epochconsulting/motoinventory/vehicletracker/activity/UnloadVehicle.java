package com.epochconsulting.motoinventory.vehicletracker.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.epochconsulting.motoinventory.vehicletracker.R;
import com.epochconsulting.motoinventory.vehicletracker.implementation.ServiceLocatorImpl;
import com.epochconsulting.motoinventory.vehicletracker.util.AlertDialogHandler;
import com.epochconsulting.motoinventory.vehicletracker.util.Constants;
import com.epochconsulting.motoinventory.vehicletracker.util.Url;
import com.epochconsulting.motoinventory.vehicletracker.util.Utility;
import com.epochconsulting.motoinventory.vehicletracker.util.pojo.SerialNumberTableEntries;
import com.epochconsulting.motoinventory.vehicletracker.util.pojo.SingletonTemp;
import com.epochconsulting.motoinventory.vehicletracker.util.pojo.VehicleDetails;
import com.epochconsulting.motoinventory.vehicletracker.view.common.BasicActivity;
import com.google.gson.JsonArray;

import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import java.util.concurrent.TimeUnit;

public class UnloadVehicle extends BasicActivity {

    String vehicelNameFromScanner;
    String current_warehouse;

    boolean overrideStatus = false;
    boolean activityTimedOut = false;
    boolean compleCycleButtonClicked = false;

    private ImageButton completeCycle;
    ImageButton barcodefooter;
    private boolean unloadingCompleted;
    boolean firstScan;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unload_vehicle);

        current_warehouse = getCurrent_warehouse();
        completeCycle = (ImageButton) findViewById(R.id.completecycle_footer);

        unloadingCompleted = false;
        barcodefooter = (ImageButton) findViewById(R.id.barcode_footer);


        completeCycle.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
        completeCycle.setImageDrawable(getResources().getDrawable(R.drawable.ic_clear_red_100dp));


        completeCycle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                compleCycleButtonClicked = true;
                //first submit the stock entry
                showAlertDialogToChooseNextActivity();
                //resetting this the number of vehicles unloaded to zero again


            }
        });


        barcodefooter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!firstScan)
                    submitStockEntry();
                initializeScanner();


            }
        });
        //start: Added on 19th Feb 2018 for the new interface
        firstScan = false;
        Bundle extras = getIntent().getExtras();
        if(extras!=null)
        {
            firstScan = extras.getBoolean("FirstScan");
        }
       /* vehicelNameFromScanner = null;
        Bundle extras = getIntent().getExtras();
        if(extras!=null)
        {
            vehicelNameFromScanner = extras.getString("VehicleScanned");
        }
        if(vehicelNameFromScanner!=null)
        {

            unloadVehicle();

        }
        else {
            showErrorDialogBox("Vehicle was not scanned properly. Please scan the vehicle again!");
        }*/
        //End: Added on 19th Feb 2018 for the new interface
        barcodefooter.performClick();
        findTotalnumberOfVehiclesToUnloadAtCurrentWarehouse();


    }

    @Override
    public void onBackPressed() {
        //do nothing here
    }

    //start - added these lines of code on Jan 3rd 2018
    private void initializeScanner() {
        firstScan = false;
        IntentIntegrator mScanIntegrator = new IntentIntegrator(this);
        mScanIntegrator.initiateScan();

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        //retrieve scan result
        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);

        if (scanningResult.getContents() != null) {
            //we have a result
            vehicelNameFromScanner = scanningResult.getContents();
            findTotalnumberOfVehiclesToUnloadAtCurrentWarehouse();
            unloadVehicle();

        } else {
            Toast toast = Toast.makeText(getApplicationContext(),
                    "No scan data received!", Toast.LENGTH_SHORT);
            toast.show();
        }
    }
    //end added the lines of code on Jan 3rd 2018

    private void showAlertDialogToChooseNextActivity() {

        String title = "";
        String msg = "";

        if (unloadingCompleted) {
            title = getResources().getString(R.string.allvehiclesunloaded);
            msg = getResources().getString(R.string.selectloadvehiclesorhomepage_str);

        } else {
            title = getResources().getString(R.string.allvehiclesnotunloaded);
            msg = getResources().getString(R.string.selectloadvehiclesorhomepage_str_incomplete);

        }
        LayoutInflater inflater = this.getLayoutInflater() ;
        View dialogiew = inflater.inflate(R.layout.dialog_info_content,null);
        TextView dialogtitle = (TextView) dialogiew.findViewById(R.id.dialog_title);
        dialogtitle.setText(title);
        TextView message = (TextView) dialogiew.findViewById(R.id.dialog_message) ;
        message.setText(msg);
        showAlertDialog(null, null, false, getResources().getString(R.string.load_vehicle_str), getResources().getString(
                R.string.home_page_str), dialogiew, new AlertDialogHandler() {
            @Override
            public void onPositiveButtonClicked() {

                submitStockEntry();

                callLoadVehiclePage();


            }

            @Override
            public void onNegativeButtonClicked() {


                submitStockEntry();
                callHomePage();
                finish();


            }
        });


    }

    private void callLoadVehiclePage() {
        Intent intent = new Intent(getApplicationContext(), LoadVehicles.class);
        intent.putExtra("FirstScan",true);
        startActivity(intent);
        finish();
    }


    private void unloadVehicle()

    {
        current_warehouse = getCurrent_warehouse();

        if (vehicelNameFromScanner != null && current_warehouse != null) {

            fetchVehicleDetailsFromBackend();


        } else {
            //throw an error dialog box saying that the current warehouse is null or vehiclenamefromscanner is null
            playBeepSound("beepnegative.mp3");
            handleErrors();

        }

    }


    private void handleErrors() {
        if (current_warehouse == null) {
            showErrorDialogBox("The current warehouse is not specified. Please click on OK to dismiss the Alert. Finish the task to retry.");
        }
        if (vehicelNameFromScanner == null) {
            showErrorDialogBox("The scanned vehicle code is not specified. Please click on OK to dismiss this Alert. Scan another vehicle to continue.");
        }

    }

    private void showErrorDialogBox(String str) {

        LayoutInflater dialogViewinflator = this.getLayoutInflater();
        View dialogView = dialogViewinflator.inflate(R.layout.dialog_error_content,null);
        TextView title = (TextView) dialogView.findViewById(R.id.dialog_error_title) ;
        title.setText(getResources().getString(R.string.error_string));
        TextView message = (TextView) dialogView.findViewById(R.id.dialog_error_message) ;
        message.setText(str);
        this.showAlertDialog(null, null, false, getResources().getString(R.string.ok_str), null, dialogView, new AlertDialogHandler() {
            @Override
            public void onPositiveButtonClicked() {


                dialogbox.dismiss();

            }

            @Override
            public void onNegativeButtonClicked() {
                //do nothing


            }

        });

    }

    private void doUnLoadTask(String requiredatWH, long daysDiff) {

        if (requiredatWH != null) {

            if (current_warehouse.equalsIgnoreCase(requiredatWH) && daysDiff < Constants.NUMBER_OF_DAYS_TO_TRANSFER_VEHICLE && daysDiff >= 0) {


                playBeepSound("beeppositive.mp3");
                showUnLoadVehicleAlert();


            } else {
                String errormsg = "";
                if (!current_warehouse.equalsIgnoreCase(requiredatWH)) {
                    errormsg = errormsg + getResources().getString(R.string.donotunloadvehicle_str);
                } else {
                    errormsg = errormsg + getResources().getString(R.string.donotunloadvehicletoday_str);

                }

                playBeepSound("beepnegative.mp3");
                showDoNotUnLoadVehicleAlert(errormsg);


            }
        } else {
            playBeepSound("beepnegative.mp3");
            showErrorDialogBox("The delivery_required_at field is not specified on ERPNext for vehicle with serial no " + vehicelNameFromScanner + ". Click on OK to dismiss this alert. Scan another vehicle to continue or enter details on ERPNext and retry.");
        }
    }

    private void findTotalnumberOfVehiclesToUnloadAtCurrentWarehouse() {

        //Added this code on 23rd Oct 2017 to enable/disable the complete cycle button
        //First get the vehicles from the backend that need to be unloaded from source WH: truck into this WH only(delivery_req_at field)
        //che
        String url = Utility.getInstance().buildUrl(Url.API_RESOURCE, null, Url.SERIAL_NO);
        Map<String, String> params = buildMoveSheetParams();
        //now build the volleyrequest
        if (getApplicationContext() != null) {
            ServiceLocatorImpl.getInstance().executeGetVolleyRequest(getApplicationContext(), url, SerialNumberTableEntries.class, params, getHeaders(), new Response.Listener<SerialNumberTableEntries>() {

                @Override
                public void onResponse(SerialNumberTableEntries response) {

                    if (response != null) {

                        SingletonTemp.getInstance().setSerialNumberTableEntries(response);

                        int numberOfVehiclesToUnload = SingletonTemp.getInstance().getSerialNumberTableEntries().getSerialNoTableList().size();
                        //System.out.println("Number of vehicles currently on truck and to unload at "+current_warehouse+" is: "+numberOfVehiclesToUnload);

                        if (numberOfVehiclesToUnload <= 1)
                            enableCompleteCycleButton();


                    }

                }


            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {


                    showErrorDialogBox("Something went wrong while fetching the number of vehicles to unload at your current location. Server Error: "+error.toString());
                }
            });
        }

    }

    private Map<String, String> buildMoveSheetParams() {
        Map<String, String> param = new HashMap<>();

        String querytable = "Serial No";
        String queryField1 = "warehouse";
        String queryOperator1 = "=";
        //String queryOperand1 = getResources().getString(R.string.truckname) ;//"Truck - RE"; //this is a hardcoded value
        //Added this instead of harcoding the value on 26th Oct
        String queryOperand1 = getTruckWH();


        String queryField2 = "delivery_required_at";
        String queryOperator2 = "=";
        String queryOperand2 = current_warehouse;

        String queryField3 = "vehicle_status";
        String queryOperator3 = "!=";
        String queryOperand3 = getResources().getString(R.string.delivery_status_of_vehicle); //vehicle_status!=Delivered

        JsonArray fieldsArray = new JsonArray();
        JsonArray filtersQuery1 = new JsonArray();
        JsonArray filtersQuery2 = new JsonArray();
        JsonArray filtersQuery3 = new JsonArray();
        JsonArray filtersArray = new JsonArray();
        //The first query for th filters condition

        filtersQuery1.add(querytable);
        filtersQuery1.add(queryField1);
        filtersQuery1.add(queryOperator1);
        filtersQuery1.add(queryOperand1);


        //the second query for the filters condition

        filtersQuery2.add(querytable);
        filtersQuery2.add(queryField2);
        filtersQuery2.add(queryOperator2);
        filtersQuery2.add(queryOperand2);


        filtersQuery3.add(querytable);
        filtersQuery3.add(queryField3);
        filtersQuery3.add(queryOperator3);
        filtersQuery3.add(queryOperand3);


        //now construct the filters param
        filtersArray.add(filtersQuery1);
        filtersArray.add(filtersQuery2);
        filtersArray.add(filtersQuery3);


        //All the fields Parameters
        fieldsArray.add("name");
        fieldsArray.add("vehicle_status");
        fieldsArray.add("item_code");
        fieldsArray.add("warehouse");
        fieldsArray.add("delivery_required_on");
        fieldsArray.add("delivery_required_at");


        //finally building the parameters
        param.put("filters", filtersArray.toString());
        param.put("fields", fieldsArray.toString());

        return param;


    }


    private void fetchVehicleDetailsFromBackend() {
        showProgress();
        String urlStr = Utility.getInstance().buildUrl(Url.API_RESOURCE, null, "Serial No", vehicelNameFromScanner);
        if (getApplicationContext() != null) {
            ServiceLocatorImpl.getInstance().executeGetVolleyRequest(getApplicationContext(), urlStr, VehicleDetails.class, null, getHeaders(), new Response.Listener<VehicleDetails>() {

                @Override
                public void onResponse(VehicleDetails response) {

                    hideProgress();
                    if (response != null) {


                        SingletonTemp.getInstance().setSerialNumberedVehicle(response);

                        String requiredAtWH = SingletonTemp.getInstance().getSerialNumberedVehicle().getVehicleData().getDelivery_required_at();
                        String requiredOnDate = SingletonTemp.getInstance().getSerialNumberedVehicle().getVehicleData().getDelivery_required_on();
                        String vehicleStatus = SingletonTemp.getInstance().getSerialNumberedVehicle().getVehicleData().getVehicle_status();
                        long daysDiff;
                        if (vehicleStatus == null) {
                            playBeepSound("beepnegative.mp3");
                            showErrorDialogBox("The vehicle status field is not specified on ERPNext. Please specify the value on ERPNext and retry unloading the vehicle.");

                        } else {
                            if (requiredAtWH == null) {
                                requiredAtWH = getResources().getString(R.string.dummywarehouse);
                            }
                            if (requiredOnDate == null) {
                                daysDiff = Constants.DUMMY_NUMBER_OF_DAYS_DIFF;
                            } else {

                                daysDiff = findDifferenceInNumberOfDays(requiredOnDate);
                            }
                            if (!vehicleStatus.equalsIgnoreCase(getResources().getString(R.string.delivery_status_of_vehicle))) {
                                doUnLoadTask(requiredAtWH, daysDiff);


                            } else {

                                playBeepSound("beepnegative.mp3");
                                showErrorDialogBox("The vehicle with serial no " + vehicelNameFromScanner + "  is already Delivered . Click on OK to dismiss this alert. Scan another vehicle to continue or click on Complete Task to go back to Home page.");

                            }
                        }

                    }

                }


            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                  hideProgress();
                   showErrorDialogBox("Something went wrong while fetching details of the vehicle from Erpnext in order to unload. Make sure vehicle entry is present on ERPNext. Server error is "+ error.toString());
                }
            });
        }
    }

    private void enableCompleteCycleButton() {


        unloadingCompleted = true;
        completeCycle.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
        completeCycle.setImageDrawable(getResources().getDrawable(R.drawable.ic_done_green_100dp));


    }

    private long findDifferenceInNumberOfDays(String requiredOnDate) {
        String pattern = "yyyy-MM-dd";
        long daysDiff = -1;
        Date date = null;
        try {
            date = new SimpleDateFormat(pattern).parse(requiredOnDate);
            Calendar testCalendar = Calendar.getInstance();
            testCalendar.setTime(date);


            long msDiff = testCalendar.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();

            daysDiff = TimeUnit.MILLISECONDS.toDays(msDiff);
        } catch (ParseException e) {
            //do nothing
        }


        return daysDiff;
    }

    private void showUnLoadVehicleAlert() {
        LayoutInflater dialogViewinflator = this.getLayoutInflater();
        View dialogView = dialogViewinflator.inflate(R.layout.dialog_success_content,null);
        TextView title = (TextView) dialogView.findViewById(R.id.dialog_success_title) ;
        title.setText(getResources().getString(R.string.success_string));
        TextView message = (TextView) dialogView.findViewById(R.id.dialog_success_message) ;
        message.setText(getResources().getString(R.string.unloadvehicle_str));
        UnloadVehicle.this.showAlertDialog(null,null , true, getResources().getString(R.string.yes_str), getResources().getString(R.string.no_str), dialogView, new AlertDialogHandler() {
            @Override
            public void onPositiveButtonClicked() {


                createStockEntry(vehicelNameFromScanner, current_warehouse);


            }

            @Override
            public void onNegativeButtonClicked() {
                dialogbox.dismiss();


            }

        });



    }

    private void showDoNotUnLoadVehicleAlert(String errormsg) {

        //Start: Added this on 20th Feb 2018 to implement new interface
        LayoutInflater dialogViewinflator = this.getLayoutInflater();
        View dialogView = dialogViewinflator.inflate(R.layout.dialog_error_content,null);
        TextView title = (TextView) dialogView.findViewById(R.id.dialog_error_title) ;
        title.setText(getResources().getString(R.string.error_string));
        TextView message = (TextView) dialogView.findViewById(R.id.dialog_error_message) ;
        message.setText(errormsg);
        UnloadVehicle.this.showAlertDialog(null, null, true, getResources().getString(R.string.yes_str), getResources().getString(R.string.no_str), dialogView, new AlertDialogHandler() {
            @Override
            public void onPositiveButtonClicked() {

                //I should add this serial no in the stock entry table
                //setting the flag to vehiclefound to true again, since I'm overriding

                createStockEntry(vehicelNameFromScanner, current_warehouse);

            }

            @Override
            public void onNegativeButtonClicked() {

                //will be implemented
                dialogbox.dismiss();
            }
        });


    }

    private void createStockEntry(final String serialNo, final String currentWarehouse) {
        showProgress();
        final RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());

        Map<String, String> map = new HashMap<>();
        map.put("serial_no", serialNo);
        map.put("destination_warehouse", currentWarehouse);
        map.put("source_warehouse", getTruckWH());

        String urlStr = Utility.getInstance().buildUrl(Url.API_METHOD, map, Url.MAKE_UNLOADVEHICLE_STOCK_ENTRY);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, urlStr, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                hideProgress();
                String returnmsg = "";
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    returnmsg = jsonObject.getString("message");

                } catch (Exception e) {
                    //do nothing
                }
                if(returnmsg.contains(getResources().getString(R.string.success_string)))
                {
                    toastMaker(returnmsg+". Vehicle is unloaded from truck!");
                }
                else{
                    showErrorDialogBox(returnmsg);
                }



            }
        }//end of sucess response
                , new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                hideProgress();
                showErrorDialogBox("Could not make a stock entry for unloading the vehicle " + vehicelNameFromScanner + ". Server Error: " + error.toString());

            }
        })
                //end of error response and stringRequest params
        {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
                String userId = prefs.getString(Constants.USER_ID, null);
                String sid = prefs.getString(Constants.SESSION_ID, null);
                headers.put("user_id", userId);
                headers.put("sid", sid);
                return headers;
            }

        };


        stringRequest.setRetryPolicy(new DefaultRetryPolicy(30 * 1000, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));


        requestQueue.add(stringRequest);
    }

    @Override
    protected void autoLogout() {

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                //do nothing here, just run the method for 5 secs

            }
        });
        destroyTimer();

        activityTimedOut = true;
        submitStockEntry();

        if (dialogbox != null) {
            dialogbox.dismiss();
        }

    }

    private void submitStockEntry() {

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        Map<String, String> map = new HashMap<>();
        map.put("serial_no", vehicelNameFromScanner);
        String urlStr = Utility.getInstance().buildUrl(Url.API_METHOD, map, Url.SUBMIT_STOCK_ENTRY);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, urlStr, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                String returnmsg = "";
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    returnmsg = jsonObject.getString("message");

                } catch (Exception e) {
                    //do nothing
                }


                toastMaker(returnmsg);


            }
        }//end of sucess responseP
                , new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                toastMaker( getResources().getString(R.string.couldnotsubmitstockentry) + " " + vehicelNameFromScanner+". Vehicle could not be unloaded/moved!");


            }
        });

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(30 * 1000, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(stringRequest);
        if (activityTimedOut) {
            activityTimedOut = false;
            logout();
            //resetting this value

        }

    }

    public Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        String userId = prefs.getString(Constants.USER_ID, null);
        String sid = prefs.getString(Constants.SESSION_ID, null);
        headers.put("user_id", userId);
        headers.put("sid", sid);

        return headers;
    }
    private void showSuccessDialogBox(String str) {

        LayoutInflater dialogViewinflator = this.getLayoutInflater();
        View dialogView = dialogViewinflator.inflate(R.layout.dialog_success_content,null);
        TextView title = (TextView) dialogView.findViewById(R.id.dialog_success_title) ;
        title.setText(getResources().getString(R.string.success_string));
        TextView message = (TextView) dialogView.findViewById(R.id.dialog_success_message) ;
        message.setText(str);
        this.showAlertDialog(null, null, false, getResources().getString(R.string.ok_str), null, dialogView, new AlertDialogHandler() {
            @Override
            public void onPositiveButtonClicked() {


                dialogbox.dismiss();

            }

            @Override
            public void onNegativeButtonClicked() {
                //do nothing


            }

        });


    }

}
