package scoplan.camera;

import android.graphics.Bitmap;

import java.io.File;

public interface OnImageCaptureListener {
    public void onImageCapture(File file, Bitmap bitmap);

    public void onImageBuildFailed(Exception e);
}
