package mod.jbk.editor.manage;


import android.util.Log;
import android.app.Activity;
import android.text.InputType;
import android.util.Pair;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.besome.sketch.beans.BlockBean;
import com.besome.sketch.beans.MoreBlockCollectionBean;
import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.beans.ProjectResourceBean;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import a.a.a.Gx;
import a.a.a.ImageCollectionManager;
import a.a.a.SoundCollectionManager;
import a.a.a.FontCollectionManager;
import a.a.a.IdentifierValidator;
import a.a.a.SketchToast;
import a.a.a.ProjectDataStore;
import a.a.a.ProjectDataManager;
import a.a.a.UIHelper;
import a.a.a.EncryptedFileUtil;
import a.a.a.BlockConstants;
import a.a.a.ViewUtil;
import a.a.a.SketchwarePaths;
import mod.hey.studios.moreblock.ReturnMoreblockManager;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;

public class MoreblockImporter {
    private final Activity activity;
    private final String sc_id;
    private final ProjectFileBean projectActivity;
    private final String activityJavaName;

    private final EncryptedFileUtil fileUtil = new EncryptedFileUtil();

    private ArrayList<Pair<Integer, String>> toBeAddedVariables;
    private ArrayList<Pair<Integer, String>> toBeAddedLists;
    private ArrayList<ProjectResourceBean> toBeAddedImages;
    private ArrayList<ProjectResourceBean> toBeAddedSounds;
    private ArrayList<ProjectResourceBean> toBeAddedFonts;

    private Callback callback;

    public MoreblockImporter(Activity activity, String sc_id, ProjectFileBean projectActivity) {
        this.activity = activity;
        this.sc_id = sc_id;
        this.projectActivity = projectActivity;
        activityJavaName = projectActivity.getJavaName();
    }

    public void importMoreblock(MoreBlockCollectionBean moreblock, Callback callback) {
        this.callback = callback;
        String blockName = ReturnMoreblockManager.getMbName(ReturnMoreblockManager.getMbNameWithTypeFromSpec(moreblock.spec));

        boolean duplicateNameFound = false;
        for (Pair<String, String> projectMoreBlock : ProjectDataManager.getProjectDataManager(sc_id).i(activityJavaName)) {
            if (ReturnMoreblockManager.getMbName(projectMoreBlock.first).equals(blockName)) {
                duplicateNameFound = true;
                break;
            }
        }
        if (!duplicateNameFound) {
            handleVariables(moreblock);
        } else {
            showEditMoreBlockNameDialog(moreblock);
        }
    }

