package com.epochconsulting.motoinventory.vehicletracker.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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
import com.epochconsulting.motoinventory.vehicletracker.util.pojo.SingletonTemp;
import com.epochconsulting.motoinventory.vehicletracker.util.pojo.VehicleDetails;
import com.epochconsulting.motoinventory.vehicletracker.view.common.BasicActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class GetVehicleStatus extends BasicActivity {

    private String vehicelNameFromScanner = null;
    ImageButton completeCycle;
    ImageButton barcodefooter;
    private String vehicleAtWH;
    private String vehicleStatus;
    GetVehicleStatus activity;
    static final String SERIALNOSTR = "serial_no";
    static final String USERIDSTR = "user_id";
    boolean firstScan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_vehicle_status);
        activity =  this;
        completeCycle = (ImageButton) findViewById(R.id.completecycle_footer);
        barcodefooter = (ImageButton) findViewById(R.id.barcode_footer);

        //Start: Added on 19th Feb 2018 to implement new interface
        firstScan = false;
        Bundle extras = getIntent().getExtras();
        if(extras!=null)
        {
            firstScan = extras.getBoolean("FirstScan");

        }


        /*vehicelNameFromScanner = null;
        Bundle extras = getIntent().getExtras();
        if(extras!=null)
        {
            vehicelNameFromScanner = extras.getString("VehicleScanned");

        }
        if(vehicelNameFromScanner!=null){
            getVehicleData();
        }
        else
        {
            showErrorDialogBox("Vehicle was not scanned properly. Please scan the vehicle again!");
        }*/
        //End: Added on 19th Feb 2018


        completeCycle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                callHomePage();
                finish();
            }
        });


        barcodefooter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //restarting the scanner

                initializeScanner();

            }
        });
        barcodefooter.performClick();


    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        //retrieve scan result
        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);

        if (scanningResult.getContents() != null) {
            //we have a result
            vehicelNameFromScanner = scanningResult.getContents();
            getVehicleData();

        }
        else{
            Toast toast = Toast.makeText(getApplicationContext(),
                    "No scan data received!", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    private void getVehicleData() {
        showProgress();
        String urlStr = Utility.getInstance().buildUrl(Url.API_RESOURCE, null, "Serial No", vehicelNameFromScanner);
        if (getApplicationContext() != null) {
            ServiceLocatorImpl.getInstance().executeGetVolleyRequest(getApplicationContext(), urlStr, VehicleDetails.class, null, getHeaders(), new Response.Listener<VehicleDetails>() {

                @Override
                public void onResponse(VehicleDetails response) {

                    if (response != null) {


                        SingletonTemp.getInstance().setSerialNumberedVehicle(response);
                        String requiredAtWH = SingletonTemp.getInstance().getSerialNumberedVehicle().getVehicleData().getDelivery_required_at();
                        String requiredOnDate = SingletonTemp.getInstance().getSerialNumberedVehicle().getVehicleData().getDelivery_required_on();
                        setVehicleStatus(SingletonTemp.getInstance().getSerialNumberedVehicle().getVehicleData().getVehicle_status());
                        String bookingrefnumber = SingletonTemp.getInstance().getSerialNumberedVehicle().getVehicleData().getBooking_reference_number();
                        //Start: Added on 13th Feb 2018...added warehouse details
                        setVehicleAtWH( SingletonTemp.getInstance().getSerialNumberedVehicle().getVehicleData().getWarehouse());
                        hideProgress();
                        buildDisplayString(requiredAtWH,requiredOnDate,bookingrefnumber);
                        //End - Change on 13th Feb 2018

                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                    hideProgress();
                    showErrorDialogBox("Something went wrong while fetching details of this vehicle. Make sure vehicle entry is present on ERPNext. Server Error:  " + error.toString());

                }
            });
        }
    }

    private void buildDisplayString(String requiredAtWH, String requiredOnDate,  String bookingrefnumber) {

        String deliverStatus;
        String allocateStatus;

        if(requiredAtWH == null||requiredAtWH.equalsIgnoreCase(""))
            requiredAtWH = getString(R.string.not_available_str);
        if(requiredOnDate == null||requiredOnDate.equalsIgnoreCase(""))
            requiredOnDate = getString(R.string.not_available_str);
        if(getVehicleStatus() == null)
            setVehicleStatus(getString(R.string.not_available_str));
        if(bookingrefnumber == null||bookingrefnumber.equalsIgnoreCase(""))
            bookingrefnumber = getString(R.string.not_available_str);
        //Start - Change on 13th Feb 2018
        if(getVehicleAtWH() == null)
            setVehicleAtWH(getString(R.string.not_available_str));
        //End - Change on 13th Feb2018
        if(getVehicleStatus().equalsIgnoreCase(getString(R.string.delivery_status_of_vehicle)))
            deliverStatus = getString(R.string.yes_str);
        else
            deliverStatus = getString(R.string.no_str);

        if(getVehicleStatus().equalsIgnoreCase(getString(R.string.vehicle_allocated_status_str)) )
            allocateStatus = getString(R.string.yes_str);
        else
            allocateStatus = getString(R.string.no_str);


        String displaystr = getString(R.string.is_vehicle_allocated_str)+allocateStatus+"\n"+getString(R.string.is_vehicle_delivered_str)+deliverStatus+"\n"+getString(R.string.BRN)+bookingrefnumber+"\n"+getString(R.string.delivery_required_at_str)+requiredAtWH+"\n"+getString(R.string.delivery_required_on_str)+requiredOnDate+"\n"+getString(R.string.vehicle_warehouse)+" "+getVehicleAtWH()+"\n";
        if( !getVehicleAtWH().equalsIgnoreCase(getCurrent_warehouse())){
            showTransferDialogBox(displaystr);
        }
        else
        {
            showInfoDialogBox(displaystr);
        }


    }

    public Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        String userId = prefs.getString(Constants.USER_ID, null);
        String sid = prefs.getString(Constants.SESSION_ID, null);
        headers.put(USERIDSTR, userId);
        headers.put("sid", sid);

        return headers;
    }
    private void showTransferDialogBox(String msg){
        LayoutInflater inflater = this.getLayoutInflater() ;
        View dialogiew = inflater.inflate(R.layout.dialog_info_content,null);
        TextView dialogtitle = (TextView) dialogiew.findViewById(R.id.dialog_title);
        dialogtitle.setText(vehicelNameFromScanner);
        TextView message = (TextView) dialogiew.findViewById(R.id.dialog_message) ;
        message.setText(msg+"\n"+getString(R.string.transferstr));
        showAlertDialog(null, null, true, getString(R.string.transfer_button_str), getString(R.string.cancel_str), dialogiew, new AlertDialogHandler() {
            @Override
            public void onPositiveButtonClicked() {
                //do the transfer here
                if (getVehicleAtWH().equalsIgnoreCase(getString(R.string.not_available_str)) && !getVehicleStatus().equalsIgnoreCase(getString(R.string.delivery_status_of_vehicle))) {
                    //the vehicle has not been received
                    receiveVehicle();
                } else {
                    if (getVehicleStatus().equalsIgnoreCase(getString(R.string.delivery_status_of_vehicle))) {
                        //cancel the sales invoice, reset the vehicle status to ABND
                        rollBackLastSalesInvoice(vehicelNameFromScanner);
                    } else {
                        //vehicle in the wrong warehouse
                        transferVehicle();
                    }
                }


            }

            @Override
            public void onNegativeButtonClicked() {
                dialogbox.dismiss();

            }
        });

    }
    private void showInfoDialogBox(String msg) {

        LayoutInflater inflater = this.getLayoutInflater() ;
        View dialogiew = inflater.inflate(R.layout.dialog_info_content,null);
        TextView dialogtitle = (TextView) dialogiew.findViewById(R.id.dialog_title);
        dialogtitle.setText(vehicelNameFromScanner);
        TextView message = (TextView) dialogiew.findViewById(R.id.dialog_message) ;
        message.setText(msg);

            this.showAlertDialog(null, null, false, getResources().getString(R.string.ok_str), null, dialogiew, new AlertDialogHandler() {
                @Override
                public void onPositiveButtonClicked() {

                    //start change on 13th Feb 2018

                        dialogbox.dismiss();
                    //End : change on 13th Feb 2018
                    }




                @Override
                public void onNegativeButtonClicked() {
                    //do nothing


                }

            });



    }

    private void rollBackLastSalesInvoice(final String serialNo) {

        showProgress();
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());

        Map<String, String> map = new HashMap<>();
        map.put("serial_no", serialNo);


        String urlStr = Utility.getInstance().buildUrl(Url.API_METHOD, map, Url.CANCEL_SALES_INVOICE);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, urlStr, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                String serverResponse = "";
                try{
                    JSONObject jsonObject = new JSONObject(response);
                    serverResponse = jsonObject.getString("message");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                hideProgress();
                if(serverResponse.contains(getResources().getString(R.string.success_string)))
                {
                    //have to reget the vehicle details from backend to do the transfer
                    String urlString = Utility.getInstance().buildUrl(Url.API_RESOURCE, null, "Serial No", vehicelNameFromScanner);
                    if (getApplicationContext() != null) {
                        ServiceLocatorImpl.getInstance().executeGetVolleyRequest(getApplicationContext(), urlString, VehicleDetails.class, null, getHeaders(), new Response.Listener<VehicleDetails>() {

                            @Override
                            public void onResponse(VehicleDetails response) {

                                if (response != null) {


                                    SingletonTemp.getInstance().setSerialNumberedVehicle(response);


                                    setVehicleStatus(SingletonTemp.getInstance().getSerialNumberedVehicle().getVehicleData().getVehicle_status());

                                    //Start: Added on 13th Feb 2018...added warehouse details
                                    setVehicleAtWH( SingletonTemp.getInstance().getSerialNumberedVehicle().getVehicleData().getWarehouse());

                                    if(!getCurrent_warehouse().equalsIgnoreCase(getVehicleAtWH())){

                                        //vehicle can be transferred only if source and destination are different WHS
                                        //ERPNext doesnt allow both to be same
                                        //the cancel_sales_invoice takes care of resetting the values in case of wrong WH
                                        //back to where the vehicle was last at(WH it was last at)

                                        transferVehicle();
                                    }
                                    else
                                    {
                                        toastMaker("Success: The vehicle is transferred to your current warehouse");
                                    }
                                    //End - Change on 13th Feb 2018

                                }
                            }
                        }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {

                                hideProgress();
                                showErrorDialogBox("Something went wrong while fetching details of this vehicle. Make sure vehicle entry is present on ERPNext. Server error is  " + error.toString());
                            }
                        });
                    }

                }
                else{
                    showErrorDialogBox(getResources().getString(R.string.could_not_rollback_prev_sales_invoice)+serverResponse);
                }




            }
        }//end of sucess response
                , new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                showErrorDialogBox(getResources().getString(R.string.could_not_rollback_prev_sales_invoice)+" Server Error: "+error.toString());

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
                headers.put(USERIDSTR, userId);
                headers.put("sid", sid);
                return headers;
            }

        };


        stringRequest.setRetryPolicy(new DefaultRetryPolicy(30 * 1000, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));


        requestQueue.add(stringRequest);
    }

    private void transferVehicle() {
        showProgress();
        //make movement stock entry
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());

        Map<String, String> map = new HashMap<>();
        map.put(SERIALNOSTR, vehicelNameFromScanner);
        map.put("source_warehouse", getVehicleAtWH());

        map.put("target_warehouse", getCurrent_warehouse());

        String urlStr = Utility.getInstance().buildUrl(Url.API_METHOD, map, Url.MAKE_MOVEMENT_STOCK_ENTRY);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, urlStr, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                String returnmsg = "";
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    returnmsg = jsonObject.getString("message");

                } catch (Exception e) {
                    Log.d("ERROR", e.toString());
                }
                hideProgress();
                if(returnmsg.contains(getResources().getString(R.string.success_string)))
                {
                    showSuccessDialogBox(returnmsg+getResources().getString(R.string.submit_transfer_stock_entry));
                }
                else
                {
                    showErrorDialogBox(returnmsg);
                }



            }
        }//end of sucess response
                , new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

               hideProgress();
                showErrorDialogBox("Could not transfer the vehicle. Server Error: "+error.toString());

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
                headers.put(USERIDSTR, userId);
                headers.put("sid", sid);
                return headers;
            }

        };


        stringRequest.setRetryPolicy(new DefaultRetryPolicy(30 * 1000, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));


        requestQueue.add(stringRequest);
    }

    private void submitStockEntry() {
        showProgress();
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        Map<String, String> map = new HashMap<>();
        map.put(SERIALNOSTR, vehicelNameFromScanner);
        String urlStr = Utility.getInstance().buildUrl(Url.API_METHOD, map, Url.SUBMIT_STOCK_ENTRY);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, urlStr, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                String returnmsg = "";
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    returnmsg = jsonObject.getString("message");
                } catch (Exception e) {
                    Log.d("ERROR", e.toString());

                }

                hideProgress();
               toastMaker(returnmsg);


            }
        }//end of sucess responseP
                , new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                hideProgress();
                toastMaker( getResources().getString(R.string.couldnotsubmitstockentry) + " " + vehicelNameFromScanner);


            }
        });

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(30 * 1000, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(stringRequest);
    }

    private void receiveVehicle() {
        //Start - Added on 11th Jan the if condition to handle null current warehouse
        if(getCurrent_warehouse()!=null) {
            showProgress();
            RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());

            Map<String, String> map = new HashMap<>();
            map.put(SERIALNOSTR, vehicelNameFromScanner);

            map.put("destination_warehouse", getCurrent_warehouse());


            String urlStr = Utility.getInstance().buildUrl(Url.API_METHOD, map, Url.MAKE_STOCK_ENTRY);
            StringRequest stringRequest = new StringRequest(Request.Method.GET, urlStr, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {

                    String returnmsg = "";
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        returnmsg = jsonObject.getString("message");
                    } catch (Exception e) {
                        Log.d("ERROR", e.toString());

                    }
                    hideProgress();
                    if(returnmsg.contains(getResources().getString(R.string.success_string)))
                    {
                        showSuccessDialogBox(returnmsg+getResources().getString(R.string.submit_transfer_stock_entry));
                    }

                    else
                    {
                        showErrorDialogBox(returnmsg);
                    }

                }
            }//end of sucess response
                    , new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                    hideProgress();
                    showErrorDialogBox("Could not transfer the vehicle. Server Error: "+error.toString());


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
                    headers.put(USERIDSTR, userId);
                    headers.put("sid", sid);
                    return headers;
                }

            };


            stringRequest.setRetryPolicy(new DefaultRetryPolicy(30 * 1000, 0,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));


            requestQueue.add(stringRequest);
        }

    }


    @Override
    public void onBackPressed(){
        //do nothing here
    }
    //start - added these lines of code on Jan 3rd 2018
    private void initializeScanner() {
        IntentIntegrator mScanIntegrator = new IntentIntegrator(this);
        mScanIntegrator.initiateScan();

    }

    @Override
    protected void autoLogout(){

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                //I am not doing anything..just killing time
            }
        });
        destroyTimer();
        logout();

        if (dialogbox != null) {
            dialogbox.dismiss();
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

                submitStockEntry();
                dialogbox.dismiss();

            }

            @Override
            public void onNegativeButtonClicked() {
                //do nothing


            }

        });


    }

    public String getVehicleAtWH() {
        return vehicleAtWH;
    }

    public void setVehicleAtWH(String vehicleAtWH) {
        this.vehicleAtWH = vehicleAtWH;
    }

    public String getVehicleStatus() {
        return vehicleStatus;
    }

    public void setVehicleStatus(String vehicleStatus) {
        this.vehicleStatus = vehicleStatus;
    }
}
