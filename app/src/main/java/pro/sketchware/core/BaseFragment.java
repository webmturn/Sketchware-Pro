package pro.sketchware.core;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.activity.OnBackPressedDispatcher;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.transition.MaterialSharedAxis;

import pro.sketchware.R;
import pro.sketchware.utility.UI;

public class BaseFragment extends Fragment {

    public Activity activity;
    @Deprecated
    public Context appContext;

    public BaseFragment() {
    }

    public void dismissProgressDialog() {
        if (getActivity() instanceof BaseAppCompatActivity) {
            ((BaseAppCompatActivity) getActivity()).dismissLoadingDialog();
        }
    }

    public void addTask(BaseAsyncTask var1) {
        if (getActivity() instanceof BaseAppCompatActivity) {
            ((BaseAppCompatActivity) getActivity()).addTask(var1);
        }
    }

    public void showProgressDialog() {
        if (getActivity() instanceof BaseAppCompatActivity) {
            ((BaseAppCompatActivity) getActivity()).showLoadingDialog();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = getActivity();
        appContext = activity.getApplicationContext();
        setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.X, true));
        setReturnTransition(new MaterialSharedAxis(MaterialSharedAxis.X, false));
        setExitTransition(new MaterialSharedAxis(MaterialSharedAxis.X, true));
        setReenterTransition(new MaterialSharedAxis(MaterialSharedAxis.X, false));
    }

    public void openFragment(Fragment fragment) {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.settings_fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    public void configureToolbar(MaterialToolbar toolbar) {
        OnBackPressedDispatcher onBackPressedDispatcher = requireActivity().getOnBackPressedDispatcher();
        toolbar.setNavigationOnClickListener(v -> {
            onBackPressedDispatcher.onBackPressed();
        });
    }

    public void handleInsets(View root) {
        UI.addWindowInsetToPadding(root, WindowInsetsCompat.Type.navigationBars(), false, false, false, true);
    }

}
