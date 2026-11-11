package pro.sketchware.activities.editor.makeblock;

import android.app.Activity;
import android.text.InputType;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import pro.sketchware.R;
import pro.sketchware.beans.BlockBean;
import pro.sketchware.beans.MoreBlockCollectionBean;
import pro.sketchware.beans.ProjectFileBean;
import pro.sketchware.beans.ProjectResourceBean;
import pro.sketchware.core.async.BackgroundTasks;
import pro.sketchware.core.async.TaskHost;
import pro.sketchware.core.project.BlockConstants;
import pro.sketchware.core.project.ClassInfo;
import pro.sketchware.core.project.FontCollectionManager;
import pro.sketchware.core.project.ImageCollectionManager;
import pro.sketchware.core.project.ProjectDataManager;
import pro.sketchware.core.project.ProjectDataStore;
import pro.sketchware.core.project.ResourceManager;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.core.project.SoundCollectionManager;
import pro.sketchware.core.validation.IdentifierValidator;
import pro.sketchware.util.Helper;
import pro.sketchware.util.SketchToast;
import pro.sketchware.util.UIHelper;
import pro.sketchware.util.ViewUtil;

public class MoreblockImporter {
    private final Activity activity;
    private final String sc_id;
    private final ProjectFileBean projectActivity;
    private final String activityJavaName;

    private ArrayList<Pair<Integer, String>> toBeAddedVariables;
    private ArrayList<Pair<Integer, String>> toBeAddedLists;
    private ArrayList<ProjectResourceBean> toBeAddedImages;
    private ArrayList<ProjectResourceBean> toBeAddedSounds;
    private ArrayList<ProjectResourceBean> toBeAddedFonts;
    private Set<String> missingResources;

    private Callback callback;

    public MoreblockImporter(Activity activity, String sc_id, ProjectFileBean projectActivity) {
        this.activity = activity;
        this.sc_id = sc_id;
        this.projectActivity = projectActivity;
        activityJavaName = projectActivity.getJavaName();
    }

    public void importMoreblock(MoreBlockCollectionBean moreblock, Callback callback) {
        this.callback = callback;
        ArrayList<BlockBean> blocks = new ArrayList<>(moreblock.blocks.size());
        for (BlockBean block : moreblock.blocks) {
            blocks.add(block.clone());
        }
        MoreBlockCollectionBean imported =
                new MoreBlockCollectionBean(moreblock.name, moreblock.spec, blocks);
        String blockName =
                ReturnMoreblockManager.getMbName(
                        ReturnMoreblockManager.getMbNameWithTypeFromSpec(imported.spec));

        boolean duplicateNameFound = false;
        for (Pair<String, String> projectMoreBlock :
                ProjectDataManager.getProjectDataManager(sc_id).getMoreBlocks(activityJavaName)) {
            if (ReturnMoreblockManager.getMbName(projectMoreBlock.first).equals(blockName)) {
                duplicateNameFound = true;
                break;
            }
        }
        if (!duplicateNameFound) {
            handleVariables(imported);
        } else {
            showEditMoreBlockNameDialog(imported);
        }
    }

