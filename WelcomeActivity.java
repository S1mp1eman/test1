package com.wzt.yolov5;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.wzt.yolov5.ocr.OcrActivity;

public class WelcomeActivity extends AppCompatActivity {

    private ToggleButton tbUseGpu;
    private Button chineseocrlite; //下药
    private Button nanoDet;   //节点
    private Button well;      //钻井
    private Button zwell;     //排列
    private int add=1;

    public static String temp="mouse";
    private boolean useGPU = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        MainActivity.USE_GPU = useGPU;
        OcrActivity.USE_GPU = useGPU;

        nanoDet = findViewById(R.id.btn_start_detect13);
        nanoDet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.USE_MODEL = MainActivity.NANODET;
                Intent intent = new Intent(WelcomeActivity.this, Address.class);
                WelcomeActivity.this.startActivity(intent);
            }
        });

        well = findViewById(R.id.btn_start_well);
        well.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.USE_MODEL=MainActivity.NANODET;
                Intent intent = new Intent(WelcomeActivity.this, WellAddress.class);
                WelcomeActivity.this.startActivity(intent);
            }
        });

        zwell = findViewById(R.id.btn_start_zwell);
        zwell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.USE_MODEL=MainActivity.NANODET;
                Intent intent = new Intent(WelcomeActivity.this, AddressZWell.class);
                WelcomeActivity.this.startActivity(intent);
            }
        });




    }
}
