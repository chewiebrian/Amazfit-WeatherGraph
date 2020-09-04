package com.fgil55.weathergraph.weather;

import android.content.Context;
import android.os.AsyncTask;

import com.fgil55.weathergraph.util.Promise;
import com.kieronquinn.library.amazfitcommunication.Transporter;
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
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.zip.GZIPInputStream;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class YrNoScraper implements WeatherScraper {

    /*
API sunrise
https://api.met.no/weatherapi/sunrise/2.0/.json?date=2020-08-05&days=4&lat=43.297682530610814&lon=-2.0037496089935307&offset=%2B02%3A00

API forecast
https://api.met.no/weatherapi/locationforecast/2.0/complete.json?lat=43.297682530610814&lon=-2.0037496089935307
 */

    private static final DateTimeFormatter zoneFormatter = DateTimeFormat.forPattern("ZZ").withLocale(new Locale("es"));
    private final OkHttpClient client = new OkHttpClient();

    @Override
    public Promise scrap(Context context, WeatherData currentData, LocalDateTime now) {
        if (isAmazfit()) {
            return refreshGeocodeAmazfit(context, currentData)
                    .then(ignore -> refreshSunriseSunsetAmazfit(context, now, currentData))
                    .then(ignore -> refreshForecastAmazfit(context, now, currentData))                    ;
        } else {
            return refreshGeocode(currentData)
                    .then(ignore -> refreshSunriseSunset(now, currentData))
                    .then(ignore -> refreshForecast(now, currentData));
        }
    }

    private boolean isAmazfitTransporterAvailable(Context context) {
        Transporter transporter = Transporter.get(context, "com.kieronquinn.app.amazfitinternetcompanion");
        return transporter.isAvailable();
    }

    private Promise execute(Request requesthttp, WeatherData currentData, BiConsumer<WeatherData,String> postProcess) {
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
            postProcess.accept(currentData, Objects.toString(object));
            return null;
        });
    }

    @NotNull
    private Promise executeAmazfit(Context context, LocalURLConnection conn, WeatherData currentData, BiConsumer<WeatherData, String> post, final boolean isGzip) {
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
            post.accept(currentData, Objects.toString(json));
            return null;
        });
    }

    private Promise refreshGeocodeAmazfit(final Context context, WeatherData currentData) {
        HttpUrl url = getUrlForGeocode(currentData);

        final LocalURLConnection conn = new LocalURLConnection();
        conn.setUrl(url.url());
        conn.addHeader("Accept", "application/json");
        conn.addHeader("User-Agent", "curl/7.68.0");

        return executeAmazfit(context, conn, currentData, this::parseGeocode, false);
    }

    private Promise refreshGeocode(WeatherData currentData) {
        HttpUrl url = getUrlForGeocode(currentData);

        Request requesthttp = new Request.Builder()
                .addHeader("accept", "application/json")
                .addHeader("User-Agent", "curl/7.68.0")
                .url(url)
                .build();

        return execute(requesthttp, currentData, this::parseGeocode);
    }

    @NotNull
    private HttpUrl getUrlForGeocode(WeatherData currentData) {
        //https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=-34.44076&lon=-58.70521
        return new HttpUrl.Builder()
                .scheme("https")
                .host("nominatim.openstreetmap.org")
                .addPathSegment("reverse")
                .addQueryParameter("format", "jsonv2")
                .addQueryParameter("lat", String.valueOf(currentData.getLat()))
                .addQueryParameter("lon", String.valueOf(currentData.getLon()))
                .build();
    }

    private void parseGeocode(WeatherData currentData, String body) {
        try {
            JSONObject object = new JSONObject(body);
            JSONObject address = object.getJSONObject("address");
            currentData.setPlace(address.has("city") ? address.getString("city") : address.getString("town"));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private Promise refreshSunriseSunsetAmazfit(final Context context, LocalDateTime now, WeatherData currentData) {
        HttpUrl url = getUrlForSunriseSunset(currentData, now);

        final LocalURLConnection conn = new LocalURLConnection();
        conn.setUrl(url.url());
        conn.addHeader("Accept", "application/json");
        conn.addHeader("Accept-Encoding", "gzip");
        conn.addHeader("User-Agent", "curl/7.68.0");

        return executeAmazfit(context, conn, currentData, this::parseSunriseSunset, true);
    }

    private Promise refreshSunriseSunset(LocalDateTime now, WeatherData currentData) {
        HttpUrl url = getUrlForSunriseSunset(currentData, now);

        Request requesthttp = new Request.Builder()
                .addHeader("accept", "application/json")
                .addHeader("User-Agent", "curl/7.68.0")
                .url(url)
                .build();

        return execute(requesthttp, currentData, this::parseSunriseSunset);
    }

    private void parseSunriseSunset(WeatherData currentData, String body) {
        try {
            JSONObject object = new JSONObject(body);
            JSONArray time = object.getJSONObject("location").getJSONArray("time");

            currentData.getSunraiseSunsets().clear();

            for (int i = 0; i < time.length(); i++) {
                JSONObject aDay = time.getJSONObject(i);

                if (!aDay.has("sunrise")) continue;

                currentData.getSunraiseSunsets().add(new SunraiseSunset(aDay));
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private HttpUrl getUrlForSunriseSunset(WeatherData currentData, LocalDateTime now) {
        return new HttpUrl.Builder()
                .scheme("https")
                .host("api.met.no")
                .addPathSegment("weatherapi")
                .addPathSegment("sunrise")
                .addPathSegment("2.0")
                .addPathSegment(".json")
                .addQueryParameter("date", now.toLocalDate().toString())
                .addQueryParameter("lat", String.valueOf(currentData.getLat()))
                .addQueryParameter("lon", String.valueOf(currentData.getLon()))
                .addQueryParameter("days", String.valueOf(currentData.getMaxDays() + 1))
                .addQueryParameter("offset", now.toDateTime(DateTimeZone.getDefault()).toString(zoneFormatter))
                .build();
    }

    private Promise refreshForecastAmazfit(final Context context, final LocalDateTime now, WeatherData currentData) {
        HttpUrl url = getUrlForForecast(currentData);

        final LocalURLConnection conn = new LocalURLConnection();
        conn.setUrl(url.url());
        conn.addHeader("Accept", "application/json");
        conn.addHeader("Accept-Encoding", "gzip");
        conn.addHeader("User-Agent", "curl/7.68.0");

        return executeAmazfit(context, conn, currentData, parseForecast(now), true);
    }

    private Promise refreshForecast(LocalDateTime now, WeatherData currentData) {
        HttpUrl url = getUrlForForecast(currentData);

        Request requesthttp = new Request.Builder()
                .addHeader("accept", "application/json")
                .addHeader("User-Agent", "curl/7.68.0")
                .url(url)
                .build();

        return execute(requesthttp, currentData, parseForecast(now));
    }

    private BiConsumer<WeatherData, String> parseForecast(LocalDateTime now) {
        return (WeatherData currentData, String body) -> {
            float minTemp, maxTemp, maxWind;
            try {
                JSONObject object = new JSONObject(body);

                currentData.getForecasts().clear();
                minTemp = Integer.MAX_VALUE;
                maxTemp = Integer.MIN_VALUE;
                maxWind = Integer.MIN_VALUE;

                JSONArray timeseries = object.getJSONObject("properties").getJSONArray("timeseries");
                AtomicInteger cloudGroup = new AtomicInteger(1);

                for (int i = 0; i < timeseries.length(); i++) {
                    JSONObject item = timeseries.getJSONObject(i);
                    ForecastItem forecast = new ForecastItem(item, cloudGroup);
                    if (forecast.getTime().isAfter(now)) {
                        currentData.getForecasts().add(forecast);

//                if (forecast.getTime().isBefore(now.plusDays(maxDays))) {
                        if (forecast.getTemp() < minTemp) minTemp = forecast.getTemp();
                        if (forecast.getTemp() > maxTemp) maxTemp = forecast.getTemp();
                        if (forecast.getWindSpeed() > maxWind) maxWind = forecast.getWindSpeed();
//                }
                    }
                }

                currentData.setMinTemp(minTemp);
                currentData.setMaxTemp(maxTemp);
                currentData.setDeltaTemp(maxTemp - minTemp);
                currentData.setMaxWind(maxWind);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @NotNull
    private HttpUrl getUrlForForecast(WeatherData currentData) {
        return new HttpUrl.Builder()
                .scheme("https")
                .host("api.met.no")
                .addPathSegment("weatherapi")
                .addPathSegment("locationforecast")
                .addPathSegment("2.0")
                .addPathSegment("complete.json")
                .addQueryParameter("lat", String.valueOf(currentData.getLat()))
                .addQueryParameter("lon", String.valueOf(currentData.getLon()))
                .build();
    }

}
