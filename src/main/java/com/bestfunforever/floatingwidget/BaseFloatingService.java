package com.bestfunforever.floatingwidget;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.support.v7.app.NotificationCompat;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

public abstract class BaseFloatingService extends Service {
    private static final int FOREGROUNDID = 4456;

    protected BaseFloatingView floatingItem;
    private View launcherTut;

    protected abstract String getActionOpen();

    protected abstract String getActionClose() ;

    protected abstract BaseFloatingView onCreateFloatingWidget(Intent intent);

    protected abstract void initialFloatingLayoutParam(WindowManager.LayoutParams defaultLayoutParam) ;

    protected abstract void onStartForeground(NotificationCompat.Builder notificationBuilder) ;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action.equals(getActionOpen())) {
                onOpen(intent);
            } else if (action.equals(getActionClose())) {
                onClose(intent);
                stopSelf();
            }
        }else{
            onOpen(intent);
        }
        return START_STICKY;
    }

    protected void onClose(Intent intent) {
        WindowManager mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        if (floatingItem != null) {
            mWindowManager.removeViewImmediate(floatingItem);
        }
    }


    protected void  onOpen(Intent intent) {
        if (floatingItem == null) {
            WindowManager mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            floatingItem = onCreateFloatingWidget(intent);
            floatingItem.setBackgroundColor(getApplicationContext().getResources().getColor(android.R.color.transparent));
            WindowManager.LayoutParams params = getDefaultLayoutParam();
            mWindowManager.addView(floatingItem, params);
            initialFloatingLayoutParam(params);
            startForegound();
        }
    }

    protected WindowManager.LayoutParams getDefaultLayoutParam(){
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED |  WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                ,
                PixelFormat.TRANSLUCENT);
        try {
            params.getClass().getField("privateFlags").set(params, 0x00000040);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        params.gravity = Gravity.START | Gravity.TOP;
        return params;
    }

    private void startForegound() {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
        notificationBuilder.setAutoCancel(false).setDefaults(-1);
        onStartForeground(notificationBuilder);
        startForeground(FOREGROUNDID, notificationBuilder.build());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        destroy();
    }

    private void destroy() {
        WindowManager mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        if (floatingItem != null) {
            floatingItem.dispose();
            if (floatingItem.getParent() != null)
                mWindowManager.removeViewImmediate(floatingItem);
        }
        onDestroyFloatingWidget();
        stopForeground(true);
    }

    protected void onDestroyFloatingWidget() {

    }
}
