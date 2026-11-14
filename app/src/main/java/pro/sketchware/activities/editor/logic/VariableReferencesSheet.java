package pro.sketchware.activities.editor.logic;

import static pro.sketchware.activities.editor.logic.ExtraMenuBean.LIST_TYPE_MAP;
import static pro.sketchware.activities.editor.logic.ExtraMenuBean.LIST_TYPE_NUMBER;
import static pro.sketchware.activities.editor.logic.ExtraMenuBean.LIST_TYPE_STRING;
import static pro.sketchware.activities.editor.logic.ExtraMenuBean.VARIABLE_TYPE_BOOLEAN;
import static pro.sketchware.activities.editor.logic.ExtraMenuBean.VARIABLE_TYPE_CUSTOM_DECLARATION;
import static pro.sketchware.activities.editor.logic.ExtraMenuBean.VARIABLE_TYPE_CUSTOM_LEGACY;
import static pro.sketchware.activities.editor.logic.ExtraMenuBean.VARIABLE_TYPE_MAP;
import static pro.sketchware.activities.editor.logic.ExtraMenuBean.VARIABLE_TYPE_NUMBER;
import static pro.sketchware.activities.editor.logic.ExtraMenuBean.VARIABLE_TYPE_STRING;
import static pro.sketchware.util.SketchwareUtil.dpToPx;

import android.os.Parcelable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.color.MaterialColors;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import pro.sketchware.R;
import pro.sketchware.activities.editor.LogicEditorActivity;
import pro.sketchware.activities.editor.makeblock.ReturnMoreblockManager;
import pro.sketchware.beans.EventBean;
import pro.sketchware.core.block.BlockSpecParser;
import pro.sketchware.core.block.view.BlockColorMapper;
import pro.sketchware.core.codegen.BuiltinBlockLocalization;
import pro.sketchware.core.codegen.EventRegistry;
import pro.sketchware.core.codegen.StringResource;
import pro.sketchware.core.project.ProjectDataStore;
import pro.sketchware.core.project.ProjectDataStore.VariableReference;
import pro.sketchware.core.project.ProjectListManager;
import pro.sketchware.databinding.VariableReferenceHeadingBinding;
import pro.sketchware.databinding.VariableReferenceRowBinding;
import pro.sketchware.databinding.VariableReferencesSheetBinding;
import pro.sketchware.util.MapValueHelper;
import pro.sketchware.util.SketchwareUtil;
import pro.sketchware.util.UIHelper;
import pro.sketchware.util.format.FormatUtil;

final class VariableReferencesSheet {
    private static final List<Group> VARIABLE_GROUPS =
            List.of(
                    new Group(R.string.logic_group_boolean, VARIABLE_TYPE_BOOLEAN),
                    new Group(R.string.logic_group_number, VARIABLE_TYPE_NUMBER),
                    new Group(R.string.logic_group_string, VARIABLE_TYPE_STRING),
                    new Group(R.string.logic_group_map, VARIABLE_TYPE_MAP),
                    new Group(
                            R.string.logic_group_custom,
                            VARIABLE_TYPE_CUSTOM_LEGACY,
                            VARIABLE_TYPE_CUSTOM_DECLARATION));
    private static final List<Group> LIST_GROUPS =
            List.of(
                    new Group(R.string.logic_group_list_integer, LIST_TYPE_NUMBER),
                    new Group(R.string.logic_group_list_string, LIST_TYPE_STRING),
                    new Group(R.string.logic_group_list_map, LIST_TYPE_MAP),
                    new Group(R.string.logic_group_list_custom, 4));

    private final LogicEditorActivity editor;
    private final ProjectDataStore project;
    private final boolean isList;
    private final String javaName;
    private final String currentEvent;
    private final BottomSheetDialog dialog;
    private final VariableReferencesSheetBinding binding;
    private final LinearLayoutManager layoutManager;
    private final RowsAdapter adapter = new RowsAdapter();
    private Page page = Page.CHOICES;
    private final OnBackPressedCallback back =
            new OnBackPressedCallback(false) {
                @Override
                public void handleOnBackPressed() {
                    goBack();
                }
            };
    private Parcelable choiceScroll;

