package scoplan.camera;

import android.content.Context;
import android.os.Bundle;
import com.dsphotoeditor.sdk.activity.DsPhotoEditorDrawActivity;

public class CDrawActivity extends DsPhotoEditorDrawActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Context context = getApplicationContext();
        int themeId = context.getResources().getIdentifier("AppTheme.NoActionBar", "style", context.getPackageName());
        setTheme(themeId);
        super.onCreate(savedInstanceState);
    }
}
