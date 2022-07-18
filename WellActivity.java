package com.wzt.yolov5;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.camera.core.UseCase;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.LifecycleOwner;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Size;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class WellActivity extends AppCompatActivity {
    public static int NANODET = 1;
    public static int YOLOV5S = 2;
    public static int YOLOV4_TINY = 3;

    public static int USE_MODEL = NANODET;
    public static boolean USE_GPU = false;

    public String temp="mouse";
    public String temp2="laptop";
    public int flag;
    public int flagsensor;

    public static CameraX.LensFacing CAMERA_ID = CameraX.LensFacing.BACK;

    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_PICK_IMAGE = 2;
    private static String[] PERMISSIONS_CAMERA = {
            Manifest.permission.CAMERA
    };
    private ImageView resultImageView;
    private SeekBar nmsSeekBar;
    private SeekBar thresholdSeekBar;
    private TextView thresholdTextview;
    private TextView tvInfo;
    private double threshold = 0.3, nms_threshold = 0.7;
    private TextureView viewFinder;

    private AtomicBoolean detecting = new AtomicBoolean(false);
    private AtomicBoolean detectPhoto = new AtomicBoolean(false);


    private long startTime = 0;
    private long endTime = 0;
    private int width;
    private int height;

    public SeekBar sb1;
    public SeekBar sb2;

    public CheckBox cb1;
    public CheckBox cb2;
    public int add;
    double total_fps = 0;
    int fps_count = 0;

    protected Bitmap mutableBitmap;
//    protected Bitmap mutableBitmap;
    ExecutorService detectService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_well);
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_CAMERA,
                    REQUEST_CAMERA
            );
            finish();
        }

        Well.init(getAssets(), USE_GPU);
        SimplePose.init(getAssets(), USE_GPU);
        add = getIntent().getIntExtra("add", 1);
        switch(add){
            case 1:
            {
                TextView text31 = findViewById(R.id.textView9);
                //设置text01的文本值，此处会覆盖掉activity_main.xml的文本值
                text31.setText("任务桩号:R1001");
                TextView text311 = findViewById(R.id.textView12);
                text311.setText("定位桩号:R1001");
//                TextView text2 = findViewById(R.id.textView3);
//                //设置text01的文本值，此处会覆盖掉activity_main.xml的文本值
//                text2.setText("已完成");
                break;
            }
            case 2:
            {
                TextView text32 = findViewById(R.id.textView9);
                //设置text01的文本值，此处会覆盖掉activity_main.xml的文本值
                text32.setText("任务桩号:R1002");
                TextView text312 = findViewById(R.id.textView12);
                text312.setText("定位桩号:R1002");
                break;
            }
            case 3:
            {
                TextView text33 = findViewById(R.id.textView9);
                //设置text01的文本值，此处会覆盖掉activity_main.xml的文本值
                text33.setText("任务桩号:R1003");
                TextView text313 = findViewById(R.id.textView12);
                text313.setText("定位桩号:R1003");
                break;
            }
            case 4:
            {
                TextView text34 = findViewById(R.id.textView9);
                //设置text01的文本值，此处会覆盖掉activity_main.xml的文本值
                text34.setText("任务桩号:R1004");
                TextView text314 = findViewById(R.id.textView12);
                text314.setText("定位桩号:R1004");
                break;
            }
            case 5:
            {
                TextView text35 = findViewById(R.id.textView9);
                //设置text01的文本值，此处会覆盖掉activity_main.xml的文本值
                text35.setText("任务桩号:R1005");
                TextView text315 = findViewById(R.id.textView12);
                text315.setText("定位桩号:R1005");
                break;
            }
        }
        sb1=findViewById(R.id.sb_video_speed);
        sb1.setVisibility(View.GONE);
        sb2=findViewById(R.id.sb_video);
        sb2.setVisibility(View.GONE);


        cb1=findViewById(R.id.chb_1);
        cb1.setClickable(false);
        cb2=findViewById(R.id.chb_2);
        cb2.setClickable(false);

        resultImageView = findViewById(R.id.imageView);
        thresholdTextview = findViewById(R.id.valTxtView);
        tvInfo = findViewById(R.id.tv_info);
        nmsSeekBar = findViewById(R.id.nms_seek);
        thresholdSeekBar = findViewById(R.id.threshold_seek);
        if (USE_MODEL != YOLOV5S && USE_MODEL != NANODET) {
            nmsSeekBar.setEnabled(false);
            thresholdSeekBar.setEnabled(false);
        } else if (USE_MODEL == YOLOV5S) {
            threshold = 0.3f;
            nms_threshold = 0.7f;
        } else if (USE_MODEL == NANODET) {
            threshold = 0.4f;
            nms_threshold = 0.6f;
        }
        nmsSeekBar.setProgress((int) (nms_threshold * 100));
        thresholdSeekBar.setProgress((int) (threshold * 100));
        final String format = "Thresh: %.2f, NMS: %.2f";
        thresholdTextview.setText(String.format(Locale.ENGLISH, format, threshold, nms_threshold));
        nmsSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                nms_threshold = i / 100.f;
                thresholdTextview.setText(String.format(Locale.ENGLISH, format, threshold, nms_threshold));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        thresholdSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                threshold = i / 100.f;
                thresholdTextview.setText(String.format(Locale.ENGLISH, format, threshold, nms_threshold));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        Button inference = findViewById(R.id.button);
        inference.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_PICK_IMAGE);
            }
        });

        resultImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                detectPhoto.set(false);
            }
        });

        viewFinder = findViewById(R.id.view_finder);
        viewFinder.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
                updateTransform();
            }
        });

        viewFinder.post(new Runnable() {
            @Override
            public void run() {
                startCamera();
            }
        });
    }

    private void updateTransform() {
        Matrix matrix = new Matrix();
        // Compute the center of the view finder
        float centerX = viewFinder.getWidth() / 2f;
        float centerY = viewFinder.getHeight() / 2f;

        float[] rotations = {0, 90, 180, 270};
        // Correct preview output to account for display rotation
        float rotationDegrees = rotations[viewFinder.getDisplay().getRotation()];

        matrix.postRotate(-rotationDegrees, centerX, centerY);

        // Finally, apply transformations to our TextureView
        viewFinder.setTransform(matrix);
    }

    private void startCamera() {
        CameraX.unbindAll();
        // 1. preview
        PreviewConfig previewConfig = new PreviewConfig.Builder()
                .setLensFacing(CAMERA_ID)
//                .setTargetAspectRatio()  // 宽高比
                .setTargetResolution(new Size(320, 320))  // 分辨率
                .build();

        Preview preview = new Preview(previewConfig);
        preview.setOnPreviewOutputUpdateListener(new Preview.OnPreviewOutputUpdateListener() {
            @Override
            public void onUpdated(Preview.PreviewOutput output) {
                ViewGroup parent = (ViewGroup) viewFinder.getParent();
                parent.removeView(viewFinder);
                parent.addView(viewFinder, 0);

                viewFinder.setSurfaceTexture(output.getSurfaceTexture());
                updateTransform();
            }
        });
        DetectAnalyzer detectAnalyzer = new DetectAnalyzer();
        CameraX.bindToLifecycle((LifecycleOwner) this, preview, gainAnalyzer(detectAnalyzer));

    }


    private UseCase gainAnalyzer(DetectAnalyzer detectAnalyzer) {
        ImageAnalysisConfig.Builder analysisConfigBuilder = new ImageAnalysisConfig.Builder();
        analysisConfigBuilder.setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE);
        analysisConfigBuilder.setTargetResolution(new Size(320, 320));  // 输出预览图像尺寸
        ImageAnalysisConfig config = analysisConfigBuilder.build();
        ImageAnalysis analysis = new ImageAnalysis(config);
        analysis.setAnalyzer(detectAnalyzer);
        return analysis;
    }

    private Bitmap imageToBitmap(ImageProxy image) {
        byte[] nv21 = imagetToNV21(image);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 100, out);
        byte[] imageBytes = out.toByteArray();

        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    private byte[] imagetToNV21(ImageProxy image) {
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        ImageProxy.PlaneProxy y = planes[0];
        ImageProxy.PlaneProxy u = planes[1];
        ImageProxy.PlaneProxy v = planes[2];
        ByteBuffer yBuffer = y.getBuffer();
        ByteBuffer uBuffer = u.getBuffer();
        ByteBuffer vBuffer = v.getBuffer();
        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();
        byte[] nv21 = new byte[ySize + uSize + vSize];
        // U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        return nv21;
    }

    private class DetectAnalyzer implements ImageAnalysis.Analyzer {

        @Override
        public void analyze(ImageProxy image, final int rotationDegrees) {
            detectOnModel(image, rotationDegrees);
        }
    }


    private void detectOnModel(ImageProxy image, final int rotationDegrees) {
        if (detecting.get() || detectPhoto.get()) {
            return;
        }
        detecting.set(true);
        startTime = System.currentTimeMillis();
        final Bitmap bitmapsrc = imageToBitmap(image);  // 格式转换
        if (detectService == null) {
            detecting.set(false);
            return;
        }
        detectService.execute(new Runnable() {
            @Override
            public void run() {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotationDegrees);
                width = bitmapsrc.getWidth();
                height = bitmapsrc.getHeight();
                Bitmap bitmap = Bitmap.createBitmap(bitmapsrc, 0, 0, Math.min(width, height), Math.min(width, height), matrix, false);
                KeyPoint[] keyPoints = null;
                WellBox[] result = null;
//                if (USE_MODEL == YOLOV5S) {
//                    result = YOLOv5.detect(bitmap, threshold, nms_threshold);
//                } else if (USE_MODEL == YOLOV4_TINY) {
//                    result = YOLOv4.detect(bitmap, threshold, nms_threshold);
//                } else

                    if (USE_MODEL == NANODET) {
                    result = Well.detect(bitmap, threshold, nms_threshold);
                    keyPoints = SimplePose.detect(bitmap);
                }
                if (result == null) {
                    detecting.set(false);
                    return;
                }
                mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                if (USE_MODEL == YOLOV5S || USE_MODEL == YOLOV4_TINY || USE_MODEL == NANODET) {
                    mutableBitmap = drawBoxRects(mutableBitmap, result);
                    mutableBitmap = drawPersonPose(mutableBitmap, keyPoints);
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        detecting.set(false);
                        if (detectPhoto.get()) {
                            return;
                        }
                        resultImageView.setImageBitmap(mutableBitmap);
                        endTime = System.currentTimeMillis();
                        long dur = endTime - startTime;
                        float fps = (float) (1000.0 / dur);
                        total_fps = (total_fps == 0) ? fps : (total_fps + fps);
                        fps_count++;
                        String modelName = getModelName();

//                        tvInfo.setText(String.format(Locale.CHINESE,
//                                "%s\nSize: %dx%d\nTime: %.3f s\nFPS: %.3f\nAVG_FPS: %.3f",
//                                modelName, height, width, dur / 1000.0, fps, (float) total_fps / fps_count));
                    }
                });
            }
        });
    }

    Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                CheckBox cb1=findViewById(R.id.chb_1);
                cb1.setChecked(true);
            }
        }
    };

    Handler handler1 = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                CheckBox cb1=findViewById(R.id.chb_1);
                cb1.setChecked(false);
            }
        }
    };
    Handler handler2 = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                CheckBox cb2=findViewById(R.id.chb_2);
                cb2.setChecked(true);
            }
        }
    };

    Handler handler3 = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                CheckBox cb2=findViewById(R.id.chb_2);
                cb2.setChecked(false);
            }
        }
    };

    protected Bitmap drawBoxRects(Bitmap mutableBitmap, WellBox[] results) {
        int flag=0;
        int flagsensor=0;
        boolean first = false;
        boolean second = true;
        if (results == null || results.length <= 0) {
            return mutableBitmap;
        }
        Canvas canvas = new Canvas(mutableBitmap);
        final Paint boxPaint = new Paint();
        boxPaint.setAlpha(200);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(4 * mutableBitmap.getWidth() / 800.0f);
        boxPaint.setTextSize(40 * mutableBitmap.getWidth() / 800.0f);
        for (WellBox box : results) {
            boxPaint.setColor(box.getColor());
            boxPaint.setStyle(Paint.Style.FILL);
            canvas.drawText(box.getLabel() + String.format(Locale.CHINESE, " %.3f", box.getScore()), box.x0 + 3, box.y0 + 40 * mutableBitmap.getWidth() / 1000.0f, boxPaint);
            boxPaint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(box.getRect(), boxPaint);
        }

        for(int i=0;i<results.length;i++){
            if(temp.equals(results[i].getLabel())){
                flag=1;
            }
            if(temp2.equals(results[i].getLabel())){
                flagsensor=1;
            }
        }

        if(flag==0 && !first){
            new Thread(() -> {
                Message msg = new Message();
                msg.what = 1;
                handler1.sendMessage(msg);
            }).start();
        }

        else if(flag==1){
            first = true;
            new Thread(() -> {
                Message msg = new Message();
                msg.what = 1;
                handler.sendMessage(msg);
            }).start();
        }

        if(flagsensor==0 && !second){
            new Thread(() -> {
                Message msg = new Message();
                msg.what = 1;
                handler3.sendMessage(msg);
            }).start();
        }
        else if(flagsensor==1){
            second = true;
            new Thread(() -> {
                Message msg = new Message();
                msg.what = 1;
                handler2.sendMessage(msg);
            }).start();
        }

        return mutableBitmap;
    }


    protected String getModelName() {
        String modelName = "ohhhhh";
        if (USE_MODEL == YOLOV5S) {
            modelName = "YOLOv5s";
        } else if (USE_MODEL == YOLOV4_TINY) {
            modelName = "YOLOv4-tiny";
        } else if (USE_MODEL == NANODET) {
            modelName = "NanoDet";
        }
        return USE_GPU ? "GPU: " + modelName : "CPU: " + modelName;
    }

    @Override
    protected void onDestroy() {
        if (detectService != null) {
            detectService.shutdown();
            detectService = null;
        }
        CameraX.unbindAll();
        super.onDestroy();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera Permission!", Toast.LENGTH_SHORT).show();
                this.finish();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) {
            return;
        }
        detectPhoto.set(true);
        Bitmap image = getPicture(data.getData());
        if (image == null) {
            Toast.makeText(this, "Photo is null", Toast.LENGTH_SHORT).show();
            return;
        }
        Bitmap mutableBitmap = image.copy(Bitmap.Config.ARGB_8888, true);
        KeyPoint[] keyPoints = null;
        WellBox[] result = null;
