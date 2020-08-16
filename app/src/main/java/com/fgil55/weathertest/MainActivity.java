package com.fgil55.weathertest;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.fgil55.weathertest.data.WeatherData;

public class MainActivity extends Activity {

    private TextView mTextView;
    private ContainerView mContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        mContainer = (ContainerView) findViewById(R.id.container);

//        WeatherData.INSTANCE.setLat(43.3494f);
//        WeatherData.INSTANCE.setLon(-4.0479f);
//        WeatherData.INSTANCE.setLat( 36.5298f);
 //       WeatherData.INSTANCE.setLon(-6.2947f);

        refresh();
        mContainer.setClickable(true);
        mContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refresh();
            }
        });
    }

    private void refresh() {
        if(Build.BRAND.equalsIgnoreCase("huami")) {
            WeatherData.INSTANCE.refresh(mContainer, new Runnable() {
                @Override
                public void run() {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mContainer.invalidate();
                        }
                    });
                }
            });
        } else {
            new AsyncTask<Void, Void, Void>() {

                @SuppressLint("StaticFieldLeak")
                @Override
                protected Void doInBackground(Void... voids) {
                    WeatherData.INSTANCE.refresh(mContainer, new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mContainer.invalidate();
                                }
                            });
                        }
                    });

                    return null;
                }
            }.execute();
        }
    }

}