    private void handleVariables(MoreBlockCollectionBean moreBlock) {
        toBeAddedVariables = new ArrayList<>();
        toBeAddedLists = new ArrayList<>();
        toBeAddedImages = new ArrayList<>();
        toBeAddedSounds = new ArrayList<>();
        toBeAddedFonts = new ArrayList<>();
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
            ArrayList<Gx> paramClassInfo = next.getParamClassInfo();
            if (!paramClassInfo.isEmpty()) {
                for (int i = 0; i < paramClassInfo.size(); i++) {
                    Gx gx = paramClassInfo.get(i);
                    String str = next.parameters.get(i);
                    if (!str.isEmpty() && str.charAt(0) != '@') {
                        if (gx.isExactType("boolean.SelectBoolean")) {
                            maybeAddVariable(0, str);
                        } else if (gx.isExactType("double.SelectDouble")) {
                            maybeAddVariable(1, str);
                        } else if (gx.isExactType("String.SelectString")) {
                            maybeAddVariable(2, str);
                        } else if (gx.isExactType("Map")) {
                            maybeAddVariable(3, str);
                        } else if (gx.isExactType("ListInt")) {
                            maybeAddList(1, str);
                        } else if (gx.isExactType("ListString")) {
                            maybeAddList(2, str);
                        } else if (gx.isExactType("ListMap")) {
                            maybeAddList(3, str);
                        } else if (!gx.isExactType("resource_bg") && !gx.isExactType("resource")) {
                            if (gx.isExactType("sound")) {
                                maybeAddSound(str);
                            } else if (gx.isExactType("font")) {
                                maybeAddFont(str);
                            }
                        } else {
                            maybeAddImage(str);
                        }
                    }
                }
            }
        }
        if (toBeAddedVariables.isEmpty() && toBeAddedLists.isEmpty() && toBeAddedImages.isEmpty() && toBeAddedSounds.isEmpty() && toBeAddedFonts.isEmpty()) {
            createEvent(moreBlock);
        } else {
            showAutoAddDialog(moreBlock);
        }
    }

    private void createEvent(MoreBlockCollectionBean moreBlock) {
        String moreBlockName = ReturnMoreblockManager.getMbNameWithTypeFromSpec(moreBlock.spec);

        ProjectDataManager.getProjectDataManager(sc_id).a(activityJavaName, moreBlockName, moreBlock.spec);
        ProjectDataManager.getProjectDataManager(sc_id).a(activityJavaName, moreBlockName + "_moreBlock", moreBlock.blocks);
        SketchToast.toast(activity, activity.getString(R.string.common_message_complete_save), 0).show();
        callback.onImportComplete();
    }

    private void showAutoAddDialog(MoreBlockCollectionBean moreBlock) {
        MaterialAlertDialogBuilder aBVar = new MaterialAlertDialogBuilder(activity);
        aBVar.setTitle(R.string.logic_more_block_title_add_variable_resource);
        aBVar.setIcon(R.drawable.break_warning_96_red);
        aBVar.setMessage(R.string.logic_more_block_desc_add_variable_resource);
        aBVar.setPositiveButton(R.string.common_word_continue, (v, which) -> {
            for (Pair<Integer, String> pair : toBeAddedVariables) {
                ProjectDataStore ProjectDataStore = ProjectDataManager.getProjectDataManager(sc_id);
                ProjectDataStore.c(activityJavaName, pair.first, pair.second);
            }
            for (Pair<Integer, String> pair : toBeAddedLists) {
                ProjectDataStore ProjectDataStore = ProjectDataManager.getProjectDataManager(sc_id);
                ProjectDataStore.b(activityJavaName, pair.first, pair.second);
            }
            for (ProjectResourceBean bean : toBeAddedImages) {
                copyImageFromCollectionsToProject(bean.resName);
            }
            for (ProjectResourceBean bean : toBeAddedSounds) {
                copySoundFromCollectionsToProject(bean.resName);
            }
            for (ProjectResourceBean bean : toBeAddedFonts) {
                copyFontFromCollectionsToProject(bean.resName);
            }
            createEvent(moreBlock);
            v.dismiss();
        });
        aBVar.setNegativeButton(R.string.common_word_cancel, null);
        aBVar.show();
    }

    private void showEditMoreBlockNameDialog(MoreBlockCollectionBean moreBlock) {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(activity);
        dialog.setTitle(R.string.logic_more_block_title_change_block_name);
        dialog.setIcon(R.drawable.more_block_96dp);

        View customView = ViewUtil.a(activity, R.layout.property_popup_save_to_favorite);
        ((TextView) customView.findViewById(R.id.tv_favorites_guide)).setText(R.string.logic_more_block_desc_change_block_name);
        EditText newName = customView.findViewById(R.id.ed_input);
        newName.setPrivateImeOptions("defaultInputmode=english;");
        newName.setLines(1);
        newName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        newName.setImeOptions(EditorInfo.IME_ACTION_DONE);

        List<String> moreBlockNamesWithoutReturnTypes = new LinkedList<>();
        for (String moreBlockName : ProjectDataManager.getProjectDataManager(sc_id).a(projectActivity)) {
            moreBlockNamesWithoutReturnTypes.add(ReturnMoreblockManager.getMbName(moreBlockName));
        }

        IdentifierValidator validator = new IdentifierValidator(activity, customView.findViewById(R.id.ti_input), BlockConstants.RESERVED_KEYWORDS, BlockConstants.COMPONENT_TYPES, new ArrayList<>(moreBlockNamesWithoutReturnTypes));
        dialog.setView(customView);
        dialog.setPositiveButton(R.string.common_word_save, (v, which) -> {
            if (validator.isValid()) {
                String moreBlockName = ReturnMoreblockManager.getMbName(ReturnMoreblockManager.getMbNameWithTypeFromSpec(moreBlock.spec));
                moreBlock.spec = Helper.getText(newName) + moreBlock.spec.substring(moreBlockName.length());

                handleVariables(moreBlock);
                UIHelper.a(activity, newName);
                v.dismiss();
            }
        });
        dialog.setNegativeButton(R.string.common_word_cancel, (v, which) -> {
            UIHelper.a(activity, newName);
            v.dismiss();
        });
        dialog.show();
    }

    private void copyImageFromCollectionsToProject(String imageName) {
        if (SoundCollectionManager.g().b(imageName)) {
            ProjectResourceBean image = SoundCollectionManager.g().a(imageName);
            try {
                fileUtil.a(SketchwarePaths.getCollectionPath() + File.separator + "image" + File.separator + "data" + File.separator + image.resFullName, SketchwarePaths.getImagesPath() + File.separator + sc_id + File.separator + image.resFullName);
                ProjectDataManager.getResourceManager(sc_id).b.add(image);
            } catch (Exception e) {
                Log.e("MoreblockImporter", e.getMessage(), e);
            }
        }
    }

    private void copySoundFromCollectionsToProject(String soundName) {
        if (FontCollectionManager.g().b(soundName)) {
            ProjectResourceBean a2 = FontCollectionManager.g().a(soundName);
            try {
                fileUtil.a(SketchwarePaths.getCollectionPath() + File.separator + "sound" + File.separator + "data" + File.separator + a2.resFullName, SketchwarePaths.getSoundsPath() + File.separator + sc_id + File.separator + a2.resFullName);
                ProjectDataManager.getResourceManager(sc_id).c.add(a2);
            } catch (Exception e) {
                Log.e("MoreblockImporter", e.getMessage(), e);
            }
        }
    }

    private void copyFontFromCollectionsToProject(String fontName) {
        if (ImageCollectionManager.g().b(fontName)) {
            ProjectResourceBean font = ImageCollectionManager.g().a(fontName);
            try {
                fileUtil.a(SketchwarePaths.getCollectionPath() + File.separator + "font" + File.separator + "data" + File.separator + font.resFullName, SketchwarePaths.getFontsResourcePath() + File.separator + sc_id + File.separator + font.resFullName);
                ProjectDataManager.getResourceManager(sc_id).d.add(font);
            } catch (Exception e) {
                Log.e("MoreblockImporter", e.getMessage(), e);
            }
        }
    }

    private void maybeAddVariable(int variableType, String variableName) {
        if (toBeAddedVariables == null) {
            toBeAddedVariables = new ArrayList<>();
        }
        for (Pair<Integer, String> variable : ProjectDataManager.getProjectDataManager(sc_id).k(activityJavaName)) {
            if (variable.first == variableType && variable.second.equals(variableName)) {
                return;
            }
        }
        boolean alreadyToBeAdded = false;
        for (Pair<Integer, String> toBeAddedVariable : toBeAddedVariables) {
            if (toBeAddedVariable.first == variableType && toBeAddedVariable.second.equals(variableName)) {
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
        for (Pair<Integer, String> list : ProjectDataManager.getProjectDataManager(sc_id).j(activityJavaName)) {
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
        for (String soundInProjectName : ProjectDataManager.getResourceManager(sc_id).p()) {
            if (soundInProjectName.equals(soundName)) {
                return;
            }
        }
        ProjectResourceBean sound = FontCollectionManager.g().a(soundName);
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
        }
    }

    private void maybeAddFont(String fontName) {
        if (toBeAddedFonts == null) {
            toBeAddedFonts = new ArrayList<>();
        }
        for (String fontInProjectName : ProjectDataManager.getResourceManager(sc_id).k()) {
            if (fontInProjectName.equals(fontName)) {
                return;
            }
        }
        ProjectResourceBean font = ImageCollectionManager.g().a(fontName);
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
        }
    }

    private void maybeAddImage(String imageName) {
        if (toBeAddedImages == null) {
            toBeAddedImages = new ArrayList<>();
        }
        for (String imageInProjectName : ProjectDataManager.getResourceManager(sc_id).m()) {
            if (imageInProjectName.equals(imageName)) {
                return;
            }
        }
        ProjectResourceBean image = SoundCollectionManager.g().a(imageName);
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
        }
    }

    public interface Callback {
        void onImportComplete();
    }
}
