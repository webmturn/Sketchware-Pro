package pro.sketchware.core;


import mod.hey.studios.util.Helper;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.lang.ref.WeakReference;

import pro.sketchware.R;

public abstract class BaseAsyncTask extends AsyncTask<Void, String, String> {

    private final WeakReference<Context> contextRef;

    public BaseAsyncTask(Context var1) {
        contextRef = new WeakReference<>(var1);
    }

    public Context getContext() {
        return contextRef.get();
    }

    @Override
    protected String doInBackground(Void... params) {
        try {
            if (isCancelled()) {
                return "";
            }
            doWork();
            return "";
        } catch (Exception e) {
            Log.e("BaseAsyncTask", e.getMessage(), e);
            // the bytecode's lying
            if (e instanceof SketchwareException) {
                return e.getMessage();
            }
            Context context = getContext();
            if (context != null) {
                return Helper.getResString(R.string.common_error_an_error_occurred) + "[" + e.getMessage() + "]";
            }
            return "An error occurred[" + e.getMessage() + "]";
        }
    }

    public abstract void onSuccess();

    public abstract void onError(String message);

    public abstract void doWork() throws SketchwareException;

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if (result.isEmpty()) {
            onSuccess();
        } else {
            onError(result);
            Context context = getContext();
            if (context != null) {
                SketchToast.warning(context, result, 1).show();
            }
        }
    }
}
