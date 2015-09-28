package com.bestfunforever.floatingwidget;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.MotionEventCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.bestfunforever.canvasanimation.AnimationExecutor;
import com.bestfunforever.canvasanimation.callback.IDisposeable;
import com.bestfunforever.canvasanimation.callback.ModifierListener;
import com.bestfunforever.canvasanimation.entity.EmptyEntityAnimation;
import com.bestfunforever.canvasanimation.entity.Entity;
import com.bestfunforever.canvasanimation.modifier.BaseModifier;
import com.bestfunforever.canvasanimation.modifier.TranformModifier;
import com.bestfunforever.canvasanimation.modifier.interpolator.BackOutInterpolator;
import com.bestfunforever.canvasanimation.modifier.interpolator.Interpolator;
import com.bestfunforever.canvasanimation.modifier.interpolator.LinearInterpolator;
import com.bestfunforever.canvasanimation.view.AnimationViewGroup;

import java.lang.ref.WeakReference;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by nguyenxuan on 9/27/2015.
 */
public abstract class BaseFloatingView extends AnimationViewGroup implements IDisposeable, ModifierListener {
    public static final int DRAG_TRIGGER = 15;
    private static final int HIDE_WHAT = 4413;
    private static final int MINFILLING = 500;
    public static final float SHOW_DURATION = 500;
    public static final int CONTENT = 7781;


    protected abstract void onHide(int positionXBeforeHide);

    protected abstract void onShow(int positionXBeforeShow);

    public abstract int getDefaultContentHeight(int id) ;

    protected abstract void onEntitySelected(Entity entity);

    protected  EmptyEntityAnimation floatingEntity;
    private  HideLauncherHandler hideLauncherHandler;
    private  TranformModifier tranformModifier;

    private boolean animationLock;
    private BaseFloatingAdapter adapter;
    protected LayoutType layoutType;
    private VelocityTracker mVelocityTracker;
    private BaseFloatingContent contentView;
    private float initialRawX;
    private float initialRawY;
    private float lastRawX;
    private float lastRawY;
    private boolean dragging;
    private FloatingSpriteEntity launcherEntity;
    protected int orientation;
    private WindowManager windowManager;


    private int screenWidth;
    protected int screenHeight;
    private int alpha;
    private  FloatingCache cache;
    private int currentActiveId;
    private FloatingSpriteEntity activeEntity;

    private float marginOnExpand;
    private TouchPointer savedPointer;
    protected CopyOnWriteArrayList<FloatingSpriteEntity> expandItems;


    public BaseFloatingView(Context context) {
        super(context);

    }


    @Override
    protected void onInit(Context context) {
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        hideLauncherHandler = new HideLauncherHandler(this);

        expandItems = new CopyOnWriteArrayList<>();
        calculateScreenDimension();
        savedPointer = new TouchPointer(0, 0);
        cache = new FloatingCache();
        floatingEntity = new EmptyEntityAnimation();
        floatingEntity.setTouchDelegateRank(50);
        tranformModifier = new TranformModifier(floatingEntity, 0, 0, 1000, BackOutInterpolator.getInstance());
        tranformModifier.setListener(new ModifierListener() {
            @Override
            public void onStart(Entity entity, BaseModifier modifier) {
                animationLock = true;
            }

            @Override
            public void onUpdate(Entity entity, float timeElapsed, BaseModifier modifier) {
                moveFloating((int) entity.getX(), (int) entity.getY());
            }

            @Override
            public void onEnd(Entity entity, BaseModifier modifier) {
                animationLock = false;
                entity.removeModifier(modifier);
                ((EmptyEntityAnimation) entity).execute();
                entities.remove(entity);
            }
        });
        post(animationRunnable);
    }

    public void setAdapter(BaseFloatingAdapter adapter) {
        if (!adapter.valid()) {
            throw new IllegalArgumentException("Adapter must size >0");
        }
        this.adapter = adapter;
        expandItems.clear();
        removeEntities();
        removeAllViews();
        int itemSize = adapter.getItemCount();

        for (int i = 0; i < itemSize; i++) {
            FloatingSpriteEntity entity = adapter.getEntity(getContext(),null, i);
            if (adapter.isPrimaryItem(entity)) {
                launcherEntity = entity;
                floatingEntity.setWidth(launcherEntity.getWidth());
                floatingEntity.setHeight(launcherEntity.getHeight());
                addEntity(entity);
            }
            expandItems.add(entity);
            TranformModifier modifier = new TranformModifier(entity, 0, 0, SHOW_DURATION, BackOutInterpolator.getInstance());
            modifier.setListener(this);
            entity.setAnimation(modifier);
        }
        layoutType = LayoutType.Collapse;
        requestLayout();
    }

