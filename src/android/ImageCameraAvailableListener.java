package scoplan.camera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.Image;
import android.media.ImageReader;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class ImageCameraAvailableListener implements ImageReader.OnImageAvailableListener {
    private ImageReader reader;
    private File file;
    private scoplan.camera.OnImageCaptureListener imageCaptureListener;
    private float rotation;

    public ImageCameraAvailableListener(File file, ImageReader reader, scoplan.camera.OnImageCaptureListener onImageCaptureListener, float rotation) {
        this.reader = reader;
        this.file = file;
        this.imageCaptureListener = onImageCaptureListener;
        this.rotation = rotation;
    }
    @Override
    public void onImageAvailable(ImageReader imageReader) {
        Image image = null;
        try{
            image = reader.acquireLatestImage();
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.capacity()];
            buffer.get(bytes);
            this.imageCaptureListener.onImageCapture(file, save(bytes));
        }
        catch (IOException e)
        {
            e.printStackTrace();
            this.imageCaptureListener.onImageBuildFailed(e);
        }
        finally {
            {
                if(image != null)
                    image.close();
            }
        }
    }

    private Bitmap save(byte[] bytes) throws IOException {
        OutputStream outputStream = null;
        try{
            outputStream = new FileOutputStream(file);
            Bitmap bmp = BitmapFactory.decodeByteArray(bytes,0, bytes.length);
            Matrix matrix = new Matrix();
            matrix.setRotate(this.rotation);
            Bitmap rotatedBitmap = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 65, outputStream);
            return rotatedBitmap;
        } finally {
            if(outputStream != null)
                outputStream.close();
        }
    }
}
