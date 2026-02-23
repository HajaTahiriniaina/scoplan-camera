package scoplan.camera;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.dsphotoeditor.sdk.utils.DsPhotoEditorConstants;
import com.google.common.util.concurrent.ListenableFuture;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.ZoomState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.lifecycle.LiveData;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import androidx.core.view.WindowInsetsCompat;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.sentry.Sentry;

public class CameraFragment extends Fragment implements scoplan.camera.OnImageCaptureListener, View.OnClickListener {
    public static String SCOPLAN_TAG = "SCOPLAN_TAG";
    private scoplan.camera.FakeR fakeR;
    private ImageButton camButton;
    private SeekBar zoomBar;
    private Button validationButton;
    private ProgressBar progressBar;
    private PreviewView previewView;
    private View cameraTopBar;
    private ImageView souche;
    private Button drawOn2;
    private ImageButton drawOn;
    private ImageButton cancelBtn;
    private Button cancelBtn2;
    private ImageButton flashBtn;
    private OrientationEventListener orientationEventListener;
    private int CAMERA_REQUEST_PERMISSION = 5000;
    private scoplan.camera.CameraSeekBarListener cameraSeekBarListener;
    private List<String> pictures = new ArrayList<String>();
    private int pictureCount = 0;
    private int photoLimit = 15;
    private ActivityResultLauncher<Intent> activityResultLauncher;
    private static CameraEventListener sCameraEventListener;
    private CameraEventListener cameraEventListener;
    private int currentOrientation = -1;
    private boolean flashOn = false;
    private LinearLayout cameraFrameLayout;

