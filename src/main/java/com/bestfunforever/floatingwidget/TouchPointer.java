package com.bestfunforever.floatingwidget;

/**
 * Created by nguyenxuan on 7/29/2015.
 */
public class TouchPointer {

    private float x;
    private float y;

    public TouchPointer(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void set(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }
}
