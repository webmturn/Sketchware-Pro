package a.a.a;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.lang.ref.WeakReference;

import pro.sketchware.R;

public abstract class MA extends AsyncTask<Void, String, String> {

    private final WeakReference<Context> contextRef;

    public MA(Context var1) {
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
            b();
            return "";
        } catch (Exception e) {
            Log.e("MA", e.getMessage(), e);
            // the bytecode's lying
            if (e instanceof By) {
                return e.getMessage();
            }
            Context context = getContext();
            if (context != null) {
                return context.getString(R.string.common_error_an_error_occurred) + "[" + e.getMessage() + "]";
            }
            return "An error occurred[" + e.getMessage() + "]";
        }
    }

    public abstract void a();

    public abstract void a(String var1);

    public abstract void b() throws By;

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if (result.isEmpty()) {
            a();
        } else {
            a(result);
            Context context = getContext();
            if (context != null) {
                bB.b(context, result, 1).show();
            }
        }
    }
}
