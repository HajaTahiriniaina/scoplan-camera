package scoplan.camera;

import androidx.core.content.ContextCompat;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Layout;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dsphotoeditor.sdk.activity.DsPhotoEditorActivity;
import com.dsphotoeditor.sdk.activity.DsPhotoEditorTextActivity;
import com.dsphotoeditor.sdk.ui.stickerview.StickerView;

import scoplan.camera.FakeR;
import scoplan.camera.CustomI;

public class PhotoEditorMesureCustomActivity extends DsPhotoEditorTextActivity implements View.OnClickListener {
    private FakeR fakeR;
    private StickerView a;
    private int b;
    private int c;
    private SparseArray<Integer> d;
    private int[] e = new int[]{-1, -16777216, -15924993, -16547841, -16580609, -5855578, -16471550, -8323327, -198, -32758, -65281, -8388353, -9136947, -65536, -65408};
    private ProgressBar f;
    private ImageButton g;
    private ImageButton h;

    public PhotoEditorMesureCustomActivity() {
    }

    @Override
    protected void onCreate(Bundle var1) {
        super.onCreate(var1);
        this.fakeR = new FakeR(this);
        this.setContentView(com.dsphotoeditor.sdk.R.layout.activity_ds_photo_editor_sticker_text);
        if (original != null && !original.isRecycled()) {
            this.a();
            this.b();
            this.c();
            RelativeLayout view = findViewById(fakeR.getId("ds_photo_editor_text_sticker_top_bar"));
            TextView textview = (TextView)view.getChildAt(1);
            textview.setText("Mesure");
        } else {
            Toast.makeText(this, this.getString(com.dsphotoeditor.sdk.R.string.ds_photo_editor_error_unknown), Toast.LENGTH_SHORT).show();
            this.finish();
        }
    }

    private void a() {
        this.g = (ImageButton)this.findViewById(com.dsphotoeditor.sdk.R.id.ds_photo_editor_text_sticker_top_button_apply);
        this.h = (ImageButton)this.findViewById(com.dsphotoeditor.sdk.R.id.ds_photo_editor_text_sticker_top_button_cancel);
        this.g.setOnClickListener(this);
        this.h.setOnClickListener(this);
        this.findViewById(com.dsphotoeditor.sdk.R.id.ds_photo_editor_text_sticker_add).setOnClickListener(this);
        LinearLayout var1 = (LinearLayout)this.findViewById(com.dsphotoeditor.sdk.R.id.ds_photo_editor_text_stickers_colors);

        int var2;
        for(var2 = 0; var2 < var1.getChildCount(); ++var2) {
            var1.getChildAt(var2).setOnClickListener(this);
        }

        this.d = new SparseArray();

        for(var2 = 0; var2 < var1.getChildCount(); ++var2) {
            this.d.put(var1.getChildAt(var2).getId(), this.e[var2]);
        }

        this.a = (StickerView)this.findViewById(com.dsphotoeditor.sdk.R.id.ds_photo_editor_text_sticker_view);
        if (original.getHeight() > original.getWidth()) {
            RelativeLayout.LayoutParams var4 = (RelativeLayout.LayoutParams)this.a.getLayoutParams();
            int var3 = (int)this.getResources().getDimension(com.dsphotoeditor.sdk.R.dimen.activity_ds_photo_editor_sticker_view_margin);
            var4.setMargins(var3, var3, var3, var3);
            this.a.setLayoutParams(var4);
        }

        this.f = (ProgressBar)this.findViewById(com.dsphotoeditor.sdk.R.id.ds_photo_editor_text_sticker_top_progress_bar);
    }

    private void b() {
        ImageView var1 = (ImageView)this.findViewById(com.dsphotoeditor.sdk.R.id.ds_photo_editor_text_sticker_image_view);
        var1.setImageBitmap(original);
        Bitmap var2 = ((BitmapDrawable)var1.getDrawable()).getBitmap();
        this.b = var2.getWidth();
        this.c = var2.getHeight();
    }

    private void c() {
        this.findViewById(com.dsphotoeditor.sdk.R.id.ds_photo_editor_text_sticker_root_layout).setBackgroundColor(com.dsphotoeditor.sdk.utils.a.b());
        this.findViewById(com.dsphotoeditor.sdk.R.id.ds_photo_editor_text_sticker_top_bar).setBackgroundColor(com.dsphotoeditor.sdk.utils.a.a());
        this.findViewById(com.dsphotoeditor.sdk.R.id.ds_photo_editor_text_stickers_bottom_bar).setBackgroundColor(com.dsphotoeditor.sdk.utils.a.a());
        this.g.setImageResource(com.dsphotoeditor.sdk.utils.a.w());
        this.h.setImageResource(com.dsphotoeditor.sdk.utils.a.x());
    }

