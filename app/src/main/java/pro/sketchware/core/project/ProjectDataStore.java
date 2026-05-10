package pro.sketchware.core.project;

import android.util.Log;
import android.util.Pair;

import com.besome.sketch.beans.BlockBean;
import com.besome.sketch.beans.ComponentBean;
import com.besome.sketch.beans.EventBean;
import com.besome.sketch.beans.LayoutBean;
import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.beans.ProjectLibraryBean;
import com.besome.sketch.beans.ViewBean;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import pro.sketchware.core.project.ClassInfo;
import pro.sketchware.core.project.ProjectDataParser;
import pro.sketchware.core.util.EncryptedFileUtil;

/**
 * In-memory store for all editable project data: views, blocks, variables, lists,
 * MoreBlocks, components, events, and FAB configuration.
 * <p>
 * Data is loaded from encrypted files in {@code .sketchware/data/{sc_id}/} and
 * optionally from backup files in {@code .sketchware/mysc/backup/{sc_id}/}.
 * All maps are keyed by the XML filename (for views) or Java filename (for logic data).
 * <p>
 * Obtain instances through {@link ProjectDataManager#getProjectDataManager(String)}.
 *
 * @see ProjectDataManager
 * @see EncryptedFileUtil
 */
public class ProjectDataStore {
  public String projectId;
  public EncryptedFileUtil fileUtil;
  public HashMap<String, ArrayList<ViewBean>> viewMap;
  public HashMap<String, HashMap<String, ArrayList<BlockBean>>> blockMap;
  public HashMap<String, ArrayList<Pair<Integer, String>>> variableMap;
  public HashMap<String, ArrayList<Pair<Integer, String>>> listMap;
  public HashMap<String, ArrayList<Pair<String, String>>> moreBlockMap;
  public HashMap<String, ArrayList<ComponentBean>> componentMap;
  public HashMap<String, ArrayList<EventBean>> eventMap;
  public HashMap<String, ViewBean> fabMap;
  public static final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
  public BuildConfig buildConfig;
  
  /**
   * Creates a new data store for the given project, initializing all maps to empty.
   *
   * @param projectId the project identifier (e.g. {@code "601"})
   */
  public ProjectDataStore(String projectId) {
    clearAllData();
    this.projectId = projectId;
    fileUtil = new EncryptedFileUtil();
    buildConfig = new BuildConfig();
  }
  
  /**
   * Returns a flattened, depth-first ordered list of views starting from root views.
   * Root views are sorted by their {@link ViewBean#index}, then child views of
   * container types are recursively appended.
   *
   * @param list all views in a layout file (unsorted)
   * @return views ordered for code generation (root first, then children depth-first)
   */
  public static ArrayList<ViewBean> getSortedRootViews(ArrayList<ViewBean> list) {
    ArrayList<ViewBean> sortedViews = new ArrayList<>();
    for (ViewBean viewBean : list) {
      if (viewBean.parent.equals("root"))
        sortedViews.add(viewBean); 
    } 
    sortedViews.sort(Comparator.comparingInt(v -> v.index));

    for (ViewBean viewBean : list) {
      int viewType = viewBean.type;
      if ((viewType == 2 || viewType == 1 || viewType == 36 || viewType == 37 || viewType == 38 || viewType == 39 || viewType == 40 || viewType == 0 || viewType == 12) && viewBean.parent.equals("root"))
        sortedViews.addAll(getChildViews(list, viewBean)); 
    } 
    return sortedViews;
  }
  
  /**
   * Recursively collects and sorts child views of the given parent, depth-first.
   *
   * @param list       all views in a layout file
   * @param parentBean the parent view whose children to collect
   * @return sorted child views (including nested children of container types)
   */
  public static ArrayList<ViewBean> getChildViews(ArrayList<ViewBean> list, ViewBean parentBean) {
    ArrayList<ViewBean> childViews = new ArrayList<>();
    for (ViewBean viewBean : list) {
      if (viewBean.parent.equals(parentBean.id))
        childViews.add(viewBean); 
    } 
    childViews.sort(Comparator.comparingInt(v -> v.index));

    for (ViewBean viewBean : list) {
      if (viewBean.parent.equals(parentBean.id)) {
        int viewType = viewBean.type;
        if (viewType == 0 || viewType == 2 || viewType == 1 || viewType == 36 || viewType == 37 || viewType == 38 || viewType == 39 || viewType == 40 || viewType == 12)
          childViews.addAll(getChildViews(list, viewBean)); 
      } 
    } 
    return childViews;
  }
  
  /**
   * Returns the component at the given index for the specified Java file.
   *
   * @param fileName the Java filename key (e.g. {@code "MainActivity.java"})
   * @param componentIndex    the component's position in the list
   * @return the component bean, or {@code null} if the file has no components
   */
  public ComponentBean getComponent(String fileName, int componentIndex) {
    return !componentMap.containsKey(fileName) ? null : componentMap.get(fileName).get(componentIndex);
  }
  
  /**
   * Collects all identifiers (variable names, list names, MoreBlock names,
   * view IDs, component IDs) defined in the given file.
   * Used for name collision detection when adding new identifiers.
   *
   * @param fileBean the project file descriptor
   * @return list of all identifier strings
   */
  public ArrayList<String> getAllIdentifiers(ProjectFileBean fileBean) {
    String xmlName = fileBean.getXmlName();
    String javaName = fileBean.getJavaName();
    ArrayList<String> identifiers = new ArrayList<>();
    for (Pair<Integer, String> var : getVariables(javaName))
      identifiers.add(var.second);
    for (Pair<Integer, String> listVar : getListVariables(javaName))
      identifiers.add(listVar.second);
    for (Pair<String, String> moreBlock : getMoreBlocks(javaName))
      identifiers.add(moreBlock.first);
    for (ViewBean view : getViews(xmlName))
      identifiers.add(view.id);
    for (ComponentBean comp : getComponents(javaName))
      identifiers.add(comp.componentId);
    return identifiers;
  }
  
  /**
   * Returns all events that target the given component in the specified file.
   *
   * @param fileName      the Java filename key
   * @param componentBean the component to filter events for
   * @return matching events, or an empty list if none found
   */
  public ArrayList<EventBean> getComponentEvents(String fileName, ComponentBean componentBean) {
    if (!eventMap.containsKey(fileName))
      return new ArrayList<>(); 
    ArrayList<EventBean> filteredEvents = new ArrayList<>();
    for (EventBean eventBean : eventMap.get(fileName)) {
      if (eventBean.targetType == componentBean.type && eventBean.targetId.equals(componentBean.componentId))
        filteredEvents.add(eventBean); 
    } 
    return filteredEvents;
  }
  
  /**
   * Returns the block chain for a specific event/MoreBlock in the given file.
   *
   * @param fileName the Java filename key (e.g. {@code "MainActivity.java"})
   * @param blockKey     the event or MoreBlock identifier (e.g. {@code "onCreate"},
   *                 {@code "btn1_onClick"}, {@code "myFunc_moreBlock_blocks"})
   * @return the list of blocks in the chain, or an empty list
   */
  public ArrayList<BlockBean> getBlocks(String fileName, String blockKey) {
    if (!blockMap.containsKey(fileName))
      return new ArrayList<>(); 
    HashMap<String, ArrayList<BlockBean>> blockEntryMap = blockMap.get(fileName);
    if (blockEntryMap == null || !blockEntryMap.containsKey(blockKey))
      return new ArrayList<>();
    return blockEntryMap.get(blockKey);
  }
  
  /**
   * Deletes the view and logic backup files for this project.
   */
  public void deleteBackupFiles() {
    String backupDir = SketchwarePaths.getBackupPath(projectId);
    fileUtil.deleteFileByPath(backupDir + File.separator + "view");
    fileUtil.deleteFileByPath(backupDir + File.separator + "logic");
  }

  private String decryptFileToString(String filePath) throws IOException {
    byte[] fileBytes = fileUtil.readFileBytes(filePath);
    if (fileBytes == null && new File(filePath).length() > 0L)
      throw new IOException("Failed to read project data file: " + filePath);
    return fileUtil.decryptToString(fileBytes);
  }

  private byte[] encryptStringForStorage(String content, String filePath) throws IOException {
    try {
      return fileUtil.encryptString(content);
    } catch (GeneralSecurityException e) {
      throw new IOException("Failed to encrypt project data file: " + filePath, e);
    }
  }

  private void loadLogicFromPath(String logicPath, String sourceName) {
    try (BufferedReader reader = new BufferedReader(new StringReader(decryptFileToString(logicPath)))) {
      readLogicData(reader);
    } catch (IOException | RuntimeException e) {
      Log.e("ProjectDataStore", "Failed to load " + sourceName + " for project " + projectId + " from " + logicPath, e);
    }
  }