    @Override
    public void onStart(Entity entity, BaseModifier modifier) {
        animationLock = true;
    }

    @Override
    public void onUpdate(Entity entity, float timeElapsed, BaseModifier modifier) {
        animationLock = true;
    }

    @Override
    public void onEnd(Entity entity, BaseModifier modifier) {
        entity.removeModifier(modifier);
        ((FloatingSpriteEntity) entity).execute();
        animationLock = false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Log.d("","onmeasure layouttype "+layoutType +" launcherEntity "+launcherEntity);
        if ((layoutType == LayoutType.Collapse || layoutType == LayoutType.Hide || layoutType == LayoutType.Tutorial )&& launcherEntity !=null) {
            setMeasuredDimension((int) launcherEntity.getWidth(), (int) launcherEntity.getHeight());
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d("", "touchevent type = " + layoutType + " lock " + animationLock + " action " + event.getAction());
        if (animationLock) {
            return false;
        }
        if (layoutType == LayoutType.Tutorial) {
            return false;
        }
        if (layoutType == LayoutType.Collapse) {
            final int action = event.getAction();
            switch (action | event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if (mVelocityTracker == null) {
                        mVelocityTracker = VelocityTracker.obtain();
                    } else {
                        mVelocityTracker.clear();
                    }
                    initialRawX = event.getRawX();
                    initialRawY = event.getRawY();
                    lastRawX = initialRawX;
                    lastRawY = initialRawY;
                    dragging = false;
                    cancelHide();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mVelocityTracker == null) {
                        mVelocityTracker = VelocityTracker.obtain();
                    }
                    mVelocityTracker.addMovement(event);
                    mVelocityTracker.computeCurrentVelocity(500);
                    final int divX = (int) (event.getRawX() - lastRawX);
                    final int divY = (int) (event.getRawY() - lastRawY);
                    lastRawX = event.getRawX();
                    lastRawY = event.getRawY();
                    if (!dragging) {

                        final int dragX = (int) (event.getRawX() - initialRawX);
                        final int dragY = (int) (event.getRawY() - initialRawY);
                        if (Math.abs(dragX) >= DRAG_TRIGGER || Math.abs(dragY) >= DRAG_TRIGGER) {
                            dragging = true;
                        }
                    } else {
                        moveFloatingBy(divX, divY);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (mVelocityTracker == null) {
                        mVelocityTracker = VelocityTracker.obtain();
                    }
                    mVelocityTracker.addMovement(event);
                    if (dragging) {
                        moveFloatingToEdge();
                    } else {
                        expand();
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                    moveFloatingToEdge();
                    break;
            }
            return true;
        } else if (layoutType == LayoutType.Hide) {
//            final int action = event.getAction();
//            Log.d("", "touchevent type = hide action " + action + " " + event.getRawX() + " " + event.getRawY() + " " + floatingEntity.getX() + " " + floatingEntity.getY());
//            if ((action == MotionEvent.ACTION_OUTSIDE && floatingEntity.containInDelegate(event.getRawX(), event.getRawY())) || action == MotionEvent.ACTION_UP) {
//                showFloating();
//            }
            final int action = event.getAction();
            switch (action | event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    initialRawX = event.getRawX();
                    initialRawY = event.getRawY();
                    lastRawX = initialRawX;
                    lastRawY = initialRawY;
                    dragging = false;
                    cancelHide();
                    break;
                case MotionEvent.ACTION_MOVE:
                    final int divX = (int) (event.getRawX() - lastRawX);
                    lastRawX = event.getRawX();
                    lastRawY = event.getRawY();
                    if (!dragging) {

                        final int dragX = (int) (event.getRawX() - initialRawX);
                        final int dragY = (int) (event.getRawY() - initialRawY);
                        if (Math.abs(dragX) >= DRAG_TRIGGER || Math.abs(dragY) >= DRAG_TRIGGER) {
                            dragging = true;
                        }
                    } else {

                        launcherEntity.translateDelta(divX, 0);
                        if (launcherEntity.getX() < screenWidth / 2) {
                            if (launcherEntity.getX() + divX >= 0) {
                                launcherEntity.setX(0);
                                layoutType = LayoutType.Collapse;
                                cancelHide();
                            } else {
                                launcherEntity.translateDelta(divX, 0);
                            }
                        } else {

                            if (launcherEntity.getX() + divX <= screenWidth - launcherEntity.getWidth()) {
                                launcherEntity.setX(screenWidth - launcherEntity.getWidth());
                                layoutType = LayoutType.Collapse;
                                cancelHide();
                            } else {
                                launcherEntity.translateDelta(divX, 0);
                            }
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (!dragging) {
                        showFloating();
                    } else {
                        hideFloating();
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                    hideFloating();
                    break;
            }
            return true;
        } else {
            final int action = event.getAction();
            switch (action & MotionEventCompat.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    boolean result = touchManager.onTouchEvent(event);
                    cancelHide();
                    if (!result) {
                        Log.d("", "checkMotionEventBoundContent " + checkMotionEventBoundContent(event));
                        if (!checkMotionEventBoundContent(event)) {
                            return true;
                        }
                    }
                    return result;
                default:
                    break;
            }
            return touchManager.onTouchEvent(event);
        }
    }

    @Override
    protected void preDraw(Canvas canvas) {
        super.preDraw(canvas);
        canvas.drawARGB(alpha, 0, 0, 0);
    }

    @Override
    public void onItemClicked(Entity entity) {
        if (entity == null) {
            collapse();
            return;
        }
        if (currentActiveId == entity.getId()) {
            collapse();
        } else {
            setActiveEntity(entity);
            onEntitySelected(entity);
        }
    }


    protected void addContentView(int id, Bundle data) {
        if (currentActiveId == id) {
            if (contentView == null) {
                initialContentViewIfNeed(id);
            }
        } else {
            if (contentView != null) {
                removeView(contentView);
                cache.addCache(contentView);
                contentView = null;
            }
            initialContentViewIfNeed(id);
        }

        currentActiveId = id;
        if (contentView == null) {
            return;
        }
        contentView.setData(data);
        contentView.setScale(1, 1);
        entities.add(contentView.getFloatingEntity());
    }

    private void initialContentViewIfNeed(int id) {
        int width = (int) (screenWidth - launcherEntity.getWidth() - 3 * marginOnExpand);
        Log.d("", "content width " + width);
        BaseFloatingContent item = cache.getItem(id);
        Log.d("", "item from cache " + item);
        if (item == null) {
            LayoutInflater layoutInflator = LayoutInflater.from(getContext());
            item = initialContentViewObject();
            item.setModifierListener(new ModifierListener() {
                @Override
                public void onStart(Entity entity, BaseModifier modifier) {
                    animationLock = true;
                }

                @Override
                public void onUpdate(Entity entity, float timeElapsed, BaseModifier modifier) {

                }

                @Override
                public void onEnd(Entity entity, BaseModifier modifier) {
                    entity.removeModifier(modifier);
                    animationLock = false;
                    entities.remove(entity);
                }
            });
            FrameLayout.LayoutParams contentParams = new FrameLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            item.setLayoutParams(contentParams);
            View view = adapter.getContentView(this,layoutInflator,item,id);
            if(view == null){
                return;
            }
            item.addView(view);
            this.contentView = item;
        } else {
            this.contentView = item;
        }
        if(item == null){
            return;
        }
        applyLayoutParams(contentView, width, id, true);
        if (contentView.getParent() == null) {
            addView(contentView);
        }
    }

    protected BaseFloatingContent initialContentViewObject() {
        return new BaseFloatingContent(getContext());
    }

    private void applyLayoutParams(BaseFloatingContent contentView, int width, int id, boolean smoothMove) {
        FrameLayout.LayoutParams contentParams = (FrameLayout.LayoutParams) contentView.getLayoutParams();
        int height = getDefaultContentHeight(id);
        if (contentParams == null) {
            contentParams = new FrameLayout.LayoutParams(width, height);
            contentView.setLayoutParams(contentParams);
        }
        contentParams.width = width;
        contentParams.height = height;
        contentView.updateFloatingDimension(contentParams.width, contentParams.height);

        int[] values = getValueForContent(width, height, id);
        contentView.setId(id);
        if (smoothMove) {
            contentView.updatePosition(values[0], values[2]);
            contentView.updateTransformAnimation(values[1], values[2]);
        } else {
            contentView.updatePosition(values[1], values[2]);
        }

        contentView.setCallback(new AnimationExecutor() {
            @Override
            public void execute(Entity entity) {
                BaseFloatingView.this.contentView.onShow();
            }
        });
    }

    public int[] getValueForContent(int width, int height, int id) {
        int x;
        int desX;
        if (launcherEntity.getX() == marginOnExpand) {
            x = -width;
            desX = (int) (launcherEntity.getX() + launcherEntity.getWidth() + marginOnExpand);
        } else {
            x = screenWidth;
            desX = (int) (launcherEntity.getX() - width - marginOnExpand);
        }
        int desY = (int) (screenHeight - 2 * marginOnExpand - contentView.getSrcHeight());
        return new int[]{x, desX, desY};
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        calculateScreenDimension(newConfig);
        if (layoutType == LayoutType.Collapse) {
            Log.d("", "onConfigurationChanged updateFloatingEntity orientation=" + orientation);
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                updateFloating(0, 0);
            } else {
                updateFloating(0, 0);
            }
        } else if (layoutType == LayoutType.Hide) {

            WindowManager.LayoutParams params = (WindowManager.LayoutParams) getLayoutParams();
            Log.d("", "hide state onConfigurationChanged updateFloatingEntity orientation=" + orientation + " " + params.x + " " + params.y + " " + params.width + " " + params.height);
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                updateFloating(0, 0);
                launcherEntity.setPosition(-launcherEntity.getWidth() / 2, 0);
            } else {
                updateFloating(0, 0);
                launcherEntity.setPosition(-launcherEntity.getWidth() / 2, 0);
            }
        } else {
            Log.d("", "onConfigurationChanged updateFloatingEntity expande orientation=" + orientation);
            updateFloatingDimensionFull();
            updateFloatingEntity();
            updateContentDimension();
        }
    }

    protected void updateFloatingEntity() {
        float desX;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            float x = entities.get(0).getX();

            if (x < marginOnExpand) {
                desX = marginOnExpand;
                savedPointer.setX(desX - marginOnExpand);
            } else {
                desX = screenWidth - entities.get(0).getWidth() - marginOnExpand;
                savedPointer.setX(desX + marginOnExpand);
            }

        } else {
            float x = entities.get(0).getX();
            if (x == marginOnExpand) {
                desX = marginOnExpand;
                savedPointer.setX(desX - marginOnExpand);
            } else {
                desX = screenWidth - entities.get(0).getWidth() - marginOnExpand;
                savedPointer.setX(desX + marginOnExpand);
            }

        }
        float startY = screenHeight - marginOnExpand - launcherEntity.getHeight();
        int size = expandItems.size();
        for (int i = 0; i < size; i++) {
            FloatingSpriteEntity entity = expandItems.get(i);
            entity.setPosition(desX, startY);
            entity.onExpand();
            startY -= launcherEntity.getHeight() + marginOnExpand;
        }
    }

    private void updateContentDimension() {
        if (contentView != null) {
            int width = (int) (screenWidth - launcherEntity.getWidth() - 3 * marginOnExpand);
            Log.d("", "content width " + width);
            FrameLayout.LayoutParams contentParams = (FrameLayout.LayoutParams) contentView.getLayoutParams();
            contentParams.width = width;
            applyLayoutParams(contentView, width, currentActiveId, false);
        }
    }

    private void updateFloatingDimensionFull() {
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) getLayoutParams();
        params.width = screenWidth;
        params.height = screenHeight;
        windowManager.updateViewLayout(this, params);

    }

    public void updateFloating(int x, int y) {
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) getLayoutParams();
        params.x = x;
        params.y = y;
        windowManager.updateViewLayout(this, params);
        floatingEntity.setPosition(x, y);
    }

