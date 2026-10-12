package pro.sketchware.activities.settings.fragments.events;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import java.util.ArrayList;
import java.util.Collections;

import pro.sketchware.activities.base.BaseFragment;
import dev.pranav.filepicker.FilePickerCallback;
import dev.pranav.filepicker.FilePickerDialogFragment;
import dev.pranav.filepicker.FilePickerOptions;
import pro.sketchware.util.CustomEventsStore;
import pro.sketchware.core.codegen.EventsHandler;
import pro.sketchware.util.Helper;
import pro.sketchware.util.LogUtil;
import pro.sketchware.beans.CustomEvent;
import pro.sketchware.beans.CustomListener;
import pro.sketchware.R;
import pro.sketchware.databinding.DialogAddNewListenerBinding;
import pro.sketchware.databinding.FragmentEventsManagerBinding;
import pro.sketchware.databinding.LayoutEventItemBinding;
import pro.sketchware.activities.settings.fragments.events.details.EventsManagerDetailsFragment;
import pro.sketchware.util.FileUtil;
import pro.sketchware.util.SketchwareUtil;
import pro.sketchware.util.UI;

public class EventsManagerFragment extends BaseFragment {

    private FragmentEventsManagerBinding binding;
    private ArrayList<CustomListener> listMap = new ArrayList<>();

