package pro.sketchware.core.project;

import pro.sketchware.core.settings.ApplicationSettings;

import android.util.Log;

import pro.sketchware.beans.ProjectLibraryBean;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import pro.sketchware.util.io.EncryptedFileUtil;

public class LibraryManager {
  private final ProjectLoadState loadState = new ProjectLoadState();

  public boolean canSave() {
    return loadState.canSave();
  }

  public IOException getLoadFailure() {
    return loadState.getFailure();
  }

  public String projectId;
  
  public EncryptedFileUtil fileUtil;
  
  public ProjectLibraryBean firebaseDB;
  
  public ProjectLibraryBean compat;
  
  public ProjectLibraryBean admob;
  
  public ProjectLibraryBean googleMap;
  
  public Gson gson;
  
  public LibraryManager(String projectId) {
    this.projectId = projectId;
    fileUtil = new EncryptedFileUtil();
    gson = (new GsonBuilder()).excludeFieldsWithoutExposeAnnotation().create();
    initializeDefaults();
  }
  
  public void deleteBackup() {
    String backupPath = SketchwarePaths.getBackupPath(projectId) + File.separator + "library";
    fileUtil.deleteFileByPath(backupPath);
  }

  private String decryptFileToString(String filePath) throws IOException {
    byte[] fileBytes = fileUtil.readFileBytes(filePath);
    if (fileBytes == null && new File(filePath).length() > 0L)
      throw new IOException("Failed to read library metadata file: " + filePath);
    return fileUtil.decryptToString(fileBytes);
  }

  private byte[] encryptStringForStorage(String content, String filePath) throws IOException {
    try {
      return fileUtil.encryptString(content, ApplicationSettings.isSettingEnabled(
              ApplicationSettings.SETTING_PROJECT_DATA_ENCRYPTION));
    } catch (GeneralSecurityException e) {
      throw new IOException("Failed to encrypt library metadata file: " + filePath, e);
    }
  }

  private void loadFromPath(String filePath, String sourceName) {
    try {
      loadState.read(() -> {
        try (BufferedReader bufferedReader = new BufferedReader(new StringReader(decryptFileToString(filePath)))) {
          parseLibraryData(bufferedReader);
        }
      });
    } catch (IOException | RuntimeException e) {
      loadState.failed(e);
      Log.w("LibraryManager", "Failed to load " + sourceName + " for project " + projectId + " from " + filePath, e);
    }
  }

