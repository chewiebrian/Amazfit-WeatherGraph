package com.fgil55.weathergraph.weather;

import android.content.Context;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;

import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.LocalDateTime;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class WeatherService {

    public static WeatherService INSTANCE = new WeatherService();
    private final WeatherScraper scraper;

    private Duration refreshPeriod = Duration.standardMinutes(60);

    private WeatherData currentData = new WeatherData();

    public WeatherService() {
        this.scraper = new YrNoScraper();
        restoreLastState();
    }

    private boolean lastRefreshIntervalShorterThan(LocalDateTime now, Duration duration) {
        return new Interval(currentData.getLastRefreshed().get().toDateTime().toInstant(), now.toDateTime().toInstant()).toDuration().isShorterThan(duration);
    }

    private boolean isAirplaneMode(Context context) {
        return Settings.Global.getInt(
                context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) == 1;
    }

    public synchronized boolean refresh(Context context, LocalDateTime now, Runnable callback) {
        boolean needsRefresh = false;

        currentData.removeExpiredData(now);

        if (currentData.getRefreshing().get()) needsRefresh = false;
        else if (isAirplaneMode(context))  {
            Log.d("WeatherGraph", "Airplane mode detected, not refreshing weather data");
            needsRefresh = false;
        }
        else if (currentData.needsRefresh()) needsRefresh = true;
        else if (lastRefreshIntervalShorterThan(now, refreshPeriod)) {
            needsRefresh = false;
        }

        if (needsRefresh) {
            Log.d("WeatherGraph", "Refreshing weather data");
            currentData.getRefreshing().set(true);

            scraper.scrap(context, currentData, now)
                    .then(ignore -> {
                        Log.d("WeatherGraph", "Refresh OK");
                        currentData.getRefreshing().set(false);
                        currentData.getLastRefreshed().set(now);
                        saveLastState();
                        callback.run();
                        return this;
                    })
                    .error(error -> {
                        Log.e("WeatherGraph", Objects.toString(error));
                        currentData.getRefreshing().set(false);
                    });
        }

        return needsRefresh;
    }


    private File getCacheFile() {
        return new File(Environment.getExternalStorageDirectory(), "weatherdata");
    }

    private void saveLastState() {
        try (FileOutputStream out = new FileOutputStream(getCacheFile());
             ObjectOutputStream oos = new ObjectOutputStream(out)) {
            oos.writeObject(this.currentData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void restoreLastState() {
        final File cacheFile = getCacheFile();
        if (!cacheFile.exists()) return;

        Log.d("WeatherGraph", "Reading cached data from " + cacheFile);
        try (FileInputStream in = new FileInputStream(cacheFile);
             ObjectInputStream ois = new ObjectInputStream(in)) {
            this.currentData = (WeatherData) ois.readObject();

            currentData.getRefreshing().set(false);
        } catch (Throwable e) {
            Log.e("WeatherGraph", e.toString());
            cacheFile.delete();
        }
    }

    public WeatherData getCurrentData() {
        return currentData;
    }
}