    protected void setActiveEntity(Entity entity) {
        int size = expandItems.size();
        if (activeEntity != null ) {
            activeEntity.onActiveChange(false);
        }
        for (int i = 0; i < size; i++) {
            FloatingSpriteEntity item = expandItems.get(i);
            if (item == entity ) {
                item.onActiveChange(true);
                activeEntity = item;
            }
        }
    }

    public void collapse() {
        Log.d("", "collapse ");
        hideLauncherHandler.removeMessages(HIDE_WHAT);
        int size = expandItems.size();
        for (int i = 0; i < size; i++) {
            FloatingSpriteEntity entity1 = expandItems.get(i);
            entity1.onCollapse();
            if(adapter.isPrimaryItem(entity1)){
                entity1.setCallback(new AnimationExecutor() {
                    @Override
                    public void execute(Entity entity) {
                        updateAfterCollapsed();
                    }
                });
            }
            entity1.setScale(1);
            smoothMoveEntity(entity1, savedPointer.getX(), savedPointer.getY());
        }
        if (contentView != null) {
            contentView.updateTransformAnimation(screenWidth, contentView.getFloatingEntity().getY());
            contentView.setCallback(new AnimationExecutor() {
                @Override
                public void execute(Entity entity) {
                    currentActiveId = -1;
                }
            });
            entities.add(contentView.getFloatingEntity());
        }
        WindowManager.LayoutParams mParams = (WindowManager.LayoutParams) getLayoutParams();
        mParams.flags = mParams.flags | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        windowManager.updateViewLayout(this, mParams);
        activeEntity = null;
    }


