package com.fgil55.weathergraph.widget;

import android.app.Service;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.provider.Settings;
import android.text.TextPaint;

import com.fgil55.weathergraph.AbstractWatchFace;
import com.fgil55.weathergraph.R;
import com.fgil55.weathergraph.data.Battery;
import com.fgil55.weathergraph.data.CustomData;
import com.fgil55.weathergraph.data.DataType;
import com.fgil55.weathergraph.data.HeartRate;
import com.fgil55.weathergraph.data.MultipleWatchDataListener;
import com.fgil55.weathergraph.data.Steps;
import com.fgil55.weathergraph.data.Time;
import com.fgil55.weathergraph.util.Utility;
import com.fgil55.weathergraph.weather.SunraiseSunset;
import com.fgil55.weathergraph.weather.WeatherData;
import com.fgil55.weathergraph.weather.WeatherService;
import com.fgil55.weathergraph.resource.ResourceManager;
import com.fgil55.weathergraph.weather.WeatherRenderer;
import com.huami.watch.watchface.util.Util;
import com.ingenic.iwds.slpt.view.core.SlptLinearLayout;
import com.ingenic.iwds.slpt.view.core.SlptPictureView;
import com.ingenic.iwds.slpt.view.core.SlptViewComponent;
import com.ingenic.iwds.slpt.view.digital.SlptHourHView;
import com.ingenic.iwds.slpt.view.digital.SlptHourLView;
import com.ingenic.iwds.slpt.view.digital.SlptMinuteHView;
import com.ingenic.iwds.slpt.view.digital.SlptMinuteLView;
import com.ingenic.iwds.slpt.view.digital.SlptTimeView;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDateTime;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static android.content.Context.BATTERY_SERVICE;

public class WeatherGraphWidget extends DigitalClockWidget implements MultipleWatchDataListener {

    public static final Paint EMPTY_PAINT = new Paint();
    private final WeatherRenderer renderer = new WeatherRenderer();
    private Service service;
    private Context context;
    private Paint clear = new Paint();
    final Paint noDataPaint = new Paint();
    final TextPaint datePaint = new TextPaint();
    final TextPaint timePaint = new TextPaint();
    final TextPaint secondsPaint = new TextPaint();
    final TextPaint batteryPaint = new TextPaint();
    final TextPaint phoneDataPaint = new TextPaint();
    final TextPaint stepsHearthRatePaint = new TextPaint();
    final TextPaint sunraisePaint = new TextPaint();
    final TextPaint sunsetPaint = new TextPaint();
    final TextPaint uvPaint = new TextPaint();
    final Paint hearthIconPaint = new Paint();
    final Paint uvIconPaint = new Paint();
    private Bitmap heartRateBmp;
    private Bitmap batteryBmp;
    private Bitmap phoneBatteryBmp;
    private Bitmap notificationsBmp;
    private Bitmap sunriseBmp;
    private Bitmap sunsetBmp;
    private Bitmap uvBmp;

    private Steps steps = new Steps(1000, 1000);
    private HeartRate heartRate = new HeartRate(60);
    private Battery batteryData = new Battery(50, 100);
    private CustomData customData = new CustomData("{\"notifications\":1,\"phoneBattery\":50}");
    private int lastUpdatedHour = 0;

