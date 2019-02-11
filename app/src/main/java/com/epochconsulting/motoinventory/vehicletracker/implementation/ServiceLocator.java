package com.epochconsulting.motoinventory.vehicletracker.implementation;

import android.content.Context;
import java.lang.reflect.Type;
import java.util.Map;

import com.android.volley.Response;

/**
 * Created by pragnya on 20/6/17.
 */

public interface ServiceLocator {
    void executeGetVolleyRequest(Context context, String url, Type typeObj, Map<String,String> params, Map<String, String> headers, Response.Listener listener, Response.ErrorListener errorListener);
    void executeGetStringRequest(Context context, String url,  Response.Listener listener, Response.ErrorListener errorListener);

}
