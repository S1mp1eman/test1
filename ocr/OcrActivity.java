package com.wzt.yolov5.ocr;

import static android.os.SystemClock.sleep;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.DialogPreference;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.wzt.yolov5.DataHolder;
import com.wzt.yolov5.MainActivity;
import com.wzt.yolov5.R;
import com.wzt.yolov5.WelcomeActivity;

import java.io.IOException;
import java.util.Locale;

public class OcrActivity extends AppCompatActivity {

    private static final int REQUEST_PICK_IMAGE = 2;
    private Bitmap srcbitmap;
    public static String temp="R1001";
    public static String temp2="R2535";
    protected Button btnPhoto1;
    private int add=1;
    private int msg;
    protected Button btnPhoto;
    protected ImageView imageSrc;
    //    protected ImageView imageResult;
    protected PhotoView imageResult;
    protected EditText etResult;
    private ImageView imageView2;
    public String layout;
    protected Switch swShowText;

    private ProgressBar progressBar;

    public String[] address={"R1001","R1002","R1003","R1004","R1005"};
    public String[] address2={"R2535","R2536","R2537","R2538","R2539"};

    protected TextView textView;
    protected boolean showText;
    protected Bitmap mutableBitmap;
    private Button submit;

    public static boolean USE_GPU = false;

    long startTime = 0;
    long endTime = 0;