  private static final class LibraryState {
    private ProjectLibraryBean firebaseDB = new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_FIREBASE);
    private ProjectLibraryBean compat = new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_COMPAT);
    private ProjectLibraryBean admob = new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_ADMOB);
    private ProjectLibraryBean googleMap = new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_GOOGLE_MAP);
  }
  
  public void setAdmob(ProjectLibraryBean libraryBean) {
    admob = libraryBean;
  }
  
  public void parseLibraryData(BufferedReader reader) throws IOException {
    loadState.read(() -> {
      LibraryState parsedState = new LibraryState();
      StringBuilder contentBuffer = new StringBuilder();
      String sectionName = "";
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.length() <= 0)
          continue;
        if (line.charAt(0) == '@') {
          if (sectionName.length() > 0) {
            parseLibrarySection(sectionName, contentBuffer.toString(), parsedState);
            contentBuffer = new StringBuilder();
          }
          sectionName = line.substring(1);
          if (sectionName.trim().isEmpty()) throw new IOException("Empty project section header");
          continue;
        }
        if (sectionName.isEmpty() && !line.matches("[ \\t]*"))
          throw new IOException("Project data before a section header");
        contentBuffer.append(line);
        contentBuffer.append("\n");
      }
      if (sectionName.length() > 0)
        parseLibrarySection(sectionName, contentBuffer.toString(), parsedState);
      firebaseDB = parsedState.firebaseDB;
      compat = parsedState.compat;
      admob = parsedState.admob;
      googleMap = parsedState.googleMap;
    });
  }
  
  public final boolean writeToFile(String filePath) {
    if (!canSave() || !ProjectDataManager.canSaveLoadedProject(projectId)) return false;
    StringBuilder contentBuffer = new StringBuilder();
    serializeLibraries(contentBuffer);
    try {
      boolean saved = fileUtil.writeBytes(filePath, encryptStringForStorage(contentBuffer.toString(), filePath));
      if (!saved)
        Log.w("LibraryManager", "Failed to write library metadata for project " + projectId + " to " + filePath);
      return saved;
    } catch (IOException | RuntimeException e) {
      Log.w("LibraryManager", "Failed to write library metadata for project " + projectId + " to " + filePath, e);
    } 
    return false;
  }
  
  public void parseLibrarySection(String sectionName, String sectionContent) {
    LibraryState currentState = new LibraryState();
    currentState.firebaseDB = firebaseDB;
    currentState.compat = compat;
    currentState.admob = admob;
    currentState.googleMap = googleMap;
    try {
      parseLibrarySection(sectionName, sectionContent, currentState);
    } catch (IOException e) {
      loadState.failed(e);
      Log.w("LibraryManager", "Invalid section: " + sectionName, e);
      return;
    }
    firebaseDB = currentState.firebaseDB;
    compat = currentState.compat;
    admob = currentState.admob;
    googleMap = currentState.googleMap;
  }

  private void parseLibrarySection(String sectionName, String sectionContent, LibraryState targetState) throws IOException {
    if (!sectionName.equals("firebaseDB") && !sectionName.equals("compat")
        && !sectionName.equals("admob") && !sectionName.equals("googleMap"))
      throw new IOException("Unknown library section: " + sectionName);
    if (sectionContent.length() <= 0)
      return; 
    try {
      ProjectLibraryBean parsedLibrary = gson.fromJson(sectionContent, ProjectLibraryBean.class);
      if (parsedLibrary == null) throw new IOException("Null library entry");
      if (sectionName.equals("firebaseDB")) {
        parsedLibrary.libType = ProjectLibraryBean.PROJECT_LIB_TYPE_FIREBASE;
        targetState.firebaseDB = parsedLibrary;
      } else if (sectionName.equals("compat")) {
        parsedLibrary.libType = ProjectLibraryBean.PROJECT_LIB_TYPE_COMPAT;
        targetState.compat = parsedLibrary;
      } else if (sectionName.equals("admob")) {
        parsedLibrary.libType = ProjectLibraryBean.PROJECT_LIB_TYPE_ADMOB;
        targetState.admob = parsedLibrary;
      } else if (sectionName.equals("googleMap")) {
        parsedLibrary.libType = ProjectLibraryBean.PROJECT_LIB_TYPE_GOOGLE_MAP;
        targetState.googleMap = parsedLibrary;
      }
    } catch (RuntimeException e) {
      throw new IOException("Invalid library section '" + sectionName + "'", e);
    }
  }
  
  public final void serializeLibraries(StringBuilder buffer) {
    if (firebaseDB != null) {
      buffer.append("@firebaseDB\n");
      buffer.append(gson.toJson(firebaseDB, ProjectLibraryBean.class));
      buffer.append("\n");
    } 
    if (compat != null) {
      buffer.append("@compat\n");
      buffer.append(gson.toJson(compat, ProjectLibraryBean.class));
      buffer.append("\n");
    } 
    if (admob != null) {
      buffer.append("@admob\n");
      buffer.append(gson.toJson(admob, ProjectLibraryBean.class));
      buffer.append("\n");
    } 
    if (googleMap != null) {
      buffer.append("@googleMap\n");
      buffer.append(gson.toJson(googleMap, ProjectLibraryBean.class));
      buffer.append("\n");
    } 
  }
  
  public ProjectLibraryBean getAdmob() {
    return admob;
  }
  
  public void setCompat(ProjectLibraryBean libraryBean) {
    compat = libraryBean;
  }
  
  public ProjectLibraryBean getCompat() {
    return compat;
  }
  
  public void setFirebaseDB(ProjectLibraryBean libraryBean) {
    firebaseDB = libraryBean;
  }
  
  public ProjectLibraryBean getFirebaseDB() {
    return firebaseDB;
  }
  
  public void setGoogleMap(ProjectLibraryBean libraryBean) {
    googleMap = libraryBean;
  }
  
  public ProjectLibraryBean getGoogleMap() {
    return googleMap;
  }
  
  public final void initializeDefaults() {
    firebaseDB = new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_FIREBASE);
    compat = new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_COMPAT);
    admob = new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_ADMOB);
    googleMap = new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_GOOGLE_MAP);
  }
  
  public boolean hasBackup() {
    return hasDistinctBackup(
        SketchwarePaths.getBackupPath(projectId) + File.separator + "library",
        SketchwarePaths.getDataPath(projectId) + File.separator + "library");
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
  
  public void loadFromBackup() {
    String basePath = SketchwarePaths.getBackupPath(projectId) + File.separator + "library";
    if (!hasUsableBackup(basePath))
      return; 
    loadFromPath(basePath, "library backup");
  }
  
  public void loadFromData() {
    String dataPath = SketchwarePaths.getDataPath(projectId) + File.separator + "library";
    if (!fileUtil.exists(dataPath))
      return; 
    loadFromPath(dataPath, "library data");
  }
  
  public void resetAll() {
    projectId = "";
    initializeDefaults();
  }
  
  public boolean saveToBackup() {
    return writeToFile(SketchwarePaths.getBackupPath(projectId) + File.separator + "library");
  }
  
  public boolean saveToData() {
    boolean saved = writeToFile(SketchwarePaths.getDataPath(projectId) + File.separator + "library");
    if (saved)
      deleteBackup();
    return saved;
  }
}
