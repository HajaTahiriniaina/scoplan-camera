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
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
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
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
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
    private CameraEventListener cameraEventListener;
    private int currentOrientation = -1;
    private boolean flashOn = false;
    private LinearLayout cameraFrameLayout;

    // CameraX
    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;

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
        drawOn2 = view.findViewById(this.fakeR.getId("draw_on_2"));
        drawOn = view.findViewById(this.fakeR.getId("draw_on"));
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

        // Select back camera
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        try {
            // Bind use cases to lifecycle
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            // Connect preview to PreviewView
            preview.setSurfaceProvider(previewView.getSurfaceProvider());

            // Setup zoom
            setupZoom();
        } catch (Exception e) {
            Log.e(SCOPLAN_TAG, "Error binding camera use cases", e);
            Sentry.captureException(e);
            failedCapture();
        }
    }

    private void setupZoom() {
        if (camera == null) return;
        cameraSeekBarListener = new scoplan.camera.CameraSeekBarListener(camera, zoomBar);
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
            File photoFile = new File(this.getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName);

            ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

            // Set target rotation based on device orientation
            imageCapture.setTargetRotation(getTargetRotation());

            // Set flash mode
            imageCapture.setFlashMode(flashOn ? ImageCapture.FLASH_MODE_ON : ImageCapture.FLASH_MODE_OFF);

            imageCapture.takePicture(outputOptions, cameraExecutor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults results) {
                        // Read the saved file to create thumbnail bitmap
                        try {
                            Bitmap bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                            if (bitmap != null) {
                                onImageCapture(photoFile, bitmap);
                            } else {
                                onImageBuildFailed(new IOException("Failed to decode captured image"));
                            }
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
