package com.fgil55.weathergraph.weather;

import android.content.Context;
import android.os.Environment;
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

public class WeatherService {

    public static WeatherService INSTANCE = new WeatherService();
    private final AtomicBoolean refreshing = new AtomicBoolean(false);
    private final AtomicReference<LocalDateTime> lastRefreshed = new AtomicReference<>(LocalDateTime.parse("1970-01-01T00:00:00"));
    private final WeatherScraper scraper;

    private Duration refreshPeriod = Duration.standardMinutes(15);

    private WeatherData currentData = new WeatherData();

    public WeatherService() {
        this.scraper = new YrNoScraper();
        restoreLastState();
    }

    private boolean lastRefreshIntervalShorterThan(LocalDateTime now, Duration duration) {
        return new Interval(lastRefreshed.get().toDateTime().toInstant(), now.toDateTime().toInstant()).toDuration().isShorterThan(duration);
    }

    public synchronized boolean refresh(Context context, LocalDateTime now) {
        boolean needsRefresh = false;

        currentData.removeExpiredData(now);

        if (refreshing.get()) needsRefresh = false;
        else if (currentData.needsRefresh()) needsRefresh = true;
        else if (lastRefreshIntervalShorterThan(now, refreshPeriod)) {
            needsRefresh = false;
        }

        if (needsRefresh) {
            Log.d("WeatherGraph", "Refreshing weather data");
            this.refreshing.set(true);

            scraper.scrap(context, currentData, now)
                    .then(ignore -> {
                        Log.d("WeatherGraph", "Refresh OK");
                        this.refreshing.set(false);
                        this.lastRefreshed.set(now);
                        saveLastState();
                        return this;
                    })
                    .error(error -> {
                        Log.e("WeatherGraph", Objects.toString(error));
                        refreshing.set(false);
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

            this.refreshing.set(false);
        } catch (Throwable e) {
            Log.e("WeatherGraph", e.toString());
            cacheFile.delete();
        }
    }

    public boolean isRefreshing() {
        return refreshing.get();
    }

    public WeatherData getCurrentData() {
        return currentData;
    }
}

