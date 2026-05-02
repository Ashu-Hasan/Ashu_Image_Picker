package com.ashu.ashuutils.fileUtils.image;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.ashu.ashuutils.Messages;
import com.ashu.ashuutils.R;
import com.ashu.ashuutils.fileUtils.FileUtils;
import com.ashu.ashuutils.fileUtils.PermissionUtils;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropActivity;

import java.io.File;
import java.util.Objects;

public class ImagePicker {

    /**
     * Displays a custom dialog to pick an image either from Camera or Gallery.
     *
     * @param TAG             Used for logging/debugging. Helps developers track logs easily
     *                        when using this utility inside different modules or apps.
     *
     * @param activity        Activity context required to create and display the dialog,
     *                        access resources, and launch intents.
     *
     * @param req_code        Request code used to identify the result in onActivityResult()
     *                        (for camera or gallery response handling).
     *
     * @param designColor     Resource color ID used to dynamically tint dialog icons
     *                        (for UI customization based on app theme).
     *
     * @param callback        Interface callback to return user actions (camera/gallery selection)
     *                        and image result back to the calling Activity.
     */
    public static void showPickImageDialog(String TAG, Activity activity, int req_code, int designColor,
                                           FileUtils.ResultCallback callback) {

        // Create dialog instance using activity context
        final Dialog dialog = new Dialog(activity);

        // Remove default dialog title
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Set custom layout for dialog UI
        dialog.setContentView(R.layout.pick_image_dialog);

        // Get dialog window reference
        Window window = dialog.getWindow();

        // Ensure window is not null and set layout width/height
        Objects.requireNonNull(window).setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
        );

        // Set transparent background for rounded/custom UI
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Prevent dialog dismissal on outside touch or back press
        dialog.setCancelable(false);

        // Get main image view and apply tint color
        AppCompatImageView imageView = dialog.findViewById(R.id.image_view_dialog);


        // Get camera icon and apply tint
        ImageView cameraIcon = dialog.findViewById(R.id.cameraIcon);

        // Get gallery icon and apply tint
        ImageView galleryIcon = dialog.findViewById(R.id.galleryIcon);

        // Get close icon and apply tint
        ImageView closeIcon = dialog.findViewById(R.id.closeIcon);

        // 🔥 Resolve color from resources (theme-based dynamic color)
        if(designColor != 0) {
            int color = ContextCompat.getColor(activity, designColor);
            imageView.setColorFilter(color);
            cameraIcon.setColorFilter(color);
            galleryIcon.setColorFilter(color);
            closeIcon.setColorFilter(color);
        }

        // Camera option button
        LinearLayout cameraOptionBtn = dialog.findViewById(R.id.cameraOption);
        cameraOptionBtn.setOnClickListener(v -> {

            // Launch camera intent
            takePictureFromCamera(TAG, activity, req_code, true);

            // Notify callback that camera is selected
            if (callback != null) {
                callback.onCameraSelected(true);
            }

            // Close dialog
            dialog.dismiss();
        });

        // Gallery option button
        LinearLayout galleryOptionBtn = dialog.findViewById(R.id.galleryOption);
        galleryOptionBtn.setOnClickListener(v -> {

            // Notify callback that gallery is selected
            if (callback != null) {
                callback.onCameraSelected(false);
                callback.onGallerySelected();
            }

            // Close dialog
            dialog.dismiss();
        });

        // Close button action
        LinearLayout closeBtn = dialog.findViewById(R.id.closeBtn);
        closeBtn.setOnClickListener(v -> {
            // Simply dismiss dialog
            dialog.dismiss();
        });

