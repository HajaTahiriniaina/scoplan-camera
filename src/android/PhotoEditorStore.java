package scoplan.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class PhotoEditorStore {
    public static File a(Context var0, Bitmap var1) {
        try {
            boolean var3 = a(var1);
            String var4 = var3 ? ".png" : ".jpg";

            String var5 = "photo_editor_ds_" + System.currentTimeMillis() + var4;
            File var8 = new File(var0.getExternalFilesDir(Environment.DIRECTORY_PICTURES), var5);
            FileOutputStream var9 = new FileOutputStream(var8);
            if (var3) {
                var1.compress(Bitmap.CompressFormat.PNG, 100, var9);
            } else {
                var1.compress(Bitmap.CompressFormat.JPEG, 100, var9);
            }

            var9.flush();
            var9.close();
            if (var3) {
                MediaScannerConnection.scanFile(var0, new String[]{var8.getAbsolutePath()}, new String[]{"image/png"}, (MediaScannerConnection.OnScanCompletedListener)null);
            } else {
                MediaScannerConnection.scanFile(var0, new String[]{var8.getAbsolutePath()}, new String[]{"image/jpeg"}, (MediaScannerConnection.OnScanCompletedListener)null);
            }

            return var8;
        } catch (FileNotFoundException var10) {
            var10.printStackTrace();
            return null;
        } catch (IOException var11) {
            var11.printStackTrace();
            return null;
        }
    }
    private static boolean a(Bitmap var0) {
        int var1 = var0.getPixel(0, 0);
        return Color.alpha(var1) == 0;
    }
}
