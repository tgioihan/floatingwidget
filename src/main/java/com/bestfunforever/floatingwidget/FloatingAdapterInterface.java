package com.bestfunforever.floatingwidget;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

/**
 * Created by nguyenxuan on 9/27/2015.
 */
public interface FloatingAdapterInterface {
    int getItemCount();

    FloatingSpriteEntity getEntity(Context context,FloatingSpriteEntity cacheEntity, int position);

    boolean valid();

    boolean isPrimaryItem(FloatingSpriteEntity entity);

    View getContentView(BaseFloatingView baseFloatingView, LayoutInflater layoutInflator, BaseFloatingContent parentView, int id);
}
