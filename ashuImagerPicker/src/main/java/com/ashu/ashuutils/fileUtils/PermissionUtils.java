package com.ashu.ashuutils.fileUtils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.ashu.ashuutils.ImagePickerAppConstants;
import com.ashu.ashuutils.ImageHelperMessages;

public class PermissionUtils {
    /**
     * Checks whether CAMERA permission is granted.
     *
     * @param TAG     Logging tag
     * @param context Activity context
     * @return true if granted, false otherwise
     */
    public static boolean isCameraPermissionGranted(String TAG, Activity context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            boolean granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED;

            ImageHelperMessages.showTestLog(TAG,
                    granted ? "✅ Camera permission granted" : "❌ Camera permission denied");

            return granted;
        }

        return true;
    }

    /**
     * Requests CAMERA permission from user.
     *
     * @param context Activity context
     *
     * ⚠️ Should be handled in Activity onRequestPermissionsResult()
     */
    public static void requestCameraPermission(Activity context) {

        ImageHelperMessages.showTestLog("PermissionUtils", "📸 Requesting camera permission");

        ActivityCompat.requestPermissions(
                context,
                new String[]{Manifest.permission.CAMERA},
                ImagePickerAppConstants.CAMERA_PERMISSION_REQUEST_CODE
        );
    }
}
