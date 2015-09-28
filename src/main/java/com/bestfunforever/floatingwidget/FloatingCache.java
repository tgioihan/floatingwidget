package com.bestfunforever.floatingwidget;

import java.util.HashMap;

public class FloatingCache {

    private HashMap<Integer, BaseFloatingContent> mCaches;

    public FloatingCache() {
        this.mCaches = new HashMap<>();
    }

    public void addCache(BaseFloatingContent item){
        mCaches.put(item.getId(),item);
    }

    public BaseFloatingContent getItem(int id){
        return mCaches.get(id);
    }
}
