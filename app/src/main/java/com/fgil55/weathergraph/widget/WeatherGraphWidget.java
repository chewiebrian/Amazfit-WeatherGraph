package com.fgil55.weathergraph.widget;

import android.app.Service;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.provider.Settings;
import android.text.TextPaint;
import android.util.Log;

import com.fgil55.weathergraph.AbstractWatchFace;
import com.fgil55.weathergraph.R;
import com.fgil55.weathergraph.data.Battery;
import com.fgil55.weathergraph.data.CustomData;
import com.fgil55.weathergraph.data.DataType;
import com.fgil55.weathergraph.data.HeartRate;
import com.fgil55.weathergraph.data.MultipleWatchDataListener;
import com.fgil55.weathergraph.data.Steps;
import com.fgil55.weathergraph.util.Utility;
import com.fgil55.weathergraph.weather.WeatherData;
import com.fgil55.weathergraph.resource.ResourceManager;
import com.fgil55.weathergraph.weather.WeatherRenderer;
import com.huami.watch.watchface.util.Util;
import com.ingenic.iwds.slpt.view.core.SlptLayout;
import com.ingenic.iwds.slpt.view.core.SlptLinearLayout;
import com.ingenic.iwds.slpt.view.core.SlptPictureView;
import com.ingenic.iwds.slpt.view.core.SlptViewComponent;
import com.ingenic.iwds.slpt.view.digital.SlptDayHView;
import com.ingenic.iwds.slpt.view.digital.SlptDayLView;
import com.ingenic.iwds.slpt.view.digital.SlptHourHView;
import com.ingenic.iwds.slpt.view.digital.SlptHourLView;
import com.ingenic.iwds.slpt.view.digital.SlptMinuteHView;
import com.ingenic.iwds.slpt.view.digital.SlptMinuteLView;
import com.ingenic.iwds.slpt.view.digital.SlptMonthHView;
import com.ingenic.iwds.slpt.view.digital.SlptMonthLView;
import com.ingenic.iwds.slpt.view.digital.SlptTimeView;
import com.ingenic.iwds.slpt.view.digital.SlptWeekView;
import com.ingenic.iwds.slpt.view.utils.SimpleFile;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.joda.time.LocalDateTime;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static android.content.ContentValues.TAG;
import static android.content.Context.BATTERY_SERVICE;

public class WeatherGraphWidget extends DigitalClockWidget implements MultipleWatchDataListener {

    private final WeatherRenderer renderer = new WeatherRenderer();
    private Service service;
    private Context context;
    private Paint clear = new Paint();
    final TextPaint timePaint = new TextPaint();
    private Bitmap heartRateBmp;
    private Bitmap batteryBmp;
    private Bitmap phoneBatteryBmp;
    private Bitmap notificationsBmp;

    private Steps steps = new Steps(1, 1000);
    private HeartRate heartRate = new HeartRate(60);
    private Battery batteryData = new Battery(50, 100);
    private CustomData customData = new CustomData("{\"notifications\":1,\"phoneBattery\":50}");

    public void setContext(Context context) {
        this.context = context;
        if (this.heartRateBmp == null) {
            this.heartRateBmp = Util.decodeImage(context.getResources(), "heartrate.png");
            this.batteryBmp = Util.decodeImage(context.getResources(), "battery.png");
            this.phoneBatteryBmp = Util.decodeImage(context.getResources(), "phone_battery.png");
            this.notificationsBmp = Util.decodeImage(context.getResources(), "notifications.png");
        }

        timePaint.setAntiAlias(true);
        timePaint.setColor(Color.WHITE);
//        timePaint.setTypeface(Typeface.DEFAULT_BOLD);
        timePaint.setTypeface(ResourceManager.getTypeFace(context.getResources(), ResourceManager.Font.Bold));
        timePaint.setTextAlign(Paint.Align.CENTER);
        timePaint.setTextSize(90.0f);
    }

    @Override
    public void init(Service service) {
        clear.setColor(Color.BLACK);
        clear.setStyle(Paint.Style.FILL);

        this.service = service;
        setContext(service.getApplicationContext());
    }


    public void refreshSlpt(boolean redraw) {
        if (this.service instanceof AbstractWatchFace) {
            ((AbstractWatchFace) this.service).restartSlpt(redraw);
        }
    }

    @Override
    public void onDrawDigital(Canvas canvas, float width, float height, float centerX, float centerY, int seconds, int minutes, int hours, int year, int month, int day, int week, int ampm) {
        clear(canvas);

        updateLatLon();

        final LocalDateTime now = new LocalDateTime(year, month, day, hours, minutes, seconds);
        boolean needsRefresh = WeatherData.INSTANCE.refresh(context, LocalDateTime.now());

        if (WeatherData.INSTANCE.isEmpty()) {
            drawNoData(canvas);
        } else {
            renderer.render(canvas, WeatherData.INSTANCE, now);
        }

        drawDate(canvas, now);
        drawClock(canvas, now);
        drawStepsHearthRate(canvas);
        drawBattery(canvas);
        drawPhoneData(canvas);
    }

