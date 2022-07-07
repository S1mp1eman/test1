package com.wzt.yolov5;

import android.content.res.AssetManager;
import android.graphics.Bitmap;

public class Zwell {
    static {
        System.loadLibrary("yolov5");  // 存放在yolov5.so中
    }

    public static native void init(AssetManager manager, boolean useGPU);
    public static native ZWellBox[] detect(Bitmap bitmap, double threshold, double nms_threshold);
}