        // Show dialog on screen
        dialog.show();
    }

    public interface CropImageCallback {
        void onCropOptionCanceled();
    }

    /**
     * Shows a dialog asking user whether they want to crop the selected image.
     *
     * @param TAG          Used for logging/debug tracking
     * @param context      Activity context (required for dialog + UCrop)
     * @param imageUri     URI of selected image (camera/gallery)
     * @param imageRequest Request code to identify crop result
     * @param imageName    Base name for cropped image file
     * @param appLogo      Drawable resource for dialog image (optional)
     * @param designColor  Color resource for YES button tint (optional)
     * @param callback     Callback for handling cancel action
     */
    public static void showCropOptionDialog(
            String TAG,
            Activity context,
            Uri imageUri,
            int imageRequest,
            String imageName,
            int appLogo,
            int designColor,
            CropImageCallback callback
    ) {

        Messages.showTestLog(TAG, "🟡 Opening crop option dialog");

        // Create dialog instance
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.crop_image_dialog_design);

        // Make dialog background transparent (for custom UI)
        Objects.requireNonNull(dialog.getWindow())
                .setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Initialize UI elements
        ImageView dialogImage = dialog.findViewById(R.id.dialogImage);
        TextView dialogMessage = dialog.findViewById(R.id.dialogMessage);
        TextView btnNo = dialog.findViewById(R.id.btnNo);
        TextView btnYes = dialog.findViewById(R.id.btnYes);

        // Apply dynamic color to YES button (if provided)
        if (designColor != 0) {
            Messages.showTestLog(TAG, "🎨 Applying button tint color");
            btnYes.setBackgroundTintList(
                    ContextCompat.getColorStateList(context, designColor)
            );
        }

        // Set app logo (if provided)
        if (appLogo != 0) {
            Messages.showTestLog(TAG, "🖼️ Setting dialog logo");
            dialogImage.setImageResource(appLogo);
        }

        // Set dialog message
        dialogMessage.setText("Would you like to crop image for better visibility?");
        Messages.showTestLog(TAG, "💬 Dialog message set");

        // ❌ NO button click → cancel crop
        btnNo.setOnClickListener(v -> {
            Messages.showTestLog(TAG, "❌ User selected NO (skip cropping)");
            dialog.dismiss();

            if (callback != null) {
                callback.onCropOptionCanceled();
            }
        });

        // ✅ YES button click → start crop
        btnYes.setOnClickListener(v -> {
            Messages.showTestLog(TAG, "✅ User selected YES (start cropping)");
            dialog.dismiss();

            startCrop(TAG, context, imageUri, imageRequest, imageName);
        });

        // Show dialog
        dialog.show();
        Messages.showTestLog(TAG, "📢 Crop dialog displayed");
    }


    /**
     * Starts UCrop image cropping activity.
     *
     * @param TAG          Logging tag
     * @param context      Activity context
     * @param sourceUri    Original image URI
     * @param requestCode  Request code for result handling
     * @param imageName    Base file name for cropped image
     */
    public static void startCrop(
            String TAG,
            Activity context,
            Uri sourceUri,
            int requestCode,
            String imageName
    ) {

        Messages.showTestLog(TAG, "✂️ Starting crop process");

        // Create destination file in cache directory
        File file = new File(
                context.getCacheDir(),
                imageName + System.currentTimeMillis() + ".jpg"
        );

        Messages.showTestLog(TAG, "📂 Destination file: " + file.getAbsolutePath());

        // Generate secure URI using FileProvider
        Uri destinationUri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".provider",
                file
        );

        Messages.showTestLog(TAG, "🔗 Destination URI: " + destinationUri);

        // Configure UCrop options
        UCrop.Options options = new UCrop.Options();

        // Compression settings
        options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
        options.setCompressionQuality(80);
        Messages.showTestLog(TAG, "⚙️ Compression set: JPEG (80%)");

        // Circular crop overlay
        options.setCircleDimmedLayer(true);
        Messages.showTestLog(TAG, "⭕ Circle crop enabled");

        // Hide crop frame & grid
        options.setShowCropFrame(false);
        options.setShowCropGrid(false);

        // Lock aspect ratio (1:1)
        options.withAspectRatio(1, 1);
        Messages.showTestLog(TAG, "📐 Aspect ratio locked to 1:1");

        // Toolbar customization
        options.setToolbarTitle("Crop");
        options.setToolbarColor(ContextCompat.getColor(context, R.color.white));
        options.setStatusBarColor(ContextCompat.getColor(context, R.color.white));

        // UI behavior
        options.setHideBottomControls(true);
        options.setFreeStyleCropEnabled(false);

        // Allow only scaling gesture
        options.setAllowedGestures(
                UCropActivity.SCALE,
                UCropActivity.NONE,
                UCropActivity.NONE
        );

        Messages.showTestLog(TAG, "🚀 Launching UCrop activity");

        // Start UCrop
        UCrop.of(sourceUri, destinationUri)
                .withOptions(options)
                .start(context, requestCode);
    }


    /**
     * Launches the device camera to capture an image and saves it to a file.
     *
     * This method:
     * - Checks for camera permission
     * - Creates a temporary image file in app storage
     * - Uses FileProvider to securely share file URI
     * - Opens camera intent with front/back camera option
     * - Stores file path for later retrieval
     *
     * @param TAG             Used for logging/debugging. Helps track logs for camera flow.
     *
     * @param context         Activity context required to:
     *                        - Access file storage
     *                        - Launch camera intent
     *                        - Request permissions
     *
     * @param req_code        Request code used to identify result in onActivityResult().
     *                        This helps distinguish camera result from other results.
     *
     * @param isFrontCamera   If true → attempts to open front camera
     *                        If false → opens back camera (default)
     *
     * ⚠️ Notes:
     * - Requires CAMERA permission
     * - Uses FileProvider (must be configured in manifest)
     * - Saved image path can be retrieved using FileUtils.getImagePath()
     */
    public static void takePictureFromCamera(
            String TAG,
            Activity context,
            int req_code,
            boolean isFrontCamera
    ) {

        Messages.showTestLog(TAG, "🚀 Starting camera flow...");

        // Step 1: Check camera permission
        if (PermissionUtils.isCameraPermissionGranted(TAG, context)) {

            try {
                Messages.showTestLog(TAG, "✅ Camera permission granted");

                // Step 2: Create image file in app-specific storage
                File photoFile = new File(
                        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                        "image_" + System.currentTimeMillis() + ".jpg"
                );

                Messages.showTestLog(TAG, "📂 Creating file: " + photoFile.getAbsolutePath());

                // Step 3: Generate secure URI using FileProvider
                Uri imageUri = FileProvider.getUriForFile(
                        context,
                        context.getPackageName() + ".provider",
                        photoFile
                );

                Messages.showTestLog(TAG, "🔗 FileProvider URI created");

                // Step 4: Create camera intent
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                // Set output location (important for full-size image)
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);

                // Grant temporary permission to camera app
                cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                // Step 5: Set camera direction (optional)
                Messages.showTestLog(TAG, "📸 Camera direction: " + (isFrontCamera ? "Front" : "Back"));

                if (isFrontCamera) {
                    // 🟢 Try to force front camera (device-dependent)
                    cameraIntent.putExtra("android.intent.extras.LENS_FACING_FRONT", 1);
                    cameraIntent.putExtra("android.intent.extras.CAMERA_FACING", 1);
                    cameraIntent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true);
                } else {
                    // 🔵 Default back camera
                    cameraIntent.putExtra("android.intent.extras.LENS_FACING_BACK", 0);
                    cameraIntent.putExtra("android.intent.extras.CAMERA_FACING", 0);
                    cameraIntent.putExtra("android.intent.extra.USE_FRONT_CAMERA", false);
                }

                // Step 6: Save image path for later use
                FileUtils.setImagePath(context, photoFile.getAbsolutePath());
                Messages.showTestLog(TAG, "💾 Image path saved successfully");

                // Step 7: Launch camera activity
                context.startActivityForResult(cameraIntent, req_code);
                Messages.showTestLog(TAG, "🚀 Camera intent launched");

            } catch (Exception e) {

                // Step 8: Handle error
                Messages.showTestLog(TAG, "🔥 Error while opening camera: " + e.getMessage());
            }

        } else {

            // Step 9: Request permission if not granted
            Messages.showTestLog(TAG, "❌ Camera permission not granted. Requesting permission...");
            PermissionUtils.requestCameraPermission(context);
        }
    }

}
