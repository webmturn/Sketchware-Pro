package com.besome.sketch.lib.base;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Color;
import android.os.AsyncTask.Status;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsCompat;

import com.besome.sketch.lib.ui.LoadingDialog;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.ArrayList;

import pro.sketchware.core.BaseAsyncTask;
import pro.sketchware.core.ProjectListManager;
import dev.chrisbanes.insetter.Insetter;
import pro.sketchware.dialogs.ProgressDialog;

public abstract class BaseAppCompatActivity extends AppCompatActivity {

    public FirebaseAnalytics mAnalytics;

    @Deprecated
    public Context e;
    public Activity parent;
    protected ProgressDialog progressDialog;
    private LoadingDialog lottieDialog;
    private ArrayList<BaseAsyncTask> taskList;

    @Deprecated
    public void a(BaseAsyncTask var1) {
        addTask(var1);
    }

    public void addTask(BaseAsyncTask task) {
        taskList.add(task);
    }

    public void showProgressDialogWithCancel(OnCancelListener cancelListener) {
        if (progressDialog != null && !progressDialog.isShowing()) {
            progressDialog.setOnCancelListener(cancelListener);
            progressDialog.show();
        }
    }

    public void setProgressMessage(String var1) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.setMessage(var1);
        }
    }

    public void cancelAllTasks() {
        for (BaseAsyncTask task : taskList) {
            if (task.getStatus() != Status.FINISHED && !task.isCancelled()) {
                task.cancel(true);
            }
        }
        taskList.clear();
    }

    public void dismissLoadingDialog() {
        try {
            if (lottieDialog != null && lottieDialog.isShowing()) {
                lottieDialog.dismiss();
            }
        } catch (Exception var2) {
            lottieDialog = null;
            lottieDialog = new LoadingDialog(this);
        }
    }

    public void dismissProgressDialog() {
        try {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        } catch (Exception var2) {
            progressDialog = null;
            progressDialog = new ProgressDialog(this);
        }

    }

    public boolean isStoragePermissionGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == 0 && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == 0;
    }

    @Deprecated
    public boolean j() {
        return isStoragePermissionGranted();
    }

    public void showLoadingDialog() {
        if (lottieDialog != null && !lottieDialog.isShowing() && !isFinishing()) {
            lottieDialog.show();
        }
    }

    @Deprecated
    public void k() {
        showLoadingDialog();
    }

    @Deprecated
    public void h() {
        dismissLoadingDialog();
    }

    @Deprecated
    public void g() {
        cancelAllTasks();
    }

    @Deprecated
    public void i() {
        dismissProgressDialog();
    }

    @Deprecated
    public void a(OnCancelListener cancelListener) {
        showProgressDialogWithCancel(cancelListener);
    }

    @Deprecated
    public void a(String message) {
        setProgressMessage(message);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        e = getApplicationContext();
        taskList = new ArrayList<>();
        lottieDialog = new LoadingDialog(this);
        ProjectListManager.initializeDb(getApplicationContext(), false);
        progressDialog = new ProgressDialog(this);
        mAnalytics = null;
    }

    @Override
    public void onDestroy() {
        cancelAllTasks();
        if (lottieDialog != null && lottieDialog.isShowing()) {
            lottieDialog.cancelAnimation();
        }
        super.onDestroy();
    }

    @Override
    public void onPause() {
        if (lottieDialog != null && lottieDialog.isShowing()) {
            lottieDialog.pauseAnimation();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (lottieDialog != null && lottieDialog.isShowing()) {
            lottieDialog.resumeAnimation();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (parent != null) {
            return parent.onCreateOptionsMenu(menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (parent != null) {
            return parent.onOptionsItemSelected(item);
        }
        return false;
    }

    public void handleInsets(View root) {
        Insetter.builder()
                .padding(WindowInsetsCompat.Type.navigationBars())
                .applyToView(root);
    }

    protected void enableEdgeToEdgeNoContrast() {
        SystemBarStyle systemBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT);
        EdgeToEdge.enable(this, systemBarStyle);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }
    }
}