    private void updateAfterCollapsed() {
        for (FloatingSpriteEntity entity : expandItems) {
            if (!adapter.isPrimaryItem(entity)) {
                entities.remove(entity);
                touchManager.removeEntity(entity);
            }
        }
        removeView(contentView);
        layoutType = LayoutType.Collapse;
        WindowManager.LayoutParams mParams = (WindowManager.LayoutParams) getLayoutParams();
        mParams.width = (int) launcherEntity.getWidth();
        mParams.height = (int) launcherEntity.getHeight();
        mParams.x = (int) savedPointer.getX();
        mParams.y = (int) savedPointer.getY();
        launcherEntity.setX(0);
        launcherEntity.setY(0);
        alpha = 0;
        windowManager.updateViewLayout(this, mParams);
        launcherEntity.collapsed();
        executeHide();
    }

    private void expand() {
        Log.d("", "expand");
        hideLauncherHandler.removeMessages(HIDE_WHAT);
        layoutType = LayoutType.Expand;
        WindowManager.LayoutParams mParams = (WindowManager.LayoutParams) getLayoutParams();
        mParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        mParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        setUpBeforeExpand(mParams);
        mParams.x = 0;
        mParams.y = 0;
        mParams.flags = mParams.flags ^ WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        alpha = 160;
        windowManager.updateViewLayout(this, mParams);
    }

