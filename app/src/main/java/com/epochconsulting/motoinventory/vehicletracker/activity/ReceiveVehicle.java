package com.epochconsulting.motoinventory.vehicletracker.activity;


import android.app.Activity;
import android.content.Context;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;

import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
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

import com.epochconsulting.motoinventory.vehicletracker.implementation.ServiceLocatorImpl;
import com.epochconsulting.motoinventory.vehicletracker.R;
import com.epochconsulting.motoinventory.vehicletracker.util.AlertDialogHandler;
import com.epochconsulting.motoinventory.vehicletracker.util.Constants;
import com.epochconsulting.motoinventory.vehicletracker.util.Url;
import com.epochconsulting.motoinventory.vehicletracker.util.Utility;

import com.epochconsulting.motoinventory.vehicletracker.util.pojo.ItemCodeData;
import com.epochconsulting.motoinventory.vehicletracker.util.pojo.ItemCodeTableDetails;
import com.epochconsulting.motoinventory.vehicletracker.util.pojo.SingletonTemp;

import com.epochconsulting.motoinventory.vehicletracker.view.common.BasicActivity;
import com.google.gson.JsonArray;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import java.util.List;
import java.util.Map;


public class ReceiveVehicle extends BasicActivity {


    String vehicelNameFromScanner = null;

    private String mEmailRecepients;

