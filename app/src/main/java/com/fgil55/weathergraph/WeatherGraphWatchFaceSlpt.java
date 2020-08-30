package com.fgil55.weathergraph;

import android.content.Intent;

import com.fgil55.weathergraph.widget.WeatherGraphWidget;
import com.fgil55.weathergraph.widget.Widget;
import com.ingenic.iwds.slpt.view.core.SlptAbsoluteLayout;
import com.ingenic.iwds.slpt.view.core.SlptLayout;
import com.ingenic.iwds.slpt.view.core.SlptViewComponent;

public class WeatherGraphWatchFaceSlpt extends AbstractWatchFaceSlpt {

    public WeatherGraphWatchFaceSlpt() {
        super(new WeatherGraphWidget());
    }

    @Override
    protected SlptLayout createClockLayout26WC() {
        SlptAbsoluteLayout result = new SlptAbsoluteLayout();
        for (SlptViewComponent component : clock.buildSlptViewComponent(this, true)) {
            result.add(component);
        }
        for (Widget widget : widgets) {
            for (SlptViewComponent component : widget.buildSlptViewComponent(this, true)) {
                result.add(component);
            }
        }

        return result;
    }

    @Override
    protected SlptLayout createClockLayout8C() {
        SlptAbsoluteLayout result = new SlptAbsoluteLayout();
        for (SlptViewComponent component : clock.buildSlptViewComponent(this)) {
            result.add(component);
        }
        for (Widget widget : widgets) {
            for (SlptViewComponent component : widget.buildSlptViewComponent(this)) {
                result.add(component);
            }
        }

        return result;
    }

    @Override
    public int onStartCommand(Intent intent, int i, int i1) {
        return super.onStartCommand(intent, i, i1);
    }

    protected void initWatchFaceConfig() {
        //Log.w("DinoDevs-GreatFit", "Initiating watchface");
    }

    @Override
    public boolean isClockPeriodSecond() {
        return false;
//        Context context = this.getApplicationContext();
//        boolean needRefreshSecond = Util.needSlptRefreshSecond(context);
//        if (needRefreshSecond) {
//            this.setClockPeriodSecond(true);
//        }
//        return needRefreshSecond;
    }

}
