package com.fgil55.weathergraph;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {

    private TextView mTextView;
    private ContainerView mContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        mContainer = (ContainerView) findViewById(R.id.container);

        mContainer.setClickable(true);
        mContainer.setOnClickListener((e) -> mContainer.invalidate());

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            runOnUiThread(() -> mContainer.invalidate());
        },0, 1, TimeUnit.SECONDS);
    }

}

