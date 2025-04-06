package com.example.Minesweeper;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.content.Context;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Random;
import android.view.WindowManager;
import android.view.Gravity;
import android.view.View;
import android.os.Build;
import android.view.MotionEvent;
import android.graphics.Color;
import android.content.pm.ActivityInfo;

public class GameActivity extends AppCompatActivity {
    private int rows, cols, mines;
    private Button[][] buttons;
    private boolean[][] minesArray;
    private boolean[][] revealed;
    private boolean[][] flagged;
    private int remainingMines;
    private TextView tvTimer, tvMineCount;
    private Handler timerHandler;
    private int seconds = 0;
    private boolean isGameOver = false;
    private long lastClickTime = 0;
    private int lastClickRow = -1;
    private int lastClickCol = -1;
    private MediaPlayer clickSound;
    private MediaPlayer explosionSound;
    private MediaPlayer winSound;
    private boolean soundEnabled;
    private Vibrator vibrator;
    private boolean isVibrating = false;
    private Handler longPressHandler = new Handler();
    private boolean isLongPressHandled = false;
    private String currentDifficulty;
    private int time;
    private boolean isFirstClick = true;
    private boolean isWin = false;
    private boolean isTimerRunning = false;
    private long startTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 设置屏幕方向为竖屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        // 设置全屏
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        
        // 隐藏标题栏
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        setContentView(R.layout.activity_game);

        // 添加安全检测
        if (!SecurityCheck.isAppSecure(this)) {
            // 如果检测到不安全，直接结束应用
            finish();
            System.exit(0);
            return;
        }

        // 初始化音效
        clickSound = MediaPlayer.create(this, R.raw.click);
        explosionSound = MediaPlayer.create(this, R.raw.explosion);
        winSound = MediaPlayer.create(this, R.raw.win);
        
        // 初始化震动器
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        
        // 获取声音设置
        SharedPreferences prefs = getSharedPreferences("MinesweeperPrefs", MODE_PRIVATE);
        soundEnabled = prefs.getBoolean("sound", true);

        // 初始化计时器
        timerHandler = new Handler(Looper.getMainLooper());

        // 获取难度参数
        Intent intent = getIntent();
        String difficulty = intent.getStringExtra("difficulty");
        if (difficulty == null) {
            difficulty = "beginner"; // 设置默认难度
        }
        
        // 保存难度值用于后续使用
        currentDifficulty = difficulty;
        
        switch (difficulty) {
            case "beginner":
                rows = 9;
                cols = 9;
                mines = 10;
                break;
            case "intermediate":
                rows = 16;
                cols = 16;
                mines = 40;
                break;
            case "expert":
                rows = 28;
                cols = 18;
                mines = 99;
                break;
            default:
                rows = 9;
                cols = 9;
                mines = 10;
                break;
        }

        remainingMines = mines;
        tvTimer = findViewById(R.id.tvTimer);
        tvMineCount = findViewById(R.id.tvMineCount);
        TextView tvGridSize = findViewById(R.id.tvGridSize);
        tvGridSize.setText(cols + "×" + rows);
        tvMineCount.setText("剩余地雷: " + remainingMines);

        // 初始化游戏
        initializeGame();
        setupButtons();
        
        // 尝试加载保存的游戏进度
        loadGameProgress();

        // 设置重新开始按钮
        Button btnRestart = findViewById(R.id.btnRestart);
        btnRestart.setOnClickListener(v -> {
            // 停止计时器
            if (timerHandler != null) {
                timerHandler.removeCallbacksAndMessages(null);
            }
            // 清除保存的进度
            clearGameProgress();
            // 重置游戏状态
            isFirstClick = true;
            isGameOver = false;
            isWin = false;
            isTimerRunning = false;
            seconds = 0;
            remainingMines = mines;
            // 重置UI
            tvTimer.setText("时间: 0");
            tvMineCount.setText("剩余地雷: " + remainingMines);
            // 重新初始化游戏
            initializeGame();
            setupButtons();
            updateAllButtons();
        });

