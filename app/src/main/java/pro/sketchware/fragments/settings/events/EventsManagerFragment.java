package pro.sketchware.fragments.settings.events;

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
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import pro.sketchware.core.BaseFragment;
import dev.pranav.filepicker.FilePickerCallback;
import dev.pranav.filepicker.FilePickerDialogFragment;
import dev.pranav.filepicker.FilePickerOptions;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;
import pro.sketchware.databinding.DialogAddNewListenerBinding;
import pro.sketchware.databinding.FragmentEventsManagerBinding;
import pro.sketchware.databinding.LayoutEventItemBinding;
import pro.sketchware.fragments.settings.events.details.EventsManagerDetailsFragment;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;
import pro.sketchware.utility.UI;

public class EventsManagerFragment extends BaseFragment {

    private FragmentEventsManagerBinding binding;
    private ArrayList<HashMap<String, Object>> listMap = new ArrayList<>();

    public static String getNumOfEvents(String name) {
        int eventAmount = 0;
        if (FileUtil.isExistFile(EventsManagerConstants.EVENTS_FILE.getAbsolutePath())) {
            ArrayList<HashMap<String, Object>> events = new Gson()
                    .fromJson(FileUtil.readFile(EventsManagerConstants.EVENTS_FILE.getAbsolutePath()), Helper.TYPE_MAP_LIST);
            if (events == null) events = new ArrayList<>();
            for (HashMap<String, Object> event : events) {
                if (name.equals(String.valueOf(event.get("listener")))) {
                    eventAmount++;
                }
            }
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
        binding.activityEvents.setOnClickListener(v -> openFragment(new EventsManagerDetailsFragment()));
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
        showListenerDialog(listMap.get(position), position);
    }

    private void showListenerDialog(@Nullable HashMap<String, Object> existingListener, int position) {
        var listenerBinding = DialogAddNewListenerBinding.inflate(LayoutInflater.from(requireContext()));
        if (existingListener != null) {
            listenerBinding.listenerName.setText(String.valueOf(existingListener.get("name")));
            listenerBinding.listenerCode.setText(String.valueOf(existingListener.get("code")));
            listenerBinding.listenerCustomImport.setText(String.valueOf(existingListener.get("imports")));
            if ("true".equals(existingListener.get("s"))) {
                listenerBinding.listenerIsIndependentClassOrMethod.setChecked(true);
                listenerBinding.listenerCode.setText(String.valueOf(existingListener.get("code")).replaceFirst("//" + Helper.getText(listenerBinding.listenerName) + "\n", ""));
            }
        }

        var dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(existingListener == null ? Helper.getResString(R.string.events_new_listener) : Helper.getResString(R.string.events_edit_listener))
                .setView(listenerBinding.getRoot())
                .setPositiveButton(R.string.common_word_save, (di, i) -> {
                    String listenerName = Helper.getText(listenerBinding.listenerName);
                    if (!listenerName.isEmpty()) {
                        HashMap<String, Object> listenerData = existingListener != null ? existingListener : new HashMap<>();
                        listenerData.put("name", listenerName);
                        listenerData.put("code", listenerBinding.listenerIsIndependentClassOrMethod.isChecked()
                                ? "//" + listenerName + "\n" + Helper.getText(listenerBinding.listenerCode)
                                : Helper.getText(listenerBinding.listenerCode));
                        listenerData.put("s", listenerBinding.listenerIsIndependentClassOrMethod.isChecked() ? "true" : "false");
                        listenerData.put("imports", Helper.getText(listenerBinding.listenerCustomImport));
                        if (position >= 0) {
                            listMap.set(position, listenerData);
                        } else {
                            listMap.add(listenerData);
                        }
                        addListenerItem();
                        di.dismiss();
                    } else {
                        SketchwareUtil.toastError(Helper.getResString(R.string.error_invalid_name));
                    }
                })
                .setNegativeButton(R.string.common_word_cancel, (di, i) -> di.dismiss()).create();
        dialog.show();
    }

    public void refreshList() {
        listMap.clear();
        if (FileUtil.isExistFile(EventsManagerConstants.LISTENERS_FILE.getAbsolutePath())) {
            try {
                listMap = new Gson().fromJson(FileUtil.readFile(EventsManagerConstants.LISTENERS_FILE.getAbsolutePath()), Helper.TYPE_MAP_LIST);
            } catch (JsonSyntaxException e) {
                listMap = new ArrayList<>();
            }
            binding.listenersRecyclerView.setAdapter(new ListenersAdapter(listMap, requireContext()));
            binding.listenersRecyclerView.getAdapter().notifyDataSetChanged();
        }
        Collections.reverse(listMap);
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
                        importEvents(new Gson().fromJson(split[0], Helper.TYPE_MAP_LIST),
                                new Gson().fromJson(split[1], Helper.TYPE_MAP_LIST));
                    } catch (JsonSyntaxException | ArrayIndexOutOfBoundsException e) {
                        SketchwareUtil.toastError(Helper.getResString(R.string.error_invalid_file));
                    }
                }
            }

        };
        FilePickerDialogFragment filePickerDialog = new FilePickerDialogFragment(options, callback);

        filePickerDialog.show(getChildFragmentManager(), "filePickerDialog");
    }

    private void importEvents(ArrayList<HashMap<String, Object>> data, ArrayList<HashMap<String, Object>> data2) {
        ArrayList<HashMap<String, Object>> events = new ArrayList<>();
        if (FileUtil.isExistFile(EventsManagerConstants.EVENTS_FILE.getAbsolutePath())) {
            try {
                events = new Gson().fromJson(FileUtil.readFile(EventsManagerConstants.EVENTS_FILE.getAbsolutePath()), Helper.TYPE_MAP_LIST);
            } catch (JsonSyntaxException ignored) {
            }
        }
        events.addAll(data2);
        FileUtil.writeFile(EventsManagerConstants.EVENTS_FILE.getAbsolutePath(), new Gson().toJson(events));
        listMap.addAll(data);
        FileUtil.writeFile(EventsManagerConstants.LISTENERS_FILE.getAbsolutePath(), new Gson().toJson(listMap));
        refreshList();
        SketchwareUtil.toast(Helper.getResString(R.string.toast_events_imported));
    }

    private void exportListener(int p) {
        String concat = FileUtil.getExternalStorageDir().concat("/.sketchware/data/system/export/events/");
        ArrayList<HashMap<String, Object>> ex = new ArrayList<>();
        ex.add(listMap.get(p));
        ArrayList<HashMap<String, Object>> ex2 = new ArrayList<>();
        if (FileUtil.isExistFile(EventsManagerConstants.EVENTS_FILE.getAbsolutePath())) {
            ArrayList<HashMap<String, Object>> events;
            try {
                events = new Gson().fromJson(FileUtil.readFile(EventsManagerConstants.EVENTS_FILE.getAbsolutePath()), Helper.TYPE_MAP_LIST);
            } catch (JsonSyntaxException e) {
                events = new ArrayList<>();
            }
            for (int i = 0; i < events.size(); i++) {
                if (String.valueOf(listMap.get(p).get("name")).equals(String.valueOf(events.get(i).get("listener")))) {
                    ex2.add(events.get(i));
                }
            }
        }
        FileUtil.writeFile(concat + String.valueOf(ex.get(0).get("name")) + ".txt", new Gson().toJson(ex) + "\n" + new Gson().toJson(ex2));
        SketchwareUtil.toast(Helper.getResString(R.string.toast_event_exported), Toast.LENGTH_LONG);
    }

    private void exportAllEvents() {
        ArrayList<HashMap<String, Object>> events = new ArrayList<>();
        if (FileUtil.isExistFile(EventsManagerConstants.EVENTS_FILE.getAbsolutePath())) {
            try {
                events = new Gson().fromJson(FileUtil.readFile(EventsManagerConstants.EVENTS_FILE.getAbsolutePath()), Helper.TYPE_MAP_LIST);
            } catch (JsonSyntaxException ignored) {
            }
        }
        FileUtil.writeFile(new File(EventsManagerConstants.EVENT_EXPORT_LOCATION, "All_Events.txt").getAbsolutePath(),
                new Gson().toJson(listMap) + "\n" + new Gson().toJson(events));
        SketchwareUtil.toast(Helper.getResString(R.string.toast_events_exported), Toast.LENGTH_LONG);
    }

    private void addListenerItem() {
        FileUtil.writeFile(EventsManagerConstants.LISTENERS_FILE.getAbsolutePath(), new Gson().toJson(listMap));
        refreshList();
    }

    private void deleteItem(int position) {
        listMap.remove(position);
        FileUtil.writeFile(EventsManagerConstants.LISTENERS_FILE.getAbsolutePath(), new Gson().toJson(listMap));
        refreshList();
    }

    private void deleteRelatedEvents(String name) {
        ArrayList<HashMap<String, Object>> events = new ArrayList<>();
        if (FileUtil.isExistFile(EventsManagerConstants.EVENTS_FILE.getAbsolutePath())) {
            events = new Gson()
                    .fromJson(FileUtil.readFile(EventsManagerConstants.EVENTS_FILE.getAbsolutePath()), Helper.TYPE_MAP_LIST);
            if (events == null) events = new ArrayList<>();
            for (int i = events.size() - 1; i > -1; i--) {
                if (name.equals(String.valueOf(events.get(i).get("listener")))) {
                    events.remove(i);
                }
            }
        }
        FileUtil.writeFile(EventsManagerConstants.EVENTS_FILE.getAbsolutePath(), new Gson().toJson(events));
    }

    public class ListenersAdapter extends RecyclerView.Adapter<ListenersAdapter.ViewHolder> {

        private final ArrayList<HashMap<String, Object>> dataArray;
        private final Context context;

        public ListenersAdapter(ArrayList<HashMap<String, Object>> arrayList, Context context) {
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
            HashMap<String, Object> item = dataArray.get(position);
            String name = (String) item.get("name");
            holder.itemView.setBackgroundResource(UI.getShapedBackgroundForList(dataArray, position));

            holder.binding.eventIcon.setImageResource(R.drawable.event_on_response_48dp);
            ((LinearLayout) holder.binding.eventIcon.getParent()).setGravity(Gravity.CENTER);

            holder.binding.eventTitle.setText(name);
            holder.binding.eventSubtitle.setText(getNumOfEvents(name));

            holder.itemView.setOnClickListener(v -> openFragment(EventsManagerDetailsFragment.newInstance(name)));

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
                                                deleteRelatedEvents(name);
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
