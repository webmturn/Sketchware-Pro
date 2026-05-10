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

    // ── Context references ──────────────────────────────────────────

    /** {@code getApplicationContext()} or {@code getContext().getApplicationContext()} */
    public String appContext() {
        return isFragment ? "getContext().getApplicationContext()" : "getApplicationContext()";
    }

    /** {@code getBaseContext()} or {@code getActivity().getBaseContext()} */
    public String baseContext() {
        return isFragment ? "getActivity().getBaseContext()" : "getBaseContext()";
    }

    /** {@code this} or {@code getContext()} — for constructors expecting a Context */
    public String thisContext() {
        return isFragment ? "getContext()" : "this";
    }

    /** {@code this} or {@code getActivity()} — for constructors expecting an Activity */
    public String thisActivity() {
        return isFragment ? "getActivity()" : "this";
    }

    /** {@code this} or {@code (Activity) getContext()} — for constructors expecting an Activity with cast */
    public String thisActivityCast() {
        return isFragment ? "(Activity) getContext()" : "this";
    }

    /** {@code ActivityName.this} or {@code getContext()} — for qualified this references */
    public String qualifiedThis() {
        return isFragment ? "getContext()" : activityName + ".this";
    }

    // ── System services ─────────────────────────────────────────────

    /** {@code (Cast) getSystemService} or {@code (Cast) getContext().getSystemService} */
    public String systemService(String castType) {
        return isFragment
                ? "(" + castType + ") getContext().getSystemService"
                : "(" + castType + ") getSystemService";
    }

    // ── Assets ──────────────────────────────────────────────────────

    /** {@code getAssets()} or {@code getContext().getAssets()} */
    public String assets() {
        return isFragment ? "getContext().getAssets()" : "getAssets()";
    }

    // ── SharedPreferences ───────────────────────────────────────────

    /** {@code getSharedPreferences} or {@code getContext().getSharedPreferences} */
    public String sharedPreferences() {
        return isFragment ? "getContext().getSharedPreferences" : "getSharedPreferences";
    }

    // ── UI thread ───────────────────────────────────────────────────

    /** {@code runOnUiThread} or {@code getActivity().runOnUiThread} */
    public String runOnUiThread() {
        return isFragment ? "getActivity().runOnUiThread" : "runOnUiThread";
    }

    // ── Layout inflater ─────────────────────────────────────────────

    /** {@code getLayoutInflater()} or {@code getActivity().getLayoutInflater()} */
    public String layoutInflater() {
        return isFragment ? "getActivity().getLayoutInflater()" : "getLayoutInflater()";
    }

    // ── Fragment manager ────────────────────────────────────────────

    /** {@code getSupportFragmentManager()} or {@code getActivity().getSupportFragmentManager()} */
    public String fragmentManager() {
        return isFragment ? "getActivity().getSupportFragmentManager()" : "getSupportFragmentManager()";
    }
}
