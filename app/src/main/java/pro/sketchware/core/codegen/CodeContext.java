package pro.sketchware.core.codegen;

/** Java expressions for generated Activity and Fragment code. */
public class CodeContext {

    private final boolean isFragment;
    private final String activityName;

    public CodeContext(String activityName, boolean isFragment) {
        this.activityName = activityName;
        this.isFragment = isFragment;
    }

    public boolean isFragment() {
        return isFragment;
    }

    public String getActivityName() {
        return activityName;
    }

    public String appContext() {
        return isFragment ? "getContext().getApplicationContext()" : "getApplicationContext()";
    }

    public String baseContext() {
        return isFragment ? "getActivity().getBaseContext()" : "getBaseContext()";
    }

    public String thisContext() {
        return isFragment ? "getContext()" : "this";
    }

    public String thisActivity() {
        return isFragment ? "getActivity()" : "this";
    }

    public String thisActivityCast() {
        return isFragment ? "(Activity) getContext()" : "this";
    }

    public String qualifiedThis() {
        return isFragment ? "getContext()" : activityName + ".this";
    }

    public String systemService(String castType) {
        return isFragment
                ? "(" + castType + ") getContext().getSystemService"
                : "(" + castType + ") getSystemService";
    }

    public String assets() {
        return isFragment ? "getContext().getAssets()" : "getAssets()";
    }

    public String sharedPreferences() {
        return isFragment ? "getContext().getSharedPreferences" : "getSharedPreferences";
    }

    public String runOnUiThread() {
        return isFragment ? "getActivity().runOnUiThread" : "runOnUiThread";
    }

    public String layoutInflater() {
        return isFragment ? "getActivity().getLayoutInflater()" : "getLayoutInflater()";
    }

    public String fragmentManager() {
        return isFragment
                ? "getActivity().getSupportFragmentManager()"
                : "getSupportFragmentManager()";
    }
}
