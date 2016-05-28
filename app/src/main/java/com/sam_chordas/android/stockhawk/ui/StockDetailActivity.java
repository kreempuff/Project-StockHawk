package com.sam_chordas.android.stockhawk.ui;

import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextSwitcher;
import android.widget.TextView;

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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;

public class StockDetailActivity extends AppCompatActivity implements View.OnClickListener, DatePickerDialog.OnDateSetListener {


    public static final String FAILED_FETCH = "com.sam_chordas.android.stockhawk.FAILED_DETAIL_FETCH";
    public static final String SUCCESSFUL_FETCH = "com.sam_chordas.android.stockhawk.SUCCESSFUL_DETAIL_FETCH";
    private static final String TAG = "StockDetailActivity";

    private int HIGH_DATA_COLOR;
    private int LOW_DATA_COLOR;
    private String DATE_RANGE_ERROR_TEXT;
    private String FUTURE_DATE__ERROR_TEXT;
    private final static float LINE_WIDTH = 2.0f;
    private final static float CIRCLE_RADIUS = 4.5f;

    private final int TO_DATE = 0;
    private final int FROM_DATE = 1;


    @Bind(R.id.detail_stock_title_textview)
    TextView textView;
    @Bind(R.id.chart)
    LineChart lineChart;
    @Bind(R.id.from_date)
    TextInputEditText fromDate;
    @Bind(R.id.to_date)
    TextInputEditText toDate;
    @Bind(R.id.fetch_button)
    Button fetchButton;
    @Bind(R.id.date_error)
    TextView errorTextView;

    TextInputEditText viewBeingEdited;

    String symbol;
    public String fromDateToFetch;
    public String toDateToFetch;
    View root;
    JSONArray stockData;
    ArrayList<String> xVals;
    ArrayList<ILineDataSet> sets;
    LineData lineData;
    private LocalReceiver localReceiver;
    public Calendar myCalendar;
    private long todayInMilli = 0L;
    private long fromDateToFetchInMilli = 0L;
    private long toDateToFetchInMilli = 0L;

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
        lineChart.setNoDataTextDescription(getString(R.string.chart_no_data));
        lineChart.setContentDescription(getString(R.string.chart_content_description));


        localReceiver = new LocalReceiver();

        myCalendar = Calendar.getInstance();
        todayInMilli = System.currentTimeMillis();
        HIGH_DATA_COLOR = ContextCompat.getColor(getApplicationContext(), R.color.material_blue_500);
        LOW_DATA_COLOR = ContextCompat.getColor(getApplicationContext(), R.color.material_red_700);
        DATE_RANGE_ERROR_TEXT = getString(R.string.date_range_error);
        FUTURE_DATE__ERROR_TEXT = getString(R.string.future_date_error);