    VariableReferencesSheet(LogicEditorActivity editor, ProjectDataStore project, boolean isList) {
        this.editor = editor;
        this.project = project;
        this.isList = isList;
        javaName = editor.projectFile.getJavaName();
        currentEvent = editor.id + "_" + editor.eventName;
        dialog =
                new BottomSheetDialog(editor, R.style.ThemeOverlay_SketchwarePro_BottomSheetDialog);
        binding = VariableReferencesSheetBinding.inflate(LayoutInflater.from(dialog.getContext()));
        layoutManager = new LinearLayoutManager(dialog.getContext());
    }

    void show() {
        dialog.setContentView(binding.getRoot());
        binding.referenceList.setLayoutManager(layoutManager);
        binding.referenceList.setAdapter(adapter);
        if (binding.referenceList.getItemAnimator() instanceof SimpleItemAnimator animator) {
            animator.setSupportsChangeAnimations(false);
        }
        binding.referenceClose.setOnClickListener(view -> dialog.dismiss());
        binding.referenceBack.setOnClickListener(view -> goBack());
        binding.referenceSearch.setOnClickListener(view -> openFilter());
        binding.referenceClearQuery.setOnClickListener(view -> binding.referenceQuery.setText(""));
        binding.referenceQuery.addTextChangedListener(
                new PaletteSelector.SimpleTextWatcher(
                        text -> {
                            updateClearButton();
                            updateChoices(text.toString());
                        }));
        binding.referenceQuery.setOnEditorActionListener(
                (view, actionId, event) -> {
                    UIHelper.hideKeyboard(editor, binding.referenceQuery);
                    binding.getRoot().requestFocus();
                    return true;
                });

        BottomSheetBehavior<?> behavior = dialog.getBehavior();
        behavior.setMaxWidth(dpToPx(560));
        behavior.setMaxHeight(
                dpToPx(editor.getResources().getConfiguration().screenHeightDp * 4 / 5));
        behavior.setSkipCollapsed(true);
        behavior.setDraggableOnNestedScroll(false);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        dialog.getWindow()
                .setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                                | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        showChoices();
        dialog.show();
        dialog.getOnBackPressedDispatcher().addCallback(back);
        dialog.setOnDismissListener(ignored -> back.remove());
    }

    private void showChoices() {
        String query = binding.referenceQuery.getText().toString();
        showHeader(query.isEmpty() ? Page.CHOICES : Page.FILTER);
        binding.referenceCount.setText(R.string.logic_editor_panel_button_find_refs_variable);
        binding.referenceCount.setVisibility(View.VISIBLE);
        binding.referenceTitle.setText(
                isList
                        ? R.string.logic_editor_find_refs_select_list
                        : R.string.logic_editor_find_refs_select_variable);
        ViewCompat.setAccessibilityPaneTitle(binding.getRoot(), binding.referenceTitle.getText());
        binding.getRoot().requestFocus();
        updateChoices(query);
        if (choiceScroll != null) layoutManager.onRestoreInstanceState(choiceScroll);
    }

    private void showHeader(Page page) {
        this.page = page;
        boolean filtering = page == Page.FILTER;
        boolean atStart = page == Page.CHOICES;
        binding.referenceHeading.setVisibility(filtering ? View.GONE : View.VISIBLE);
        binding.referenceQuery.setVisibility(filtering ? View.VISIBLE : View.GONE);
        binding.referenceBack.setVisibility(atStart ? View.GONE : View.VISIBLE);
        binding.referenceSearch.setVisibility(atStart ? View.VISIBLE : View.GONE);
        updateClearButton();
        // Let this sheet handle back on its subpages before Material can dismiss it.
        dialog.setCancelable(atStart);
        back.setEnabled(!atStart);
    }

    private void updateClearButton() {
        binding.referenceClearQuery.setVisibility(
                page == Page.FILTER && binding.referenceQuery.length() > 0
                        ? View.VISIBLE
                        : View.GONE);
    }

    private void openFilter() {
        showHeader(Page.FILTER);
        var query = binding.referenceQuery;
        query.requestFocus();
        query.post(
                () -> {
                    if (page != Page.FILTER || !query.hasFocus()) return;
                    var keyboard = editor.getSystemService(InputMethodManager.class);
                    keyboard.showSoftInput(query, InputMethodManager.SHOW_IMPLICIT);
                });
    }