    public static String getNumOfEvents(String name) {
        int eventAmount = 0;
        try {
            for (CustomEvent event : CustomEventsStore.INSTANCE.readEvents()) {
                if (name.equals(event.getListener())) eventAmount++;
            }
        } catch (IOException e) {
            LogUtil.w("EventsManagerFragment", "Could not count events", e);
        }
        return String.format(Helper.getResString(R.string.app_settings_events_count), eventAmount);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEventsManagerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        configureToolbar(binding.toolbar);
        binding.toolbar.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_import_events) {
                showImportEventsDialog();
                return true;
            } else if (itemId == R.id.action_export_events) {
                exportAllEvents();
                return true;
            }
            return false;
        });
        binding.activityEvents.setOnClickListener(v -> openFragment(R.id.settings_fragment_container, new EventsManagerDetailsFragment()));
        binding.activityEventsDescription.setText(getNumOfEvents(""));
        binding.fabNewListener.setOnClickListener(v -> showAddNewListenerDialog());
        refreshList();
        UI.addSystemWindowInsetToPadding(binding.appBarLayout, true, true, true, true);
        UI.addSystemWindowInsetToPadding(binding.content, true, false, true, true);
        UI.addSystemWindowInsetToMargin(binding.fabNewListener, false, false, true, true);
    }

    private void showAddNewListenerDialog() {
        showListenerDialog(null, -1);
    }

    private void showEditListenerDialog(int position) {
        if (position < 0 || position >= listMap.size()) {
            SketchwareUtil.toastError(Helper.getResString(R.string.common_error_an_error_occurred));
            return;
        }
        showListenerDialog(listMap.get(position), position);
    }

    private void showListenerDialog(@Nullable CustomListener existingListener, int position) {
        var listenerBinding = DialogAddNewListenerBinding.inflate(LayoutInflater.from(requireContext()));
        if (existingListener != null) {
            listenerBinding.listenerName.setText(existingListener.getName());
            listenerBinding.listenerCode.setText(existingListener.getCode());
            listenerBinding.listenerCustomImport.setText(existingListener.getImports());
            if (existingListener.isIndependent()) {
                listenerBinding.listenerIsIndependentClassOrMethod.setChecked(true);
                String independentPrefix = "//" + existingListener.getName() + "\n";
                String listenerCode = existingListener.getCode();
                if (listenerCode.startsWith(independentPrefix)) {
                    listenerBinding.listenerCode.setText(listenerCode.substring(independentPrefix.length()));
                }
            }
        }

        String original = existingListener == null ? null : new Gson().toJson(existingListener);
        var dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(existingListener == null ? R.string.events_new_listener : R.string.events_edit_listener)
                .setView(listenerBinding.getRoot())
                .setPositiveButton(R.string.common_word_save, null)
                .setNegativeButton(R.string.common_word_cancel, null).create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String listenerName = Helper.getText(listenerBinding.listenerName);
            if (listenerName.isEmpty()) {
                listenerBinding.listenerName.setError(Helper.getResString(R.string.error_invalid_name));
                return;
            }
            try {
                ArrayList<CustomListener> listeners = CustomEventsStore.INSTANCE.readListeners();
                ArrayList<CustomEvent> events = CustomEventsStore.INSTANCE.readEvents();
                int selected = original == null ? -1 : findListener(listeners, original);
                if (original != null && selected < 0) {
                    SketchwareUtil.toastError(Helper.getResString(R.string.common_error_an_error_occurred));
                    return;
                }
                for (int i = 0; i < listeners.size(); i++) {
                    if (i != selected && listenerName.equals(listeners.get(i).getName())) {
                        listenerBinding.listenerName.setError(Helper.getResString(R.string.error_item_already_exists));
                        return;
                    }
                }
                CustomListener listenerData = new CustomListener();
                listenerData.setName(listenerName);
                listenerData.setCode(listenerBinding.listenerIsIndependentClassOrMethod.isChecked()
                        ? "//" + listenerName + "\n" + Helper.getText(listenerBinding.listenerCode)
                        : Helper.getText(listenerBinding.listenerCode));
                listenerData.setIndependentFlag(listenerBinding.listenerIsIndependentClassOrMethod.isChecked() ? "true" : "false");
                listenerData.setImports(Helper.getText(listenerBinding.listenerCustomImport));
                if (selected >= 0) {
                    String oldName = listeners.get(selected).getName();
                    if (!oldName.equals(listenerName)) {
                        // Existing duplicate listener names cannot be split without knowing which events belong to each.
                        for (int i = 0; i < listeners.size(); i++) {
                            if (i != selected && oldName.equals(listeners.get(i).getName())) {
                                listenerBinding.listenerName.setError(Helper.getResString(R.string.error_item_already_exists));
                                return;
                            }
                        }
                        for (CustomEvent event : events) {
                            if (oldName.equals(event.getListener())) event.setListener(listenerName);
                        }
                    }
                    listeners.set(selected, listenerData);
                } else listeners.add(listenerData);
                CustomEventsStore.INSTANCE.saveBoth(events, listeners);
                refreshCodegen();
                refreshList();
                dialog.dismiss();
            } catch (IOException e) {
                LogUtil.w("EventsManagerFragment", "Could not save listener", e);
                SketchwareUtil.toastError(Helper.getResString(R.string.common_error_failed_to_save));
            }
        });
    }

    private int findListener(ArrayList<CustomListener> listeners, String original) {
        int found = -1;
        for (int i = 0; i < listeners.size(); i++) {
            if (original.equals(new Gson().toJson(listeners.get(i)))) {
                if (found >= 0) return -1;
                found = i;
            }
        }
        return found;
    }

    private static void refreshCodegen() {
        EventsHandler.refreshCachedCustomEvents();
        EventsHandler.refreshCachedCustomListeners();
    }

    public void refreshList() {
        try {
            listMap = CustomEventsStore.INSTANCE.readListeners();
        } catch (IOException e) {
            listMap = new ArrayList<>();
            SketchwareUtil.toastError(Helper.getResString(R.string.error_invalid_file));
        }
        Collections.reverse(listMap);
        binding.listenersRecyclerView.setAdapter(new ListenersAdapter(listMap, requireContext()));
        binding.activityEventsDescription.setText(getNumOfEvents(""));
    }

    private void showImportEventsDialog() {
        FilePickerOptions options = new FilePickerOptions();
        options.setTitle(Helper.getResString(R.string.file_picker_select_txt));
        options.setExtensions(new String[]{"txt"});

        FilePickerCallback callback = new FilePickerCallback() {
            @Override
            public void onFileSelected(File file) {
                if (FileUtil.readFile(file.getAbsolutePath()).isEmpty()) {
                    SketchwareUtil.toastError(Helper.getResString(R.string.error_file_empty));
                } else if (FileUtil.readFile(file.getAbsolutePath()).equals("[]")) {
                    SketchwareUtil.toastError(Helper.getResString(R.string.error_file_empty));
                } else {
                    try {
                        String[] split = FileUtil.readFile(file.getAbsolutePath()).split("\n");
                        ArrayList<CustomListener> importedListeners =
                                new Gson().fromJson(split[0], new TypeToken<ArrayList<CustomListener>>(){}.getType());
                        ArrayList<CustomEvent> importedEvents =
                                new Gson().fromJson(split[1], new TypeToken<ArrayList<CustomEvent>>(){}.getType());
                        if (importedListeners == null || importedEvents == null) {
                            SketchwareUtil.toastError(Helper.getResString(R.string.error_invalid_file));
                            return;
                        }
                        importEvents(importedListeners, importedEvents);
                    } catch (JsonSyntaxException | ArrayIndexOutOfBoundsException e) {
                        SketchwareUtil.toastError(Helper.getResString(R.string.error_invalid_file));
                    }
                }
            }

        };
        FilePickerDialogFragment filePickerDialog = new FilePickerDialogFragment(options, callback);

        filePickerDialog.show(getChildFragmentManager(), "filePickerDialog");
    }

    private void importEvents(ArrayList<CustomListener> data, ArrayList<CustomEvent> data2) {
        if (data == null || data2 == null || data.contains(null) || data2.contains(null)) {
            SketchwareUtil.toastError(Helper.getResString(R.string.error_invalid_file));
            return;
        }
        try {
            ArrayList<CustomListener> listeners = CustomEventsStore.INSTANCE.readListeners();
            ArrayList<CustomEvent> events = CustomEventsStore.INSTANCE.readEvents();
            Set<String> listenerNames = new HashSet<>();
            Set<String> eventNames = new HashSet<>();
            for (CustomListener listener : listeners) listenerNames.add(listener.getName());
            for (CustomEvent event : events) eventNames.add(event.getName());
            for (CustomListener listener : data) {
                if (listener.getName().isEmpty()) {
                    SketchwareUtil.toastError(Helper.getResString(R.string.error_invalid_file));
                    return;
                }
                if (!listenerNames.add(listener.getName())) {
                    SketchwareUtil.toastError(Helper.getResString(R.string.error_item_already_exists));
                    return;
                }
            }
            for (CustomEvent event : data2) {
                if (event.getName().isEmpty() || (!event.getListener().isEmpty() && !listenerNames.contains(event.getListener()))) {
                    SketchwareUtil.toastError(Helper.getResString(R.string.error_invalid_file));
                    return;
                }
                if (!eventNames.add(event.getName())) {
                    SketchwareUtil.toastError(Helper.getResString(R.string.error_item_already_exists));
                    return;
                }
            }
            events.addAll(data2);
            listeners.addAll(data);
            CustomEventsStore.INSTANCE.saveBoth(events, listeners);
            refreshCodegen();
            refreshList();
            SketchwareUtil.toast(Helper.getResString(R.string.toast_events_imported));
        } catch (IOException e) {
            LogUtil.w("EventsManagerFragment", "Could not import definitions", e);
            SketchwareUtil.toastError(Helper.getResString(R.string.common_error_failed_to_save));
        }
    }

    private void exportListener(int p) {
        if (p < 0 || p >= listMap.size()) {
            SketchwareUtil.toastError(Helper.getResString(R.string.common_error_an_error_occurred));
            return;
        }
        String concat = EventsManagerConstants.EVENT_EXPORT_LOCATION.getAbsolutePath() + File.separator;
        ArrayList<CustomListener> ex = new ArrayList<>();
        ex.add(listMap.get(p));
        ArrayList<CustomEvent> ex2 = new ArrayList<>();
        if (FileUtil.isExistFile(EventsManagerConstants.EVENTS_FILE.getAbsolutePath())) {
            ArrayList<CustomEvent> events;
            try {
                events = new Gson().fromJson(FileUtil.readFile(EventsManagerConstants.EVENTS_FILE.getAbsolutePath()),
                        new TypeToken<ArrayList<CustomEvent>>(){}.getType());
                if (events == null) {
                    events = new ArrayList<>();
                }
            } catch (JsonSyntaxException e) {
                events = new ArrayList<>();
            }
            String listenerName = listMap.get(p).getName();
            for (int i = 0; i < events.size(); i++) {
                if (listenerName.equals(events.get(i).getListener())) {
                    ex2.add(events.get(i));
                }
            }
        }
        FileUtil.writeFile(concat + ex.get(0).getName() + ".txt", new Gson().toJson(ex) + "\n" + new Gson().toJson(ex2));
        SketchwareUtil.toast(Helper.getResString(R.string.toast_event_exported), Toast.LENGTH_LONG);
    }

    private void exportAllEvents() {
        ArrayList<CustomEvent> events = new ArrayList<>();
        if (FileUtil.isExistFile(EventsManagerConstants.EVENTS_FILE.getAbsolutePath())) {
            try {
                events = new Gson().fromJson(FileUtil.readFile(EventsManagerConstants.EVENTS_FILE.getAbsolutePath()),
                        new TypeToken<ArrayList<CustomEvent>>(){}.getType());
                if (events == null) {
                    events = new ArrayList<>();
                }
            } catch (JsonSyntaxException e) {
                LogUtil.w("EventsManagerFragment", "Failed to parse events JSON", e);
            }
        }
        FileUtil.writeFile(new File(EventsManagerConstants.EVENT_EXPORT_LOCATION, "All_Events.txt").getAbsolutePath(),
                new Gson().toJson(listMap) + "\n" + new Gson().toJson(events));
        SketchwareUtil.toast(Helper.getResString(R.string.toast_events_exported), Toast.LENGTH_LONG);
    }

    private void deleteItem(int position) {
        if (position < 0 || position >= listMap.size()) return;
        String original = new Gson().toJson(listMap.get(position));
        try {
            ArrayList<CustomListener> listeners = CustomEventsStore.INSTANCE.readListeners();
            ArrayList<CustomEvent> events = CustomEventsStore.INSTANCE.readEvents();
            int selected = findListener(listeners, original);
            if (selected < 0) {
                SketchwareUtil.toastError(Helper.getResString(R.string.common_error_an_error_occurred));
                return;
            }
            String name = listeners.remove(selected).getName();
            boolean stillUsed = false;
            for (CustomListener listener : listeners) if (name.equals(listener.getName())) stillUsed = true;
            if (!stillUsed) events.removeIf(event -> name.equals(event.getListener()));
            CustomEventsStore.INSTANCE.saveBoth(events, listeners);
            refreshCodegen();
            refreshList();
        } catch (IOException e) {
            SketchwareUtil.toastError(Helper.getResString(R.string.common_error_failed_to_save));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private class ListenersAdapter extends RecyclerView.Adapter<ListenersAdapter.ViewHolder> {

        private final ArrayList<CustomListener> dataArray;
        private final Context context;

        public ListenersAdapter(ArrayList<CustomListener> arrayList, Context context) {
            dataArray = arrayList;
            this.context = context;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutEventItemBinding binding = LayoutEventItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CustomListener item = dataArray.get(position);
            String name = item.getName();
            holder.itemView.setBackgroundResource(UI.getShapedBackgroundForList(dataArray, position));

            holder.binding.eventIcon.setImageResource(R.drawable.event_on_response_48dp);
            ((LinearLayout) holder.binding.eventIcon.getParent()).setGravity(Gravity.CENTER);

            holder.binding.eventTitle.setText(name);
            holder.binding.eventSubtitle.setText(getNumOfEvents(name));

            holder.itemView.setOnClickListener(v -> openFragment(R.id.settings_fragment_container, EventsManagerDetailsFragment.newInstance(name)));

            holder.itemView.setOnLongClickListener(v -> {
                new MaterialAlertDialogBuilder(context)
                        .setTitle(name)
                        .setItems(new String[]{Helper.getResString(R.string.common_word_edit), Helper.getResString(R.string.common_word_export), Helper.getResString(R.string.common_word_delete)}, (dialog, which) -> {
                            switch (which) {
                                case 0:
                                    showEditListenerDialog(position);
                                    break;
                                case 1:
                                    exportListener(position);
                                    break;
                                case 2:
                                    new MaterialAlertDialogBuilder(context)
                                            .setTitle(R.string.events_delete_listener_title)
                                            .setMessage(R.string.events_delete_confirm_msg)
                                            .setPositiveButton(R.string.common_word_yes, (di, i) -> {
                                                deleteItem(position);
                                                di.dismiss();
                                            })
                                            .setNegativeButton(R.string.common_word_no, (di, i) -> di.dismiss())
                                            .show();
                                    break;
                            }
                        }).show();
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return dataArray.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            private final LayoutEventItemBinding binding;

            public ViewHolder(@NonNull LayoutEventItemBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