    private void handleVariables(MoreBlockCollectionBean moreBlock) {
        toBeAddedVariables = new ArrayList<>();
        toBeAddedLists = new ArrayList<>();
        toBeAddedImages = new ArrayList<>();
        toBeAddedSounds = new ArrayList<>();
        toBeAddedFonts = new ArrayList<>();
        missingResources = new LinkedHashSet<>();
        for (BlockBean next : moreBlock.blocks) {
            if (next.opCode.equals("getVar")) {
                switch (next.type) {
                    case "b":
                        maybeAddVariable(0, next.spec);
                        break;
                    case "d":
                        maybeAddVariable(1, next.spec);
                        break;
                    case "s":
                        maybeAddVariable(2, next.spec);
                        break;
                    case "a":
                        maybeAddVariable(3, next.spec);
                        break;
                    case "l":
                        switch (next.typeName) {
                            case "List Number" -> maybeAddList(1, next.spec);
                            case "List String" -> maybeAddList(2, next.spec);
                            case "List Map" -> maybeAddList(3, next.spec);
                        }
                        break;
                }
            }
            ArrayList<ClassInfo> paramClassInfo = next.getParamClassInfo();
            if (!paramClassInfo.isEmpty()) {
                for (int i = 0; i < paramClassInfo.size(); i++) {
                    ClassInfo classInfo = paramClassInfo.get(i);
                    String paramValue = next.parameters.get(i);
                    if (!paramValue.isEmpty() && paramValue.charAt(0) != '@') {
                        if (classInfo.isExactType("boolean.SelectBoolean")) {
                            maybeAddVariable(0, paramValue);
                        } else if (classInfo.isExactType("double.SelectDouble")) {
                            maybeAddVariable(1, paramValue);
                        } else if (classInfo.isExactType("String.SelectString")) {
                            maybeAddVariable(2, paramValue);
                        } else if (classInfo.isExactType("Map")) {
                            maybeAddVariable(3, paramValue);
                        } else if (classInfo.isExactType("ListInt")) {
                            maybeAddList(1, paramValue);
                        } else if (classInfo.isExactType("ListString")) {
                            maybeAddList(2, paramValue);
                        } else if (classInfo.isExactType("ListMap")) {
                            maybeAddList(3, paramValue);
                        } else if (classInfo.isExactType("Image")
                                || classInfo.isExactType("BackgroundImage")) {
                            maybeAddImage(paramValue);
                        } else if (classInfo.isExactType("sound")) {
                            maybeAddSound(paramValue);
                        } else if (classInfo.isExactType("font")) {
                            maybeAddFont(paramValue);
                        }
                    }
                }
            }
        }
        if (!missingResources.isEmpty()) {
            showImportError(
                    Helper.getResString(
                            R.string.moreblock_import_file_missing,
                            String.join(", ", missingResources)));
            return;
        }
        if (toBeAddedVariables.isEmpty()
                && toBeAddedLists.isEmpty()
                && toBeAddedImages.isEmpty()
                && toBeAddedSounds.isEmpty()
                && toBeAddedFonts.isEmpty()) {
            try {
                addMoreBlock(moreBlock, ProjectDataManager.getProjectDataManager(sc_id));
            } catch (RuntimeException failure) {
                Log.e("MoreblockImporter", "Could not add MoreBlock", failure);
                showImportError(resourceErrorMessage(failure));
                return;
            }
            notifyImportComplete();
        } else {
            showAutoAddDialog(moreBlock);
        }
    }

    private void addMoreBlock(MoreBlockCollectionBean moreBlock, ProjectDataStore data) {
        String moreBlockName = ReturnMoreblockManager.getMbNameWithTypeFromSpec(moreBlock.spec);
        String blockKey = moreBlockName + "_moreBlock";
        var blockMap = data.blockMap.get(activityJavaName);
        Runnable restoreDefinition = undoAppend(data.moreBlockMap);
        Runnable restoreBody =
                blockMap == null
                        ? restoreEntry(data.blockMap, activityJavaName)
                        : restoreEntry(blockMap, blockKey);
        try {
            data.addMoreBlock(activityJavaName, moreBlockName, moreBlock.spec);
            data.putBlocks(activityJavaName, blockKey, moreBlock.blocks);
        } catch (RuntimeException failure) {
            undoChanges(failure, restoreDefinition, restoreBody);
            throw failure;
        }
    }

    private <T> Runnable undoAppend(Map<String, ArrayList<T>> entries) {
        ArrayList<T> existing = entries.get(activityJavaName);
        return existing == null ? restoreEntry(entries, activityJavaName) : undoAppend(existing);
    }

    private static Runnable undoAppend(ArrayList<?> entries) {
        int size = entries.size();
        return () -> entries.subList(size, entries.size()).clear();
    }

    private static <T> Runnable restoreEntry(Map<String, T> entries, String key) {
        if (!entries.containsKey(key)) return () -> entries.remove(key);
        T previous = entries.get(key);
        return () -> entries.put(key, previous);
    }

    private static void undoChanges(RuntimeException failure, Runnable... changes) {
        for (int i = changes.length - 1; i >= 0; i--) {
            try {
                changes[i].run();
            } catch (RuntimeException undoFailure) {
                failure.addSuppressed(undoFailure);
            }
        }
    }

    private void notifyImportComplete() {
        SketchToast.toast(activity, Helper.getResString(R.string.common_message_complete_save), 0)
                .show();
        callback.onImportComplete();
    }

    private void showAutoAddDialog(MoreBlockCollectionBean moreBlock) {
        new ImportTask(moreBlock).show();
    }