        fromDate.setOnClickListener(this);
        toDate.setOnClickListener(this);
        fetchButton.setOnClickListener(this);
        fetchButton.setEnabled(false);
        startFetchDataService();

    }

    private void updateLabel(TextInputEditText textInputEditText, int dateToUpdate) {

        String myFormat = "MMM dd, yyyy"; //In which you need put here
        String fetchFromat = "yyyy-MM-dd";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
        SimpleDateFormat fetchFormatter = new SimpleDateFormat(fetchFromat, Locale.US);
        errorTextView.setVisibility(View.INVISIBLE);

        textInputEditText.setError(null);
        textInputEditText.setText(sdf.format(myCalendar.getTime()));

        Log.i(TAG, "updateLabel: " + myCalendar.getTimeInMillis());

        if (dateToUpdate == FROM_DATE) {
            fromDateToFetch = fetchFormatter.format(myCalendar.getTime());
            fromDateToFetchInMilli = myCalendar.getTimeInMillis();

            if (todayInMilli < fromDateToFetchInMilli) {
                errorTextView.setVisibility(View.VISIBLE);
                errorTextView.setText(FUTURE_DATE__ERROR_TEXT);
                textInputEditText.setError("Some Error");
            } else if (toDateToFetchInMilli != 0L && fromDateToFetchInMilli > toDateToFetchInMilli) {
                errorTextView.setVisibility(View.VISIBLE);
                errorTextView.setText(DATE_RANGE_ERROR_TEXT);
                textInputEditText.setError("Some Error");
            }
        } else if (dateToUpdate == TO_DATE) {
            toDateToFetch = fetchFormatter.format(myCalendar.getTime());
            toDateToFetchInMilli = myCalendar.getTimeInMillis();
            if (todayInMilli < toDateToFetchInMilli) {
                errorTextView.setVisibility(View.VISIBLE);
                errorTextView.setText(FUTURE_DATE__ERROR_TEXT);
                textInputEditText.setError("Some Error");
            } else if (fromDateToFetchInMilli != 0L && fromDateToFetchInMilli > toDateToFetchInMilli) {
                errorTextView.setVisibility(View.VISIBLE);
                errorTextView.setText(DATE_RANGE_ERROR_TEXT);
                textInputEditText.setError("Some Error");
            }
        }

        fetchButton.setEnabled(errorTextView.getVisibility() == View.INVISIBLE);
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

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.fetch_button) {
            startFetchDataService();
            return;
        }

        viewBeingEdited = (TextInputEditText) v;
        // TODO Auto-generated method stub
        new DatePickerDialog(this, StockDetailActivity.this, myCalendar
                .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                myCalendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        // TODO Auto-generated method stub
        myCalendar.set(Calendar.YEAR, year);
        myCalendar.set(Calendar.MONTH, monthOfYear);
        myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        if (viewBeingEdited.getId() == R.id.from_date) {
            updateLabel(viewBeingEdited, FROM_DATE);
        } else if (viewBeingEdited.getId() == R.id.to_date) {
            updateLabel(viewBeingEdited, TO_DATE);
        }
    }

    private void startFetchDataService() {
        if (errorTextView.getVisibility() == View.VISIBLE) {
            return;
        }
        String fromDate;
        String toDate;
        if (toDateToFetch == null) {
            toDate = "2016-02-28";
        } else {
            toDate = toDateToFetch;
        }

        if (fromDateToFetch == null) {
            fromDate = "2016-02-01";
        } else {
            fromDate = fromDateToFetch;
        }

        Intent intent = new Intent(this, StockDetailService.class);
        intent.putExtra("symbol", symbol);
        intent.putExtra("start", fromDate);
        intent.putExtra("end", toDate);

        Snackbar.make(root, "Getting stocks", Snackbar.LENGTH_LONG).show();

        Log.i(TAG, "startFetchDataService: " + fromDateToFetch);
        Log.i(TAG, "startFetchDataService: " + toDateToFetch);

        startService(intent);
    }

    private class LocalReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Bundle extras = intent.getExtras();

            if (action.equals(FAILED_FETCH)) {
                Snackbar.make(root, R.string.fetch_stocks_error_snackbar, Snackbar.LENGTH_LONG).show();
            } else {
                Snackbar.make(root, R.string.fetch_stocks_success_snackbar, Snackbar.LENGTH_LONG).show();
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

            //Line DataSet 1
            LineDataSet linedataset1 = new LineDataSet(lows, "Lows");
            linedataset1.setAxisDependency(YAxis.AxisDependency.LEFT);

            linedataset1.setLineWidth(2.0f);
            linedataset1.setCircleRadius(4.5f);

            linedataset1.setColor(LOW_DATA_COLOR);
            linedataset1.setCircleColor(LOW_DATA_COLOR);
            linedataset1.setCircleColorHole(LOW_DATA_COLOR);


            //Line DataSet 2
            LineDataSet linedataset2 = new LineDataSet(highs, "Highs");
            linedataset2.setAxisDependency(YAxis.AxisDependency.LEFT);

            linedataset2.setColor(HIGH_DATA_COLOR);
            linedataset2.setCircleColor(HIGH_DATA_COLOR);
            linedataset2.setCircleColorHole(HIGH_DATA_COLOR);

            linedataset2.setLineWidth(LINE_WIDTH);
            linedataset2.setCircleRadius(CIRCLE_RADIUS);

            //Line DataSet 2 TODO - Remove
            //LineDataSet linedataset3 = new LineDataSet(closes, "Close");
            //linedataset3.setAxisDependency(YAxis.AxisDependency.LEFT);

            sets.add(linedataset1);
            sets.add(linedataset2);


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

