package pro.sketchware.core;

import com.besome.sketch.beans.CollectionBean;
import com.besome.sketch.beans.ProjectResourceBean;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

public class ImageCollectionManager extends BaseCollectionManager {
    private static volatile ImageCollectionManager instance;

    public static ImageCollectionManager getInstance() {
        if (instance == null) {
            synchronized (ImageCollectionManager.class) {
                if (instance == null) {
                    instance = new ImageCollectionManager();
                }
            }
        }
        return instance;
    }

    public ProjectResourceBean getResourceByName(String name) {
        for (ProjectResourceBean resource : getResources()) {
            if (resource.resName.equals(name)) return resource;
        }
        return null;
    }

    public void renameResource(ProjectResourceBean resourceBean, String newName, boolean save) {
        for (int i = collections.size() - 1; i >= 0; i--) {
            CollectionBean bean = collections.get(i);
            if (bean.name.equals(resourceBean.resName)) {
                bean.name = newName;
                break;
            }
        }
        if (save) saveCollections();
    }

    public void addResource(String sourcePath, ProjectResourceBean resourceBean) throws CompileException {
        addResource(sourcePath, resourceBean, true);
    }

    public void addResource(String sourcePath, ProjectResourceBean resourceBean, boolean save) throws CompileException {
        if (collections == null) initialize();
        for (CollectionBean bean : collections) {
            if (bean.name.equals(resourceBean.resName)) {
                throw new CompileException("duplicate_name");
            }
        }
        String dataName = resourceBean.resName + (resourceBean.isNinePatch() ? ".9.png" : ".png");
        String destPath = dataDirPath + File.separator + dataName;
        if (resourceBean.savedPos == 1) {
            if (!fileUtil.exists(resourceBean.resFullName)) {
                throw new CompileException("file_no_exist");
            }
            try {
                fileUtil.mkdirs(dataDirPath);
                BitmapUtil.processAndSaveBitmap(resourceBean.resFullName, destPath, resourceBean.rotate, resourceBean.flipHorizontal, resourceBean.flipVertical);
            } catch (Exception e) {
                throw new CompileException("fail_to_copy");
            }
        } else {
            String srcPath = SketchwarePaths.getImagesPath() + File.separator + sourcePath + File.separator + resourceBean.resFullName;
            if (!fileUtil.exists(srcPath)) {
                throw new CompileException("file_no_exist");
            }
            try {
                fileUtil.mkdirs(dataDirPath);
                fileUtil.copyFile(srcPath, destPath);
            } catch (Exception e) {
                throw new CompileException("fail_to_copy");
            }
        }
        collections.add(new CollectionBean(resourceBean.resName, dataName));
        if (save) saveCollections();
    }

    public void addResources(String category, ArrayList<ProjectResourceBean> resources, boolean save) throws CompileException {
        if (collections == null) initialize();

        ArrayList<String> duplicateNames = new ArrayList<>();
        for (CollectionBean bean : collections) {
            for (ProjectResourceBean resource : resources) {
                if (bean.name.equals(resource.resName)) {
                    duplicateNames.add(bean.name);
                }
            }
        }
        if (!duplicateNames.isEmpty()) {
            CompileException ex = new CompileException("duplicate_name");
            ex.setErrorDetails(duplicateNames);
            throw ex;
        }

        ArrayList<String> missingNames = new ArrayList<>();
        for (ProjectResourceBean resource : resources) {
            String srcPath = resolveSourcePath(category, resource);
            if (!fileUtil.exists(srcPath)) {
                missingNames.add(resource.resName);
            }
        }
        if (!missingNames.isEmpty()) {
            CompileException ex = new CompileException("file_no_exist");
            ex.setErrorDetails(missingNames);
            throw ex;
        }

        ArrayList<String> failedNames = new ArrayList<>();
        ArrayList<String> processedPaths = new ArrayList<>();
        for (ProjectResourceBean resource : resources) {
            String fileName = resource.resName + (resource.isNinePatch() ? ".9.png" : ".png");
            String srcPath = resolveSourcePath(category, resource);
            String destPath = dataDirPath + File.separator + fileName;
            try {
                fileUtil.mkdirs(dataDirPath);
                BitmapUtil.processAndSaveBitmap(srcPath, destPath, resource.rotate, resource.flipHorizontal, resource.flipVertical);
                collections.add(new CollectionBean(resource.resName, fileName));
                processedPaths.add(destPath);
            } catch (Exception e) {
                failedNames.add(resource.resName);
            }
        }
        if (!failedNames.isEmpty()) {
            for (String path : processedPaths) {
                fileUtil.deleteFileByPath(path);
            }
            CompileException ex = new CompileException("fail_to_copy");
            ex.setErrorDetails(failedNames);
            throw ex;
        }
        if (save) saveCollections();
    }

    private String resolveSourcePath(String category, ProjectResourceBean resource) {
        if (resource.savedPos == 0) {
            return SketchwarePaths.getImagesPath() + File.separator + category + File.separator + resource.resFullName;
        }
        return resource.resFullName;
    }

    public void removeResource(String name, boolean save) {
        Iterator<CollectionBean> it = collections.iterator();
        while (it.hasNext()) {
            CollectionBean bean = it.next();
            if (bean.name.equals(name)) {
                fileUtil.deleteFileByPath(dataDirPath + File.separator + bean.data);
                it.remove();
            }
        }
        if (save) saveCollections();
    }

    public void initializePaths() {
        String basePath = SketchwarePaths.getCollectionPath() + File.separator + "image" + File.separator;
        collectionFilePath = basePath + "list";
        dataDirPath = basePath + "data";
    }

    public boolean hasResource(String name) {
        for (ProjectResourceBean resource : getResources()) {
            if (resource.resName.equals(name)) return true;
        }
        return false;
    }

    public void clearCollections() {
        super.clearCollections();
        instance = null;
    }

    public ArrayList<ProjectResourceBean> getResources() {
        if (collections == null) initialize();
        ArrayList<ProjectResourceBean> resources = new ArrayList<>();
        for (CollectionBean bean : collections) {
            resources.add(new ProjectResourceBean(ProjectResourceBean.PROJECT_RES_TYPE_FILE, bean.name, bean.data));
        }
        return resources;
    }
}
