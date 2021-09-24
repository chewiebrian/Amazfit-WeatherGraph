package com.fgil55.weathergraph.data;

import android.util.Log;

import com.huami.watch.watchface.WatchDataListener;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Adapter for the multiple listeners in one.
 */
public class MultipleWatchDataListenerAdapter implements WatchDataListener {

    private final MultipleWatchDataListener listener;
    private final DataType type;

    public MultipleWatchDataListenerAdapter(MultipleWatchDataListener listener, DataType type) {
        this.listener = listener;
        this.type = type;
    }

    @Override
    public int getDataType() {
        return type.getDataType();
    }

    @Override
    public void onDataUpdate(int i, Object... objects) {
        DataType type = DataType.fromValue(i);
        listener.onDataUpdate(type, type.getValue(objects));

        Log.d("WeatherGraph", "Data Update: "+type.toString()+" = " + Stream.of(objects).map(Object::toString).collect(Collectors.joining(",")) );
    }
}