    private void setUpBeforeExpand(WindowManager.LayoutParams mParams) {
        float x = mParams.x;
        float y = mParams.y;
        savedPointer.set(x, y);
        float startX;
        if (x == 0) {
            startX = marginOnExpand;
        } else {
            startX = screenWidth - marginOnExpand - launcherEntity.getWidth();
        }
        float startY = screenHeight - 2 * marginOnExpand - launcherEntity.getHeight();
        int size = expandItems.size();
        for (int i = 0; i < size; i++) {
            FloatingSpriteEntity entity = adapter.getEntity(getContext(), expandItems.get(i), i);
            setUpItem(entity, mParams, !adapter.isPrimaryItem(entity), startY, startX);
            entity.setCallback(new AnimationExecutor() {
                @Override
                public void execute(Entity entity) {
                    ((FloatingSpriteEntity) entity).onExpand();
                }
            });
            startY -= entity.getHeight() + marginOnExpand;
        }
    }

    private void setUpItem(FloatingSpriteEntity entity, WindowManager.LayoutParams mParams, boolean add, float startY, float startX) {
        this.setUpItem(entity, mParams, add, startY, startX, true);

    }

    private void setUpItem(FloatingSpriteEntity entity, WindowManager.LayoutParams mParams, boolean add, float startY, float startX, boolean smoothMove) {

        entity.setX(mParams.x);
        entity.setY(mParams.y);
        if (smoothMove) {
            smoothMoveEntity(entity, startX, startY);
        } else {
            entity.setX(startX);
            entity.setY(startY);
        }
        touchManager.addEntity(entity);
        if (add) {
            entities.add(0, entity);
        }

    }

    private boolean checkMotionEventBoundContent(MotionEvent event) {
        if (contentView == null) {
            return false;
        }
        final float x = event.getX();
        final float y = event.getY();
        return x >= contentView.getLeft() && x <= contentView.getLeft() + contentView.getWidth() && y >= contentView.getTop() && y <= contentView.getTop() + contentView.getHeight();
    }

