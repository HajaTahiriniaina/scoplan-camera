package scoplan.camera;

import java.util.List;

public interface CameraEventListener {
    public void onUserCancel();
    public void onUserValid(List<String> pictures);
    /** L'utilisateur veut annoter la photo : la caméra est mise en pause et l'éditeur web prend le relais. */
    public void onEditRequest(String path, int index);
}
