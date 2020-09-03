package com.fgil55.weathergraph.weather;

import android.content.Context;
import android.os.Build;

import com.fgil55.weathergraph.util.Promise;

import org.joda.time.LocalDateTime;

public interface WeatherScraper {

    Promise scrap(Context context, WeatherData currentData, LocalDateTime now);

    default boolean isAmazfit() {
        return Build.BRAND.equalsIgnoreCase("huami");
    }
}
