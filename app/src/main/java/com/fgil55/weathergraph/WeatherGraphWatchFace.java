package com.fgil55.weathergraph;

import com.fgil55.weathergraph.widget.WeatherGraphWidget;
import com.huami.watch.watchface.AbstractSlptClock;

import java.lang.ref.WeakReference;

public class WeatherGraphWatchFace extends AbstractWatchFace {

    private static WeakReference<WeatherGraphWatchFace> instance;

    public WeatherGraphWatchFace() {
        super(new WeatherGraphWidget());
    }

    @Override
    public Class<? extends AbstractSlptClock> slptClockClass() {
        return WeatherGraphWatchFaceSlpt.class;
    }

    @Override
    public void onCreate() {
        instance = new WeakReference(this);

        super.onCreate();
    }
}