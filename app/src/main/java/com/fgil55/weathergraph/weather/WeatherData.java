package com.fgil55.weathergraph.weather;

import android.util.Log;

import com.fgil55.weathergraph.util.DateUtils;

import org.joda.time.DateTimeUtils;
import org.joda.time.LocalDateTime;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class WeatherData implements Serializable {

    public static final LocalDateTime NOT_REFRESHED = LocalDateTime.parse("1970-01-01T00:00:00");
    private List<SunraiseSunset> sunraiseSunsets = new ArrayList<>();
    private List<ForecastItem> forecasts = new ArrayList<>();
    private float minTemp = 0, maxTemp = 0, deltaTemp = 0;
    private float maxWind = 0;
    private float lat = 43.323065f, lon = -1.9284507f;   //Pasaia
    //    private float lat = 63.4305f , lon = 10.3951f;   //Trondheim
    private String place;
    private int maxDays = 4;

    private final AtomicBoolean refreshing = new AtomicBoolean(false);
    private final AtomicReference<LocalDateTime> lastRefreshed = new AtomicReference<>(NOT_REFRESHED);

    public List<SunraiseSunset> getSunraiseSunsets() {
        return sunraiseSunsets;
    }

    public void setSunraiseSunsets(List<SunraiseSunset> sunraiseSunsets) {
        this.sunraiseSunsets = sunraiseSunsets;
    }

    public List<ForecastItem> getForecasts() {
        return forecasts;
    }

    public void setForecasts(List<ForecastItem> forecasts) {
        this.forecasts = forecasts;
    }

    public float getMinTemp() {
        return minTemp;
    }

    public void setMinTemp(float minTemp) {
        this.minTemp = minTemp;
    }

    public float getMaxTemp() {
        return maxTemp;
    }

    public void setMaxTemp(float maxTemp) {
        this.maxTemp = maxTemp;
    }

    public float getDeltaTemp() {
        return deltaTemp;
    }

    public void setDeltaTemp(float deltaTemp) {
        this.deltaTemp = deltaTemp;
    }

    public float getMaxWind() {
        return maxWind;
    }

    public void setMaxWind(float maxWind) {
        this.maxWind = maxWind;
    }

    public float getLat() {
        return lat;
    }

    public void setLat(float lat) {
        if (lat != this.lat) this.lastRefreshed.set(NOT_REFRESHED);
        this.lat = lat;
    }

    public float getLon() {
        return lon;
    }

    public void setLon(float lon) {
        if (lon != this.lon) this.lastRefreshed.set(NOT_REFRESHED);
        this.lon = lon;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public int getMaxDays() {
        return maxDays;
    }

    public boolean isEmpty() {
        return sunraiseSunsets.isEmpty() || forecasts.isEmpty();
    }

    public boolean needsRefresh() {
        if (isEmpty() || sunraiseSunsets.size() < (maxDays + 2)) return true;
        final LocalDateTime firstForecast = forecasts.get(0).getTime();
        final LocalDateTime lastForecast = forecasts.get(forecasts.size() - 1).getTime();

        return DateUtils.getDaysOfDifference(lastForecast, firstForecast) < maxDays;
    }

    void removeExpiredData(LocalDateTime now) {
        if (!this.sunraiseSunsets.isEmpty()) {
            final List<SunraiseSunset> sunraiseSunsetsToRemove = this.sunraiseSunsets.stream()
                    .filter(sunraiseSunset -> sunraiseSunset.getDate().isBefore(now.toLocalDate()))
                    .collect(Collectors.toList());

            if (!sunraiseSunsetsToRemove.isEmpty()) Log.d("WeatherGraph", "Removing " + sunraiseSunsetsToRemove.size() + " items from SunraiseSunsets data");

            this.sunraiseSunsets.removeAll(sunraiseSunsetsToRemove);
        }

        if (!this.forecasts.isEmpty()) {
            final List<ForecastItem> forecastsToRemove = this.forecasts.stream()
                    .filter(forecastItem -> forecastItem.getTime().isBefore(now))
                    .collect(Collectors.toList());

            if (!forecastsToRemove.isEmpty()) Log.d("WeatherGraph", "Removing " + forecastsToRemove.size() + " items from forecast data");

            this.forecasts.removeAll(forecastsToRemove);
        }
    }

    public String getCurrentTempAndPlace() {
        return String.format("%dº %s", (int) forecasts.get(0).getTemp(), place);
    }

    public int getCurrentUv() {
        return Math.round(getForecasts().stream().findFirst().map(ForecastItem::getUv).orElse(0f));
    }

    public AtomicBoolean getRefreshing() {
        return refreshing;
    }

    public AtomicReference<LocalDateTime> getLastRefreshed() {
        return lastRefreshed;
    }
}
