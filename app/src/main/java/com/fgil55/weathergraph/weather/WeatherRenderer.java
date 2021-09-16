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

import com.fgil55.weathergraph.util.DateUtils;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class WeatherRenderer {

    final float widgetWidth = 320f;
    final float widgetHeight = 150f; //300 / 2
    final float maxHours = WeatherService.INSTANCE.getCurrentData().getMaxDays() * 24;
    final float pixelsPerHour = BigDecimal.valueOf((widgetWidth / maxHours) - 0.2).setScale(1, RoundingMode.DOWN).floatValue();
    final float paddingX = BigDecimal.valueOf((widgetWidth - (pixelsPerHour * maxHours)) / 2.0).setScale(1, RoundingMode.DOWN).floatValue();

    final float tempMinY = widgetHeight - 8;
    final float tempMaxY = (widgetHeight * 2) / 3;
    final float tempDeltaY = tempMinY - tempMaxY;

    final float cloudY = (widgetHeight / 4) * 2;

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
    final Paint humidityPaintLine = new Paint();
    final Paint windPaintLine = new Paint();
    final Paint cloudsPaint = new Paint();
    final Paint cloudsLinePaint = new Paint();
    final TextPaint minMaxTempPaint = new TextPaint();
    final Paint currentConditionsPaint = new Paint();
    final Paint clear = new Paint();

    final Paint precipPaint = new Paint();
    final Paint weakPrecipLinePaint = new Paint();
    final Paint strongPrecipLinePaint = new Paint();

    final Paint conditionSunPaint;
    final Paint conditionSunPaintEven;
    final Paint conditionSunPartlyPaint;
    final Paint conditionSunPartlyPaintEven;
    final Paint conditionLightRainPaint;
    final Paint conditionLightRainPaintEven;
    final Paint conditionHeavyRainPaint;
    final Paint conditionHeavyRainPaintEven;


    float dateToX(LocalDateTime date, LocalDateTime now) {
        LocalDateTime max = now.plusDays(WeatherService.INSTANCE.getCurrentData().getMaxDays());
        if (date.isBefore(now)) return paddingX;
        if (date.isAfter(max)) return widgetWidth - paddingX;

        return paddingX + (DateUtils.getHoursOfDifference(date, now) * pixelsPerHour);
    }


    float tempToY(float temp, WeatherData weatherData) {
        float thisTempDist = (temp - weatherData.getMinTemp());
        if (thisTempDist == 0) return tempMinY;
        return tempMinY - (int) ((thisTempDist * tempDeltaY) / weatherData.getDeltaTemp());
    }

    float cloudToY(float cloudArea, boolean upper) {
        double v = 14 * (cloudArea / 100.0);
        return (float) (upper ? cloudY - v : cloudY + v);
    }

    float humidityToY(float humidity) {
        return tempMinY - (int) ((humidity/100.0) * tempDeltaY);
    }

    float windToY(float wind, WeatherData weatherData) {
        return tempMinY - (int) ((wind / 100.0) * tempDeltaY);
    }

    boolean isOutOfScreen(LocalDateTime date, LocalDateTime now, WeatherData weatherData) {
        return date.isBefore(now) || date.isAfter(now.plusDays(weatherData.getMaxDays()).plusHours(6));
    }

    public WeatherRenderer() {
        clear.setColor(Color.WHITE);
        clear.setStyle(Paint.Style.FILL);

        mPaintDay.setColor(colorDay);
        mPaintNight.setColor(colorNight);

        daySeparatorPaint.setARGB(100, 255, 255, 255);
        daySeparatorPaint.setStyle(Paint.Style.STROKE);
        daySeparatorPaint.setPathEffect(new DashPathEffect(new float[]{2, 4}, 0));

        dayNamesPaint.setAntiAlias(true);
        dayNamesPaint.setColor(Color.parseColor("#555555"));
        dayNamesPaint.setTextSize(14);
        dayNamesPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));

        dayDurationPaint.setColor(Color.parseColor("#aaaaaa"));
        dayDurationPaint.setStrokeWidth(4.0f);
        dayHoursPaint.setColor(Color.parseColor("#aaaaaa"));
        dayHoursPaint.setStrokeWidth(2.0f);

        tempPaint.setColor(Color.parseColor("#ffaa00"));
        tempPaint.setAntiAlias(true);

        tempPaintLine.setColor(Color.   parseColor("#ffffff"));
        tempPaintLine.setStyle(Paint.Style.STROKE);
        tempPaintLine.setAntiAlias(true);
        tempPaintLine.setStrokeWidth(4.0f);
        tempPaintLine.setPathEffect(new CornerPathEffect(10f));
        //tempPaintLine.setShadowLayer(2, 1, 1, Color.parseColor("#ff5f00"));

        humidityPaintLine.setColor(Color.parseColor("#0088ff"));
        humidityPaintLine.setStyle(Paint.Style.STROKE);
        humidityPaintLine.setAntiAlias(true);
        humidityPaintLine.setStrokeWidth(2.0f);