    private final class ImportTask implements DefaultLifecycleObserver {
        private final MoreBlockCollectionBean moreBlock;
        private final ProjectDataStore data = ProjectDataManager.getProjectDataManager(sc_id);
        private final ResourceManager resources = ProjectDataManager.getResourceManager(sc_id);
        private final TaskHost host = TaskHost.of(activity);
        private final LifecycleOwner owner =
                activity instanceof LifecycleOwner value ? value : null;
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private final AlertDialog popup;
        private boolean completed;

        ImportTask(MoreBlockCollectionBean moreBlock) {
            this.moreBlock = moreBlock;
            popup =
                    new MaterialAlertDialogBuilder(activity)
                            .setTitle(R.string.logic_more_block_title_add_variable_resource)
                            .setIcon(R.drawable.break_warning_96_red)
                            .setMessage(R.string.logic_more_block_desc_add_variable_resource)
                            .setPositiveButton(R.string.common_word_continue, null)
                            .setNegativeButton(R.string.common_word_cancel, null)
                            .create();
        }

        void show() {
            if (owner != null) owner.getLifecycle().addObserver(this);
            popup.setOnDismissListener(
                    ignored -> {
                        if (!completed) cancelled.set(true);
                        if (owner != null) owner.getLifecycle().removeObserver(this);
                    });
            popup.show();
            popup.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(ignored -> start());
            popup.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(ignored -> cancel());
        }

        @Override
        public void onStop(LifecycleOwner ignored) {
            cancel();
        }

        private void cancel() {
            if (!completed) cancelled.set(true);
            popup.dismiss();
        }

        private boolean canContinue() {
            return !cancelled.get()
                    && !activity.isFinishing()
                    && !activity.isDestroyed()
                    && popup.isShowing();
        }

        private void start() {
            popup.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            popup.getButton(AlertDialog.BUTTON_POSITIVE).setText(R.string.common_word_please_wait);
            BackgroundTasks.runSerial(
                    host, "MoreblockImporter", this::copyResources, null, this::showError);
        }

        private void copyResources() throws IOException {
            List<Path> created;
            try {
                created = MoreblockResourceFiles.copyAll(resourceCopies(resources), cancelled::get);
            } catch (CancellationException cancellation) {
                if (cancellation.getSuppressed().length > 0) {
                    Log.e("MoreblockImporter", "Failed to clean cancelled import", cancellation);
                }
                return;
            }
            // Finalization must run even if TaskHost would drop a callback for a closed activity.
            BackgroundTasks.postMain(() -> finish(created));
        }

        private void finish(List<Path> created) {
            if (!canContinue()) {
                rollback(created, null);
                return;
            }
            try {
                addToProject();
            } catch (RuntimeException failure) {
                rollback(created, failure);
                return;
            }
            completed = true;
            popup.dismiss();
            notifyImportComplete();
        }

        private void addToProject() {
            Runnable undoVariables = undoAppend(data.variableMap);
            Runnable undoLists = undoAppend(data.listMap);
            Runnable undoImages = undoAppend(resources.images);
            Runnable undoSounds = undoAppend(resources.sounds);
            Runnable undoFonts = undoAppend(resources.fonts);
            try {
                for (Pair<Integer, String> variable : toBeAddedVariables) {
                    data.addVariable(activityJavaName, variable.first, variable.second);
                }
                for (Pair<Integer, String> list : toBeAddedLists) {
                    data.addListVariable(activityJavaName, list.first, list.second);
                }
                resources.images.addAll(toBeAddedImages);
                resources.sounds.addAll(toBeAddedSounds);
                resources.fonts.addAll(toBeAddedFonts);
                addMoreBlock(moreBlock, data);
            } catch (RuntimeException failure) {
                undoChanges(failure, undoVariables, undoLists, undoImages, undoSounds, undoFonts);
                throw failure;
            }
        }

        private void rollback(List<Path> created, Exception failure) {
            BackgroundTasks.runSerial(
                    host,
                    "MoreblockImporter",
                    () -> {
                        try {
                            MoreblockResourceFiles.rollback(created);
                        } catch (IOException cleanupFailure) {
                            if (failure == null) throw cleanupFailure;
                            failure.addSuppressed(cleanupFailure);
                        }
                        if (failure != null) throw failure;
                    },
                    null,
                    this::showError);
        }

        private void showError(Throwable error) {
            if (!canContinue()) return;
            popup.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            popup.getButton(AlertDialog.BUTTON_POSITIVE).setText(R.string.common_word_continue);
            showImportError(resourceErrorMessage(error));
        }
    }