    int modelnumber=0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr);
        textView = findViewById(R.id.textView4);
        textView.setVisibility(View.INVISIBLE);
        etResult = findViewById(R.id.et_info);

        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    1
            );
            finish();
        }

        submit = findViewById(R.id.submit);

        msg=getIntent().getIntExtra("msg",0);
        srcbitmap= (Bitmap) DataHolder.getData("bitmap");
        add=getIntent().getIntExtra("add",1);
        progressBar = findViewById(R.id.progressBar);
        modelnumber=getIntent().getIntExtra("modelnumber",1);

        temp=address[add-1];
        temp2=address2[add-1];
        btnPhoto1=findViewById(R.id.btn_photo0);
        btnPhoto1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(OcrActivity.this, WelcomeActivity.class);
                OcrActivity.this.startActivity(intent);
            }
        });
        imageView2 = findViewById(R.id.imageView2);

        initView();

        submit.setOnClickListener(view -> {
            sleep(500);
            String allText = etResult.getText().toString();
            if(allText.contains("R")){
                layout=allText.substring(allText.indexOf("R"),allText.indexOf("R")+5);
                new AlertDialog.Builder(OcrActivity.this)
                        .setTitle("????????????"+ layout)
                        .setMessage("????????????????????????")
                        .setPositiveButton("???", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new Thread(){
                                    public void run(){
                                        Message msg = new Message();
                                        msg.what = 2;
                                        handler.sendMessage(msg);
                                        Log.i("gggggg", String.valueOf(msg.what));
                                    }
                                }.start();
                            }
                        })
                        .setNegativeButton("???", null)
                        .show();
            }else{
                Toast.makeText(getBaseContext(), "??????????????????????????????R1001", Toast.LENGTH_SHORT).show();
            }

        });

        if (msg==77&&srcbitmap!=null){
            Bitmap imagetemp=null;
            imagetemp=Bitmap.createBitmap(srcbitmap);
            final Bitmap image=Bitmap.createBitmap(imagetemp);
            if (image == null) {
                Toast.makeText(this, "Photo is null", Toast.LENGTH_SHORT).show();
                return;
            }
            imageResult.setImageResource(R.drawable.ic_launcher_foreground);
            imageSrc.setImageBitmap(image);
            etResult.setText("Please wait...");
        }
        ChineseOCRLite.init(getAssets(), USE_GPU);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) {
            return;
        }
        Bitmap imagetemp=null;

        imagetemp = getPicture(data.getData());

        final Bitmap image=Bitmap.createBitmap(imagetemp);
        if (image == null) {
            Toast.makeText(this, "Photo is null", Toast.LENGTH_SHORT).show();
            return;
        }
        imageResult.setImageResource(R.drawable.ic_launcher_foreground);
        imageSrc.setImageBitmap(image);
        etResult.setText("??????????????????");
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
            // ???
            canvas.drawLine((float) result.boxes[0], (float) result.boxes[1], (float) result.boxes[2], (float) result.boxes[3], boxPaint);
            canvas.drawLine((float) result.boxes[2], (float) result.boxes[3], (float) result.boxes[4], (float) result.boxes[5], boxPaint);
            canvas.drawLine((float) result.boxes[4], (float) result.boxes[5], (float) result.boxes[6], (float) result.boxes[7], boxPaint);
            canvas.drawLine((float) result.boxes[6], (float) result.boxes[7], (float) result.boxes[0], (float) result.boxes[1], boxPaint);
            // ??????
            if (showText) {  // ????????????
                double angle = getBoxAngle(result, true);
                canvas.save();
                canvas.rotate((float) angle, (float) result.boxes[0], (float) result.boxes[1] - 5);
                boxPaint.setColor(Color.BLUE);  // ?????????????????????????????????
                if (angle > 70) {
                    canvas.drawText(String.format(Locale.CHINESE, "%s  (%.3f)", result.text, result.boxScore[0]),
                            (float) result.boxes[0] + 5, (float) result.boxes[1] + 15, boxPaint);
                } else {
                    canvas.drawText(String.format(Locale.CHINESE, "%s  (%.3f)", result.text, result.boxScore[0]),
                            (float) result.boxes[0], (float) result.boxes[1] - 5, boxPaint);
                }
                canvas.restore();
            }
            // ??????
            boxPaint.setColor(Color.YELLOW);  // ?????????????????????
            canvas.drawPoint((float) result.boxes[0], (float) result.boxes[1], boxPaint);
            boxPaint.setColor(Color.GREEN);  // ?????????????????????
            canvas.drawPoint((float) result.boxes[4], (float) result.boxes[5], boxPaint);
        }
        return mutableBitmap;
    }

    /**
     * ?????????????????????????????????
     *
     * @param ocrResult
     * @param toDegrees
     * @return
     */
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


    Handler handler = new Handler() {


        @Override
        public void handleMessage(Message msg) {
//            if (msg.what == 1) {
//                sleep(500);
//                new AlertDialog.Builder(OcrActivity.this)
//                        .setTitle("????????????"+layout)
//                        .setMessage("????????????????????????")
//                        .setPositiveButton("???", new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                new Thread(){
//                                    public void run(){
//                                        Message msg = new Message();
//                                        msg.what = 2;
//                                        handler.sendMessage(msg);
//                                        Log.i("gggggg", String.valueOf(msg.what));
//                                    }
//                                }.start();
//                            }
//                        })
//                        .setNegativeButton("???", null)
//                        .show();
//            }
            if(msg.what==2){
                sleep(500);
                new AlertDialog.Builder(OcrActivity.this)
                        .setTitle("????????????")
                        .setMessage("??????????????????")
                        .setPositiveButton("??????", null)
                        .show();

            }
        }
    };

    private void initView() {
        btnPhoto = findViewById(R.id.btn_photo);
        if (msg!=77)
        {
            btnPhoto.setEnabled(false);
        }
        else if(msg==77){
            textView.setVisibility(View.VISIBLE);
            btnPhoto.setEnabled(true);
            progressBar.setVisibility(View.INVISIBLE);
            btnPhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startTime = System.currentTimeMillis();
                    textView.setVisibility(View.INVISIBLE);
                    imageResult.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.INVISIBLE);
                    Thread ocrThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try{
                                mutableBitmap = srcbitmap.copy(Bitmap.Config.ARGB_8888, true);
                                OCRResult[] ocrResult = ChineseOCRLite.detect(srcbitmap, 1080);
                                final StringBuilder allText = new StringBuilder();

                                if (ocrResult != null && ocrResult.length > 0) {
                                    mutableBitmap = drawResult(mutableBitmap, ocrResult);
                                    for (int i=0;i<ocrResult.length;i++){
                                        allText.append(ocrResult[i].text).append("\r\n");
                                    }
                                }

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        endTime = System.currentTimeMillis();
                                        imageResult.setImageBitmap(mutableBitmap);
                                        Log.i("long", String.valueOf(allText.length()));
                                        etResult.setText(String.format(Locale.ENGLISH,
                                                "Text:\n%s", allText));
                                        if(modelnumber==1){
                                            try {
                                                layout=allText.substring(allText.indexOf("R"),allText.indexOf("R")+5);

                                                Log.i("answer",layout);
                                                Log.i("answer",temp);
                                                if(temp.equals(layout)){
                                                    Log.i("answer", "ifok");
                                                    new Thread(){
                                                        public void run(){
                                                            Message msg = new Message();
                                                            msg.what = 1;
                                                            handler.sendMessage(msg);

                                                        }
                                                    }.start();

                                                }

                                            } catch (Exception e) {
                                                etResult.setText("?????????????????????????????????????????????");
                                                Toast.makeText(getBaseContext(), "????????????", Toast.LENGTH_SHORT).show();
                                            }
                                        }

                                        else if ((modelnumber==2)){
                                            try {
                                                layout=allText.substring(allText.indexOf("R"),allText.indexOf("R")+5);
                                                Log.i("answer",layout);
                                                Log.i("answer",temp);
                                                if(temp2.equals(layout)) {
                                                    Log.i("answer", "ifok");
                                                    new Thread() {
                                                        public void run() {
                                                            Message msg = new Message();
                                                            msg.what = 1;
                                                            handler.sendMessage(msg);

                                                        }
                                                    }.start();
                                                }
                                            }catch(Exception e){
                                                etResult.setText("?????????????????????????????????????????????");
                                                Toast.makeText(getBaseContext(), "????????????", Toast.LENGTH_SHORT).show();
                                            }

                                        }

                                    }
                                });
                            }catch (Exception e){

                            }

                        }
                    });
                    imageView2.setVisibility(View.GONE);
                    ocrThread.start();
                }
            });

        }

        imageSrc = findViewById(R.id.image_src);
        imageResult = findViewById(R.id.image_result);
//        imageResult.enable();
//        imageResult.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
//        imageResult.setMaxScale(3.0f);
        swShowText = findViewById(R.id.sw_show_text);
        showText = swShowText.isChecked();
        swShowText.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                showText = isChecked;
                Toast.makeText(OcrActivity.this, showText ? "Show text" : "Hide text", Toast.LENGTH_SHORT).show();
            }
        });
    }
}