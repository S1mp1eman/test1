package com.wzt.yolov5;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;

//节点
public class WellAddress extends AppCompatActivity {

    private RadioGroup radgroup;
    private int add=1;
    private Button bt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address);

        System.out.println("123456");
        radgroup = (RadioGroup) findViewById(R.id.radioGroup);
        radgroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.btnAdd1: {
                        add = 1;

                        break;
                    }

                    case R.id.btnAdd2:
                        add=2;
                        break;
                    case R.id.btnAdd3:
                        add=3;
                        break;
                    case R.id.btnAdd4:
                        add=4;
                        break;
                    case R.id.btnAdd5:
                        add=5;
                        break;
                }

            }
        });
//        switch(add)
//        {
//            case 1:
//            {
//                                        TextView text1 = findViewById(R.id.textView4);
//                        //设置text01的文本值，此处会覆盖掉activity_main.xml的文本值
//                        text1.setText("所选择的号码为 R10001 请仔细对照");
//                break;
//            }
//
//            case 2:
//            break;
//            case 3:
//                break;
//            case 4:
//                break;
//            case 5:
//                break;
//        }
        bt=findViewById(R.id.btn_start_ok);
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.USE_MODEL = MainActivity.NANODET;
                Intent intent = new Intent(WellAddress.this, WellActivity.class);
                intent.putExtra("add",add);
                WellAddress.this.startActivity(intent);
            }
        });
    }
}