package com.fgil55.weathertest;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import com.fgil55.weathertest.widget.WeatherGraphWidget;

public class ContainerView extends View {

    WeatherGraphWidget widget;

    public ContainerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        widget = new WeatherGraphWidget(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        widget.draw(canvas, 320,300,160,100);
    }
}