    private String resourceErrorMessage(Throwable error) {
        if (error instanceof FileAlreadyExistsException existing) {
            return Helper.getResString(
                    R.string.moreblock_import_file_exists, new File(existing.getFile()).getName());
        }
        if (error instanceof NoSuchFileException missing) {
            return Helper.getResString(
                    R.string.moreblock_import_file_missing, new File(missing.getFile()).getName());
        }
        if (error instanceof InvalidPathException invalid) {
            return Helper.getResString(
                    R.string.moreblock_import_invalid_filename,
                    new File(invalid.getInput()).getName());
        }
        return Helper.getResString(
                error instanceof IOException
                        ? R.string.moreblock_import_resource_failed
                        : R.string.common_error_an_error_occurred);
    }

    private void showEditMoreBlockNameDialog(MoreBlockCollectionBean moreBlock) {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(activity);
        dialog.setTitle(R.string.logic_more_block_title_change_block_name);
        dialog.setIcon(R.drawable.more_block_96dp);

        View customView =
                ViewUtil.inflateLayout(activity, R.layout.property_popup_save_to_favorite);
        ((TextView) customView.findViewById(R.id.tv_favorites_guide))
                .setText(R.string.logic_more_block_desc_change_block_name);
        EditText newName = customView.findViewById(R.id.ed_input);
        newName.setPrivateImeOptions("defaultInputmode=english;");
        newName.setLines(1);
        newName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        newName.setImeOptions(EditorInfo.IME_ACTION_DONE);

        List<String> moreBlockNamesWithoutReturnTypes = new LinkedList<>();
        for (String moreBlockName :
                ProjectDataManager.getProjectDataManager(sc_id)
                        .getAllIdentifiers(projectActivity)) {
            moreBlockNamesWithoutReturnTypes.add(ReturnMoreblockManager.getMbName(moreBlockName));
        }

        IdentifierValidator validator =
                new IdentifierValidator(
                        activity,
                        customView.findViewById(R.id.ti_input),
                        BlockConstants.RESERVED_KEYWORDS,
                        BlockConstants.COMPONENT_TYPES,
                        new ArrayList<>(moreBlockNamesWithoutReturnTypes));
        dialog.setView(customView);
        dialog.setPositiveButton(
                R.string.common_word_save,
                (v, which) -> {
                    if (validator.isValid()) {
                        String moreBlockName =
                                ReturnMoreblockManager.getMbName(
                                        ReturnMoreblockManager.getMbNameWithTypeFromSpec(
                                                moreBlock.spec));
                        moreBlock.spec =
                                Helper.getText(newName)
                                        + moreBlock.spec.substring(moreBlockName.length());

                        handleVariables(moreBlock);
                        UIHelper.hideKeyboard(activity, newName);
                        v.dismiss();
                    }
                });
        dialog.setNegativeButton(
                R.string.common_word_cancel,
                (v, which) -> {
                    UIHelper.hideKeyboard(activity, newName);
                    v.dismiss();
                });
        dialog.show();
    }

    private List<MoreblockResourceFiles.Copy> resourceCopies(ResourceManager resources)
            throws IOException {
        ArrayList<MoreblockResourceFiles.Copy> copies = new ArrayList<>();
        addResourceCopies(copies, toBeAddedImages, "image", resources.getImageDirPath());
        addResourceCopies(copies, toBeAddedSounds, "sound", resources.getSoundDirPath());
        addResourceCopies(copies, toBeAddedFonts, "font", resources.getFontDirPath());
        return copies;
    }

    private void addResourceCopies(
            List<MoreblockResourceFiles.Copy> copies,
            List<ProjectResourceBean> resources,
            String collection,
            String directory)
            throws IOException {
        for (ProjectResourceBean resource : resources) {
            String name = resource.resFullName;
            if (name == null
                    || name.isEmpty()
                    || name.equals(".")
                    || name.equals("..")
                    || !new File(name).getName().equals(name)) {
                throw new InvalidPathException(resource.resName, "Invalid resource file name");
            }
            Path source =
                    new File(SketchwarePaths.getCollectionPath(), collection + "/data/" + name)
                            .toPath();
            Path target = new File(directory, name).toPath();
            copies.add(new MoreblockResourceFiles.Copy(source, target));
        }
    }

    private void showImportError(String reason) {
        SketchToast.warning(
                        activity,
                        Helper.getResString(R.string.error_import_moreblocks, reason),
                        Toast.LENGTH_LONG)
                .show();
    }