    private void goBack() {
        if (page == Page.RESULTS) {
            showChoices();
        } else if (page == Page.FILTER) {
            UIHelper.hideKeyboard(editor, binding.referenceQuery);
            binding.getRoot().requestFocus();
            showHeader(Page.CHOICES);
            binding.referenceQuery.setText("");
        }
    }

    private void updateChoices(String query) {
        String filter = query.trim().toLowerCase(Locale.ROOT);
        List<Row> rows = new ArrayList<>();
        boolean hasChoices = false;
        for (Group group : isList ? LIST_GROUPS : VARIABLE_GROUPS) {
            List<Row> choices = new ArrayList<>();
            for (int type : group.types()) {
                List<String> declarations =
                        isList
                                ? project.getListNamesByType(javaName, type)
                                : project.getVariableNamesByType(javaName, type);
                hasChoices |= !declarations.isEmpty();
                for (String declaration : declarations) {
                    String parsedName = CustomVariableUtil.getVariableName(declaration);
                    String name = parsedName == null ? declaration : parsedName;
                    String detail = parsedName == null ? null : declaration;
                    if (!name.toLowerCase(Locale.ROOT).contains(filter)
                            && (detail == null
                                    || !detail.toLowerCase(Locale.ROOT).contains(filter))) {
                        continue;
                    }
                    String customType =
                            isList ? null : CustomVariableUtil.getVariableType(declaration);
                    choices.add(new Choice(name, detail, customType));
                }
            }
            if (!choices.isEmpty()) {
                rows.add(new Heading(editor.getString(group.title(), choices.size()), null));
                rows.addAll(choices);
            }
        }
        int emptyText =
                hasChoices
                        ? R.string.search_canvas_no_results
                        : isList
                                ? R.string.logic_editor_message_no_lists
                                : R.string.logic_editor_message_no_variables;
        showRows(rows, emptyText);
    }

