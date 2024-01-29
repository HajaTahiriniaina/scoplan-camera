package scoplan.camera;

import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraMetadata;
import android.os.Build;
import android.util.Size;
import android.view.SurfaceView;
import android.view.TextureView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CameraUtils {
    /** Return the biggest preview size available which is smaller than the window */
    private static Size findBestPreviewSize(Size windowSize, CameraCharacteristics characteristics) {
        List<Size> supportedPreviewSizes = new ArrayList<>();
        Size[] sizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                .getOutputSizes(SurfaceTexture.class);
        if(sizes != null) {
            for(Size a : sizes) {
                if(windowSize.getHeight() * windowSize.getWidth() - a.getWidth() * a.getHeight() > 0) {
                    supportedPreviewSizes.add(a);
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                supportedPreviewSizes.sort(new SizeComparator());
            }
        }
        return supportedPreviewSizes.size() > 0 ? supportedPreviewSizes.get(0) : new Size(0, 0);
    }

    public static SurfaceTexture buildTargetTexture(TextureView textureView, CameraCharacteristics characteristics, int surfaceRotation) {
        int surfaceRotationDegrees = surfaceRotation * 90;
        Size windowSize = new Size(textureView.getWidth(), textureView.getHeight());
        Size previewSize = CameraUtils.findBestPreviewSize(windowSize, characteristics);
        int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        boolean isRotationRequired = CameraUtils.computeRelativeRotation(characteristics, surfaceRotationDegrees) % 180 != 0;
        float scaleX = 1f;
        float scaleY = 1f;
        if(sensorOrientation == 0) {
            scaleX = isRotationRequired ? windowSize.getWidth() / previewSize.getWidth() : windowSize.getWidth() / previewSize.getHeight();
            scaleY = isRotationRequired ? windowSize.getHeight() / previewSize.getHeight() : windowSize.getHeight() / previewSize.getWidth();
        } else {
            scaleX = isRotationRequired ? windowSize.getWidth() / previewSize.getHeight() : windowSize.getWidth() / previewSize.getWidth();
            scaleY = isRotationRequired ? windowSize.getHeight() / previewSize.getWidth() : windowSize.getHeight() / previewSize.getHeight();
        }
        float finalScale = Math.max(scaleX, scaleY);
        float halfWidth = windowSize.getWidth() / 2f;
        float halfHeight = windowSize.getHeight() / 2f;
        Matrix matrix = new Matrix();
        if(isRotationRequired) {
            matrix.setScale(
            1/ scaleX * finalScale,
            1/ scaleY * finalScale,
                halfWidth,
                halfHeight
            );
        } else {
            matrix.setScale(
                    windowSize.getHeight() / windowSize.getWidth() / scaleY * finalScale,
                    windowSize.getWidth() / windowSize.getHeight() / scaleX * finalScale,
                    halfWidth,
                    halfHeight
            );
        }
        // Rotate to compensate display rotation
        matrix.postRotate(
            -surfaceRotationDegrees,
            halfWidth,
            halfHeight
        );

        textureView.setTransform(matrix);

        SurfaceTexture texture = textureView.getSurfaceTexture();
        assert  texture != null;
        texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());

        return texture;
    }

    public static int computeRelativeRotation(CameraCharacteristics cameraCharacteristics, int deviceOrientationDegrees) {
        int sensorOrientationDegrees = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        int sign = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT ? 1 : -1;
        return (sensorOrientationDegrees - (deviceOrientationDegrees * sign) + 360) % 360;
    }
}
class SizeComparator implements Comparator<Size> {
    @Override
    public int compare(Size a, Size b) {
        return b.getHeight() * b.getWidth() - a.getWidth() * a.getHeight();
    }
}
