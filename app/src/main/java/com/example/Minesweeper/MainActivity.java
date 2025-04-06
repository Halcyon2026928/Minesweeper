package com.example.Minesweeper;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import java.io.File;

public class MainActivity extends AppCompatActivity {
    private TextView tvBestTime;
    private Button btnContinueGame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 设置全屏
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        
        // 隐藏标题栏
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        // 初始化深色模式设置
        SharedPreferences prefs = getSharedPreferences("MinesweeperPrefs", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("darkMode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        
        // 确保应用数据目录存在
        File appDir = getApplicationContext().getFilesDir();
        if (!appDir.exists()) {
            appDir.mkdirs();
        }
        
        setContentView(R.layout.activity_main);

        // 添加安全检测
        if (!SecurityCheck.isAppSecure(this)) {
            // 如果检测到不安全，直接结束应用
            finish();
            System.exit(0);
            return;
        }

        // 初始化最佳记录显示
        tvBestTime = findViewById(R.id.tvBestTime);
        updateBestTime();

        // 设置按钮点击事件
        btnContinueGame = findViewById(R.id.btnContinueGame);
        Button btnStartGame = findViewById(R.id.btnStartGame);
        Button btnHelp = findViewById(R.id.btnHelp);
        Button btnSettings = findViewById(R.id.btnSettings);

        // 检查是否有保存的游戏进度
        SharedPreferences gameProgress = getSharedPreferences("GameProgress", MODE_PRIVATE);
        boolean hasProgress = gameProgress.contains("difficulty") && 
                            !gameProgress.getBoolean("isGameOver", false) && 
                            !gameProgress.getBoolean("isWin", false);
        
        btnContinueGame.setEnabled(hasProgress);
        btnContinueGame.setOnClickListener(v -> continueGame());
        btnStartGame.setOnClickListener(v -> startNewGame());
        btnHelp.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, HelpActivity.class)));
        btnSettings.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SettingsActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 更新最佳记录
        updateBestTime();
        
        // 检查是否有保存的游戏进度
        SharedPreferences gameProgress = getSharedPreferences("GameProgress", MODE_PRIVATE);
        boolean hasProgress = gameProgress.contains("difficulty") && 
                            !gameProgress.getBoolean("isGameOver", false) && 
                            !gameProgress.getBoolean("isWin", false);
        btnContinueGame.setEnabled(hasProgress);
    }

    private void continueGame() {
        SharedPreferences prefs = getSharedPreferences("GameProgress", MODE_PRIVATE);
        String savedDifficulty = prefs.getString("difficulty", "beginner");
        
        Intent intent = new Intent(MainActivity.this, GameActivity.class);
        intent.putExtra("difficulty", savedDifficulty);
        intent.putExtra("continueGame", true);
        startActivity(intent);
    }

    private void updateBestTime() {
        SharedPreferences recordsPrefs = getSharedPreferences("MinesweeperRecords", MODE_PRIVATE);
        int beginnerBest = recordsPrefs.getInt("bestTime_beginner", Integer.MAX_VALUE);
        int intermediateBest = recordsPrefs.getInt("bestTime_intermediate", Integer.MAX_VALUE);
        int expertBest = recordsPrefs.getInt("bestTime_expert", Integer.MAX_VALUE);
        
        int beginnerGames = recordsPrefs.getInt("gamesPlayed_beginner", 0);
        int intermediateGames = recordsPrefs.getInt("gamesPlayed_intermediate", 0);
        int expertGames = recordsPrefs.getInt("gamesPlayed_expert", 0);
        
        StringBuilder bestTimeText = new StringBuilder("最佳记录：\n");
        bestTimeText.append("初级：").append(beginnerBest == Integer.MAX_VALUE ? "暂无" : beginnerBest + "秒")
                   .append("（已玩").append(beginnerGames).append("局）\n");
        bestTimeText.append("中级：").append(intermediateBest == Integer.MAX_VALUE ? "暂无" : intermediateBest + "秒")
                   .append("（已玩").append(intermediateGames).append("局）\n");
        bestTimeText.append("高级：").append(expertBest == Integer.MAX_VALUE ? "暂无" : expertBest + "秒")
                   .append("（已玩").append(expertGames).append("局）");
        tvBestTime.setText(bestTimeText.toString());
    }

    private void showDifficultyDialog() {
        String[] difficulties = {"初级", "中级", "高级"};
        new AlertDialog.Builder(this)
                .setTitle("选择难度")
                .setItems(difficulties, (dialog, which) -> {
                    String difficulty;
                    switch (which) {
                        case 0:
                            difficulty = "beginner";
                            break;
                        case 1:
                            difficulty = "intermediate";
                            break;
                        case 2:
                            difficulty = "expert";
                            break;
                        default:
                            difficulty = "beginner";
                    }
                    startGame(difficulty);
                })
                .show();
    }

    private void startGame(String difficulty) {
        Intent intent = new Intent(MainActivity.this, GameActivity.class);
        intent.putExtra("difficulty", difficulty);
        startActivity(intent);
    }

    private void startNewGame() {
        // 清除保存的进度
        SharedPreferences prefs = getSharedPreferences("GameProgress", MODE_PRIVATE);
        prefs.edit().clear().apply();
        
        // 显示难度选择对话框
        showDifficultyDialog();
    }
}