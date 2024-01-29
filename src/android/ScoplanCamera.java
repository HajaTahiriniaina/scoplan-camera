package scoplan.camera;

import android.content.pm.ActivityInfo;
import android.widget.FrameLayout;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;

import scoplan.camera.CameraEventListener;
import scoplan.camera.CameraFragment;

/**
 * This class echoes a string called from JavaScript.
 */
public class ScoplanCamera extends CordovaPlugin implements CameraEventListener {
    private int containerViewId = 505;

    private CallbackContext callbackContext;
    private CameraFragment cameraFragment;

    private FrameLayout containerView;
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        if (action.equals("takePictures")) {
            this.takePictures();
            return true;
        }
        return false;
    }

    private void takePictures() {
        cordova.getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        CameraEventListener cameraEventListener = this;
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                containerView = (FrameLayout)cordova.getActivity().findViewById(containerViewId);
                if(containerView == null){
                    containerView = new FrameLayout(cordova.getActivity().getApplicationContext());
                    containerView.setId(containerViewId);
                    FrameLayout.LayoutParams containerLayoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
                    cordova.getActivity().addContentView(containerView, containerLayoutParams);
                    //add the fragment to the container
                }
                containerView.setClickable(true);
                cameraFragment = new CameraFragment();
                cameraFragment.setCameraEventListener(cameraEventListener);
                FragmentManager manager = cordova.getActivity().getSupportFragmentManager();
                FragmentTransaction transaction = manager.beginTransaction();
                transaction.add(containerViewId, cameraFragment);
                transaction.commit();
            }
        });
    }

    @Override
    public void onUserCancel() {
        callbackContext.success(new JSONArray());
        closeCamera();
    }

    @Override
    public void onUserValid(List<String> pictures) {
        JSONArray array = new JSONArray();
        for(String picture : pictures) {
            array.put(picture);
        }
        callbackContext.success(array);
        closeCamera();
    }

    public void closeCamera() {
        if(containerView != null) {
            containerView.setClickable(false);
        }
        FragmentManager manager = cordova.getActivity().getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        if(cameraFragment != null) {
            transaction.remove(cameraFragment);
        }
        transaction.commit();
        cordova.getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }
}