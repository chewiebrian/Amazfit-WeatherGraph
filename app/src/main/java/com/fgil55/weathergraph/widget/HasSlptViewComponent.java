package com.fgil55.weathergraph.widget;

import android.app.Service;

import com.ingenic.iwds.slpt.view.core.SlptViewComponent;

import java.util.List;


public interface HasSlptViewComponent {

    List<SlptViewComponent> buildSlptViewComponent(Service service);
    List<SlptViewComponent> buildSlptViewComponent(Service service, boolean better_resolution);
}
