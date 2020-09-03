package com.fgil55.weathergraph.weather;

import org.joda.time.LocalDateTime;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WeatherData implements Serializable {

    private List<SunraiseSunset> sunraiseSunsets = new ArrayList<>();
    private List<ForecastItem> forecasts = new ArrayList<>();
    private float minTemp = 0, maxTemp = 0, deltaTemp = 0;
    private float lat = 43.323065f, lon = -1.9284507f;   //Pasaia
    private String place;
    private int maxDays = 3;

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

    public float getLat() {
        return lat;
    }

    public void setLat(float lat) {
        this.lat = lat;
    }

    public float getLon() {
        return lon;
    }

    public void setLon(float lon) {
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
        return isEmpty() || sunraiseSunsets.size() < (maxDays + 2) || forecasts.size() < maxDays * 24;
    }

    void removeExpiredData(LocalDateTime now) {
        if (!this.sunraiseSunsets.isEmpty()) {
            final List<SunraiseSunset> sunraiseSunsetsToRemove = this.sunraiseSunsets.stream()
                    .filter(sunraiseSunset -> sunraiseSunset.getDate().isBefore(now.toLocalDate()))
                    .collect(Collectors.toList());

            this.sunraiseSunsets.removeAll(sunraiseSunsetsToRemove);
        }

        if (!this.forecasts.isEmpty()) {
            final List<ForecastItem> forecastsToRemove = this.forecasts.stream()
                    .filter(forecastItem -> forecastItem.getTime().isBefore(now))
                    .collect(Collectors.toList());

            this.forecasts.removeAll(forecastsToRemove);
        }
    }

    public String getCurrentTempAndPlace() {
        return String.format("%dº %s", (int) forecasts.get(0).getTemp(), place);
    }
}
