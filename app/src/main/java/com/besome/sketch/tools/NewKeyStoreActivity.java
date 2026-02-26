package com.besome.sketch.tools;


import android.util.Log;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import pro.sketchware.core.LowercaseNameValidator;
import pro.sketchware.core.LengthRangeValidator;
import pro.sketchware.core.VariableNameValidator;
import pro.sketchware.core.SketchToast;
import pro.sketchware.core.KeyStoreManager;
import pro.sketchware.core.UIHelper;
import pro.sketchware.core.SketchwarePaths;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;

public class NewKeyStoreActivity extends BaseAppCompatActivity implements OnClickListener {
    private final int validityInYears = 25;
    private LowercaseNameValidator organizationValidator;
    private LowercaseNameValidator localityValidator;
    private LowercaseNameValidator stateValidator;
    private LowercaseNameValidator countryValidator;
    private LowercaseNameValidator commonNameValidator;
    private LowercaseNameValidator organizationalUnitValidator;
    private KeyStoreManager keyStoreManager;
    private EditText alias;
    private EditText password;
    private EditText passwordConfirm;
    private EditText commonName;
    private EditText organizationalUnit;
    private EditText organization;
    private EditText locality;
    private EditText state;
    private EditText country;
    private VariableNameValidator aliasValidator;
    private LengthRangeValidator passwordValidator, passwordConfirmValidator;

    private void showDoneDialog(boolean success, String password) {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setTitle(Helper.getResString(R.string.myprojects_sign_apk_new_certificate_title_certificate));
        if (success) {
            dialog.setIcon(R.drawable.certificate_96_blue);
            dialog.setMessage(Helper.getResString(R.string.myprojects_sign_apk_dialog_complete_create_certificate));
        } else {
            dialog.setIcon(R.drawable.error_96_yellow);
            dialog.setMessage(Helper.getResString(R.string.myprojects_sign_apk_error_failed_create_new_certificate));
        }

        dialog.setPositiveButton(Helper.getResString(R.string.common_word_close), (v, which) -> {
            if (!UIHelper.isClickThrottled()) {
                v.dismiss();
                if (success) {
                    Intent intent = new Intent();
                    intent.putExtra("pwd", password);
                    setResult(-1, intent);
                    finish();
                } else {
                    finish();
                }
            }
        });
        dialog.show();
    }

    private void save() {
        if (passwordValidator.isValid() && passwordConfirmValidator.isValid()) {
            String text = Helper.getText(password);
            if (!text.equals(Helper.getText(passwordConfirm))) {
                SketchToast.warning(getApplicationContext(), Helper.getResString(R.string.myprojects_sign_apk_incorrect_password), 0).show();
                password.setText("");
                passwordConfirm.setText("");
                return;
            }

            if (!aliasValidator.isValid()) return;

            if (!commonNameValidator.isValid()) return;

            if (!organizationalUnitValidator.isValid()) return;

            if (!organizationValidator.isValid()) return;

            if (!localityValidator.isValid()) return;

            if (!stateValidator.isValid()) return;

            if (!countryValidator.isValid()) return;

            StringBuilder dnBuilder = new StringBuilder();
            dnBuilder.append("CN=");
            dnBuilder.append(Helper.getText(commonName));
            dnBuilder.append("OU=");
            dnBuilder.append(Helper.getText(organizationalUnit));
            dnBuilder.append("O=");
            dnBuilder.append(Helper.getText(organization));
            dnBuilder.append("L=");
            dnBuilder.append(Helper.getText(locality));
            dnBuilder.append("ST=");
            dnBuilder.append(Helper.getText(state));
            dnBuilder.append("C=");
            dnBuilder.append(Helper.getText(country));

            try {
                keyStoreManager.generateAndSaveKeyStore(SketchwarePaths.getKeystoreFilePath(), dnBuilder.toString(), validityInYears, Helper.getText(alias), text);
                showDoneDialog(true, text);
            } catch (Exception e) {
                Log.e("NewKeyStoreActivity", e.getMessage(), e);
                showDoneDialog(false, null);
            }
        }

    }

