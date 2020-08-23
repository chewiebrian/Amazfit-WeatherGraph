package com.fgil55.weathergraph;

import com.fgil55.weathergraph.widget.ClockWidget;
import com.fgil55.weathergraph.widget.Widget;
import com.huami.watch.watchface.AbstractSlptClock;
import com.ingenic.iwds.slpt.view.core.SlptAbsoluteLayout;
import com.ingenic.iwds.slpt.view.core.SlptLayout;
import com.ingenic.iwds.slpt.view.core.SlptViewComponent;

import java.util.Arrays;
import java.util.LinkedList;

/**
 * Splt version of
 */

public abstract class AbstractWatchFaceSlpt extends AbstractSlptClock {


    public ClockWidget clock;
    final LinkedList<Widget> widgets = new LinkedList<>();

    protected AbstractWatchFaceSlpt(final ClockWidget clock, final Widget... widgets) {
        this.clock = clock;
        this.widgets.addAll(Arrays.asList(widgets));
    }

    protected AbstractWatchFaceSlpt() {

    }
}
