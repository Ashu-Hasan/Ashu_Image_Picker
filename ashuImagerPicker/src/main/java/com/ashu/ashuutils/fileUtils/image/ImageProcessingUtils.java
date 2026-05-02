package com.ashu.ashuutils.fileUtils.image;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.ashu.ashuutils.AppConstants;
import com.ashu.ashuutils.Messages;
import com.ashu.ashuutils.R;
import com.ashu.ashuutils.fileUtils.FileUtils;
import com.ashu.ashuutils.models.CompressFileData;
import com.bumptech.glide.Glide;
import com.yalantis.ucrop.UCrop;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageProcessingUtils {

    /**
     * Handles image coming from Camera.
     *
     * @param TAG            Logging tag for debugging
     * @param activity       Activity context
     * @param cameraPath     Absolute file path returned from camera capture
     * @param imageView      Optional ImageView to display result
     * @param progressDialog Optional loader
     * @param callback       Result callback
     */
    public static void handleCameraImage(
            String TAG,
            Activity activity,
            String cameraPath,
            ImageView imageView,
            boolean cropImage,
            ProgressDialog progressDialog,
            FileUtils.FileCallback callback
    ) {
        Messages.showTestLog(TAG, "📸 Camera Image Path: " + cameraPath);

        if (cameraPath == null || cameraPath.isEmpty()) {
            Toast.makeText(activity, "Camera image not found", Toast.LENGTH_SHORT).show();
            return;
        }

        File imageFile = new File(cameraPath);
        processAndDisplayImage(TAG, activity, imageFile, imageView, cropImage, progressDialog, callback);
    }

    /**
     * Handles image picked from Gallery using Intent data.
     * Supports both single and multiple selection.
     *
     * @param TAG            Logging tag
     * @param activity       Activity context
     * @param data           Intent data returned from gallery
     * @param imageView      Optional preview ImageView
     * @param progressDialog Loader dialog
     * @param callback       Result callback
     */
    public static void handleGalleryFromIntent(
            String TAG,
            Activity activity,
            Intent data,
            ImageView imageView,
            boolean cropImage,
            ProgressDialog progressDialog,
            FileUtils.FileCallback callback
    ) {
        Uri selectedUri = null;

        // Case 1: Single image
        if (data.getData() != null) {
            selectedUri = data.getData();
            Messages.showTestLog(TAG, "🖼️ getData URI: " + selectedUri);
        }

        // Case 2: Multiple images / Android 13+
        else if (data.getClipData() != null && data.getClipData().getItemCount() > 0) {
            selectedUri = data.getClipData().getItemAt(0).getUri();
            Messages.showTestLog(TAG, "🖼️ ClipData URI: " + selectedUri);
        }

        if (selectedUri == null) {
            Toast.makeText(activity, "No image selected", Toast.LENGTH_SHORT).show();
            return;
        }

        handleGalleryFromUri(TAG, activity, selectedUri, imageView, cropImage, progressDialog, callback);
    }

    /**
     * Handles image when direct URI is available.
     * Used for Android 13+, Xiaomi devices, or permission-less pickers.
     *
     * @param TAG            Logging tag
     * @param activity       Activity context
     * @param uri            Image URI
     * @param imageView      Optional preview
     * @param progressDialog Loader
     * @param callback       Result callback
     */
    public static void handleGalleryFromUri(
            String TAG,
            Activity activity,
            Uri uri,
            ImageView imageView,
            boolean cropImage,
            ProgressDialog progressDialog,
            FileUtils.FileCallback callback
    ) {
        try {
            File imageFile = FileUtils.copyUriToCacheFile(activity, uri);

            if (imageFile == null || !imageFile.exists()) {
                throw new IOException("Failed to copy image");
            }

            Messages.showTestLog(TAG, "📂 File copied: " + imageFile.getAbsolutePath());

            processAndDisplayImage(TAG, activity, imageFile, imageView, cropImage, progressDialog, callback);

        } catch (Exception e) {
            Messages.showTestLog(TAG, "❌ Error: " + e.getMessage());
            Toast.makeText(activity, "Unable to load image", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Handles cropped image result from uCrop.
     *
     * This method:
     * - Retrieves cropped image URI from UCrop
     * - Converts URI → File
     * - Passes it to common processor
     *
     * @param TAG            Log tag for debugging
     * @param activity       Activity context
     * @param data           Intent returned from UCrop
     * @param imageView      Optional preview ImageView
     * @param progressDialog Optional loader
     * @param callback       Result callback
     */
    public static void handleCroppedImage(
            String TAG,
            Activity activity,
            Intent data,
            ImageView imageView,
            ProgressDialog progressDialog,
            FileUtils.FileCallback callback
    ) {
        try {
            // ✅ Get cropped image URI from UCrop
            Uri croppedUri = UCrop.getOutput(data);

            if (croppedUri == null) {
                throw new IllegalStateException("UCrop returned null URI");
            }

            Log.i(TAG, "✂️ Cropped Image URI: " + croppedUri);

            // ✅ Convert URI → File (safe approach)
            File imageFile = FileUtils.copyUriToCacheFile(activity, croppedUri);

            if (imageFile == null || !imageFile.exists()) {
                throw new IOException("Failed to copy cropped image");
            }

            Log.i(TAG, "📂 Cropped File Path: " + imageFile.getAbsolutePath());

            // 🔥 Send to common processor
            processAndDisplayImage(TAG, activity, imageFile, imageView, false, progressDialog, callback);

        } catch (Exception e) {
            Log.e(TAG, "❌ Crop handling error: " + e.getMessage());
            Toast.makeText(activity, "Failed to process cropped image", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Common method to:
     * - Compress image
     * - Load into ImageView (Glide)
     * - Return result via callback
     *
     * This keeps all heavy work in one place.
     */
    private static void processAndDisplayImage(
            String TAG,
            Activity activity,
            File imageFile,
            ImageView imageView,
            boolean cropImage,
            ProgressDialog progressDialog,
            FileUtils.FileCallback callback
    ) {

        // Show loader (UI thread)
        if (progressDialog != null) {
            progressDialog = new ProgressDialog(activity);
            progressDialog.setMessage("Loading image...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        ProgressDialog finalDialog = progressDialog;

        executor.execute(() -> {
            try {

                Messages.showTestLog(TAG, "🧩 Starting image processing...");

                // 🔥 Compress image (BACKGROUND THREAD)
                CompressFileData data = compressImage(TAG, activity, "AppFolder", imageFile, 1024);

                Messages.showTestLog(TAG, "✅ Compression completed");

                // 🔥 Decode bitmap (BACKGROUND THREAD)
                Bitmap croppedBitmap = null;
                if (cropImage) {
                    croppedBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                    Messages.showTestLog(TAG, "🖼️ Bitmap decoded for crop");
                }

                Bitmap finalCroppedBitmap = croppedBitmap;

                // 🔁 Switch to MAIN THREAD
                handler.post(() -> {

                    // Load into ImageView (UI thread)
                    if (imageView != null) {
                        Messages.showTestLog(TAG, "📸 Loading image into ImageView");
                        Glide.with(activity)
                                .load(imageFile)
                                .into(imageView);
                    }

                    // Dismiss loader
                    if (finalDialog != null && finalDialog.isShowing()) {
                        finalDialog.dismiss();
                        Messages.showTestLog(TAG, "⏹️ Loader dismissed");
                    }

                    // Callback
                    if (callback != null) {
                        Messages.showTestLog(TAG, "📤 Returning file via callback");
                        callback.onFileReady(data);
                    }

                    // ✂️ Show crop dialog (UI thread)
                    if (cropImage && finalCroppedBitmap != null) {

                        Messages.showTestLog(TAG, "✂️ Showing crop dialog");

                        Uri cropUri = FileUtils.getUriFromBitmap(finalCroppedBitmap, activity);

                        ImagePicker.showCropOptionDialog(
                                TAG,
                                activity,
                                cropUri,
                                AppConstants.IMAGE_CROP_REQUEST,
                                "cropped_image",
                                R.drawable.gallery_icon,
                                R.color.black,
                                new ImagePicker.CropImageCallback() {
                                    @Override
                                    public void onCropOptionCanceled() {
                                        Messages.showTestLog(TAG, "❌ Crop canceled by user");
                                    }
                                }
                        );
                    }

                });

            } catch (Exception e) {

                Messages.showTestLog(TAG, "🔥 Exception: " + e.getMessage());

                handler.post(() -> {
                    if (finalDialog != null && finalDialog.isShowing()) {
                        finalDialog.dismiss();
                    }

                    Toast.makeText(activity, "Error processing image", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    public static CompressFileData compressImage(String TAG, Activity activity, String appImageFolderName, File originalFile, int targetSizeKB) {
        CompressFileData data = new CompressFileData();

        try {
            Log.i(TAG, "🧮 Starting compression for: " + originalFile.getAbsolutePath());
            if (!originalFile.exists()) {
                Log.e(TAG, "❌ Original file does not exist!");
                return data;
            }

            // Decode image with scaling options to avoid OOM for large files
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(originalFile.getAbsolutePath(), options);
            Log.i(TAG, "📏 Original Image Dimensions: " + options.outWidth + "x" + options.outHeight);

            options.inSampleSize = calculateInSampleSize(options);
            options.inJustDecodeBounds = false;
            Bitmap bitmap = BitmapFactory.decodeFile(originalFile.getAbsolutePath(), options);

            if (bitmap == null) {
                Log.e(TAG, "❌ Failed to decode bitmap from file!");
                return data;
            }

            // Handle rotation
            ExifInterface exif = new ExifInterface(originalFile.getAbsolutePath());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int rotationAngle = switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90 -> 90;
                case ExifInterface.ORIENTATION_ROTATE_180 -> 180;
                case ExifInterface.ORIENTATION_ROTATE_270 -> 270;
                default -> 0;
            };

            if (rotationAngle != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotationAngle);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                Log.i(TAG, "🔄 Rotated image by " + rotationAngle + " degrees");
            }

            // Prepare for compression
            int quality = 90;
            int targetSizeBytes = targetSizeKB * 1024;
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            File appDir = new File(activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES), appImageFolderName);
            if (!appDir.exists()) appDir.mkdirs();

            File compressedFile = new File(appDir,
                    "compressed_" + System.currentTimeMillis() + "_" + originalFile.getName());


            // Compress loop
            do {
                byteArrayOutputStream.reset();
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream);
                Log.i(TAG, "📉 Compressing... quality=" + quality + ", size=" + byteArrayOutputStream.size() / 1024 + " KB");
                quality -= 5;
            } while (byteArrayOutputStream.size() > targetSizeBytes && quality > 10);

            // Write file
            try (FileOutputStream fos = new FileOutputStream(compressedFile)) {
                fos.write(byteArrayOutputStream.toByteArray());
            }

            Log.i(TAG, "✅ Compression finished, saved at: " + compressedFile.getAbsolutePath());

            // Save data
            String base64String = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);
            data.setBitmapFormat(bitmap);
            data.setFileFormat(compressedFile);
            data.setString64BaseFormat(base64String);

            bitmap.recycle();

        } catch (Exception e) {
            Log.e(TAG, "🔥 Compression failed: " + e.getMessage(), e);
            data.setBitmapFormat(null);
            data.setFileFormat(null);
            data.setString64BaseFormat(null);
        }

        return data;
    }

    private static int calculateInSampleSize(BitmapFactory.Options options) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > 1000 || width > 1000) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= 1000 && (halfWidth / inSampleSize) >= 1000) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

}
