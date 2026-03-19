package pro.sketchware.fragments.settings.events.details;

import static pro.sketchware.utility.GsonUtils.getGson;

import com.google.gson.JsonSyntaxException;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Collections;

import pro.sketchware.core.BaseFragment;
import mod.hey.studios.util.Helper;
import mod.jbk.util.OldResourceIdMapper;
import pro.sketchware.model.CustomEvent;
import pro.sketchware.R;
import pro.sketchware.databinding.FragmentEventsManagerDetailsBinding;
import pro.sketchware.databinding.LayoutEventItemBinding;
import pro.sketchware.fragments.settings.events.EventsManagerConstants;
import pro.sketchware.fragments.settings.events.creator.EventsManagerCreatorFragment;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.UI;

public class EventsManagerDetailsFragment extends BaseFragment {

    private final ArrayList<CustomEvent> listMap = new ArrayList<>();
    private String listName = "";
    private FragmentEventsManagerDetailsBinding binding;

    public static EventsManagerDetailsFragment newInstance(String listName) {
        var fragment = new EventsManagerDetailsFragment();
        var args = new Bundle();
        args.putString("listName", listName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            listName = args.getString("listName");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEventsManagerDetailsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        configureToolbar(binding.toolbar);
        binding.toolbar.setTitle(R.string.events_details_title);
        binding.toolbar.setSubtitle(listName);
        binding.fabNewEvent.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("lis_name", listName);
            EventsManagerCreatorFragment fragment = new EventsManagerCreatorFragment();
            fragment.setArguments(args);
            openFragment(fragment);
        });
        refreshList();
        UI.addSystemWindowInsetToPadding(binding.appBarLayout, true, true, true, true);
        UI.addSystemWindowInsetToPadding(binding.content, true, false, true, true);
        UI.addSystemWindowInsetToMargin(binding.fabNewEvent, false, false, true, true);
    }

    private void refreshList() {
        listMap.clear();
        if (FileUtil.isExistFile(EventsManagerConstants.EVENTS_FILE.getAbsolutePath())) {
            ArrayList<CustomEvent> events;
            try {
                events = getGson()
                        .fromJson(FileUtil.readFile(EventsManagerConstants.EVENTS_FILE.getAbsolutePath()),
                                new TypeToken<ArrayList<CustomEvent>>(){}.getType());
                if (events == null) {
                    events = new ArrayList<>();
                }
            } catch (JsonSyntaxException e) {
                events = new ArrayList<>();
            }
            for (int i = 0; i < events.size(); i++) {
                if (listName.equals(events.get(i).getListener())) {
                    listMap.add(events.get(i));
                }
            }
            Collections.reverse(listMap);
            binding.eventsRecyclerView.setAdapter(new EventsAdapter(listMap));
            binding.eventsRecyclerView.getAdapter().notifyDataSetChanged();
        }
        binding.noEventsLayout.setVisibility(listMap.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void deleteItem(int position) {
        if (position < 0 || position >= listMap.size()) {
            SketchwareUtil.toastError(Helper.getResString(R.string.common_error_an_error_occurred));
            return;
        }
        if (FileUtil.isExistFile(EventsManagerConstants.EVENTS_FILE.getAbsolutePath())) {
            ArrayList<CustomEvent> events;
            try {
                events = getGson()
                        .fromJson(FileUtil.readFile(EventsManagerConstants.EVENTS_FILE.getAbsolutePath()),
                                new TypeToken<ArrayList<CustomEvent>>(){}.getType());
                if (events == null) {
                    events = new ArrayList<>();
                }
            } catch (JsonSyntaxException e) {
                return;
            }
            listMap.remove(position);
            for (int i = events.size() - 1; i > -1; i--) {
                if (listName.equals(events.get(i).getListener())) {
                    events.remove(i);
                }
            }
            events.addAll(listMap);
            FileUtil.writeFile(EventsManagerConstants.EVENTS_FILE.getAbsolutePath(), getGson().toJson(events));
            refreshList();
        } else {
            listMap.remove(position);
            refreshList();
        }
    }

    private class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.ViewHolder> {

        private final ArrayList<CustomEvent> dataArray;

        public EventsAdapter(ArrayList<CustomEvent> arrayList) {
            dataArray = arrayList;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutEventItemBinding binding = LayoutEventItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CustomEvent item = dataArray.get(position);

            holder.itemView.setBackgroundResource(UI.getShapedBackgroundForList(dataArray, position));

            if (listName.isEmpty()) {
                holder.binding.eventIcon.setImageResource(R.drawable.ic_mtrl_code);
            } else {
                try {
                    int imgRes = Integer.parseInt(item.getIcon());
                    holder.binding.eventIcon.setImageResource(OldResourceIdMapper.getDrawableFromOldResourceId(imgRes));
                } catch (NumberFormatException e) {
                    holder.binding.eventIcon.setImageResource(R.drawable.android_icon);
                }
            }

            holder.binding.eventTitle.setText(item.getName());
            if (item.getVar().isEmpty()) {
                holder.binding.eventSubtitle.setText(Helper.getResString(R.string.events_activity_event));
            } else {
                holder.binding.eventSubtitle.setText(item.getVar());
            }
            holder.itemView.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString("lis_name", listName);
                args.putString("event", item.getName());
                args.putString("_pos", String.valueOf(position));
                args.putString("_name", item.getName());
                args.putString("_var", item.getVar());
                args.putString("_lis", item.getListener());
                args.putString("_icon", item.getIcon());
                args.putString("_desc", item.getDescription());
                args.putString("_par", item.getParameters());
                args.putString("_spec", item.getHeaderSpec());
                args.putString("_code", item.getCode());
                EventsManagerCreatorFragment fragment = new EventsManagerCreatorFragment();
                fragment.setArguments(args);
                openFragment(fragment);
            });
            holder.itemView.setOnLongClickListener(v -> {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(item.getName())
                        .setMessage(R.string.events_delete_event_msg)
                        .setPositiveButton(R.string.common_word_delete, (dialog, i) -> deleteItem(position))
                        .setNeutralButton(R.string.common_word_edit, (dialog, i) -> {
                            Bundle args = new Bundle();
                            args.putString("lis_name", listName);
                            args.putString("event", item.getName());
                            args.putString("_pos", String.valueOf(position));
                            args.putString("_name", item.getName());
                            args.putString("_var", item.getVar());
                            args.putString("_lis", item.getListener());
                            args.putString("_icon", item.getIcon());
                            args.putString("_desc", item.getDescription());
                            args.putString("_par", item.getParameters());
                            args.putString("_spec", item.getHeaderSpec());
                            args.putString("_code", item.getCode());
                            EventsManagerCreatorFragment fragment = new EventsManagerCreatorFragment();
                            fragment.setArguments(args);
                            openFragment(fragment);
                        })
                        .setNegativeButton(R.string.common_word_cancel, (di, i) -> di.dismiss())
                        .show();
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
