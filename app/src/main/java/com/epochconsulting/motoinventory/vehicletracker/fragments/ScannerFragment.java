package com.epochconsulting.motoinventory.vehicletracker.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.epochconsulting.motoinventory.vehicletracker.R;
import com.epochconsulting.motoinventory.vehicletracker.activity.Home;
import com.epochconsulting.motoinventory.vehicletracker.activity.IntentIntegrator;
import com.epochconsulting.motoinventory.vehicletracker.activity.ReceiveVehicle;

/**
 * A simple {@link Fragment} subclass.
 */
public class ScannerFragment extends Fragment {


    public ScannerFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        IntentIntegrator mScanIntegrator = new IntentIntegrator(getActivity());
        mScanIntegrator.initiateScan();
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_scanner, container, false);
    }

}