    public void onClick(View var1) {
        int var2 = var1.getId();
        if (var2 == com.dsphotoeditor.sdk.R.id.ds_photo_editor_text_sticker_top_button_apply) {
            PhotoEditorMesureCustomActivity.a var3 = new PhotoEditorMesureCustomActivity.a();
            var3.execute(new Void[0]);
        } else if (var2 == com.dsphotoeditor.sdk.R.id.ds_photo_editor_text_sticker_top_button_cancel) {
            this.onBackPressed();
        } else if (var2 == com.dsphotoeditor.sdk.R.id.ds_photo_editor_text_sticker_add) {
            this.d();
        } else {
            int var5 = (Integer)this.d.get(var2, 0);
            CustomI var4 = (CustomI) this.a.getCurrentSticker();
            if (var4 != null && var5 != 0) {
                var4.a(var5);
                var4.a(Typeface.DEFAULT_BOLD);
                this.a.c(var4);
                this.a.invalidate();
            }
        }

    }

    private void d() {
        AlertDialog.Builder var1 = new AlertDialog.Builder(this);
        LayoutInflater var2 = this.getLayoutInflater();
        View var3 = var2.inflate(com.dsphotoeditor.sdk.R.layout.dialog_ds_photo_editor_text_sticker, (ViewGroup)null);
        final EditText var4 = (EditText)var3.findViewById(com.dsphotoeditor.sdk.R.id.ds_photo_editor_text_sticker_content);
        var4.setHint("Entrez votre texte (facultatif)");
        var1.setView(var3).setPositiveButton(this.getString(com.dsphotoeditor.sdk.R.string.ds_photo_editor_text_dialog_positive_button), new android.content.DialogInterface.OnClickListener() {
            public void onClick(DialogInterface var1, int var2) {
                String var3 = var4.getText().toString();
                if(var3.length() == 0){
                    var3 = " ";
                }
                if (var3.length() > 0) {
                    CustomI var4x = new CustomI(PhotoEditorMesureCustomActivity.this);
                    Drawable d = ContextCompat.getDrawable(PhotoEditorMesureCustomActivity.this, fakeR.getDrawable("mesure_bar"));
                    var4x.a(d.mutate());
                    var4x.a(var3);
                    var4x.a(-16777216);
                    var4x.a(Typeface.DEFAULT_BOLD);
                    var4x.a(Layout.Alignment.ALIGN_CENTER);
                    var4x.b();
                    PhotoEditorMesureCustomActivity.this.a.e(var4x);
                }

            }
        }).setNegativeButton(this.getString(com.dsphotoeditor.sdk.R.string.ds_photo_editor_text_dialog_negative_button), new android.content.DialogInterface.OnClickListener() {
            public void onClick(DialogInterface var1, int var2) {
            }
        });
        var1.create().show();
    }

    public void onBackPressed() {
        if (this.f != null && this.f.getVisibility() != View.VISIBLE) {
            Intent var1 = new Intent();
            this.setResult(0, var1);
            this.finish();
        }

    }

    private class a extends AsyncTask<Void, Void, Bitmap> {
        private Bitmap b;

        private a() {
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            Bitmap b = a(voids);
            return b;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            a(bitmap);
        }

        protected void onPreExecute() {
            super.onPreExecute();
            PhotoEditorMesureCustomActivity.this.f.setVisibility(View.VISIBLE);
            this.b = PhotoEditorMesureCustomActivity.this.a.f();
        }

        protected Bitmap a(Void... var1) {
            float var2 = 1.0F * (float)Math.min(PhotoEditorMesureCustomActivity.this.b, PhotoEditorMesureCustomActivity.this.c) / (float)Math.min(this.b.getWidth(), this.b.getHeight());
            Matrix var3 = new Matrix();
            var3.postScale(var2, var2);
            this.b = Bitmap.createBitmap(this.b, 0, 0, this.b.getWidth(), this.b.getHeight(), var3, true);
            return this.b;
        }

        protected void a(Bitmap var1) {
            DsPhotoEditorActivity.intentResult = var1;
            PhotoEditorMesureCustomActivity.this.f.setVisibility(View.GONE);
            Intent var2 = new Intent();
            PhotoEditorMesureCustomActivity.this.setResult(-1, var2);
            PhotoEditorMesureCustomActivity.this.finish();
            super.onPostExecute(var1);
        }
    }
}
