package com.fgil55.weathertest.data;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Typeface;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Locale;

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
}
