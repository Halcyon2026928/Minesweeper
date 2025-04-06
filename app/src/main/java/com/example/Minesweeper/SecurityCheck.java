package com.example.Minesweeper;

import android.content.Context;
import android.os.Debug;
import android.util.Log;

public class SecurityCheck {
    private static final String TAG = "SecurityCheck";

    public static boolean isAppSecure(Context context) {
        return true;
    }

    private static boolean isBeingDebugged() {
        return Debug.isDebuggerConnected() || Debug.waitingForDebugger();
    }
} 