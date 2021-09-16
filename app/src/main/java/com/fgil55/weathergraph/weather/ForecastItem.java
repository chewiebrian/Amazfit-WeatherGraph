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

    public enum Condition {
        SUN,
        SUN_PARTLY,
        LIGHT_RAIN,
        HEAVY_RAIN,
        SNOW,
        UNKNOWN
    }

    private final LocalDateTime time;
    private final float temp;
    private final float cloudArea;
    private final int cloudGroup;
    private final float precipitation;
    private final float humidity;
    private final int windDirection;
    private final float windSpeed;
    private final float uv;
    private final Condition condition;

    public ForecastItem(LocalDateTime time, float temp, float cloudArea, int cloudGroup, float precipitation, float humidity, int windDirection, float windSpeed, float uvIndex, float uv, Condition condition) {
        this.time = time;
        this.temp = temp;
        this.cloudArea = cloudArea;
        this.cloudGroup = cloudGroup;
        this.precipitation = precipitation;
        this.humidity = humidity;
        this.windDirection = windDirection;
        this.windSpeed = windSpeed;
        this.uv = uv;
        this.condition = condition;
    }

    public ForecastItem(JSONObject item, AtomicInteger cloudGroupCounter) throws JSONException {
        final JSONObject data = item.getJSONObject("data");
        final JSONObject details = data.getJSONObject("instant").getJSONObject("details");
        this.time = ISODateTimeFormat.dateTimeNoMillis().parseDateTime(item.getString("time")).toLocalDateTime();
        this.temp = (float) details.getDouble("air_temperature");
        this.windDirection = details.getInt("wind_from_direction");
        this.windSpeed = (float) details.getDouble("wind_speed") * 3.6f;  // m/s to km/h
        this.humidity = (float) details.getDouble("relative_humidity");
        this.cloudArea = (float) details.getDouble("cloud_area_fraction");
        this.cloudGroup = this.cloudArea > 0f ? cloudGroupCounter.get() : cloudGroupCounter.incrementAndGet();
        this.uv = (details.has("ultraviolet_index_clear_sky")) ? (float) details.getDouble("ultraviolet_index_clear_sky") : 0.0f;

        if (data.has("next_1_hours")) {
            final JSONObject next_1_hours = data.getJSONObject("next_1_hours");
            final JSONObject summaryNextHour = next_1_hours.getJSONObject("summary");
            final JSONObject detailsNextHour = next_1_hours.getJSONObject("details");
            this.precipitation = (float) detailsNextHour.getDouble("precipitation_amount");

            final String symbolCode = summaryNextHour.getString("symbol_code").toLowerCase();

            if (symbolCode.contains("clearsky") && symbolCode.endsWith("day")) {
                this.condition = Condition.SUN;
            } else if (symbolCode.contains("fair")) {
                this.condition = Condition.SUN_PARTLY;
            } else if (symbolCode.contains("rain")) {
                if (symbolCode.contains("light")) {
                    this.condition = Condition.LIGHT_RAIN;
                } else {
                    this.condition = Condition.HEAVY_RAIN;
                }
            } else if (symbolCode.contains("snow")) {
                this.condition = Condition.SNOW;
            } else {
                this.condition = Condition.UNKNOWN;
            }
        } else {
            this.precipitation = 0.0f;
            this.condition = Condition.UNKNOWN;
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

    public Condition getCondition() {
        return condition;
    }

    public float getUv() {
        return uv;
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
                .append(uv, that.uv)
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
                .append(uv)
                .toHashCode();
    }
}
