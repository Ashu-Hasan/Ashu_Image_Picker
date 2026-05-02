📸 Ashu Image Picker

A lightweight Android utility library to pick images from Camera & Gallery without storage permission, fully compliant with Google Play Photo & Video policy.

Designed for apps that need one-time or occasional image selection (profile image, document upload, etc.).

🚀 Features

✔ Camera Image Capture
✔ Gallery Image Picker (No Permission Required)
✔ Image Compression
✔ Optional Crop (UCrop)
✔ Play Store Policy Safe
✔ Debug Logging Support
✔ Clean Callback API

⚠️ Play Store Compliance

This library does NOT require:

❌ READ_MEDIA_IMAGES
❌ READ_MEDIA_VIDEO
❌ READ_EXTERNAL_STORAGE

✔ Uses Android Photo Picker (recommended by Google)

📦 Installation
implementation 'com.ashu:ashu-image-picker:1.0.0'
⚙️ Initial Setup (VERY IMPORTANT)

You must initialize once in your Activity:

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    ImagePickerWithoutPermission.init(this); // ✅ REQUIRED
}
🟢 Step 1: Open Image Picker Dialog
binding.pickDocument.setOnClickListener(v -> {

    AppConstants.isDebug = true; // ✅ Enable debug logs

    ImagePicker.showPickImageDialog(
            TAG,
            MainActivity.this,
            AppConstants.IMAGE_REQUEST,
            R.color.black,
            new FileUtils.ResultCallback() {

                @Override
                public void onCameraSelected(boolean isCamera) {
                    isCameraSelected = isCamera; // Track selection
                }

                @Override
                public void onGallerySelected() {

                    // ✅ Gallery WITHOUT permission
                    ImagePickerWithoutPermission.pickImage(TAG, uri -> {

                        ImageProcessingUtils.handleGalleryFromUri(
                                TAG,
                                MainActivity.this,
                                uri,
                                binding.image,
                                true, // ✅ Enable crop if needed
                                null,
                                selectedImageData -> {

                                    binding.selectedDocument.setText(
                                            "Selected Document:\n" +
                                                    FileUtils.getFileNameFromPath(
                                                            MainActivity.this,
                                                            selectedImageData.getFilePath()
                                                    )
                                    );
                                }
                        );
                    });
                }
            }
    );
});
🟡 Step 2: Handle Result (Camera + Crop)
@Override
public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == AppConstants.IMAGE_REQUEST && resultCode == RESULT_OK) {

        // ✅ Camera Image Handling
        ImageProcessingUtils.handleCameraImage(
                TAG,
                this,
                FileUtils.getImagePath(this),
                binding.image,
                true, // ✅ Enable crop here
                null,
                selectedImageData -> {

                    binding.selectedDocument.setText(
                            "Selected Document:\n" +
                                    FileUtils.getFileNameFromPath(
                                            this,
                                            selectedImageData.getFilePath()
                                    )
                    );
                }
        );

    } else if (requestCode == AppConstants.IMAGE_CROP_REQUEST && resultCode == RESULT_OK) {

        // ✅ Cropped Image Handling
        ImageProcessingUtils.handleCroppedImage(
                TAG,
                this,
                data,
                binding.image,
                null,
                selectedImageData -> {

                    binding.selectedDocument.setText(
                            "Selected Document:\n" +
                                    FileUtils.getFileNameFromPath(
                                            this,
                                            selectedImageData.getFilePath()
                                    )
                    );
                }
        );
    }
}
✂️ Important: Crop Feature

To enable crop:

true  // cropImage parameter

Example:

ImageProcessingUtils.handleCameraImage(..., true, ...)
ImageProcessingUtils.handleGalleryFromUri(..., true, ...)

👉 If false → No crop dialog
👉 If true → Crop dialog will appear

🖼️ Important: Gallery Handling

There are 2 different ways to handle gallery:

✅ Without Permission (Recommended)
ImagePickerWithoutPermission.pickImage(TAG, uri -> {
    ImageProcessingUtils.handleGalleryFromUri(...);
});

✔ Play Store Safe
✔ No permission required

⚠️ With Intent (Old Method)
ImageProcessingUtils.handleGalleryFromIntent(...)

👉 Use only if you are handling Intent data manually

🔄 Complete Flow
User Click
   ↓
Dialog (Camera / Gallery)
   ↓
Camera → File Path
Gallery → URI
   ↓
Processing (Compression)
   ↓
Optional Crop
   ↓
Final Result (File + Bitmap + Base64)
📊 Result Data

You receive CompressFileData:

selectedImageData.getFilePath();
selectedImageData.getBitmapFormat();
selectedImageData.getString64BaseFormat();
selectedImageData.getFileFormat();
🐞 Debug Mode

Enable logs:

AppConstants.isDebug = true;

Logs include:

✔ Camera flow 📸
✔ Gallery URI 🖼️
✔ Compression 📉
✔ Crop ✂️

⚠️ Important Notes

✔ Must call init() before using gallery picker
✔ Camera permission is required
✔ No storage permission required
✔ Works best for single image

🎯 Use Cases

✔ Profile Image Upload
✔ Document Upload
✔ KYC Apps
✔ Chat Attachments
✔ Forms

❌ Not Suitable For

❌ Gallery Apps
❌ File Managers
❌ Bulk Image Access

🧠 Interview Line

“This library uses Android Photo Picker instead of storage permissions to comply with Google Play policies.”

🙌 Author

Ashu (Android Developer)
