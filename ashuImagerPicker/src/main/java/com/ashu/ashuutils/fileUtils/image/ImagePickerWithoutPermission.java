package com.ashu.ashuutils.fileUtils.image;

import android.net.Uri;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;

import com.ashu.ashuutils.ImageHelperMessages;

import org.jetbrains.annotations.Nullable;

/**
 * Utility class to pick images from gallery WITHOUT requiring storage permission.
 *
 * This uses Android Photo Picker (Activity Result API), which is:
 * ✔ Google Play policy compliant
 * ✔ No READ_MEDIA / STORAGE permission required
 * ✔ Safe and modern approach (Android 13+ recommended)
 *
 * ⚠️ IMPORTANT:
 * - You MUST call init() inside Activity.onCreate()
 * - This class uses a static launcher, so initialization is required before usage
 */
public class ImagePickerWithoutPermission {

    /**
     * Callback interface to return selected image URI.
     */
    public interface ImagePickCallback {
        void onImagePicked(@Nullable Uri uri);
    }

    // Activity Result launcher (handles photo picker result)
    private static ActivityResultLauncher<PickVisualMediaRequest> launcher;

    // Callback reference
    private static ImagePickCallback callback;

    /**
     * Initializes the Image Picker.
     *
     * MUST be called once inside Activity.onCreate()
     *
     * @param activity ComponentActivity (required for Activity Result API)
     */
    public static void init(ComponentActivity activity) {

        ImageHelperMessages.showTestLog("ImagePicker", "🔧 Initializing ImagePickerWithoutPermission");

        launcher = activity.registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (callback != null) {

                        if (uri != null) {
                            ImageHelperMessages.showTestLog("ImagePicker", "🖼️ Image selected: " + uri);
                        } else {
                            ImageHelperMessages.showTestLog("ImagePicker", "❌ No image selected (user cancelled)");
                        }

                        callback.onImagePicked(uri);
                    }
                }
        );
    }

    /**
     * Launches the system gallery picker WITHOUT requiring storage permission.
     *
     * @param TAG Used for logging/debugging
     * @param cb  Callback to receive selected image URI
     *
     * ⚠️ Notes:
     * - init() must be called before using this method
     * - Returns null if user cancels selection
     */
    public static void pickImage(String TAG, ImagePickCallback cb) {

        ImageHelperMessages.showTestLog(TAG, "📂 Opening gallery (Photo Picker)");

        if (launcher == null) {
            ImageHelperMessages.showTestLog(TAG, "❌ ImagePicker not initialized. Call init() in onCreate()");
            throw new IllegalStateException("ImagePicker not initialized. Call init() in onCreate()");
        }

        callback = cb;

        // Launch Photo Picker (no permission required)
        launcher.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()
        );

        ImageHelperMessages.showTestLog(TAG, "🚀 Gallery picker launched");
    }
}
