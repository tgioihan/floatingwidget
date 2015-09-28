package com.bestfunforever.floatingwidget;

import android.graphics.Bitmap;
import android.graphics.Paint;

import com.bestfunforever.canvasanimation.AnimationExecutor;
import com.bestfunforever.canvasanimation.entity.Sprite;
import com.bestfunforever.canvasanimation.modifier.TranformModifier;
import com.bestfunforever.canvasanimation.modifier.interpolator.Interpolator;

/**
 * Created by nguyenxuan on 8/9/2015.
 */
public class FloatingSpriteEntity extends Sprite {

    private TranformModifier mTranform;

    private AnimationExecutor callback;

    public FloatingSpriteEntity() {
        super();
        setEnable(true);
    }

    public FloatingSpriteEntity(Paint mPaint, float x, float y, Bitmap bitmap) {
        super(mPaint, x, y, bitmap);
        setEnable(true);
    }

    public void setAnimation(TranformModifier modifier){
        mTranform = modifier;
    }

    public void updateAnimation(float x, float y, Interpolator interpolator) {

        mTranform.updateValue(x, y);
        mTranform.setInterpolator(interpolator);
        removeModifier(mTranform);
        addModifier(mTranform);
    }

    public  void setCallback(AnimationExecutor callback){
        this.callback = callback;
    }

    public  void execute(){
        if(callback!=null){
            callback.execute(this);
            callback = null;
        }
    }

    public void onExpand() {

    }

    public void onCollapse() {

    }

    public void onActiveChange(boolean active) {

    }

    public void collapsed() {

    }
}
