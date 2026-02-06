package scoplan.camera;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.dsphotoeditor.sdk.utils.DsPhotoEditorConstants;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import capacitor.cordova.android.plugins.R;
import io.sentry.Sentry;

public class CameraFragment extends Fragment implements scoplan.camera.OnImageCaptureListener, View.OnClickListener {
    public static String SCOPLAN_TAG = "SCOPLAN_TAG";
    private scoplan.camera.FakeR fakeR;
    private ImageButton camButton;
    private SeekBar zoomBar;
    private Button validationButton;
    private ProgressBar progressBar;
    private SurfaceView surfaceView;
    private CameraDevice cameraDevice;
    private View cameraTopBar;
    private ImageView souche;
    private Button drawOn2;
    private ImageButton drawOn;
    private ImageButton cancelBtn;
    private Button cancelBtn2;
    private ImageButton flashBtn;
    private OrientationEventListener orientationEventListener;
    private CameraCaptureSession cameraCaptureSessions;
    private CaptureRequest.Builder captureRequestBuilder;
    private String cameraId = null;
    private int CAMERA_REQUEST_PERMISSION = 5000;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private scoplan.camera.CameraSeekBarListener cameraSeekBarListener;
    private CameraManager manager;
    private List<String> pictures = new ArrayList<String>();
    private int pictureCount = 0;
    private int photoLimit = 15;
    private ActivityResultLauncher<Intent> activityResultLauncher;
    private CameraEventListener cameraEventListener;
    private int currentOrientation = -1;
    private boolean flashOn = false;
    private boolean cameraIsOpen = false;
    private boolean previewReady = false;
    private SurfaceHolder mSurfaceHolder;

