package com.sam_chordas.android.stockhawk.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.Voice;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.service.StockDetailService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by kare2436 on 4/16/16.
 */
public class StockDetailActivity extends AppCompatActivity {


    public static final String FAILED_FETCH = "com.sam_chordas.android.stockhawk.FAILED_DETAIL_FETCH";
    public static final String SUCCESSFUL_FETCH = "com.sam_chordas.android.stockhawk.SUCCESSFUL_DETAIL_FETCH";
    @Bind(R.id.detail_stock_title_textview)
    TextView textView;
    @Bind(R.id.chart)
    LineChart lineChart;
    String symbol;
    View root;
    JSONArray stockData;
    ArrayList<String> xVals;
    ArrayList<ILineDataSet> sets;
    LineData lineData;
    private LocalReceiver localReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_detail);
        root = findViewById(R.id.root);

        ButterKnife.bind(this);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            symbol = extras.getString("title");
            textView.setText(symbol);
        }

        lineChart.setLogEnabled(true);
        lineChart.setNoDataTextDescription("No data to display.");

        Intent intent = new Intent(this, StockDetailService.class);
        intent.putExtra("symbol", symbol);
        intent.putExtra("start", "2016-01-29");
        intent.putExtra("end", "2016-02-28");

        startService(intent);
        localReceiver = new LocalReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SUCCESSFUL_FETCH);
        intentFilter.addAction(FAILED_FETCH);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(localReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(localReceiver);
    }

    private class LocalReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Bundle extras = intent.getExtras();

            if (action.equals(FAILED_FETCH)) {
                Snackbar.make(root, "Failed to get details", Snackbar.LENGTH_LONG).show();
            } else {
                Snackbar.make(root, "Got details", Snackbar.LENGTH_LONG).show();
                try {
                    stockData = new JSONArray(extras.getString("quotes"));
                    new FormatDataTask().execute(stockData);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }
    }


    private class FormatDataTask extends AsyncTask<JSONArray, Void, Void> {

        /**
         * Sample data set
         * {"Symbol":"fb",
         * "Date":"2016-02-26",
         * "Open":"108.699997",
         * "High":"109.449997",
         * "Low":"107.160004",
         * "Close":"107.919998",
         * "Volume":"26578900",
         * "Adj_Close":"107.919998"}
         */

        @Override
        protected Void doInBackground(JSONArray... params) {
            JSONObject temp;
            ArrayList<Entry> highs = new ArrayList<>();
            ArrayList<Entry> lows = new ArrayList<>();
            ArrayList<Entry> closes = new ArrayList<>();
            xVals = new ArrayList<>();
            sets = new ArrayList<>();
            for (int index = 0; index < params[0].length(); index++) {
                try {
                    temp = params[0].getJSONObject(index);
                    xVals.add(temp.getString("Date"));
                    highs.add(new Entry((float) temp.getDouble("High"), index));
                    lows.add(new Entry((float) temp.getDouble("Low"), index));
                    closes.add(new Entry((float) temp.getDouble("Close"), index));


                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            LineDataSet linedataset1 = new LineDataSet(lows, "Lows");
            linedataset1.setAxisDependency(YAxis.AxisDependency.LEFT);
            linedataset1.setLineWidth(2.0f);
            linedataset1.setFillColor(ContextCompat.getColor(getApplicationContext(), android.R.color.black));

            LineDataSet linedataset2 = new LineDataSet(highs, "Highs");
            linedataset2.setAxisDependency(YAxis.AxisDependency.LEFT);

            LineDataSet linedataset3 = new LineDataSet(closes, "Close");
            linedataset3.setAxisDependency(YAxis.AxisDependency.LEFT);

            sets.add(linedataset1);


            lineData = new LineData(xVals, sets);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            lineChart.setData(lineData);
            lineChart.invalidate();
        }
    }
}
