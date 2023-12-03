package scoplan.camera;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.hardware.display.DisplayManager;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;

import com.dsphotoeditor.sdk.utils.DsPhotoEditorConstants;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import scoplan.camera.CameraUtils;
import scoplan.camera.PhotoEditorActivity;
import scoplan.camera.CameraEventListener;
import scoplan.camera.FakeR;

public class CameraFragment extends Fragment implements scoplan.camera.OnImageCaptureListener, View.OnClickListener {
    public static String SCOPLAN_TAG = "SCOPLAN_TAG";
    private scoplan.camera.FakeR fakeR;
    private ImageButton camButton;
    private SeekBar zoomBar;
    private Button validationButton;
    private ProgressBar progressBar;
    private TextureView textureView;
    private CameraDevice cameraDevice;
    private View cameraTopBar;
    private ImageView souche;
    private Button drawOn2;
    private ImageButton drawOn;
    private ImageButton cancelBtn;
    private Button cancelBtn2;
    private ImageButton flashBtn;
    private DisplayManager displayManager;
    private OrientationEventListener orientationEventListener;
    private CameraCaptureSession cameraCaptureSessions;
    private CaptureRequest.Builder captureRequestBuilder;
    private String cameraId;
    private int CAMERA_REQUEST_PERMISSION = 5000;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private scoplan.camera.CameraSeekBarListener cameraSeekBarListener;
    private CameraManager manager;
    private List<String> pictures = new ArrayList<String>();
    private ActivityResultLauncher<Intent> activityResultLauncher;
    private CameraEventListener cameraEventListener;
    private int currentOrientation = -1;
    private boolean flashOn = false;
    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
            createCameraPreview();
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }
    };

    CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    public CameraFragment() {
        // Required empty public constructor
        activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                photoEditorCallBack(result);
            }
        );
    }

    public void setCameraEventListener(CameraEventListener cameraEventListener) {
        this.cameraEventListener = cameraEventListener;
    }

    private void photoEditorCallBack(ActivityResult result) {
        if(result.getData() != null) {
            Uri outputUri = result.getData().getData();
            if(outputUri != null && outputUri.compareTo(Uri.parse("remove")) == 0){
                pictures.remove(pictures.size() - 1);

            } else if(outputUri != null){
                pictures.set(pictures.size() - 1, outputUri.getPath());
            }
            defineViewVisibility();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        manager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);

        displayManager = (DisplayManager) getActivity().getSystemService(Context.DISPLAY_SERVICE);
        this.orientationEventListener = new OrientationEventListener(this.getContext(), SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int i) {
                if(i != OrientationEventListener.ORIENTATION_UNKNOWN) {
                    if(i >= 315 || i <= 45) {
                        currentOrientation = 90; // Portrait
                    } else if(i > 45 && i <= 135) {
                        currentOrientation = 180;
                    } else if(i > 135 && i <= 225) {
                        currentOrientation = 270;
                    } else {
                        currentOrientation = 0;
                    }
                }
            }
        };

        this.fakeR = new FakeR(getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(this.fakeR.getLayout("fragment_camera"), container, false);
        camButton = view.findViewById(this.fakeR.getId("button_capture"));
        zoomBar = view.findViewById(this.fakeR.getId("camera_zoom"));
        validationButton = view.findViewById(this.fakeR.getId("valid_btn"));
        progressBar = view.findViewById(this.fakeR.getId("progressBar"));
        textureView = view.findViewById(this.fakeR.getId("cameraView"));
        drawOn2 = view.findViewById(this.fakeR.getId("draw_on_2"));
        drawOn = view.findViewById(this.fakeR.getId("draw_on"));
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);
        souche = view.findViewById(this.fakeR.getId("image_souche"));
        cameraTopBar = view.findViewById(this.fakeR.getId("cameraTopBar"));
        cancelBtn = view.findViewById(this.fakeR.getId("cancel"));
        cancelBtn2 = view.findViewById(this.fakeR.getId("cancel_btn"));
        flashBtn = view.findViewById(this.fakeR.getId("flash_btn"));

        camButton.setOnClickListener(this);
        drawOn2.setOnClickListener(this);
        drawOn.setOnClickListener(this);
        souche.setOnClickListener(this);
        cancelBtn.setOnClickListener(this);
        cancelBtn2.setOnClickListener(this);
        validationButton.setOnClickListener(this);
        flashBtn.setOnClickListener(this);
        this.defineViewVisibility();
        return view;
    }

    private void createCameraPreview() {
        if(cameraDevice == null || !textureView.isAvailable())
            return;
        try{
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            Surface surface = new Surface(CameraUtils.buildTargetTexture(textureView, characteristics, displayManager.getDisplay(Display.DEFAULT_DISPLAY).getRotation()));
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if(cameraDevice == null)
                        return;
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Log.e(SCOPLAN_TAG, "Something failed");
                }
            },null);
        } catch (CameraAccessException e) {
            Log.e(SCOPLAN_TAG, "Error", e);
        }
    }

    private void openCamera() {
        try{
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            //Check realtime permission if run higher API 23
            if(ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(getActivity(), new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, CAMERA_REQUEST_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback,null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        if(cameraDevice == null)
            Log.e(SCOPLAN_TAG, "Error camera");
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE,CaptureRequest.CONTROL_MODE_AUTO);
        try{
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(),null, mBackgroundHandler);
            this.cameraSeekBarListener = new scoplan.camera.CameraSeekBarListener(cameraId, manager, zoomBar, captureRequestBuilder, cameraCaptureSessions, mBackgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(SCOPLAN_TAG, "Error", e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();
        if(textureView.isAvailable())
            openCamera();
        else
            textureView.setSurfaceTextureListener(textureListener);
        this.orientationEventListener.enable();
    }

    @Override
    public void onPause() {
        stopBackgroundThread();
        super.onPause();
        this.orientationEventListener.disable();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //displayManager.unregisterDisplayListener(displayListener);
        release();
    }

    private void release() {
        if(cameraDevice != null) {
            cameraDevice.close();
        }
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try{
            mBackgroundThread.join();
            mBackgroundThread= null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void takePicture() {
        if(cameraDevice == null) {
            return;
        }
        camButton.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            Size[] jpegSizes = null;
            if(characteristics != null)
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    .getOutputSizes(ImageFormat.JPEG);
            //Capture image with custom size
            int width = 640;
            int height = 480;
            if(jpegSizes != null && jpegSizes.length > 0)
            {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            final ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG,1);
            List<Surface> outputSurface = new ArrayList<>(2);
            outputSurface.add(reader.getSurface());
            outputSurface.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            captureBuilder.set(CaptureRequest.FLASH_MODE, flashOn ? CaptureRequest.FLASH_MODE_TORCH : CaptureRequest.FLASH_MODE_OFF);
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = "IMG_"+ timeStamp + ".jpg";
            File file = new File( this.getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName);
            ImageReader.OnImageAvailableListener readerListener = new scoplan.camera.ImageCameraAvailableListener(file, reader, this, currentOrientation);

            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    createCameraPreview();
                }
            };

            cameraDevice.createCaptureSession(outputSurface, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    try{
                        cameraCaptureSession.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        Log.e(SCOPLAN_TAG, "Error", e);
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                }
            }, mBackgroundHandler);

        } catch (CameraAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onImageCapture(File file, Bitmap bitmap) {
        pictures.add(file.getAbsolutePath());
        getActivity().runOnUiThread(() -> {
            souche.setImageBitmap(bitmap);
            defineViewVisibility();
        });
    }

    public void defineViewVisibility() {
        if(pictures.size() > 0) {
            souche.setVisibility(View.VISIBLE);
            cameraTopBar.setVisibility(View.VISIBLE);
            validationButton.setVisibility(View.VISIBLE);
            cancelBtn2.setVisibility(View.GONE);
        } else {
            souche.setVisibility(View.GONE);
            cameraTopBar.setVisibility(View.GONE);
            validationButton.setVisibility(View.GONE);
            cancelBtn2.setVisibility(View.VISIBLE);
        }
        camButton.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }

    private void startDrawing() {
        Intent dsPhotoEditorIntent = new Intent(getContext(), PhotoEditorActivity.class);
        String pc = pictures.get(pictures.size() - 1);
        dsPhotoEditorIntent.setData(Uri.fromFile(new File(pc)));
        int[] toolsToHide = {
            PhotoEditorActivity.TOOL_ORIENTATION,
            PhotoEditorActivity.TOOL_FRAME,
            PhotoEditorActivity.TOOL_FILTER,
            PhotoEditorActivity.TOOL_ROUND,
            PhotoEditorActivity.TOOL_EXPOSURE,
            PhotoEditorActivity.TOOL_CONTRAST,
            PhotoEditorActivity.TOOL_VIGNETTE,
            PhotoEditorActivity.TOOL_SATURATION,
            PhotoEditorActivity.TOOL_SHARPNESS,
            PhotoEditorActivity.TOOL_WARMTH,
            PhotoEditorActivity.TOOL_PIXELATE,
            PhotoEditorActivity.TOOL_STICKER
        };
        dsPhotoEditorIntent.putExtra(DsPhotoEditorConstants.DS_PHOTO_EDITOR_TOOLS_TO_HIDE, toolsToHide);
        activityResultLauncher.launch(dsPhotoEditorIntent);
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == this.fakeR.getId("button_capture")) {
            this.takePicture();
        } else if(
            view.getId() == this.fakeR.getId("draw_on_2") ||
                view.getId() == this.fakeR.getId("image_souche") ||
                view.getId() == this.fakeR.getId("draw_on")
        ) {
            this.startDrawing();
        } else if(
            view.getId() == this.fakeR.getId("cancel") ||
                view.getId() == this.fakeR.getId("cancel_btn")
        ) {
            this.cancelTakePhoto();
            this.pictures = new ArrayList<>();
        } else if(
            view.getId() == this.fakeR.getId("valid_btn")
        ) {
            this.cameraEventListener.onUserValid(this.pictures);
            this.pictures = new ArrayList<>();
        } else if(
            view.getId() == this.fakeR.getId("flash_btn")
        ) {
            this.flashOn = !this.flashOn;
            this.flashBtn.setImageResource(this.fakeR.getDrawable(flashOn ? "flash_on" : "flash"));
        }
    }

    private void cancelTakePhoto() {
        AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
        alert.setMessage("Voulez-vous sortir sans enregistrer la photo")
            .setPositiveButton("Oui", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    cameraEventListener.onUserCancel();
                }
            })
            .setNegativeButton("Non", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                }
            });
        alert.create().show();
    }
}
