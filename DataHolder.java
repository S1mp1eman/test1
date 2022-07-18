package com.wzt.yolov5;

import android.graphics.Bitmap;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

    public class DataHolder {
        private static Map dataList = new HashMap<>();
        private static volatile DataHolder instance;
        public static DataHolder getInstance() {
            if(instance==null) {
                synchronized(DataHolder.class) {
            if(instance==null) {
                instance = new DataHolder();
            }
        }
    }
    return instance;
    }

    public static void setData(String key, Bitmap o) {
        WeakReference value =new WeakReference<>(o);
        dataList.put(key, value);
    }

    public static Object getData(String key) {
        WeakReference reference = (WeakReference) dataList.get(key);
        if(reference !=null) {
            Bitmap o = (Bitmap) reference.get();
        return o;
        }
        return null;
    }
}
