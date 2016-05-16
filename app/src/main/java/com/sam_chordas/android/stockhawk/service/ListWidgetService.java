package com.sam_chordas.android.stockhawk.service;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kare2436 on 5/15/16.
 */
public class ListWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new ListViewsFactory(this.getApplicationContext(), intent);
    }

    private class Stock {
        private String mSymbol;
        private String mChange;
        private String mBidPrice;

        public int getmIsUp() {
            return mIsUp;
        }

        public void setmIsUp(int mIsUp) {
            this.mIsUp = mIsUp;
        }

        private int mIsUp;

        public Stock() {}

        public void setmBidPrice(String mBidPrice) {
            this.mBidPrice = mBidPrice;
        }

        public void setmSymbol(String mSymbol) {
            this.mSymbol = mSymbol;
        }

        public String getmSymbol() {
            return mSymbol;
        }

        public String getmChange() {
            return mChange;
        }

        public void setmChange(String mChange) {
            this.mChange = mChange;
        }

        public String getmBidPrice() {
            return mBidPrice;
        }
    }

    private class ListViewsFactory implements RemoteViewsService.RemoteViewsFactory {


        private static final String TAG = "ListViewsFactory";
        private final Context mContext;
        List<Stock> stocks = new ArrayList<>();
        private int mCount;
        private final int mAppWidgetId;
        private Cursor mCursor;

        public ListViewsFactory(Context context, Intent intent) {
            mContext = context;
            mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        @Override
        public void onCreate() {}

        @Override
        public void onDataSetChanged() {
            mCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{ QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                            QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                    QuoteColumns.ISCURRENT + " = ?",
                    new String[]{"1"},
                    null);
            mCount = mCursor != null ? mCursor.getCount() : 0;

            Log.d(TAG, "onDataSetChanged: " + mCount);

            Stock stock;

            for (int i = 0; i < mCount; i++) {
                if (!mCursor.moveToNext()) break;
                stock = new Stock();
                stock.setmBidPrice(mCursor.getString(mCursor.getColumnIndex(QuoteColumns.SYMBOL)));
                stock.setmChange(mCursor.getString(mCursor.getColumnIndex(QuoteColumns.CHANGE)));
                stock.setmBidPrice(mCursor.getString(mCursor.getColumnIndex(QuoteColumns.BIDPRICE)));
                stock.setmIsUp(mCursor.getInt(mCursor.getColumnIndex(QuoteColumns.ISUP)));
                stocks.add(stock);
            }
            if (mCursor != null) {
                mCursor.close();
            }
        }

        @Override
        public void onDestroy() {

        }

        @Override
        public int getCount() {
            return mCount;
        }

        @Override
        public RemoteViews getViewAt(int position) {
            // Construct a remote views item based on the app widget item XML file,
            // and set the text based on the position.
            RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.widget_list_item_quote);

            rv.setTextViewText(R.id.stock_symbol, stocks.get(position).getmSymbol());
            rv.setTextViewText(R.id.change, stocks.get(position).getmChange());
            rv.setTextViewText(R.id.bid_price, stocks.get(position).getmBidPrice());

//            int sdk = Build.VERSION.SDK_INT;
//            if (stocks.get(position).getmIsUp() == 1){
//                if (sdk < Build.VERSION_CODES.JELLY_BEAN){
//                    rv.getLayoutId()setBackgroundDrawable(
//                            mContext.getResources().getDrawable(R.drawable.percent_change_pill_green));
//                }else {
//                    viewHolder.change.setBackground(
//                            mContext.getResources().getDrawable(R.drawable.percent_change_pill_green));
//                }
//            } else{
//                if (sdk < Build.VERSION_CODES.JELLY_BEAN) {
//                    viewHolder.change.setBackgroundDrawable(
//                            mContext.getResources().getDrawable(R.drawable.percent_change_pill_red));
//                } else{
//                    viewHolder.change.setBackground(
//                            mContext.getResources().getDrawable(R.drawable.percent_change_pill_red));
//                }
//            }
            // Return the remote views object.
            return rv;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }
    }
}