    private void updateLatLon() {
        try {
            // Get ALL data from system
            final String weatherInfoJson = Settings.System.getString(this.service.getApplicationContext().getContentResolver(), "WeatherInfo");

            // Extract data from JSON
            JSONObject weather_data = new JSONObject(weatherInfoJson);
            if (weather_data.has("lat") && weather_data.has("lon")) {
                float lat = (float) weather_data.getDouble("lat");
                float lon = (float) weather_data.getDouble("lon");

                WeatherData.INSTANCE.setLat(lat);
                WeatherData.INSTANCE.setLon(lon);
            }
        } catch (Throwable ignore) {
        }
    }

    private void updateCustomData(Service service) {
        this.customData = new CustomData(Settings.System.getString(service.getContentResolver(), "CustomWatchfaceData"));
    }

    private void updateBattery(Service service) {
        final BatteryManager bm = (BatteryManager) service.getApplicationContext().getSystemService(BATTERY_SERVICE);
        this.batteryData = new Battery(bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY), 100);
    }

    private void clear(Canvas canvas) {
        canvas.drawRect(0f, 0f, canvas.getWidth(), canvas.getHeight(), clear);
    }

    private void drawNoData(Canvas canvas) {
        final Paint noDataPaint = new Paint();
        noDataPaint.setAntiAlias(true);
        noDataPaint.setColor(Color.WHITE);
        noDataPaint.setTypeface(Typeface.DEFAULT_BOLD);
        noDataPaint.setTextSize(18.0f);
        noDataPaint.setTextAlign(Paint.Align.CENTER);
        noDataPaint.setShadowLayer(0.01f, 2, 2, Color.BLACK);
        canvas.drawText("No data", canvas.getWidth() / 2, canvas.getHeight() / 6, noDataPaint);
    }

    @NotNull
    private Bitmap convertBitmapToGrayscale(Bitmap bitmap) {
        Bitmap bw = Bitmap.createBitmap(bitmap);
        Canvas bwCanvas = new Canvas(bw);
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0f);
        Paint bwPaint = new Paint();
        bwPaint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        bwCanvas.drawBitmap(bitmap, 0, 0, bwPaint);
        return bw;
    }

    private void drawDate(Canvas canvas, LocalDateTime now) {
        final String dateString = now.toString("E d MMM").toLowerCase().replaceAll("\\.", "");
        TextPaint datePaint = new TextPaint();
        datePaint.setAntiAlias(true);
        datePaint.setColor(Color.WHITE);
        datePaint.setTypeface(Typeface.DEFAULT_BOLD);
        datePaint.setTextAlign(Paint.Align.CENTER);
        datePaint.setTextSize(20.0f);

        canvas.drawText(dateString, canvas.getWidth() / 2, canvas.getHeight() - 112, datePaint);
    }

    private void drawClock(Canvas canvas, LocalDateTime now) {
        final String timeString = now.getSecondOfMinute() % 2 > 0 ? now.toString("H:mm") : now.toString("H mm");
        drawClockStr(canvas, timeString);
    }

    private void drawClockStr(Canvas canvas, String str) {
        canvas.drawText(str, canvas.getWidth() / 2, canvas.getHeight() - 40, timePaint);
    }

    private void drawStepsHearthRate(Canvas canvas) {
        TextPaint stepsHearthRatePaint = new TextPaint();
        stepsHearthRatePaint.setAntiAlias(true);
        stepsHearthRatePaint.setColor(Color.WHITE);
        stepsHearthRatePaint.setTypeface(Typeface.DEFAULT_BOLD);
        stepsHearthRatePaint.setTextAlign(Paint.Align.CENTER);
        stepsHearthRatePaint.setTextSize(20.0f);

        String message = String.format("%s / %s", heartRate.getHeartRate(), steps.getSteps());
        canvas.drawText(message, canvas.getWidth() / 2, canvas.getHeight() - 18, stepsHearthRatePaint);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColorFilter(new LightingColorFilter(Color.RED, 1));
        canvas.drawBitmap(heartRateBmp, (canvas.getWidth() / 2) - stepsHearthRatePaint.measureText(message), canvas.getHeight() - 36, paint);
    }

    private void drawBattery(Canvas canvas) {
        TextPaint batteryPaint = new TextPaint();
        batteryPaint.setAntiAlias(true);
        batteryPaint.setColor(Color.WHITE);
        batteryPaint.setTypeface(Typeface.DEFAULT_BOLD);
        batteryPaint.setTextAlign(Paint.Align.CENTER);
        batteryPaint.setTextSize(16.0f);

        String text = this.batteryData.getLevel() * 100 / this.batteryData.getScale() + "%";
        canvas.drawText(text, canvas.getWidth() / 6, canvas.getHeight() - 100, batteryPaint);
        canvas.drawBitmap(batteryBmp, (canvas.getWidth() / 6) - batteryPaint.measureText(text) - 4, canvas.getHeight() - 116, new Paint());
    }

    private void drawPhoneData(Canvas canvas) {
        TextPaint phoneDataPaint = new TextPaint();
        phoneDataPaint.setAntiAlias(true);
        phoneDataPaint.setColor(Color.WHITE);
        phoneDataPaint.setTypeface(Typeface.DEFAULT_BOLD);
        phoneDataPaint.setTextAlign(Paint.Align.CENTER);
        phoneDataPaint.setTextSize(16.0f);

        int x = ((5 * canvas.getWidth()) / 6) + 15;
        if (StringUtils.isNotBlank(customData.phoneBattery)) {
            String text = customData.phoneBattery + "%";
            canvas.drawText(text, x, canvas.getHeight() - 100, phoneDataPaint);
            canvas.drawBitmap(phoneBatteryBmp, x - 32, canvas.getHeight() - 116, new Paint());
        }

        if (StringUtils.isNotBlank(customData.notifications) && !StringUtils.equalsIgnoreCase(customData.notifications, "0")) {
            String text = customData.notifications;
            canvas.drawText(text, x, canvas.getHeight() - 80, phoneDataPaint);
            canvas.drawBitmap(notificationsBmp, x - 32, canvas.getHeight() - 96, new Paint());
        }

    }

    @Override
    public List<DataType> getDataTypes() {
        return Arrays.asList(DataType.BATTERY, DataType.HEART_RATE, DataType.STEPS, DataType.CUSTOM);
    }

    @Override
    public void onDataUpdate(DataType type, Object value) {
        boolean refresh = false;

        switch (type) {
            case BATTERY:
                this.batteryData = (Battery) value;
                refresh = true;
                break;
            case HEART_RATE:
                this.heartRate = (HeartRate) value;
                break;
            case STEPS:
                this.steps = (Steps) value;
                break;
            case CUSTOM:
                this.customData = (CustomData) value;
                refresh = true;
                break;
            default:
                return;
        }

        if (refresh) refreshSlpt(true);
    }

    @Override
    public List<SlptViewComponent> buildSlptViewComponent(Service service) {
        setContext(service.getApplicationContext());
        updateBattery(service);
        updateCustomData(service);

        SlptPictureView canvas = new SlptPictureView();
        canvas.setImagePicture(generateBitmap());

        SlptLinearLayout hourLayout = new SlptLinearLayout();
        hourLayout.add(new SlptHourHView());
        hourLayout.add(new SlptHourLView());
        Utility.setStringPictureArrayForAll(hourLayout, SlptTimeView.digital_nums);

        SlptLinearLayout minuteLayout = new SlptLinearLayout();
        minuteLayout.add(new SlptMinuteHView());
        minuteLayout.add(new SlptMinuteLView());
        Utility.setStringPictureArrayForAll(minuteLayout, SlptTimeView.digital_nums);

        Typeface timeTypeFace = ResourceManager.getTypeFace(service.getResources(), ResourceManager.Font.Bold);

        hourLayout.setTextAttrForAll(
                service.getResources().getDimension(R.dimen.malvarez_time_font_size),
                -1,
                timeTypeFace);
        minuteLayout.setTextAttrForAll(
                service.getResources().getDimension(R.dimen.malvarez_time_font_size),
                -1,
                timeTypeFace);
        hourLayout.setStart(
                (int) service.getResources().getDimension(R.dimen.malvarez_time_hour_left_slpt),
                (int) service.getResources().getDimension(R.dimen.malvarez_time_hour_top_slpt));
        minuteLayout.setStart(
                (int) service.getResources().getDimension(R.dimen.malvarez_time_minute_left_slpt),
                (int) service.getResources().getDimension(R.dimen.malvarez_time_minute_top_slpt));

        return Arrays.asList(canvas, hourLayout, minuteLayout);
    }

    private byte[] generateBitmap() {
        final Bitmap bitmap = Bitmap.createBitmap(320, 300, Bitmap.Config.RGB_565);
        final LocalDateTime now = LocalDateTime.now();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();) {
            Canvas canvas = new Canvas(bitmap);
            renderer.render(canvas, WeatherData.INSTANCE, now);
            drawDate(canvas, now);
            drawBattery(canvas);
            drawPhoneData(canvas);
            drawClockStr(canvas, ":");

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);

            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            bitmap.recycle();
        }
    }

    @Override
    public List<SlptViewComponent> buildSlptViewComponent(Service service, boolean better_resolution) {
        return Collections.emptyList();
    }
}
