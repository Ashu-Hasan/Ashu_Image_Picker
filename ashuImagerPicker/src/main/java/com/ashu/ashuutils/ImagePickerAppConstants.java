package com.ashu.ashuutils;

/**
 * Central place for all constant values used across the library.
 *
 * This class:
 * ✔ Prevents magic numbers
 * ✔ Improves maintainability
 * ✔ Provides debug control
 */
public final class ImagePickerAppConstants {

    private ImagePickerAppConstants() {
        // Prevent instantiation
    }

    /**
     * Request code for image picking (Camera / Gallery)
     */
    public static final int IMAGE_REQUEST = 100;

    /**
     * ⚠️ Deprecated (Not required for modern Android Photo Picker)
     */
    @Deprecated
    public static final int STORAGE_PERMISSION_REQUEST_CODE = 102;

    /**
     * Request code for CAMERA permission
     */
    public static final int CAMERA_PERMISSION_REQUEST_CODE = 103;

    /**
     * Request code for image cropping result
     */
    public static final int IMAGE_CROP_REQUEST = 104;

    /**
     * Global debug flag
     *
     * ✔ true  → Enable logs & debug toasts
     * ✔ false → Disable logs (recommended for production)
     *
     * Usage:
     * AppConstants.isDebug = true;
     */
    public static boolean isDebug = false;
}