  private void loadViewFromPath(String viewPath, String sourceName) {
    try (BufferedReader reader = new BufferedReader(new StringReader(decryptFileToString(viewPath)))) {
      readViewData(reader);
    } catch (IOException | RuntimeException e) {
      Log.e("ProjectDataStore", "Failed to load " + sourceName + " for project " + projectId + " from " + viewPath, e);
    }
  }
  
  /**
   * Synchronizes this data store with the file manager by removing orphaned
   * references: views for deleted layout files, events/blocks for deleted
   * activities, invalid custom view references, and stale intentSetScreen targets.
   *
   * @param fileManager the project file manager containing current file list
   */
  public void syncWithFileManager(ProjectFileManager fileManager) {
    for (ProjectFileBean projectFileBean : fileManager.getActivities()) {
      if (!projectFileBean.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_FAB))
        removeFab(projectFileBean); 
      if (!projectFileBean.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_DRAWER))
        removeViewTypeEvents(projectFileBean.getJavaName()); 
    } 
    ArrayList<String> viewKeysToRemove = new ArrayList<>();
    for (Map.Entry<String, ArrayList<ViewBean>> entry : viewMap.entrySet()) {
      String key = entry.getKey();
      if (!fileManager.hasXmlName(key)) {
        viewKeysToRemove.add(key);
        continue;
      } 
      for (ViewBean viewBean : entry.getValue()) {
        if (viewBean.type == 9 || viewBean.type == 10 || viewBean.type == 25 || viewBean.type == 48 || viewBean.type == 31) {
          String customViewName = viewBean.customView;
          if (customViewName != null && customViewName.length() > 0 && !customViewName.equals("none")) {
            boolean found = false;
            for (ProjectFileBean customView : fileManager.getCustomViews()) {
              if (customView.fileName.equals(viewBean.customView)) {
                found = true;
                break;
              }
            }
            if (!found)
              viewBean.customView = ""; 
          } 
        } 
      } 
    } 
    for (String key : viewKeysToRemove)
      viewMap.remove(key); 
    variableMap.entrySet().removeIf(entry -> !fileManager.hasJavaName(entry.getKey()));
    listMap.entrySet().removeIf(entry -> !fileManager.hasJavaName(entry.getKey()));
    moreBlockMap.entrySet().removeIf(entry -> !fileManager.hasJavaName(entry.getKey()));
    componentMap.entrySet().removeIf(entry -> !fileManager.hasJavaName(entry.getKey()));
    eventMap.entrySet().removeIf(entry -> !fileManager.hasJavaName(entry.getKey()));
    ArrayList<String> blockKeysToRemove = new ArrayList<>();
    for (Map.Entry<String, HashMap<String, ArrayList<BlockBean>>> entry : blockMap.entrySet()) {
      String key = entry.getKey();
      if (!fileManager.hasJavaName(key)) {
        blockKeysToRemove.add(key);
        continue;
      } 
      for (Map.Entry<String, ArrayList<BlockBean>> blockEntry : entry.getValue().entrySet()) {
        for (BlockBean blockBean : blockEntry.getValue()) {
          if (blockBean.opCode.equals("intentSetScreen")) {
            boolean activityExists = false;
            for (ProjectFileBean activity : fileManager.getActivities()) {
              if (activity.getActivityName().equals(blockBean.parameters.get(1))) {
                activityExists = true;
                break;
              }
            }
            if (!activityExists)
              blockBean.parameters.set(1, "");
          } 
        } 
      } 
    } 
    for (String key : blockKeysToRemove)
      blockMap.remove(key); 
  }
  
  /**
   * Resets font references in setTypeface blocks to {@code "default_font"}
   * if the referenced font no longer exists in the resource manager.
   *
   * @param resourceManager the resource manager with current font list
   */
  public void syncFonts(ResourceManager resourceManager) {
    HashSet<String> fontNames = new HashSet<>(resourceManager.getFontNames());
    for (HashMap<String, ArrayList<BlockBean>> fileBlocks : blockMap.values()) {
      for (ArrayList<BlockBean> blocks : fileBlocks.values()) {
        for (BlockBean blockBean : blocks) {
          if ("setTypeface".equals(blockBean.opCode) && !fontNames.contains(blockBean.parameters.get(1)))
            blockBean.parameters.set(1, "default_font"); 
        } 
      } 
    } 
  }
  
  /**
   * Removes a view and all its associated events and block references.
   * For custom view files, also cleans up onBindCustomView block references
   * across all files that use this custom view.
   *
   * @param fileBean   the file containing the view
   * @param targetBean the view to remove
   */
  public void removeView(ProjectFileBean fileBean, ViewBean targetBean) {
    if (!viewMap.containsKey(fileBean.getXmlName()))
      return; 
    ArrayList<ViewBean> views = viewMap.get(fileBean.getXmlName());
    for (int i = views.size() - 1; i >= 0; i--) {
      if (views.get(i).id.equals(targetBean.id)) {
        views.remove(i);
        break;
      }
    }
    int fileType = fileBean.fileType;
    if (fileType == 0) {
      removeEventsByTarget(fileBean.getJavaName(), targetBean.id);
      removeBlockReferences(fileBean.getJavaName(), targetBean.getClassInfo(), targetBean.id, true);
    } else if (fileType == 1) {
      ArrayList<Pair<String, String>> customViewPairs = new ArrayList<>();
      for (Map.Entry<String, ArrayList<ViewBean>> entry : viewMap.entrySet()) {
        for (ViewBean viewBean : entry.getValue()) {
          if ((viewBean.type == 9 || viewBean.type == 10 || viewBean.type == 25 || viewBean.type == 48 || viewBean.type == 31) && viewBean.customView.equals(fileBean.fileName)) {
            String eventName = viewBean.id + "_onBindCustomView";
            String xmlKey = entry.getKey();
            customViewPairs.add(new Pair<>(ProjectFileBean.getJavaName(xmlKey.substring(0, xmlKey.lastIndexOf(".xml"))), eventName));
          } 
        } 
      } 
      for (Pair<String, String> viewPair : customViewPairs) {
        if (!blockMap.containsKey(viewPair.first))
          continue;
        HashMap<String, ArrayList<BlockBean>> blockEntryMap = blockMap.get(viewPair.first);
        if (!blockEntryMap.containsKey(viewPair.second))
          continue;
        ArrayList<BlockBean> blockBeans = blockEntryMap.get(viewPair.second);
        if (blockBeans == null || blockBeans.isEmpty())
          continue;
        for (int i = blockBeans.size() - 1; i >= 0; i--) {
          BlockBean blockBean = blockBeans.get(i);
          ClassInfo blockClassInfo = blockBean.getClassInfo();
          if (blockClassInfo != null && blockClassInfo.isExactType(targetBean.getClassInfo().getClassName()) && blockBean.spec.equals(targetBean.id)) {
            blockBeans.remove(i);
            continue;
          }
          ArrayList<ClassInfo> paramClassInfos = blockBean.getParamClassInfo();
          if (paramClassInfos != null && !paramClassInfos.isEmpty()) {
            for (int b = 0; b < paramClassInfos.size(); b++) {
              ClassInfo paramClassInfo = paramClassInfos.get(b);
              if (paramClassInfo != null && targetBean.getClassInfo().isAssignableFrom(paramClassInfo) && blockBean.parameters.get(b).equals(targetBean.id))
                blockBean.parameters.set(b, "");
            }
          }
        }
      } 
    } else if (fileType == 2) {
      removeViewEventsByTarget(fileBean.getDrawersJavaName(), targetBean.id);
    } 
  }
  
  public void removeAdmobComponents(ProjectLibraryBean libraryBean) {
    if (libraryBean.useYn.equals("Y"))
      return; 
    for (String key : new ArrayList<>(componentMap.keySet())) {
      removeComponentsByType(key, 6);
      removeComponentsByType(key, 12);
      removeComponentsByType(key, 14);
    } 
  }
  
  public void removeFirebaseViews(ProjectLibraryBean libraryBean, ProjectFileManager fileManager) {
    if (libraryBean.useYn.equals("Y"))
      return; 
    for (ProjectFileBean projectFileBean : fileManager.getActivities()) {
      ArrayList<ViewBean> toRemove = new ArrayList<>();
      for (ViewBean viewBean : getViews(projectFileBean.getXmlName())) {
        if (viewBean.type == 17)
          toRemove.add(viewBean); 
      } 
      for (ViewBean viewBean : toRemove)
        removeView(projectFileBean, viewBean); 
    } 
    for (ProjectFileBean projectFileBean : fileManager.getCustomViews()) {
      ArrayList<ViewBean> toRemove = new ArrayList<>();
      for (ViewBean viewBean : getViews(projectFileBean.getXmlName())) {
        if (viewBean.type == 17)
          toRemove.add(viewBean); 
      } 
      for (ViewBean viewBean : toRemove)
        removeView(projectFileBean, viewBean); 
    } 
    for (String key : new ArrayList<>(componentMap.keySet()))
      removeComponentsByType(key, 13); 
  }
  
  /**
   * Parses the logic data file format, populating variables, lists, MoreBlocks,
   * components, events, and blocks maps. The format uses {@code @sectionName}
   * as section delimiters.
   *
   * @param reader a reader over the decrypted logic file content
   * @throws IOException if reading fails
   */
  public void readLogicData(BufferedReader reader) throws IOException {
    HashMap<String, ArrayList<Pair<Integer, String>>> parsedVariableMap = new HashMap<>();
    HashMap<String, ArrayList<Pair<Integer, String>>> parsedListMap = new HashMap<>();
    HashMap<String, ArrayList<Pair<String, String>>> parsedMoreBlockMap = new HashMap<>();
    HashMap<String, ArrayList<ComponentBean>> parsedComponentMap = new HashMap<>();
    HashMap<String, ArrayList<EventBean>> parsedEventMap = new HashMap<>();
    HashMap<String, HashMap<String, ArrayList<BlockBean>>> parsedBlockMap = new HashMap<>();
    StringBuilder contentBuffer = new StringBuilder();
    String sectionName = "";
    while (true) {
      String line = reader.readLine();
      if (line != null) {
        if (line.length() <= 0)
          continue; 
        if (line.charAt(0) == '@') {
          StringBuilder tempBuffer = contentBuffer;
          if (sectionName.length() > 0) {
            parseLogicSection(sectionName, contentBuffer.toString(), parsedVariableMap,
                parsedListMap, parsedMoreBlockMap, parsedComponentMap, parsedEventMap,
                parsedBlockMap);
            tempBuffer = new StringBuilder();
          } 
          sectionName = line.substring(1);
          contentBuffer = tempBuffer;
          continue;
        } 
        contentBuffer.append(line);
        contentBuffer.append("\n");
        continue;
      } 
      if (sectionName.length() > 0 && contentBuffer.length() > 0)
        parseLogicSection(sectionName, contentBuffer.toString(), parsedVariableMap,
            parsedListMap, parsedMoreBlockMap, parsedComponentMap, parsedEventMap,
            parsedBlockMap);
      variableMap = parsedVariableMap;
      listMap = parsedListMap;
      moreBlockMap = parsedMoreBlockMap;
      componentMap = parsedComponentMap;
      eventMap = parsedEventMap;
      blockMap = parsedBlockMap;
      return;
    } 
  }
  
  /**
   * Initializes a default FAB (Floating Action Button) view for the given file
   * if one does not already exist. The FAB is positioned at bottom-right with
   * 16dp margins.
   *
   * @param fileName the Java filename key
   */
  public void initFab(String fileName) {
    if (fabMap.containsKey(fileName))
      return; 
    ViewBean viewBean = new ViewBean("_fab", 16);
    LayoutBean layoutBean = viewBean.layout;
    layoutBean.marginLeft = 16;
    layoutBean.marginTop = 16;
    layoutBean.marginRight = 16;
    layoutBean.marginBottom = 16;
    layoutBean.layoutGravity = 85;
    fabMap.put(fileName, viewBean);
  }
  
  /**
   * Adds a new event to the specified file.
   *
   * @param fileName the Java filename key
   * @param eventType         the event type
   * @param targetType        the event target type
   * @param targetId          the target ID (e.g. view ID or component ID)
   * @param eventName         the event name
   */
  public void addEvent(String fileName, int eventType, int targetType, String targetId, String eventName) {
    if (!eventMap.containsKey(fileName))
      eventMap.put(fileName, new ArrayList<>()); 
    eventMap.get(fileName).add(new EventBean(eventType, targetType, targetId, eventName));
  }
  
  /**
   * Adds a new component to the specified file.
   *
   * @param fileName the Java filename key
   * @param componentType    the component type index (see {@link ComponentBean} type constants)
   * @param componentId     the component ID
   */
  public void addComponent(String fileName, int componentType, String componentId) {
    if (!componentMap.containsKey(fileName))
      componentMap.put(fileName, new ArrayList<>()); 
    componentMap.get(fileName).add(new ComponentBean(componentType, componentId));
  }
  
  /**
   * Adds a new component with an extra parameter to the specified file.
   *
   * @param fileName the Java filename key
   * @param componentType    the component type index
   * @param componentId     the component ID
   * @param parameterValue    additional configuration parameter
   */
  public void addComponentWithParam(String fileName, int componentType, String componentId, String parameterValue) {
    if (!componentMap.containsKey(fileName))
      componentMap.put(fileName, new ArrayList<>()); 
    componentMap.get(fileName).add(new ComponentBean(componentType, componentId, parameterValue));
  }
  
  /**
   * Removes or clears block references to a deleted view/component across all
   * event handlers in the given file.
   *
   * @param fileName       the Java filename key
   * @param classInfo      the class info of the removed element
   * @param targetId           the ID of the removed element
   * @param skipCustomView if {@code true}, skips onBindCustomView event handlers
   */
  public void removeBlockReferences(String fileName, ClassInfo classInfo, String targetId, boolean skipCustomView) {
    if (!blockMap.containsKey(fileName))
      return; 
    HashMap<String, ArrayList<BlockBean>> blockEntryMap = blockMap.get(fileName);
    if (blockEntryMap == null)
      return; 
    for (Map.Entry<String, ArrayList<BlockBean>> entry : blockEntryMap.entrySet()) {
      if (skipCustomView && entry.getKey().substring(entry.getKey().lastIndexOf("_") + 1).equals("onBindCustomView"))
        continue; 
      ArrayList<BlockBean> blockBeans = entry.getValue();
      if (blockBeans == null || blockBeans.isEmpty())
        continue; 
      for (int i = blockBeans.size() - 1; i >= 0; i--) {
        BlockBean blockBean = blockBeans.get(i);
        ClassInfo blockClassInfo = blockBean.getClassInfo();
        if (blockClassInfo != null && blockClassInfo.isExactType(classInfo.getClassName()) && blockBean.spec.equals(targetId)) {
          blockBeans.remove(i);
          continue;
        }
        ArrayList<ClassInfo> paramClassInfos = blockBean.getParamClassInfo();
        if (paramClassInfos != null && !paramClassInfos.isEmpty()) {
          for (int b = 0; b < paramClassInfos.size(); b++) {
            ClassInfo paramClassInfo = paramClassInfos.get(b);
            if (paramClassInfo != null && classInfo.isAssignableFrom(paramClassInfo) && blockBean.parameters.get(b).equals(targetId))
              blockBean.parameters.set(b, ""); 
          }
        }
      }
    } 
  }
  
  public void addEventBean(String fileName, EventBean eventBean) {
    if (!eventMap.containsKey(fileName))
      eventMap.put(fileName, new ArrayList<>()); 
    eventMap.get(fileName).add(eventBean);
  }
  
  public void addView(String fileName, ViewBean viewBean) {
    if (!viewMap.containsKey(fileName))
      viewMap.put(fileName, new ArrayList<>()); 
    viewMap.get(fileName).add(viewBean);
  }
  
  public void addMoreBlock(String fileName, String moreBlockName, String moreBlockSpec) {
    if (!moreBlockMap.containsKey(fileName))
      moreBlockMap.put(fileName, new ArrayList<>()); 
    moreBlockMap.get(fileName).add(new Pair<>(moreBlockName, moreBlockSpec));
  }
  
  public void putBlocks(String fileName, String blockKey, ArrayList<BlockBean> blocks) {
    if (!blockMap.containsKey(fileName))
      blockMap.put(fileName, new HashMap<>()); 
    blockMap.get(fileName).put(blockKey, blocks);
  }
  
  public final void serializeLogicData(StringBuilder buffer) {
    if (variableMap != null && !variableMap.isEmpty()) {
      for (Map.Entry<String, ArrayList<Pair<Integer, String>>> entry : variableMap.entrySet()) {
        ArrayList<Pair<Integer, String>> variables = entry.getValue();
        if (variables == null || variables.isEmpty())
          continue;
        StringBuilder contentBuilder = new StringBuilder();
        for (Pair<Integer, String> varEntry : variables) {
          contentBuilder.append(varEntry.first).append(":").append(varEntry.second).append("\n");
        }
        buffer.append("@").append(entry.getKey()).append("_var").append("\n");
        buffer.append(contentBuilder).append("\n");
      }
    }
    if (listMap != null && !listMap.isEmpty()) {
      for (Map.Entry<String, ArrayList<Pair<Integer, String>>> entry : listMap.entrySet()) {
        ArrayList<Pair<Integer, String>> listEntries = entry.getValue();
        if (listEntries == null || listEntries.isEmpty())
          continue;
        StringBuilder contentBuilder = new StringBuilder();
        for (Pair<Integer, String> listEntry : listEntries) {
          contentBuilder.append(listEntry.first).append(":").append(listEntry.second).append("\n");
        }
        buffer.append("@").append(entry.getKey()).append("_list").append("\n");
        buffer.append(contentBuilder).append("\n");
      }
    }
    if (moreBlockMap != null && !moreBlockMap.isEmpty()) {
      for (Map.Entry<String, ArrayList<Pair<String, String>>> entry : moreBlockMap.entrySet()) {
        ArrayList<Pair<String, String>> moreBlocks = entry.getValue();
        if (moreBlocks == null || moreBlocks.isEmpty())
          continue;
        StringBuilder contentBuilder = new StringBuilder();
        for (Pair<String, String> moreBlockEntry : moreBlocks) {
          contentBuilder.append(moreBlockEntry.first).append(":").append(moreBlockEntry.second).append("\n");
        }
        buffer.append("@").append(entry.getKey()).append("_func").append("\n");
        buffer.append(contentBuilder).append("\n");
      }
    }
    if (componentMap != null && !componentMap.isEmpty()) {
      for (Map.Entry<String, ArrayList<ComponentBean>> entry : componentMap.entrySet()) {
        ArrayList<ComponentBean> components = entry.getValue();
        if (components == null || components.isEmpty())
          continue;
        StringBuilder contentBuilder = new StringBuilder();
        for (ComponentBean componentBean : components) {
          componentBean.clearClassInfo();
          contentBuilder.append(gson.toJson(componentBean)).append("\n");
        }
        buffer.append("@").append(entry.getKey()).append("_components").append("\n");
        buffer.append(contentBuilder).append("\n");
      }
    }
    if (eventMap != null && !eventMap.isEmpty()) {
      for (Map.Entry<String, ArrayList<EventBean>> entry : eventMap.entrySet()) {
        ArrayList<EventBean> events = entry.getValue();
        if (events == null || events.isEmpty())
          continue;
        StringBuilder contentBuilder = new StringBuilder();
        for (EventBean eventBean : events) {
          contentBuilder.append(gson.toJson(eventBean)).append("\n");
        }
        buffer.append("@").append(entry.getKey()).append("_events").append("\n");
        buffer.append(contentBuilder).append("\n");
      }
    }
    if (blockMap != null && !blockMap.isEmpty()) {
      for (Map.Entry<String, HashMap<String, ArrayList<BlockBean>>> entry : blockMap.entrySet()) {
        String key = entry.getKey();
        HashMap<String, ArrayList<BlockBean>> blockEntryMap = entry.getValue();
        if (blockEntryMap == null || blockEntryMap.isEmpty())
          continue;
        for (Map.Entry<String, ArrayList<BlockBean>> blockEntry : blockEntryMap.entrySet()) {
          ArrayList<BlockBean> blocks = blockEntry.getValue();
          if (blocks == null || blocks.isEmpty())
            continue;
          StringBuilder contentBuilder = new StringBuilder();
          for (BlockBean blockBean : blocks) {
            contentBuilder.append(gson.toJson(blockBean)).append("\n");
          }
          buffer.append("@").append(key).append("_").append(blockEntry.getKey()).append("\n");
          buffer.append(contentBuilder).append("\n");
        }
      }
    }
  }
  
  public String getMoreBlockSpec(String fileName, String moreBlockName) {
    if (!moreBlockMap.containsKey(fileName))
      return ""; 
    ArrayList<Pair<String, String>> moreBlocks = moreBlockMap.get(fileName);
    if (moreBlocks == null)
      return ""; 
    for (Pair<String, String> moreBlockEntry : moreBlocks) {
      if (moreBlockEntry.first.equals(moreBlockName))
        return moreBlockEntry.second; 
    } 
    return "";
  }
  
  public ArrayList<String> getComponentIdsByType(String fileName, int componentType) {
    ArrayList<String> componentIds = new ArrayList<>();
    if (!componentMap.containsKey(fileName))
      return componentIds; 
    ArrayList<ComponentBean> components = componentMap.get(fileName);
    if (components == null)
      return componentIds; 
    for (ComponentBean componentBean : components) {
      if (componentBean.type == componentType)
        componentIds.add(componentBean.componentId); 
    } 
    return componentIds;
  }
  
  public ArrayList<ViewBean> getViewWithChildren(String fileName, ViewBean viewBean) {
    ArrayList<ViewBean> result = new ArrayList<>();
    result.add(viewBean);
    ArrayList<ViewBean> views = viewMap.get(fileName);
    if (views != null) {
      result.addAll(getChildViews(views, viewBean));
    }
    return result;
  }
  
  public HashMap<String, ArrayList<BlockBean>> getBlockMap(String fileName) {
    return blockMap.containsKey(fileName) ? blockMap.get(fileName) : new HashMap<>();
  }
  
  public void clearAllData() {
    if (viewMap != null) viewMap.clear();
    if (blockMap != null) blockMap.clear();
    if (variableMap != null) variableMap.clear();
    if (listMap != null) listMap.clear();
    if (moreBlockMap != null) moreBlockMap.clear();
    if (componentMap != null) componentMap.clear();
    if (eventMap != null) eventMap.clear();
    viewMap = new HashMap<>();
    blockMap = new HashMap<>();
    variableMap = new HashMap<>();
    listMap = new HashMap<>();
    moreBlockMap = new HashMap<>();
    componentMap = new HashMap<>();
    eventMap = new HashMap<>();
    fabMap = new HashMap<>();
  }
  
  public void syncImages(ResourceManager resourceManager) {
    HashSet<String> imageNames = new HashSet<>(resourceManager.getImageNames());
    for (ArrayList<ViewBean> views : viewMap.values()) {
      for (ViewBean viewBean : views) {
        if (!imageNames.contains(viewBean.layout.backgroundResource))
          viewBean.layout.backgroundResource = null; 
        if (!imageNames.contains(viewBean.image.resName))
          viewBean.image.resName = "default_image"; 
      } 
    } 
    for (ViewBean viewBean : fabMap.values()) {
      if (!imageNames.contains(viewBean.image.resName))
        viewBean.image.resName = "NONE"; 
    } 
    for (HashMap<String, ArrayList<BlockBean>> fileBlocks : blockMap.values()) {
      for (ArrayList<BlockBean> blocks : fileBlocks.values()) {
        for (BlockBean blockBean : blocks) {
          if ("setImage".equals(blockBean.opCode)) {
            if (!imageNames.contains(blockBean.parameters.get(1)))
              blockBean.parameters.set(1, "default_image"); 
            continue;
          } 
          if ("setBgResource".equals(blockBean.opCode) && !imageNames.contains(blockBean.parameters.get(1)))
            blockBean.parameters.set(1, "NONE"); 
        } 
      } 
    } 
  }
  
  public void removeFab(ProjectFileBean fileBean) {
    if (fabMap.containsKey(fileBean.getXmlName()))
      fabMap.remove(fileBean.getXmlName()); 
    removeEventsByTarget(fileBean.getJavaName(), "_fab");
}

