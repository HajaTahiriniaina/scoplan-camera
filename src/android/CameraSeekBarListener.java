package scoplan.camera;

import android.util.Log;
import android.widget.SeekBar;

import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;

public class CameraSeekBarListener implements SeekBar.OnSeekBarChangeListener {
    private Camera camera;
    private SeekBar seekBar;

    public CameraSeekBarListener(Camera camera, SeekBar seekBar) {
        this.camera = camera;
        this.seekBar = seekBar;
        seekBar.setMax(100);
        seekBar.setProgress(0);
        seekBar.setOnSeekBarChangeListener(this);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (camera == null) return;
        try {
            // setLinearZoom takes a value from 0.0 (no zoom) to 1.0 (max zoom)
            camera.getCameraControl().setLinearZoom(progress / 100f);
        } catch (Exception e) {
            Log.e(CameraFragment.SCOPLAN_TAG, "Error setting zoom: ", e);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }
}