        // 设置返回主页按钮
        Button btnHome = findViewById(R.id.btnHome);
        btnHome.setOnClickListener(v -> {
            // 保存当前进度
            saveGameProgress();
            finish();
        });
    }

    private void initializeGame() {
        buttons = new Button[rows][cols];
        minesArray = new boolean[rows][cols];
        revealed = new boolean[rows][cols];
        flagged = new boolean[rows][cols];

        // 随机放置地雷
        Random random = new Random();
        int minesPlaced = 0;
        while (minesPlaced < mines) {
            int row = random.nextInt(rows);
            int col = random.nextInt(cols);
            if (!minesArray[row][col]) {
                minesArray[row][col] = true;
                minesPlaced++;
            }
        }
    }

    private void setupButtons() {
        GridLayout gridLayout = findViewById(R.id.gridLayout);
        gridLayout.setColumnCount(cols);
        gridLayout.setRowCount(rows);

        // 计算每个按钮的大小
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        
        // 计算可用高度（减去顶部状态栏、标题栏和底部按钮的空间）
        int availableHeight = (int)(screenHeight * 0.7); // 使用屏幕高度的70%作为游戏区域
        
        // 根据宽高比例计算按钮大小
        int buttonSizeFromWidth = (int) (screenWidth * 0.95f / cols);
        int buttonSizeFromHeight = availableHeight / rows;
        
        // 选择较小的尺寸以确保完全适应屏幕
        int buttonSize = Math.min(buttonSizeFromWidth, buttonSizeFromHeight);

        // 设置网格布局参数
        gridLayout.setUseDefaultMargins(false);
        gridLayout.setAlignmentMode(GridLayout.ALIGN_BOUNDS);

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                Button button = new Button(this);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = buttonSize;
                params.height = buttonSize;
                params.setMargins(1, 1, 1, 1);
                
                // 设置网格位置
                params.rowSpec = GridLayout.spec(i, 1f);
                params.columnSpec = GridLayout.spec(j, 1f);
                
                button.setLayoutParams(params);
                button.setPadding(0, 0, 0, 0);
                
                // 设置按钮样式
                button.setBackgroundResource(R.drawable.cell_unrevealed);
                button.setTextSize(buttonSize / 4); // 根据按钮大小动态设置文字大小
                button.setMinWidth(0);
                button.setMinHeight(0);
                button.setMinimumWidth(0);
                button.setMinimumHeight(0);
                button.setGravity(Gravity.CENTER); // 设置文本居中
                button.setIncludeFontPadding(false); // 移除文本的内边距
                
                final int row = i;
                final int col = j;

                button.setOnTouchListener((v, event) -> {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        isLongPressHandled = false;
                        longPressHandler.postDelayed(() -> {
                            if (!isLongPressHandled && !isGameOver) {
                                isLongPressHandled = true;
                                toggleFlag(row, col);
                            }
                        }, 125); // 125毫秒后触发长按事件
                    } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                        longPressHandler.removeCallbacksAndMessages(null);
                        if (!isLongPressHandled && !isGameOver && !flagged[row][col]) {
                            // 如果不是长按，且格子没有被标记，则触发点击事件
                            if (soundEnabled) {
                                playSound(clickSound);
                            }
                            revealCell(row, col);
                        }
                    }
                    return false; // 返回false表示事件未被处理，允许继续传递给onClick
                });

                // 设置双击事件
                button.setOnClickListener(v -> {
                    if (!isGameOver && !isLongPressHandled) {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastClickTime < 500 && row == lastClickRow && col == lastClickCol) {
                            // 双击事件
                            handleDoubleClick(row, col);
                        }
                        lastClickTime = currentTime;
                        lastClickRow = row;
                        lastClickCol = col;
                    }
                });

                buttons[i][j] = button;
                gridLayout.addView(button);
            }
        }
    }

    private void playSound(MediaPlayer sound) {
        if (soundEnabled && sound != null) {
            sound.seekTo(0);
            sound.start();
        }
    }

    private void revealCell(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols || revealed[row][col] || flagged[row][col]) {
            return;
        }

        // 第一次点击时启动计时器
        if (isFirstClick) {
            isFirstClick = false;
            isTimerRunning = true;
            startTimer();
        }

        revealed[row][col] = true;
        Button button = buttons[row][col];

        if (minesArray[row][col]) {
            button.setText("💣");
            button.setBackgroundResource(R.drawable.cell_revealed);
            // 根据难度设置炸弹大小
            String difficulty = getIntent().getStringExtra("difficulty");
            if ("expert".equals(difficulty)) {
                button.setTextSize(12); // 高级模式使用更小的大小
            } else if ("intermediate".equals(difficulty)) {
                button.setTextSize(16); // 中级模式使用中等大小
            } else {
                button.setTextSize(20); // 初级保持原来的大小
            }
            button.setTextColor(getResources().getColor(R.color.game_over_text));
            button.setPadding(2, 0, 2, 0);
            button.setGravity(Gravity.CENTER);
            button.setIncludeFontPadding(false);
            playSound(explosionSound);
            gameOver();
            return;
        }

        playSound(clickSound);
        int adjacentMines = countAdjacentMines(row, col);
        if (adjacentMines > 0) {
            button.setText(String.valueOf(adjacentMines));
            // 根据难度设置数字大小
            String difficulty = getIntent().getStringExtra("difficulty");
            if ("expert".equals(difficulty)) {
                button.setTextSize(14); // 高级模式使用更小的大小
            } else {
                button.setTextSize(20); // 初级和中级保持原来的大小
            }
            button.setTextColor(getResources().getColor(getNumberColor(adjacentMines)));
            button.setPadding(2, 0, 2, 0);
            button.setGravity(Gravity.CENTER);
            button.setIncludeFontPadding(false);
        } else {
            button.setText("");
            button.setBackgroundResource(R.drawable.cell_revealed);
        }
        button.setBackgroundResource(R.drawable.cell_revealed);

        if (adjacentMines == 0) {
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    revealCell(row + i, col + j);
                }
            }
        }

        checkWin();
    }

    private void toggleFlag(int row, int col) {
        if (!revealed[row][col]) {
            flagged[row][col] = !flagged[row][col];
            Button button = buttons[row][col];
            if (flagged[row][col]) {
                button.setText("🚩");
                button.setTextSize(20);
                button.setTextColor(getResources().getColor(R.color.flagged_text));
                button.setPadding(2, 0, 2, 0);
                button.setGravity(Gravity.CENTER);
                button.setIncludeFontPadding(false);
                remainingMines--;
            } else {
                button.setText("");
                button.setBackgroundResource(R.drawable.cell_unrevealed);
                button.setTextSize(20);
                button.setPadding(2, 0, 2, 0);
                button.setGravity(Gravity.CENTER);
                button.setIncludeFontPadding(false);
                remainingMines++;
            }
            // 添加和取消标记时都震动
            if (vibrator != null && vibrator.hasVibrator() && !isVibrating) {
                isVibrating = true;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(50);
                }
                // 50毫秒后重置震动标志
                new Handler().postDelayed(() -> isVibrating = false, 50);
            }
            tvMineCount.setText("剩余地雷: " + remainingMines);
            checkWin();
        }
    }

    private int countAdjacentMines(int row, int col) {
        int count = 0;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int newRow = row + i;
                int newCol = col + j;
                if (newRow >= 0 && newRow < rows && newCol >= 0 && newCol < cols) {
                    if (minesArray[newRow][newCol]) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private void checkWin() {
        boolean won = true;
        int correctFlags = 0;
        
        // 检查标记的准确性
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (flagged[i][j]) {
                    if (minesArray[i][j]) {
                        correctFlags++; // 正确标记的地雷数
                    } else {
                        won = false; // 标错位置，直接判定失败
                        break;
                    }
                }
            }
            if (!won) break;
        }
        
        // 只有所有地雷都被正确标记才算胜利
        if (won && correctFlags == mines) {
            isGameOver = true;
            timerHandler.removeCallbacksAndMessages(null);
            
            // 游戏胜利时自动显示所有安全区
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    if (!minesArray[i][j] && !revealed[i][j]) {
                        Button button = buttons[i][j];
                        revealed[i][j] = true;
                        int adjacentMines = countAdjacentMines(i, j);
                        if (adjacentMines > 0) {
                            button.setText(String.valueOf(adjacentMines));
                            button.setTextSize(20);
                            button.setTextColor(getResources().getColor(getNumberColor(adjacentMines)));
                            button.setPadding(2, 0, 2, 0);
                            button.setGravity(Gravity.CENTER);
                        } else {
                            button.setText("");
                            button.setBackgroundResource(R.drawable.cell_revealed);
                        }
                        button.setBackgroundResource(R.drawable.cell_revealed);
                    }
                }
            }
            
            showWinDialog();
        }
    }

    private void showWinDialog() {
        playSound(winSound);
        
        // 获取当前难度的最佳记录
        SharedPreferences prefs = getSharedPreferences("MinesweeperRecords", MODE_PRIVATE);
        String difficulty = getIntent().getStringExtra("difficulty");
        int bestTime = prefs.getInt("bestTime_" + difficulty, Integer.MAX_VALUE);
        
        // 如果当前成绩更好，更新记录
        if (seconds < bestTime) {
            prefs.edit().putInt("bestTime_" + difficulty, seconds).apply();
            bestTime = seconds;
        }

        // 更新局数统计
        int gamesPlayed = prefs.getInt("gamesPlayed_" + difficulty, 0);
        prefs.edit().putInt("gamesPlayed_" + difficulty, gamesPlayed + 1).apply();

        // 构建对话框消息
        String message = String.format("恭喜你赢了！\n\n本次用时：%d秒\n历史最佳：%d秒\n\n当前难度已玩局数：%d",
                seconds,
                bestTime == Integer.MAX_VALUE ? 0 : bestTime,
                gamesPlayed + 1);

        // 显示对话框
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("游戏胜利")
                .setMessage(message)
                .setPositiveButton("重新开始", (dialog1, which) -> {
                    // 停止计时器
                    if (timerHandler != null) {
                        timerHandler.removeCallbacksAndMessages(null);
                    }
                    // 清除保存的进度
                    clearGameProgress();
                    // 重置游戏状态
                    isFirstClick = true;
                    isGameOver = false;
                    isWin = false;
                    isTimerRunning = false;
                    seconds = 0;
                    remainingMines = mines;
                    // 重置UI
                    tvTimer.setText("时间: 0");
                    tvMineCount.setText("剩余地雷: " + remainingMines);
                    // 重新初始化游戏
                    initializeGame();
                    setupButtons();
                    updateAllButtons();
                })
                .setNegativeButton("返回主页", (dialog1, which) -> {
                    finish();
                })
                .setNeutralButton("关闭", null) // 添加关闭按钮
                .setCancelable(true) // 允许点击外部关闭
                .create();
        
        dialog.show();
    }

    private void gameOver() {
        isGameOver = true;
        isTimerRunning = false;
        
        // 停止计时器
        if (timerHandler != null) {
            timerHandler.removeCallbacksAndMessages(null);
        }
        
        // 播放爆炸音效
        if (soundEnabled && explosionSound != null) {
            explosionSound.start();
        }
        
        // 更新局数统计
        SharedPreferences prefs = getSharedPreferences("MinesweeperRecords", MODE_PRIVATE);
        String difficulty = getIntent().getStringExtra("difficulty");
        int gamesPlayed = prefs.getInt("gamesPlayed_" + difficulty, 0);
        prefs.edit().putInt("gamesPlayed_" + difficulty, gamesPlayed + 1).apply();
        
        // 显示所有地雷
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (minesArray[i][j]) {
                    Button button = buttons[i][j];
                    button.setText("💣");
                    button.setBackgroundResource(R.drawable.cell_revealed);
                    // 根据难度统一设置炸弹大小
                    if ("expert".equals(difficulty)) {
                        button.setTextSize(12); // 高级模式使用更小的大小
                    } else if ("intermediate".equals(difficulty)) {
                        button.setTextSize(16); // 中级模式使用中等大小
                    } else {
                        button.setTextSize(20); // 初级保持原来的大小
                    }
                    button.setTextColor(getResources().getColor(R.color.game_over_text));
                    button.setPadding(2, 0, 2, 0);
                    button.setGravity(Gravity.CENTER);
                    button.setIncludeFontPadding(false);
                }
            }
        }
        
        // 显示游戏结束对话框
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("游戏结束")
            .setMessage(String.format("很遗憾，你踩到地雷了！\n\n当前难度已玩局数：%d", gamesPlayed + 1))
            .setPositiveButton("重新开始", (dialog1, which) -> {
                // 停止计时器
                if (timerHandler != null) {
                    timerHandler.removeCallbacksAndMessages(null);
                }
                // 清除保存的进度
                clearGameProgress();
                // 重置游戏状态
                isFirstClick = true;
                isGameOver = false;
                isWin = false;
                isTimerRunning = false;
                seconds = 0;
                remainingMines = mines;
                // 重置UI
                tvTimer.setText("时间: 0");
                tvMineCount.setText("剩余地雷: " + remainingMines);
                // 重新初始化游戏
                initializeGame();
                setupButtons();
                updateAllButtons();
            })
            .setNegativeButton("返回主页", (dialog1, which) -> {
                // 保存当前进度
                saveGameProgress();
                finish();
            })
            .setNeutralButton("关闭", null) // 添加关闭按钮
            .setCancelable(true) // 允许点击外部关闭
            .create();
        
        dialog.show();
    }

    private void startTimer() {
        if (timerHandler == null) {
            timerHandler = new Handler(Looper.getMainLooper());
        }
        
        // 如果已经有计时器在运行，先移除
        timerHandler.removeCallbacksAndMessages(null);
        
        // 更新显示当前时间
        tvTimer.setText("时间: " + seconds);
        
        // 创建新的计时器任务
        timerHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isTimerRunning && !isGameOver && !isWin) {
                    seconds++;
                    tvTimer.setText("时间: " + seconds);
                    timerHandler.postDelayed(this, 1000);
                }
            }
        });
    }

    private void handleDoubleClick(int row, int col) {
        if (!revealed[row][col]) return;
        
        int adjacentMines = countAdjacentMines(row, col);
        if (adjacentMines == 0) return;

        // 计算周围已标记的地雷数和未点开的格子
        int flaggedCount = 0;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int newRow = row + i;
                int newCol = col + j;
                if (newRow >= 0 && newRow < rows && newCol >= 0 && newCol < cols) {
                    if (flagged[newRow][newCol]) {
                        flaggedCount++;
                    }
                }
            }
        }

        // 如果标记数量等于周围地雷数，则打开未标记的格子
        if (flaggedCount == adjacentMines) {
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    int newRow = row + i;
                    int newCol = col + j;
                    if (newRow >= 0 && newRow < rows && newCol >= 0 && newCol < cols) {
                        if (!revealed[newRow][newCol] && !flagged[newRow][newCol]) {
                            revealCell(newRow, newCol);
                        }
                    }
                }
            }
        } else {
            // 标记数量不足时，闪烁提示周围未标记的格子
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    int newRow = row + i;
                    int newCol = col + j;
                    if (newRow >= 0 && newRow < rows && newCol >= 0 && newCol < cols) {
                        if (!revealed[newRow][newCol] && !flagged[newRow][newCol]) {
                            Button button = buttons[newRow][newCol];
                            // 保存原始背景
                            int originalBackground = R.drawable.cell_unrevealed;
                            // 设置闪烁背景
                            button.setBackgroundResource(R.drawable.cell_revealed);
                            // 0.5秒后恢复原始背景
                            new Handler().postDelayed(() -> {
                                if (!revealed[newRow][newCol] && !flagged[newRow][newCol]) {
                                    button.setBackgroundResource(originalBackground);
                                }
                            }, 50);
                        }
                    }
                }
            }
        }
    }

    private int getNumberColor(int count) {
        switch (count) {
            case 1:
                return R.color.number_1;
            case 2:
                return R.color.number_2;
            case 3:
                return R.color.number_3;
            case 4:
                return R.color.number_4;
            case 5:
                return R.color.number_5;
            case 6:
                return R.color.number_6;
            case 7:
                return R.color.number_7;
            case 8:
                return R.color.number_8;
            default:
                return R.color.number_unknown;
        }
    }

    private void saveGameProgress() {
        SharedPreferences prefs = getSharedPreferences("GameProgress", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        // 保存游戏状态
        editor.putString("difficulty", getIntent().getStringExtra("difficulty"));
        editor.putInt("time", seconds);
        editor.putInt("remainingMines", remainingMines);
        editor.putBoolean("isFirstClick", isFirstClick);
        editor.putBoolean("isGameOver", isGameOver);
        editor.putBoolean("isWin", isWin);
        
        // 保存棋盘状态
        StringBuilder boardState = new StringBuilder();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                boardState.append(revealed[i][j] ? "1" : "0");
                boardState.append(flagged[i][j] ? "1" : "0");
                boardState.append(minesArray[i][j] ? "1" : "0");
            }
        }
        editor.putString("boardState", boardState.toString());
        
        // 保存计时器状态
        editor.putBoolean("isTimerRunning", isTimerRunning);
        
        editor.apply();
    }

    private void loadGameProgress() {
        SharedPreferences prefs = getSharedPreferences("GameProgress", MODE_PRIVATE);
        
        // 检查是否有保存的进度
        if (!prefs.contains("difficulty")) {
            return;
        }
        
        // 加载游戏状态
        currentDifficulty = prefs.getString("difficulty", "beginner");
        seconds = prefs.getInt("time", 0);
        remainingMines = prefs.getInt("remainingMines", 0);
        isFirstClick = prefs.getBoolean("isFirstClick", true);
        isGameOver = prefs.getBoolean("isGameOver", false);
        isWin = prefs.getBoolean("isWin", false);
        
        // 加载棋盘状态
        String boardState = prefs.getString("boardState", "");
        if (!boardState.isEmpty()) {
            // 检查保存的棋盘大小是否与当前难度匹配
            int expectedLength = rows * cols * 3; // 每个格子3个状态
            if (boardState.length() == expectedLength) {
                int index = 0;
                for (int i = 0; i < rows; i++) {
                    for (int j = 0; j < cols; j++) {
                        revealed[i][j] = boardState.charAt(index++) == '1';
                        flagged[i][j] = boardState.charAt(index++) == '1';
                        minesArray[i][j] = boardState.charAt(index++) == '1';
                    }
                }
            } else {
                // 如果棋盘大小不匹配，清除保存的进度
                clearGameProgress();
                return;
            }
        }
        
        // 加载计时器状态
        isTimerRunning = prefs.getBoolean("isTimerRunning", false);
        
        // 更新UI
        updateAllButtons();
        tvMineCount.setText("剩余地雷: " + remainingMines);
        tvTimer.setText("时间: " + seconds);
        
        // 如果游戏正在进行，恢复计时器
        if (!isFirstClick && !isGameOver && !isWin) {
            isTimerRunning = true;
            startTimer();
        }
    }

    private void clearGameProgress() {
        SharedPreferences prefs = getSharedPreferences("GameProgress", MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    private void updateAllButtons() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                Button button = buttons[i][j];
                if (revealed[i][j]) {
                    if (minesArray[i][j]) {
                        button.setText("💣");
                        button.setBackgroundResource(R.drawable.cell_revealed);
                        String difficulty = getIntent().getStringExtra("difficulty");
                        if ("expert".equals(difficulty)) {
                            button.setTextSize(12);
                        } else if ("intermediate".equals(difficulty)) {
                            button.setTextSize(16);
                        } else {
                            button.setTextSize(20);
                        }
                        button.setTextColor(getResources().getColor(R.color.game_over_text));
                    } else {
                        int count = countAdjacentMines(i, j);
                        if (count > 0) {
                            button.setText(String.valueOf(count));
                            String difficulty = getIntent().getStringExtra("difficulty");
                            if ("expert".equals(difficulty)) {
                                button.setTextSize(14);
                            } else {
                                button.setTextSize(20);
                            }
                            button.setTextColor(getResources().getColor(getNumberColor(count)));
                        } else {
                            button.setText("");
                        }
                        button.setBackgroundResource(R.drawable.cell_revealed);
                    }
                } else if (flagged[i][j]) {
                    button.setText("🚩");
                    button.setBackgroundResource(R.drawable.cell_unrevealed);
                    button.setTextSize(20);
                    button.setTextColor(getResources().getColor(R.color.flagged_text));
                } else {
                    button.setText("");
                    button.setBackgroundResource(R.drawable.cell_unrevealed);
                }
                button.setPadding(2, 0, 2, 0);
                button.setGravity(Gravity.CENTER);
                button.setIncludeFontPadding(false);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!isGameOver && !isWin) {
            saveGameProgress();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timerHandler != null) {
            timerHandler.removeCallbacksAndMessages(null);
        }
        // 释放音效资源
        if (clickSound != null) {
            clickSound.release();
            clickSound = null;
        }
        if (explosionSound != null) {
            explosionSound.release();
            explosionSound = null;
        }
        if (winSound != null) {
            winSound.release();
            winSound = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadGameProgress();
    }

    private void startNewGame() {
        // 清除保存的进度
        clearGameProgress();
        
        // 重置游戏状态
        isFirstClick = true;
        isGameOver = false;
        isWin = false;
        time = 0;
        remainingMines = mines;
        isTimerRunning = false;
        startTime = 0;
        
        // 重置棋盘
        initializeGame();
        setupButtons();
        updateAllButtons();
        updateMineCount();
        updateTimer();
    }

    private void updateMineCount() {
        tvMineCount.setText("剩余地雷: " + remainingMines);
    }

    private void updateTimer() {
        tvTimer.setText("时间: " + time);
    }
}