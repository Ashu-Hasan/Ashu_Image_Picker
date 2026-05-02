package com.ashu.ashuutils.fileUtils.image;

import android.app.Activity;
import android.net.Uri;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;

import com.ashu.ashuutils.Messages;

import org.jetbrains.annotations.Nullable;

public class ImagePickerWithoutPermission {

    public interface ImagePickCallback {
        void onImagePicked(@Nullable Uri uri);
    }

    private static ActivityResultLauncher<PickVisualMediaRequest> launcher;
    private static ImagePickCallback callback;

    // ✅ MUST be called in Activity.onCreate()
    public static void init(ComponentActivity activity) {

        launcher = activity.registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (callback != null) {
                        callback.onImagePicked(uri);
                    }
                }
        );
    }

    // ✅ Only launch here (no registration)
    public static void pickImage(String TAG, ImagePickCallback cb) {

        if (launcher == null) {
            Messages.showTestLog(TAG, "ImagePicker not initialized. Call init() in onCreate()");
        }

        callback = cb;

        launcher.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()
        );
    }
}
