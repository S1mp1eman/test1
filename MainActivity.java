package com.wzt.yolov5;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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
import androidx.lifecycle.LifecycleOwner;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.widget.CompoundButton;
import android.widget.ImageView;

import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Switch;
import android.widget.TextView;


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
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.chrisbanes.photoview.PhotoView;
import com.wzt.yolov5.ocr.ChineseOCRLite;
import com.wzt.yolov5.ocr.OCRResult;
import com.wzt.yolov5.ocr.OcrActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import wseemann.media.FFmpegMediaMetadataRetriever;



public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("yolov5");
    }
    public native int[] gray(int[] buf, int w, int h);
    public native String stringFromJNI();
    public native int getLighting(int[] pix,int w,int h);

    public static String temp = "surveyLine";
    public String temp2 = "concentrator";
    public static int MOBILENETV2_YOLOV3_NANO = 3;
    public static int NANODET = 12;
    public static int USE_MODEL = MOBILENETV2_YOLOV3_NANO;
    public static boolean USE_GPU = false;
    public static CameraX.LensFacing CAMERA_ID = CameraX.LensFacing.BACK;
    public String layout = "";


    public CheckBox key1;
    public CheckBox key2;


    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_PICK_IMAGE = 2;
    private static final int REQUEST_PICK_VIDEO = 3;


    private static String[] PERMISSIONS_CAMERA = {
            Manifest.permission.CAMERA
    };


    private Toolbar toolbar;
    private ImageView resultImageView;
    private TextView tvNMS;
    private Button button1;
    private TextView tvThreshold;
    private SeekBar nmsSeekBar;
    private SeekBar thresholdSeekBar;
    private TextView tvNMNThreshold;
    private TextView tvInfo;
    private ImageView imageView2;
    private Button btnPhoto;
    private Button btnVideo;
    private double threshold = 0.4, nms_threshold = 0.7;
    private TextureView viewFinder;
    private SeekBar sbVideo;
    private SeekBar sbVideoSpeed;


    private int add = 1;
    public boolean globalSwitch = false;

    protected float videoSpeed = 1.0f;
    protected long videoCurFrameLoc = 0;
    public static int VIDEO_SPEED_MAX = 20 + 1;
    public static int VIDEO_SPEED_MIN = 1;
    private EditText resultText;

    private AtomicBoolean detectCamera = new AtomicBoolean(false);
    private AtomicBoolean detectPhoto = new AtomicBoolean(false);
    private AtomicBoolean detectVideo = new AtomicBoolean(false);

    private final String filePath = Environment.getExternalStorageDirectory() + File.separator + "output_image.jpg";

    private long startTime = 0;
    private long endTime = 0;
    private int width;
    private int height;
    public static final int TAKE_PHOTO = 1001;
    private Uri imageUri;
    double total_fps = 0;
    int fps_count = 0;

    protected Bitmap mutableBitmap;

    private Button submit;
    ExecutorService detectService = Executors.newSingleThreadExecutor();

    FFmpegMediaMetadataRetriever mmr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_CAMERA,
                    REQUEST_CAMERA
            );
            finish();
        }

        submit = findViewById(R.id.submit_2);
        submit.setOnClickListener(view ->{
            String allText = resultText.getText().toString();
            if(allText.contains("R")){
                layout=allText.substring(allText.indexOf("R"),allText.indexOf("R")+5);
                new AlertDialog.Builder(MainActivity.this)
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
        resultText = findViewById(R.id.result_text);
        Switch aSwitch = findViewById(R.id.switch1);

        aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Toast.makeText(getBaseContext(), "检测开始!", Toast.LENGTH_SHORT).show();
                    globalSwitch = true;
                } else {
                    Toast.makeText(getBaseContext(), "检测中断!", Toast.LENGTH_SHORT).show();
                    key1.setChecked(false);
                    key2.setChecked(false);
                    globalSwitch = false;
                }
            }
        });

        imageView2 = findViewById(R.id.imageView2);

        Button reset = findViewById(R.id.reset);

        reset.setOnClickListener(view -> {
            aSwitch.setChecked(false);
            key1.setChecked(false);
            key2.setChecked(false);
        });

        Button back = findViewById(R.id.back);
        back.setOnClickListener(view -> {
            finish();
        });


        add = getIntent().getIntExtra("add", 1);
        switch(add){
            case 1:
            {
                TextView text21 = findViewById(R.id.textView4);
                //设置text01的文本值，此处会覆盖掉activity_main.xml的文本值
                text21.setText("任务桩号:R1001");
                TextView text211 = findViewById(R.id.textView10);
                text211.setText("定位桩号:R1001");
//                TextView text2 = findViewById(R.id.textView3);
//                //设置text01的文本值，此处会覆盖掉activity_main.xml的文本值
//                text2.setText("已完成");
                break;
            }
            case 2:
            {
                TextView text22 = findViewById(R.id.textView4);
                //设置text01的文本值，此处会覆盖掉activity_main.xml的文本值
                text22.setText("任务桩号:R1002");
                TextView text212 = findViewById(R.id.textView10);
                text212.setText("定位桩号:R1002");
                break;
            }
            case 3:
            {
                TextView text23 = findViewById(R.id.textView4);
                //设置text01的文本值，此处会覆盖掉activity_main.xml的文本值
                text23.setText("任务桩号:R1003");
                TextView text213 = findViewById(R.id.textView10);
                text213.setText("定位桩号:R1003");
                break;
            }
            case 4:
            {
                TextView text24 = findViewById(R.id.textView4);
                //设置text01的文本值，此处会覆盖掉activity_main.xml的文本值
                text24.setText("任务桩号:R1004");
                TextView text214 = findViewById(R.id.textView10);
                text214.setText("定位桩号:R1004");
                break;
            }
            case 5:
            {
                TextView text25 = findViewById(R.id.textView4);
                //设置text01的文本值，此处会覆盖掉activity_main.xml的文本值
                text25.setText("任务桩号:R1005");
                TextView text215 = findViewById(R.id.textView10);
                text215.setText("定位桩号:R1005");
                break;
            }
        }

        key1=findViewById(R.id.chb_1);
        //key1.setClickable(false);
        key2=findViewById(R.id.chb_2);
        //key2.setClickable(false);

        Button photo = findViewById(R.id.camera_bt);
        photo.setOnClickListener(view->{
            requestPermission();
        });
        button1 = findViewById(R.id.btn_getocr);
        initModel();
        initViewID();
        initViewListener();
        viewFinder.setVisibility(View.GONE);
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
            Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(intent, TAKE_PHOTO);
            //调用会返回结果的开启方式，返回成功的话，则把它显示出来
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    protected void initViewListener() {
        toolbar.setNavigationIcon(R.drawable.actionbar_dark_back_icon);
        toolbar.setNavigationOnClickListener(v -> finish());

        if (USE_MODEL != NANODET) {
            nmsSeekBar.setEnabled(false);
            thresholdSeekBar.setEnabled(false);
            tvNMS.setVisibility(View.GONE);
            tvThreshold.setVisibility(View.GONE);
            nmsSeekBar.setVisibility(View.GONE);
            thresholdSeekBar.setVisibility(View.GONE);
            tvNMNThreshold.setVisibility(View.GONE);
        } else if (USE_MODEL == NANODET) {
            threshold = 0.4f;
            nms_threshold = 0.6f;
        }

        nmsSeekBar.setProgress((int) (nms_threshold * 100));
        thresholdSeekBar.setProgress((int) (threshold * 100));
        final String format = "THR: %.2f, NMS: %.2f";
        tvNMNThreshold.setText(String.format(Locale.ENGLISH, format, threshold, nms_threshold));
        nmsSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                nms_threshold = i / 100.f;
                tvNMNThreshold.setText(String.format(Locale.ENGLISH, format, threshold, nms_threshold));
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
                tvNMNThreshold.setText(String.format(Locale.ENGLISH, format, threshold, nms_threshold));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        btnPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int permission = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                            MainActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            777
                    );
                } else {
                    Intent intent = new Intent(Intent.ACTION_PICK);
                    intent.setType("image/*");
                    startActivityForResult(intent, REQUEST_PICK_IMAGE);
                }
            }
        });


        resultImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (detectVideo.get() || detectPhoto.get()) {
                    detectPhoto.set(false);
                    detectVideo.set(false);
                    sbVideo.setVisibility(View.GONE);
                    sbVideoSpeed.setVisibility(View.GONE);
                    startCamera();
                }
            }
        });

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

        sbVideoSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                videoSpeed = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Toast.makeText(MainActivity.this, "Video Speed:" + seekBar.getProgress(), Toast.LENGTH_SHORT).show();
            }
        });

        sbVideo.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                videoCurFrameLoc = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                videoCurFrameLoc = seekBar.getProgress();
            }
        });
    }

    protected void initViewID() {

        key1=findViewById(R.id.chb_1);
        key2=findViewById(R.id.chb_2);
        toolbar = findViewById(R.id.tool_bar);
        resultImageView = findViewById(R.id.imageView);
        tvNMNThreshold = findViewById(R.id.valTxtView);
        tvInfo = findViewById(R.id.tv_info);
        tvNMS = findViewById(R.id.txtNMS);
        tvThreshold = findViewById(R.id.txtThresh);
        nmsSeekBar = findViewById(R.id.nms_seek);
        thresholdSeekBar = findViewById(R.id.threshold_seek);
        btnPhoto = findViewById(R.id.button);
        //btnVideo = findViewById(R.id.btn_video);
        viewFinder = findViewById(R.id.view_finder);
        sbVideo = findViewById(R.id.sb_video);
        sbVideo.setVisibility(View.GONE);
        sbVideoSpeed = findViewById(R.id.sb_video_speed);
        sbVideoSpeed.setMin(VIDEO_SPEED_MIN);
        sbVideoSpeed.setMax(VIDEO_SPEED_MAX);
        sbVideoSpeed.setVisibility(View.GONE);
    }

    protected void initModel() {
        if (USE_MODEL == NANODET) {
            NanoDet.init(getAssets(), USE_GPU);
        }


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
//                .setTargetAspectRatio(Rational.NEGATIVE_INFINITY)  // 宽高比
                .setTargetResolution(new Size(480, 640))  // 分辨率
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

    private void requestPermission() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //请求权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, 1);
        } else {
            //调用
            requestCamera();
        }
    }

    private UseCase gainAnalyzer(DetectAnalyzer detectAnalyzer) {
        ImageAnalysisConfig.Builder analysisConfigBuilder = new ImageAnalysisConfig.Builder();
        analysisConfigBuilder.setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE);
        analysisConfigBuilder.setTargetResolution(new Size(480, 640));  // 输出预览图像尺寸
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
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        if (detectCamera.get() || detectPhoto.get() || detectVideo.get()) {
            return;
        }
        detectCamera.set(true);
        startTime = System.currentTimeMillis();
        final Bitmap bitmapsrc = imageToBitmap(image);  // 格式转换
        if (detectService == null) {
            detectCamera.set(false);
            return;
        }
        detectService.execute(new Runnable() {
            @Override
            public void run() {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotationDegrees);
                width = bitmapsrc.getWidth();
                height = bitmapsrc.getHeight();
                Bitmap bitmap = Bitmap.createBitmap(bitmapsrc, 0, 0, width, height, matrix, false);

                detectAndDraw(bitmap);
                showResultOnUI();
            }
        });
    }

    protected void showResultOnUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                detectCamera.set(false);
                resultImageView.setImageBitmap(mutableBitmap);
                endTime = System.currentTimeMillis();
                long dur = endTime - startTime;
                float fps = (float) (1000.0 / dur);
                total_fps = (total_fps == 0) ? fps : (total_fps + fps);
                fps_count++;
                String modelName = getModelName();

