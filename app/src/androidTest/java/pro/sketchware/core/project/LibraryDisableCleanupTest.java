package pro.sketchware.core.project;

import static org.junit.Assert.*;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import pro.sketchware.beans.ComponentBean;
import pro.sketchware.beans.ProjectFileBean;
import pro.sketchware.beans.ProjectLibraryBean;
import pro.sketchware.beans.ViewBean;

/** Exercises only private in-memory stores; never loads or saves a project. */
@RunWith(AndroidJUnit4.class)
public class LibraryDisableCleanupTest {
    @Test
    public void disablingFirebaseRemovesEveryFirebaseTypeAndItsEvents() {
        Fixture fixture = new Fixture();
        for (ProjectFileBean file : fixture.files.getActivities()) {
            for (int type :
                    new int[] {
                        ComponentBean.COMPONENT_TYPE_FIREBASE,
                        ComponentBean.COMPONENT_TYPE_FIREBASE_AUTH,
                        ComponentBean.COMPONENT_TYPE_FIREBASE_STORAGE,
                        ComponentBean.COMPONENT_TYPE_FIREBASE_AUTH_PHONE,
                        ComponentBean.COMPONENT_TYPE_FIREBASE_CLOUD_MESSAGE,
                        ComponentBean.COMPONENT_TYPE_FIREBASE_AUTH_GOOGLE_LOGIN
                    }) {
                fixture.component(file, type, "firebase_" + type);
            }
            fixture.component(file, ComponentBean.COMPONENT_TYPE_INTENT, "keep_intent");
            fixture.component(
                    file, ComponentBean.COMPONENT_TYPE_REWARDED_VIDEO_AD, "keep_rewarded");
        }

        fixture.store.removeFirebaseComponents(
                new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_FIREBASE));