////        if (USE_MODEL == YOLOV5S) {
////            result = YOLOv5.detect(image, threshold, nms_threshold);
////        } else if (USE_MODEL == YOLOV4_TINY) {
////            result = YOLOv4.detect(image, threshold, nms_threshold);
////        } else
            if (USE_MODEL == NANODET) {
            result = Well.detect(image, threshold, nms_threshold);
            keyPoints = SimplePose.detect(image);
        }
        if (USE_MODEL == YOLOV5S || USE_MODEL == YOLOV4_TINY|| USE_MODEL == NANODET) {
            mutableBitmap = drawBoxRects(mutableBitmap, result);
            mutableBitmap = drawPersonPose(image, keyPoints);
        }
        resultImageView.setImageBitmap(mutableBitmap);

    }
    protected Bitmap drawPersonPose(Bitmap mutableBitmap, KeyPoint[] keyPoints) {
        if (keyPoints == null || keyPoints.length <= 0) {
            return mutableBitmap;
        }
        // draw bone
        // 0 nose, 1 left_eye, 2 right_eye, 3 left_Ear, 4 right_Ear, 5 left_Shoulder, 6 rigth_Shoulder, 7 left_Elbow, 8 right_Elbow,
        // 9 left_Wrist, 10 right_Wrist, 11 left_Hip, 12 right_Hip, 13 left_Knee, 14 right_Knee, 15 left_Ankle, 16 right_Ankle
        int[][] joint_pairs = {{0, 1}, {1, 3}, {0, 2}, {2, 4}, {5, 6}, {5, 7}, {7, 9}, {6, 8}, {8, 10}, {5, 11}, {6, 12}, {11, 12}, {11, 13}, {12, 14}, {13, 15}, {14, 16}};
        Canvas canvas = new Canvas(mutableBitmap);
        final Paint keyPointPaint = new Paint();
        keyPointPaint.setAlpha(200);
        keyPointPaint.setStyle(Paint.Style.STROKE);
        keyPointPaint.setColor(Color.BLUE);
        int color = Color.BLUE;
        // 画线、画框、画点
        for (int i = 0; i < keyPoints.length; i++) {
            // 其它随机颜色
            Random random = new Random(i + 2020);
            color = Color.argb(255, random.nextInt(256), random.nextInt(256), random.nextInt(256));
            // 画线
            keyPointPaint.setStrokeWidth(5 * mutableBitmap.getWidth() / 800.0f);
            for (int j = 0; j < 16; j++) {  // 17个点连成16条线
                int pl0 = joint_pairs[j][0];
                int pl1 = joint_pairs[j][1];
                // 人体左侧改为红线
                if ((pl0 % 2 == 1) && (pl1 % 2 == 1) && pl0 >= 5 && pl1 >= 5) {
                    keyPointPaint.setColor(Color.RED);
                } else {
                    keyPointPaint.setColor(color);
                }
                canvas.drawLine(keyPoints[i].x[joint_pairs[j][0]], keyPoints[i].y[joint_pairs[j][0]],
                        keyPoints[i].x[joint_pairs[j][1]], keyPoints[i].y[joint_pairs[j][1]],
                        keyPointPaint);
            }
            // 画点
            keyPointPaint.setColor(Color.GREEN);
            keyPointPaint.setStrokeWidth(8 * mutableBitmap.getWidth() / 800.0f);
            for (int n = 0; n < 17; n++) {
                canvas.drawPoint(keyPoints[i].x[n], keyPoints[i].y[n], keyPointPaint);
            }
            // 画框
            keyPointPaint.setColor(color);
            keyPointPaint.setStrokeWidth(3 * mutableBitmap.getWidth() / 800.0f);
            canvas.drawRect(keyPoints[i].x0, keyPoints[i].y0, keyPoints[i].x1, keyPoints[i].y1, keyPointPaint);
        }
        return mutableBitmap;
    }
    public Bitmap getPicture(Uri selectedImage) {
        String[] filePathColumn = {MediaStore.Images.Media.DATA};
        Cursor cursor = this.getContentResolver().query(selectedImage, filePathColumn, null, null, null);
        if (cursor == null) {
            return null;
        }
        cursor.moveToFirst();
        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String picturePath = cursor.getString(columnIndex);
        cursor.close();
        Bitmap bitmap = BitmapFactory.decodeFile(picturePath);
        if (bitmap == null) {
            return null;
        }
        int rotate = readPictureDegree(picturePath);
        return rotateBitmapByDegree(bitmap, rotate);
    }

    public int readPictureDegree(String path) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }

    public Bitmap rotateBitmapByDegree(Bitmap bm, int degree) {
        Bitmap returnBm = null;
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        try {
            returnBm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(),
                    bm.getHeight(), matrix, true);
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        }
        if (returnBm == null) {
            returnBm = bm;
        }
        if (bm != returnBm) {
            bm.recycle();
        }
        return returnBm;
    }


}
