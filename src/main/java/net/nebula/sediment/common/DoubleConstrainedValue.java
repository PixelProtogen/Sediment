package net.nebula.sediment.common;

import net.minecraft.util.Mth;

public class DoubleConstrainedValue {
    private final int minCount;
    private final int maxCount;
    private int count;

    public DoubleConstrainedValue(int minValue, int maxValue) {
        this.minCount = Math.min(minValue, maxValue);
        this.maxCount = Math.max(minValue, maxValue);
        this.count = Mth.clamp(0, this.minCount, this.maxCount);
    }

    public DoubleConstrainedValue(int maxValue) {
        this(0, maxValue);
    }

    public DoubleConstrainedValue() {
        this(0, 0);
    }

    public static DoubleConstrainedValue of(int minValue, int maxValue) {
        return new DoubleConstrainedValue(minValue, maxValue);
    }

    public void set(int value) {
        this.count = Mth.clamp(value, this.minCount, this.maxCount);
    }

    public void add(int amount) {
        this.set(this.count + amount);
    }

    public void sub(int amount) {
        this.set(this.count - amount);
    }

    public void reset() {
        this.set(0);
    }

    public boolean isMin() {
        return this.count == this.minCount;
    }

    public boolean isMax() {
        return this.count == this.maxCount;
    }

    public int get() {
        return this.count;
    }

    public int min() {
        return this.minCount;
    }

    public int max() {
        return this.maxCount;
    }
}