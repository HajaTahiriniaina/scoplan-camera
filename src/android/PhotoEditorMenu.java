package scoplan.camera;

import static android.app.Activity.RESULT_OK;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.dsphotoeditor.sdk.activity.DsPhotoEditorActivity;
import com.dsphotoeditor.sdk.activity.DsPhotoEditorCropActivity;
import com.dsphotoeditor.sdk.activity.DsPhotoEditorDrawActivity;
import com.dsphotoeditor.sdk.activity.DsPhotoEditorTextActivity;

import scoplan.camera.PhotoEditorMesureCustomActivity;
import scoplan.camera.FakeR;

public class PhotoEditorMenu extends Fragment implements View.OnClickListener {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private DsPhotoEditorActivity a;
    private LinearLayout c;
    int[] clickableButton;
    private FakeR fakeR;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.fakeR = new FakeR(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater var1, ViewGroup var2, Bundle var3) {
        View var4 = var1.inflate(fakeR.getLayout("fragment_photo_editor_menu"), var2, false);
        clickableButton = new int[] {
            fakeR.getId("ds_photo_editor_btn_crop"),
            fakeR.getId("ds_photo_editor_btn_sticker"),
            fakeR.getId("ds_photo_editor_btn_text"),
            fakeR.getId("ds_photo_editor_btn_draw"),
            fakeR.getId("ds_photo_editor_btn_remove"),
        };
        this.a = (DsPhotoEditorActivity) this.getActivity();
        this.c = var4.findViewById(fakeR.getId("custom_photo_editor_bar_bottom_container"));
        int var6;
        for(var6 = 0; var6 < clickableButton.length; ++var6) {
            this.c.findViewById(clickableButton[var6]).setOnClickListener(this);
        }
        return var4;
    }

    public void onClick(View var1) {
        int var2 = var1.getId();
        DsPhotoEditorActivity var10000;
        Intent var3;
        if(var2 == fakeR.getId("ds_photo_editor_btn_crop")) {
            DsPhotoEditorActivity.intentResult = null;
            DsPhotoEditorCropActivity.original = this.a.getHdBitmap();
            var3 = new Intent(this.a, DsPhotoEditorCropActivity.class);
            var10000 = this.a;
            this.a.getClass();
            var10000.startActivityForResult(var3, 3);
            this.a.toolType = 6;
        } else if(var2 == fakeR.getId("ds_photo_editor_btn_sticker")) {
            DsPhotoEditorActivity.intentResult = null;
            PhotoEditorMesureCustomActivity.original = this.a.getHdBitmap();
            var3 = new Intent(this.a, PhotoEditorMesureCustomActivity.class);
            var10000 = this.a;
            this.a.getClass();
            var10000.startActivityForResult(var3, 2);
            this.a.toolType = 14;
        } else if(var2 == fakeR.getId("ds_photo_editor_btn_text")) {
            DsPhotoEditorActivity.intentResult = null;
            DsPhotoEditorTextActivity.original = this.a.getHdBitmap();
            var3 = new Intent(this.a, DsPhotoEditorTextActivity.class);
            var10000 = this.a;
            this.a.getClass();
            var10000.startActivityForResult(var3, 2);
            this.a.toolType = 14;
        } else if(var2 == fakeR.getId("ds_photo_editor_btn_draw")) {
            DsPhotoEditorActivity.intentResult = null;
            DsPhotoEditorDrawActivity.original = this.a.getHdBitmap();
            var3 = new Intent(this.a, DsPhotoEditorDrawActivity.class);
            var10000 = this.a;
            this.a.getClass();
            var10000.startActivityForResult(var3, 4);
            this.a.toolType = 12;
        } else if(var2 == fakeR.getId("ds_photo_editor_btn_remove")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Voulez vous vraiment supprimer l'image?").setPositiveButton("Oui", new android.content.DialogInterface.OnClickListener() {
                public void onClick(DialogInterface var1, int var2) {
                    Intent data = new Intent();
                    String result = "remove";
                    data.setData(Uri.parse(result));
                    getActivity().setResult(RESULT_OK, data);
                    getActivity().finish();
                }
            }).setNegativeButton("Annuler", new android.content.DialogInterface.OnClickListener() {
                public void onClick(DialogInterface var1, int var2) {
                }
            });
            builder.create().show();
        }
    }
}
