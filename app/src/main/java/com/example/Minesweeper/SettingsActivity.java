package com.example.Minesweeper;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.view.Gravity;
import android.view.ViewGroup.LayoutParams;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class SettingsActivity extends AppCompatActivity {
    private SharedPreferences preferences;
    private CompoundButton switchSound;
    private CompoundButton switchDarkMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // 设置窗口变暗程度（浮动卡片式设置面板）
        getWindow().setDimAmount(0.5f);
        
        preferences = getSharedPreferences("MinesweeperPrefs", MODE_PRIVATE);
        
        // 初始化控件
        switchSound = findViewById(R.id.switchSound);
        switchDarkMode = findViewById(R.id.switchDarkMode);
        ImageButton btnBack = findViewById(R.id.btnBack);
        TextView tvAbout = findViewById(R.id.tvAbout);
        TextView tvClearRecords = findViewById(R.id.tvClearRecords);
        TextView tvClearGames = findViewById(R.id.tvClearGames);
        TextView tvDonate = findViewById(R.id.tvDonate);

        // 加载保存的设置
        switchSound.setChecked(preferences.getBoolean("sound", true));
        switchDarkMode.setChecked(preferences.getBoolean("darkMode", false));

        // 设置开关监听器
        switchSound.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean("sound", isChecked).apply();
        });

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean("darkMode", isChecked).apply();
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        // 返回按钮点击事件
        btnBack.setOnClickListener(v -> finish());

        // 关于按钮点击事件
        tvAbout.setOnClickListener(v -> {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
            builder.setTitle("关于")
                   .setMessage("扫雷 v2.1\n\n作者：酷安@只对RMB着迷\n\n有bug或者别的问题可以到酷安留言或者联系我\n\n联系我：2470004849@qq.com")
                   .setPositiveButton("确定", null)
                   .show();
        });

        // 清除记录按钮点击事件
        tvClearRecords.setOnClickListener(v -> {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
            builder.setTitle("清除最佳记录")
                   .setMessage("确定要清除所有难度的最佳记录吗？")
                   .setPositiveButton("确定", (dialog, which) -> {
                       SharedPreferences recordsPrefs = getSharedPreferences("MinesweeperRecords", MODE_PRIVATE);
                       recordsPrefs.edit().clear().apply();
                   })
                   .setNegativeButton("取消", null)
                   .show();
        });

        // 清除局数统计按钮点击事件
        tvClearGames.setOnClickListener(v -> {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
            builder.setTitle("清除局数统计")
                   .setMessage("确定要清除所有难度的局数统计吗？")
                   .setPositiveButton("确定", (dialog, which) -> {
                       SharedPreferences recordsPrefs = getSharedPreferences("MinesweeperRecords", MODE_PRIVATE);
                       // 只清除局数统计，保留最佳记录
                       recordsPrefs.edit()
                           .remove("gamesPlayed_beginner")
                           .remove("gamesPlayed_intermediate")
                           .remove("gamesPlayed_expert")
                           .apply();
                   })
                   .setNegativeButton("取消", null)
                   .show();
        });

        // 捐赠按钮点击事件
        tvDonate.setOnClickListener(v -> {
            String[] paymentMethods = {"微信支付", "支付宝"};
            new MaterialAlertDialogBuilder(this)
                .setTitle("选择支付方式")
                .setItems(paymentMethods, (dialog, which) -> {
                    // 创建垂直布局
                    LinearLayout layout = new LinearLayout(this);
                    layout.setOrientation(LinearLayout.VERTICAL);
                    layout.setLayoutParams(new LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT));
                    layout.setPadding(50, 30, 50, 30);
                    layout.setGravity(Gravity.CENTER);

                    // 添加图片
                    ImageView imageView = new ImageView(this);
                    imageView.setLayoutParams(new LinearLayout.LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT));
                    imageView.setAdjustViewBounds(true);
                    if (which == 0) {
                        imageView.setImageResource(R.drawable.wechat_donate);
                    } else {
                        imageView.setImageResource(R.drawable.alipay_donate);
                    }
                    layout.addView(imageView);

                    // 添加文字
                    TextView textView = new TextView(this);
                    textView.setLayoutParams(new LinearLayout.LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT));
                    textView.setText("您的捐赠是它更新的动力");
                    textView.setTextSize(16);
                    textView.setGravity(Gravity.CENTER);
                    textView.setPadding(0, 30, 0, 0);
                    layout.addView(textView);

                    // 显示对话框
                    new MaterialAlertDialogBuilder(this)
                        .setView(layout)
                        .setPositiveButton("关闭", null)
                        .show();
                })
                .show();
        });
    }
} 