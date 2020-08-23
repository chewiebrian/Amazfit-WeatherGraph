package com.fgil55.weathergraph.data;

/**
 * TodayFloor data type
 */

public class TodayFloor {

    private final int floor;

    public TodayFloor(float todayFloor){
        this.floor = (int)todayFloor;
    }

    public int getFloor(){return floor;}
}