    public void setContext(Context context) {
        this.context = context;
        if (this.heartRateBmp == null) {
            this.heartRateBmp = Util.decodeImage(context.getResources(), "heartrate.png");
            this.batteryBmp = Util.decodeImage(context.getResources(), "battery.png");
            this.phoneBatteryBmp = Util.decodeImage(context.getResources(), "phone_battery.png");
            this.notificationsBmp = Util.decodeImage(context.getResources(), "notifications.png");
            this.sunriseBmp = Util.decodeImage(context.getResources(), "sunrise.png");
            this.sunsetBmp = Util.decodeImage(context.getResources(), "sunset.png");
            this.uvBmp = Util.decodeImage(context.getResources(), "uv.png");
        }

        clear.setColor(Color.WHITE);
        clear.setStyle(Paint.Style.FILL);

        noDataPaint.setAntiAlias(true);
        noDataPaint.setColor(Color.BLACK);
        noDataPaint.setTypeface(Typeface.DEFAULT_BOLD);
        noDataPaint.setTextSize(18.0f);
        noDataPaint.setTextAlign(Paint.Align.CENTER);
        noDataPaint.setShadowLayer(0.01f, 2, 2, Color.BLACK);

        batteryPaint.setAntiAlias(true);
        batteryPaint.setColor(Color.WHITE);
        batteryPaint.setTypeface(Typeface.DEFAULT_BOLD);
        batteryPaint.setTextAlign(Paint.Align.CENTER);
        batteryPaint.setTextSize(16.0f);
        batteryPaint.setShadowLayer(0.01f, 2, 2, Color.BLACK);

        phoneDataPaint.setAntiAlias(true);
        phoneDataPaint.setColor(Color.BLACK);
        phoneDataPaint.setTypeface(Typeface.DEFAULT_BOLD);
        phoneDataPaint.setTextAlign(Paint.Align.CENTER);
        phoneDataPaint.setTextSize(16.0f);
        //phoneDataPaint.setShadowLayer(0.01f, 2, 2, Color.BLACK);

        stepsHearthRatePaint.setAntiAlias(true);
        stepsHearthRatePaint.setColor(Color.BLACK);
        stepsHearthRatePaint.setTypeface(Typeface.DEFAULT_BOLD);
        stepsHearthRatePaint.setTextAlign(Paint.Align.CENTER);
        stepsHearthRatePaint.setTextSize(20.0f);

        hearthIconPaint.setAntiAlias(true);
        hearthIconPaint.setColorFilter(new LightingColorFilter(Color.RED, 1));
        uvIconPaint.setAntiAlias(true);

        timePaint.setAntiAlias(true);
        timePaint.setColor(Color.BLACK);
        timePaint.setTypeface(ResourceManager.getTypeFace(context.getResources(), ResourceManager.Font.Bold));
        timePaint.setTextAlign(Paint.Align.CENTER);
        timePaint.setTextSize(100.0f);

        secondsPaint.setAntiAlias(true);
        secondsPaint.setColor(Color.BLACK);
        secondsPaint.setTypeface(ResourceManager.getTypeFace(context.getResources(), ResourceManager.Font.Bold));
        secondsPaint.setTextAlign(Paint.Align.CENTER);
        secondsPaint.setTextSize(25.0f);

        sunraisePaint.setAntiAlias(true);
        sunraisePaint.setColor(Color.parseColor("#555555"));
        sunraisePaint.setTypeface(Typeface.DEFAULT);
        sunraisePaint.setTextAlign(Paint.Align.LEFT);
        sunraisePaint.setTextSize(14.0f);

        sunsetPaint.setAntiAlias(true);
        sunsetPaint.setColor(Color.parseColor("#555555"));
        sunsetPaint.setTypeface(Typeface.DEFAULT);
        sunsetPaint.setTextAlign(Paint.Align.RIGHT);
        sunsetPaint.setTextSize(14.0f);

    }

    @Override
    public void init(Service service) {
        this.service = service;
        setContext(service.getApplicationContext());
    }


    public void refreshSlptAndPurgeLastRender(boolean redraw) {
        if (this.lastRender != null) this.lastRender.recycle();
        this.lastRender = null;

        if (this.service instanceof AbstractWatchFace) {
            ((AbstractWatchFace) this.service).restartSlpt(redraw);
        }
    }

    Bitmap lastRender = null;

