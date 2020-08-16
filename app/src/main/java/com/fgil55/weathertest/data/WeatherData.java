package com.fgil55.weathertest.data;

import android.content.Context;
import android.os.Build;
import android.view.View;

import com.kieronquinn.library.amazfitcommunication.internet.LocalHTTPRequest;
import com.kieronquinn.library.amazfitcommunication.internet.LocalHTTPResponse;
import com.kieronquinn.library.amazfitcommunication.internet.LocalURLConnection;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WeatherData {

    private final static DateTimeFormatter zoneFormatter = DateTimeFormat.forPattern("ZZ").withLocale(new Locale("es"));

    /*
    API sunrise
    https://api.met.no/weatherapi/sunrise/2.0/.json?date=2020-08-05&days=4&lat=43.297682530610814&lon=-2.0037496089935307&offset=%2B02%3A00

    API forecast
    https://api.met.no/weatherapi/locationforecast/2.0/complete.json?lat=43.297682530610814&lon=-2.0037496089935307
     */

    public static WeatherData INSTANCE = new WeatherData();

    private final List<SunraiseSunset> sunraiseSunsets = new ArrayList<>();
    private final List<ForecastItem> forecasts = new ArrayList<>();
    private float minTemp = 0, maxTemp = 0, deltaTemp = 0;
    private int maxDays = 3;

    private float lat = 43.323065f, lon = -1.9284507f;
    private String place;

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

    public void refresh(View view, Runnable callback) {
        final LocalDateTime now = LocalDateTime.now().withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);

        if (Build.BRAND.equalsIgnoreCase("huami")) {
            refreshGeocodeAmazfit(view.getContext());
            refreshSunriseSunsetAmazfit(view.getContext(), now);
            refreshForecastAmazfit(view.getContext(), now, callback);

        } else {
            refreshGeocode();
            refreshSunriseSunset(now);
            refreshForecast(now);
            callback.run();
        }
    }

    private void refreshGeocodeAmazfit(final Context context) {
        HttpUrl url = getUrlForGeocode();

        final LocalURLConnection conn = new LocalURLConnection();
        conn.setUrl(url.url());
        conn.addHeader("Accept", "application/json");
        conn.addHeader("User-Agent", "curl/7.68.0");

        new LocalHTTPRequest(context, conn, new LocalHTTPResponse() {
            @Override
            public void onResult(HttpURLConnection response) {
                try (InputStream in = response.getInputStream()) {
                    parseGeocode(IOUtils.toString(in));
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onConnectError() {

            }

            @Override
            public void onTimeout() {

            }
        });
    }

    private void refreshGeocode() {
        HttpUrl url = getUrlForGeocode();

        Request requesthttp = new Request.Builder()
                .addHeader("accept", "application/json")
                .addHeader("User-Agent", "curl/7.68.0")
                .url(url)
                .build();

        try (Response response = client.newCall(requesthttp).execute()) {
            parseGeocode(response.body().string());
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
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

    private void parseGeocode(String body) throws JSONException, IOException {
        JSONObject object = new JSONObject(body);
        JSONObject address = object.getJSONObject("address");
        this.place = address.has("city") ? address.getString("city") : address.getString("town");
    }

    private void refreshSunriseSunsetAmazfit(final Context context, LocalDateTime now) {
        HttpUrl url = getUrlForSunriseSunset(now);

        final LocalURLConnection conn = new LocalURLConnection();
        conn.setUrl(url.url());
        conn.addHeader("Accept", "application/json");
        conn.addHeader("Accept-Encoding", "gzip");
        conn.addHeader("User-Agent", "curl/7.68.0");

        new LocalHTTPRequest(context, conn, new LocalHTTPResponse() {
            @Override
            public void onResult(HttpURLConnection response) {
                try (InputStream in = new GZIPInputStream(response.getInputStream())) {
                    parseSunriseSunset(IOUtils.toString(in));
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onConnectError() {

            }

            @Override
            public void onTimeout() {

            }
        });
    }

    private void refreshSunriseSunset(LocalDateTime now) {
        HttpUrl url = getUrlForSunriseSunset(now);

        Request requesthttp = new Request.Builder()
                .addHeader("accept", "application/json")
                .addHeader("User-Agent", "curl/7.68.0")
                .url(url)
                .build();

        try (Response response = client.newCall(requesthttp).execute()) {
            parseSunriseSunset(response.body().string());

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    private void parseSunriseSunset(String body) throws JSONException, IOException {
        JSONObject object = new JSONObject(body);
        JSONArray time = object.getJSONObject("location").getJSONArray("time");

        this.sunraiseSunsets.clear();

        for (int i = 0; i < time.length(); i++) {
            JSONObject aDay = time.getJSONObject(i);

            if (!aDay.has("sunrise")) continue;

            this.sunraiseSunsets.add(new SunraiseSunset(aDay));
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

    private void refreshForecastAmazfit(final Context context, final LocalDateTime now, final Runnable callback) {
        HttpUrl url = getUrlForForecast();

        final LocalURLConnection conn = new LocalURLConnection();
        conn.setUrl(url.url());
        conn.addHeader("Accept", "application/json");
        conn.addHeader("Accept-Encoding", "gzip");
        conn.addHeader("User-Agent", "curl/7.68.0");

        new LocalHTTPRequest(context, conn, new LocalHTTPResponse() {
            @Override
            public void onResult(HttpURLConnection response) {
                try (InputStream in = new GZIPInputStream(response.getInputStream())) {
                    parseForecast(now, IOUtils.toString(in));

                    callback.run();
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onConnectError() {

            }

            @Override
            public void onTimeout() {

            }
        });
    }

    private void refreshForecast(LocalDateTime now) {
        HttpUrl url = getUrlForForecast();

        Request requesthttp = new Request.Builder()
                .addHeader("accept", "application/json")
                .addHeader("User-Agent", "curl/7.68.0")
                .url(url)
                .build();

        try (Response response = client.newCall(requesthttp).execute()) {
            parseForecast(now, response.body().string());
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    private void parseForecast(LocalDateTime now, String body) throws JSONException, IOException {
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
}

