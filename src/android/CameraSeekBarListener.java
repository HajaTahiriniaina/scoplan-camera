package scoplan.camera;

import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Handler;
import android.util.Log;
import android.widget.SeekBar;

public class CameraSeekBarListener implements SeekBar.OnSeekBarChangeListener {
    private String currentCameraId;
    private CameraManager cameraManager;
    private SeekBar seekBar;
    private CaptureRequest.Builder captureRequestBuilder;
    private CameraCaptureSession cameraCaptureSessions;
    private Handler mBackgroundHandler;
    private float minZoom = 0;
    private float maxZoom = 0;
    int zoomStep = 1;
    int zoomLevel = 0;

    public CameraSeekBarListener(
            String cameraId,
            CameraManager cameraManager,
            SeekBar seekBar,
            CaptureRequest.Builder captureRequestBuilder,
            CameraCaptureSession cameraCaptureSessions,
            Handler mBackgroundHandler
    ) {
        currentCameraId = cameraId;
        this.cameraManager = cameraManager;
        this.seekBar = seekBar;
        this.captureRequestBuilder = captureRequestBuilder;
        this.cameraCaptureSessions = cameraCaptureSessions;
        this.mBackgroundHandler = mBackgroundHandler;
        this.maxZoom = this.getMaxZoom();
        seekBar.setMax((int) maxZoom);
        seekBar.setOnSeekBarChangeListener(this);
    }
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
        setCurrentZoom(Math.round(minZoom + (progress * zoomStep)));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    private Rect getZoomRect(float zoomLevel) {
        try {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(this.currentCameraId);
            Rect activeRect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
            if((zoomLevel <= maxZoom) && (zoomLevel > 1)) {
                int minW = (int) (activeRect.width() / maxZoom);
                int minH = (int) (activeRect.height() / maxZoom);
                int difW = activeRect.width() - minW;
                int difH = activeRect.height() - minH;
                int cropW = difW / 100 * (int) zoomLevel;
                int cropH = difH / 100 * (int) zoomLevel;
                cropW -= cropW & 3;
                cropH -= cropH & 3;
                return new Rect(cropW, cropH, activeRect.width() - cropW, activeRect.height() - cropH);
            } else if(zoomLevel == 0){
                return new Rect(0, 0, activeRect.width(), activeRect.height());
            }
            return null;
        } catch (Exception e) {
            Log.e(scoplan.camera.CameraFragment.SCOPLAN_TAG, "Error during camera init");
            return null;
        }
    }

    public float getMaxZoom() {
        try {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(this.currentCameraId);
            return characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) * 10;
        } catch (CameraAccessException e) {
            Log.e(scoplan.camera.CameraFragment.SCOPLAN_TAG, "Error during camera get zoom", e);
        }
        return 0;
    }

    public void setCurrentZoom(float zoomLevel) {
        Rect zoomRect = getZoomRect(zoomLevel);
        if(zoomRect != null) {
            try {
                //you can try to add the synchronized object here
                captureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomRect);
                cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
            } catch (Exception e) {
                Log.e(scoplan.camera.CameraFragment.SCOPLAN_TAG, "Error updating preview: ", e);
            }
            this.zoomLevel = (int) zoomLevel;
        }
    }
}