    private boolean surfaceAvailable = false;
    private boolean surfaceSizeConfigured = false;
    private Size optimalPreviewSize = null;
    private LinearLayout cameraFrameLayout;
    private boolean callbackAdded = false;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!callbackAdded) {
            mSurfaceHolder.addCallback(surfaceHolderCallBack);
            callbackAdded = true;
        }

        // Apply window insets to root view to handle system bars
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());

            // Apply padding to root view
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);

            return androidx.core.view.WindowInsetsCompat.CONSUMED;
        });
    }

    private final SurfaceHolder.Callback surfaceHolderCallBack = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
            surfaceAvailable = true;
            if(!cameraIsOpen) {
                openCamera();
            }
        }

        @Override
        public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {
            createCameraPreview();
        }

        @Override
        public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
            surfaceAvailable = false;
            surfaceSizeConfigured = false;
            release();
        }
    };

    CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            cameraIsOpen = true;
            // Configure surface size first; surfaceChanged will trigger createCameraPreview
            configureSurfaceSize();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraIsOpen = false;
            previewReady = false;
            surfaceSizeConfigured = false;
            release();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(SCOPLAN_TAG, "CameraDevice.StateCallback onError: " + error);
            cameraIsOpen = false;
            previewReady = false;
            surfaceSizeConfigured = false;
            release();
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

        this.fakeR = new FakeR(requireActivity());
    }

    private int findStyleResId(Context context, String styleName) {
        // Convert dotted names (AppTheme.NoActionBar) to compiled names (AppTheme_NoActionBar)
        String compiledName = styleName.replace('.', '_');

        int resId = context.getResources().getIdentifier(
            compiledName,
            "style",
            context.getPackageName()
        );

        return resId;
    }

    @Override
    public View onCreateView(LayoutInflater outerInflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        int themeId = getActivity().getResources().getIdentifier("AppTheme.fullscreen", "style", getActivity().getPackageName());
        ContextThemeWrapper themeContext = new ContextThemeWrapper(requireActivity(), themeId);
        LayoutInflater inflater = outerInflater.cloneInContext(themeContext);
        View view =  inflater.inflate(this.fakeR.getLayout("fragment_camera"), container, false);
        camButton = view.findViewById(this.fakeR.getId("button_capture"));
        zoomBar = view.findViewById(this.fakeR.getId("camera_zoom"));
        validationButton = view.findViewById(this.fakeR.getId("valid_btn"));
        progressBar = view.findViewById(this.fakeR.getId("progressBar"));
        cameraFrameLayout = view.findViewById(this.fakeR.getId("camera_frame_layout"));
        cameraFrameLayout.setBackgroundColor(Color.rgb(0, 0, 0));
        surfaceView = view.findViewById(this.fakeR.getId("cameraView"));
        drawOn2 = view.findViewById(this.fakeR.getId("draw_on_2"));
        drawOn = view.findViewById(this.fakeR.getId("draw_on"));
        assert surfaceView != null;
        mSurfaceHolder = surfaceView.getHolder();
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

    private void configureSurfaceSize() {
        if (cameraId == null || getActivity() == null) {
            // Can't configure yet, fall back to direct preview
            surfaceSizeConfigured = true;
            createCameraPreview();
            return;
        }
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map != null) {
                Size[] outputSizes = map.getOutputSizes(SurfaceTexture.class);
                optimalPreviewSize = selectedPreviewSize(outputSizes);
                if (optimalPreviewSize != null && cameraFrameLayout.getWidth() > 0) {
                    Size displaySize = calculateSurfaceSize(optimalPreviewSize);
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded()) return;
                        ViewGroup.LayoutParams layoutParams = surfaceView.getLayoutParams();
                        layoutParams.width = displaySize.getWidth();
                        layoutParams.height = displaySize.getHeight();
                        surfaceView.setLayoutParams(layoutParams);
                        // Set buffer to camera native resolution
                        mSurfaceHolder.setFixedSize(optimalPreviewSize.getWidth(), optimalPreviewSize.getHeight());
                        // surfaceChanged will be called → createCameraPreview
                        surfaceSizeConfigured = true;
                    });
                    return;
                }
            }
        } catch (Exception e) {
            Log.e(SCOPLAN_TAG, "Error configuring surface size", e);
        }
        // Fallback: proceed directly
        surfaceSizeConfigured = true;
        createCameraPreview();
    }

    private void createCameraPreview() {
        if(cameraDevice == null || !surfaceAvailable || previewReady || !surfaceSizeConfigured)
            return;
        if(!cameraIsOpen) {
            this.openCamera();
            return;
        }
        previewReady = true;
        try{
            // Close any existing session before creating a new one
            closeSession();

            Surface surface = mSurfaceHolder.getSurface();
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if(cameraDevice == null) {
                        previewReady = false;
                        return;
                    }
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Log.e(SCOPLAN_TAG, "onConfigureFailed for preview session");
                    Sentry.captureMessage("onConfigureFailed: " + cameraCaptureSession.toString());
                    previewReady = false;
                    failedCapture();
                }
            }, mBackgroundHandler);
        } catch (Exception e) {
            Log.e(SCOPLAN_TAG, "Error creating camera preview", e);
            Sentry.captureException(e);
            previewReady = false;
            this.failedCapture();
        }
    }

    private void openCamera() {
        try{
            if (cameraId != null ) {
                return;
            }
            for (String id : manager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics =
                    manager.getCameraCharacteristics(id);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                    CameraCharacteristics.LENS_FACING_BACK) {
                    cameraId = id;
                    break;
                }
            }
            // Fallback to first camera only if no back camera was found
            if (cameraId == null) {
                cameraId = manager.getCameraIdList()[0];
            }
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            //Check realtime permission if run higher API 23
            if(ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(getActivity(), new String[]{
                    Manifest.permission.CAMERA,
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ? Manifest.permission.WRITE_EXTERNAL_STORAGE : Manifest.permission.READ_MEDIA_IMAGES
                }, CAMERA_REQUEST_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback,null);
        } catch (Exception e) {
            Sentry.captureException(e);
            this.failedCapture();
        }
    }

    private Size chooseOptimalSize(Size[] sizes, int desiredWidth, int desiredHeight) {
        // Choose the smallest size that is at least as large as the desired size
        for (Size size : sizes) {
            if (size.getWidth() >= desiredWidth && size.getHeight() >= desiredHeight) {
                return size;
            }
        }

        // If no size is large enough, choose the largest available size
        return sizes[sizes.length - 1];
    }

    private void updatePreview() {
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
            this.cameraSeekBarListener = new scoplan.camera.CameraSeekBarListener(cameraId, manager, zoomBar, captureRequestBuilder, cameraCaptureSessions, mBackgroundHandler);
        } catch (Exception e) {
            Sentry.captureException(e);
            this.failedCapture();
        }
    }

    private Size calculateSurfaceSize(Size selectedSize) {
        int parentWidth = this.cameraFrameLayout.getWidth();
        int parentHeight = this.cameraFrameLayout.getHeight();

        // Camera sensor is landscape, but we display in portrait → swap W/H
        float previewAspectRatio = (float) selectedSize.getHeight() / selectedSize.getWidth();

        // Center-crop: fill the entire parent, crop overflow (no distortion)
        int surfaceWidth, surfaceHeight;
        int fitWidth = parentWidth;
        int fitHeight = (int) (parentWidth / previewAspectRatio);

        if (fitHeight >= parentHeight) {
            // Fit by width already fills the height — use it
            surfaceWidth = fitWidth;
            surfaceHeight = fitHeight;
        } else {
            // Fit by height and let width overflow
            surfaceHeight = parentHeight;
            surfaceWidth = (int) (parentHeight * previewAspectRatio);
        }

        return new Size(surfaceWidth, surfaceHeight);
    }

    private Size selectedPreviewSize(Size[] outputSizes) {
        // Target 4:3 aspect ratio (landscape sensor: width > height, so 4/3 ≈ 1.333)
        float targetAspectRatio = 4f / 3f;
        Size selectedSize = null;
        int bestArea = 0;

        for (Size size : outputSizes) {
            float aspectRatio = (float) size.getWidth() / size.getHeight();
            if (Math.abs(aspectRatio - targetAspectRatio) < 0.05) {
                // Among matching ratios, pick the largest for best quality
                int area = size.getWidth() * size.getHeight();
                if (area > bestArea) {
                    bestArea = area;
                    selectedSize = size;
                }
            }
        }

        // Fallback: if no 4:3, pick the largest size available
        if (selectedSize == null && outputSizes.length > 0) {
            selectedSize = outputSizes[0];
            for (Size size : outputSizes) {
                if (size.getWidth() * size.getHeight() > selectedSize.getWidth() * selectedSize.getHeight()) {
                    selectedSize = size;
                }
            }
        }
        return selectedSize;
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();
        if(surfaceAvailable && !cameraIsOpen) {
            openCamera();
        }
        this.orientationEventListener.enable();

        Window window = requireActivity().getWindow();

        // Enable edge-to-edge mode but keep system bars visible
        WindowCompat.setDecorFitsSystemWindows(window, false);

        WindowInsetsControllerCompat controller =
            WindowCompat.getInsetsController(window, window.getDecorView());

        if (controller != null) {
            // Show status and navigation bars
            controller.show(WindowInsets.Type.systemBars());
        }
    }

    @Override
    public void onPause() {
        stopBackgroundThread();
        super.onPause();
        this.orientationEventListener.disable();
        Window window = requireActivity().getWindow();

        // Restore normal window mode when leaving camera
        WindowCompat.setDecorFitsSystemWindows(window, true);

        WindowInsetsControllerCompat controller =
            WindowCompat.getInsetsController(window, window.getDecorView());

        if (controller != null) {
            controller.show(WindowInsets.Type.systemBars());
        }
        release();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        release();
    }

    private void closeSession() {
        if(cameraCaptureSessions != null) {
            try {
                cameraCaptureSessions.close();
            } catch (Exception e) {
                Log.e(SCOPLAN_TAG, "Error closing capture session", e);
            }
            cameraCaptureSessions = null;
        }
    }

    private void release() {
        closeSession();
        if(cameraDevice != null) {
            try {
                cameraDevice.close();
            } catch (Exception e) {
                Log.e(SCOPLAN_TAG, "Error closing camera device", e);
            }
            cameraIsOpen = false;
            previewReady = false;
            cameraDevice = null;
            cameraId = null;
        }
    }

    private void stopBackgroundThread() {
        if (mBackgroundThread == null) {
            return;
        }
        mBackgroundThread.quitSafely();
        try{
            mBackgroundThread.join();
        } catch (InterruptedException e) {
            Log.e(SCOPLAN_TAG, "Error stopping background thread", e);
        }
        mBackgroundThread = null;
        mBackgroundHandler = null;
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    public void setPhotoLimit(int limit){
        this.photoLimit = limit;
    }

    public void setCurrentCount(int currentCount){
        this.pictureCount = currentCount;
    }

    private void takePicture() {
        if(getContext() == null) {
            return;
        }
        if(this.pictureCount >= this.photoLimit){
            Toast toast = Toast.makeText(this.getContext(), "La prise de photos est limitée à " + this.photoLimit + " par envoi", Toast.LENGTH_SHORT);
            toast.show();
            return;
        }
        if(this.mBackgroundThread == null || !this.mBackgroundThread.isAlive()) {
            this.startBackgroundThread();
        }
        if(cameraDevice == null || cameraId == null) {
            return;
        }
        this.pictureCount++;
        camButton.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            Size[] jpegSizes = null;
            if(characteristics != null) {
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map != null) {
                    jpegSizes = map.getOutputSizes(ImageFormat.JPEG);
                }
            }
            //Capture image with custom size
            int width = 640;
            int height = 480;
            if(jpegSizes != null && jpegSizes.length > 0)
            {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            final ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurface = new ArrayList<>(2);
            outputSurface.add(reader.getSurface());
            outputSurface.add(mSurfaceHolder.getSurface());
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            captureBuilder.set(CaptureRequest.FLASH_MODE, flashOn ? CaptureRequest.FLASH_MODE_TORCH : CaptureRequest.FLASH_MODE_OFF);
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = "IMG_"+ timeStamp + ".jpg";
            File file = new File(this.getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName);
            ImageReader.OnImageAvailableListener readerListener = new scoplan.camera.ImageCameraAvailableListener(file, reader, this, currentOrientation);

            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    reader.close();
                    previewReady = false;
                    createCameraPreview();
                }

                @Override
                public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
                    super.onCaptureFailed(session, request, failure);
                    reader.close();
                    Sentry.captureMessage("Failed capture _" + failure.getReason());
                    Log.e(SCOPLAN_TAG, "Error FAILED Capture " + failure.getReason());
                    pictureCount--;
                    failedCapture();
                }
            };

            cameraDevice.createCaptureSession(outputSurface, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    try{
                        cameraCaptureSession.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (Exception e) {
                        Log.e(SCOPLAN_TAG, "Error - " + e.getMessage());
                        Sentry.captureException(e);
                        reader.close();
                        pictureCount--;
                        failedCapture();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Log.e(SCOPLAN_TAG, "Error FAILED Configuration for capture session");
                    Sentry.captureMessage("Failed configure capture session");
                    reader.close();
                    pictureCount--;
                    failedCapture();
                }
            }, mBackgroundHandler);

        } catch (Exception e) {
            Log.e(SCOPLAN_TAG, "Error in takePicture", e);
            Sentry.captureException(e);
            pictureCount--;
            failedCapture();
        }
    }

    @Override
    public void onImageCapture(File file, Bitmap bitmap) {
        pictures.add(file.getAbsolutePath());
        if (getActivity() != null && isAdded()) {
            getActivity().runOnUiThread(() -> {
                if (isAdded()) {
                    souche.setImageBitmap(bitmap);
                    defineViewVisibility();
                }
            });
        }
    }

    @Override
    public void onImageBuildFailed(Exception e) {
        Sentry.captureException(e);
        pictureCount--;
        failedCapture();
    }

    public void failedCapture() {
        if (getActivity() == null || !isAdded()) {
            return;
        }
        getActivity().runOnUiThread(() -> {
            if (!isAdded() || getContext() == null) {
                return;
            }
            Toast.makeText(getContext(), "Oups! une erreur pendant la capture. Veuillez rééssayer.", Toast.LENGTH_LONG).show();
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
