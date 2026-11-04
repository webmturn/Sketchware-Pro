package pro.sketchware.core.project;

import static org.junit.Assert.*;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import pro.sketchware.beans.ComponentBean;
import pro.sketchware.beans.ProjectFileBean;
import pro.sketchware.beans.ProjectLibraryBean;
import pro.sketchware.beans.ViewBean;

/** Exercises only private in-memory stores; never loads or saves a project. */
@RunWith(AndroidJUnit4.class)
public class LibraryDisableCleanupTest {
    @Test public void disablingFirebaseRemovesEveryFirebaseTypeAndItsEvents() {
        Fixture fixture = new Fixture();
        for (ProjectFileBean file : fixture.files.getActivities()) {
            for (int type : new int[]{ComponentBean.COMPONENT_TYPE_FIREBASE,
                    ComponentBean.COMPONENT_TYPE_FIREBASE_AUTH, ComponentBean.COMPONENT_TYPE_FIREBASE_STORAGE,
                    ComponentBean.COMPONENT_TYPE_FIREBASE_AUTH_PHONE, ComponentBean.COMPONENT_TYPE_FIREBASE_CLOUD_MESSAGE,
                    ComponentBean.COMPONENT_TYPE_FIREBASE_AUTH_GOOGLE_LOGIN}) {
                fixture.component(file, type, "firebase_" + type);
            }
            fixture.component(file, ComponentBean.COMPONENT_TYPE_INTENT, "keep_intent");
            fixture.component(file, ComponentBean.COMPONENT_TYPE_REWARDED_VIDEO_AD, "keep_rewarded");
        }

        fixture.store.removeFirebaseComponents(new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_FIREBASE));

        for (ProjectFileBean file : fixture.files.getActivities()) {
            assertEquals(List.of("keep_intent", "keep_rewarded"), fixture.componentIds(file));
            assertEquals(List.of("keep_intent", "keep_rewarded"), fixture.eventTargets(file));
            assertEquals(2, fixture.store.blockMap.get(file.getJavaName()).size());
            assertFalse(fixture.store.blockMap.get(file.getJavaName()).keySet().stream().anyMatch(key -> key.startsWith("firebase_")));
        }
    }

    @Test public void disablingAdmobRemovesBothAdComponentsAndBannerViews() {
        Fixture fixture = new Fixture();
        for (ProjectFileBean file : fixture.files.getActivities()) {
            fixture.component(file, ComponentBean.COMPONENT_TYPE_INTERSTITIAL_AD, "interstitial");
            fixture.component(file, ComponentBean.COMPONENT_TYPE_REWARDED_VIDEO_AD, "rewarded");
            fixture.component(file, ComponentBean.COMPONENT_TYPE_FIREBASE_AUTH_PHONE, "keep_phone");
            fixture.banner(file);
        }
        fixture.banner(fixture.files.getCustomViews().get(0));

        fixture.store.removeAdmobViewsAndComponents(new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_ADMOB), fixture.files);

        for (ProjectFileBean file : fixture.files.getActivities()) {
            assertEquals(List.of("keep_phone"), fixture.componentIds(file));
            assertEquals(List.of("keep_phone"), fixture.eventTargets(file));
            assertEquals(List.of("keep_phone_onComplete"), new ArrayList<>(fixture.store.blockMap.get(file.getJavaName()).keySet()));
        }
        for (ArrayList<ViewBean> views : fixture.store.viewMap.values()) {
            assertEquals(1, views.size());
            assertEquals("keep_label", views.get(0).id);
        }
    }

    @Test public void enabledLibrariesKeepComponentsEventsAndViews() {
        Fixture fixture = new Fixture();
        ProjectFileBean file = fixture.files.getActivities().get(0);
        fixture.component(file, ComponentBean.COMPONENT_TYPE_FIREBASE_AUTH_GOOGLE_LOGIN, "google_login");
        fixture.component(file, ComponentBean.COMPONENT_TYPE_REWARDED_VIDEO_AD, "rewarded");
        fixture.banner(file);
        ProjectLibraryBean firebase = new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_FIREBASE);
        firebase.useYn = ProjectLibraryBean.LIB_USE_Y;
        ProjectLibraryBean admob = new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_ADMOB);
        admob.useYn = ProjectLibraryBean.LIB_USE_Y;

        fixture.store.removeFirebaseComponents(firebase);
        fixture.store.removeAdmobViewsAndComponents(admob, fixture.files);

        assertEquals(List.of("google_login", "rewarded"), fixture.componentIds(file));
        assertEquals(List.of("google_login", "rewarded", "banner"), fixture.eventTargets(file));
        assertEquals(3, fixture.store.blockMap.get(file.getJavaName()).size());
        assertEquals(2, fixture.store.getViews(file.getXmlName()).size());
    }

    private static final class Fixture {
        final String id = "__library_cleanup_" + UUID.randomUUID();
        final ProjectDataStore store = new ProjectDataStore(id);
        final ProjectFileManager files = new ProjectFileManager(id);

        Fixture() {
            files.getActivities().add(new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY, "second"));
            files.getCustomViews().add(new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_CUSTOM_VIEW, "row"));
        }

        void component(ProjectFileBean file, int type, String name) {
            store.addComponent(file.getJavaName(), type, name);
            event(file, name);
        }

        void banner(ProjectFileBean file) {
            store.addView(file.getXmlName(), new ViewBean("banner", ViewBean.VIEW_TYPE_WIDGET_ADVIEW));
            store.addView(file.getXmlName(), new ViewBean("keep_label", ViewBean.VIEW_TYPE_WIDGET_TEXTVIEW));
            if (file.fileType == ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY) event(file, "banner");
        }

        void event(ProjectFileBean file, String name) {
            store.addEvent(file.getJavaName(), 2, 0, name, "onComplete");
            store.blockMap.computeIfAbsent(file.getJavaName(), key -> new HashMap<>())
                    .put(name + "_onComplete", new ArrayList<>());
        }

        List<String> componentIds(ProjectFileBean file) {
            return store.getComponents(file.getJavaName()).stream().map(component -> component.componentId).collect(Collectors.toList());
        }

        List<String> eventTargets(ProjectFileBean file) {
            return store.eventMap.get(file.getJavaName()).stream().map(event -> event.targetId).collect(Collectors.toList());
        }
    }
}