    private void showReferences(String name, String customType) {
        choiceScroll = layoutManager.onSaveInstanceState();
        UIHelper.hideKeyboard(editor, binding.referenceQuery);
        binding.getRoot().requestFocus();
        showHeader(Page.RESULTS);
        binding.referenceTitle.setText(name);
        ViewCompat.setAccessibilityPaneTitle(
                binding.getRoot(), editor.getString(R.string.logic_editor_title_find_refs, name));
        editor.saveBlocks();
        String packageName =
                MapValueHelper.getString(
                        ProjectListManager.getProjectById(editor.scId), "my_sc_pkg_name");
        var references =
                project.findVariableReferences(javaName, name, isList, customType, packageName);
        binding.referenceCount.setText(
                references.isEmpty()
                        ? editor.getString(R.string.logic_editor_panel_button_find_refs_variable)
                        : editor.getString(
                                R.string.logic_editor_find_refs_count, references.size()));
        int highlight = MaterialColors.getColor(binding.referenceTitle, R.attr.colorPrimary);
        var groups = new LinkedHashMap<String, List<VariableReference>>();
        for (VariableReference reference : references) {
            groups.computeIfAbsent(reference.blockEntryKey, key -> new ArrayList<>())
                    .add(reference);
        }
        var sections = new ArrayList<EventReferences>();
        for (var entry : groups.entrySet()) {
            String key = entry.getKey();
            EventBean event = findEvent(key);
            sections.add(
                    new EventReferences(
                            key, event == null ? key : eventTitle(event), event, entry.getValue()));
        }
        sections.sort(
                Comparator.comparing(
                                (EventReferences section) -> !currentEvent.equals(section.key()))
                        .thenComparing(EventReferences::title, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(EventReferences::key));
        var numbers =
                NumberFormat.getIntegerInstance(
                        editor.getResources().getConfiguration().getLocales().get(0));
        List<Row> rows = new ArrayList<>();
        for (EventReferences section : sections) {
            String title =
                    currentEvent.equals(section.key())
                            ? editor.getString(
                                    R.string.logic_editor_reference_current_event, section.title())
                            : section.title();
            rows.add(new Heading(title, numbers.format(section.references().size())));
            for (VariableReference reference : section.references()) {
                rows.add(
                        new Result(
                                reference,
                                section.event(),
                                reference.sourceLine == null
                                        ? null
                                        : highlightName(reference.sourceLine, name, highlight)));
            }
        }
        showRows(rows, R.string.logic_editor_message_no_refs_found);
    }

    private static CharSequence highlightName(String source, String name, int color) {
        if (name.isEmpty()) return source;
        var text = new SpannableString(source);
        int first = -1;
        for (int start = source.indexOf(name);
                start >= 0;
                start = source.indexOf(name, start + name.length())) {
            int end = start + name.length();
            if (start > 0 && Character.isJavaIdentifierPart(source.codePointBefore(start)))
                continue;
            if (end < source.length() && Character.isJavaIdentifierPart(source.codePointAt(end)))
                continue;
            text.setSpan(
                    new ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (first < 0) first = start;
        }
        // Keep the identifier visible when the beginning of a long line is clipped.
        return first > 24
                ? TextUtils.concat(
                        "…", text.subSequence(source.offsetByCodePoints(first, -12), text.length()))
                : text;
    }

    private void showRows(List<Row> rows, int emptyText) {
        adapter.setRows(rows);
        layoutManager.scrollToPosition(0);
        binding.referenceList.setVisibility(rows.isEmpty() ? View.GONE : View.VISIBLE);
        binding.referenceEmpty.setText(emptyText);
        binding.referenceEmpty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openReference(VariableReference reference, EventBean event) {
        if (currentEvent.equals(reference.blockEntryKey)) {
            var block = editor.blockPane.findBlockByString(reference.blockId);
            if (block == null) {
                showReferenceUnavailable();
                return;
            }
            dialog.dismiss();
            editor.focusBlockReference(block);
        } else if (event != null) {
            editor.navigateToEvent(event.targetId, event.eventName, reference.blockId);
        } else {
            showReferenceUnavailable();
        }
    }

    private void showReferenceUnavailable() {
        SketchwareUtil.toast(editor.getString(R.string.logic_editor_reference_unavailable));
    }

    private EventBean findEvent(String key) {
        if ("onCreate_initializeLogic".equals(key)) {
            return new EventBean(EventBean.EVENT_TYPE_ACTIVITY, -1, "onCreate", "initializeLogic");
        }
        for (var moreBlock : project.getMoreBlocks(javaName)) {
            if ((moreBlock.first + "_moreBlock").equals(key)) {
                return new EventBean(EventBean.EVENT_TYPE_ETC, -1, moreBlock.first, "moreBlock");
            }
        }
        for (EventBean event : project.getEvents(javaName)) {
            if ((event.targetId + "_" + event.eventName).equals(key)) return event;
        }
        return null;
    }

    private static String eventTitle(EventBean event) {
        if ("moreBlock".equals(event.eventName)) {
            return ReturnMoreblockManager.getMbName(event.targetId);
        }
        String name = EventRegistry.getEventName(event.eventName);
        return event.eventType == EventBean.EVENT_TYPE_ACTIVITY
                ? name
                : event.targetId + " · " + name;
    }

    private String blockLabel(VariableReference reference) {
        String source = reference.blockSpec;
        if (source == null || source.isEmpty()) return reference.opCode;
        if (!"getVar".equals(reference.opCode)
                && BlockColorMapper.getBlockColor(editor, reference.opCode, reference.blockType)
                        != BlockColorMapper.CUSTOM_BLOCK_FALLBACK_COLOR) {
            String localized =
                    StringResource.getInstance().getEventTranslation(editor, reference.opCode);
            if (!localized.isEmpty()
                    && BlockSpecParser.parseParameters(source)
                            .equals(BlockSpecParser.parseParameters(localized))) {
                source = localized;
            }
        } else {
            source = BuiltinBlockLocalization.getSpec(editor, reference.opCode, source, false);
        }
        List<String> parts = FormatUtil.parseBlockSpec(source);
        int parameter = 0;
        for (int i = 0; i < parts.size(); i++) {
            String part = parts.get(i);
            if (part.length() >= 2
                    && part.charAt(0) == '%'
                    && "bdsm".indexOf(part.charAt(1)) >= 0) {
                String value =
                        parameter < reference.parameters.size()
                                ? reference.parameters.get(parameter)
                                : null;
                parts.set(
                        i, value == null || value.isEmpty() || value.startsWith("@") ? "…" : value);
                parameter++;
            } else {
                parts.set(i, FormatUtil.unescapeString(part));
            }
        }
        return String.join(" ", parts);
    }

    private enum Page {
        CHOICES,
        FILTER,
        RESULTS
    }

    private record Group(int title, int... types) {}

    private record EventReferences(
            String key, String title, EventBean event, List<VariableReference> references) {}

    private interface Row {}

    private record Heading(String title, String count) implements Row {}

    private record Choice(String name, String declaration, String type) implements Row {}

    private record Result(VariableReference reference, EventBean event, CharSequence preview)
            implements Row {}

    private final class RowsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private List<Row> rows = List.of();
        private int selected = RecyclerView.NO_POSITION;

        void setRows(List<Row> rows) {
            this.rows = rows;
            selected = RecyclerView.NO_POSITION;
            notifyDataSetChanged();
        }

        private void open(int position) {
            if (position == RecyclerView.NO_POSITION) return;
            Row row = rows.get(position);
            if (row instanceof Choice choice) {
                showReferences(choice.name(), choice.type());
            } else if (row instanceof Result result) {
                if (position != selected) {
                    int previous = selected;
                    selected = position;
                    if (previous != RecyclerView.NO_POSITION) notifyItemChanged(previous);
                    notifyItemChanged(position);
                }
                openReference(result.reference(), result.event());
            }
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        @Override
        public int getItemViewType(int position) {
            return rows.get(position) instanceof Heading
                    ? R.layout.variable_reference_heading
                    : R.layout.variable_reference_row;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int layout) {
            var inflater = LayoutInflater.from(parent.getContext());
            if (layout == R.layout.variable_reference_heading) {
                return new HeadingHolder(
                        VariableReferenceHeadingBinding.inflate(inflater, parent, false));
            }
            var holder =
                    new EntryHolder(VariableReferenceRowBinding.inflate(inflater, parent, false));
            holder.itemView.setOnClickListener(view -> open(holder.getBindingAdapterPosition()));
            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Row row = rows.get(position);
            if (row instanceof Heading heading) {
                var views = ((HeadingHolder) holder).binding;
                views.referenceGroupTitle.setText(heading.title());
                views.referenceGroupCount.setText(heading.count());
                views.referenceGroupCount.setVisibility(
                        heading.count() == null ? View.GONE : View.VISIBLE);
                return;
            }
            var entry = (EntryHolder) holder;
            if (row instanceof Choice choice) {
                entry.bind(
                        choice.name(),
                        choice.declaration(),
                        isList ? R.drawable.ic_mtrl_list : R.drawable.ic_mtrl_code,
                        false);
            } else {
                var result = (Result) row;
                boolean source = result.preview() != null;
                entry.bind(
                        source
                                ? editor.getString(R.string.block_add_source_directly_head)
                                : blockLabel(result.reference()),
                        result.preview(),
                        source ? R.drawable.ic_mtrl_code : R.drawable.ic_mtrl_block,
                        source);
            }
            holder.itemView.setSelected(position == selected);
        }
    }

    private static final class HeadingHolder extends RecyclerView.ViewHolder {
        private final VariableReferenceHeadingBinding binding;

        HeadingHolder(VariableReferenceHeadingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private static final class EntryHolder extends RecyclerView.ViewHolder {
        private final VariableReferenceRowBinding binding;

        EntryHolder(VariableReferenceRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(String title, CharSequence detail, int icon, boolean source) {
            binding.referenceRowTitle.setText(title);
            binding.referenceRowIcon.setImageResource(icon);
            var text = binding.referenceRowDetail;
            text.setText(detail);
            text.setVisibility(detail == null ? View.GONE : View.VISIBLE);
            text.setBackgroundResource(source ? R.drawable.bg_reference_code : 0);
            if (!source) text.setPadding(0, 0, 0, 0);
        }
    }
}