    private void maybeAddVariable(int variableType, String variableName) {
        if (toBeAddedVariables == null) {
            toBeAddedVariables = new ArrayList<>();
        }
        for (Pair<Integer, String> variable :
                ProjectDataManager.getProjectDataManager(sc_id).getVariables(activityJavaName)) {
            if (variable.first == variableType && variable.second.equals(variableName)) {
                return;
            }
        }
        boolean alreadyToBeAdded = false;
        for (Pair<Integer, String> toBeAddedVariable : toBeAddedVariables) {
            if (toBeAddedVariable.first == variableType
                    && toBeAddedVariable.second.equals(variableName)) {
                alreadyToBeAdded = true;
                break;
            }
        }
        if (!alreadyToBeAdded) {
            toBeAddedVariables.add(new Pair<>(variableType, variableName));
        }
    }

    private void maybeAddList(int listType, String listName) {
        if (toBeAddedLists == null) {
            toBeAddedLists = new ArrayList<>();
        }
        for (Pair<Integer, String> list :
                ProjectDataManager.getProjectDataManager(sc_id)
                        .getListVariables(activityJavaName)) {
            if (list.first == listType && list.second.equals(listName)) {
                return;
            }
        }

        boolean alreadyToBeAdded = false;
        for (Pair<Integer, String> toBeAddedList : toBeAddedLists) {
            if (toBeAddedList.first == listType && toBeAddedList.second.equals(listName)) {
                alreadyToBeAdded = true;
                break;
            }
        }
        if (!alreadyToBeAdded) {
            toBeAddedLists.add(new Pair<>(listType, listName));
        }
    }

    private void maybeAddSound(String soundName) {
        if (toBeAddedSounds == null) {
            toBeAddedSounds = new ArrayList<>();
        }
        for (String soundInProjectName :
                ProjectDataManager.getResourceManager(sc_id).getSoundNames()) {
            if (soundInProjectName.equals(soundName)) {
                return;
            }
        }
        ProjectResourceBean sound =
                SoundCollectionManager.getInstance().getResourceByName(soundName);
        if (sound != null) {
            boolean alreadyToBeAdded = false;
            for (ProjectResourceBean toBeAddedSound : toBeAddedSounds) {
                if (toBeAddedSound.resName.equals(soundName)) {
                    alreadyToBeAdded = true;
                    break;
                }
            }
            if (!alreadyToBeAdded) {
                toBeAddedSounds.add(sound);
            }
        } else {
            missingResources.add(soundName);
        }
    }

    private void maybeAddFont(String fontName) {
        if (fontName.equals("default_font")) return;
        if (toBeAddedFonts == null) {
            toBeAddedFonts = new ArrayList<>();
        }
        for (String fontInProjectName :
                ProjectDataManager.getResourceManager(sc_id).getFontNames()) {
            if (fontInProjectName.equals(fontName)) {
                return;
            }
        }
        ProjectResourceBean font = FontCollectionManager.getInstance().getResourceByName(fontName);
        if (font != null) {
            boolean alreadyToBeAdded = false;
            for (ProjectResourceBean toBeAddedFont : toBeAddedFonts) {
                if (toBeAddedFont.resName.equals(fontName)) {
                    alreadyToBeAdded = true;
                    break;
                }
            }
            if (!alreadyToBeAdded) {
                toBeAddedFonts.add(font);
            }
        } else {
            missingResources.add(fontName);
        }
    }

    private void maybeAddImage(String imageName) {
        if (imageName.equals("app_icon")
                || imageName.equals("default_image")
                || imageName.equals("NONE")) return;
        if (toBeAddedImages == null) {
            toBeAddedImages = new ArrayList<>();
        }
        for (String imageInProjectName :
                ProjectDataManager.getResourceManager(sc_id).getImageNames()) {
            if (imageInProjectName.equals(imageName)) {
                return;
            }
        }
        ProjectResourceBean image =
                ImageCollectionManager.getInstance().getResourceByName(imageName);
        if (image != null) {
            boolean alreadyToBeAdded = false;
            for (ProjectResourceBean toBeAddedImage : toBeAddedImages) {
                if (toBeAddedImage.resName.equals(imageName)) {
                    alreadyToBeAdded = true;
                    break;
                }
            }
            if (!alreadyToBeAdded) {
                toBeAddedImages.add(image);
            }
        } else {
            missingResources.add(imageName);
        }
    }

    public interface Callback {
        void onImportComplete();
    }
}
