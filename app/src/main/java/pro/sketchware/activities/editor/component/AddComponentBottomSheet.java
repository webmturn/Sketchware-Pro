package pro.sketchware.activities.editor.component;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import java.util.ArrayList;
import pro.sketchware.R;
import pro.sketchware.beans.ComponentBean;
import pro.sketchware.beans.ProjectFileBean;
import pro.sketchware.core.project.ComponentsHandler;
import pro.sketchware.databinding.ComponentAddItemBinding;
import pro.sketchware.databinding.LogicAddComponentBinding;
import pro.sketchware.dialogs.InnerAddComponentBottomSheet;
import pro.sketchware.util.Helper;

public class AddComponentBottomSheet extends BottomSheetDialogFragment {
    public static final String RESULT_JAVA_FILE = "java_file";
    private static final int MIN_COMPONENT_WIDTH_DP = 100;
    private String sc_id;
    private ProjectFileBean projectFileBean;

    private final ArrayList<ComponentOption> options = new ArrayList<>();
    private LogicAddComponentBinding binding;
    private int sheetOrientation;

    public static AddComponentBottomSheet newInstance(
            String scId, ProjectFileBean projectFileBean) {
        AddComponentBottomSheet addComponentBottomSheet = new AddComponentBottomSheet();
        Bundle args = new Bundle();
        args.putString("sc_id", scId);
        args.putParcelable("project_file_bean", projectFileBean);
        addComponentBottomSheet.setArguments(args);
        return addComponentBottomSheet;
    }

    public static String resultKey(String projectId) {
        return AddComponentBottomSheet.class.getName() + ":" + projectId;
    }

    /** Notify the current host, including after FragmentManager has recreated the sheets. */
    public static void dispatchAdded(FragmentManager manager, String projectId, String javaFile) {
        Bundle result = new Bundle();
        result.putString(RESULT_JAVA_FILE, javaFile);
        manager.setFragmentResult(resultKey(projectId), result);
        for (Fragment fragment : manager.getFragments()) {
            if (fragment instanceof AddComponentBottomSheet selector
                    && projectId.equals(selector.sc_id)
                    && selector.projectFileBean != null
                    && javaFile.equals(selector.projectFileBean.getJavaName())) {
                selector.dismiss();
            }
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args == null) {
            dismissAllowingStateLoss();
            return;
        }
        sc_id = args.getString("sc_id");
        projectFileBean = args.getParcelable("project_file_bean");
        if (sc_id == null || projectFileBean == null) {
            dismissAllowingStateLoss();
            return;
        }

        populateComponents();
    }

