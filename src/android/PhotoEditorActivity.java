package scoplan.camera;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.dsphotoeditor.sdk.activity.DsPhotoEditorActivity;

import java.io.File;

import scoplan.camera.FakeR;
import scoplan.camera.PhotoEditorMenu;

public class PhotoEditorActivity extends DsPhotoEditorActivity {
    private Uri e;

    private FakeR fakeR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.fakeR = new FakeR(this);
        this.e = this.getIntent().getData();
        if (this.e == null) {
            Toast.makeText(this, this.getString(com.dsphotoeditor.sdk.R.string.ds_photo_editor_error_no_uri), Toast.LENGTH_LONG).show();
            this.finish();
        } else {
            this.createBottomBar(savedInstanceState);
        }
    }

    @SuppressLint("ResourceType")
    private void createBottomBar(Bundle var1) {
        if (super.findViewById(fakeR.getId("ds_photo_editor_bottom_bar_fragment_container")) != null) {
            View super_bottom_bar = super.findViewById(fakeR.getId("ds_photo_editor_bottom_bar_fragment_container"));
            super_bottom_bar.setVisibility(View.INVISIBLE);
            View custom_bottom_bar = new FrameLayout(this);
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, super_bottom_bar.getLayoutParams().height);
            lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            custom_bottom_bar.setLayoutParams(lp);
            custom_bottom_bar.setId(12345678);
            ((RelativeLayout) super.findViewById(fakeR.getId("ds_photo_editor_root_layout"))).addView(custom_bottom_bar);
            PhotoEditorMenu var2 = new PhotoEditorMenu();
            var2.setArguments(super.getIntent().getExtras());
            super.getFragmentManager().beginTransaction().add(custom_bottom_bar.getId(), var2, "custom_bottom_fragment_tag").commit();
        }
    }

    public void onClick(View var1) {
        int var2 = this.getFragmentManager().getBackStackEntryCount();
        if (var1.getId() == com.dsphotoeditor.sdk.R.id.ds_photo_editor_top_button_apply) {
            if (var2 == 0) {
                if (this.getHdBitmap() != null && !this.isLoadingIndicatorShowing()) {
                    a var3 = new a();
                    var3.execute(new Bitmap[]{this.getHdBitmap()});
                }
            } else if (!this.isLoadingIndicatorShowing()) {
                this.getFragmentManager().popBackStack();
                this.updateTopbarTitle(this.getString(com.dsphotoeditor.sdk.R.string.ds_photo_editor_main_title));
                if (this.toolType == 7) {
                    this.setHdBitmap(com.dsphotoeditor.sdk.b.d.a);
                    com.dsphotoeditor.sdk.b.d.a = null;
                }
                com.dsphotoeditor.sdk.a.a var4 = new com.dsphotoeditor.sdk.a.a(this, this.filterLutIdValue, this.frameIdValue, this.seekBarValue);
                var4.execute(new Integer[]{this.toolType});
            }
        } else {
            super.onClick(var1);
        }
    }

    private class a extends AsyncTask<Bitmap, Void, File> {
        private ProgressDialog b;

        private a() {
        }

        @Override
        protected File doInBackground(Bitmap... bitmaps) {
            return a(bitmaps);
        }

        protected void onPreExecute() {
            super.onPreExecute();
            this.b = com.dsphotoeditor.sdk.utils.c.a(PhotoEditorActivity.this, PhotoEditorActivity.this.getString(com.dsphotoeditor.sdk.R.string.ds_photo_editor_finish_up_loading_indicator), 1, false);
            if (!this.b.isShowing()) {
                this.b.show();
            }

        }

        protected File a(Bitmap... var1) {
            Bitmap var2 = var1[0];
            File var3 = scoplan.camera.PhotoEditorStore.a(PhotoEditorActivity.this, var2);
            return var3;
        }

        @Override
        protected void onPostExecute(File file) {
            a(file);
        }

        protected void a(File var1) {
            super.onPostExecute(var1);
            if (this.b != null && this.b.isShowing()) {
                this.b.dismiss();
            }

            if (var1 != null) {
                Uri var2 = Uri.fromFile(var1);
                Intent var3 = new Intent();
                var3.setData(var2);
                PhotoEditorActivity.this.setResult(-1, var3);
            }

            PhotoEditorActivity.this.finish();
        }
    }
}
