package com.example.Minesweeper;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * 状态栏沉浸：状态栏透明、内容延伸到系统栏下方，并按系统栏高度补齐内边距。
 *
 * 用法：在 setContentView 之前调用 {@link #enableImmersiveStatusBar(Activity)}。
 */
public final class ImmersiveUtils {

    private ImmersiveUtils() {
    }

    public static void enableImmersiveStatusBar(Activity activity) {
        Window window = activity.getWindow();

        // 1. 状态栏透明，内容延伸到状态栏下方
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // 关闭系统给状态栏自动加的遮罩，否则透明状态栏会发灰
            window.setStatusBarContrastEnforced(false);
        }
        WindowCompat.setDecorFitsSystemWindows(window, false);

        // 2. 状态栏图标深浅跟随当前深色/浅色模式
        boolean nightMode = (activity.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(!nightMode);
        }

        // 3. 给内容根视图补上系统栏内边距，保证内容不被状态栏/导航栏/刘海遮挡
        View content = activity.findViewById(android.R.id.content);
        if (content != null) {
            applySystemBarInsets(content);
        }
    }

    private static void applySystemBarInsets(final View view) {
        final int paddingLeft = view.getPaddingLeft();
        final int paddingTop = view.getPaddingTop();
        final int paddingRight = view.getPaddingRight();
        final int paddingBottom = view.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(paddingLeft + bars.left,
                    paddingTop + bars.top,
                    paddingRight + bars.right,
                    paddingBottom + bars.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(view);
    }
}
