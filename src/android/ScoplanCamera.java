package scoplan.camera;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.widget.FrameLayout;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * This class echoes a string called from JavaScript.
 */
public class ScoplanCamera extends CordovaPlugin implements CameraEventListener {
    private int containerViewId = 505;

    /** Callback de takePictures : reste ouvert pendant toute la session caméra (événements « edit »). */
    private CallbackContext callbackContext;
    private CameraFragment cameraFragment;
    /** Fermeture laissée au web (closeCamera) : il affiche sa page derrière la caméra avant qu'elle disparaisse. */
    private boolean deferClose = false;

    private FrameLayout containerView;
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("takePictures")) {
            this.callbackContext = callbackContext;
            int limit = args.getJSONObject(0).getInt("photoLimit");
            int currentCount = args.getJSONObject(1).getInt("currentCount");
            JSONObject options = args.optJSONObject(2);
            this.deferClose = options != null && options.optBoolean("deferClose", false);
            this.takePictures(limit, currentCount);
            return true;
        }
        if (action.equals("closeCamera")) {
            cordova.getActivity().runOnUiThread(() -> {
                closeCamera();
                callbackContext.success();
            });
            return true;
        }
        if (action.equals("resumeCamera")) {
            JSONObject options = args.optJSONObject(0);
            int index = options != null ? options.optInt("index", -1) : -1;
            String path = options != null && options.has("path") && !options.isNull("path") ? options.getString("path") : null;
            boolean removed = options != null && options.optBoolean("removed", false);
            this.resumeCamera(index, path, removed, callbackContext);
            return true;
        }
        if (action.equals("pauseCamera")) {
            this.pauseCamera(callbackContext);
            return true;
        }
        return false;
    }

    /**
     * Demande d'édition : la caméra reste affichée, le web ouvre son éditeur derrière elle
     * puis appelle pauseCamera ; le callback de takePictures reste ouvert.
     */
    @Override
    public void onEditRequest(String path, int index) {
        cordova.getActivity().runOnUiThread(() -> {
            if (cameraFragment == null || callbackContext == null) {
                return;
            }
            try {
                JSONObject event = new JSONObject();
                event.put("type", "edit");
                event.put("path", path);
                event.put("index", index);
                PluginResult result = new PluginResult(PluginResult.Status.OK, event);
                result.setKeepCallback(true);
                callbackContext.sendPluginResult(result);
            } catch (JSONException e) {
                // Impossible d'ouvrir l'éditeur : la caméra est restée affichée, rien à faire.
            }
        });
    }

    /** Éditeur web affiché : masque la caméra pour le révéler. */
    private void pauseCamera(CallbackContext context) {
        cordova.getActivity().runOnUiThread(() -> {
            if (cameraFragment != null) {
                // Le conteneur de la caméra intercepte les touchers : l'éditeur web doit les recevoir.
                if (containerView != null) {
                    containerView.setClickable(false);
                }
                FragmentManager manager = cordova.getActivity().getSupportFragmentManager();
                manager.beginTransaction().hide(cameraFragment).commitNowAllowingStateLoss();
            }
            context.success();
        });
    }

    /**
     * Retour de l'éditeur web : applique la photo annotée (ou la retire) et réaffiche la caméra.
     * Le succès n'est envoyé qu'une fois la caméra réaffichée : le web peut alors fermer son éditeur.
     */
    private void resumeCamera(int index, String path, boolean removed, CallbackContext context) {
        cordova.getActivity().runOnUiThread(() -> {
            if (cameraFragment == null) {
                context.success();
                return;
            }
            String filePath = path;
            if (filePath != null && filePath.startsWith("file://")) {
                filePath = android.net.Uri.parse(filePath).getPath();
            }
            cameraFragment.applyEdit(index, filePath, removed);
            if (containerView != null) {
                containerView.setClickable(true);
            }
            FragmentManager manager = cordova.getActivity().getSupportFragmentManager();
            manager.beginTransaction().show(cameraFragment).commitNowAllowingStateLoss();
            context.success();
        });
    }

    private void takePictures(int limit, int currentCount) {
        cordova.getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        scoplan.camera.CameraEventListener cameraEventListener = this;
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                containerView = (FrameLayout)cordova.getActivity().findViewById(containerViewId);
                if(containerView == null){
                    Context context = cordova.getActivity();
                    containerView = new FrameLayout(context);
                    containerView.setId(containerViewId);
                    FrameLayout.LayoutParams containerLayoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
                    cordova.getActivity().addContentView(containerView, containerLayoutParams);
                    //add the fragment to the container
                }
                containerView.setClickable(true);
                cameraFragment = new scoplan.camera.CameraFragment();
                if(currentCount > 0){
                    cameraFragment.setCurrentCount(currentCount);
                }
                cameraFragment.setPhotoLimit(limit);
                cameraFragment.setCameraEventListener(cameraEventListener);
                FragmentManager manager = cordova.getActivity().getSupportFragmentManager();
                FragmentTransaction transaction = manager.beginTransaction();
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                transaction.replace(android.R.id.content, cameraFragment); // use root content
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });
    }

    @Override
    public void onUserCancel() {
        if (callbackContext == null) {
            return;
        }
        callbackContext.success(new JSONArray());
        callbackContext = null;
        if (!deferClose) {
            closeCamera();
        }
    }

    @Override
    public void onUserValid(List<String> pictures) {
        if (callbackContext == null) {
            return;
        }
        JSONArray array = new JSONArray();
        for(String picture : pictures) {
            array.put(picture);
        }
        callbackContext.success(array);
        callbackContext = null;
        if (!deferClose) {
            closeCamera();
        }
    }

    public void closeCamera() {
        deferClose = false;
        if (cameraFragment == null) {
            return;
        }
        if(containerView != null) {
            containerView.setClickable(false);
        }
        FragmentManager manager = cordova.getActivity().getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        if(cameraFragment != null) {
            transaction.remove(cameraFragment);
        }
        transaction.commit();
        cameraFragment = null;
        cordova.getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }
}
