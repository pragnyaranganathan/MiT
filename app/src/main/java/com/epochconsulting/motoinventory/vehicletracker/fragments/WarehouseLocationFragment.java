package com.epochconsulting.motoinventory.vehicletracker.fragments;



import android.content.Context;
import android.content.SharedPreferences;

import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.epochconsulting.motoinventory.vehicletracker.implementation.GpsTracker;
import com.epochconsulting.motoinventory.vehicletracker.implementation.ServiceLocatorImpl;
import com.epochconsulting.motoinventory.vehicletracker.R;
import com.epochconsulting.motoinventory.vehicletracker.activity.Home;
import com.epochconsulting.motoinventory.vehicletracker.util.Constants;
import com.epochconsulting.motoinventory.vehicletracker.util.Url;
import com.epochconsulting.motoinventory.vehicletracker.util.Utility;
import com.epochconsulting.motoinventory.vehicletracker.util.pojo.SingletonTemp;
import com.epochconsulting.motoinventory.vehicletracker.util.pojo.WareHouseDetails;
import com.epochconsulting.motoinventory.vehicletracker.util.pojo.WarehouseAddress;
import com.epochconsulting.motoinventory.vehicletracker.view.common.BaseFragment;
import com.google.gson.JsonArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.util.Map;

import static android.content.Context.MODE_PRIVATE;
import static com.epochconsulting.motoinventory.vehicletracker.view.common.BasicActivity.setCurrent_warehouse;

/**
 * A simple {@link Fragment} subclass.
 * 22nd Feb 2019 - Made changes to this file to take into account the changes made on ERPNext, Moved base warehouse
 * location to another Doctype and hence changes, made another chang
 */
public class WarehouseLocationFragment extends BaseFragment {

    Button getGPSButton;
    private Home activity;
    String IS_NEW_LOGIN;


    private String currentWarehouse = null;
    private Spinner warehouselist;
    private Spinner truckwarehouselist;
    private Button proceed;
    private int iCurrentSelection=0;
    private int iCurrentTruckSelection = 0;
    double latitude = 0.0;
    double longitude = 0.0;
    private String userBaseWHFromBackend = null;


    public WarehouseLocationFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View warehouselocationFragment = inflater.inflate(R.layout.fragment_warehouse_location, container, false);
        this.activity = (Home) this.getActivity();
        warehouselist = (Spinner) warehouselocationFragment.findViewById(R.id.spinner_whlist);
        warehouselist.setEnabled(false);

        truckwarehouselist = (Spinner) warehouselocationFragment.findViewById(R.id.spinner_truckwhlist);
        truckwarehouselist.setEnabled(false);

        getGPSButton = (Button) warehouselocationFragment.findViewById(R.id.whloc);
        getGPSButton.setEnabled(false);

