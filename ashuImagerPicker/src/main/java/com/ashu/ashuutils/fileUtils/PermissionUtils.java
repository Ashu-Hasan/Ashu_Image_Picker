package com.ashu.ashuutils.fileUtils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.ashu.ashuutils.AppConstants;
import com.ashu.ashuutils.Messages;

public class PermissionUtils {
    public static boolean isStoragePermissionGranted(String TAG, Activity context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED) {
                Messages.showTestLog(TAG, "Permission is granted");
                return true;
            } else {
                Messages.showTestLog(TAG,"Permission is revoked");
                ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 1000);
                return false;
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Messages.showTestLog(TAG,  "Permission is granted");
                return true;
            } else {
                Messages.showTestLog(TAG, "Permission is revoked");
                ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1000);
                return false;
            }
        } else {
            Messages.showTestLog(TAG,"Permission is granted");
            return true;
        }
    }

    // Method to request storage permission
    public static void requestStoragePermission(String TAG, Activity context) {
        Log.d(TAG, "requestStoragePermission Run");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Check if permissions are already granted
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO)
                            != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO)
                            != PackageManager.PERMISSION_GRANTED) {

                // Request permissions
                ActivityCompat.requestPermissions(
                        context,
                        new String[]{
                                Manifest.permission.READ_MEDIA_IMAGES,
                                Manifest.permission.READ_MEDIA_VIDEO,
                                Manifest.permission.READ_MEDIA_AUDIO
                        },
                        AppConstants.STORAGE_PERMISSION_REQUEST_CODE
                );
            } else {
                Log.d(TAG, "Permissions already granted for Android 14+");
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Check if READ_EXTERNAL_STORAGE is granted for older versions
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                // Request permission
                ActivityCompat.requestPermissions(
                        context,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        AppConstants.STORAGE_PERMISSION_REQUEST_CODE
                );
            } else {
                Log.d(TAG, "Permissions already granted for Android M+");
            }
        } else {
            Log.d(TAG, "No permissions required for Android versions below M");
        }
    }

    public static boolean isCameraPermissionGranted(String TAG, Activity context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            boolean granted = context.checkSelfPermission(Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED;

            Messages.showTestLog(TAG, granted ? "✅ Permission granted" : "❌ Permission revoked");

            return granted;
        }

        return true;
    }

    // Method to explicitly request camera permission
    public static void requestCameraPermission(Activity context) {
        ActivityCompat.requestPermissions(
                context,
                new String[]{Manifest.permission.CAMERA},
                AppConstants.CAMERA_PERMISSION_REQUEST_CODE
        );
    }
}