//                tvInfo.setText(String.format(Locale.CHINESE,
//                        "%s\nSize: %dx%d\nTime: %.3f s\nFPS: %.3f\nAVG_FPS: %.3f",
//                        modelName, height, width, dur / 1000.0, fps, (float) total_fps / fps_count));
            }
        });
    }


    protected Bitmap drawENetMask(Bitmap mutableBitmap, float[] results) {
        if (results == null || results.length <= 0) {
            return mutableBitmap;
        }
        int[][] cityspace_colormap = {
                {128, 64, 128}, {244, 35, 232}, {70, 70, 70}, {102, 102, 156}, {190, 153, 153}, {153, 153, 153},
                {250, 170, 30}, {220, 220, 0}, {107, 142, 35}, {152, 251, 152}, {70, 130, 180}, {220, 20, 60},
                {255, 0, 0}, {0, 0, 142}, {0, 0, 70}, {0, 60, 100}, {0, 80, 100}, {0, 0, 230}, {119, 11, 32}
        };
        Canvas canvas = new Canvas(mutableBitmap);
        final Paint maskPaint = new Paint();
        maskPaint.setStyle(Paint.Style.STROKE);
        maskPaint.setStrokeWidth(4 * mutableBitmap.getWidth() / 800.0f);
        maskPaint.setTextSize(30 * mutableBitmap.getWidth() / 800.0f);
        float mask = 0;
        int color = 0;
        float tempC = 0;
        int lengthW = 1;
        for (int y = 0; y < mutableBitmap.getHeight(); y++) {
            for (int x = 0; x < mutableBitmap.getWidth(); x++) {
                mask = results[y * mutableBitmap.getWidth() + x];
                if (mask >= cityspace_colormap.length) {
                    continue;
                }
                if (mask != tempC) {
                    color = Color.argb(255,
                            cityspace_colormap[(int) tempC][0],
                            cityspace_colormap[(int) tempC][1],
                            cityspace_colormap[(int) tempC][2]);
                    maskPaint.setColor(color);
                    maskPaint.setAlpha(100);
                    canvas.drawLine(x - lengthW, y, x, y, maskPaint);
                    tempC = mask;
                    lengthW = 1;
                } else {
                    lengthW++;
                }
            }
            color = Color.argb(255,
                    cityspace_colormap[(int) tempC][0],
                    cityspace_colormap[(int) tempC][1],
                    cityspace_colormap[(int) tempC][2]);
            maskPaint.setColor(color);
            maskPaint.setAlpha(100);
            canvas.drawLine(mutableBitmap.getWidth() - lengthW, y, mutableBitmap.getWidth(), y, maskPaint);
            tempC = mask;
            lengthW = 1;
        }
        return mutableBitmap;
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
                CheckBox cb1=findViewById(R.id.chb_2);
                cb1.setChecked(true);
            }
        }
    };

    Handler handler3 = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                CheckBox cb1=findViewById(R.id.chb_2);
                cb1.setChecked(false);
            }
        }
    };

    Handler handler4 = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                button1.setEnabled(true);
            }
        }
    };
    Handler handler5 = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                button1.setEnabled(false);
            }
        }
    };
    protected Bitmap drawBoxRects(Bitmap mutableBitmap, Box[] results) {
        int flag=0;
        int countflag=0;
        int flagsensor=0;
        if (results == null || results.length <= 0) {
            return mutableBitmap;
        }

        Canvas canvas = new Canvas(mutableBitmap);
        final Paint boxPaint = new Paint();
        boxPaint.setAlpha(200);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(4 * mutableBitmap.getWidth() / 800.0f);
        boxPaint.setTextSize(30 * mutableBitmap.getWidth() / 800.0f);
        for (int i = 0; i < results.length; i++) {
            if (USE_MODEL == MOBILENETV2_YOLOV3_NANO) {
                if (results[i].getScore() < 0.15f) {
                    continue;
                }
                results[i].x0 = results[i].x0 < 0 ? results[i].x0 / 9 : results[i].x0;
                results[i].y0 = results[i].y0 < 0 ? results[i].y0 / 9 : results[i].y0;
            }
            boxPaint.setColor(results[i].getColor());
            boxPaint.setStyle(Paint.Style.FILL);
            //canvas.drawText(results[i].getLabel() , results[i].x0 + 3, results[i].y0 + 30 * mutableBitmap.getWidth() / 1000.0f, boxPaint);
            boxPaint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(results[i].getRect(), boxPaint);

            if (temp.equals(results[i].getLabel())) {
                flag = 1;
            }
            if ("sensor".equals(results[i].getLabel())) {
                flagsensor = 1;
            }
        }

        if (globalSwitch){
            if(flag==0){
//            new Thread() {
//                public void run() {
//                    Message msg = new Message();
//                    msg.what = 1;
//                    handler3.sendMessage(msg);
//                }
//            }.start();
            }
            else if(flag==1){
                new Thread() {
                    public void run() {
                        Message msg = new Message();
                        msg.what = 1;
                        handler2.sendMessage(msg);
                    }
                }.start();
            }

            if (flagsensor == 1) {
                Bitmap finalbitmap = null;
                for (int i = 0; i < results.length; i++) {
                    int needwidth;
                    int needheight;
                    if ("sensor".equals(results[i].getLabel())) {
                        needwidth = (int) (results[i].x1 - results[i].x0);
                        needheight = (int) (results[i].y1 - results[i].y0);
                        Bitmap resultbitmap = null;
                        resultbitmap = Bitmap.createBitmap(mutableBitmap, (int) results[i].x0, (int) results[i].y0, needwidth, needheight);
                        finalbitmap =Bitmap.createBitmap(resultbitmap);
                        break;
                    }
                }
                countflag=getGray(finalbitmap);
                if(countflag>=1){
                    new Thread() {
                        public void run() {
                            Message msg = new Message();
                            msg.what = 1;
                            handler.sendMessage(msg);
                        }
                    }.start();
                }
                else if(countflag<1){
//                new Thread() {
//                    public void run() {
//                        Message msg = new Message();
//                        msg.what = 1;
//                        handler1.sendMessage(msg);
//                    }
//                }.start();
                }
            }else if(flagsensor==0)
            {
//            new Thread() {
//                public void run() {
//                    Message msg = new Message();
//                    msg.what = 1;
//                    handler1.sendMessage(msg);
//                }
//            }.start();
            }

            if(key1.isChecked()&&key2.isChecked()){
                new Thread() {
                    public void run() {
                        Message msg = new Message();
                        msg.what = 1;
                        handler4.sendMessage(msg);
                        msg.obj = button1;
                    }
                }.start();
            }

            if(!(key1.isChecked()&&key2.isChecked())){
                new Thread() {
                    public void run() {
                        Message msg = new Message();
                        msg.what = 1;
                        handler5.sendMessage(msg);
                        msg.obj = button1;
                    }
                }.start();
            }

            button1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Bitmap finalbitmap = null;
                    resultText.setText("正在识别......");
                    for (int i = 0; i < results.length; i++) {
                        int needwidth;
                        int needheight;
                        if (temp.equals(results[i].getLabel())) {
                            needwidth = (int) (results[i].x1 - results[i].x0);
                            needheight = (int) (results[i].y1 - results[i].y0);
                            Bitmap resultbitmap = null;
                            resultbitmap = Bitmap.createBitmap(mutableBitmap, (int) results[i].x0, (int) results[i].y0, needwidth, needheight);
                            if(resultbitmap == null){
                                Toast.makeText(getBaseContext(), "noResult", Toast.LENGTH_SHORT).show();
                            }
                            finalbitmap = Bitmap.createBitmap(resultbitmap);
                            ChineseOCRLite.init(getAssets(), USE_GPU);
                            if(finalbitmap == null){
                                Toast.makeText(getBaseContext(), "noResult", Toast.LENGTH_SHORT).show();
                            }
                            break;
                        }

                    }

                    initView(finalbitmap);
                }
            });
        }


        return mutableBitmap;
    }

    private int getGray(Bitmap finalbitmap) {
        int bl;
        Bitmap bitmap = Bitmap.createBitmap(finalbitmap);
        int w = bitmap.getWidth(), h = bitmap.getHeight();
        int[] pix = new int[w * h];
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);

        bl=getLighting(pix,w,h);

        return bl;


    }


    protected Bitmap detectAndDraw(Bitmap image) {
        Box[] result = null;
        KeyPoint[] keyPoints = null;
        float[] enetMasks = null;
        if (USE_MODEL == NANODET) {
            result = NanoDet.detect(image, threshold, nms_threshold);
        }
        if (result == null && keyPoints == null && enetMasks == null
        ) {
            detectCamera.set(false);
            return image;
        }
        if (USE_MODEL == MOBILENETV2_YOLOV3_NANO
                || USE_MODEL == NANODET) {
            mutableBitmap = drawBoxRects(image, result);
        }
        return mutableBitmap;
    }

    protected String getModelName() {
        String modelName = "ohhhhh";
        if (USE_MODEL == MOBILENETV2_YOLOV3_NANO) {
            modelName = "MobileNetV2-YOLOv3-Nano";
        } else if (USE_MODEL == NANODET) {
            modelName = "NanoDet";
        }
        return USE_GPU ? "[ GPU ] " + modelName : "[ CPU ] " + modelName;
    }

    @Override
    protected void onDestroy() {
        detectCamera.set(false);
        detectVideo.set(false);
        if (detectService != null) {
            detectService.shutdown();
            detectService = null;
        }
        if (mmr != null) {
            mmr.release();
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
            if (requestCode == 1) {
                requestCamera();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) {
            return;
        }
        if (requestCode == REQUEST_PICK_IMAGE) {
            // photo
            runByPhoto(requestCode, resultCode, data);
        } else if (requestCode == REQUEST_PICK_VIDEO) {
            // video
            runByVideo(requestCode, resultCode, data);
        } else {
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
        }

        if (requestCode == TAKE_PHOTO) {
            if (resultCode == RESULT_OK) {
                try {
                    Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                    resultImageView.setImageBitmap(bitmap);
                    //将图片解析成Bitmap对象，并把它显现出来
                    File outputImage = new File(filePath);
                    submit.setOnClickListener(view -> {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle("确认窗口");
                        builder.setMessage("确认提交这张照片吗"); //设置内容
                        builder.setIcon(R.mipmap.ic_launcher);//设置图标
                        builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                //TODO
                                finish();
                            }
                        });
                        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        //显示dialog
                        builder.create().show();
                    });
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public void runByPhoto(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode != RESULT_OK || data == null) {
            Toast.makeText(this, "Photo error", Toast.LENGTH_SHORT).show();
            return;
        }
        if (detectVideo.get()) {
            Toast.makeText(this, "Video is running", Toast.LENGTH_SHORT).show();
            return;
        }
        detectPhoto.set(true);
        final Bitmap image = getPicture(data.getData());
        if (image == null) {
            Toast.makeText(this, "Photo is null", Toast.LENGTH_SHORT).show();
            return;
        }
        CameraX.unbindAll();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                long start = System.currentTimeMillis();
                mutableBitmap = image.copy(Bitmap.Config.ARGB_8888, true);
                width = image.getWidth();
                height = image.getHeight();

                mutableBitmap = detectAndDraw(mutableBitmap);

                final long dur = System.currentTimeMillis() - start;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String modelName = getModelName();
                        resultImageView.setImageBitmap(mutableBitmap);
//                        tvInfo.setText(String.format(Locale.CHINESE, "%s\nSize: %dx%d\nTime: %.3f s\nFPS: %.3f",
//                                modelName, height, width, dur / 1000.0, 1000.0f / dur));
                    }
                });
            }
        }, "photo detect");
        thread.start();
    }

    public void runByVideo(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode != RESULT_OK || data == null) {
            Toast.makeText(this, "Video error", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Uri uri = data.getData();
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
                String v_path = cursor.getString(1); // 文件路径
                String v_size = cursor.getString(2); // 大小
                String v_name = cursor.getString(3); // 文件名
                detectOnVideo(v_path);
            } else {
                Toast.makeText(this, "Video is null", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Video is null", Toast.LENGTH_SHORT).show();
        }
    }

    public void detectOnVideo(final String path) {
        if (detectVideo.get()) {
            Toast.makeText(this, "Video is running", Toast.LENGTH_SHORT).show();
            return;
        }
        detectVideo.set(true);
        Toast.makeText(MainActivity.this, "FPS is not accurate!", Toast.LENGTH_SHORT).show();
        sbVideo.setVisibility(View.VISIBLE);
        sbVideoSpeed.setVisibility(View.VISIBLE);
        CameraX.unbindAll();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                mmr = new FFmpegMediaMetadataRetriever();
                mmr.setDataSource(path);
                String dur = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION);  // ms
                String sfps = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_FRAMERATE);  // fps
                String rota = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);  // rotation
                int duration = Integer.parseInt(dur);
                float fps = Float.parseFloat(sfps);
                float rotate = 0;
                if (rota != null) {
                    rotate = Float.parseFloat(rota);
                }
                sbVideo.setMax(duration * 1000);
                float frameDis = 1.0f / fps * 1000 * 1000 * videoSpeed;
                videoCurFrameLoc = 0;
                while (detectVideo.get() && (videoCurFrameLoc) < (duration * 1000)) {
                    videoCurFrameLoc = (long) (videoCurFrameLoc + frameDis);
                    sbVideo.setProgress((int) videoCurFrameLoc);
                    final Bitmap b = mmr.getFrameAtTime(videoCurFrameLoc, FFmpegMediaMetadataRetriever.OPTION_CLOSEST);
                    if (b == null) {
                        continue;
                    }
                    Matrix matrix = new Matrix();
                    matrix.postRotate(rotate);
                    width = b.getWidth();
                    height = b.getHeight();
                    final Bitmap bitmap = Bitmap.createBitmap(b, 0, 0, width, height, matrix, false);
                    startTime = System.currentTimeMillis();
                    detectAndDraw(bitmap.copy(Bitmap.Config.ARGB_8888, true));
                    showResultOnUI();
                    frameDis = 1.0f / fps * 1000 * 1000 * videoSpeed;
                }
                mmr.release();
                if (detectVideo.get()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            sbVideo.setVisibility(View.GONE);
                            sbVideoSpeed.setVisibility(View.GONE);
                            Toast.makeText(MainActivity.this, "Video end!", Toast.LENGTH_LONG).show();
                        }
                    });
                }
                detectVideo.set(false);
            }
        }, "video detect");
        thread.start();
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

        int modelnumber = 1;

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
                            if (modelnumber == 1) {
                                try {
                                    layout = allText.substring(allText.indexOf("R"), allText.indexOf("R") + 5);
                                    resultText.setText(layout);
                                } catch (Exception e) {
                                    resultText.setText("识别失败，请手动输入");
                                    Toast.makeText(getBaseContext(), "识别失败", Toast.LENGTH_SHORT).show();
                                }
                            } else if ((modelnumber == 2)) {
                                try {
                                    layout = allText.substring(allText.indexOf("R"), allText.indexOf("R") + 5);
                                    if (temp2.equals(layout)) {
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