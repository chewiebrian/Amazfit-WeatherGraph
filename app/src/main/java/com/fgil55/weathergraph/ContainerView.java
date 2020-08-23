package com.fgil55.weathergraph;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import com.fgil55.weathergraph.widget.WeatherGraphWidget;

import org.joda.time.LocalDateTime;

public class ContainerView extends View {

    WeatherGraphWidget widget;

    public ContainerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        widget = new WeatherGraphWidget();
        widget.setContext(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        LocalDateTime now = LocalDateTime.now();

        widget.onDrawDigital(canvas, 320,300,160,100, now.getSecondOfMinute(), now.getMinuteOfHour(), now.getHourOfDay(),now.getYear(), now.getMonthOfYear(), now.getDayOfMonth(), now.getWeekOfWeekyear(), 0);
    }
}
