package scoplan.camera;

import java.util.List;

public interface CameraEventListener {
    public void onUserCancel();
    public void onUserValid(List<String> pictures);
}
