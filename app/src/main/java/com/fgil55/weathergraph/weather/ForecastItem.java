package com.fgil55.weathergraph.weather;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.joda.time.LocalDateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

public class ForecastItem implements Serializable {

    private final LocalDateTime time;
    private final float temp;
    private final float cloudArea;
    private final int cloudGroup;
    private final float precipitation;
    private final float humidity;
    private final int windDirection;
    private final float windSpeed;

    public ForecastItem(LocalDateTime time, float temp, float cloudArea, int cloudGroup, float precipitation, float humidity, int windDirection, float windSpeed, float uvIndex) {
        this.time = time;
        this.temp = temp;
        this.cloudArea = cloudArea;
        this.cloudGroup = cloudGroup;
        this.precipitation = precipitation;
        this.humidity = humidity;
        this.windDirection = windDirection;
        this.windSpeed = windSpeed;
    }

    public ForecastItem(JSONObject item, AtomicInteger cloudGroupCounter) throws JSONException {
        final JSONObject data = item.getJSONObject("data");
        final JSONObject details = data.getJSONObject("instant").getJSONObject("details");
        this.time = ISODateTimeFormat.dateTimeNoMillis().parseDateTime(item.getString("time")).toLocalDateTime();
        this.temp = (float) details.getDouble("air_temperature");
        this.windDirection = details.getInt("wind_from_direction");
        this.windSpeed = (float)details.getDouble("wind_speed");
        this.humidity = (float) details.getDouble("relative_humidity");
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

    public float getHumidity() {
        return humidity;
    }

    public int getWindDirection() {
        return windDirection;
    }

    public float getWindSpeed() {
        return windSpeed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        ForecastItem that = (ForecastItem) o;

        return new EqualsBuilder()
                .append(temp, that.temp)
                .append(cloudArea, that.cloudArea)
                .append(cloudGroup, that.cloudGroup)
                .append(precipitation, that.precipitation)
                .append(time, that.time)
                .append(humidity, that.humidity)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(time)
                .append(temp)
                .append(humidity)
                .append(cloudArea)
                .append(cloudGroup)
                .append(precipitation)
                .toHashCode();
    }
}