    @Override
    public void onDrawDigital(Canvas canvas, float width, float height, float centerX, float centerY, int seconds, int minutes, int hours, int year, int month, int day, int week, int ampm) {
        clear(canvas);

        final LocalDateTime now = new LocalDateTime(year, month, day, hours, minutes, seconds);
        drawDate(canvas, now);
        Rect timeBounds = drawClock(canvas, now);
        drawStepsHearthRate(canvas);

        updateLatLon(this.service);

        WeatherService.INSTANCE.refresh(context, LocalDateTime.now());

        WeatherData currentData = WeatherService.INSTANCE.getCurrentData();
        drawUv(canvas, currentData, timeBounds);

        if (currentData.isEmpty()) {
            drawNoData(canvas);
        } else {
            if (lastRender == null) {
                lastRender = generateBitmap(currentData, now);
            }
            canvas.drawBitmap(lastRender, 0f, 0f, EMPTY_PAINT);
            drawSunriseSunset(canvas, currentData, now);
        }

        drawBattery(canvas);
        drawPhoneData(canvas);
    }

    private void updateLatLon(Service service) {
        try {
            // Get ALL data from system
            final String weatherInfoJson = Settings.System.getString(service.getApplicationContext().getContentResolver(), "WeatherInfo");

            // Extract data from JSON
            JSONObject weather_data = new JSONObject(weatherInfoJson);
            if (weather_data.has("lat") && weather_data.has("lon")) {
                float lat = (float) weather_data.getDouble("lat");
                float lon = (float) weather_data.getDouble("lon");

                WeatherService.INSTANCE.getCurrentData().setLat(lat);
                WeatherService.INSTANCE.getCurrentData().setLon(lon);
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
        canvas.drawText("No data", canvas.getWidth() / 2, canvas.getHeight() / 6, noDataPaint);
    }

/*    @NotNull
    private Bitmap convertBitmapToGrayscale(Bitmap bitmap) {
        Bitmap bw = Bitmap.createBitmap(bitmap);
        Canvas bwCanvas = new Canvas(bw);
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0f);
        Paint bwPaint = new Paint();
        bwPaint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        bwCanvas.drawBitmap(bitmap, 0, 0, bwPaint);
        return bw;
    }*/

    private void drawDate(Canvas canvas, LocalDateTime now) {
        final String dateString = now.toString("E d MMM").toLowerCase().replaceAll("\\.", "");
        datePaint.setAntiAlias(true);
        datePaint.setColor(Color.BLACK);
        datePaint.setTypeface(Typeface.DEFAULT_BOLD);
        datePaint.setTextAlign(Paint.Align.CENTER);
        datePaint.setTextSize(20.0f);

        canvas.drawText(dateString, canvas.getWidth() / 2, canvas.getHeight() - 112, datePaint);
    }

    private Rect drawClock(Canvas canvas, LocalDateTime now) {
        //final String timeString = now.getSecondOfMinute() % 2 > 0 ? now.toString("H:mm") : now.toString("H mm");
        final String timeString = now.toString("H:mm");
        final String secondsString = now.toString("ss");

        Rect timeBounds = new Rect();
        timePaint.getTextBounds(timeString, 0, timeString.length(), timeBounds);

        drawClockStr(canvas, timeString);
        canvas.drawText(secondsString, (canvas.getWidth() / 2) + timeBounds.centerX() + 10, canvas.getHeight() - 100 + 16, secondsPaint);

        return timeBounds;
    }

    private void drawClockStr(Canvas canvas, String str) {
        canvas.drawText(str, canvas.getWidth() / 2, canvas.getHeight() - 32, timePaint);
    }

    private void drawUv(Canvas canvas, WeatherData weatherData, Rect timeBounds) {
        final int currentUv = weatherData.getCurrentUv();
        final int uvColor;

        if (currentUv <=2) {
            uvColor = Color.GREEN;
            return;
        } else if (currentUv > 2 && currentUv <= 5) {
            uvColor = Color.YELLOW;
        }else if (currentUv > 5 && currentUv <= 7) {
            uvColor = Color.parseColor("#FFA500");
        }else if (currentUv > 7 && currentUv <= 10) {
            uvColor = Color.RED;
        } else {
            uvColor = Color.parseColor("#EE82EE");
        }

        uvPaint.setAntiAlias(true);
        uvPaint.setColor(uvColor);
        uvIconPaint.setColorFilter(new LightingColorFilter(uvColor, 1));
        uvPaint.setTypeface(Typeface.DEFAULT_BOLD);
        uvPaint.setTextAlign(Paint.Align.LEFT);
        uvPaint.setTextSize(18.0f);


        final String uvString = String.valueOf(currentUv);
        canvas.drawBitmap(uvBmp, (canvas.getWidth() / 2) - timeBounds.centerX(), canvas.getHeight() - 130, uvIconPaint);
        canvas.drawText(uvString, (canvas.getWidth() / 2) - timeBounds.centerX() + 25, canvas.getHeight() - 112, uvPaint);
    }

    private void drawStepsHearthRate(Canvas canvas) {
        String message = String.format("%s / %s", heartRate.getHeartRate(), steps.getSteps());
        canvas.drawText(message, canvas.getWidth() / 2 + 12, canvas.getHeight() - 8, stepsHearthRatePaint);

        Rect messageBounds = new Rect();
        stepsHearthRatePaint.getTextBounds(message, 0, message.length(), messageBounds);
        canvas.drawBitmap(heartRateBmp, (canvas.getWidth() / 2) - (messageBounds.width() / 2) - 12, canvas.getHeight() - 24, hearthIconPaint);
    }

    private void drawBattery(Canvas canvas) {
        //this is needed for slpt mode rendering
        batteryPaint.setAntiAlias(true);
        batteryPaint.setColor(Color.WHITE);
        batteryPaint.setTypeface(Typeface.DEFAULT_BOLD);
        batteryPaint.setTextAlign(Paint.Align.CENTER);
        batteryPaint.setTextSize(16.0f);
        batteryPaint.setShadowLayer(0.01f, 2, 2, Color.BLACK);

        String text = this.batteryData.getLevel() * 100 / this.batteryData.getScale() + "%";
        canvas.drawText(text, canvas.getWidth() / 2 - 20, 27, batteryPaint);
        canvas.drawBitmap(batteryBmp, canvas.getWidth() / 2 - 32 - 20, 11, EMPTY_PAINT);
    }

    private void drawPhoneData(Canvas canvas) {
        if (StringUtils.isNotBlank(customData.phoneBattery)) {
            String text = customData.phoneBattery + "%";
            canvas.drawText(text, canvas.getWidth() / 2 + 32, 27, batteryPaint);
            canvas.drawBitmap(phoneBatteryBmp, canvas.getWidth() / 2 - 32 + 30, 11, EMPTY_PAINT);
        }

        if (StringUtils.isNotBlank(customData.notifications) && !StringUtils.equalsIgnoreCase(customData.notifications, "0")) {
            //this is needed for slpt mode rendering
            phoneDataPaint.setAntiAlias(true);
            phoneDataPaint.setColor(Color.BLACK);
            phoneDataPaint.setTypeface(Typeface.DEFAULT_BOLD);
            phoneDataPaint.setTextAlign(Paint.Align.CENTER);
            phoneDataPaint.setTextSize(16.0f);
            //phoneDataPaint.setShadowLayer(0.01f, 2, 2, Color.BLACK);

            String text = customData.notifications;
            canvas.drawText(text, canvas.getWidth() / 6, canvas.getHeight() - 100 + 32, phoneDataPaint);
            canvas.drawBitmap(notificationsBmp, (canvas.getWidth() / 6) - phoneDataPaint.measureText(text) - 2, canvas.getHeight() - 102, EMPTY_PAINT);
        }

    }

    private void drawSunriseSunset(Canvas canvas, WeatherData weatherData, LocalDateTime now) {
        final Optional<SunraiseSunset> optSunraiseSunset = weatherData.getSunraiseSunsets().stream()
                .filter(s -> s.getDate().equals(now.toLocalDate()))
                .findFirst();

        optSunraiseSunset.ifPresent(sunraiseSunset -> {
            final String sunriseString = sunraiseSunset.getSunrise().toString("H:mm");
            final String sunsetString = sunraiseSunset.getSunset().toString("H:mm");

            Rect sunsetStringBounds = new Rect();file:///home/fernando/Amazfit/GreatFit/app/src/main/assets/icons/uv.png
            sunsetPaint.getTextBounds(sunsetString, 0, sunsetString.length(), sunsetStringBounds);

            canvas.drawText(sunriseString, 32, canvas.getHeight() - 114, sunraisePaint);
            canvas.drawBitmap(sunriseBmp, 10, canvas.getHeight() - 132, EMPTY_PAINT);
            canvas.drawText(sunsetString, canvas.getWidth()-12, canvas.getHeight() - 114, sunsetPaint);
            canvas.drawBitmap(sunsetBmp, canvas.getWidth() - sunsetStringBounds.width() - 35, canvas.getHeight() - 132, EMPTY_PAINT);
        });
    }

    @Override
    public List<DataType> getDataTypes() {
        return Arrays.asList(DataType.BATTERY, DataType.HEART_RATE, DataType.STEPS, DataType.CUSTOM, DataType.TIME, DataType.WEATHER);
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
            case WEATHER:
                //com.fgil55.weathergraph.data.WeatherData wd = (com.fgil55.weathergraph.data.WeatherData) value;
                refresh = true;
                break;
            case TIME:
                Time time = (Time) value;
                refresh = this.lastUpdatedHour != time.getHours();
                this.lastUpdatedHour = time.getHours();
                break;
            default:
                return;
        }

        if (refresh) refreshSlptAndPurgeLastRender(true);
    }

    @Override
    public List<SlptViewComponent> buildSlptViewComponent(Service service) {
        setContext(service.getApplicationContext());
        updateLatLon(service);
        updateBattery(service);
        updateCustomData(service);

        SlptPictureView canvas = new SlptPictureView();
        canvas.setImagePicture(generateBitmapForSlpt());

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
                Color.BLACK,
                timeTypeFace);
        minuteLayout.setTextAttrForAll(
                service.getResources().getDimension(R.dimen.malvarez_time_font_size),
                Color.BLACK,
                timeTypeFace);
        hourLayout.setStart(
                (int) service.getResources().getDimension(R.dimen.malvarez_time_hour_left_slpt),
                (int) service.getResources().getDimension(R.dimen.malvarez_time_hour_top_slpt));
        minuteLayout.setStart(
                (int) service.getResources().getDimension(R.dimen.malvarez_time_minute_left_slpt),
                (int) service.getResources().getDimension(R.dimen.malvarez_time_minute_top_slpt));

        return Arrays.asList(canvas, hourLayout, minuteLayout);
    }

    private Bitmap generateBitmap(WeatherData currentData, LocalDateTime now) {
        final Bitmap bitmap = Bitmap.createBitmap(320, 300, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);

        renderer.render(canvas, currentData, now, false);

        return bitmap;
    }

    private byte[] generateBitmapForSlpt() {
        final Bitmap bitmap = Bitmap.createBitmap(320, 300, Bitmap.Config.RGB_565);
        final LocalDateTime now = LocalDateTime.now();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();) {
            Canvas canvas = new Canvas(bitmap);
            renderer.render(canvas, WeatherService.INSTANCE.getCurrentData(), now, true);
            drawDate(canvas, now);
            drawBattery(canvas);
            drawPhoneData(canvas);
            drawClockStr(canvas, ":");

            bitmap.compress(Bitmap.CompressFormat.PNG, 80, out);

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
