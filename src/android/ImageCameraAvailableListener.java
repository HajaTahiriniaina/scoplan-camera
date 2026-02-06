package scoplan.camera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.Image;
import android.media.ImageReader;

import java.io.File;
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
            if (image == null) {
                this.imageCaptureListener.onImageBuildFailed(new IOException("Failed to acquire image"));
                return;
            }
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.capacity()];
            buffer.get(bytes);
            this.imageCaptureListener.onImageCapture(file, save(bytes));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            this.imageCaptureListener.onImageBuildFailed(e);
        }
        finally {
            if(image != null)
                image.close();
        }
    }

    private Bitmap save(byte[] bytes) throws IOException {
        OutputStream outputStream = null;
        Bitmap bmp = null;
        try{
            outputStream = new FileOutputStream(file);
            bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (bmp == null) {
                throw new IOException("Failed to decode image bytes");
            }
            Matrix matrix = new Matrix();
            matrix.setRotate(this.rotation);
            Bitmap rotatedBitmap = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 65, outputStream);
            // Recycle original bitmap only if rotation created a new one
            if (rotatedBitmap != bmp) {
                bmp.recycle();
            }
            return rotatedBitmap;
        } finally {
            if(outputStream != null)
                outputStream.close();
        }
    }
}