    protected void moveFloatingToEdge() {
        mVelocityTracker.computeCurrentVelocity(500);
        float velocityX = mVelocityTracker.getXVelocity();
        float velocityY = mVelocityTracker.getYVelocity();
        Log.d("", "velocitytracker " + velocityX + " " + velocityY);
        if (Math.abs(velocityX) >= MINFILLING || Math.abs(velocityY) >= MINFILLING) {
            float x, y;
            float tangAlpha;
            if (velocityX == 0) {
                tangAlpha = 1;
            } else {
                tangAlpha = Math.abs(velocityY / velocityX);
            }

            if (velocityX > 0) {
                if (velocityY < 0) {
                    y = floatingEntity.getY() - tangAlpha * (screenWidth - floatingEntity.getX());
                    if (Math.abs(velocityX) >= MINFILLING) {
                        x = screenWidth - floatingEntity.getWidth();
                    } else {
                        if (floatingEntity.getX() < screenWidth / 2) {
                            x = 0;
                        } else {
                            x = screenWidth - floatingEntity.getWidth();
                        }
                    }

                    Log.d("", "velocitytracker floatingEntity.getY() " + floatingEntity.getY() + " tangAlpha " + tangAlpha + " (screenWidth - floatingEntity.getX()) " + (screenWidth - floatingEntity.getX()));
                } else {
                    y = floatingEntity.getY() + tangAlpha * (screenWidth - floatingEntity.getX());
                    if (Math.abs(velocityX) >= MINFILLING) {
                        x = screenWidth - floatingEntity.getWidth();
                    } else {
                        if (floatingEntity.getX() < screenWidth / 2) {
                            x = 0;
                        } else {
                            x = screenWidth - floatingEntity.getWidth();
                        }
                    }
                }
            } else {
                if (velocityY < 0) {
                    y = floatingEntity.getY() - tangAlpha * (floatingEntity.getX());
                    if (Math.abs(velocityX) >= MINFILLING) {
                        x = 0;
                    } else {
                        if (floatingEntity.getX() < screenWidth / 2) {
                            x = 0;
                        } else {
                            x = screenWidth - floatingEntity.getWidth();
                        }
                    }
                } else {
                    y = floatingEntity.getY() + tangAlpha * (floatingEntity.getX());
                    if (Math.abs(velocityX) >= MINFILLING) {
                        x = 0;
                    } else {
                        if (floatingEntity.getX() < screenWidth / 2) {
                            x = 0;
                        } else {
                            x = screenWidth - floatingEntity.getWidth();
                        }
                    }
                }
            }
            Log.d("", "velocitytracker smoothMoveFloating " + x + " " + y);

            if (x < 0) {
                x = 0;
            } else if (x > screenWidth - floatingEntity.getWidth()) {
                x = screenWidth - floatingEntity.getWidth();
            }

            if (y < 0) {
                y = 0;
            } else if (y > screenHeight - floatingEntity.getWidth()) {
                y = screenHeight - floatingEntity.getWidth();
            }
            Log.d("", "velocitytracker smoothMoveFloating after " + x + " " + y);
            smoothMoveFloating((int) x, (int) y, true);
        } else {
            WindowManager.LayoutParams params = (WindowManager.LayoutParams) getLayoutParams();
            int x;
            int y = params.y;
            if (params.x > screenWidth / 2) {
                x = screenWidth - getWidth();
            } else {
                x = 0;
            }
            if (y < 0) {
                y = 0;
            } else if (y + getHeight() > screenHeight) {
                y = screenHeight - getHeight();
            }
            smoothMoveFloating(x, y, true);
        }

    }

    private void smoothMoveFloating(int x, int y) {
        smoothMoveFloating(x, y, false);
    }

