package pro.sketchware.activities.help;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;

import pro.sketchware.activities.base.BaseAppCompatActivity;

import pro.sketchware.util.io.EncryptedFileUtil;
import pro.sketchware.util.Helper;
import pro.sketchware.databinding.ActivityOssLibrariesBinding;

public class LicenseActivity extends BaseAppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        enableEdgeToEdgeNoContrast();
        super.onCreate(savedInstanceState);
        ActivityOssLibrariesBinding binding = ActivityOssLibrariesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(Helper.getBackPressedClickListener(this));

        binding.licensesText.setText(new EncryptedFileUtil().readAssetFile(getApplicationContext(), "oss.txt"));
        binding.licensesText.setAutoLinkMask(Linkify.WEB_URLS);
        binding.licensesText.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