    ImageButton completeCycle;
    ImageButton barcodefooter;
    private boolean activityTimedOut = false;
    private boolean compleCycleButtonClicked = false;
    Activity activity;
    EditText input;
    static final String SERIALNOSTR = "serial_no";
    static final String USERID = "user_id";
    Spinner itemCodeList;
    String itemCode;
    int iCurrentSelection = 0;
    static final String SERVER_DENIED_REQUEST = "Server denied request ";
    boolean firstScan;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_vehicle);

        activityTimedOut = false;
        compleCycleButtonClicked = false;
        vehicelNameFromScanner = null;
        completeCycle = (ImageButton) findViewById(R.id.completecycle_footer);
        barcodefooter = (ImageButton) findViewById(R.id.barcode_footer);

        //Start: Added on 19th FEB 2018 to implement new interface
        //vehicelNameFromScanner = null;
        firstScan = false;
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            firstScan = extras.getBoolean("FirstScan");
        }
       /* if (vehicelNameFromScanner != null) {
            getBarCodeData();
        } else {
            showErrorDialogBox("Vehicle was not scanned properly. Please scan the vehicle again!");
        }*/
        //End; Added on 19th Feb 2018



        completeCycle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //setting this variable to true so that submit stock doesnt call the sendmail method everytime
                compleCycleButtonClicked = true;
                submitStockEntry();

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
        barcodefooter.performClick();


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

            getBarCodeData();
        } else {
            Toast toast = Toast.makeText(getApplicationContext(),
                    "No scan data received!", Toast.LENGTH_SHORT);
            toast.show();
        }
    }
    //end added the lines of code on Jan 3rd 2018

    private void getItemTable() {
        //build th url appropriately

        Map<String, String> param = buildParameters();
        //build the url
        String url = Utility.getInstance().buildUrl(Url.API_RESOURCE, null, Url.ITEM_TABLE);

        if (this.getApplicationContext() != null) {
            showProgress();
            ServiceLocatorImpl.getInstance().executeGetVolleyRequest(this.getApplicationContext(), url, ItemCodeTableDetails.class, param, getHeaders(), new Response.Listener<ItemCodeTableDetails>() {

                @Override
                public void onResponse(ItemCodeTableDetails response) {

                    hideProgress();
                    if (response != null) {

                        SingletonTemp.getInstance().setItemTableEntries(response);


                        List<ItemCodeData> list = SingletonTemp.getInstance().getItemTableEntries().getItemCodeDataList();
                        showAlertDialogToCreateSerialNoDocType(list);


                    }


                }


            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                    hideProgress();
                    showErrorDialogBox(getResources().getString(R.string.error_getting_item_table)+" "+SERVER_DENIED_REQUEST+" "+error.toString());

                }
            });
        }
    }

    private Map<String, String> buildParameters() {

        //get all the items
        //all the field parameters
        Map<String, String> params = new HashMap<>();
        String field1 = "item_code";


        JsonArray fieldsArray = new JsonArray();
        fieldsArray.add(field1);

        params.put("fields", fieldsArray.toString());
        //the conditions
        String tableName = "Item";
        String fieldName = "has_serial_no";
        String operand = "=";
        String fieldValue = "1";

        JsonArray oneFilter = new JsonArray();
        JsonArray filtersParam = new JsonArray();

        //add the conditions
        oneFilter.add(tableName);
        oneFilter.add(fieldName);
        oneFilter.add(operand);
        oneFilter.add(fieldValue);

        //parameter is actually a JsonArray of an array

        filtersParam.add(oneFilter);

        params.put("filters", filtersParam.toString());

        //Start Add: 27/08/18 to display more items in the returned list
        params.put("limit_page_length",String.valueOf(500));
        //End: Addded on 27/08/2018

        return params;


    }

    private void submitStockEntry() {
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());

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
                    Log.d("Error", e.toString());
                }
                toastMaker(returnmsg);



            }
        }//end of sucess response
                , new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                toastMaker(getResources().getString(R.string.couldnotsubmitstockentry) + " " + vehicelNameFromScanner+". Vehicle could not be received.");


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
                headers.put(USERID, userId);
                headers.put("sid", sid);
                return headers;
            }

        };


        stringRequest.setRetryPolicy(new DefaultRetryPolicy(30 * 1000, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));


        requestQueue.add(stringRequest);

        if (activityTimedOut) {
            activityTimedOut = false;
            logout();
            //resetting this value

        }
        if (compleCycleButtonClicked) {
            compleCycleButtonClicked = false; //resetting this once this is called
            showAlertDialogForSendingEmail();
        }
    }



    private void sendIBNRMail(String recepientparam) {
        //send the latest IBNR

        Map<String, String> params = new HashMap<>();

        params.put("emailadd", recepientparam);
        //now build the string url
        String urlStr = Utility.getInstance().buildUrl(Url.API_METHOD, params, Url.SEND_MAIL);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, urlStr, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                String returnmsg = "";

                try {
                    JSONObject jsonObject = new JSONObject(response);
                    returnmsg = jsonObject.getString("message");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
               toastMaker(returnmsg);


                callHomePage();
                finish();

            }
        }//end of sucess response
                , new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                toastMaker(getResources().getString(R.string.could_not_send_mail)+SERVER_DENIED_REQUEST + error.toString());
                callHomePage();
                finish();

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
                headers.put(USERID, userId);
                headers.put("sid", sid);
                return headers;
            }

        };


        stringRequest.setRetryPolicy(new DefaultRetryPolicy(30 * 1000, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        requestQueue.add(stringRequest);


    }


    private void showAlertDialogWhenFound() {
        LayoutInflater dialogViewinflator = this.getLayoutInflater();
        View dialogView = dialogViewinflator.inflate(R.layout.dialog_success_content,null);
        TextView title = (TextView) dialogView.findViewById(R.id.dialog_success_title) ;
        title.setText(getResources().getString(R.string.vehicle_found_ibnr_title));
        TextView message = (TextView) dialogView.findViewById(R.id.dialog_success_message) ;
        message.setText(getResources().getString(R.string.vehicle_found_invoiced_action_message));
        this.showAlertDialog(null,null , true, getResources().getString(R.string.yes_str), getResources().getString(R.string.no_str), dialogView, new AlertDialogHandler() {
            @Override
            public void onPositiveButtonClicked() {

                createStockEntry(vehicelNameFromScanner);

            }

            @Override
            public void onNegativeButtonClicked() {
                //do nothing
                dialogbox.dismiss();


            }

        });



    }

    private void showAlertDialogWhenNotFound() {
        //Start: Added this on 20th Feb 2018 to implement new interface
        LayoutInflater dialogViewinflator = this.getLayoutInflater();
        View dialogView = dialogViewinflator.inflate(R.layout.dialog_error_content,null);
        TextView title = (TextView) dialogView.findViewById(R.id.dialog_error_title) ;
        title.setText(getResources().getString(R.string.vehicle_notfound_ibnr_title));
        TextView message = (TextView) dialogView.findViewById(R.id.dialog_error_message) ;
        message.setText(getResources().getString(R.string.vehicle_notfound_invoiced_action_message));
        //End: Added this on 20th Feb 2018
        this.showAlertDialog(null, null, true, getResources().getString(R.string.yes_str), getResources().getString(R.string.no_str), dialogView, new AlertDialogHandler() {
            @Override
            public void onPositiveButtonClicked() {

                //I should add this serial no in the stock entry table
                //setting the flag to vehiclefound to true again, since I'm overriding

                createStockEntry(vehicelNameFromScanner);

            }

            @Override
            public void onNegativeButtonClicked() {

                dialogbox.dismiss(); //will be implemented
            }
        });



    }


    private void createStockEntry(final String serialNo) {

        //Start - Added on 11th Jan the if condition to handle null current warehouse
        if (getCurrent_warehouse() != null) {
            showProgress();
            final RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());

            Map<String, String> map = new HashMap<>();
            map.put(SERIALNOSTR, serialNo);

            map.put("destination_warehouse", getCurrent_warehouse());


            String urlStr = Utility.getInstance().buildUrl(Url.API_METHOD, map, Url.MAKE_STOCK_ENTRY);
            StringRequest stringRequest = new StringRequest(Request.Method.GET, urlStr, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {

                    hideProgress();
                    String returnmsg = "";

                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        returnmsg = jsonObject.getString("message");
                        if(returnmsg.contains(getResources().getString(R.string.success_string)))
                        {
                            String msgtouser = returnmsg+getResources().getString(R.string.receive_vehicle_success);
                            toastMaker(msgtouser);

                        }
                        else
                        {
                            String msgtouser = getResources().getString(R.string.receive_vehicle_failed)+returnmsg;
                            showErrorDialogBox(msgtouser);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }



                }
            }//end of sucess response
                    , new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                    hideProgress();
                    showErrorDialogBox(getResources().getString(R.string.receive_vehicle_failed)+" "+error.toString());

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
                    headers.put(USERID, userId);
                    headers.put("sid", sid);
                    return headers;
                }

            };


            stringRequest.setRetryPolicy(new DefaultRetryPolicy(30 * 1000, 0,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));


            requestQueue.add(stringRequest);
        } else//current_warehouse=null
        {
            playBeepSound("beepnegative.mp3");
            showErrorDialogBox("Oops! Something seems to be broken. The current warehouse is not set correctly. Cannot receive vehicle.  Click on OK to dismiss this alert. Please log in again to retry. ");
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


    public void getBarCodeData() {

        showProgress();

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        //create a map with the query parameters
        Map<String, String> mapObject = new HashMap<>();
        mapObject.put(SERIALNOSTR, vehicelNameFromScanner);


        String url = Utility.getInstance().buildUrl(Url.API_METHOD, mapObject, Url.VALIDATE_SERIAL_NO);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                hideProgress();
                int returnvalue = 0;
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    returnvalue = jsonObject.getInt("message");

                    if (returnvalue == 1) {
                        playBeepSound("beeppositive.mp3");
                        showAlertDialogWhenFound();

                    } else if (returnvalue == 2) {
                        playBeepSound("beepnegative.mp3");
                        showAlertDialogWhenNotFound();

                    } else if (returnvalue == -1) {
                        playBeepSound("beepnegative.mp3");

                        getItemTable();

                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }
        }//end of sucess responseP
                , new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                hideProgress();
                showErrorDialogBox(SERVER_DENIED_REQUEST+" "+error.toString());


            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
                String userId = prefs.getString(Constants.USER_ID, null);
                String sid = prefs.getString(Constants.SESSION_ID, null);
                headers.put(USERID, userId);
                headers.put("sid", sid);
                return headers;
            }

        };

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(30 * 1000, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(stringRequest);


    }

    private void showAlertDialogToCreateSerialNoDocType(List<ItemCodeData> itemList) {

        LayoutInflater inflater = this.getLayoutInflater() ;
        View dialogiew = inflater.inflate(R.layout.dialog_info_content,null);
        TextView title = (TextView) dialogiew.findViewById(R.id.dialog_title);
        title.setText(getResources().getString(R.string.serialNoNotFound_str));
        TextView message = (TextView) dialogiew.findViewById(R.id.dialog_message) ;
        message.setText(vehicelNameFromScanner + " " + getResources().getString(R.string.createserialno_str));
        LinearLayout layout = (LinearLayout) dialogiew.findViewById(R.id.dialog_layout);
        itemCodeList = new Spinner(this);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        itemCodeList.setLayoutParams(lp);
        layout.addView(itemCodeList);

        iCurrentSelection = 0;
        populateTheItemCodeListDropDown(itemList);

        showAlertDialog(null,null , false, getResources().getString(R.string.ok_str), getString(R.string.cancel_str), dialogiew, new AlertDialogHandler() {
            @Override
            public void onPositiveButtonClicked() {

                if (itemCode != null) {
                    createNewSerialNo();
                } else {

                    showErrorDialogBox("The item code is null, cannot add vehicle " + vehicelNameFromScanner + " to the backend");
                }


            }

            @Override
            public void onNegativeButtonClicked() {

                //do nothing on cancel
                dialogbox.dismiss();



            }
        });


    }

    @Override
    public void onBackPressed() {
        //do nothing here
    }

    private void createNewSerialNo() {

        showProgress();

        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());

        Map<String, String> map = new HashMap<>();
        map.put("serial_no", vehicelNameFromScanner);
        map.put("item_code", itemCode);


        String urlStr = Utility.getInstance().buildUrl(Url.API_METHOD, map, Url.MAKE_NEW_SERIAL_NO_ENTRY);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, urlStr, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                String returnmsg = "";

                try {
                    JSONObject jsonObject = new JSONObject(response);
                    returnmsg = jsonObject.getString("message");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                hideProgress();

                toastMaker(returnmsg);

                showAlertDialogWhenFound();

            }
        }//end of sucess response
                , new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                hideProgress();
                showErrorDialogBox(getResources().getString(R.string.error_creating_new_serial_no)+ " "+vehicelNameFromScanner+SERVER_DENIED_REQUEST+" "+error.getMessage());
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


    private void populateTheItemCodeListDropDown(List<ItemCodeData> itemList) {
        List<String> array = new ArrayList<>();
        array.add("Select Item Code");
        for (ItemCodeData item : itemList) {

            array.add(item.getItem_code());

        }
        //now set the adapter

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, array);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        itemCodeList.setAdapter(adapter);

        itemCodeList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = parent.getItemAtPosition(position).toString();

                if (iCurrentSelection != position && position != 0) {
                    //set the item_code here
                    itemCode = selectedItem;

                }
                iCurrentSelection = position;


            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //do nothing here because we are just staying here in the same activity if nothing is selected
                itemCode = null;
                Toast.makeText(ReceiveVehicle.this, "You have to select a valid Item code to add the vehicle " + vehicelNameFromScanner + " to the Serial No in the backend", Toast.LENGTH_SHORT).show();

            }
        });
    }

    public Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        String userId = prefs.getString(Constants.USER_ID, null);
        String sid = prefs.getString(Constants.SESSION_ID, null);
        headers.put(USERID, userId);
        headers.put("sid", sid);

        return headers;
    }


    @Override
    protected void autoLogout() {

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                //showprogress method call
            }
        });
        destroyTimer();

        activityTimedOut = true;
        submitStockEntry();

        if (dialogbox != null) {
            dialogbox.dismiss();
        }
    }

    private void showAlertDialogForSendingEmail() {

        LayoutInflater inflater = this.getLayoutInflater() ;
        View dialogiew = inflater.inflate(R.layout.dialog_info_content,null);
        TextView title = (TextView) dialogiew.findViewById(R.id.dialog_title);
        title.setText(getResources().getString(R.string.send_mail_title));
        TextView message = (TextView) dialogiew.findViewById(R.id.dialog_message) ;
        message.setText(getResources().getString(R.string.send_mail_action_message));
        LinearLayout layout = (LinearLayout) dialogiew.findViewById(R.id.dialog_layout);

        input = new EditText(this);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setHint(R.string.emailaddr_hint);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        input.setLayoutParams(lp);
        layout.addView(input);

        showAlertDialog(null,null , false, getResources().getString(R.string.send_str), getString(R.string.cancel_str), dialogiew, new AlertDialogHandler() {
            @Override
            public void onPositiveButtonClicked() {
                mEmailRecepients = input.getText().toString();
                sendIBNRMail(mEmailRecepients);


            }

            @Override
            public void onNegativeButtonClicked() {


                dialogbox.dismiss();
                callHomePage();
                finish();


            }
        });


    }


}





