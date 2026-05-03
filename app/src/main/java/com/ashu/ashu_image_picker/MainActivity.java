package com.ashu.ashu_image_picker;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ashu.ashu_image_picker.databinding.ActivityMainBinding;
import com.ashu.ashuutils.ImagePickerAppConstants;
import com.ashu.ashuutils.fileUtils.FileUtils;
import com.ashu.ashuutils.fileUtils.image.ImagePicker;
import com.ashu.ashuutils.fileUtils.image.ImagePickerWithoutPermission;
import com.ashu.ashuutils.fileUtils.image.ImageProcessingUtils;
import com.ashu.ashuutils.models.CompressFileData;

public class MainActivity extends AppCompatActivity {

    String TAG = "MainActivityData";
    ActivityMainBinding binding;
    boolean isCameraSelected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ImagePickerWithoutPermission.init(this);


        binding.pickDocument.setOnClickListener(v -> {
            ImagePickerAppConstants.isDebug = true;
            ImagePicker.showPickImageDialog(TAG, MainActivity.this, ImagePickerAppConstants.IMAGE_REQUEST, R.color.black, new FileUtils.ResultCallback() {
                @Override
                public void onCameraSelected(boolean isCamera) {
                    isCameraSelected = isCamera;
                }

                @SuppressLint("SetTextI18n")
                @Override
                public void onGallerySelected() {
                    ImagePickerWithoutPermission.pickImage(TAG, uri -> {
                        ImageProcessingUtils.handleGalleryFromUri(
                                "MainActivityData",
                                MainActivity.this,
                                uri,
                                binding.image,
                                true,
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
            });
        });

    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ImagePickerAppConstants.IMAGE_REQUEST && resultCode == RESULT_OK) {

            if (isCameraSelected){
                ImageProcessingUtils.handleCameraImage("MainActivityData", MainActivity.this, FileUtils.getImagePath(MainActivity.this), binding.image, true,null, new FileUtils.FileCallback() {
                    @Override
                    public void onFileReady(CompressFileData selectedImageData) {

                        binding.selectedDocument.setText("Selected Document:\n" + FileUtils.getFileNameFromPath(MainActivity.this, selectedImageData.getFilePath()));

                    }
                });
            }
            else {
                ImageProcessingUtils.handleGalleryFromIntent("MainActivityData", MainActivity.this, data, binding.image, true, null, new FileUtils.FileCallback() {
                    @Override
                    public void onFileReady(CompressFileData selectedImageData) {

                        binding.selectedDocument.setText("Selected Document:\n" + FileUtils.getFileNameFromPath(MainActivity.this, selectedImageData.getFilePath()));

                    }
                });
            }
        } else if (requestCode == ImagePickerAppConstants.IMAGE_CROP_REQUEST && resultCode == RESULT_OK) {

            ImageProcessingUtils.handleCroppedImage("MainActivityData", MainActivity.this, data, binding.image, null, new FileUtils.FileCallback() {
                @Override
                public void onFileReady(CompressFileData selectedImageData) {

                    binding.selectedDocument.setText("Selected Document:\n" + FileUtils.getFileNameFromPath(MainActivity.this, selectedImageData.getFilePath()));

                }
            });
        }
    }
}