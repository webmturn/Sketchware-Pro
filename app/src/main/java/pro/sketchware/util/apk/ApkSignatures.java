package pro.sketchware.util.apk;

import android.content.Context;
import android.view.LayoutInflater;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import pro.sketchware.util.Helper;
import pro.sketchware.R;
import pro.sketchware.databinding.DialogAppSignaturesBinding;

public class ApkSignatures {

    private final Context context;
    private String abMsg;

    public ApkSignatures(Context context, String path) {
        this.context = context;
        ApkUtils.setApkPath(path);
        try {
            abMsg = path == null || path.isEmpty()
                    ? context.getString(R.string.apk_signatures_missing_path)
                    : "SHA-1: " + getSha1() + "\n\nSHA-256: " + getSha256();
        } catch (Exception e) {
            abMsg = context.getString(R.string.apk_signatures_error, e.getMessage());
        }
    }

    public String getSha1() {
        String sha1 = ApkUtils.getSHA1();
        return sha1 != null ? sha1 : context.getString(R.string.apk_signatures_missing_sha1);
    }

    public String getSha256() {
        String sha256 = ApkUtils.getSHA256();
        return sha256 != null ? sha256 : context.getString(R.string.apk_signatures_missing_sha256);
    }

    public void showSignaturesDialog() {
        DialogAppSignaturesBinding signaturesBinding = DialogAppSignaturesBinding.inflate(LayoutInflater.from(context));
        MaterialAlertDialogBuilder signaturesDialog = new MaterialAlertDialogBuilder(context);
        signaturesDialog.setTitle(Helper.getResString(R.string.signatures_title));

        signaturesBinding.tvMsg.setText(abMsg);

        signaturesDialog.setView(signaturesBinding.getRoot());
        signaturesDialog.setPositiveButton(Helper.getResString(R.string.common_word_ok), null);
        signaturesDialog.show();
    }
}
