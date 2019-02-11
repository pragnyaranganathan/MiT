package com.epochconsulting.motoinventory.vehicletracker.fragments;



import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.epochconsulting.motoinventory.vehicletracker.R;
import com.epochconsulting.motoinventory.vehicletracker.activity.AllocateVehicles;
import com.epochconsulting.motoinventory.vehicletracker.activity.DeliverVehicle;
import com.epochconsulting.motoinventory.vehicletracker.activity.GetVehicleStatus;
import com.epochconsulting.motoinventory.vehicletracker.activity.Home;

import com.epochconsulting.motoinventory.vehicletracker.activity.LoadVehicles;
import com.epochconsulting.motoinventory.vehicletracker.activity.ReceiveVehicle;
import com.epochconsulting.motoinventory.vehicletracker.activity.UnloadVehicle;
import com.epochconsulting.motoinventory.vehicletracker.view.common.BaseFragment;

import static com.epochconsulting.motoinventory.vehicletracker.view.common.BasicActivity.getTruckWH;

/**
 * A simple {@link Fragment} subclass.
 */
public class SelectModeFragment extends BaseFragment {

     Button receiveVehicleFromRE;
     Button allocateVehicle;
     Button receiveVehicleFromWH;
     Button loadVehicle;
     Button deliverVehicle;
     Button getvehicledetails;

    private Home activity;
    //Start: Added on 19th Feb to develop new interface
    OnTaskSelectedListener mCallback;
    // Container Activity must implement this interface
    public interface OnTaskSelectedListener {
         void onTaskSelected(View button);
    }
    //End: added on 19th Feb to develop new interface


    String mode;
    static final  String MODEOFOP = "MODE_OF_OP";



    public SelectModeFragment() {
        // Required empty public constructor
    }
    //start: Addded on 19th Feb to implement new interface
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (OnTaskSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnTaskSelectedListener");
        }
    }
    //End: Added on 19th Feb to implement new interface



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View selectModeFragment = inflater.inflate(R.layout.fragment_select_mode, container, false);

        this.activity = (Home) this.getActivity();
        this.context = activity.getApplicationContext();

        receiveVehicleFromRE = (Button)selectModeFragment.findViewById(R.id.nav_receivefrom_re);
        allocateVehicle = (Button) selectModeFragment.findViewById(R.id.nav_allocate);
        receiveVehicleFromWH = (Button) selectModeFragment.findViewById(R.id.nav_receivefrom_wh);
        loadVehicle = (Button) selectModeFragment.findViewById(R.id.nav_load);
        deliverVehicle = (Button) selectModeFragment.findViewById(R.id.nav_deliver);
        getvehicledetails = (Button) selectModeFragment.findViewById(R.id.nav_getvehicleinfo);

        //added on 25Oct 2017, to reflect the user's location in the toolbar
        String truckstring =getResources().getString(R.string.truckstring)+getTruckWH();
        String whstring = getResources().getString(R.string.whstring)+activity.getCurrent_warehouse();
        activity.toolbarUserWHLoc.setText(whstring);
        activity.toolbarTruckWH.setText(truckstring);


        receiveVehicleFromRE.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mode = "RECEIVE_FROM_RE";
               // mCallback.onTaskSelected(receiveVehicleFromRE);


                Intent startReceiveFromRE = new Intent(activity, ReceiveVehicle.class);
                startReceiveFromRE.putExtra("FirstScan",true);
                startActivity(startReceiveFromRE);


            }
        });
        allocateVehicle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               /* mode = "ALLOCATE_VEHICLE";
                mCallback.onTaskSelected(allocateVehicle);*/


               Intent startAllocateVehicle = new Intent(activity, AllocateVehicles.class);
                startAllocateVehicle.putExtra("FirstScan",true);
                startActivity(startAllocateVehicle);


            }
        });
        receiveVehicleFromWH.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                //mCallback.onTaskSelected(receiveVehicleFromWH);
                Intent startUnLoadVehicle = new Intent(activity, UnloadVehicle.class);
                startUnLoadVehicle.putExtra("FirstScan",true);
                startActivity(startUnLoadVehicle);



            }
        });
        loadVehicle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

               // mCallback.onTaskSelected(loadVehicle);

                Intent startLoadVehicle = new Intent(activity, LoadVehicles.class);
                startLoadVehicle.putExtra("FirstScan",true)  ;
                startActivity(startLoadVehicle);



            }
        });
        deliverVehicle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //mCallback.onTaskSelected(deliverVehicle);

                Intent startDeliverVehicle = new Intent(activity, DeliverVehicle.class);
                startDeliverVehicle.putExtra("FirstScan",true);
                startActivity(startDeliverVehicle);


            }
        });
        getvehicledetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //mCallback.onTaskSelected(getvehicledetails);

                Intent startVehicleDetails = new Intent(activity, GetVehicleStatus.class);
                startVehicleDetails.putExtra("FirstScan",true);
                startActivity(startVehicleDetails);


            }
        });


        // Inflate the layout for this fragment
        return selectModeFragment;
    }


}
