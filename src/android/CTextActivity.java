package scoplan.camera;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.dsphotoeditor.sdk.activity.DsPhotoEditorTextActivity;

public class CTextActivity extends DsPhotoEditorTextActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Context context = getApplicationContext();
        int themeId = context.getResources().getIdentifier("AppTheme.NoActionBar", "style", context.getPackageName());
        setTheme(themeId);
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        View rootView = findViewById(com.dsphotoeditor.sdk.R.id.ds_photo_editor_text_sticker_root_layout);
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
                return WindowInsetsCompat.CONSUMED;
            });
        }
    }
}

