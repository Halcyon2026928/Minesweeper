package com.example.Minesweeper;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class HelpActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 沉浸式状态栏
        ImmersiveUtils.enableImmersiveStatusBar(this);
        setContentView(R.layout.activity_help);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // 设置返回按钮点击事件
        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }
} 