//        humidityPaintLine.setPathEffect(new CornerPathEffect(10f));
        humidityPaintLine.setPathEffect(new DashPathEffect(new float[]{4, 4}, 0));

        windPaintLine.setColor(Color.RED);
        windPaintLine.setStyle(Paint.Style.STROKE);
        windPaintLine.setAntiAlias(true);
        windPaintLine.setStrokeWidth(2.0f);
//        humidityPaintLine.setPathEffect(new CornerPathEffect(10f));
        windPaintLine.setPathEffect(new DashPathEffect(new float[]{4, 4}, 0));

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

        minMaxTempPaint.setColor(Color.WHITE);
        minMaxTempPaint.setAntiAlias(true);
        minMaxTempPaint.setTypeface(Typeface.DEFAULT_BOLD);
        minMaxTempPaint.setTextSize(12.0f);
        minMaxTempPaint.setTextAlign(Paint.Align.CENTER);
        minMaxTempPaint.setShadowLayer(0.01f, 1, 1, Color.BLACK);

        currentConditionsPaint.setAntiAlias(true);
        currentConditionsPaint.setColor(Color.WHITE);
        currentConditionsPaint.setTypeface(Typeface.DEFAULT_BOLD);
        currentConditionsPaint.setTextSize(20.0f);
        currentConditionsPaint.setTextAlign(Paint.Align.CENTER);
        currentConditionsPaint.setShadowLayer(0.01f, 2, 2, Color.BLACK);

        conditionSunPaint = new Paint();
        conditionSunPaint.setAntiAlias(true);
        conditionSunPaint.setAlpha(50);
        conditionSunPaint.setColor(Color.YELLOW);
//        conditionSunPaint.setStyle(Paint.Style.FILL);
        conditionSunPaint.setStrokeWidth(pixelsPerHour);
        conditionSunPaint.setPathEffect(new DashPathEffect(new float[]{2, 2}, 0));

        conditionSunPaintEven = new Paint(conditionSunPaint);
        conditionSunPaintEven.setPathEffect(new DashPathEffect(new float[]{2, 2}, 2));

        conditionSunPartlyPaint = new Paint(conditionSunPaint);
        conditionSunPartlyPaint.setAlpha(25);

        conditionSunPartlyPaintEven = new Paint(conditionSunPaintEven);
        conditionSunPartlyPaintEven.setAlpha(25);

        conditionLightRainPaint = new Paint();
        conditionLightRainPaint.setAntiAlias(true);
        conditionLightRainPaint.setAlpha(40);
        conditionLightRainPaint.setColor(Color.BLUE);
