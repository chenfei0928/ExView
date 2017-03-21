package com.chenfei.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.chenfei.exview.ExAnalysis;

import java.util.Collections;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_catchEx).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Collections.EMPTY_LIST.get(0);
                } catch (Throwable t) {
                    Log.e(TAG, "onClick: ", t);
                    ExAnalysis.holderEx(TAG, t);
                }
            }
        });
    }
}