    // CameraX
    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private ScaleGestureDetector scaleGestureDetector;
    private Button zoom05xBtn;
    private Button zoom1xBtn;
    private Button zoom2xBtn;
    private boolean hasUltraWide = false;
    private boolean usingUltraWide = false;
    private CameraSelector ultraWideSelector;
    private CameraSelector mainSelector;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Apply window insets to root view to handle system bars
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());

            // Apply padding to root view
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);

            return androidx.core.view.WindowInsetsCompat.CONSUMED;
        });
    }

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
        sCameraEventListener = cameraEventListener; // Keep static reference for fragment recreation
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

        // Restore listener if fragment was recreated by the system
        if (this.cameraEventListener == null && sCameraEventListener != null) {
            this.cameraEventListener = sCameraEventListener;
        }

        cameraExecutor = Executors.newSingleThreadExecutor();

        this.orientationEventListener = new OrientationEventListener(this.getContext(), SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int i) {
                if(i != OrientationEventListener.ORIENTATION_UNKNOWN) {
                    if(i >= 315 || i <= 45) {
                        currentOrientation = 0; // Portrait
                    } else if(i > 45 && i <= 135) {
                        currentOrientation = 270;
                    } else if(i > 135 && i <= 225) {
                        currentOrientation = 180;
                    } else {
                        currentOrientation = 90;
                    }
                }
            }
        };

        this.fakeR = new FakeR(requireActivity());
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
        previewView = view.findViewById(this.fakeR.getId("cameraView"));
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);

        // Pinch-to-zoom — exploits full device zoom range (e.g. x10, x30)
        scaleGestureDetector = new ScaleGestureDetector(requireContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (camera == null) return true;
                ZoomState zoomState = camera.getCameraInfo().getZoomState().getValue();
                if (zoomState == null) return true;
                float currentZoomRatio = zoomState.getZoomRatio();
                float delta = detector.getScaleFactor();
                float newRatio = currentZoomRatio * delta;
                // Clamp to device min/max
                newRatio = Math.max(zoomState.getMinZoomRatio(), Math.min(zoomState.getMaxZoomRatio(), newRatio));
                camera.getCameraControl().setZoomRatio(newRatio);
                updateZoomButtonHighlights();
                return true;
            }
        });
        previewView.setOnTouchListener((v, event) -> {
            scaleGestureDetector.onTouchEvent(event);
            return true;
        });

        drawOn2 = view.findViewById(this.fakeR.getId("draw_on_2"));
        drawOn = view.findViewById(this.fakeR.getId("draw_on"));
        souche = view.findViewById(this.fakeR.getId("image_souche"));
        cameraTopBar = view.findViewById(this.fakeR.getId("cameraTopBar"));
        cancelBtn = view.findViewById(this.fakeR.getId("cancel"));
        cancelBtn2 = view.findViewById(this.fakeR.getId("cancel_btn"));
        flashBtn = view.findViewById(this.fakeR.getId("flash_btn"));
        zoom05xBtn = view.findViewById(this.fakeR.getId("zoom_05x"));
        zoom1xBtn = view.findViewById(this.fakeR.getId("zoom_1x"));
        zoom2xBtn = view.findViewById(this.fakeR.getId("zoom_2x"));

        camButton.setOnClickListener(this);
        drawOn2.setOnClickListener(this);
        drawOn.setOnClickListener(this);
        souche.setOnClickListener(this);
        cancelBtn.setOnClickListener(this);
        cancelBtn2.setOnClickListener(this);
        validationButton.setOnClickListener(this);
        flashBtn.setOnClickListener(this);
        zoom05xBtn.setOnClickListener(this);
        zoom1xBtn.setOnClickListener(this);
        zoom2xBtn.setOnClickListener(this);
        this.defineViewVisibility();

        return view;
    }

    private void startCamera() {
        if (getContext() == null) return;

        // Check permission first
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{
                Manifest.permission.CAMERA,
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ? Manifest.permission.WRITE_EXTERNAL_STORAGE : Manifest.permission.READ_MEDIA_IMAGES
            }, CAMERA_REQUEST_PERMISSION);
            return;
        }

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
            ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (Exception e) {
                Log.e(SCOPLAN_TAG, "Error starting camera", e);
                Sentry.captureException(e);
                failedCapture();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindCameraUseCases() {
        bindCameraWithSelector(false);
    }

    private void bindCameraWithSelector(boolean useUltraWide) {
        if (cameraProvider == null || !isAdded()) return;

        // Unbind all before rebinding
        cameraProvider.unbindAll();

        // Preview
        Preview preview = new Preview.Builder().build();

        // ImageCapture
        imageCapture = new ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setJpegQuality(65)
            .build();

        // Detect ultra-wide camera availability
        detectUltraWideCamera();

        // Select camera
        CameraSelector cameraSelector;
        if (useUltraWide && hasUltraWide && ultraWideSelector != null) {
            cameraSelector = ultraWideSelector;
            usingUltraWide = true;
        } else {
            cameraSelector = mainSelector != null ? mainSelector : CameraSelector.DEFAULT_BACK_CAMERA;
            usingUltraWide = false;
        }

        try {
            // Bind use cases to lifecycle
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            // Connect preview to PreviewView
            preview.setSurfaceProvider(previewView.getSurfaceProvider());

            // Update zoom button highlights
            updateZoomButtonHighlights();
        } catch (Exception e) {
            Log.e(SCOPLAN_TAG, "Error binding camera use cases", e);
            Sentry.captureException(e);
            failedCapture();
        }
    }

    private void detectUltraWideCamera() {
        if (cameraProvider == null) return;
        try {
            // Main camera (default back) — typically lens facing back with normal focal length
            mainSelector = CameraSelector.DEFAULT_BACK_CAMERA;

            // Try to find ultra-wide: a back camera that is NOT the default
            // CameraX on devices with multiple back cameras can select via CameraSelector
            // We check if there's a camera with LENS_FACING_BACK that has a shorter focal length
            List<CameraInfo> availableCameras = cameraProvider.getAvailableCameraInfos();
            hasUltraWide = false;

            for (CameraInfo cameraInfo : availableCameras) {
                try {
                    // Check if this is a back-facing camera
                    CameraSelector testSelector = cameraInfo.getCameraSelector();
                    // Try to see if it's different from default back camera
                    // Ultra-wide cameras have intrinsicZoomRatio < 1.0
                    LiveData<ZoomState> zoomStateLiveData = cameraInfo.getZoomState();
                    ZoomState zoomState = zoomStateLiveData.getValue();
                    if (zoomState != null) {
                        float minZoom = zoomState.getMinZoomRatio();
                        // On most devices, the ultra-wide has a minZoomRatio < 1.0
                        // or the intrinsic zoom ratio is < 1.0
                        // We check all cameras and find one with smaller minimum zoom
                        if (minZoom < 0.9f) {
                            // This camera likely supports ultra-wide range natively
                            hasUltraWide = true;
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            // If device supports zoom ratio < 1.0 on main camera, it means the main camera
            // can zoom out to ultra-wide natively (logical multi-camera)
            try {
                Camera testCamera = cameraProvider.bindToLifecycle(this, mainSelector);
                ZoomState zoomState = testCamera.getCameraInfo().getZoomState().getValue();
                if (zoomState != null && zoomState.getMinZoomRatio() < 0.9f) {
                    hasUltraWide = true;
                }
                cameraProvider.unbindAll();
            } catch (Exception ignored) {
                // If we can't test, just continue without ultra-wide
            }

            // Show/hide 0.5x button
            if (hasUltraWide) {
                zoom05xBtn.setVisibility(View.VISIBLE);
            } else {
                zoom05xBtn.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.w(SCOPLAN_TAG, "Error detecting ultra-wide camera", e);
            hasUltraWide = false;
            zoom05xBtn.setVisibility(View.GONE);
        }
    }

    private void updateZoomButtonHighlights() {
        // Reset all to white
        zoom05xBtn.setTextColor(Color.WHITE);
        zoom1xBtn.setTextColor(Color.WHITE);
        zoom2xBtn.setTextColor(Color.WHITE);

        if (camera == null) return;
        ZoomState zoomState = camera.getCameraInfo().getZoomState().getValue();
        if (zoomState == null) return;

        float currentRatio = zoomState.getZoomRatio();

        if (usingUltraWide || currentRatio < 0.9f) {
            zoom05xBtn.setTextColor(Color.parseColor("#FFD700"));
        } else if (currentRatio < 1.5f) {
            zoom1xBtn.setTextColor(Color.parseColor("#FFD700"));
        } else {
            zoom2xBtn.setTextColor(Color.parseColor("#FFD700"));
        }
    }

    private void setZoomRatio(float ratio) {
        if (camera == null) return;
        ZoomState zoomState = camera.getCameraInfo().getZoomState().getValue();
        if (zoomState == null) return;

        float minRatio = zoomState.getMinZoomRatio();
        float maxRatio = zoomState.getMaxZoomRatio();

        // Clamp to device limits
        float targetRatio = Math.max(minRatio, Math.min(maxRatio, ratio));
        camera.getCameraControl().setZoomRatio(targetRatio);
        updateZoomButtonHighlights();
    }

    private int getTargetRotation() {
        switch (currentOrientation) {
            case 0: return Surface.ROTATION_0;
            case 90: return Surface.ROTATION_90;
            case 180: return Surface.ROTATION_180;
            case 270: return Surface.ROTATION_270;
            default: return Surface.ROTATION_0;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        startCamera();
        this.orientationEventListener.enable();

        try {
            Window window = requireActivity().getWindow();
            // Enable edge-to-edge mode but keep system bars visible
            WindowCompat.setDecorFitsSystemWindows(window, false);

            WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());

            if (controller != null) {
                controller.show(WindowInsetsCompat.Type.systemBars());
            }
        } catch (Exception e) {
            Log.w(SCOPLAN_TAG, "Edge-to-edge not supported on this device", e);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        this.orientationEventListener.disable();

        try {
            Window window = requireActivity().getWindow();
            // Restore normal window mode when leaving camera
            WindowCompat.setDecorFitsSystemWindows(window, true);

            WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());

            if (controller != null) {
                controller.show(WindowInsetsCompat.Type.systemBars());
            }
        } catch (Exception e) {
            Log.w(SCOPLAN_TAG, "Edge-to-edge restore not supported on this device", e);
        }

        // CameraX unbinds automatically via lifecycle, but we can explicitly unbind
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            camera = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        // Clear static listener reference to avoid memory leak
        sCameraEventListener = null;
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
            Toast toast = Toast.makeText(this.getContext(), "La prise de photos est limit\u00e9e \u00e0 " + this.photoLimit + " par envoi", Toast.LENGTH_SHORT);
            toast.show();
            return;
        }
        if (imageCapture == null) {
            return;
        }
        this.pictureCount++;
        camButton.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = "IMG_"+ timeStamp + ".jpg";

            // Use app-internal cache dir as fallback if getExternalFilesDir returns null
            File picturesDir = this.getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (picturesDir == null) {
                picturesDir = new File(this.getContext().getCacheDir(), "Pictures");
                picturesDir.mkdirs();
            }
            File photoFile = new File(picturesDir, fileName);

            ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

            // Set target rotation based on device orientation
            imageCapture.setTargetRotation(getTargetRotation());

            // Set flash mode
            imageCapture.setFlashMode(flashOn ? ImageCapture.FLASH_MODE_ON : ImageCapture.FLASH_MODE_OFF);

            final File finalPhotoFile = photoFile;
            imageCapture.takePicture(outputOptions, cameraExecutor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults results) {
                        try {
                            // Decode with subsampling to avoid OutOfMemoryError on thumbnails
                            BitmapFactory.Options opts = new BitmapFactory.Options();
                            opts.inJustDecodeBounds = true;
                            BitmapFactory.decodeFile(finalPhotoFile.getAbsolutePath(), opts);

                            // Calculate inSampleSize for ~512px thumbnail
                            int maxDim = Math.max(opts.outWidth, opts.outHeight);
                            int inSampleSize = 1;
                            while (maxDim / inSampleSize > 1024) {
                                inSampleSize *= 2;
                            }

                            BitmapFactory.Options decodeOpts = new BitmapFactory.Options();
                            decodeOpts.inSampleSize = inSampleSize;
                            Bitmap bitmap = BitmapFactory.decodeFile(finalPhotoFile.getAbsolutePath(), decodeOpts);

                            if (bitmap != null) {
                                onImageCapture(finalPhotoFile, bitmap);
                            } else {
                                onImageBuildFailed(new IOException("Failed to decode captured image"));
                            }
                        } catch (OutOfMemoryError oom) {
                            Log.e(SCOPLAN_TAG, "OOM decoding thumbnail", oom);
                            // Still report success with null bitmap — file is saved
                            onImageCapture(finalPhotoFile, null);
                        } catch (Exception e) {
                            onImageBuildFailed(e);
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(SCOPLAN_TAG, "Photo capture failed: " + exception.getMessage(), exception);
                        Sentry.captureException(exception);
                        pictureCount--;
                        failedCapture();
                    }
                });
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
                    if (bitmap != null) {
                        souche.setImageBitmap(bitmap);
                    }
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
            Toast.makeText(getContext(), "Oups! une erreur pendant la capture. Veuillez r\u00e9\u00e9ssayer.", Toast.LENGTH_LONG).show();
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

    /**
     * Downscale an image file if it exceeds MAX_EDITOR_PIXELS to prevent
     * "Canvas: trying to draw too large bitmap" in PhotoEditorActivity.
     * The resized image replaces the original file and the path in pictures list is updated.
     */
    private static final int MAX_EDITOR_DIMENSION = 4096;

    private String downscaleForEditor(String imagePath) {
        try {
            // Decode bounds only
            BitmapFactory.Options boundsOpts = new BitmapFactory.Options();
            boundsOpts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imagePath, boundsOpts);

            int origW = boundsOpts.outWidth;
            int origH = boundsOpts.outHeight;

            // If image is within limits, no need to resize
            if (origW <= MAX_EDITOR_DIMENSION && origH <= MAX_EDITOR_DIMENSION) {
                return imagePath;
            }

            // Calculate inSampleSize for initial downsampling
            int inSampleSize = 1;
            while (origW / inSampleSize > MAX_EDITOR_DIMENSION || origH / inSampleSize > MAX_EDITOR_DIMENSION) {
                inSampleSize *= 2;
            }

            BitmapFactory.Options decodeOpts = new BitmapFactory.Options();
            decodeOpts.inSampleSize = inSampleSize;
            Bitmap sampled = BitmapFactory.decodeFile(imagePath, decodeOpts);

            if (sampled == null) {
                return imagePath;
            }

            // Fine-scale to fit exactly within MAX_EDITOR_DIMENSION
            int sampledW = sampled.getWidth();
            int sampledH = sampled.getHeight();
            Bitmap finalBitmap;
            if (sampledW > MAX_EDITOR_DIMENSION || sampledH > MAX_EDITOR_DIMENSION) {
                float scale = Math.min(
                    (float) MAX_EDITOR_DIMENSION / sampledW,
                    (float) MAX_EDITOR_DIMENSION / sampledH
                );
                int targetW = Math.round(sampledW * scale);
                int targetH = Math.round(sampledH * scale);
                finalBitmap = Bitmap.createScaledBitmap(sampled, targetW, targetH, true);
                if (finalBitmap != sampled) {
                    sampled.recycle();
                }
            } else {
                finalBitmap = sampled;
            }

            // Write resized image to a new file (keep original intact for final result)
            File originalFile = new File(imagePath);
            File resizedFile = new File(originalFile.getParent(),
                "editor_" + originalFile.getName());
            OutputStream out = new FileOutputStream(resizedFile);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
            finalBitmap.recycle();

            return resizedFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e(SCOPLAN_TAG, "Error downscaling image for editor", e);
            Sentry.captureException(e);
            return imagePath; // Fallback: use original
        } catch (OutOfMemoryError oom) {
            Log.e(SCOPLAN_TAG, "OOM downscaling image for editor", oom);
            return imagePath; // Fallback: use original
        }
    }

    private void startDrawing() {
        Intent dsPhotoEditorIntent = new Intent(getContext(), PhotoEditorActivity.class);
        String pc = pictures.get(pictures.size() - 1);
        // Downscale image to avoid "Canvas: trying to draw too large" crash
        String editorPath = downscaleForEditor(pc);
        dsPhotoEditorIntent.setData(Uri.fromFile(new File(editorPath)));
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
            if (this.cameraEventListener != null) {
                this.cameraEventListener.onUserValid(this.pictures);
            }
            this.pictures = new ArrayList<>();
        } else if(
            view.getId() == this.fakeR.getId("flash_btn")
        ) {
            this.flashOn = !this.flashOn;
            this.flashBtn.setImageResource(this.fakeR.getDrawable(flashOn ? "flash_on" : "flash"));
        } else if(view.getId() == this.fakeR.getId("zoom_05x")) {
            // 0.5x — use min zoom ratio (ultra-wide range)
            if (camera != null) {
                ZoomState zoomState = camera.getCameraInfo().getZoomState().getValue();
                if (zoomState != null) {
                    setZoomRatio(zoomState.getMinZoomRatio());
                }
            }
        } else if(view.getId() == this.fakeR.getId("zoom_1x")) {
            setZoomRatio(1.0f);
        } else if(view.getId() == this.fakeR.getId("zoom_2x")) {
            setZoomRatio(2.0f);
        }
    }

    private void cancelTakePhoto() {
        if (pictures.isEmpty() && cameraEventListener != null) {
            // No photos taken, just close without confirmation
            cameraEventListener.onUserCancel();
            return;
        }
        AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
        alert.setMessage("Voulez-vous sortir sans enregistrer la photo")
            .setPositiveButton("Oui", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (cameraEventListener != null) {
                        cameraEventListener.onUserCancel();
                    }
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
