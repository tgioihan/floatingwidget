package com.bestfunforever.floatingwidget;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;

import com.bestfunforever.canvasanimation.AnimationExecutor;
import com.bestfunforever.canvasanimation.callback.ModifierListener;
import com.bestfunforever.canvasanimation.entity.AnimationEntity;
import com.bestfunforever.canvasanimation.entity.Entity;
import com.bestfunforever.canvasanimation.modifier.BaseModifier;
import com.bestfunforever.canvasanimation.modifier.ScaleModifier;
import com.bestfunforever.canvasanimation.modifier.TranformModifier;
import com.bestfunforever.canvasanimation.modifier.interpolator.BackInnterpolator;
import com.bestfunforever.canvasanimation.modifier.interpolator.BackOutInterpolator;


/**
 * Created by nguyenxuan on 8/2/2015.
 */
public class BaseFloatingContent extends FrameLayout implements ModifierListener {

    private final TranformModifier modifer;
    private final ScaleModifier zoomModifier;
    private int srcWidth;
    private int srcHeight;

    public void setCallback(AnimationExecutor callback) {
        this.callback = callback;
    }

    private AnimationExecutor callback;

    public void setModifierListener(ModifierListener modifierListener) {
        this.modifierListener = modifierListener;
    }

    private ModifierListener modifierListener;

    public AnimationEntity getFloatingEntity() {
        return floatingEntity;
    }

    private final AnimationEntity floatingEntity;

    public BaseFloatingContent(Context context) {
        super(context);
        floatingEntity = new AnimationEntity();
        floatingEntity.setId(BaseFloatingView.CONTENT);
        modifer = new TranformModifier(floatingEntity, 0, 0, BaseFloatingView.SHOW_DURATION, BackOutInterpolator.getInstance());
        modifer.setListener(this);

        zoomModifier = new ScaleModifier(floatingEntity,1,1, BaseFloatingView.SHOW_DURATION, BackInnterpolator.getInstance());
    }

    public void zoom(float desScaleX,float desScaleY,float desX, float desY){
        zoomModifier.setDesScale(desScaleX, desScaleY);
        Log.d("", "zoom " + desScaleX + " " + desScaleY);
        modifer.updateValue(desX, desY);
        floatingEntity.removeModifiers();
        floatingEntity.addModifier(zoomModifier);
        floatingEntity.addModifier(modifer);
    }

    public void setScale(float scaleX,float scaleY){
        floatingEntity.setScale(scaleX, scaleY);
    }

    public void onShow() {

    }

    public void setPackageId(String packageId) {

    }

    public void updateTransformAnimation(float x, float y) {
        modifer.updateValue(x,y);
        floatingEntity.removeModifiers();
        floatingEntity.addModifier(modifer);
    }

    @Override
    public void onStart(Entity entity, BaseModifier modifier) {
        setDrawingCacheEnabled(false);
        if(modifierListener!=null){
            modifierListener.onStart(entity,modifier);
        }
    }

    @Override
    public void onUpdate(Entity entity, float timeElapsed, BaseModifier modifier) {
//        layout((int) entity.getX(), (int) entity.getY(), (int) (entity.getX() + getWidth()), (int) (entity.getY() + getHeight()));
        ((LayoutParams)getLayoutParams()).leftMargin = (int) entity.getX();
        ((LayoutParams)getLayoutParams()).topMargin = (int) entity.getY();
        ((LayoutParams)getLayoutParams()).width = (int) (srcWidth*entity.getScaleX());
        ((LayoutParams)getLayoutParams()).height = (int) (srcHeight*entity.getScaleY());
        Log.d("","scaleX "+ entity.getScaleX()+" scaleY "+ entity.getScaleY());
        Log.d("","onupdate content dimension "+ ((LayoutParams)getLayoutParams()).width+" "+((LayoutParams)getLayoutParams()).height+" "+((LayoutParams)getLayoutParams()).leftMargin+" "+ ((LayoutParams)getLayoutParams()).topMargin);
        requestLayout();
        if(modifierListener!=null){
            modifierListener.onUpdate(entity, timeElapsed, modifier);
        }
    }

    @Override
    public void onEnd(Entity entity, BaseModifier modifier) {
        setDrawingCacheEnabled(true);
        if(modifierListener!=null){
            modifierListener.onEnd(entity, modifier);
        }
        if(callback!=null){
            callback.execute(floatingEntity);
            callback = null;
        }
    }

    public void updatePosition(int x, int y) {
        ((LayoutParams)getLayoutParams()).leftMargin = x;
        ((LayoutParams)getLayoutParams()).topMargin = y;
        floatingEntity.setX(x);
        floatingEntity.setY(y);
        Log.d("", "applyLayoutParams " + ((LayoutParams)getLayoutParams()).leftMargin + " " + ((LayoutParams)getLayoutParams()).topMargin);
    }

    public void updateFloatingDimension(int width, int height) {
        floatingEntity.setWidth(width);
        floatingEntity.setHeight(height);

        srcWidth = width;
        srcHeight = height;
        Log.d("", "srcwidth " + srcWidth + " srcHeight " + srcHeight);
    }

    public boolean isFull(int screenWidth){
        return getWidth() == screenWidth;
    }

    public int getSrcWidth() {
        return srcWidth;
    }

    public int getSrcHeight() {
        return srcHeight;
    }

    public void setZoomed(boolean isFull) {

    }

    public void setData(Bundle data) {

    }
}
