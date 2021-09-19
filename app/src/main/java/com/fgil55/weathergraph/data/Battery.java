package com.fgil55.weathergraph.data;

/**
 * Battery data.
 */

public class Battery {

    private final int level;
    private final int scale;

    public Battery(int level, int scale) {
        this.level = level;
        this.scale = scale;
    }

    public int getLevel() {
        return level;
    }

    public int getScale() {
        return scale;
    }

    public String getLabel() {
        return level * 100 / scale + "%";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Battery battery = (Battery) o;

        if (level != battery.level) return false;
        return scale == battery.scale;
    }

    @Override
    public int hashCode() {
        int result = level;
        result = 31 * result + scale;
        return result;
    }
}