    @Override
    public void onStart() {
        super.onStart();
        sheetOrientation = getResources().getConfiguration().orientation;
        if (sheetOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            ((BottomSheetDialog) requireDialog())
                    .getBehavior()
                    .setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (sheetOrientation != newConfig.orientation) {
            sheetOrientation = newConfig.orientation;
            if (getDialog() instanceof BottomSheetDialog dialog) {
                dialog.getBehavior()
                        .setState(
                                sheetOrientation == Configuration.ORIENTATION_LANDSCAPE
                                        ? BottomSheetBehavior.STATE_EXPANDED
                                        : BottomSheetBehavior.STATE_COLLAPSED);
            }
        }
    }

    private void populateComponents() {
        addSection(
                R.string.component_group_app_tools,
                ComponentBean.COMPONENT_TYPE_INTENT,
                ComponentBean.COMPONENT_TYPE_CALENDAR,
                ComponentBean.COMPONENT_TYPE_TIMERTASK,
                ComponentBean.COMPONENT_TYPE_OBJECTANIMATOR,
                ComponentBean.COMPONENT_TYPE_NOTIFICATION,
                ComponentBean.COMPONENT_TYPE_FRAGMENT_ADAPTER,
                ComponentBean.COMPONENT_TYPE_ASYNC_TASK);
        addSection(
                R.string.component_group_dialogs,
                ComponentBean.COMPONENT_TYPE_DIALOG,
                ComponentBean.COMPONENT_TYPE_PROGRESS_DIALOG,
                ComponentBean.COMPONENT_TYPE_DATE_PICKER_DIALOG,
                ComponentBean.COMPONENT_TYPE_TIME_PICKER_DIALOG);
        addSection(
                R.string.component_group_storage_files,
                ComponentBean.COMPONENT_TYPE_SHAREDPREF,
                ComponentBean.COMPONENT_TYPE_SQLITE,
                ComponentBean.COMPONENT_TYPE_FILE_PICKER);
        addSection(
                R.string.component_group_media_voice,
                ComponentBean.COMPONENT_TYPE_CAMERA,
                ComponentBean.COMPONENT_TYPE_MEDIAPLAYER,
                ComponentBean.COMPONENT_TYPE_SOUNDPOOL,
                ComponentBean.COMPONENT_TYPE_TEXT_TO_SPEECH,
                ComponentBean.COMPONENT_TYPE_SPEECH_TO_TEXT);
        addSection(
                R.string.component_group_network_device,
                ComponentBean.COMPONENT_TYPE_REQUEST_NETWORK,
                ComponentBean.COMPONENT_TYPE_BLUETOOTH_CONNECT,
                ComponentBean.COMPONENT_TYPE_LOCATION_MANAGER,
                ComponentBean.COMPONENT_TYPE_GYROSCOPE,
                ComponentBean.COMPONENT_TYPE_VIBRATOR);
        addSection(
                R.string.component_group_firebase,
                ComponentBean.COMPONENT_TYPE_FIREBASE,
                ComponentBean.COMPONENT_TYPE_FIREBASE_AUTH,
                ComponentBean.COMPONENT_TYPE_FIREBASE_STORAGE,
                ComponentBean.COMPONENT_TYPE_FIREBASE_AUTH_PHONE,
                ComponentBean.COMPONENT_TYPE_FIREBASE_CLOUD_MESSAGE,
                ComponentBean.COMPONENT_TYPE_FIREBASE_AUTH_GOOGLE_LOGIN);
        addSection(
                R.string.component_group_ads,
                ComponentBean.COMPONENT_TYPE_INTERSTITIAL_AD,
                ComponentBean.COMPONENT_TYPE_REWARDED_VIDEO_AD);

        var customComponents = new ArrayList<ComponentBean>();
        ComponentsHandler.addCustomComponents(customComponents);
        if (!customComponents.isEmpty()) {
            options.add(new ComponentOption(R.string.component_group_custom, null));
            for (ComponentBean component : customComponents) {
                options.add(new ComponentOption(0, component));
            }
        }
    }

    private void addSection(@StringRes int title, int... types) {
        options.add(new ComponentOption(title, null));
        for (int type : types) {
            options.add(new ComponentOption(0, new ComponentBean(type)));
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = LogicAddComponentBinding.inflate(getLayoutInflater());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (sc_id == null || projectFileBean == null) return;

        binding.title.setText(Helper.getResString(R.string.component_title_add_component));
        GridLayoutManager grid =
                new GridLayoutManager(requireContext(), 3) {
                    @Override
                    public void onMeasure(
                            @NonNull RecyclerView.Recycler recycler,
                            @NonNull RecyclerView.State state,
                            int widthSpec,
                            int heightSpec) {
                        int contentWidth =
                                View.MeasureSpec.getSize(widthSpec)
                                        - getPaddingLeft()
                                        - getPaddingRight();
                        if (contentWidth > 0) {
                            float density = getResources().getDisplayMetrics().density;
                            float fontScale =
                                    Math.max(1f, getResources().getConfiguration().fontScale);
                            float cellWidth = MIN_COMPONENT_WIDTH_DP * density * fontScale;
                            setSpanCount(Math.max(1, (int) (contentWidth / cellWidth)));
                        }
                        super.onMeasure(recycler, state, widthSpec, heightSpec);
                    }
                };
        grid.setSpanSizeLookup(
                new GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int position) {
                        return options.get(position).title() != 0 ? grid.getSpanCount() : 1;
                    }
                });
        binding.componentList.setHasFixedSize(true);
        binding.componentList.setAdapter(new ComponentsAdapter());
        binding.componentList.setLayoutManager(grid);

        BottomSheetDialog dialog = (BottomSheetDialog) requireDialog();
        BottomSheetBehavior<?> behavior = dialog.getBehavior();
        behavior.setDraggableOnNestedScroll(false);
        fitListToVisibleSheet(dialog, binding.componentList);

        // Keep list gestures in the list, even after scrolling to an edge.
        binding.componentList.addOnItemTouchListener(
                new RecyclerView.SimpleOnItemTouchListener() {
                    @Override
                    public boolean onInterceptTouchEvent(
                            @NonNull RecyclerView recyclerView, @NonNull MotionEvent event) {
                        int action = event.getActionMasked();
                        behavior.setDraggable(
                                action == MotionEvent.ACTION_UP
                                        || action == MotionEvent.ACTION_CANCEL);
                        return false;
                    }
                });

        binding.componentList.addOnScrollListener(
                new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                        if (binding == null || recyclerView != binding.componentList) return;
                        binding.dividerTop.setVisibility(
                                recyclerView.canScrollVertically(-1) ? View.VISIBLE : View.GONE);
                        binding.dividerBottom.setVisibility(
                                recyclerView.canScrollVertically(1) ? View.VISIBLE : View.GONE);
                    }
                });
    }

    private static void fitListToVisibleSheet(BottomSheetDialog dialog, RecyclerView list) {
        View sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        View container = (View) sheet.getParent();
        int bottomPadding = list.getPaddingBottom();
        Runnable updatePadding =
                () -> {
                    int hiddenHeight = Math.max(0, sheet.getBottom() - container.getHeight());
                    int padding = bottomPadding + hiddenHeight;
                    if (list.getPaddingBottom() != padding) {
                        list.setPaddingRelative(
                                list.getPaddingStart(),
                                list.getPaddingTop(),
                                list.getPaddingEnd(),
                                padding);
                    }
                };
        container.addOnLayoutChangeListener(
                (view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                        updatePadding.run());
        dialog.getBehavior()
                .addBottomSheetCallback(
                        new BottomSheetBehavior.BottomSheetCallback() {
                            @Override
                            public void onStateChanged(@NonNull View sheet, int state) {
                                // The sheet keeps its full height when collapsed. Apply the inset
                                // once it settles.
                                if (state == BottomSheetBehavior.STATE_COLLAPSED
                                        || state == BottomSheetBehavior.STATE_EXPANDED) {
                                    updatePadding.run();
                                }
                            }

                            @Override
                            public void onSlide(@NonNull View sheet, float slideOffset) {}
                        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString("sc_id", sc_id);
        savedInstanceState.putParcelable("project_file", projectFileBean);
        super.onSaveInstanceState(savedInstanceState);
    }

    private void showAddComponentDialog(ComponentBean componentBean) {
        if (!isAdded() || getParentFragmentManager().isStateSaved()) return;
        String tag = InnerAddComponentBottomSheet.class.getName();
        if (getParentFragmentManager().findFragmentByTag(tag) != null) return;
        InnerAddComponentBottomSheet.newInstance(sc_id, projectFileBean, componentBean)
                .showNow(getParentFragmentManager(), tag);
    }

    private record ComponentOption(@StringRes int title, @Nullable ComponentBean component) {}

    private static class HeadingViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;

        HeadingViewHolder(TextView title) {
            super(title);
            this.title = title;
        }
    }

    private class ComponentsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @Override
        public int getItemViewType(int position) {
            return options.get(position).title() != 0
                    ? R.layout.component_add_section_header
                    : R.layout.component_add_item;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ComponentOption option = options.get(position);
            if (holder instanceof HeadingViewHolder heading) {
                heading.title.setText(Helper.getResString(option.title()));
            } else if (holder instanceof ComponentBeanViewHolder card) {
                card.bind(option.component());
            }
        }

        @Override
        @NonNull
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == R.layout.component_add_section_header) {
                return new HeadingViewHolder((TextView) inflater.inflate(viewType, parent, false));
            }
            return new ComponentBeanViewHolder(
                    ComponentAddItemBinding.inflate(inflater, parent, false));
        }

        @Override
        public int getItemCount() {
            return options.size();
        }

        private class ComponentBeanViewHolder extends RecyclerView.ViewHolder {
            private final ComponentAddItemBinding binding;

            public ComponentBeanViewHolder(ComponentAddItemBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            public void bind(ComponentBean componentBean) {
                String componentName =
                        ComponentBean.getComponentName(itemView.getContext(), componentBean.type);
                binding.name.setText(componentName);
                binding.icon.setImageResource(ComponentBean.getIconResource(componentBean.type));
                binding.getRoot().setOnClickListener(v -> showAddComponentDialog(componentBean));
            }
        }
    }
}
