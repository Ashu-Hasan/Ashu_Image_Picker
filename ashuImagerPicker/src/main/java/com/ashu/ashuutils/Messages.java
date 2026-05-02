package com.ashu.ashuutils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

/**
 * Utility class for logging and debug messages.
 *
 * This class:
 * ✔ Centralizes all logs & debug toasts
 * ✔ Respects debug mode (AppConstants)
 * ✔ Prevents logs in production builds
 */
public final class Messages {

    private Messages() {
        // Prevent instantiation
    }

    /**
     * Prints debug log (only if debug is enabled)
     *
     * @param TAG     Tag for log identification
     * @param message Message to print
     */
    public static void showTestLog(String TAG, String message) {

        if (AppConstants.isDebug) {
            Log.d(TAG, message);
        }
    }

    /**
     * Prints error log (always useful even in production)
     *
     * @param TAG     Tag for log
     * @param message Error message
     */
    public static void showErrorLog(String TAG, String message) {
        Log.e(TAG, message);
    }

    /**
     * Shows debug toast (only if debug is enabled)
     *
     * @param context Context
     * @param message Message to show
     */
    public static void showTestToast(Context context, String message) {

        if (AppConstants.isDebug) {
            Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}