    @Override
    public void onClick(View v) {
        if (!UIHelper.isClickThrottled()) {
            int id = v.getId();
            if (id == R.id.btn_keystore_cancel) {
                finish();
            } else if (id == R.id.btn_keystore_save) {
                save();
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!super.isStoragePermissionGranted()) {
            finish();
        }

        setContentView(R.layout.keystore_new);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        findViewById(R.id.layout_main_logo).setVisibility(View.GONE);
        getSupportActionBar().setTitle(Helper.getResString(R.string.myprojects_sign_apk_new_certificate_title_new_certificate));
        getSupportActionBar().setSubtitle(String.format(Helper.getResString(R.string.keystore_export_path_format), SketchwarePaths.getKeystoreFilePath()));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        toolbar.setNavigationOnClickListener(Helper.getBackPressedClickListener(this));
        keyStoreManager = new KeyStoreManager();


        Button button = findViewById(R.id.btn_keystore_save);
        button.setOnClickListener(this);
        button = findViewById(R.id.btn_keystore_cancel);
        button.setOnClickListener(this);
        alias = findViewById(R.id.et_keystore_alias);
        TextInputLayout tilAlias = findViewById(R.id.ti_keystore_alias);
        password = findViewById(R.id.et_keystore_passwd);
        TextInputLayout tilPassword = findViewById(R.id.ti_keystore_passwd);
        passwordConfirm = findViewById(R.id.et_keystore_passwd1);
        TextInputLayout tilPasswordConfirm = findViewById(R.id.ti_keystore_passwd1);
        EditText validity = findViewById(R.id.et_valid_year);
        validity.setText(String.valueOf(validityInYears));
        ((TextView) findViewById(R.id.tv_cert_title)).setText(Helper.getResString(R.string.myprojects_sign_apk_new_certificate_title_certificate));
        commonName = findViewById(R.id.et_dn_cn);
        TextInputLayout tilCommonName = findViewById(R.id.ti_dn_cn);
        organizationalUnit = findViewById(R.id.et_dn_ou);
        TextInputLayout tilOrganizationalUnit = findViewById(R.id.ti_dn_ou);
        organization = findViewById(R.id.et_dn_o);
        TextInputLayout tilOrganization = findViewById(R.id.ti_dn_o);
        locality = findViewById(R.id.et_dn_l);
        TextInputLayout tilLocality = findViewById(R.id.ti_dn_l);
        state = findViewById(R.id.et_dn_st);
        TextInputLayout tilState = findViewById(R.id.ti_dn_st);
        country = findViewById(R.id.et_dn_c);
        TextInputLayout tilCountry = findViewById(R.id.ti_dn_c);


        aliasValidator = new VariableNameValidator(getApplicationContext(), tilAlias);
        passwordValidator = new LengthRangeValidator(getApplicationContext(), tilPassword, 4, 32);
        passwordConfirmValidator = new LengthRangeValidator(getApplicationContext(), tilPasswordConfirm, 4, 32);
        commonNameValidator = new LowercaseNameValidator(getApplicationContext(), tilCommonName);
        organizationalUnitValidator = new LowercaseNameValidator(getApplicationContext(), tilOrganizationalUnit);
        organizationValidator = new LowercaseNameValidator(getApplicationContext(), tilOrganization);
        localityValidator = new LowercaseNameValidator(getApplicationContext(), tilLocality);
        stateValidator = new LowercaseNameValidator(getApplicationContext(), tilState);
        countryValidator = new LowercaseNameValidator(getApplicationContext(), tilCountry);


        alias.setPrivateImeOptions("defaultInputmode=english;");
        commonName.setPrivateImeOptions("defaultInputmode=english;");
        organizationalUnit.setPrivateImeOptions("defaultInputmode=english;");
        organization.setPrivateImeOptions("defaultInputmode=english;");
        locality.setPrivateImeOptions("defaultInputmode=english;");
        state.setPrivateImeOptions("defaultInputmode=english;");
        country.setPrivateImeOptions("defaultInputmode=english;");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!super.isStoragePermissionGranted()) {
            finish();
        }
    }
}
