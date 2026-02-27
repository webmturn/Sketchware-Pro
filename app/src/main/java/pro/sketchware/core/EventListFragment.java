package pro.sketchware.core;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.besome.sketch.beans.BlockBean;
import com.besome.sketch.beans.ComponentBean;
import com.besome.sketch.beans.EventBean;
import com.besome.sketch.beans.MoreBlockCollectionBean;
import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.beans.ViewBean;
import com.besome.sketch.editor.LogicEditorActivity;
import com.besome.sketch.editor.event.AddEventActivity;
import com.besome.sketch.editor.event.CollapsibleEventLayout;
import com.besome.sketch.lib.base.CollapsibleViewHolder;
import com.besome.sketch.lib.ui.CollapsibleButton;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigationrail.NavigationRailView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import mod.hey.studios.moreblock.ReturnMoreblockManager;
import mod.hey.studios.moreblock.importer.MoreblockImporterDialog;
import mod.hey.studios.util.Helper;
import mod.jbk.editor.manage.MoreblockImporter;
import pro.sketchware.R;

public class EventListFragment extends BaseFragment implements View.OnClickListener, MoreblockImporterDialog.CallBack {

    private ProjectFileBean currentActivity;
    private NavigationRailView paletteView;
    private EventAdapter eventAdapter;
    private FloatingActionButton fab;
    private HashMap<Integer, ArrayList<EventBean>> events;
    private ArrayList<EventBean> moreBlocks;
    private ArrayList<EventBean> viewEvents;
    private ArrayList<EventBean> componentEvents;
    private ArrayList<EventBean> activityEvents;
    private ArrayList<EventBean> drawerViewEvents;
    private TextView noEvents;
    private MaterialButton importMoreBlockFromCollection;
    private String sc_id;
    private EditText searchInput;
    private ImageView sortMenuIcon;
    private View searchContainer;
    private final ActivityResultLauncher<Intent> addEventLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> refreshEvents());
    private final ActivityResultLauncher<Intent> openEvent = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        // in case any Events were added, e.g. a new MoreBlock
        refreshEvents();
    });

    public static int getCategoryIcon(int i) {
        return switch (i) {
            case 0 -> R.drawable.ic_mtrl_lifecycle;
            case 1 -> R.drawable.ic_mtrl_devices;
            case 2 -> R.drawable.ic_mtrl_component;
            case 3 -> R.drawable.ic_drawer;
            case 4 -> R.drawable.ic_mtrl_block;
            default -> 0;
        };
    }

    public static String getCategoryName(Context context, int i) {
        return switch (i) {
            case 0 -> Helper.getResString(R.string.common_word_activity);
            case 1 -> Helper.getResString(R.string.common_word_view);
            case 2 -> Helper.getResString(R.string.common_word_component);
            case 3 -> Helper.getResString(R.string.common_word_drawer);
            case 4 -> Helper.getResString(R.string.common_word_moreblock);
            default -> "";
        };
    }

    private int getPaletteIndex(int id) {
        if (id == R.id.activity) {
            return 0;
        } else if (id == R.id.view) {
            return 1;
        } else if (id == R.id.component) {
            return 2;
        } else if (id == R.id.drawer) {
            return 3;
        } else if (id == R.id.moreblock) {
            return 4;
        }
        return -1;
    }

    private int getPaletteIndex() {
        return getPaletteIndex(paletteView.getSelectedItemId());
    }

    @Override
    public void onClick(View v) {
        if (!UIHelper.isClickThrottled() && v.getId() == R.id.fab) {
            Intent intent = new Intent(requireActivity().getApplicationContext(), AddEventActivity.class);
            intent.putExtra("sc_id", sc_id);
            intent.putExtra("project_file", currentActivity);
            intent.putExtra("category_index", getPaletteIndex());
            addEventLauncher.launch(intent);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.fr_logic_list, container, false);
        initialize(view);
        if (savedInstanceState != null) {
            sc_id = savedInstanceState.getString("sc_id");
        } else {
            sc_id = requireActivity().getIntent().getStringExtra("sc_id");
        }
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("sc_id", sc_id);
        super.onSaveInstanceState(outState);
    }

    public ProjectFileBean getCurrentActivity() {
        return currentActivity;
    }

    public void setCurrentActivity(ProjectFileBean projectFileBean) {
        currentActivity = projectFileBean;
    }

    public void refreshEvents() {
        if (currentActivity != null) {
            moreBlocks.clear();
            viewEvents.clear();
            componentEvents.clear();
            activityEvents.clear();
            drawerViewEvents.clear();
            for (Pair<String, String> moreBlock : ProjectDataManager.getProjectDataManager(sc_id).getMoreBlocks(currentActivity.getJavaName())) {
                EventBean eventBean = new EventBean(EventBean.EVENT_TYPE_ETC, -1, moreBlock.first, "moreBlock");
                eventBean.initValue();
                moreBlocks.add(eventBean);
            }
            EventBean initLogicEvent = new EventBean(EventBean.EVENT_TYPE_ACTIVITY, -1, "onCreate", "initializeLogic");
            initLogicEvent.initValue();
            activityEvents.add(initLogicEvent);
            for (EventBean eventBean : ProjectDataManager.getProjectDataManager(sc_id).getEvents(currentActivity.getJavaName())) {
                eventBean.initValue();
                int eventType = eventBean.eventType;
                if (eventType == EventBean.EVENT_TYPE_VIEW) {
                    viewEvents.add(eventBean);
                } else if (eventType == EventBean.EVENT_TYPE_COMPONENT) {
                    componentEvents.add(eventBean);
                } else if (eventType == EventBean.EVENT_TYPE_ACTIVITY) {
                    if (!"initializeLogic".equals(eventBean.eventName)) {
                        activityEvents.add(eventBean);
                    }
                } else if (eventType == EventBean.EVENT_TYPE_DRAWER_VIEW) {
                    drawerViewEvents.add(eventBean);
                }
            }
            if (getPaletteIndex() == -1) {
                eventAdapter.setEvents(events.get(0));
                paletteView.setSelectedItemId(R.id.activity);
            }
            if (getPaletteIndex() == 4) {
                importMoreBlockFromCollection.setVisibility(View.VISIBLE);
            } else {
                importMoreBlockFromCollection.setVisibility(View.GONE);
            }
            if (eventAdapter != null) {
                eventAdapter.setEvents(events.get(getPaletteIndex()));
                eventAdapter.notifyDataSetChanged();
                restoreSearchState();
            }
        }
    }

    private void restoreSearchState() {
        if (eventAdapter != null && searchInput != null) {
            String currentQuery = eventAdapter.getCurrentSearchQuery();
            if (!currentQuery.isEmpty() && searchContainer != null) {
                searchInput.setText(currentQuery);
                searchInput.setSelection(currentQuery.length());
                if (searchContainer.getVisibility() != View.VISIBLE) {
                    searchContainer.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private void deleteMoreBlock(EventBean moreBlock, int position) {
        if (ProjectDataManager.getProjectDataManager(sc_id).isMoreBlockUsed(currentActivity.getJavaName(), moreBlock.targetId)) {
            SketchToast.warning(requireContext(), Helper.getResString(R.string.logic_editor_message_currently_used_block), 0).show();
        } else {
            ProjectDataManager.getProjectDataManager(sc_id).removeMoreBlock(currentActivity.getJavaName(), moreBlock.targetId);
            SketchToast.toast(requireContext(), Helper.getResString(R.string.common_message_complete_delete), 0).show();
            events.get(getPaletteIndex()).remove(position);
            eventAdapter.refreshAfterDelete();
        }
    }

    public void resetEventValues() {
        if (currentActivity != null) {
            for (Map.Entry<Integer, ArrayList<EventBean>> entry : events.entrySet()) {
                for (EventBean bean : entry.getValue()) {
                    bean.initValue();
                }
            }
            eventAdapter.notifyDataSetChanged();
        }
    }

    private void initialize(ViewGroup parent) {
        noEvents = parent.findViewById(R.id.tv_no_events);
        RecyclerView eventList = parent.findViewById(R.id.event_list);
        searchInput = parent.findViewById(R.id.search_events);
        sortMenuIcon = parent.findViewById(R.id.sort_menu);
        searchContainer = parent.findViewById(R.id.search_container);
        paletteView = parent.findViewById(R.id.palette);
        paletteView.setOnItemSelectedListener(
                item -> {
                    initializeEvents(events.get(getPaletteIndex(item.getItemId())));
                    if (getPaletteIndex(item.getItemId()) == 4) {
                        importMoreBlockFromCollection.setVisibility(View.VISIBLE);
                    } else {
                        importMoreBlockFromCollection.setVisibility(View.GONE);
                    }
                    eventAdapter.setEvents(events.get(getPaletteIndex(item.getItemId())));
                    eventAdapter.notifyDataSetChanged();
                    return true;
                });
        fab = parent.findViewById(R.id.fab);
        noEvents.setVisibility(View.GONE);
        noEvents.setText(R.string.event_message_no_events);
        eventList.setLayoutManager(new LinearLayoutManager(null, RecyclerView.VERTICAL, false));
        eventAdapter = new EventAdapter();
        eventList.setAdapter(eventAdapter);
        fab.setOnClickListener(this);
        events = new HashMap<>();
        moreBlocks = new ArrayList<>();
        viewEvents = new ArrayList<>();
        componentEvents = new ArrayList<>();
        activityEvents = new ArrayList<>();
        drawerViewEvents = new ArrayList<>();
        events.put(0, activityEvents);
        events.put(1, viewEvents);
        events.put(2, componentEvents);
        events.put(3, drawerViewEvents);
        events.put(4, moreBlocks);
        importMoreBlockFromCollection = parent.findViewById(R.id.tv_import);
        importMoreBlockFromCollection.setText(R.string.logic_button_import_more_block);
        importMoreBlockFromCollection.setOnClickListener(v -> showImportMoreBlockFromCollectionsDialog());
        setupSearchAndSort(parent);
    }

    private void setupSearchAndSort(ViewGroup parent) {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (eventAdapter != null) {
                    eventAdapter.filterEvents(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        sortMenuIcon.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(requireContext(), sortMenuIcon);
            popup.getMenuInflater().inflate(R.menu.logic_sort_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.sort_default) {
                    if (eventAdapter != null) eventAdapter.setSortMode(0);
                    return true;
                } else if (id == R.id.sort_alphabetical) {
                    if (eventAdapter != null) eventAdapter.setSortMode(1);
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    public void toggleSearchBar() {
        if (searchContainer == null) return;
        
        if (searchContainer.getVisibility() == View.VISIBLE) {
            searchContainer.setVisibility(View.GONE);
            searchInput.setText("");
            searchInput.clearFocus();
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
            }
        } else {
            searchContainer.setVisibility(View.VISIBLE);
            searchInput.requestFocus();
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
            }
        }
    }

    private void showSaveMoreBlockToCollectionsDialog(int moreBlockPosition) {
        MaterialAlertDialogBuilder aBVar = new MaterialAlertDialogBuilder(requireActivity());
        aBVar.setTitle(R.string.logic_more_block_favorites_save_title);
        aBVar.setIcon(R.drawable.ic_bookmark_red_48dp);
        View dialogView = ViewUtil.inflateLayout(requireContext(), R.layout.property_popup_save_to_favorite);
        ((TextView) dialogView.findViewById(R.id.tv_favorites_guide)).setText(R.string.logic_more_block_favorites_save_guide);
        EditText editText = dialogView.findViewById(R.id.ed_input);
        editText.setPrivateImeOptions("defaultInputmode=english;");
        editText.setLines(1);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        UniqueNameValidator nameValidator = new UniqueNameValidator(requireContext(), dialogView.findViewById(R.id.ti_input), MoreBlockCollectionManager.getInstance().getMoreBlockNames());
        aBVar.setView(dialogView);
        aBVar.setPositiveButton(R.string.common_word_save, (v, which) -> {
            if (nameValidator.isValid()) {
                saveMoreBlockToCollection(Helper.getText(editText), moreBlocks.get(moreBlockPosition));
                UIHelper.hideKeyboard(requireContext(), editText);
                v.dismiss();
            }
        });
        aBVar.setNegativeButton(R.string.common_word_cancel, (v, which) -> {
            UIHelper.hideKeyboard(requireContext(), editText);
            v.dismiss();
        });
        aBVar.show();
    }

    private void resetEvent(EventBean event) {
        ProjectDataStore projectDataStore = ProjectDataManager.getProjectDataManager(sc_id);
        String javaName = currentActivity.getJavaName();
        projectDataStore.putBlocks(javaName, event.targetId + "_" + event.eventName, new ArrayList<>());
        SketchToast.toast(requireContext(), Helper.getResString(R.string.common_message_complete_reset), 0).show();
    }

    @Override
    public void onSelected(MoreBlockCollectionBean bean) {
        new MoreblockImporter(requireActivity(), sc_id, currentActivity).importMoreblock(bean, this::refreshEvents);
    }

    private void showImportMoreBlockFromCollectionsDialog() {
        ArrayList<MoreBlockCollectionBean> moreBlocksInCollections = MoreBlockCollectionManager.getInstance().getMoreBlocks();
        new MoreblockImporterDialog(requireActivity(), moreBlocksInCollections, this).show();
    }

    private void deleteEvent(EventBean event, int position) {
        EventBean.deleteEvent(sc_id, event, currentActivity);
        SketchToast.toast(requireContext(), Helper.getResString(R.string.common_message_complete_delete), 0).show();
        events.get(getPaletteIndex()).remove(position);
        eventAdapter.refreshAfterDelete();
    }

    private void initializeEvents(ArrayList<EventBean> events) {
        for (EventBean bean : events) {
            bean.initValue();
        }
    }

    private void openEvent(String targetId, String eventId, String description) {
        Intent intent = new Intent(requireActivity(), LogicEditorActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("sc_id", sc_id);
        intent.putExtra("id", targetId);
        intent.putExtra("event", eventId);
        intent.putExtra("project_file", currentActivity);
        intent.putExtra("event_text", description);
        openEvent.launch(intent);
    }

    private void saveMoreBlockToCollection(String moreBlockName, EventBean moreBlock) {
        String moreBlockSpec = ProjectDataManager.getProjectDataManager(sc_id).getMoreBlockSpec(currentActivity.getJavaName(), moreBlock.targetId);
        ProjectDataStore projectDataStore = ProjectDataManager.getProjectDataManager(sc_id);
        String javaName = currentActivity.getJavaName();
        ArrayList<BlockBean> moreBlockBlocks = projectDataStore.getBlocks(javaName, moreBlock.targetId + "_" + moreBlock.eventName);

        boolean hasAnyBlocks = false;
        boolean failedToAddResourceToCollections = false;
        for (BlockBean next : moreBlockBlocks) {
            ArrayList<ClassInfo> classInfo = next.getParamClassInfo();
            if (!classInfo.isEmpty()) {
                for (int i = 0; i < classInfo.size(); i++) {
                    ClassInfo paramType = classInfo.get(i);
                    String parameter = next.parameters.get(i);

                    if (paramType.isExactType("resource") || paramType.isExactType("resource_bg")) {
                        if (ProjectDataManager.getResourceManager(sc_id).hasImage(parameter) && !ImageCollectionManager.getInstance().hasResource(parameter)) {
                            try { ImageCollectionManager.getInstance().addResource(sc_id, ProjectDataManager.getResourceManager(sc_id).getImageBean(parameter)); } catch (CompileException ignored) {}
                        }
                    } else if (paramType.isExactType("sound")) {
                        if (ProjectDataManager.getResourceManager(sc_id).hasSound(parameter) && !SoundCollectionManager.getInstance().hasResource(parameter)) {
                            try {
                                SoundCollectionManager.getInstance().addResource(sc_id, ProjectDataManager.getResourceManager(sc_id).getSoundBean(parameter));
                            } catch (Exception unused) {
                                failedToAddResourceToCollections = true;
                            }
                        }
                    } else if (paramType.isExactType("font")) {
                        if (ProjectDataManager.getResourceManager(sc_id).hasFont(parameter) && !FontCollectionManager.getInstance().hasResource(parameter)) {
                            try { FontCollectionManager.getInstance().addResource(sc_id, ProjectDataManager.getResourceManager(sc_id).getFontBean(parameter)); } catch (CompileException ignored) {}
                        }
                    }
                }
                hasAnyBlocks = true;
            }
        }
        if (hasAnyBlocks) {
            if (failedToAddResourceToCollections) {
                SketchToast.warning(requireContext(), Helper.getResString(R.string.logic_more_block_message_missed_resource_exist), 0).show();
            } else {
                SketchToast.toast(requireContext(), Helper.getResString(R.string.logic_more_block_message_resource_added), 0).show();
            }
        }
        try {
            MoreBlockCollectionManager.getInstance().addMoreBlock(moreBlockName, moreBlockSpec, moreBlockBlocks, true);
        } catch (Exception saveException) {
            SketchToast.warning(requireContext(), Helper.getResString(R.string.common_error_failed_to_save), 0).show();
        }
    }

    private class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {
        private ArrayList<EventBean> currentCategoryEvents = new ArrayList<>();
        private ArrayList<EventBean> filteredEvents = new ArrayList<>();
        private String searchQuery = "";
        private int sortMode = 0;

        @Override
        public int getItemCount() {
            return getActiveList().size();
        }

        private ArrayList<EventBean> getActiveList() {
            return searchQuery.isEmpty() ? currentCategoryEvents : filteredEvents;
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            EventBean eventBean = getActiveList().get(position);
            holder.targetType.setVisibility(View.VISIBLE);
            holder.previewContainer.setVisibility(View.VISIBLE);
            holder.preview.setVisibility(View.VISIBLE);
            holder.preview.setImageResource(EventRegistry.getEventIconResource(eventBean.eventName));
            holder.optionsLayout.showDelete();
            if (eventBean.eventType == EventBean.EVENT_TYPE_ETC) {
                holder.optionsLayout.showAddToCollection();
            } else {
                holder.optionsLayout.hideAddToCollection();
            }
            if (eventBean.eventType == EventBean.EVENT_TYPE_ACTIVITY) {
                if (eventBean.eventName.equals("initializeLogic")) {
                    holder.optionsLayout.hideDelete();
                }
                holder.targetId.setText(eventBean.targetId);
                holder.type.setBackgroundResource(EventRegistry.getEventIconResource(eventBean.eventName));
                holder.name.setText(eventBean.eventName);
                holder.description.setText(EventRegistry.getEventName(eventBean.eventName));
                holder.icon.setImageResource(R.drawable.ic_mtrl_code);
                holder.preview.setVisibility(View.GONE);
                holder.targetType.setVisibility(View.GONE);
            } else {
                holder.icon.setImageResource(EventBean.getEventIconResource(eventBean.eventType, eventBean.targetType));
                if (eventBean.eventType == EventBean.EVENT_TYPE_VIEW) {
                    holder.targetType.setText(ViewBean.getViewTypeName(eventBean.targetType));
                } else if (eventBean.eventType == EventBean.EVENT_TYPE_DRAWER_VIEW) {
                    holder.targetType.setText(ViewBean.getViewTypeName(eventBean.targetType));
                } else if (eventBean.eventType == EventBean.EVENT_TYPE_COMPONENT) {
                    holder.targetType.setText(ComponentBean.getComponentName(requireContext(), eventBean.targetType));
                } else if (eventBean.eventType == EventBean.EVENT_TYPE_ETC) {
                    holder.icon.setImageResource(R.drawable.ic_mtrl_code);
                    holder.targetType.setVisibility(View.GONE);
                    holder.preview.setVisibility(View.GONE);
                }
                if (eventBean.targetId.equals("_fab")) {
                    holder.targetId.setText("fab");
                } else {
                    holder.targetId.setText(ReturnMoreblockManager.getMbName(eventBean.targetId));
                }
                holder.type.setText(EventBean.getEventTypeName(eventBean.eventType));
                holder.type.setBackgroundResource(EventBean.getEventTypeBgRes(eventBean.eventType));
                holder.name.setText(eventBean.eventName);
                holder.description.setText(EventRegistry.getEventName(eventBean.eventName));
                if (eventBean.eventType == EventBean.EVENT_TYPE_ETC) {
                    holder.description.setText(ReturnMoreblockManager.getMbTypeList(eventBean.targetId));
                }
            }
            if (eventBean.isCollapsed) {
                holder.optionContainer.setVisibility(View.GONE);
                holder.menu.setRotation(0);
            } else {
                holder.optionContainer.setVisibility(View.VISIBLE);
                holder.menu.setRotation(-180);
            }
            if (eventBean.isConfirmation) {
                if (holder.shouldAnimateNextTransformation()) {
                    holder.optionsLayout.showConfirmation();
                    holder.setAnimateNextTransformation(false);
                } else {
                    holder.optionsLayout.showConfirmationWithoutAnimation();
                }
            } else {
                if (holder.shouldAnimateNextTransformation()) {
                    holder.optionsLayout.hideConfirmation();
                } else {
                    holder.optionsLayout.hideConfirmationWithoutAnimation();
                }
            }
            holder.optionContainer.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
        }

        public void setEvents(ArrayList<EventBean> arrayList) {
            currentCategoryEvents = arrayList;
            String previousQuery = searchQuery;
            searchQuery = "";
            filteredEvents.clear();
            
            if (!previousQuery.isEmpty()) {
                searchQuery = previousQuery;
                applyFilterAndSort();
            } else {
                applySorting();
                updateEmptyState();
            }
        }

        public void filterEvents(String query) {
            searchQuery = query.toLowerCase().trim();
            applyFilterAndSort();
        }

        public String getCurrentSearchQuery() {
            return searchQuery;
        }

        public void setSortMode(int mode) {
            sortMode = mode;
            applyFilterAndSort();
        }

        public void refreshAfterDelete() {
            if (!searchQuery.isEmpty()) {
                applyFilterAndSort();
            } else {
                updateEmptyState();
                notifyDataSetChanged();
            }
        }

        private void applyFilterAndSort() {
            filteredEvents.clear();

            if (!searchQuery.isEmpty()) {
                for (EventBean event : currentCategoryEvents) {
                    String eventName = event.eventName.toLowerCase();
                    String targetId = event.targetId.toLowerCase();
                    if (eventName.contains(searchQuery) || targetId.contains(searchQuery)) {
                        filteredEvents.add(event);
                    }
                }
            }

            applySorting();
            updateEmptyState();
            notifyDataSetChanged();
        }

        private void applySorting() {
            ArrayList<EventBean> listToSort = getActiveList();
            if (listToSort.isEmpty()) return;

            if (sortMode == 1) {
                Collections.sort(listToSort, (a, b) -> 
                    a.eventName.compareToIgnoreCase(b.eventName));
            }
        }

        private void updateEmptyState() {
            if (getActiveList().isEmpty()) {
                noEvents.setVisibility(View.VISIBLE);
            } else {
                noEvents.setVisibility(View.GONE);
            }
        }

        @Override
        @NonNull
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.fr_logic_list_item, parent, false));
        }

        private class ViewHolder extends CollapsibleViewHolder {
            public final MaterialCardView root;
            public final ImageView menu;
            public final ImageView preview;
            public final LinearLayout previewContainer;
            public final LinearLayout optionContainer;
            public final CollapsibleEventLayout optionsLayout;
            public final ImageView icon;
            public final TextView targetType;
            public final TextView targetId;
            public final TextView type;
            public final TextView name;
            public final TextView description;

            public ViewHolder(View itemView) {
                super(itemView, 200);
                root = (MaterialCardView) itemView;
                icon = itemView.findViewById(R.id.img_icon);
                targetType = itemView.findViewById(R.id.tv_target_type);
                targetId = itemView.findViewById(R.id.tv_target_id);
                type = itemView.findViewById(R.id.tv_event_type);
                name = itemView.findViewById(R.id.tv_event_name);
                description = itemView.findViewById(R.id.tv_event_text);
                menu = itemView.findViewById(R.id.img_menu);
                preview = itemView.findViewById(R.id.img_preview);
                previewContainer = itemView.findViewById(R.id.ll_preview);
                optionContainer = itemView.findViewById(R.id.event_option_layout);
                optionsLayout = itemView.findViewById(R.id.event_option);
                optionsLayout.setButtonOnClickListener(v -> {
                    if (!UIHelper.isClickThrottled()) {
                        EventBean eventBean = getActiveList().get(getLayoutPosition());
                        if (v instanceof CollapsibleButton button) {
                            setAnimateNextTransformation(true);
                            int id = button.getButtonId();
                            if (id == 2) {
                                eventBean.buttonPressed = id;
                                eventBean.isConfirmation = false;
                                eventBean.isCollapsed = false;
                                notifyItemChanged(getLayoutPosition());
                                showSaveMoreBlockToCollectionsDialog(getLayoutPosition());
                            } else {
                                eventBean.buttonPressed = id;
                                eventBean.isConfirmation = true;
                                notifyItemChanged(getLayoutPosition());
                            }
                        } else {
                            if (v.getId() == R.id.confirm_no) {
                                eventBean.isConfirmation = false;
                                setAnimateNextTransformation(true);
                                notifyItemChanged(getLayoutPosition());
                            } else if (v.getId() == R.id.confirm_yes) {
                                if (eventBean.buttonPressed == 0) {
                                    eventBean.isConfirmation = false;
                                    eventBean.isCollapsed = true;
                                    setAnimateNextTransformation(true);
                                    resetEvent(eventBean);
                                    notifyItemChanged(getLayoutPosition());
                                } else if (eventBean.buttonPressed == 1) {
                                    eventBean.isConfirmation = false;
                                    int originalPosition = currentCategoryEvents.indexOf(eventBean);
                                    if (getPaletteIndex() != 4) {
                                        deleteEvent(eventBean, originalPosition);
                                    } else {
                                        deleteMoreBlock(eventBean, originalPosition);
                                    }
                                }
                                fab.show();
                            }
                        }
                    }
                });
                onDoneInitializingViews();
                root.setOnClickListener(v -> {
                    if (!UIHelper.isClickThrottled()) {
                        EventBean eventBean = getActiveList().get(getLayoutPosition());
                        openEvent(eventBean.targetId, eventBean.eventName, Helper.getText(description));
                    }
                });
            }

            @Override
            protected boolean isCollapsed() {
                return getActiveList().get(getLayoutPosition()).isCollapsed;
            }

            @Override
            protected void setIsCollapsed(boolean isCollapsed) {
                getActiveList().get(getLayoutPosition()).isCollapsed = isCollapsed;
            }

            @NonNull
            @Override
            protected ViewGroup getOptionsLayout() {
                return optionContainer;
            }

            @NonNull
            @Override
            protected Set<? extends View> getOnClickCollapseTriggerViews() {
                return Set.of(menu);
            }

            @NonNull
            @Override
            protected Set<? extends View> getOnLongClickCollapseTriggerViews() {
                return Set.of(root);
            }
        }
    }
}
