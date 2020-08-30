package com.fgil55.weathergraph.weather;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.fgil55.weathergraph.util.Promise;
import com.kieronquinn.library.amazfitcommunication.Transporter;
import com.kieronquinn.library.amazfitcommunication.internet.LocalHTTPRequest;
import com.kieronquinn.library.amazfitcommunication.internet.LocalHTTPResponse;
import com.kieronquinn.library.amazfitcommunication.internet.LocalURLConnection;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WeatherData {

    private static final DateTimeFormatter zoneFormatter = DateTimeFormat.forPattern("ZZ").withLocale(new Locale("es"));

    /*
    API sunrise
    https://api.met.no/weatherapi/sunrise/2.0/.json?date=2020-08-05&days=4&lat=43.297682530610814&lon=-2.0037496089935307&offset=%2B02%3A00

    API forecast
    https://api.met.no/weatherapi/locationforecast/2.0/complete.json?lat=43.297682530610814&lon=-2.0037496089935307
     */

    public static WeatherData INSTANCE = new WeatherData();
    private final AtomicBoolean refreshing = new AtomicBoolean(false);
    private final AtomicReference<LocalDateTime> lastRefreshed = new AtomicReference<>(LocalDateTime.parse("1970-01-01T00:00:00"));

    private List<SunraiseSunset> sunraiseSunsets = new ArrayList<>();
    private List<ForecastItem> forecasts = new ArrayList<>();
    private float minTemp = 0, maxTemp = 0, deltaTemp = 0;
    private int maxDays = 3;

    private float lat = 43.323065f, lon = -1.9284507f;
    private String place;
    private Duration refreshPeriod = Duration.standardMinutes(15);

    public List<SunraiseSunset> getSunraiseSunsets() {
        return sunraiseSunsets;
    }

    public List<ForecastItem> getForecasts() {
        return forecasts;
    }

    public float getMinTemp() {
        return minTemp;
    }

    public float getMaxTemp() {
        return maxTemp;
    }

    public float getDeltaTemp() {
        return deltaTemp;
    }

    final OkHttpClient client = new OkHttpClient();

    public WeatherData() {
        restoreLastState();
    }

    private void removeExpiredData(LocalDateTime now) {
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

    private boolean lastRefreshIntervalShorterThan(LocalDateTime now, Duration duration) {
        return new Interval(lastRefreshed.get().toDateTime().toInstant(), now.toDateTime().toInstant()).toDuration().isShorterThan(duration);
    }

    public synchronized boolean refresh(Context context, LocalDateTime now) {
        boolean needsRefresh = false;

        removeExpiredData(now);

        if (refreshing.get()) needsRefresh = false;
        else if (isEmpty()) needsRefresh = true;
        else if (sunraiseSunsets.size() < (maxDays + 1)) needsRefresh = true;
        else if (forecasts.size() < maxDays * 24) needsRefresh = true;
        else {
            if (lastRefreshIntervalShorterThan(now, refreshPeriod)) {
                needsRefresh = false;
            }
        }

        if (needsRefresh) {
            this.refreshing.set(true);
            if (isAmazfit()) {
                refreshGeocodeAmazfit(context)
                        .then(ignore -> refreshSunriseSunsetAmazfit(context, now))
                        .then(ignore -> refreshForecastAmazfit(context, now))
                        .then(ignore -> {
                            this.refreshing.set(false);
                            this.lastRefreshed.set(now);
                            saveLastState();
                            return this;
                        })
                        .error(error -> {
                            Log.e("WeatherGraph", Objects.toString(error));
                            refreshing.set(false);
                        });
            } else {
                refreshGeocode()
                        .then(ignore -> refreshSunriseSunset(now))
                        .then(ignore -> refreshForecast(now))
                        .then(ignore -> {
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
        }

        return needsRefresh;
    }

    private boolean isAmazfit() {
        return Build.BRAND.equalsIgnoreCase("huami");
    }

    private File getCacheFile() {
        return new File(Environment.getExternalStorageDirectory(), "weatherdata");
    }

    private void saveLastState() {
        try (FileOutputStream out = new FileOutputStream(getCacheFile());
             ObjectOutputStream oos = new ObjectOutputStream(out)) {
            oos.writeObject(this.sunraiseSunsets);
            oos.writeObject(this.forecasts);
            oos.writeFloat(this.lat);
            oos.writeFloat(this.lon);
            oos.writeUTF(this.place);
            oos.writeFloat(this.minTemp);
            oos.writeFloat(this.maxTemp);
            oos.writeFloat(this.deltaTemp);
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
            this.sunraiseSunsets = (List<SunraiseSunset>) ois.readObject();
            this.forecasts = (List<ForecastItem>) ois.readObject();

            this.setLat(ois.readFloat());
            this.setLon(ois.readFloat());
            this.setPlace(ois.readUTF());

            this.minTemp = ois.readFloat();
            this.maxTemp = ois.readFloat();
            this.deltaTemp = ois.readFloat();

            this.refreshing.set(false);
        } catch (Throwable e) {
            Log.e("WeatherGraph", e.toString());
        }
    }

    private boolean isAmazfitTransporterAvailable(Context context) {
        Transporter transporter = Transporter.get(context, "com.kieronquinn.app.amazfitinternetcompanion");
        return transporter.isAvailable();
    }

    private Promise execute(Request requesthttp, Consumer<String> postProcess) {
        final Promise p = new Promise();

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                try (Response response = client.newCall(requesthttp).execute()) {
                    p.resolve(response.body().string());
                } catch (IOException e) {
                    p.reject(e);
                }
                return null;
            }
        }.execute();

        return p.then(object -> {
            postProcess.accept(Objects.toString(object));
            return null;
        });
    }

    @NotNull
    private Promise executeAmazfit(Context context, LocalURLConnection conn, Consumer<String> post, final boolean isGzip) {
        final Promise p = new Promise();

        conn.setTimeout(10000);

        new LocalHTTPRequest(context, conn, new LocalHTTPResponse() {
            @Override
            public void onResult(HttpURLConnection response) {
                if (isGzip) {
                    try (InputStream in = new GZIPInputStream(response.getInputStream())) {
                        p.resolve(IOUtils.toString(in));
                    } catch (IOException e) {
                        p.reject(e);
                    }
                } else {
                    try (InputStream in = response.getInputStream()) {
                        p.resolve(IOUtils.toString(in));
                    } catch (IOException e) {
                        p.reject(e);
                    }
                }
            }

            @Override
            public void onConnectError() {
                p.reject(new ConnectException());
            }

            @Override
            public void onTimeout() {
                p.reject(new TimeoutException());
            }
        });

        return p.then(json -> {
            post.accept(Objects.toString(json));
            return null;
        });
    }

    private Promise refreshGeocodeAmazfit(final Context context) {
        HttpUrl url = getUrlForGeocode();

        final LocalURLConnection conn = new LocalURLConnection();
        conn.setUrl(url.url());
        conn.addHeader("Accept", "application/json");
        conn.addHeader("User-Agent", "curl/7.68.0");

        return executeAmazfit(context, conn, this::parseGeocode, false);
    }

    private Promise refreshGeocode() {
        HttpUrl url = getUrlForGeocode();

        Request requesthttp = new Request.Builder()
                .addHeader("accept", "application/json")
                .addHeader("User-Agent", "curl/7.68.0")
                .url(url)
                .build();

        return execute(requesthttp, this::parseGeocode);
    }

    @NotNull
    private HttpUrl getUrlForGeocode() {
        //https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=-34.44076&lon=-58.70521
        return new HttpUrl.Builder()
                .scheme("https")
                .host("nominatim.openstreetmap.org")
                .addPathSegment("reverse")
                .addQueryParameter("format", "jsonv2")
                .addQueryParameter("lat", String.valueOf(lat))
                .addQueryParameter("lon", String.valueOf(lon))
                .build();
    }

    private void parseGeocode(String body) {
        try {
            JSONObject object = new JSONObject(body);
            JSONObject address = object.getJSONObject("address");
            this.place = address.has("city") ? address.getString("city") : address.getString("town");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private Promise refreshSunriseSunsetAmazfit(final Context context, LocalDateTime now) {
        HttpUrl url = getUrlForSunriseSunset(now);

        final LocalURLConnection conn = new LocalURLConnection();
        conn.setUrl(url.url());
        conn.addHeader("Accept", "application/json");
        conn.addHeader("Accept-Encoding", "gzip");
        conn.addHeader("User-Agent", "curl/7.68.0");

        return executeAmazfit(context, conn, this::parseSunriseSunset, true);
    }

    private Promise refreshSunriseSunset(LocalDateTime now) {
        HttpUrl url = getUrlForSunriseSunset(now);

        Request requesthttp = new Request.Builder()
                .addHeader("accept", "application/json")
                .addHeader("User-Agent", "curl/7.68.0")
                .url(url)
                .build();

        return execute(requesthttp, this::parseSunriseSunset);
    }

    private void parseSunriseSunset(String body) {
        try {
            JSONObject object = new JSONObject(body);
            JSONArray time = object.getJSONObject("location").getJSONArray("time");

            this.sunraiseSunsets.clear();

            for (int i = 0; i < time.length(); i++) {
                JSONObject aDay = time.getJSONObject(i);

                if (!aDay.has("sunrise")) continue;

                this.sunraiseSunsets.add(new SunraiseSunset(aDay));
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private HttpUrl getUrlForSunriseSunset(LocalDateTime now) {
        return new HttpUrl.Builder()
                .scheme("https")
                .host("api.met.no")
                .addPathSegment("weatherapi")
                .addPathSegment("sunrise")
                .addPathSegment("2.0")
                .addPathSegment(".json")
                .addQueryParameter("date", now.toLocalDate().toString())
                .addQueryParameter("lat", String.valueOf(lat))
                .addQueryParameter("lon", String.valueOf(lon))
                .addQueryParameter("days", String.valueOf(maxDays + 1))
                .addQueryParameter("offset", now.toDateTime(DateTimeZone.getDefault()).toString(zoneFormatter))
                .build();
    }

    private Promise refreshForecastAmazfit(final Context context, final LocalDateTime now) {
        HttpUrl url = getUrlForForecast();

        final LocalURLConnection conn = new LocalURLConnection();
        conn.setUrl(url.url());
        conn.addHeader("Accept", "application/json");
        conn.addHeader("Accept-Encoding", "gzip");
        conn.addHeader("User-Agent", "curl/7.68.0");

        return executeAmazfit(context, conn, parseForecast(now), true);
    }

    private Promise refreshForecast(LocalDateTime now) {
        HttpUrl url = getUrlForForecast();

        Request requesthttp = new Request.Builder()
                .addHeader("accept", "application/json")
                .addHeader("User-Agent", "curl/7.68.0")
                .url(url)
                .build();

        return execute(requesthttp, parseForecast(now));
    }

    private Consumer<String> parseForecast(LocalDateTime now) {
        return (String body) -> {
            try {
                JSONObject object = new JSONObject(body);

                forecasts.clear();
                minTemp = Integer.MAX_VALUE;
                maxTemp = Integer.MIN_VALUE;

                JSONArray timeseries = object.getJSONObject("properties").getJSONArray("timeseries");
                AtomicInteger cloudGroup = new AtomicInteger(1);

                for (int i = 0; i < timeseries.length(); i++) {
                    JSONObject item = timeseries.getJSONObject(i);
                    ForecastItem forecast = new ForecastItem(item, cloudGroup);
                    if (forecast.getTime().isAfter(now)) {
                        forecasts.add(forecast);

//                if (forecast.getTime().isBefore(now.plusDays(maxDays))) {
                        if (forecast.getTemp() < minTemp) minTemp = forecast.getTemp();
                        if (forecast.getTemp() > maxTemp) maxTemp = forecast.getTemp();
//                }
                    }
                }

                deltaTemp = maxTemp - minTemp;
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @NotNull
    private HttpUrl getUrlForForecast() {
        return new HttpUrl.Builder()
                .scheme("https")
                .host("api.met.no")
                .addPathSegment("weatherapi")
                .addPathSegment("locationforecast")
                .addPathSegment("2.0")
                .addPathSegment("complete.json")
                .addQueryParameter("lat", String.valueOf(lat))
                .addQueryParameter("lon", String.valueOf(lon))
                .build();
    }

    public boolean isEmpty() {
        return sunraiseSunsets.isEmpty() || forecasts.isEmpty();
    }

    public int getMaxDays() {
        return maxDays;
    }

    public void setMaxDays(int maxDays) {
        this.maxDays = maxDays;
    }

    public String getPlace() {
        return place;
    }

    public String getCurrentTempAndPlace() {
        return String.format("%dº %s", (int) forecasts.get(0).getTemp(), place);
    }

    public void setLat(float lat) {
        this.lat = lat;
    }

    public void setLon(float lon) {
        this.lon = lon;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public boolean isRefreshing() {
        return refreshing.get();
    }
}