public void removeMapViews(ProjectLibraryBean libraryBean, ProjectFileManager fileManager) {
    if (libraryBean.useYn.equals("Y"))
      return; 
    for (ProjectFileBean projectFileBean : fileManager.getActivities()) {
      for (ViewBean viewBean : getViews(projectFileBean.getXmlName())) {
        if (viewBean.type == 18)
          removeView(projectFileBean, viewBean); 
      } 
    } 
}

public void readViewData(BufferedReader reader) throws IOException {
    HashMap<String, ArrayList<ViewBean>> parsedViewMap = new HashMap<>();
    HashMap<String, ViewBean> parsedFabMap = new HashMap<>();
    StringBuilder contentBuffer = new StringBuilder();
    String sectionName = "";
    while (true) {
      String line = reader.readLine();
      if (line != null) {
        if (line.length() <= 0)
          continue; 
        if (line.charAt(0) == '@') {
          StringBuilder tempBuffer = contentBuffer;
          if (sectionName.length() > 0) {
            parseViewSection(sectionName, contentBuffer.toString(), parsedViewMap, parsedFabMap);
            tempBuffer = new StringBuilder();
          } 
          sectionName = line.substring(1);
          contentBuffer = tempBuffer;
          continue;
        } 
        contentBuffer.append(line);
        contentBuffer.append("\n");
        continue;
      } 
      if (sectionName.length() > 0 && contentBuffer.length() > 0)
        parseViewSection(sectionName, contentBuffer.toString(), parsedViewMap, parsedFabMap);
      viewMap = parsedViewMap;
      fabMap = parsedFabMap;
      return;
    } 
  }
  
  public void addListVariable(String fileName, int listType, String listName) {
    if (!listMap.containsKey(fileName))
      listMap.put(fileName, new ArrayList<>()); 
    listMap.get(fileName).add(new Pair<>(listType, listName));
  }
  
  public void removeComponent(String fileName, ComponentBean componentBean) {
    if (!componentMap.containsKey(fileName))
      return; 
    ArrayList<ComponentBean> components = componentMap.get(fileName);
    if (!components.contains(componentBean))
      return; 
    components.remove(componentBean);
    removeEventsByTarget(fileName, componentBean.componentId);
    removeBlockReferences(fileName, componentBean.getClassInfo(), componentBean.componentId, false);
    buildConfig.constVarComponent.handleDeleteComponent(componentBean.componentId);
  }
  
  public final void serializeViewData(StringBuilder buffer) {
    if (viewMap != null && !viewMap.isEmpty()) {
      for (Map.Entry<String, ArrayList<ViewBean>> entry : viewMap.entrySet()) {
        ArrayList<ViewBean> viewBeans = entry.getValue();
        if (viewBeans == null || viewBeans.isEmpty())
          continue;
        ArrayList<ViewBean> sortedViews = getSortedRootViews(viewBeans);
        StringBuilder contentBuilder = new StringBuilder();
        if (sortedViews != null && !sortedViews.isEmpty()) {
          for (ViewBean viewBean : sortedViews) {
            viewBean.clearClassInfo();
            contentBuilder.append(gson.toJson(viewBean)).append("\n");
          }
        }
        buffer.append("@").append(entry.getKey()).append("\n");
        buffer.append(contentBuilder).append("\n");
      }
    }
    if (fabMap != null && !fabMap.isEmpty()) {
      for (Map.Entry<String, ViewBean> entry : fabMap.entrySet()) {
        ViewBean viewBean = entry.getValue();
        if (viewBean == null)
          continue;
        buffer.append("@").append(entry.getKey()).append("_fab").append("\n");
        buffer.append(gson.toJson(viewBean)).append("\n");
        buffer.append("\n");
      }
    }
  }
  
  public boolean isListUsedInBlocks(String fileName, String listName, String excludedEventKey) {
    HashMap<String, ArrayList<BlockBean>> blockEntryMap = blockMap.get(fileName);
    if (blockEntryMap == null)
      return false; 
    for (Map.Entry<String, ArrayList<BlockBean>> entry : blockEntryMap.entrySet()) {
      if (entry.getKey().equals(excludedEventKey))
        continue; 
      for (BlockBean blockBean : entry.getValue()) {
        ClassInfo blockClassInfo = blockBean.getClassInfo();
        if (blockClassInfo != null && blockClassInfo.isList() && blockBean.spec.equals(listName))
          return true; 
        ArrayList<ClassInfo> paramClassInfos = blockBean.getParamClassInfo();
        if (paramClassInfos != null && !paramClassInfos.isEmpty())
          for (int b = 0; b < paramClassInfos.size(); b++) {
            ClassInfo paramClassInfo = paramClassInfos.get(b);
            if (paramClassInfo != null && paramClassInfo.isList() && blockBean.parameters.get(b).equals(listName))
              return true; 
          }  
      } 
    } 
    return false;
  }
  
  public ViewBean getViewBean(String fileName, String viewId) {
    ArrayList<ViewBean> views = viewMap.get(fileName);
    if (views == null)
      return null; 
    for (int b = 0; b < views.size(); b++) {
      ViewBean viewBean = views.get(b);
      if (viewId.equals(viewBean.id))
        return viewBean; 
    } 
    return null;
  }
  
  public ArrayList<String> getListNames(String fileName) {
    ArrayList<String> listNames = new ArrayList<>();
    if (!listMap.containsKey(fileName))
      return listNames; 
    ArrayList<Pair<Integer, String>> listVars = listMap.get(fileName);
    if (listVars == null)
      return listNames; 
    for (Pair<Integer, String> listVar : listVars)
      listNames.add(listVar.second); 
    return listNames;
  }
  
  public ArrayList<ComponentBean> getComponentsByType(String fileName, int componentType) {
    ArrayList<ComponentBean> filteredComponents = new ArrayList<>();
    if (!componentMap.containsKey(fileName))
      return filteredComponents; 
    ArrayList<ComponentBean> components = componentMap.get(fileName);
    if (components == null)
      return filteredComponents; 
    for (ComponentBean componentBean : components) {
      if (componentBean.type == componentType)
        filteredComponents.add(componentBean); 
    } 
    return filteredComponents;
  }
  
  public void syncSounds(ResourceManager resourceManager) {
    HashSet<String> soundNames = new HashSet<>(resourceManager.getSoundNames());
    for (HashMap<String, ArrayList<BlockBean>> fileBlocks : blockMap.values()) {
      for (ArrayList<BlockBean> blocks : fileBlocks.values()) {
        for (BlockBean blockBean : blocks) {
          if (blockBean.opCode.equals("mediaplayerCreate") && !soundNames.contains(blockBean.parameters.get(1)))
            blockBean.parameters.set(1, ""); 
          if (blockBean.opCode.equals("soundpoolLoad") && !soundNames.contains(blockBean.parameters.get(1)))
            blockBean.parameters.set(1, ""); 
        } 
      } 
    } 
  }
  
  public void addVariable(String fileName, int variableType, String variableName) {
    if (!variableMap.containsKey(fileName))
      variableMap.put(fileName, new ArrayList<>()); 
    variableMap.get(fileName).add(new Pair<>(variableType, variableName));
  }
  
  public boolean hasLogicBackup() {
    return hasDistinctBackup(
        SketchwarePaths.getBackupPath(projectId) + File.separator + "logic",
        SketchwarePaths.getDataPath(projectId) + File.separator + "logic");
  }
  
  public boolean isVariableUsedInBlocks(String fileName, String variableName, String excludedEventKey) {
    HashMap<String, ArrayList<BlockBean>> blockEntryMap = blockMap.get(fileName);
    if (blockEntryMap == null)
      return false; 
    for (Map.Entry<String, ArrayList<BlockBean>> entry : blockEntryMap.entrySet()) {
      if (entry.getKey().equals(excludedEventKey))
        continue; 
      for (BlockBean blockBean : entry.getValue()) {
        ClassInfo blockClassInfo = blockBean.getClassInfo();
        if (blockClassInfo != null && blockClassInfo.isVariable() && blockBean.spec.equals(variableName))
          return true; 
        ArrayList<ClassInfo> paramClassInfos = blockBean.getParamClassInfo();
        if (paramClassInfos != null && !paramClassInfos.isEmpty())
          for (int b = 0; b < paramClassInfos.size(); b++) {
            ClassInfo paramClassInfo = paramClassInfos.get(b);
            if (paramClassInfo != null && paramClassInfo.isVariable() && blockBean.parameters.get(b).equals(variableName))
              return true; 
          }  
      } 
    } 
    return false;
  }
  
  public ArrayList<VariableReference> findVariableReferences(String fileName, String variableName, boolean isList) {
    ArrayList<VariableReference> references = new ArrayList<>();
    HashMap<String, ArrayList<BlockBean>> blockEntryMap = blockMap.get(fileName);
    if (blockEntryMap == null)
      return references;
    for (Map.Entry<String, ArrayList<BlockBean>> entry : blockEntryMap.entrySet()) {
      String blockEntryKey = entry.getKey();
      for (BlockBean blockBean : entry.getValue()) {
        ClassInfo blockClassInfo = blockBean.getClassInfo();
        if (blockClassInfo != null
            && (isList ? blockClassInfo.isList() : blockClassInfo.isVariable())
            && blockBean.spec.equals(variableName)) {
          references.add(new VariableReference(blockEntryKey, blockBean.opCode, blockBean.spec, blockBean.id));
          continue;
        }
        ArrayList<ClassInfo> paramClassInfos = blockBean.getParamClassInfo();
        if (paramClassInfos != null && !paramClassInfos.isEmpty()) {
          for (int b = 0; b < paramClassInfos.size(); b++) {
            ClassInfo paramClassInfo = paramClassInfos.get(b);
            if (paramClassInfo != null
                && (isList ? paramClassInfo.isList() : paramClassInfo.isVariable())
                && b < blockBean.parameters.size()
                && blockBean.parameters.get(b).equals(variableName)) {
              references.add(new VariableReference(blockEntryKey, blockBean.opCode, blockBean.spec, blockBean.id));
              break;
            }
          }
        }
      }
    }
    return references;
  }
  
  public static class VariableReference {
    public final String blockEntryKey;
    public final String opCode;
    public final String blockSpec;
    public final String blockId;
    
    public VariableReference(String blockEntryKey, String opCode, String blockSpec, String blockId) {
      this.blockEntryKey = blockEntryKey;
      this.opCode = opCode;
      this.blockSpec = blockSpec;
      this.blockId = blockId;
    }
  }
  
  public ArrayList<ViewBean> getViews(String fileName) {
    ArrayList<ViewBean> views = viewMap.get(fileName);
    return views != null ? views : new ArrayList<>();
  }
  
  public ArrayList<String> getListNamesByType(String fileName, int listType) {
    ArrayList<String> filteredNames = new ArrayList<>();
    if (!listMap.containsKey(fileName))
      return filteredNames; 
    ArrayList<Pair<Integer, String>> listVars = listMap.get(fileName);
    if (listVars == null)
      return filteredNames; 
    for (Pair<Integer, String> listEntry : listVars) {
      if (listEntry.first == listType)
        filteredNames.add(listEntry.second); 
    } 
    return filteredNames;
  }
  
  public ArrayList<Pair<Integer, String>> getViewsByType(String fileName, String viewTypeName) {
    ArrayList<Pair<Integer, String>> filteredViews = new ArrayList<>();
    ArrayList<ViewBean> viewBeans = viewMap.get(fileName);
    if (viewBeans == null)
      return filteredViews; 
    for (ViewBean viewBean : viewBeans) {
      Pair<Integer, String> pair;
      if (viewTypeName.equals("CheckBox")) {
        if (viewBean.getClassInfo().matchesType("CompoundButton")) {
          pair = new Pair<>(viewBean.type, viewBean.id);
        } else {
          continue;
        } 
      } else if (viewBean.getClassInfo().matchesType(viewTypeName)) {
        pair = new Pair<>(viewBean.type, viewBean.id);
      } else {
        continue;
      } 
      filteredViews.add(pair);
    } 
    return filteredViews;
  }
  
  public void removeEvent(String fileName, String targetId, String eventName) {
    if (!eventMap.containsKey(fileName))
      return; 
    ArrayList<EventBean> events = eventMap.get(fileName);
    if (events == null)
      return; 
    for (int i = events.size() - 1; i >= 0; i--) {
      EventBean eventBean = events.get(i);
      if (eventBean.targetId.equals(targetId) && eventName.equals(eventBean.eventName)) {
        events.remove(i);
        HashMap<String, ArrayList<BlockBean>> fileBlocks = blockMap != null ? blockMap.get(fileName) : null;
        if (fileBlocks != null) {
          String eventKey = eventBean.targetId + "_" + eventBean.eventName;
          fileBlocks.remove(eventKey);
        }
      }
    }
  }
  
  public boolean hasViewBackup() {
    return hasDistinctBackup(
        SketchwarePaths.getBackupPath(projectId) + File.separator + "view",
        SketchwarePaths.getDataPath(projectId) + File.separator + "view");
  }

  private boolean hasDistinctBackup(String backupPath, String dataPath) {
    if (!fileUtil.exists(backupPath))
      return false; 
    byte[] backupBytes = fileUtil.readFileBytes(backupPath);
    if (backupBytes == null || backupBytes.length == 0)
      return false; 
    byte[] dataBytes = fileUtil.readFileBytes(dataPath);
    return dataBytes == null || !Arrays.equals(backupBytes, dataBytes);
  }

  private boolean hasUsableBackup(String backupPath) {
    if (!fileUtil.exists(backupPath))
      return false;
    byte[] backupBytes = fileUtil.readFileBytes(backupPath);
    return backupBytes != null && backupBytes.length > 0;
  }
  
  public boolean hasComponent(String fileName, int componentType, String componentId) {
    ArrayList<ComponentBean> components = componentMap.get(fileName);
    if (components == null)
      return false; 
    for (ComponentBean componentBean : components) {
      if (componentBean.type == componentType && componentBean.componentId.equals(componentId))
        return true; 
    } 
    return false;
  }
  
  public ArrayList<ComponentBean> getComponents(String fileName) {
    return !componentMap.containsKey(fileName) ? new ArrayList<>() : componentMap.get(fileName);
  }
  
  public ArrayList<String> getVariableNamesByType(String fileName, int variableType) {
    ArrayList<String> varNames = new ArrayList<>();
    if (!variableMap.containsKey(fileName))
      return varNames; 
    ArrayList<Pair<Integer, String>> varEntries = variableMap.get(fileName);
    if (varEntries == null)
      return varNames; 
    for (Pair<Integer, String> varEntry : varEntries) {
      if (varEntry.first == variableType)
        varNames.add(varEntry.second); 
    } 
    return varNames;
  }
  
  public void loadLogicFromData() {
    String logicPath = SketchwarePaths.getDataPath(projectId) + File.separator + "logic";
    if (!fileUtil.exists(logicPath))
      return;
    loadLogicFromPath(logicPath, "logic data");
  }
  
  public boolean hasListVariable(String fileName, int listType, String listName) {
    ArrayList<Pair<Integer, String>> listVars = listMap.get(fileName);
    if (listVars == null)
      return false; 
    for (Pair<Integer, String> listEntry : listVars) {
      if (listEntry.first == listType && listEntry.second.equals(listName))
        return true; 
    } 
    return false;
  }
  
  public boolean hasCompoundButtonView(String fileName, String viewId) {
    ArrayList<ViewBean> views = viewMap.get(fileName);
    if (views == null)
      return false; 
    for (ViewBean viewBean : views) {
      if (viewBean.getClassInfo().matchesType("CompoundButton") && viewBean.id.equals(viewId))
        return true; 
    } 
    return false;
  }
  
  public ArrayList<ViewBean> getCustomViewBeans(String xmlName) {
    ArrayList<ViewBean> customViews = new ArrayList<>();
    ArrayList<ViewBean> viewBeans = viewMap.get(xmlName);
    if (viewBeans == null)
      return customViews; 
    for (ViewBean viewBean : viewBeans) {
      if (viewBean.type == 9 || viewBean.type == 10 || viewBean.type == 25 || viewBean.type == 48 || viewBean.type == 31) {
        String customViewName = viewBean.customView;
        if (customViewName != null && customViewName.length() > 0 && !viewBean.customView.equals("none"))
          customViews.add(viewBean); 
      } 
    } 
    return customViews;
  }
  
  public void loadLogicFromBackup() {
    String logicPath = SketchwarePaths.getBackupPath(projectId) + File.separator + "logic";
    if (!hasUsableBackup(logicPath))
      return; 
    loadLogicFromPath(logicPath, "logic backup");
  }
  
  public boolean hasComponentOfType(String fileName, int componentType) {
    ArrayList<ComponentBean> components = componentMap.get(fileName);
    if (components == null)
      return false; 
    for (ComponentBean component : components) {
      if (component.type == componentType)
        return true; 
    } 
    return false;
  }
  
  public boolean hasVariable(String fileName, int variableType, String variableName) {
    ArrayList<Pair<Integer, String>> varEntries = variableMap.get(fileName);
    if (varEntries == null)
      return false; 
    for (Pair<Integer, String> varEntry : varEntries) {
      if (varEntry.first == variableType && varEntry.second.equals(variableName))
        return true; 
    } 
    return false;
  }
  
  public boolean isMoreBlockUsed(String fileName, String moreBlockName) {
    HashMap<String, ArrayList<BlockBean>> blockEntryMap = blockMap.get(fileName);
    if (blockEntryMap == null)
      return false; 
    String moreBlockKey = moreBlockName + "_moreBlock";
    for (Map.Entry<String, ArrayList<BlockBean>> entry : blockEntryMap.entrySet()) {
      if (entry.getKey().equals(moreBlockKey))
        continue; 
      for (BlockBean blockBean : entry.getValue()) {
        if (blockBean.opCode.equals("definedFunc")) {
          int spaceIdx = blockBean.spec.indexOf(" ");
          String specStr = blockBean.spec;
          String funcName = specStr;
          if (spaceIdx > 0)
            funcName = specStr.substring(0, spaceIdx); 
          if (funcName.equals(moreBlockName))
            return true; 
        } 
      } 
    } 
    return false;
  }
  
  public ArrayList<EventBean> getEvents(String fileName) {
    return !eventMap.containsKey(fileName) ? new ArrayList<>() : eventMap.get(fileName);
  }
  
  public void loadViewFromData() {
    String viewPath = SketchwarePaths.getDataPath(projectId) + File.separator + "view";
    if (!fileUtil.exists(viewPath))
      return;
    loadViewFromPath(viewPath, "view data");
  }
  
  public void removeComponentsByType(String fileName, int componentType) {
    if (!componentMap.containsKey(fileName))
      return; 
    ArrayList<ComponentBean> components = getComponentsByType(fileName, componentType);
    if (components != null && !components.isEmpty()) {
      for (ComponentBean component : components)
        removeComponent(fileName, component); 
    } 
  }
  
  public boolean hasViewOfType(String fileName, int viewType, String viewId) {
    ArrayList<ViewBean> views = viewMap.get(fileName);
    if (views == null)
      return false; 
    for (ViewBean viewBean : views) {
      if (viewBean.type == viewType && viewBean.id.equals(viewId))
        return true; 
    } 
    return false;
  }
  
  public boolean hasTextView(String fileName, String viewId) {
    ArrayList<ViewBean> views = viewMap.get(fileName);
    if (views == null)
      return false; 
    for (ViewBean viewBean : views) {
      if (viewBean.getClassInfo().matchesType("TextView") && viewBean.id.equals(viewId))
        return true; 
    } 
    return false;
  }
  
  public ViewBean getFabView(String fileName) {
    if (!fabMap.containsKey(fileName))
      initFab(fileName); 
    return fabMap.get(fileName);
  }
  
  public void loadViewFromBackup() {
    String viewPath = SketchwarePaths.getBackupPath(projectId) + File.separator + "view";
    if (!hasUsableBackup(viewPath))
      return; 
    loadViewFromPath(viewPath, "view backup");
  }
  
  public boolean hasView(String fileName, String viewId) {
    ArrayList<ViewBean> views = viewMap.get(fileName);
    if (views == null)
      return false; 
    for (ViewBean viewBean : views) {
      if (viewBean.id.equals(viewId))
        return true; 
    } 
    return false;
  }
  
  public ArrayList<Pair<String, String>> getMoreBlocks(String fileName) {
    return moreBlockMap.containsKey(fileName) ? moreBlockMap.get(fileName) : new ArrayList<>();
  }
  
  public void resetProject() {
    projectId = "";
    clearAllData();
  }
  
  @SuppressWarnings("unchecked")
  private void parseLogicSection(String sectionKey, String sectionContent,
      HashMap<String, ArrayList<Pair<Integer, String>>> targetVariableMap,
      HashMap<String, ArrayList<Pair<Integer, String>>> targetListMap,
      HashMap<String, ArrayList<Pair<String, String>>> targetMoreBlockMap,
      HashMap<String, ArrayList<ComponentBean>> targetComponentMap,
      HashMap<String, ArrayList<EventBean>> targetEventMap,
      HashMap<String, HashMap<String, ArrayList<BlockBean>>> targetBlockMap) {
    if (sectionContent.length() <= 0)
      return; 
    try {
      ProjectDataParser parser = new ProjectDataParser(sectionKey);
      String parsedFileName = parser.getFileName();
      ProjectDataParser.DataType dataType = parser.getDataType();
      switch (dataType) {
        case VARIABLE:
          targetVariableMap.put(parsedFileName, (ArrayList<Pair<Integer, String>>)parser.parseData(sectionContent));
          break;
        case LIST:
          targetListMap.put(parsedFileName, (ArrayList<Pair<Integer, String>>)parser.parseData(sectionContent));
          break;
        case COMPONENT:
          targetComponentMap.put(parsedFileName, (ArrayList<ComponentBean>)parser.parseData(sectionContent));
          break;
        case EVENT:
          targetEventMap.put(parsedFileName, (ArrayList<EventBean>)parser.parseData(sectionContent));
          break;
        case MORE_BLOCK:
          targetMoreBlockMap.put(parsedFileName, (ArrayList<Pair<String, String>>)parser.parseData(sectionContent));
          break;
        case EVENT_BLOCK:
          if (!targetBlockMap.containsKey(parsedFileName)) {
            targetBlockMap.put(parsedFileName, new HashMap<String, ArrayList<BlockBean>>());
          }
          targetBlockMap.get(parsedFileName).put(parser.getBlockKey(), parser.parseData(sectionContent));
          break;
        default:
          return;
      }
    } catch (RuntimeException e) {
      Log.e("ProjectDataStore", "Failed to parse logic section '" + sectionKey + "' for project " + projectId, e);
    } 
  }

  public void parseLogicSection(String sectionKey, String sectionContent) {
    parseLogicSection(sectionKey, sectionContent, variableMap, listMap, moreBlockMap, componentMap,
        eventMap, blockMap);
  }
  
  public ArrayList<Pair<Integer, String>> getListVariables(String fileName) {
    return listMap.containsKey(fileName) ? listMap.get(fileName) : new ArrayList<>();
  }
  
  public boolean saveAllData() {
    String basePath = SketchwarePaths.getDataPath(projectId);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    CompletableFuture<Boolean> viewFuture = CompletableFuture.supplyAsync(
        () -> saveViewFile(basePath + File.separator + "view"), pool);
    CompletableFuture<Boolean> logicFuture = CompletableFuture.supplyAsync(
        () -> saveLogicFile(basePath + File.separator + "logic"), pool);
    boolean viewOk = viewFuture.join();
    boolean logicOk = logicFuture.join();
    pool.shutdown();
    if (viewOk && logicOk) {
      deleteBackupFiles();
    }
    return viewOk && logicOk;
  }
  
  @SuppressWarnings("unchecked")
  private void parseViewSection(String sectionKey, String sectionContent,
      HashMap<String, ArrayList<ViewBean>> targetViewMap,
      HashMap<String, ViewBean> targetFabMap) {
    try {
      ProjectDataParser parser = new ProjectDataParser(sectionKey);
      String parsedFileName = parser.getFileName();
      ProjectDataParser.DataType dataType = parser.getDataType();
      if (dataType == ProjectDataParser.DataType.VIEW) {
        targetViewMap.put(parsedFileName, parser.parseData(sectionContent));
      } else if (dataType == ProjectDataParser.DataType.FAB) {
        targetFabMap.put(parsedFileName, parser.parseData(sectionContent));
      }
    } catch (RuntimeException e) {
      Log.e("ProjectDataStore", "Failed to parse view section '" + sectionKey + "' for project " + projectId, e);
    } 
  }

  public void parseViewSection(String sectionKey, String sectionContent) {
    parseViewSection(sectionKey, sectionContent, viewMap, fabMap);
  }
  
  public ArrayList<Pair<Integer, String>> getVariables(String fileName) {
    return variableMap.containsKey(fileName) ? variableMap.get(fileName) : new ArrayList<>();
  }
  
  public boolean saveAllBackup() {
    String basePath = SketchwarePaths.getBackupPath(projectId);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    CompletableFuture<Boolean> viewFuture = CompletableFuture.supplyAsync(
        () -> saveViewFile(basePath + File.separator + "view"), pool);
    CompletableFuture<Boolean> logicFuture = CompletableFuture.supplyAsync(
        () -> saveLogicFile(basePath + File.separator + "logic"), pool);
    boolean viewOk = viewFuture.join();
    boolean logicOk = logicFuture.join();
    pool.shutdown();
    return viewOk && logicOk;
  }
  
  public void removeBlockEntry(String fileName, String blockKey) {
    if (!blockMap.containsKey(fileName))
      return; 
    HashMap<String, ArrayList<BlockBean>> blockEntryMap = blockMap.get(fileName);
    if (blockEntryMap == null)
      return; 
    blockEntryMap.remove(blockKey);
  }
  
  public void removeViewTypeEvents(String fileName) {
    if (!eventMap.containsKey(fileName))
      return; 
    ArrayList<EventBean> events = eventMap.get(fileName);
    events.removeIf(event -> event.eventType == 4);
  }
  
  public void removeViewEventsByTarget(String fileName, String targetId) {
    if (!eventMap.containsKey(fileName))
      return; 
    ArrayList<EventBean> events = eventMap.get(fileName);
    events.removeIf(event -> event.eventType == 4 && event.targetId.equals(targetId));
  }
  
  public final boolean saveLogicFile(String filePath) {
    StringBuilder contentBuffer = new StringBuilder();
    serializeLogicData(contentBuffer);
    try {
      boolean saved = fileUtil.writeBytes(filePath, encryptStringForStorage(contentBuffer.toString(), filePath));
      if (!saved)
        Log.e("ProjectDataStore", "Failed to write logic file for project " + projectId + " to " + filePath);
      return saved;
    } catch (IOException | RuntimeException e) {
      Log.e("ProjectDataStore", "Failed to save logic file for project " + projectId + " to " + filePath, e);
      return false;
    } 
  }
  
  public void removeEventsByTarget(String fileName, String targetId) {
    if (!eventMap.containsKey(fileName))
      return; 
    ArrayList<EventBean> events = eventMap.get(fileName);
    if (events == null)
      return; 
    HashMap<String, ArrayList<BlockBean>> fileBlocks = blockMap != null ? blockMap.get(fileName) : null;
    for (int i = events.size() - 1; i >= 0; i--) {
      EventBean eventBean = events.get(i);
      if (eventBean.targetId.equals(targetId)) {
        events.remove(i);
        if (fileBlocks != null) {
          fileBlocks.remove(eventBean.targetId + "_" + eventBean.eventName);
        }
      }
    }
  }
  
  public final boolean saveViewFile(String filePath) {
    StringBuilder contentBuffer = new StringBuilder();
    serializeViewData(contentBuffer);
    try {
      boolean saved = fileUtil.writeBytes(filePath, encryptStringForStorage(contentBuffer.toString(), filePath));
      if (!saved)
        Log.e("ProjectDataStore", "Failed to write view file for project " + projectId + " to " + filePath);
      return saved;
    } catch (IOException | RuntimeException e) {
      Log.e("ProjectDataStore", "Failed to save view file for project " + projectId + " to " + filePath, e);
      return false;
    } 
  }
  
  public void removeMoreBlock(String fileName, String moreBlockName) {
    if (!moreBlockMap.containsKey(fileName))
      return; 
    ArrayList<Pair<String, String>> moreBlocks = moreBlockMap.get(fileName);
    if (moreBlocks == null)
      return; 
    for (Pair<String, String> moreBlockEntry : moreBlocks) {
      if (moreBlockEntry.first.equals(moreBlockName)) {
        moreBlocks.remove(moreBlockEntry);
        break;
      } 
    } 
    String blockKey = moreBlockName + "_moreBlock";
    HashMap<String, ArrayList<BlockBean>> blockEntryMap = blockMap.get(fileName);
    if (blockEntryMap != null) {
      blockEntryMap.remove(blockKey);
    } 
  }
  
  public void removeListVariable(String fileName, String listName) {
    if (!listMap.containsKey(fileName))
      return; 
    ArrayList<Pair<Integer, String>> listVars = listMap.get(fileName);
    if (listVars == null)
      return; 
    for (Pair<Integer, String> listEntry : listVars) {
      if (listEntry.second.equals(listName)) {
        listVars.remove(listEntry);
        break;
      } 
    } 
  }
  
  public void removeVariable(String fileName, String variableName) {
    if (!variableMap.containsKey(fileName))
      return; 
    ArrayList<Pair<Integer, String>> varEntries = variableMap.get(fileName);
    if (varEntries == null)
      return; 
    for (Pair<Integer, String> varEntry : varEntries) {
      if (varEntry.second.equals(variableName)) {
        varEntries.remove(varEntry);
        break;
      } 
    } 
  }
  
  public void renameVariable(String fileName, String oldName, String newName) {
    ArrayList<Pair<Integer, String>> vars = variableMap.get(fileName);
    if (vars != null) {
      for (int i = 0; i < vars.size(); i++) {
        if (vars.get(i).second.equals(oldName)) {
          vars.set(i, new Pair<>(vars.get(i).first, newName));
          break;
        }
      }
    }
    renameVariableInBlocks(fileName, oldName, newName, true);
  }
  
  public void renameListVariable(String fileName, String oldName, String newName) {
    ArrayList<Pair<Integer, String>> lists = listMap.get(fileName);
    if (lists != null) {
      for (int i = 0; i < lists.size(); i++) {
        if (lists.get(i).second.equals(oldName)) {
          lists.set(i, new Pair<>(lists.get(i).first, newName));
          break;
        }
      }
    }
    renameVariableInBlocks(fileName, oldName, newName, false);
  }
  
  private static final Map<String, Integer> VAR_REF_PARAM_INDEX = new HashMap<>();
  private static final Map<String, Integer> LIST_REF_PARAM_INDEX = new HashMap<>();
  static {
    VAR_REF_PARAM_INDEX.put("getVar", -1);
    for (String op : new String[]{"setVarBoolean", "setVarInt", "setVarString",
        "increaseInt", "decreaseInt", "mapCreateNew", "mapPut", "mapGet", "mapContainKey",
        "mapRemoveKey", "mapSize", "mapClear", "mapIsEmpty", "mapGetAllKeys",
        "addListMap", "insertListMap", "mapToStr"})
      VAR_REF_PARAM_INDEX.put(op, 0);
    VAR_REF_PARAM_INDEX.put("strToMap", 1);
    VAR_REF_PARAM_INDEX.put("getAtListMap", 2);

    LIST_REF_PARAM_INDEX.put("getVar", -1);
    for (String op : new String[]{"lengthList", "containListInt", "containListStr",
        "containListMap", "clearList", "listMapToStr"})
      LIST_REF_PARAM_INDEX.put(op, 0);
    for (String op : new String[]{"addListInt", "addListStr", "getAtListInt", "getAtListStr",
        "indexListInt", "indexListStr", "deleteList", "spnSetData", "listSetData",
        "listSetCustomViewData", "strToListMap"})
      LIST_REF_PARAM_INDEX.put(op, 1);
    for (String op : new String[]{"insertListInt", "insertListStr", "addListMap", "getAtListMap"})
      LIST_REF_PARAM_INDEX.put(op, 2);
    for (String op : new String[]{"insertListMap", "setListMap"})
      LIST_REF_PARAM_INDEX.put(op, 3);
  }

  private void renameVariableInBlocks(String fileName, String oldName, String newName, boolean isVariable) {
    HashMap<String, ArrayList<BlockBean>> blockEntryMap = blockMap.get(fileName);
    if (blockEntryMap == null) return;
    Map<String, Integer> refIndex = isVariable ? VAR_REF_PARAM_INDEX : LIST_REF_PARAM_INDEX;
    for (Map.Entry<String, ArrayList<BlockBean>> entry : blockEntryMap.entrySet()) {
      for (BlockBean blockBean : entry.getValue()) {
        // ClassInfo-based detection (catches getVar blocks and typed parameters)
        ClassInfo blockClassInfo = blockBean.getClassInfo();
        if (blockClassInfo != null
            && (isVariable ? blockClassInfo.isVariable() : blockClassInfo.isList())
            && blockBean.spec.equals(oldName)) {
          blockBean.spec = newName;
        }
        ArrayList<ClassInfo> paramClassInfos = blockBean.getParamClassInfo();
        if (paramClassInfos != null && !paramClassInfos.isEmpty()) {
          for (int b = 0; b < paramClassInfos.size(); b++) {
            ClassInfo paramClassInfo = paramClassInfos.get(b);
            if (paramClassInfo != null
                && (isVariable ? paramClassInfo.isVariable() : paramClassInfo.isList())
                && blockBean.parameters.get(b).equals(oldName)) {
              blockBean.parameters.set(b, newName);
            }
          }
        }
        // opCode-based detection (catches %m.varInt style selector parameters
        // that ClassInfo misses because their ClassInfo resolves to "Component")
        Integer paramIdx = refIndex.get(blockBean.opCode);
        if (paramIdx != null) {
          if (paramIdx == -1) {
            if (blockBean.spec.equals(oldName)) blockBean.spec = newName;
          } else if (paramIdx < blockBean.parameters.size()
              && blockBean.parameters.get(paramIdx).equals(oldName)) {
            blockBean.parameters.set(paramIdx, newName);
          }
        }
      }
    }
  }
  
  public boolean hasViewType(String fileName, int viewType) {
    ArrayList<ViewBean> views = viewMap.get(fileName);
    if (views == null)
      return false;
    for (ViewBean viewBean : views) {
      if (viewBean.type == viewType)
        return true; 
    } 
    return false;
  }
  
  public boolean hasViewMatchingType(String fileName, String viewTypeName) {
    ArrayList<ViewBean> views = viewMap.get(fileName);
    if (views == null)
      return false;
    for (ViewBean viewBean : views) {
      if (viewBean.getClassInfo().matchesType(viewTypeName))
        return true; 
    } 
    return false;
  }
  
  public final String getSimpleClassName(String className) {
    String simpleName = className;
    if (simpleName.contains(".")) {
      String[] parts = simpleName.split("\\.");
      simpleName = parts[parts.length - 1];
    } 
    return simpleName;
  }
}
