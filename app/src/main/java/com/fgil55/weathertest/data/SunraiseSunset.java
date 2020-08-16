package com.fgil55.weathertest.data;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONException;
import org.json.JSONObject;

public class SunraiseSunset {
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
}