        for (ProjectFileBean file : fixture.files.getActivities()) {
            assertEquals(List.of("keep_intent", "keep_rewarded"), fixture.componentIds(file));
            assertEquals(List.of("keep_intent", "keep_rewarded"), fixture.eventTargets(file));
            assertEquals(2, fixture.store.blockMap.get(file.getJavaName()).size());
            assertFalse(
                    fixture.store.blockMap.get(file.getJavaName()).keySet().stream()
                            .anyMatch(key -> key.startsWith("firebase_")));
        }
    }

    @Test
    public void disablingAdmobRemovesBothAdComponentsAndBannerViews() {
        Fixture fixture = new Fixture();
        for (ProjectFileBean file : fixture.files.getActivities()) {
            fixture.component(file, ComponentBean.COMPONENT_TYPE_INTERSTITIAL_AD, "interstitial");
            fixture.component(file, ComponentBean.COMPONENT_TYPE_REWARDED_VIDEO_AD, "rewarded");
            fixture.component(file, ComponentBean.COMPONENT_TYPE_FIREBASE_AUTH_PHONE, "keep_phone");
            fixture.banner(file);
        }
        fixture.banner(fixture.files.getCustomViews().get(0));

        fixture.store.removeAdmobViewsAndComponents(
                new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_ADMOB), fixture.files);

        for (ProjectFileBean file : fixture.files.getActivities()) {
            assertEquals(List.of("keep_phone"), fixture.componentIds(file));
            assertEquals(List.of("keep_phone"), fixture.eventTargets(file));
            assertEquals(
                    List.of("keep_phone_onComplete"),
                    new ArrayList<>(fixture.store.blockMap.get(file.getJavaName()).keySet()));
        }
        for (ArrayList<ViewBean> views : fixture.store.viewMap.values()) {
            assertEquals(1, views.size());
            assertEquals("keep_label", views.get(0).id);
        }
    }

    @Test
    public void enabledLibrariesKeepComponentsEventsAndViews() {
        Fixture fixture = new Fixture();
        ProjectFileBean file = fixture.files.getActivities().get(0);
        fixture.component(
                file, ComponentBean.COMPONENT_TYPE_FIREBASE_AUTH_GOOGLE_LOGIN, "google_login");
        fixture.component(file, ComponentBean.COMPONENT_TYPE_REWARDED_VIDEO_AD, "rewarded");
        fixture.banner(file);
        ProjectLibraryBean firebase =
                new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_FIREBASE);
        firebase.useYn = ProjectLibraryBean.LIB_USE_Y;
        ProjectLibraryBean admob =
                new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_ADMOB);
        admob.useYn = ProjectLibraryBean.LIB_USE_Y;
        ProjectLibraryBean maps =
                new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_GOOGLE_MAP);
        maps.useYn = ProjectLibraryBean.LIB_USE_Y;
        fixture.store.addView(
                file.getXmlName(), new ViewBean("map", ViewBean.VIEW_TYPE_WIDGET_MAPVIEW));

        fixture.store.removeFirebaseComponents(firebase);
        fixture.store.removeAdmobViewsAndComponents(admob, fixture.files);
        fixture.store.removeMapViews(maps, fixture.files);

        assertEquals(List.of("google_login", "rewarded"), fixture.componentIds(file));
        assertEquals(List.of("google_login", "rewarded", "banner"), fixture.eventTargets(file));
        assertEquals(3, fixture.store.blockMap.get(file.getJavaName()).size());
        assertEquals(3, fixture.store.getViews(file.getXmlName()).size());
    }

    @Test
    public void disablingMapsRemovesConsecutiveMapsAndKeepsOtherViews() {
        Fixture fixture = new Fixture();
        ProjectFileBean first = fixture.files.getActivities().get(0);
        ProjectFileBean second = fixture.files.getActivities().get(1);
        ProjectFileBean custom = fixture.files.getCustomViews().get(0);
        ProjectFileBean drawer =
                new ProjectFileBean(
                        ProjectFileBean.PROJECT_FILE_TYPE_DRAWER, first.getDrawerName());
        fixture.files.getCustomViews().add(drawer);

        fixture.store.addView(
                first.getXmlName(),
                new ViewBean("first_label", ViewBean.VIEW_TYPE_WIDGET_TEXTVIEW));
        fixture.store.addView(
                first.getXmlName(), new ViewBean("first_map", ViewBean.VIEW_TYPE_WIDGET_MAPVIEW));
        fixture.store.addView(
                first.getXmlName(), new ViewBean("second_map", ViewBean.VIEW_TYPE_WIDGET_MAPVIEW));
        fixture.event(first, "first_map");
        fixture.event(first, "second_map");
        fixture.event(first, "first_label");

        fixture.store.addView(
                second.getXmlName(), new ViewBean("map", ViewBean.VIEW_TYPE_WIDGET_MAPVIEW));
        fixture.store.addView(
                second.getXmlName(), new ViewBean("label1", ViewBean.VIEW_TYPE_WIDGET_TEXTVIEW));
        fixture.store.addView(
                second.getXmlName(), new ViewBean("label2", ViewBean.VIEW_TYPE_WIDGET_TEXTVIEW));
        fixture.store.addView(
                custom.getXmlName(), new ViewBean("map", ViewBean.VIEW_TYPE_WIDGET_MAPVIEW));
        fixture.store.addView(
                drawer.getXmlName(), new ViewBean("map", ViewBean.VIEW_TYPE_WIDGET_MAPVIEW));

        fixture.store.removeMapViews(
                new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_GOOGLE_MAP),
                fixture.files);

        assertEquals(List.of("first_label"), fixture.viewIds(first));
        assertEquals(List.of("label1", "label2"), fixture.viewIds(second));
        assertTrue(fixture.viewIds(custom).isEmpty());
        assertTrue(fixture.viewIds(drawer).isEmpty());
        assertEquals(List.of("first_label"), fixture.eventTargets(first));
        assertEquals(
                List.of("first_label_onComplete"),
                new ArrayList<>(fixture.store.blockMap.get(first.getJavaName()).keySet()));
    }

    private static final class Fixture {
        final String id = "__library_cleanup_" + UUID.randomUUID();
        final ProjectDataStore store = new ProjectDataStore(id);
        final ProjectFileManager files = new ProjectFileManager(id);

        Fixture() {
            files.getActivities()
                    .add(new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY, "second"));
            files.getCustomViews()
                    .add(new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_CUSTOM_VIEW, "row"));
        }

        void component(ProjectFileBean file, int type, String name) {
            store.addComponent(file.getJavaName(), type, name);
            event(file, name);
        }

        void banner(ProjectFileBean file) {
            store.addView(
                    file.getXmlName(), new ViewBean("banner", ViewBean.VIEW_TYPE_WIDGET_ADVIEW));
            store.addView(
                    file.getXmlName(),
                    new ViewBean("keep_label", ViewBean.VIEW_TYPE_WIDGET_TEXTVIEW));
            if (file.fileType == ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY) event(file, "banner");
        }

        void event(ProjectFileBean file, String name) {
            store.addEvent(file.getJavaName(), 2, 0, name, "onComplete");
            store.blockMap
                    .computeIfAbsent(file.getJavaName(), key -> new HashMap<>())
                    .put(name + "_onComplete", new ArrayList<>());
        }

        List<String> componentIds(ProjectFileBean file) {
            return store.getComponents(file.getJavaName()).stream()
                    .map(component -> component.componentId)
                    .collect(Collectors.toList());
        }

        List<String> eventTargets(ProjectFileBean file) {
            return store.eventMap.get(file.getJavaName()).stream()
                    .map(event -> event.targetId)
                    .collect(Collectors.toList());
        }

        List<String> viewIds(ProjectFileBean file) {
            return store.getViews(file.getXmlName()).stream()
                    .map(view -> view.id)
                    .collect(Collectors.toList());
        }
    }
}