//        conditionSunPaint.setStyle(Paint.Style.FILL);
//        conditionLightRainPaint.setStrokeWidth(pixelsPerHour/2f);
        conditionLightRainPaint.setPathEffect(new DashPathEffect(new float[]{2, 2}, 0));

        conditionLightRainPaintEven = new Paint(conditionLightRainPaint);
        conditionLightRainPaintEven.setPathEffect(new DashPathEffect(new float[]{2, 2}, 2));

        conditionHeavyRainPaint = new Paint(conditionLightRainPaint);
        conditionHeavyRainPaint.setStrokeWidth(pixelsPerHour/2f);

        conditionHeavyRainPaintEven = new Paint(conditionHeavyRainPaint);
        conditionHeavyRainPaintEven.setPathEffect(new DashPathEffect(new float[]{2, 2}, 2));

        precipPaint.setColor(Color.parseColor("#0000ff"));
        precipPaint.setStrokeWidth(3.0f);
        precipPaint.setAlpha(200);
    }

    public void render(Canvas canvas, WeatherData weatherData, LocalDateTime now, boolean clearCanvas) {
        if (weatherData.isEmpty()) {
            return;
        }

        if (clearCanvas) canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), clear);

        for (SunraiseSunset sunraiseSunset : weatherData.getSunraiseSunsets()) {
            LocalDate currentDate = sunraiseSunset.getDate();
            LocalDateTime start = currentDate.toLocalDateTime(LocalTime.parse("00:00:00"));
            LocalDateTime midday = currentDate.toLocalDateTime(LocalTime.parse("12:00:00"));
            LocalDateTime quarterday = currentDate.toLocalDateTime(LocalTime.parse("06:00:00"));
            LocalDateTime threequarterday = currentDate.toLocalDateTime(LocalTime.parse("18:00:00"));
            LocalDateTime midnight = currentDate.plusDays(1).toLocalDateTime(LocalTime.parse("00:00:00"));
            float sunraiseX = dateToX(sunraiseSunset.getSunrise(), now);
            float sunsetX = dateToX(sunraiseSunset.getSunset(), now);
            float startX = dateToX(start, now);
            float midnightX = dateToX(midnight, now);
            float middayX = dateToX(midday, now);
            float quarterdayX = dateToX(quarterday, now);
            float threequarterdayX = dateToX(threequarterday, now);

            float gradientSize = pixelsPerHour * 2;

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

        //clear borders
        canvas.drawRect(0, 0, paddingX, widgetHeight, clear);
        canvas.drawRect(widgetWidth - paddingX, 0, widgetWidth, widgetHeight, clear);

        drawConditions(canvas,weatherData,now);
        drawClouds(canvas, weatherData, now);
        drawTemperature(canvas, weatherData, now);
        drawPrecipitation(canvas, weatherData, now);
        drawHumidity(canvas, weatherData, now);
        drawWind(canvas, weatherData, now);
        drawPlace(canvas, weatherData);
        drawMinMaxTemp(canvas, weatherData, now);
    }

    private Paint getPaintForCondition(ForecastItem.Condition condition, boolean isEven) {
        switch (condition) {
//            case SUN:
//                return isEven ? conditionSunPaintEven : conditionSunPaint;
//            case SUN_PARTLY:
//                return isEven ? conditionSunPartlyPaintEven : conditionSunPartlyPaint;
            case LIGHT_RAIN:
                return isEven ? conditionLightRainPaintEven : conditionLightRainPaint;
            case HEAVY_RAIN:
                return isEven  ? conditionHeavyRainPaintEven : conditionHeavyRainPaint;
            default:
                return null;
        }
    }

    private void drawConditions(Canvas canvas, WeatherData weatherData, LocalDateTime now) {
        if (weatherData.getForecasts().isEmpty()) return;

        AtomicBoolean even = new AtomicBoolean();

        weatherData.getForecasts().stream()
                .filter(forecastItem -> !isOutOfScreen(forecastItem.getTime(), now, weatherData))
                .filter(forecastItem -> !forecastItem.getCondition().equals(ForecastItem.Condition.UNKNOWN))
                .forEach(forecastItem -> {
                    final float x = dateToX(forecastItem.getTime(), now);

                    Paint paintForCondition = getPaintForCondition(forecastItem.getCondition(), even.getAndSet(!even.get()));
                    if (paintForCondition != null) canvas.drawLine(x, cloudY, x, widgetHeight, paintForCondition);
                });
    }

    private void drawHumidity(Canvas canvas, WeatherData weatherData, LocalDateTime now) {
        if (weatherData.getForecasts().isEmpty()) return;

        final Path humidityLine = new Path();

        humidityLine.moveTo(paddingX, humidityToY(weatherData.getForecasts().get(0).getHumidity()));

        weatherData.getForecasts().stream()
                .filter(forecastItem -> !isOutOfScreen(forecastItem.getTime(), now, weatherData))
                .forEach(forecastItem -> {
                    final float x = dateToX(forecastItem.getTime(), now);
                    final float y = humidityToY(forecastItem.getHumidity());
                    humidityLine.lineTo(x, y);
                });

        canvas.drawPath(humidityLine, humidityPaintLine);
    }

    private void drawWind(Canvas canvas, WeatherData weatherData, LocalDateTime now) {
        if (weatherData.getForecasts().isEmpty()) return;

        final Path windLine = new Path();

        windLine.moveTo(paddingX, windToY(weatherData.getForecasts().get(0).getWindSpeed(), weatherData));

        weatherData.getForecasts().stream()
                .filter(forecastItem -> !isOutOfScreen(forecastItem.getTime(), now, weatherData))
                .forEach(forecastItem -> {
                    final float x = dateToX(forecastItem.getTime(), now);
                    final float y = windToY(forecastItem.getWindSpeed(), weatherData);
                    windLine.lineTo(x, y);
                });

        canvas.drawPath(windLine, windPaintLine);
    }

    private void drawMinMaxTemp(Canvas canvas, WeatherData weatherData, LocalDateTime now) {
        for (SunraiseSunset sunraiseSunset : weatherData.getSunraiseSunsets()) {
            final LocalDate currentDate = sunraiseSunset.getDate();
            LocalDateTime startDateTime = currentDate.toLocalDateTime(LocalTime.MIDNIGHT);
            LocalDateTime endDateTime = currentDate.toLocalDateTime(LocalTime.parse("23:59:59"));
            float minTemp = Integer.MAX_VALUE;
            float maxTemp = Integer.MIN_VALUE;
            float minTempX = 0f, maxTempX = 0f;

            for (ForecastItem forecast : weatherData.getForecasts()) {
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
        if (weatherData.getRefreshing().get()) {
            canvas.drawText("Refreshing", widgetWidth / 2, widgetHeight / 3, currentConditionsPaint);
        } else {
            canvas.drawText(StringUtils.abbreviate(weatherData.getCurrentTempAndPlace(), 22), widgetWidth / 2, widgetHeight / 3, currentConditionsPaint);
        }
//        canvas.drawText(WeatherService.INSTANCE.isRefreshing() ? "Updating" : weatherData.getCurrentTempAndPlace(), widgetWidth / 2, widgetHeight / 3, currentConditionsPaint);
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
                    final float x = dateToX(forecastItem.getTime(), now);
                    final float y = tempToY(forecastItem.getTemp(), weatherData);
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
        weatherData.getForecasts().stream()
                .filter(forecastItem -> !isOutOfScreen(forecastItem.getTime(), now, weatherData) && forecastItem.getPrecipitation() > 0f)
                .forEach(forecastItem -> {
                    float x = dateToX(forecastItem.getTime(), now);
                    float y = widgetHeight - Math.min((forecastItem.getPrecipitation() * 20), widgetHeight / 3);
                    canvas.drawLine(x, widgetHeight, x, y, precipPaint);
                });
    }

}
