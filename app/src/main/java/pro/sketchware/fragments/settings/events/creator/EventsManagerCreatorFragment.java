package pro.sketchware.fragments.settings.events.creator;

import static pro.sketchware.utility.GsonUtils.getGson;

import com.google.gson.JsonSyntaxException;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.HashMap;

import pro.sketchware.core.BaseFragment;
import mod.hey.studios.util.Helper;
import mod.hilal.saif.activities.tools.IconSelectorDialog;
import mod.jbk.util.OldResourceIdMapper;
import pro.sketchware.R;
import pro.sketchware.databinding.FragmentEventsManagerCreatorBinding;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;

public class EventsManagerCreatorFragment extends BaseFragment {

    private String _code;
    private String _desc;
    private String _icon;
    private String _name;
    private String _par;
    private String _spec;
    private String _var;
    private String event_name = "";
    private boolean isActivityEvent;
    private boolean isEdit;
    private String lisName;

    private FragmentEventsManagerCreatorBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null && getArguments().containsKey("lis_name")) {
            lisName = getArguments().getString("lis_name");
            isActivityEvent = lisName.isEmpty();
        }
        if (getArguments() != null && getArguments().containsKey("event")) {
            event_name = getArguments().getString("event");
            isEdit = true;
            _name = getArguments().getString("_name");
            _var = getArguments().getString("_var");
            _icon = getArguments().getString("_icon");
            _desc = getArguments().getString("_desc");
            _par = getArguments().getString("_par");
            _spec = getArguments().getString("_spec");
            _code = getArguments().getString("_code");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentEventsManagerCreatorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setToolbar();
        getViewsById();
        setupViews();

        if (isEdit) fillUp();

        {
            View view1 = binding.appBarLayout;
            int left = view1.getPaddingLeft();
            int top = view1.getPaddingTop();
            int right = view1.getPaddingRight();
            int bottom = view1.getPaddingBottom();

            ViewCompat.setOnApplyWindowInsetsListener(view1, (v, i) -> {
                Insets insets = i.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
                v.setPadding(left + insets.left, top + insets.top, right + insets.right, bottom + insets.bottom);
                return i;
            });
        }

        {
            View view1 = binding.content;
            int left = view1.getPaddingLeft();
            int top = view1.getPaddingTop();
            int right = view1.getPaddingRight();
            int bottom = view1.getPaddingBottom();

            ViewCompat.setOnApplyWindowInsetsListener(view1, (v, i) -> {
                Insets insets = i.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime() | WindowInsetsCompat.Type.displayCutout());
                v.setPadding(left + insets.left, top, right + insets.right, bottom + insets.bottom);
                return i;
            });
        }
    }

    private void fillUp() {
        binding.eventsCreatorEventname.setText(_name);
        binding.eventsCreatorVarname.setText(_var);
        binding.eventsCreatorIcon.setText(_icon);
        binding.eventsCreatorDesc.setText(_desc);
        binding.eventsCreatorParams.setText(_par);
        binding.eventsCreatorSpec.setText(_spec);
        binding.eventsCreatorCode.setText(_code);
    }

    private boolean filledIn() {
        if (isActivityEvent) {
            return !Helper.getText(binding.eventsCreatorEventname).isEmpty()
                    && !Helper.getText(binding.eventsCreatorSpec).isEmpty()
                    && !Helper.getText(binding.eventsCreatorCode).isEmpty();
        } else {
            return !Helper.getText(binding.eventsCreatorEventname).isEmpty()
                    && !Helper.getText(binding.eventsCreatorVarname).isEmpty()
                    && !Helper.getText(binding.eventsCreatorIcon).isEmpty()
                    && !Helper.getText(binding.eventsCreatorSpec).isEmpty()
                    && !Helper.getText(binding.eventsCreatorCode).isEmpty();
        }
    }

    private void getViewsById() {
        ((View) binding.eventsCreatorListenercode.getParent().getParent()).setVisibility(View.GONE);
        binding.eventsCreatorChooseicon.setImageResource(R.drawable.ic_mtrl_add);
        if (isActivityEvent) {
            binding.eventsCreatorVarname.setText("");
            ((View) binding.eventsCreatorVarname.getParent().getParent()).setVisibility(View.GONE);
            binding.eventsCreatorIcon.setText("2131165298");
            ((View) binding.eventsCreatorIcon.getParent().getParent().getParent()).setVisibility(View.GONE);
        }
        Helper.addClearErrorOnTextChangeTextWatcher(binding.eventsCreatorIconTil);
    }

    private void setupViews() {
        binding.eventsCreatorCancel.setOnClickListener(Helper.getBackPressedClickListener(requireActivity()));
        binding.eventsCreatorSave.setOnClickListener(v -> save());
        binding.eventsCreatorChooseicon.setOnClickListener(v -> showIconSelectorDialog());
    }

    private void showIconSelectorDialog() {
        new IconSelectorDialog(requireActivity(), binding.eventsCreatorIcon).show();
    }

    private void save() {
        if (!filledIn()) {
            SketchwareUtil.toast(Helper.getResString(R.string.error_required_fields_empty));
            return;
        }
        if (!OldResourceIdMapper.isValidIconId(Helper.getText(binding.eventsCreatorIcon))) {
            binding.eventsCreatorIconTil.setError(getString(R.string.error_invalid_icon_id));
            binding.eventsCreatorIcon.requestFocus();
            return;
        }
        ArrayList<HashMap<String, Object>> eventsList;
        String concat = FileUtil.getExternalStorageDir().concat("/.sketchware/data/system/events.json");
        if (FileUtil.isExistFile(concat)) {
            try {
                eventsList = getGson().fromJson(FileUtil.readFile(concat), Helper.TYPE_MAP_LIST);
            } catch (JsonSyntaxException e) {
                eventsList = new ArrayList<>();
            }
        } else {
            eventsList = new ArrayList<>();
        }
        HashMap<String, Object> eventData = new HashMap<>();
        if (isEdit) {
            eventData = eventsList.get(figureP(_name));
        }
        eventData.put("name", Helper.getText(binding.eventsCreatorEventname));
        eventData.put("var", Helper.getText(binding.eventsCreatorVarname));
        if (isActivityEvent) {
            eventData.put("listener", "");
        } else {
            eventData.put("listener", lisName);
        }
        eventData.put("icon", Helper.getText(binding.eventsCreatorIcon));
        eventData.put("description", Helper.getText(binding.eventsCreatorDesc));
        eventData.put("parameters", Helper.getText(binding.eventsCreatorParams));
        eventData.put("code", Helper.getText(binding.eventsCreatorCode));
        eventData.put("headerSpec", Helper.getText(binding.eventsCreatorSpec));
        if (!isEdit) {
            eventsList.add(eventData);
        }
        FileUtil.writeFile(concat, getGson().toJson(eventsList));
        SketchwareUtil.toast(Helper.getResString(R.string.common_word_saved));
        getParentFragmentManager().popBackStack();
    }

                 private int figureP(String eventName) {
        String concat = FileUtil.getExternalStorageDir().concat("/.sketchware/data/system/events.json");
        if (FileUtil.isExistFile(concat)) {
            ArrayList<HashMap<String, Object>> eventsList;
            try {
                eventsList = getGson().fromJson(FileUtil.readFile(concat), Helper.TYPE_MAP_LIST);
            } catch (JsonSyntaxException e) {
                return 0;
            }
            for (int i = 0; i < eventsList.size(); i++) {
                if (eventName.equals(eventsList.get(i).get("name"))) {
                    return i;
                }
            }
        }
        return 0;
    }

    private void setToolbar() {
        configureToolbar(binding.toolbar);

        if (isEdit) {
            binding.toolbar.setTitle(R.string.events_properties_title);
            binding.toolbar.setSubtitle(event_name);
        } else if (isActivityEvent) {
            binding.toolbar.setTitle(R.string.events_new_activity_title);
        } else {
            binding.toolbar.setTitle(R.string.events_new_event_title);
            binding.toolbar.setSubtitle(lisName);
        }
    }
}
