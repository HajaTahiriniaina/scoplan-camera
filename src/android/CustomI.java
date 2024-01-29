package scoplan.camera;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import com.dsphotoeditor.sdk.ui.stickerview.i;

import java.util.ArrayList;

public class CustomI extends i {
    public CustomI(@NonNull Context context) {
        super(context);
    }
    public static ArrayList<Drawable> drawables;
    private int index = 0;
    @Override
    @NonNull
    public i a(@ColorInt int var1) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            drawables.get(this.index).setTint(var1);
        }
        return super.a(var1);
    }

    @Override
    public i a(@NonNull Drawable var1) {
        if(drawables == null)
            drawables = new ArrayList<Drawable>();
        this.drawables.add(var1);
        this.index = this.drawables.size() - 1;
        return super.a(var1);
    }
}
