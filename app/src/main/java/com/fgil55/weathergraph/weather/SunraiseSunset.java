package com.fgil55.weathergraph.weather;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class SunraiseSunset implements Serializable {
  LocalDate date;

  LocalDateTime sunrise;
  LocalDateTime sunset;

    public SunraiseSunset(LocalDate date, LocalDateTime sunrise, LocalDateTime sunset) {
        this.date = date;
        this.sunrise = sunrise;
        this.sunset = sunset;
    }

    public SunraiseSunset(JSONObject item) throws JSONException {
        this.date = LocalDate.parse(item.getString("date"));
        this.sunrise = ISODateTimeFormat.dateTimeNoMillis().parseLocalDateTime(item.getJSONObject("sunrise").getString("time"));
        this.sunset = ISODateTimeFormat.dateTimeNoMillis().parseLocalDateTime(item.getJSONObject("sunset").getString("time"));
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalDateTime getSunrise() {
        return sunrise;
    }

    public LocalDateTime getSunset() {
        return sunset;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        SunraiseSunset that = (SunraiseSunset) o;

        return new EqualsBuilder()
                .append(date, that.date)
                .append(sunrise, that.sunrise)
                .append(sunset, that.sunset)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(date)
                .append(sunrise)
                .append(sunset)
                .toHashCode();
    }
}
