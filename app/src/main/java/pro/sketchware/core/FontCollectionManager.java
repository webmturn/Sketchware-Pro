package pro.sketchware.core;

import com.besome.sketch.beans.CollectionBean;
import com.besome.sketch.beans.ProjectResourceBean;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class FontCollectionManager extends BaseCollectionManager {
    private static volatile FontCollectionManager instance;

    public static FontCollectionManager getInstance() {
        if (instance == null) {
            synchronized (FontCollectionManager.class) {
                if (instance == null) {
                    instance = new FontCollectionManager();
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
        String dataName = resourceBean.resName;
        if (resourceBean.resFullName.contains(".")) {
            String ext = resourceBean.resFullName.substring(resourceBean.resFullName.lastIndexOf('.'));
            dataName = dataName + ext;
        }
        String destPath = dataDirPath + File.separator + dataName;
        String srcPath;
        if (resourceBean.savedPos == 1) {
            srcPath = resourceBean.resFullName;
        } else {
            srcPath = SketchwarePaths.getFontsResourcePath() + File.separator + sourcePath + File.separator + resourceBean.resFullName;
        }
        if (!fileUtil.exists(srcPath)) {
            throw new CompileException("file_no_exist");
        }
        try {
            fileUtil.mkdirs(dataDirPath);
            fileUtil.copyFile(srcPath, destPath);
        } catch (IOException e) {
            throw new CompileException("fail_to_copy");
        }
        collections.add(new CollectionBean(resourceBean.resName, dataName));
        if (save) saveCollections();
    }

    public void removeResource(String name, boolean save) {
        Iterator<CollectionBean> it = collections.iterator();
        while (it.hasNext()) {
            CollectionBean bean = it.next();
            if (bean.name.equals(name)) {
                it.remove();
                fileUtil.deleteFileByPath(dataDirPath + File.separator + bean.data);
                break;
            }
        }
        if (save) saveCollections();
    }

    public void initializePaths() {
        String basePath = SketchwarePaths.getCollectionPath() + File.separator + "font" + File.separator;
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
