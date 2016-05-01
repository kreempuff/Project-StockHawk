package com.sam_chordas.android.stockhawk.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.sam_chordas.android.stockhawk.ui.StockDetailActivity;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by kare2436 on 4/30/16.
 */
public class StockDetailService extends IntentService {

    private static final String TAG = "StockDetailService";
    private OkHttpClient client = new OkHttpClient();
    private Context mContext;

    String fetchData(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            return null;
        } else {
            return response.body().string();
        }
    }

    public StockDetailService() {
        super(StockIntentService.class.getName());
    }

    public StockDetailService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        String response = null;
        if (extras == null) return;
        Uri.Builder urlStringBuilder = new Uri.Builder().scheme("https");
        urlStringBuilder
                .authority("query.yahooapis.com")
                .appendPath("v1")
                .appendPath("public")
                .appendPath("yql");
        String symbol = extras.getString("symbol");
        String startDate = extras.getString("start");
        String endDate = extras.getString("end");
        String yahooQuery = String.format("select * from yahoo.finance.historicaldata where symbol = " +
                "\"%s\" and startDate = \"%s\" and endDate = \"%s\"", symbol, startDate, endDate);
        Uri finalUri = null;
        finalUri = urlStringBuilder
                .appendQueryParameter("q", yahooQuery)
                .appendQueryParameter("format", "json")
                .appendQueryParameter("diagnostics", "true")
                .build();

        String finalUriString = finalUri.toString() + "&env=store://datatables.org/alltableswithkeys&callback=";

        try {
            response = fetchData(finalUriString);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (response == null) {
            Intent i = new Intent(StockDetailActivity.FAILED_FETCH);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
        } else {
            JSONArray jsonResponse;
            try {
                jsonResponse = new JSONObject(response)
                        .getJSONObject("query")
                        .getJSONObject("results")
                        .getJSONArray("quote");
            } catch (JSONException e) {
                e.printStackTrace();
                return;
            }
            Intent i = new Intent(StockDetailActivity.SUCCESSFUL_FETCH);
            i.putExtra("quotes", jsonResponse.toString());
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
        }
    }
}
