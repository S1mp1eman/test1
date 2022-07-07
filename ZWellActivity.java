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
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
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
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.wzt.yolov5.ocr.ChineseOCRLite;
import com.wzt.yolov5.ocr.OCRResult;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class ZWellActivity extends AppCompatActivity {

    public static int NANODET = 1;
    public static int YOLOV5S = 2;
    public static int YOLOV4_TINY = 3;
    public boolean globalSwitch = false;

    private Bitmap finalBitmap;
    public String layout = "";
    public static int USE_MODEL = NANODET;
    public static boolean USE_GPU = false;

    public String temp = "concentrator";
    public String temp2 = "surveyLine";
    public int flag;
    public int flagsensor;

    public Button submit;

    public static CameraX.LensFacing CAMERA_ID = CameraX.LensFacing.BACK;

    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_PICK_IMAGE = 2;
    private static String[] PERMISSIONS_CAMERA = {
            Manifest.permission.CAMERA
    };

    private ImageView photoImage;
    private ImageView resultImageView;
    private SeekBar nmsSeekBar;
    private SeekBar thresholdSeekBar;
    private TextView thresholdTextview;
    private TextView tvInfo;
    private double threshold = 0.3, nms_threshold = 0.7;
    private TextureView viewFinder;
    private final String filePath = Environment.getExternalStorageDirectory() + File.separator + "output_image.jpg";
    private AtomicBoolean detecting = new AtomicBoolean(false);
    private AtomicBoolean detectPhoto = new AtomicBoolean(false);

    private EditText resultText;
    public static final int TAKE_PHOTO = 1;//声明一个请求码，用于识别返回的结果
    private Uri imageUri;

    private long startTime = 0;
    private long endTime = 0;
    private int width;
    private int height;
    public int add;

    boolean photoFlag = false;

    public SeekBar sb1;
    public SeekBar sb2;

    public CheckBox cb1;
    public CheckBox cb2;

    double total_fps = 0;
    int fps_count = 0;

    protected Bitmap mutableBitmap;

    ExecutorService detectService = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zwell);
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

        photoImage = findViewById(R.id.photoImage2);
        Zwell.init(getAssets(), USE_GPU);
        add = getIntent().getIntExtra("add", 1);
        switch(add){
            case 1:
            {
                TextView text11 = findViewById(R.id.textView);
                //设置text01的文本值，此处会覆盖掉activity_main.xml的文本值
                text11.setText("任务桩号:R1001");
                TextView text111 = findViewById(R.id.textView11);
                text111.setText("定位桩号:R1001");
//                TextView text2 = findViewById(R.id.textView3);
//                //设置text01的文本值，此处会覆盖掉activity_main.xml的文本值
//                text2.setText("已完成");
                break;
            }
            case 2:
            {
                TextView text12 = findViewById(R.id.textView);
                //设置text01的文本值，此处会覆盖掉activity_main.xml的文本值
                text12.setText("任务桩号:R1002");
                TextView text112 = findViewById(R.id.textView11);
                text112.setText("定位桩号:R1002");
                break;
            }
            case 3:
            {
                TextView text13 = findViewById(R.id.textView);
                //设置text01的文本值，此处会覆盖掉activity_main.xml的文本值
                text13.setText("任务桩号:R1003");
                TextView text113 = findViewById(R.id.textView11);
                text113.setText("定位桩号:R1003");
                break;
            }
            case 4:
            {
                TextView text14 = findViewById(R.id.textView);
                //设置text01的文本值，此处会覆盖掉activity_main.xml的文本值
                text14.setText("任务桩号:R1004");
                TextView text114 = findViewById(R.id.textView11);
                text114.setText("定位桩号:R1004");
                break;
            }
            case 5:
            {
                TextView text15 = findViewById(R.id.textView);
                //设置text01的文本值，此处会覆盖掉activity_main.xml的文本值
                text15.setText("任务桩号:R1005");
                TextView text115 = findViewById(R.id.textView11);
                text115.setText("定位桩号:R1005");
                break;
            }
        }
        sb1 = findViewById(R.id.sb_video_speed);
        sb1.setVisibility(View.GONE);
        sb2 = findViewById(R.id.sb_video);
        sb2.setVisibility(View.GONE);

        submit = findViewById(R.id.submit_1);
        submit.setOnClickListener(view ->{
            String allText = resultText.getText().toString();
            if(allText.contains("R")){
                layout=allText.substring(allText.indexOf("R"),allText.indexOf("R")+5);
                new AlertDialog.Builder(ZWellActivity.this)
                        .setTitle("点号为："+ layout)
                        .setMessage("是否更新质检信息")
                        .setPositiveButton("是", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new Thread(){
                                    public void run(){
                                        Message msg = new Message();
                                        msg.what = 2;
                                        handler.sendMessage(msg);
                                        Log.i("gggggg", String.valueOf(msg.what));

                                        System.exit(0);
                                        overridePendingTransition(0, 0);
//                                        android.os.Process.killProcess(android.os.Process.myPid());
//                                        Intent intent = new Intent(ZWellActivity.this,MainActivity.class);
//                                        startActivity(intent);
                                    }
                                }.start();
                            }

                        })
                        .setNegativeButton("否", null)
                        .show();
            }else{
                Toast.makeText(getBaseContext(), "请输入正确的点号，如R1001", Toast.LENGTH_SHORT).show();
            }
        });
        cb1 = findViewById(R.id.chb_1);
        cb1.setClickable(false);
        cb2 = findViewById(R.id.chb_2);
        cb2.setClickable(false);

        Switch aSwitch = findViewById(R.id.switch1);

        aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Toast.makeText(getBaseContext(), "检测开始!", Toast.LENGTH_SHORT).show();
                    globalSwitch = true;
                } else {
                    Toast.makeText(getBaseContext(), "检测中断!", Toast.LENGTH_SHORT).show();
                    globalSwitch = false;
                }
            }
        });

        Button reset = findViewById(R.id.reset);

        Button photo = findViewById(R.id.camera_bt_1);
        photo.setOnClickListener(view->{
            photoFlag = true;
            requestPermission();
            submit.setOnClickListener(newView -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("确认窗口");
                builder.setMessage("确认提交这张照片吗"); //设置内容
                builder.setIcon(R.mipmap.ic_launcher);//设置图标
                builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //TODO
                        photoImage.setVisibility(View.INVISIBLE);
                        reStart();
                    }
                });

                builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        photoImage.setVisibility(View.INVISIBLE);
                        reStart();
                    }
                });
                //显示dialog
                builder.create().show();
            });
        });

        Button back = findViewById(R.id.back);
        back.setOnClickListener(view -> {
            finish();
        });


        resultImageView = findViewById(R.id.imageView);
        thresholdTextview = findViewById(R.id.valTxtView);
        resultText = findViewById(R.id.result_text);
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

        viewFinder.setVisibility(View.GONE);

        reset.setOnClickListener(view -> {
            aSwitch.setChecked(false);
            cb1.setChecked(false);
            cb2.setChecked(false);
            if(photoFlag){
                photoImage.setVisibility(View.GONE);
              startCamera();
            }
        });
    }

    private void reStart(){
        final Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

        //杀掉以前进程
        android.os.Process.killProcess(android.os.Process.myPid());
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

    private void requestCamera() {
        File outputImage = new File(filePath);
                /*
                创建一个File文件对象，用于存放摄像头拍下的图片，我们把这个图片命名为output_image.jpg
                并把它存放在应用关联缓存目录下，调用getExternalCacheDir()可以得到这个目录，为什么要
                用关联缓存目录呢？由于android6.0开始，读写sd卡列为了危险权限，使用的时候必须要有权限，
                应用关联目录则可以跳过这一步
                 */
        try//判断图片是否存在，存在则删除在创建，不存在则直接创建
        {
            if (!outputImage.getParentFile().exists()) {
                outputImage.getParentFile().mkdirs();
            }
            if (outputImage.exists()) {
                outputImage.delete();
            }

            outputImage.createNewFile();

            if (Build.VERSION.SDK_INT >= 24) {
                imageUri = FileProvider.getUriForFile(this,
                        "com.example.mydemo.fileprovider", outputImage);
            } else {
                imageUri = Uri.fromFile(outputImage);
            }
            //使用隐示的Intent，系统会找到与它对应的活动，即调用摄像头，并把它存储
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(intent, TAKE_PHOTO);
            //调用会返回结果的开启方式，返回成功的话，则把它显示出来
        } catch (IOException e) {
            e.printStackTrace();
        }

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
        CameraX.bindToLifecycle(this, preview, gainAnalyzer(detectAnalyzer));

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

    private void requestPermission() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //请求权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, 1);
        } else {
            //调用
            requestCamera();
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

                ZWellBox[] result = null;

                if (USE_MODEL == NANODET) {
                    result = Zwell.detect(bitmap, threshold, nms_threshold);
                }
                if (result == null) {
                    detecting.set(false);
                    return;
                }
                mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                if (USE_MODEL == YOLOV5S || USE_MODEL == YOLOV4_TINY || USE_MODEL == NANODET) {
                    mutableBitmap = drawBoxRects(mutableBitmap, result);
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

                    }
                });
            }
        });
    }

    Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                CheckBox cb1 = findViewById(R.id.chb_1);
                cb1.setChecked(true);
            }
        }
    };

    Handler handler1 = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                CheckBox cb1 = findViewById(R.id.chb_1);
                cb1.setChecked(false);
            }
        }
    };
    Handler handler2 = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                CheckBox cb2 = findViewById(R.id.chb_2);
                cb2.setChecked(true);
            }
        }
    };

    Handler handler3 = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                CheckBox cb2 = findViewById(R.id.chb_2);
                cb2.setChecked(false);
            }
        }
    };

    Handler handler4 = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                Button bt1 = findViewById(R.id.btn_getocr);
                bt1.setEnabled(true);
            }

        }
    };


    protected Bitmap drawBoxRects(Bitmap mutableBitmap, ZWellBox[] results) {
        int flag = 0;
        int flagsensor = 0;


        if (results == null || results.length <= 0) {
            return mutableBitmap;
        }
        Canvas canvas = new Canvas(mutableBitmap);
        final Paint boxPaint = new Paint();
        boxPaint.setAlpha(200);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(4 * mutableBitmap.getWidth() / 800.0f);
        boxPaint.setTextSize(40 * mutableBitmap.getWidth() / 800.0f);
        for (ZWellBox box : results) {
            boxPaint.setColor(box.getColor());
            boxPaint.setStyle(Paint.Style.FILL);
            //canvas.drawText(box.getLabel() + String.format(Locale.CHINESE, " %.3f", box.getScore()), box.x0 + 3, box.y0 + 40 * mutableBitmap.getWidth() / 1000.0f, boxPaint);
            boxPaint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(box.getRect(), boxPaint);
        }

        for (int i = 0; i < results.length; i++) {
            if (temp.equals(results[i].getLabel())) {
                flag = 1;
            }
            if (temp2.equals(results[i].getLabel())) {
                flagsensor = 1;
            }
        }
        if (globalSwitch) {
//            if (flag == 0 && !cb1founded.get()) {
//            new Thread(() -> {
//                Message msg = new Message();
//                msg.what = 1;
//                handler1.sendMessage(msg);
//            }).start();
//        }


            if (flag == 1) {
                new Thread(() -> {
                    Message msg = new Message();
                    msg.what = 1;
                    handler.sendMessage(msg);
                }).start();
            }

//        if(flagsensor==0 && !cb2founded.get()){
//            new Thread(() -> {
//                cb2founded.set(true);
//                Message msg = new Message();
//                msg.what = 1;
//                handler3.sendMessage(msg);
//            }).start();
//        }

            if (flagsensor == 1) {
                new Thread(() -> {
                    Message msg = new Message();
                    msg.what = 1;
                    handler2.sendMessage(msg);
                }).start();
            }

            if (cb1.isChecked() && cb2.isChecked()) {
                new Thread(() -> {
                    Message msg = new Message();
                    msg.what = 1;
                    handler4.sendMessage(msg);

                }).start();

            }
        }
//
        Button bt1 = findViewById(R.id.btn_getocr);
        bt1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finalBitmap = null;
                resultText.setText("正在识别......");
                for (int i = 0; i < results.length; i++) {
                    int needwidth;
                    int needheight;
                    if (temp2.equals(results[i].getLabel())) {
                        needwidth = (int) (results[i].x1 - results[i].x0);
                        needheight = (int) (results[i].y1 - results[i].y0);
                        Bitmap resultbitmap = null;
                        resultbitmap = Bitmap.createBitmap(mutableBitmap, (int) results[i].x0, (int) results[i].y0, needwidth, needheight);
                        finalBitmap = Bitmap.createBitmap(resultbitmap);
                        ChineseOCRLite.init(getAssets(), USE_GPU);
                        break;
                    }
                }
                initView(finalBitmap);
            }
        });

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
        if(photoFlag){
            reStart();
        }

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
        if(requestCode == 2){
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

            ZWellBox[] result = null;
////        if (USE_MODEL == YOLOV5S) {
////            result = YOLOv5.detect(image, threshold, nms_threshold);
////        } else if (USE_MODEL == YOLOV4_TINY) {
////            result = YOLOv4.detect(image, threshold, nms_threshold);
////        } else
//            if (USE_MODEL == NANODET) {
            result = Zwell.detect(image, threshold, nms_threshold);
//        }
            if (USE_MODEL == YOLOV5S || USE_MODEL == YOLOV4_TINY || USE_MODEL == NANODET) {
                mutableBitmap = drawBoxRects(mutableBitmap, result);
            }
            resultImageView.setImageBitmap(mutableBitmap);
        }


        if (requestCode == TAKE_PHOTO) {
            if (resultCode == RESULT_OK) {
                try {
                    photoImage.setVisibility(View.VISIBLE);
                    Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                    photoImage.setImageBitmap(bitmap);
                    //将图片解析成Bitmap对象，并把它显现出来
                    File outputImage = new File(filePath);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
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

    private void initView(Bitmap srcbitmap) {

        int modelnumber = 2;

        startTime = System.currentTimeMillis();
        Thread ocrThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mutableBitmap = srcbitmap.copy(Bitmap.Config.ARGB_8888, true);
                    OCRResult[] ocrResult = ChineseOCRLite.detect(srcbitmap, 1080);
                    final StringBuilder allText = new StringBuilder();

                    if (ocrResult != null && ocrResult.length > 0) {
                        mutableBitmap = drawResult(mutableBitmap, ocrResult);
                        for (int i = 0; i < ocrResult.length; i++) {
                            allText.append(ocrResult[i].text).append("\r\n");
                        }
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            endTime = System.currentTimeMillis();
                            Log.i("long", String.valueOf(allText.length()));
                            thresholdTextview.setText(String.format(Locale.ENGLISH,
                                    "Text:\n%s", allText));
                            if (modelnumber == 1) {
                                try {
                                    layout = allText.substring(allText.indexOf("R"), allText.indexOf("R") + 5);

                                    Log.i("answer", layout);
                                    Log.i("answer", temp);
                                    if (temp.equals(layout)) {
                                        Log.i("answer", "ifok");
                                        new Thread() {
                                            public void run() {
                                                Message msg = new Message();
                                                msg.what = 1;
                                                handler.sendMessage(msg);

                                            }
                                        }.start();

                                    }

                                } catch (Exception e) {
                                    resultText.setText("识别失败，请手动输入");
                                    Toast.makeText(getBaseContext(), "识别失败", Toast.LENGTH_SHORT).show();
                                }
                            } else if ((modelnumber == 2)) {
                                try {
                                    layout = allText.substring(allText.indexOf("R"), allText.indexOf("R") + 5);
                                    resultText.setText(layout);
                                } catch (Exception e) {
                                    resultText.setText("识别失败，请手动输入");
                                    Toast.makeText(getBaseContext(), "识别失败", Toast.LENGTH_SHORT).show();
                                }

                            }

                        }
                    });
                } catch (Exception e) {

                }

            }
        });

        ocrThread.start();

    }

    protected double getBoxAngle(OCRResult ocrResult, boolean toDegrees) {
        double angle = 0.0f;
        if (ocrResult == null) {
            return angle;
        }
        // 0 1  2 3  4 5  6 7
        // x0y0 x1y1 x2y2 x3y3
        double dx1 = ocrResult.boxes[2] - ocrResult.boxes[0];
        double dy1 = ocrResult.boxes[3] - ocrResult.boxes[1];
        double dis1 = dy1 * dy1 + dx1 * dx1;
        double dx2 = ocrResult.boxes[4] - ocrResult.boxes[2];
        double dy2 = ocrResult.boxes[5] - ocrResult.boxes[3];
        double dis2 = dy2 * dy2 + dx2 * dx2;
        if (dis1 > dis2) {
            if (dx1 != 0) {
                angle = Math.asin(dy1 / dx1);
            }
        } else {
            if (dx2 != 0) {
                angle = Math.asin(dx2 / dy2);
            }
        }
        if (toDegrees) {
            angle = Math.toDegrees(angle);
            if (dis2 > dis1) {
                angle = angle + 90;
            }
//            Log.d("wzt", "degrees:" + angle + " dx:" + dx1 + " dy:" + dy1);
            return angle;
        }
//        Log.d("wzt", "angle:" + angle + " dx:" + dx1 + " dy:" + dy1);
        return angle;
    }

    protected Bitmap drawResult(Bitmap mutableBitmap, OCRResult[] results) {
        if (results == null || results.length <= 0) {
            return mutableBitmap;
        }
        Canvas canvas = new Canvas(mutableBitmap);
        final Paint boxPaint = new Paint();
        boxPaint.setAlpha(200);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(2 * Math.min(mutableBitmap.getWidth(), mutableBitmap.getHeight()) / 800.0f);
        boxPaint.setTextSize(15 * Math.min(mutableBitmap.getWidth(), mutableBitmap.getHeight()) / 800.0f);
        boxPaint.setColor(Color.BLUE);
        boxPaint.setAntiAlias(true);
        for (OCRResult result : results) {
            boxPaint.setColor(Color.RED);
            boxPaint.setStyle(Paint.Style.FILL);
            // 框
            canvas.drawLine((float) result.boxes[0], (float) result.boxes[1], (float) result.boxes[2], (float) result.boxes[3], boxPaint);
            canvas.drawLine((float) result.boxes[2], (float) result.boxes[3], (float) result.boxes[4], (float) result.boxes[5], boxPaint);
            canvas.drawLine((float) result.boxes[4], (float) result.boxes[5], (float) result.boxes[6], (float) result.boxes[7], boxPaint);
            canvas.drawLine((float) result.boxes[6], (float) result.boxes[7], (float) result.boxes[0], (float) result.boxes[1], boxPaint);
            // 文字
            if (true) {  // 防止太乱
                double angle = getBoxAngle(result, true);
                canvas.save();
                canvas.rotate((float) angle, (float) result.boxes[0], (float) result.boxes[1] - 5);
                boxPaint.setColor(Color.BLUE);  // 防止有角度的框与之重叠
                if (angle > 70) {
                    canvas.drawText(String.format(Locale.CHINESE, "%s  (%.3f)", result.text, result.boxScore[0]),
                            (float) result.boxes[0] + 5, (float) result.boxes[1] + 15, boxPaint);
                } else {
                    canvas.drawText(String.format(Locale.CHINESE, "%s  (%.3f)", result.text, result.boxScore[0]),
                            (float) result.boxes[0], (float) result.boxes[1] - 5, boxPaint);
                }
                canvas.restore();
            }
            // 提示
            boxPaint.setColor(Color.YELLOW);  // 左上角画个红点
            canvas.drawPoint((float) result.boxes[0], (float) result.boxes[1], boxPaint);
            boxPaint.setColor(Color.GREEN);  // 右下角画个绿点
            canvas.drawPoint((float) result.boxes[4], (float) result.boxes[5], boxPaint);
        }
        return mutableBitmap;
    }
}
