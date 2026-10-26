package pro.sketchware.activities.design;


import pro.sketchware.util.Helper;
import pro.sketchware.core.project.StringfogHandler;
import pro.sketchware.R;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import pro.sketchware.databinding.FragmentStringfogManagerBinding;

public class ManageStringFogFragment extends BottomSheetDialogFragment {

    private FragmentStringfogManagerBinding binding;
    private StringfogHandler stringfogHandler;

    @NonNull
    @Override
    public BottomSheetDialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        dialog.getBehavior().setSkipCollapsed(true);
        return dialog;
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStringfogManagerBinding.inflate(inflater, container, false);
        initializeLogic();
        return binding.getRoot();
    }

    private void initializeLogic() {
        stringfogHandler = new StringfogHandler(requireActivity().getIntent().getStringExtra("sc_id"));
        binding.swPgEnabled.setChecked(stringfogHandler.isStringfogEnabled());
        binding.swPgEnabled.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            stringfogHandler.setStringfogEnabled(isChecked);
            setStringFogStatus(isChecked);
        });
        setStringFogStatus(stringfogHandler.isStringfogEnabled());
    }

    private void setStringFogStatus(boolean enabled) {
        if (enabled) {
            binding.swPgEnabled.setText(Helper.getResString(R.string.stringfog_enabled));
        } else {
            binding.swPgEnabled.setText(Helper.getResString(R.string.stringfog_disabled));
        }
    }
}
