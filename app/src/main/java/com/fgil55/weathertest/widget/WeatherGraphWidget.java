package com.fgil55.weathertest.widget;

import android.app.Service;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.TextPaint;

import com.fgil55.weathertest.data.ForecastItem;
import com.fgil55.weathertest.data.SunraiseSunset;
import com.fgil55.weathertest.data.WeatherData;
import com.fgil55.weathertest.resource.ResourceManager;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class WeatherGraphWidget implements Widget {

    WeatherData weatherData = WeatherData.INSTANCE;
    private final Context context;
    final int widgetWidth = 320;
    final int widgetHeight = 300 / 2;
    final int maxHours = weatherData.getMaxDays() * 24;
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

    public WeatherGraphWidget(Context context) {
        this.context = context;
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

    @Override
    public int getX() {
        return 0;
    }

    @Override
    public int getY() {
        return 0;
    }

    @Override
    public void setX(int x) {

    }

    @Override
    public void setY(int y) {

    }

    @Override
    public void init(Service service) {

    }

    private LocalDateTime getNow() {
        return LocalDateTime.now();
    }

    int dateToX(LocalDateTime date) {
        LocalDateTime now = getNow();
        LocalDateTime max = now.plusDays(weatherData.getMaxDays());
        if (date.isBefore(now)) return paddingX;
        if (date.isAfter(max)) return widgetWidth - paddingX;

        return paddingX + (Period.fieldDifference(now, date).toStandardHours().getHours() * pixelsPerHour);
    }


    int tempToY(float temp) {
        float thisTempDist = (temp - weatherData.getMinTemp());
        if (thisTempDist == 0) return tempMinY;
        return tempMinY - (int) ((thisTempDist * tempDeltaY) / weatherData.getDeltaTemp());
    }

    float cloudToY(float cloudArea, boolean upper) {
        double v = 14 * (cloudArea / 100.0);
        return (float) (upper ? cloudY - v : cloudY + v);
    }

    boolean isOutOfScreen(LocalDateTime date) {
        LocalDateTime now = getNow().plusDays(weatherData.getMaxDays()).plusHours(5);
        return date.isBefore(getNow()) || date.isAfter(now);
    }

    @Override
    public void draw(Canvas canvas, float width, float height, float centerX, float centerY) {
        if (weatherData.isEmpty()) return;

        LocalDateTime now = getNow();

        for (int i = 0; i <= weatherData.getMaxDays(); i++) {
            SunraiseSunset sunraiseSunset = weatherData.getSunraiseSunsets().get(i);
            LocalDate currentDate = sunraiseSunset.getDate();
            LocalDateTime start = currentDate.toLocalDateTime(LocalTime.parse("00:00:00"));
            LocalDateTime midday = currentDate.toLocalDateTime(LocalTime.parse("12:00:00"));
            LocalDateTime quarterday = currentDate.toLocalDateTime(LocalTime.parse("06:00:00"));
            LocalDateTime threequarterday = currentDate.toLocalDateTime(LocalTime.parse("18:00:00"));
            LocalDateTime midnight = currentDate.plusDays(i + 1).toLocalDateTime(LocalTime.parse("00:00:00"));
            int sunraiseX = dateToX(sunraiseSunset.getSunrise());
            int sunsetX = dateToX(sunraiseSunset.getSunset());
            int startX = dateToX(start);
            int midnightX = dateToX(midnight);
            int middayX = dateToX(midday);
            int quarterdayX = dateToX(quarterday);
            int threequarterdayX = dateToX(threequarterday);

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

            if (!isOutOfScreen(sunraiseSunset.getSunrise())) {
                canvas.drawRect(sunraiseX - gradientSize, 0, sunraiseX + gradientSize, widgetHeight, mPaintDawn);
                canvas.drawRect(startX, 0, startX, widgetHeight, daySeparatorPaint);
            }

            if (!isOutOfScreen(sunraiseSunset.getSunset()))
                canvas.drawRect(sunsetX - gradientSize, 0, Math.min(sunsetX + gradientSize, widgetWidth - paddingX), widgetHeight, mPaintDusk);

            // draw days of week names
            if (!isOutOfScreen(start))
                canvas.drawText(getDayName(start), startX, widgetHeight + dayNamesPaint.getTextSize(), dayNamesPaint);

            // draw daylight duration line
            canvas.drawLine(sunraiseX, widgetHeight + dayDurationPaint.getStrokeWidth(), sunsetX, widgetHeight + dayDurationPaint.getStrokeWidth(), dayDurationPaint);

            if (!isOutOfScreen(midday))
                canvas.drawLine(middayX, widgetHeight + dayDurationPaint.getStrokeWidth(), middayX, widgetHeight + dayDurationPaint.getStrokeWidth() + 8, dayHoursPaint);
            if (!isOutOfScreen(quarterday))
                canvas.drawLine(quarterdayX, widgetHeight + dayDurationPaint.getStrokeWidth(), quarterdayX, widgetHeight + dayDurationPaint.getStrokeWidth() + 4, dayHoursPaint);
            if (!isOutOfScreen(threequarterday))
                canvas.drawLine(threequarterdayX, widgetHeight + dayDurationPaint.getStrokeWidth(), threequarterdayX, widgetHeight + dayDurationPaint.getStrokeWidth() + 4, dayHoursPaint);
        }


        drawClouds(canvas);
        drawTemperature(canvas);
        drawPrecipitation(canvas);
        drawPlace(canvas);
        drawMinMaxTemp(canvas);

        drawClock(canvas, now);
    }

    private void drawMinMaxTemp(Canvas canvas) {
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
                        minTempX = dateToX(forecast.getTime());
                    }
                    if (forecast.getTemp() > maxTemp) {
                        maxTemp = forecast.getTemp();
                        maxTempX = dateToX(forecast.getTime());
                    }
                }
            }

            if (!isOutOfScreen(startDateTime)) {
                canvas.drawText(String.valueOf((int) minTemp), minTempX - 4, tempToY(minTemp) - 10, minMaxTempPaint);
            }
            if (!isOutOfScreen(endDateTime)) {
                canvas.drawText(String.valueOf((int) maxTemp), maxTempX - 2, tempToY(maxTemp) - 6, minMaxTempPaint);
            }
        }
    }

    private void drawPlace(Canvas canvas) {
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

    private void drawTemperature(Canvas canvas) {
        if (weatherData.getForecasts().isEmpty()) return;

        final Path tempPath = new Path();
        final Path tempPathLine = new Path();

        tempPath.moveTo(paddingX, widgetHeight);
        tempPathLine.moveTo(paddingX, tempToY(weatherData.getForecasts().get(0).getTemp()));

        weatherData.getForecasts().stream()
                .filter(forecastItem -> !isOutOfScreen(forecastItem.getTime()))
                .forEach(forecastItem -> {
                    final int x = dateToX(forecastItem.getTime());
                    final int y = tempToY(forecastItem.getTemp());
                    tempPath.lineTo(x, y);
                    tempPathLine.lineTo(x, y);
                });

        tempPath.lineTo(widgetWidth - paddingX, widgetHeight);
        tempPath.lineTo(paddingX, widgetHeight);
        tempPath.close();

        canvas.drawPath(tempPath, tempPaint);
        canvas.drawPath(tempPathLine, tempPaintLine);
    }

    private void drawClouds(Canvas canvas) {
        if (weatherData.getForecasts().isEmpty()) return;

        final Path cloudPath = new Path();
        final Path cloudPathLineUpper = new Path();
        final Path cloudPathLineLower = new Path();

        final Collection<List<ForecastItem>> cloudGroups = weatherData.getForecasts().stream()
                .filter(forecastItem -> forecastItem.getCloudArea() > 0f)
                .filter(forecastItem -> !isOutOfScreen(forecastItem.getTime()))
                .collect(Collectors.groupingBy(ForecastItem::getCloudGroup))
                .values();

        for (List<ForecastItem> forecasts : cloudGroups) {
            final ForecastItem first = forecasts.get(0);
            final ForecastItem last = forecasts.get(forecasts.size() - 1);

            cloudPath.moveTo(dateToX(first.getTime()), cloudY);
            cloudPathLineLower.moveTo(dateToX(first.getTime()), cloudY);
            cloudPathLineUpper.moveTo(dateToX(first.getTime()), cloudY);
            forecasts.stream().forEach(forecast -> {
                cloudPath.lineTo(dateToX(forecast.getTime()), cloudToY(forecast.getCloudArea(), true));
                cloudPathLineUpper.lineTo(dateToX(forecast.getTime()), cloudToY(forecast.getCloudArea(), true));
            });
            cloudPath.lineTo(dateToX(last.getTime()), cloudY);
            cloudPathLineUpper.lineTo(dateToX(last.getTime()), cloudY);

            //cloudPath.close();

            cloudPath.moveTo(dateToX(first.getTime()), cloudY);
            forecasts.stream().forEach(forecast -> {
                cloudPath.lineTo(dateToX(forecast.getTime()), cloudToY(forecast.getCloudArea(), false));
                cloudPathLineLower.lineTo(dateToX(forecast.getTime()), cloudToY(forecast.getCloudArea(), false));
            });
            cloudPath.lineTo(dateToX(last.getTime()), cloudY);
            cloudPathLineLower.lineTo(dateToX(last.getTime()), cloudY);

            cloudPath.close();
        }

        canvas.drawPath(cloudPath, cloudsPaint);
        canvas.drawPath(cloudPathLineUpper, cloudsLinePaint);
        canvas.drawPath(cloudPathLineLower, cloudsLinePaint);
        return;
    }

    private void drawPrecipitation(Canvas canvas) {
        if (weatherData.getForecasts().isEmpty()) return;

        final Paint precipPaint = new Paint();
        precipPaint.setColor(Color.parseColor("#0000ff"));
        precipPaint.setStrokeWidth(4.0f);

        weatherData.getForecasts().stream()
                .filter(forecastItem -> !isOutOfScreen(forecastItem.getTime()) && forecastItem.getPrecipitation() > 0f)
                .forEach(forecastItem -> {
                    int x = dateToX(forecastItem.getTime());
                    canvas.drawLine(x, widgetHeight, x, widgetHeight - Math.min((forecastItem.getPrecipitation() * 20), widgetHeight / 6), precipPaint);
                });
    }

    private void drawClock(Canvas canvas, LocalDateTime now) {
        String dateString = now.toString("E d MMM").toLowerCase().replaceAll("\\.", "");
        String timeString = now.toString("H:mm");

        TextPaint datePaint = new TextPaint();
        datePaint.setAntiAlias(true);
        datePaint.setColor(Color.WHITE);
        datePaint.setTypeface(Typeface.DEFAULT_BOLD);
        datePaint.setTextAlign(Paint.Align.CENTER);
        datePaint.setTextSize(20.0f);

        TextPaint timePaint = new TextPaint();
        timePaint.setAntiAlias(true);
        timePaint.setColor(Color.WHITE);
//        timePaint.setTypeface(Typeface.DEFAULT_BOLD);
        timePaint.setTypeface(ResourceManager.getTypeFace(context.getResources(), ResourceManager.Font.Bold));
        timePaint.setTextAlign(Paint.Align.CENTER);
        timePaint.setTextSize(90.0f);

        float dateWidth = datePaint.measureText(dateString);
        float timeWidth = timePaint.measureText(timeString);

        canvas.drawText(dateString, canvas.getWidth() / 2, canvas.getHeight() - 112, datePaint);
        canvas.drawText(timeString, canvas.getWidth() / 2, canvas.getHeight() - 40, timePaint);
    }
}