    private void smoothMoveFloating(int x, int y, boolean hide) {
        floatingEntity.removeModifiers();
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) getLayoutParams();
        floatingEntity.setX(params.x);
        floatingEntity.setY(params.y);
        tranformModifier.updateValue(x, y);
        tranformModifier.setDuration(SHOW_DURATION);
        Log.d("", "duration time " + tranformModifier.getDuration() + " desX " + x + " desY " + y);
        floatingEntity.addModifier(tranformModifier);
        entities.add(floatingEntity);
        if (hide) {
            floatingEntity.setCallback(new AnimationExecutor() {
                @Override
                public void execute(Entity entity) {
                    executeHide();
                }
            });
        }
    }

    private void moveFloatingBy(int divX, int divY) {
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) getLayoutParams();
        params.x += divX;
        params.y += divY;
        floatingEntity.setX(params.x);
        floatingEntity.setY(params.y);
        windowManager.updateViewLayout(this, params);
    }

    protected void moveFloating(int x, int y) {
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) getLayoutParams();
        params.x = x;
        params.y = y;
        floatingEntity.setX(params.x);
        floatingEntity.setY(params.y);
        windowManager.updateViewLayout(this, params);
        Log.d("", "movefloating " + x + " " + y + " launcher entity " + launcherEntity.getX() + " " + launcherEntity.getY());
    }

    protected void cancelHide() {
        hideLauncherHandler.removeMessages(HIDE_WHAT);
    }

    public void executeHide() {
        hideLauncherHandler.sendEmptyMessageDelayed(HIDE_WHAT, 2000);
    }

    private void hideFloating() {
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) getLayoutParams();
        Log.d("", "hideFloating " + params.x);
        if (params.x == 0) {
            smoothMoveEntity(launcherEntity, (int) (-launcherEntity.getWidth() / 2), launcherEntity.getY(), LinearInterpolator.getInstance());
        } else {
            smoothMoveEntity(launcherEntity, (int) (launcherEntity.getWidth() / 2), launcherEntity.getY(), LinearInterpolator.getInstance());
        }
        onHide(params.x);
        layoutType = LayoutType.Hide;
    }

    private void showFloating() {
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) getLayoutParams();
        Log.d("", "showFloating " + params.x);
        if (params.x == 0) {
            smoothMoveEntity(launcherEntity, 0, launcherEntity.getY(), LinearInterpolator.getInstance());
        } else {
            smoothMoveEntity(launcherEntity, 0, launcherEntity.getY(), LinearInterpolator.getInstance());
        }
        onShow(params.x);
        layoutType = LayoutType.Collapse;
        executeHide();
    }

    private void smoothMoveEntity(Entity entity, float x, float y) {
        smoothMoveEntity(entity, x, y, BackOutInterpolator.getInstance());

    }

    private void smoothMoveEntity(Entity entity, float x, float y, Interpolator interpolator) {
        if (entity instanceof FloatingSpriteEntity)
            ((FloatingSpriteEntity) entity).updateAnimation(x, y, interpolator);

    }

    private void calculateScreenDimension() {
        calculateScreenDimension(getResources().getConfiguration());
    }

    protected void calculateScreenDimension(Configuration configuration) {
        orientation = configuration.orientation;
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        Point naviPoint = ScreenCaculator.getNavigationBarSize(getContext());
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels - ScreenCaculator.getStatusBarHeight(getContext()) - naviPoint.y;
        Log.d("", "onconfigchange screen width " + screenWidth + " " + screenHeight + " navigation height " + naviPoint.y);
    }

    public void setMarginOnExpand(float marginOnExpand) {
        this.marginOnExpand = marginOnExpand;
    }

    @Override
    protected void onUpdate(long elapseTime) {

    }

    @Override
    protected void onDispose() {
    }

    public FloatingSpriteEntity findItem(int id){
        int size = expandItems.size();
        for (int i = 0; i < size; i++) {
            FloatingSpriteEntity entity = expandItems.get(i);
            if(entity.getId() == id){
                return entity;
            }
        }
        return null;
    }

    public boolean isAnimationLock() {
        return animationLock;
    }

    public void setAnimationLock(boolean animationLock) {
        this.animationLock = animationLock;
    }

    public float getMarginOnExpand() {
        return marginOnExpand;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public void setScreenHeight(int screenHeight) {
        this.screenHeight = screenHeight;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public void setScreenWidth(int screenWidth) {
        this.screenWidth = screenWidth;
    }

    public FloatingSpriteEntity getLauncherEntity(){
        return launcherEntity;
    }

    public enum LayoutType {
        Expand, Collapse, Hide, Tutorial
    }


    private static class HideLauncherHandler extends Handler {

        private WeakReference<BaseFloatingView> reference;

        public HideLauncherHandler(BaseFloatingView param) {
            reference = new WeakReference<>(param);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (reference != null && reference.get() != null) {
                reference.get().hideFloating();
            }
        }
    }
}
