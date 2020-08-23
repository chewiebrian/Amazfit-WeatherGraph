package com.fgil55.weathergraph.weather;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.TextPaint;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class WeatherRenderer {

    final int widgetWidth = 320;
    final int widgetHeight = 300 / 2;
    final int maxHours = WeatherData.INSTANCE.getMaxDays() * 24;
    final int pixelsPerHour = widgetWidth / maxHours;
    final int paddingX = (widgetWidth - (pixelsPerHour * maxHours)) / 2;

    final int tempMinY = widgetHeight - 8;
    final int tempMaxY = (widgetHeight * 2) / 3;
    final int tempDeltaY = tempMinY - tempMaxY;

    final int cloudY = (widgetHeight / 4) * 2;

    final DateTimeFormatter dayNamesFormatter = DateTimeFormat.forPattern("EE").withLocale(new Locale("es"));

    private int colorNight = Color.parseColor("#0000ff");
    private int colorDay = Color.parseColor("#00aaff");

    final Paint dayNamesPaint = new Paint();
    final Paint dayDurationPaint = new Paint();
    final Paint dayHoursPaint = new Paint();
    final Paint daySeparatorPaint = new Paint();
    final Paint mPaintDay = new Paint();
    final Paint mPaintNight = new Paint();
    final Paint mPaintDawn = new Paint();
    final Paint mPaintDusk = new Paint();
    final Paint tempPaint = new Paint();
    final Paint tempPaintLine = new Paint();
    final Paint cloudsPaint = new Paint();
    final Paint cloudsLinePaint = new Paint();

    final Paint weakPrecipLinePaint = new Paint();
    final Paint strongPrecipLinePaint = new Paint();


    int dateToX(LocalDateTime date, LocalDateTime now) {
        LocalDateTime max = now.plusDays(WeatherData.INSTANCE.getMaxDays());
        if (date.isBefore(now)) return paddingX;
        if (date.isAfter(max)) return widgetWidth - paddingX;

        return paddingX + (Period.fieldDifference(now, date).toStandardHours().getHours() * pixelsPerHour);
    }


    int tempToY(float temp, WeatherData weatherData) {
        float thisTempDist = (temp - weatherData.getMinTemp());
        if (thisTempDist == 0) return tempMinY;
        return tempMinY - (int) ((thisTempDist * tempDeltaY) / weatherData.getDeltaTemp());
    }

    float cloudToY(float cloudArea, boolean upper) {
        double v = 14 * (cloudArea / 100.0);
        return (float) (upper ? cloudY - v : cloudY + v);
    }

    boolean isOutOfScreen(LocalDateTime date, LocalDateTime now, WeatherData weatherData) {
        return date.isBefore(now) || date.isAfter(now.plusDays(weatherData.getMaxDays()).plusHours(5));
    }

    public WeatherRenderer() {
        mPaintDay.setColor(colorDay);
        mPaintNight.setColor(colorNight);

        daySeparatorPaint.setARGB(100, 255, 255, 255);
        daySeparatorPaint.setStyle(Paint.Style.STROKE);
        daySeparatorPaint.setPathEffect(new DashPathEffect(new float[]{2, 4}, 0));

        dayNamesPaint.setAntiAlias(true);
        dayNamesPaint.setColor(Color.parseColor("#aaaaaa"));
        dayNamesPaint.setTextSize(14);
        dayNamesPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));

        dayDurationPaint.setColor(Color.parseColor("#555555"));
        dayDurationPaint.setStrokeWidth(4.0f);
        dayHoursPaint.setColor(Color.parseColor("#555555"));
        dayHoursPaint.setStrokeWidth(2.0f);

        tempPaint.setColor(Color.parseColor("#ffaa00"));
        tempPaint.setAntiAlias(true);

        tempPaintLine.setColor(Color.parseColor("#ffffff"));
        tempPaintLine.setStyle(Paint.Style.STROKE);
        tempPaintLine.setAntiAlias(true);
        tempPaintLine.setStrokeWidth(4.0f);
        tempPaintLine.setPathEffect(new CornerPathEffect(10f));
        //tempPaintLine.setShadowLayer(2, 1, 1, Color.parseColor("#ff5f00"));

        cloudsPaint.setColor(Color.parseColor("#aaaaaa"));
        cloudsLinePaint.setColor(Color.parseColor("#ffffff"));
        cloudsLinePaint.setStyle(Paint.Style.STROKE);
        cloudsLinePaint.setAntiAlias(true);
        cloudsLinePaint.setStrokeWidth(3.0f);
        cloudsLinePaint.setPathEffect(new CornerPathEffect(10f));

        weakPrecipLinePaint.setARGB(100, 255, 255, 255);
        weakPrecipLinePaint.setStyle(Paint.Style.STROKE);
        weakPrecipLinePaint.setStrokeWidth(2.0f);
        weakPrecipLinePaint.setPathEffect(new DashPathEffect(new float[]{1, 4}, 0));

        strongPrecipLinePaint.setARGB(100, 255, 255, 255);
        strongPrecipLinePaint.setStyle(Paint.Style.STROKE);
        strongPrecipLinePaint.setStrokeWidth(2.0f);
        strongPrecipLinePaint.setPathEffect(new DashPathEffect(new float[]{1, 3}, 0));
    }

    public void render(Canvas canvas, WeatherData weatherData, LocalDateTime now) {
        if (weatherData.isEmpty()) {
            return;
        }

        for (int i = 0; i <= weatherData.getMaxDays(); i++) {
            SunraiseSunset sunraiseSunset = weatherData.getSunraiseSunsets().get(i);
            LocalDate currentDate = sunraiseSunset.getDate();
            LocalDateTime start = currentDate.toLocalDateTime(LocalTime.parse("00:00:00"));
            LocalDateTime midday = currentDate.toLocalDateTime(LocalTime.parse("12:00:00"));
            LocalDateTime quarterday = currentDate.toLocalDateTime(LocalTime.parse("06:00:00"));
            LocalDateTime threequarterday = currentDate.toLocalDateTime(LocalTime.parse("18:00:00"));
            LocalDateTime midnight = currentDate.plusDays(i + 1).toLocalDateTime(LocalTime.parse("00:00:00"));
            int sunraiseX = dateToX(sunraiseSunset.getSunrise(), now);
            int sunsetX = dateToX(sunraiseSunset.getSunset(), now);
            int startX = dateToX(start, now);
            int midnightX = dateToX(midnight, now);
            int middayX = dateToX(midday, now);
            int quarterdayX = dateToX(quarterday, now);
            int threequarterdayX = dateToX(threequarterday, now);

            int gradientSize = 10;

            LinearGradient shaderDawn;
            LinearGradient shaderDusk;
            shaderDawn = new LinearGradient(sunraiseX - gradientSize, 0, sunraiseX + gradientSize, 0, new int[]{colorNight, colorDay}, null, Shader.TileMode.CLAMP);
            shaderDusk = new LinearGradient(sunsetX - gradientSize, 0, sunsetX + gradientSize, 0, new int[]{colorDay, colorNight}, null, Shader.TileMode.CLAMP);
            mPaintDawn.setShader(shaderDawn);
            mPaintDusk.setShader(shaderDusk);

            if (startX != sunraiseX)
                canvas.drawRect(startX, 0, sunraiseX, widgetHeight, mPaintNight);
            if (sunraiseX != middayX)
                canvas.drawRect(sunraiseX, 0, middayX, widgetHeight, mPaintDay);
            if (middayX != sunsetX) canvas.drawRect(middayX, 0, sunsetX, widgetHeight, mPaintDay);
            canvas.drawRect(sunsetX, 0, midnightX, widgetHeight, mPaintNight);

            if (!isOutOfScreen(sunraiseSunset.getSunrise(), now, weatherData)) {
                canvas.drawRect(sunraiseX - gradientSize, 0, sunraiseX + gradientSize, widgetHeight, mPaintDawn);
                canvas.drawRect(startX, 0, startX, widgetHeight, daySeparatorPaint);
            }

            if (!isOutOfScreen(sunraiseSunset.getSunset(), now, weatherData))
                canvas.drawRect(sunsetX - gradientSize, 0, Math.min(sunsetX + gradientSize, widgetWidth - paddingX), widgetHeight, mPaintDusk);

            // draw days of week names
            if (!isOutOfScreen(start, now, weatherData))
                canvas.drawText(getDayName(start), startX, widgetHeight + dayNamesPaint.getTextSize(), dayNamesPaint);

            // draw daylight duration line
            canvas.drawLine(sunraiseX, widgetHeight + dayDurationPaint.getStrokeWidth(), sunsetX, widgetHeight + dayDurationPaint.getStrokeWidth(), dayDurationPaint);

            if (!isOutOfScreen(midday, now, weatherData))
                canvas.drawLine(middayX, widgetHeight + dayDurationPaint.getStrokeWidth(), middayX, widgetHeight + dayDurationPaint.getStrokeWidth() + 8, dayHoursPaint);
            if (!isOutOfScreen(quarterday, now, weatherData))
                canvas.drawLine(quarterdayX, widgetHeight + dayDurationPaint.getStrokeWidth(), quarterdayX, widgetHeight + dayDurationPaint.getStrokeWidth() + 4, dayHoursPaint);
            if (!isOutOfScreen(threequarterday, now, weatherData))
                canvas.drawLine(threequarterdayX, widgetHeight + dayDurationPaint.getStrokeWidth(), threequarterdayX, widgetHeight + dayDurationPaint.getStrokeWidth() + 4, dayHoursPaint);
        }


        drawClouds(canvas, weatherData, now);
        drawTemperature(canvas, weatherData, now);
        drawPrecipitation(canvas, weatherData, now);
        drawPlace(canvas, weatherData);
        drawMinMaxTemp(canvas, weatherData, now);
    }

    private void drawMinMaxTemp(Canvas canvas, WeatherData weatherData, LocalDateTime now) {
        TextPaint minMaxTempPaint = new TextPaint();
        minMaxTempPaint.setColor(Color.WHITE);
        minMaxTempPaint.setAntiAlias(true);
        minMaxTempPaint.setTypeface(Typeface.DEFAULT_BOLD);
        minMaxTempPaint.setTextSize(12.0f);
        minMaxTempPaint.setTextAlign(Paint.Align.CENTER);
        minMaxTempPaint.setShadowLayer(0.01f, 1, 1, Color.BLACK);

        for (int i = 0; i <= weatherData.getMaxDays(); i++) {
            final LocalDate currentDate = weatherData.getSunraiseSunsets().get(i).getDate();
            LocalDateTime startDateTime = currentDate.toLocalDateTime(LocalTime.MIDNIGHT);
            LocalDateTime endDateTime = currentDate.toLocalDateTime(LocalTime.parse("23:59:59"));
            float minTemp = Integer.MAX_VALUE;
            float maxTemp = Integer.MIN_VALUE;
            float minTempX = 0f, maxTempX = 0f;

            for (int j = 0; j < weatherData.getForecasts().size(); j++) {
                ForecastItem forecast = weatherData.getForecasts().get(j);
                if (forecast.getTime().isAfter(startDateTime) && forecast.getTime().isBefore(endDateTime)) {
                    if (forecast.getTemp() < minTemp) {
                        minTemp = forecast.getTemp();
                        minTempX = dateToX(forecast.getTime(), now);
                    }
                    if (forecast.getTemp() > maxTemp) {
                        maxTemp = forecast.getTemp();
                        maxTempX = dateToX(forecast.getTime(), now);
                    }
                }
            }

            if (!isOutOfScreen(startDateTime, now, weatherData) && minTempX != paddingX) {
                canvas.drawText(String.valueOf((int) minTemp), minTempX - 4, tempToY(minTemp, weatherData) - 10, minMaxTempPaint);
            }
            if (!isOutOfScreen(endDateTime, now, weatherData) && maxTempX != paddingX) {
                canvas.drawText(String.valueOf((int) maxTemp), maxTempX - 2, tempToY(maxTemp, weatherData) - 6, minMaxTempPaint);
            }
        }
    }

    private void drawPlace(Canvas canvas, WeatherData weatherData) {
        Paint currentConditionsPaint = new Paint();
        currentConditionsPaint.setAntiAlias(true);
        currentConditionsPaint.setColor(Color.WHITE);
        currentConditionsPaint.setTypeface(Typeface.DEFAULT_BOLD);
        currentConditionsPaint.setTextSize(18.0f);
        currentConditionsPaint.setTextAlign(Paint.Align.CENTER);
        currentConditionsPaint.setShadowLayer(0.01f, 2, 2, Color.BLACK);
        canvas.drawText(weatherData.getCurrentTempAndPlace(), widgetWidth / 2, widgetHeight / 3, currentConditionsPaint);
    }

    private String getDayName(LocalDateTime date) {
        return StringUtils.stripAccents(StringUtils.left(StringUtils.upperCase(date.toString(dayNamesFormatter)), 2));
    }

    private void drawTemperature(Canvas canvas, WeatherData weatherData, LocalDateTime now) {
        if (weatherData.getForecasts().isEmpty()) return;

        final Path tempPath = new Path();
        final Path tempPathLine = new Path();

        tempPath.moveTo(paddingX, widgetHeight);
        tempPathLine.moveTo(paddingX, tempToY(weatherData.getForecasts().get(0).getTemp(), weatherData));

        weatherData.getForecasts().stream()
                .filter(forecastItem -> !isOutOfScreen(forecastItem.getTime(), now, weatherData))
                .forEach(forecastItem -> {
                    final int x = dateToX(forecastItem.getTime(), now);
                    final int y = tempToY(forecastItem.getTemp(), weatherData);
                    tempPath.lineTo(x, y);
                    tempPathLine.lineTo(x, y);
                });

        tempPath.lineTo(widgetWidth - paddingX, widgetHeight);
        tempPath.lineTo(paddingX, widgetHeight);
        tempPath.close();

        canvas.drawPath(tempPath, tempPaint);
        canvas.drawPath(tempPathLine, tempPaintLine);
    }

    private void drawClouds(Canvas canvas, WeatherData weatherData, LocalDateTime now) {
        if (weatherData.getForecasts().isEmpty()) return;

        final Path cloudPath = new Path();
        final Path cloudPathLineUpper = new Path();
        final Path cloudPathLineLower = new Path();

        final Collection<List<ForecastItem>> cloudGroups = weatherData.getForecasts().stream()
                .filter(forecastItem -> forecastItem.getCloudArea() > 0f)
                .filter(forecastItem -> !isOutOfScreen(forecastItem.getTime(), now, weatherData))
                .collect(Collectors.groupingBy(ForecastItem::getCloudGroup))
                .values();

        for (List<ForecastItem> forecasts : cloudGroups) {
            final ForecastItem first = forecasts.get(0);
            final ForecastItem last = forecasts.get(forecasts.size() - 1);

            cloudPath.moveTo(dateToX(first.getTime(), now), cloudY);
            cloudPathLineLower.moveTo(dateToX(first.getTime(), now), cloudY);
            cloudPathLineUpper.moveTo(dateToX(first.getTime(), now), cloudY);
            forecasts.stream().forEach(forecast -> {
                cloudPath.lineTo(dateToX(forecast.getTime(), now), cloudToY(forecast.getCloudArea(), true));
                cloudPathLineUpper.lineTo(dateToX(forecast.getTime(), now), cloudToY(forecast.getCloudArea(), true));
            });
            cloudPath.lineTo(dateToX(last.getTime(), now), cloudY);
            cloudPathLineUpper.lineTo(dateToX(last.getTime(), now), cloudY);

            //cloudPath.close();

            cloudPath.moveTo(dateToX(first.getTime(), now), cloudY);
            forecasts.stream().forEach(forecast -> {
                cloudPath.lineTo(dateToX(forecast.getTime(), now), cloudToY(forecast.getCloudArea(), false));
                cloudPathLineLower.lineTo(dateToX(forecast.getTime(), now), cloudToY(forecast.getCloudArea(), false));
            });
            cloudPath.lineTo(dateToX(last.getTime(), now), cloudY);
            cloudPathLineLower.lineTo(dateToX(last.getTime(), now), cloudY);

            cloudPath.close();
        }

        canvas.drawPath(cloudPath, cloudsPaint);
        canvas.drawPath(cloudPathLineUpper, cloudsLinePaint);
        canvas.drawPath(cloudPathLineLower, cloudsLinePaint);
        return;
    }

    private void drawPrecipitation(Canvas canvas, WeatherData weatherData, LocalDateTime now) {
        final Paint precipPaint = new Paint();
        precipPaint.setColor(Color.parseColor("#0000ff"));
        precipPaint.setStrokeWidth(4.0f);

        weatherData.getForecasts().stream()
                .filter(forecastItem -> !isOutOfScreen(forecastItem.getTime(), now, weatherData) && forecastItem.getPrecipitation() > 0f)
                .forEach(forecastItem -> {
                    float x = dateToX(forecastItem.getTime(), now);
                    float y = widgetHeight - Math.min((forecastItem.getPrecipitation() * 20), widgetHeight / 6);
                    canvas.drawLine(x, widgetHeight, x, y, precipPaint);
                });
    }

}
