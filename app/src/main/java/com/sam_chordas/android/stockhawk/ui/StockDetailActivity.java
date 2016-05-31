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

    Context mContext;
    private String DATE_RANGE_ERROR_TEXT;
    private String FUTURE_DATE__ERROR_TEXT;
    private String fetchFormat = "yyyy-MM-dd";
    private String displayFormat = "MMM dd, yyyy";
    SimpleDateFormat displayFormatter = new SimpleDateFormat(displayFormat, Locale.US);
    SimpleDateFormat fetchFormatter = new SimpleDateFormat(fetchFormat, Locale.US);

    private final int TO_DATE = 0;
    private final int FROM_DATE = 1;


    @Bind(R.id.detail_stock_title_textview)
    TextView textView;
    @Bind(R.id.chart)
    LineChart lineChart;
    @Bind(R.id.from_date)
    TextInputEditText fromDateEditText;
    @Bind(R.id.to_date)
    TextInputEditText toDateEditText;
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
        mContext = this;
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
        DATE_RANGE_ERROR_TEXT = getString(R.string.date_range_error);
        FUTURE_DATE__ERROR_TEXT = getString(R.string.future_date_error);

        fromDateEditText.setOnClickListener(this);
        toDateEditText.setOnClickListener(this);
        fetchButton.setOnClickListener(this);
        fetchButton.setEnabled(false);
        startFetchDataService();

    }

    private void setError(@Nullable TextInputEditText textInputEditText, String error) {
        if (textInputEditText != null) {
            textInputEditText.setError(error);
        }
        errorTextView.setVisibility(View.VISIBLE);
        errorTextView.setText(error);
    }

    private void updateLabel(TextInputEditText textInputEditText, int dateToUpdate) {

        //Reset errors
        errorTextView.setVisibility(View.INVISIBLE);
        textInputEditText.setError(null);
        //Put Date in input
        textInputEditText.setText(displayFormatter.format(myCalendar.getTime()));


        // If date range doesn't make logical sense or any date is beyond today
        // Disable button
        if (dateToUpdate == FROM_DATE) {
            fromDateToFetch = fetchFormatter.format(myCalendar.getTime());
            fromDateToFetchInMilli = myCalendar.getTimeInMillis();

            if (todayInMilli < fromDateToFetchInMilli) {
                setError(textInputEditText, FUTURE_DATE__ERROR_TEXT);
            } else if (toDateToFetchInMilli != 0L && fromDateToFetchInMilli > toDateToFetchInMilli) {
                setError(textInputEditText, DATE_RANGE_ERROR_TEXT);
            }
        } else if (dateToUpdate == TO_DATE) {
            toDateToFetch = fetchFormatter.format(myCalendar.getTime());
            toDateToFetchInMilli = myCalendar.getTimeInMillis();

            if (todayInMilli < toDateToFetchInMilli) {
                setError(textInputEditText, FUTURE_DATE__ERROR_TEXT);
            } else if (fromDateToFetchInMilli != 0L && fromDateToFetchInMilli > toDateToFetchInMilli) {
                setError(textInputEditText, DATE_RANGE_ERROR_TEXT);
            }
        }

        boolean enableButton = (errorTextView.getVisibility() == View.INVISIBLE)
                && (fromDateEditText.getError() == null && toDateEditText.getError() == null);

        if (fromDateEditText.getError() != null){
            setError(null, (String) fromDateEditText.getError());
        } else if (toDateEditText.getError() != null) {
            setError(null, (String) toDateEditText.getError());
        }

        fetchButton.setEnabled(enableButton);
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
        String fromDateForQuery;
        String toDateForQuery;
        if (toDateToFetch == null) {
            Calendar calendar = Calendar.getInstance();
            toDateForQuery = fetchFormatter.format(calendar.getTime());
            toDateEditText.setText(displayFormatter.format(calendar.getTime()));
        } else {
            toDateForQuery = toDateToFetch;
        }

        if (fromDateToFetch == null) {
            Calendar calendar = Calendar.getInstance();
            long twoWeeksAgo = calendar.getTimeInMillis() - (1000 * 60 * 60 * 24 * 14);
            calendar.setTimeInMillis(twoWeeksAgo);
            fromDateForQuery = fetchFormatter.format(calendar.getTime());
            fromDateEditText.setText(displayFormatter.format(calendar.getTime()));
        } else {
            fromDateForQuery = fromDateToFetch;
        }

        Intent intent = new Intent(this, StockDetailService.class);
        intent.putExtra("symbol", symbol);
        intent.putExtra("start", fromDateForQuery);
        intent.putExtra("end", toDateForQuery);

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

        private final float LINE_WIDTH = 2.0f;
        private final float CIRCLE_RADIUS = 3.0f;
        private final int HIGH_DATA_COLOR = ContextCompat.getColor(getApplicationContext(), R.color.material_blue_500);
        private final int LOW_DATA_COLOR = ContextCompat.getColor(getApplicationContext(), R.color.material_red_700);

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
            for (int index = (params[0].length() - 1); index > -1; index--) {
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

            linedataset1.setLineWidth(LINE_WIDTH);
            linedataset1.setCircleRadius(CIRCLE_RADIUS);

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

