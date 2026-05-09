package pro.sketchware.core.codegen;
import pro.sketchware.core.codegen.ActivityCodeGenerator;

/**
 * Provides context-aware code generation helpers that produce correct API calls
 * for both Activity and Fragment targets, eliminating the need for fragile
 * post-processing {@code String.replace()} in {@link ActivityCodeGenerator}.
 */
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

    // 鈹€鈹€ Context references 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    /** {@code getApplicationContext()} or {@code getContext().getApplicationContext()} */
    public String appContext() {
        return isFragment ? "getContext().getApplicationContext()" : "getApplicationContext()";
    }

    /** {@code getBaseContext()} or {@code getActivity().getBaseContext()} */
    public String baseContext() {
        return isFragment ? "getActivity().getBaseContext()" : "getBaseContext()";
    }

    /** {@code this} or {@code getContext()} 鈥?for constructors expecting a Context */
    public String thisContext() {
        return isFragment ? "getContext()" : "this";
    }

    /** {@code this} or {@code getActivity()} 鈥?for constructors expecting an Activity */
    public String thisActivity() {
        return isFragment ? "getActivity()" : "this";
    }

    /** {@code this} or {@code (Activity) getContext()} 鈥?for constructors expecting an Activity with cast */
    public String thisActivityCast() {
        return isFragment ? "(Activity) getContext()" : "this";
    }

    /** {@code ActivityName.this} or {@code getContext()} 鈥?for qualified this references */
    public String qualifiedThis() {
        return isFragment ? "getContext()" : activityName + ".this";
    }

    // 鈹€鈹€ System services 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    /** {@code (Cast) getSystemService} or {@code (Cast) getContext().getSystemService} */
    public String systemService(String castType) {
        return isFragment
                ? "(" + castType + ") getContext().getSystemService"
                : "(" + castType + ") getSystemService";
    }

    // 鈹€鈹€ Assets 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    /** {@code getAssets()} or {@code getContext().getAssets()} */
    public String assets() {
        return isFragment ? "getContext().getAssets()" : "getAssets()";
    }

    // 鈹€鈹€ SharedPreferences 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    /** {@code getSharedPreferences} or {@code getContext().getSharedPreferences} */
    public String sharedPreferences() {
        return isFragment ? "getContext().getSharedPreferences" : "getSharedPreferences";
    }

    // 鈹€鈹€ UI thread 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    /** {@code runOnUiThread} or {@code getActivity().runOnUiThread} */
    public String runOnUiThread() {
        return isFragment ? "getActivity().runOnUiThread" : "runOnUiThread";
    }

    // 鈹€鈹€ Layout inflater 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    /** {@code getLayoutInflater()} or {@code getActivity().getLayoutInflater()} */
    public String layoutInflater() {
        return isFragment ? "getActivity().getLayoutInflater()" : "getLayoutInflater()";
    }

    // 鈹€鈹€ Fragment manager 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    /** {@code getSupportFragmentManager()} or {@code getActivity().getSupportFragmentManager()} */
    public String fragmentManager() {
        return isFragment ? "getActivity().getSupportFragmentManager()" : "getSupportFragmentManager()";
    }
}
