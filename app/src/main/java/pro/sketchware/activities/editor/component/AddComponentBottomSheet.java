package pro.sketchware.activities.editor.component;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import pro.sketchware.beans.ComponentBean;
import pro.sketchware.beans.ProjectFileBean;
import com.google.android.flexbox.AlignItems;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;

import pro.sketchware.util.Helper;
import pro.sketchware.core.project.ComponentsHandler;
import pro.sketchware.R;
import pro.sketchware.databinding.ComponentAddItemBinding;
import pro.sketchware.databinding.LogicAddComponentBinding;
import pro.sketchware.dialogs.InnerAddComponentBottomSheet;

public class AddComponentBottomSheet extends BottomSheetDialogFragment {
    public static final String RESULT_JAVA_FILE = "java_file";
    private String sc_id;
    private ProjectFileBean projectFileBean;

    private ArrayList<ComponentBean> componentList;
    private LogicAddComponentBinding binding;

    public static AddComponentBottomSheet newInstance(String scId, ProjectFileBean projectFileBean) {
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
                    && projectId.equals(selector.sc_id) && selector.projectFileBean != null
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

        initializeComponentBeans();
    }

    @NonNull
    @Override
    public BottomSheetDialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        dialog.getBehavior().setSkipCollapsed(true);
        return dialog;
    }

    private void initializeComponentBeans() {
        componentList = new ArrayList<>();
        componentList.add(new ComponentBean(ComponentBean.COMPONENT_TYPE_INTENT));
        componentList.add(new ComponentBean(ComponentBean.COMPONENT_TYPE_SHAREDPREF));
        componentList.add(new ComponentBean(ComponentBean.COMPONENT_TYPE_FILE_PICKER));
        componentList.add(new ComponentBean(ComponentBean.COMPONENT_TYPE_CALENDAR));
        componentList.add(new ComponentBean(ComponentBean.COMPONENT_TYPE_VIBRATOR));
        componentList.add(new ComponentBean(ComponentBean.COMPONENT_TYPE_TIMERTASK));
        componentList.add(new ComponentBean(ComponentBean.COMPONENT_TYPE_DIALOG));
        componentList.add(new ComponentBean(ComponentBean.COMPONENT_TYPE_MEDIAPLAYER));
        componentList.add(new ComponentBean(ComponentBean.COMPONENT_TYPE_SOUNDPOOL));
        componentList.add(new ComponentBean(ComponentBean.COMPONENT_TYPE_OBJECTANIMATOR));
        componentList.add(new ComponentBean(ComponentBean.COMPONENT_TYPE_CAMERA));
        componentList.add(new ComponentBean(ComponentBean.COMPONENT_TYPE_GYROSCOPE));
        componentList.add(new ComponentBean(ComponentBean.COMPONENT_TYPE_TEXT_TO_SPEECH));
        componentList.add(new ComponentBean(ComponentBean.COMPONENT_TYPE_SPEECH_TO_TEXT));
        componentList.add(new ComponentBean(ComponentBean.COMPONENT_TYPE_REQUEST_NETWORK));
        componentList.add(new ComponentBean(ComponentBean.COMPONENT_TYPE_BLUETOOTH_CONNECT));
        componentList.add(new ComponentBean(ComponentBean.COMPONENT_TYPE_LOCATION_MANAGER));
        componentList.add(new ComponentBean(ComponentBean.COMPONENT_TYPE_PROGRESS_DIALOG));
        componentList.add(new ComponentBean(ComponentBean.COMPONENT_TYPE_DATE_PICKER_DIALOG));
        componentList.add(new ComponentBean(ComponentBean.COMPONENT_TYPE_TIME_PICKER_DIALOG));
        componentList.add(new ComponentBean(ComponentBean.COMPONENT_TYPE_NOTIFICATION));
        componentList.add(new ComponentBean(ComponentBean.COMPONENT_TYPE_SQLITE));
        componentList.add(new ComponentBean(ComponentBean.COMPONENT_TYPE_FRAGMENT_ADAPTER));

        // Ads
        componentList.add(new ComponentBean(ComponentBean.COMPONENT_TYPE_INTERSTITIAL_AD));
        componentList.add(new ComponentBean(ComponentBean.COMPONENT_TYPE_REWARDED_VIDEO_AD));

        // Firebase
        componentList.add(new ComponentBean(ComponentBean.COMPONENT_TYPE_FIREBASE));
        componentList.add(new ComponentBean(ComponentBean.COMPONENT_TYPE_FIREBASE_AUTH));
        componentList.add(new ComponentBean(ComponentBean.COMPONENT_TYPE_FIREBASE_STORAGE));
        componentList.add(new ComponentBean(ComponentBean.COMPONENT_TYPE_FIREBASE_AUTH_PHONE));
        componentList.add(new ComponentBean(ComponentBean.COMPONENT_TYPE_FIREBASE_CLOUD_MESSAGE));
        componentList.add(new ComponentBean(ComponentBean.COMPONENT_TYPE_FIREBASE_AUTH_GOOGLE_LOGIN));

        ComponentsHandler.addCustomComponents(componentList);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = LogicAddComponentBinding.inflate(getLayoutInflater());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (sc_id == null || projectFileBean == null) return;

        FlexboxLayoutManager flexboxLayoutManager = new FlexboxLayoutManager(getContext(), FlexDirection.ROW, FlexWrap.WRAP);
        flexboxLayoutManager.setJustifyContent(JustifyContent.SPACE_BETWEEN);
        flexboxLayoutManager.setAlignItems(AlignItems.CENTER);

        binding.title.setText(Helper.getResString(R.string.component_title_add_component));
        binding.componentList.setHasFixedSize(true);
        binding.componentList.setAdapter(new ComponentsAdapter());
        binding.componentList.setLayoutManager(flexboxLayoutManager);

        // Keep list gestures in the list, even after scrolling to an edge.
        BottomSheetBehavior<?> behavior = ((BottomSheetDialog) requireDialog()).getBehavior();
        binding.componentList.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView recyclerView, @NonNull MotionEvent event) {
                int action = event.getActionMasked();
                behavior.setDraggable(action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL);
                return false;
            }
        });

        binding.componentList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (binding == null || recyclerView != binding.componentList
                        || recyclerView.getAdapter() == null) return;
                FlexboxLayoutManager lm = (FlexboxLayoutManager) recyclerView.getLayoutManager();
                if (lm == null) return;
                int first = lm.findFirstCompletelyVisibleItemPosition();
                int last = lm.findLastCompletelyVisibleItemPosition();
                int total = binding.componentList.getAdapter().getItemCount();
                binding.dividerTop.setVisibility(first > 0 ? View.VISIBLE : View.GONE);
                binding.dividerBottom.setVisibility(last < total - 1 ? View.VISIBLE : View.GONE);
            }
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

    private class ComponentsAdapter extends RecyclerView.Adapter<ComponentsAdapter.ComponentBeanViewHolder> {
        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public void onBindViewHolder(@NonNull ComponentBeanViewHolder holder, int position) {
            var componentBean = componentList.get(position);
            holder.bind(componentBean);
        }

        @Override
        @NonNull
        public ComponentBeanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ComponentAddItemBinding binding = ComponentAddItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ComponentBeanViewHolder(binding);
        }

        @Override
        public int getItemCount() {
            return componentList.size();
        }

        private class ComponentBeanViewHolder extends RecyclerView.ViewHolder {
            private final ComponentAddItemBinding binding;

            public ComponentBeanViewHolder(ComponentAddItemBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            public void bind(ComponentBean componentBean) {
                String componentName = ComponentBean.getComponentName(itemView.getContext(), componentBean.type);
                binding.name.setText(componentName);
                binding.icon.setImageResource(ComponentBean.getIconResource(componentBean.type));
                binding.getRoot().setOnClickListener(v -> showAddComponentDialog(componentBean));
            }
        }
    }
}
