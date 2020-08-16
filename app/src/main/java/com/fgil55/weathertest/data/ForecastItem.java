package com.fgil55.weathertest.data;

import org.joda.time.LocalDateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicInteger;

public class ForecastItem {

    private final LocalDateTime time;
    private final float temp;
    private final float cloudArea;
    private final int cloudGroup;
    private final float precipitation;

    public ForecastItem(LocalDateTime time, float temp, float cloudArea, int cloudGroup, float precipitation) {
        this.time = time;
        this.temp = temp;
        this.cloudArea = cloudArea;
        this.cloudGroup = cloudGroup;
        this.precipitation = precipitation;
    }

    public ForecastItem(JSONObject item, AtomicInteger cloudGroupCounter) throws JSONException {
        final JSONObject data = item.getJSONObject("data");
        final JSONObject details = data.getJSONObject("instant").getJSONObject("details");
        this.time = ISODateTimeFormat.dateTimeNoMillis().parseDateTime(item.getString("time")).toLocalDateTime();
        this.temp = (float) details.getDouble("air_temperature");
        this.cloudArea = (float) details.getDouble("cloud_area_fraction");
        this.cloudGroup = this.cloudArea > 0f ? cloudGroupCounter.get() : cloudGroupCounter.incrementAndGet();
        if (data.has("next_1_hours")) {
            final JSONObject detailsNextHour = data.getJSONObject("next_1_hours").getJSONObject("details");
            this.precipitation = (float) detailsNextHour.getDouble("precipitation_amount");
        } else {
            this.precipitation = 0.0f;
        }
    }

    public LocalDateTime getTime() {
        return time;
    }

    public float getTemp() {
        return temp;
    }

    public float getCloudArea() {
        return cloudArea;
    }

    public int getCloudGroup() {
        return cloudGroup;
    }

    public float getPrecipitation() {
        return precipitation;
    }
}