        proceed =(Button) warehouselocationFragment.findViewById(R.id.nextbutton);
        proceed.setEnabled(false);
        proceed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(activity.getCurrent_warehouse() !=null) {

                    activity.toolbarUserWHLoc.setText(activity.getCurrent_warehouse());
                    boolean isaNewLogin = false;
                    SharedPreferences.Editor editor = activity.getSharedPreferences(IS_NEW_LOGIN, MODE_PRIVATE).edit();
                    editor.putBoolean("IsNewUser",isaNewLogin);

                    editor.apply();
                    editor.commit();
                    loadSelectModeFragment();
                }
            }
        });

        userBaseWHFromBackend = activity.getLoggedInUserBaseWHLoc();
        getGPSLocation();


            getGPSButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getGPSLocation();
                }
            });


        // Inflate the layout for this fragment
        return warehouselocationFragment;
    }

    public void getGPSLocation(){


        // create class object

        GpsTracker gpsData = new GpsTracker(this.activity, activity);


        // check if GPS enabled
        if(gpsData.canGetLocation()){

             latitude = gpsData.getLatitude();
             longitude = gpsData.getLongitude();
            //System.out.print to find the latitude and logitude
            System.out.println("The latitude of my current location is : "+latitude);
            System.out.println("The longitude of my current location is : "+ longitude);

            fetchWareHouseDetails();



        }else{

            // can't get location
            // GPS or Network is not enabled
            // Ask user to enable GPS/network in settings
            Toast.makeText(activity,"Please turn on the high accuracy GPS mode and enable WIFI in your settings and then click on the Get your warehouse location button again",Toast.LENGTH_LONG).show();

            gpsData.showSettingsAlert();
            getGPSButton.setEnabled(true);
            getGPSButton.setTextColor(getResources().getColor(R.color.cardview_light_background));
            getGPSButton.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));

        }

    }
    private void fetchWareHouseDetails() {

        //build th url appropriately
        Map<String,String> param =buildParameters();
        //build the url
        String url = Utility.getInstance().buildUrl(Url.API_RESOURCE,null,Url.WAREHOUSE_TABLE);

        if (this.activity.getApplicationContext() != null) {
            ServiceLocatorImpl.getInstance().executeGetVolleyRequest(this.activity.getApplicationContext(), url, WareHouseDetails.class, param, getHeaders(), new Response.Listener<WareHouseDetails>() {

                @Override
                public void onResponse(WareHouseDetails response) {

                    if (response != null) {

                        SingletonTemp.getInstance().setWarehouse(response);

                        WareHouseDetails warehouse = SingletonTemp.getInstance().getWarehouse();
                        List<WarehouseAddress> list = warehouse.getWarehouseData();

                        setMyTruckWarehouse(list); //added on 3Nov 2017 to get my truck warehouse

                        boolean foundmatch = compareGPSDataWithBackend(list);
                        setMyCurrentWarehouse(foundmatch, list);


                    }


                }


            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {


                    Toast.makeText(activity, "Server denied request with error" + error.toString(), Toast.LENGTH_SHORT).show();

                }
            });
        }


    }
    //added this line of code on 3rd Nov2017, to get truck warehouse
    private void setMyTruckWarehouse(List<WarehouseAddress> list) {


        List<String>  array = new ArrayList<>();
        array.add("Select Truck Warehouse");

        for(WarehouseAddress warehouse :list){

            if( warehouse.getIsTruck()!=null && warehouse.getIsTruck().equalsIgnoreCase("Yes")){
                array.add(warehouse.getName());
            }

        }
        if(array.size() == 2){
            //this means there is only 1 warehouse that is a truck
            activity.setTruckWH(array.get(1));
            return;
        }
        if(array.size() == 1) //this means that there is no wharehouse which is a truck that is found, add all WHs as options in the select list
        {
            Toast.makeText(activity,"There is no truck warehouse set on ERPNext, please choose your truck warehouse from the drop down list.",Toast.LENGTH_SHORT).show();
            for(WarehouseAddress warehouse: list)
            {
                array.add(warehouse.getName());
            }
        }


        //now set the adapter
        truckwarehouselist.setEnabled(true);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this.activity,android.R.layout.simple_spinner_item,array);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        truckwarehouselist.setAdapter(adapter);

        truckwarehouselist.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String wh = parent.getItemAtPosition(position).toString();
                if (position != 0 && position != iCurrentTruckSelection) {

                    activity.setTruckWH(wh); //warehouse selected from drop down list since user base loc not set on ERPNext

                }
                iCurrentTruckSelection = position;


            }


            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //do nothing here because we are just staying here in the same activity if nothing is selected

            }
        });

    }

    private void setMyCurrentWarehouse(boolean foundmatch, List<WarehouseAddress> list) {

        //added this on 26th Oct 2017, to get the name of the truck warehouse
        //set the truck wh also here

        if(!userBaseWHFromBackend.equalsIgnoreCase("")) {
            setCurrent_warehouse(userBaseWHFromBackend);

            proceed.setEnabled(true);
            proceed.setTextColor(getResources().getColor(R.color.cardview_light_background));
            proceed.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
            if (foundmatch) {

                //Added on Oct 25 2017, to tie a user down to a wh and flag him if his base wh doesnt match with gps coords
                if (currentWarehouse.equalsIgnoreCase(userBaseWHFromBackend)) {
                    Toast.makeText(activity, "Your GPS coordinates match with your base warehouse location. Your current warehouse is " + currentWarehouse, Toast.LENGTH_LONG).show();


                } else {
                    Toast.makeText(activity, "You do not seem to be at your base warehouse location. You are at " + currentWarehouse + " as per GPS data.You do not have access to this warehouse. Setting your warehouse to your default base warehouse   " + userBaseWHFromBackend + ". Click Next to continue or logout to exit.", Toast.LENGTH_LONG).show();

                }

            }
            else
            {
                Toast.makeText(activity, "Your GPS loction does not match any of the warehouses listed on ERPNext. Setting your warehouse to your default base warehouse " + userBaseWHFromBackend +". Click Next to continue or logout to exit.", Toast.LENGTH_LONG).show();
            }

        }
        else {
            if(foundmatch)
            {
                setCurrent_warehouse(currentWarehouse); //warehouse from GPS data since user base loc not set on ERPNext
                Toast.makeText(activity, "Your base warehouse location was not set on ERPNext. Your current warehouse according to GPS coordinates is " + currentWarehouse, Toast.LENGTH_LONG).show();
                proceed.setEnabled(true);
                proceed.setTextColor(getResources().getColor(R.color.cardview_light_background));
                proceed.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));

            }
            else{

                Toast.makeText(activity,"Your base warehouse location was not set on ERPNext. GPS couldn't find your current warehouse's location, please select your warehouse from the drop down list",Toast.LENGTH_LONG).show();
                warehouselist.setEnabled(true);
                showAlertDialogToFetchWarehouse(list);
                getGPSButton.setEnabled(false);

            }



        }

    }

    private void loadSelectModeFragment() {


        SelectModeFragment selectModeFragment = new SelectModeFragment();
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.home_content, selectModeFragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void showAlertDialogToFetchWarehouse(List<WarehouseAddress> warehouseAddressList) {


        List<String>  array = new ArrayList<>();
        array.add("Select Warehouse");
        for(WarehouseAddress warehouse:warehouseAddressList){
            array.add(warehouse.getName());

        }
        //now set the adapter

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this.activity,android.R.layout.simple_spinner_item,array);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        warehouselist.setAdapter(adapter);

        warehouselist.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String wh = parent.getItemAtPosition(position).toString();
                if (position != 0 && position != iCurrentSelection) {
                    currentWarehouse = wh;
                    setCurrent_warehouse(currentWarehouse); //warehouse selected from drop down list since user base loc not set on ERPNext
                    proceed.setEnabled(true);
                    proceed.setTextColor(getResources().getColor(R.color.cardview_light_background));
                    proceed.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
                }
                iCurrentSelection = position;


            }


            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //do nothing here because we are just staying here in the same activity if nothing is selected

            }
        });


    }


    private boolean compareGPSDataWithBackend(List<WarehouseAddress> list) {


       for(WarehouseAddress warehouseAddress:list)
       {
           //get the latitude
           double latitudeFromWH =  Double.parseDouble(warehouseAddress.getLatitude());
           double longitudeFromWH = Double.parseDouble(warehouseAddress.getLongitude());
          Log.d("TAG","The latitude from backend is "+latitudeFromWH);
           Log.d("TAG","The longitude from backend is "+longitudeFromWH);



           Location locationFromGPS = new Location("");
           locationFromGPS.setLatitude(latitude);
           locationFromGPS.setLongitude(longitude);

           Location locationFromWH = new Location("");
           locationFromWH.setLatitude(latitudeFromWH);
           locationFromWH.setLongitude(longitudeFromWH);

           float distanceInMeters = locationFromGPS.distanceTo(locationFromWH);
           Log.d("Distance","The distance between the GPS location and Warehouse in backend is "+distanceInMeters);
           if(distanceInMeters<=10.000){

               currentWarehouse = warehouseAddress.getName();
               return true;

           }



       }
       return false;

    }

    private Map<String,String> buildParameters() {

        //get all the warehouses

        String field5 ="name";
        String field6 ="latitude";
        String field7 = "longitude";
        String field8 = "is_truck";
        Map<String,String> params = new HashMap<>();

        JsonArray fieldsArray = new JsonArray();

        fieldsArray.add(field5);
        fieldsArray.add(field6);
        fieldsArray.add(field7);
        fieldsArray.add(field8);
        params.put("fields",fieldsArray.toString());

        //now print these parameters
        return params;



    }
    public Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        SharedPreferences prefs = this.activity.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        String userId = prefs.getString(Constants.USER_ID, null);

        String sid = prefs.getString(Constants.SESSION_ID, null);

        headers.put("user_id", userId);
        headers.put("sid", sid);

        return headers;
    }

}
