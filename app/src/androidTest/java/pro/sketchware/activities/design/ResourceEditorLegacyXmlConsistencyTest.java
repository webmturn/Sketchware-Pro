package pro.sketchware.activities.design;

import static org.junit.Assert.*;
import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.Consumer;
import pro.sketchware.activities.resourceseditor.ResourcesEditorActivity;
import pro.sketchware.activities.resourceseditor.ResourceEditorSaveResult;
import pro.sketchware.core.project.ProjectDataManager;
import pro.sketchware.core.project.SketchwarePaths;

@RunWith(AndroidJUnit4.class)
public class ResourceEditorLegacyXmlConsistencyTest {
    @Test public void xmlOnlyThenStaleLegacyWithoutUuidKeepsSavedB() throws Exception { xmlOnly(false,false); }
    @Test public void xmlOnlyThenClosedUuidAndStaleLegacyKeepsSavedB() throws Exception { xmlOnly(true,false); }
    @Test public void confirmationFailureStillBlocksLegacyAUsingPreparedTarget() throws Exception { xmlOnly(false,true); }

    private void xmlOnly(boolean keepSession,boolean failConfirm) throws Exception {
        withLegacy(keepSession,false,(fixture,scenario,old)->{
            fault(point->{if(point.equals("before_manifest")||failConfirm&&point.equals("before_legacy_xml_confirm"))throw new RuntimeException("Injected");});
            on(scenario,a->{
                a.stringsEditor.addString("saved_new_b","saved B","new B note");
                assertTrue(((ResourceEditorSaveResult)call(a,"saveEditorsChanges")).canSaveXmlOnly);
                call(a,"performXmlOnlySave",new Object[]{null});
            });
            assertTrue(read(xml(fixture.id,"strings.xml")).contains("saved_new_b"));
            fault(p->{});restoreOld(fixture.id,scenario,old);
            on(scenario,a->{assertB(a);assertTrue(((ResourceEditorSaveResult)call(a,"saveEditorsChanges")).navigationAllowed);});
            assertTrue(read(xml(fixture.id,"strings.xml")).contains("saved_new_b"));
        });
    }
    @Test public void normalSaveThenSameUuidAndOldLegacyKeepsSavedB() throws Exception {
        withLegacy(true,false,(fixture,scenario,old)->{
            fault(p->{});
            on(scenario,a->{a.stringsEditor.addString("saved_new_b","saved B","new B note");
                assertTrue(((ResourceEditorSaveResult)call(a,"saveEditorsChanges")).navigationAllowed);});
            restoreOld(fixture.id,scenario,old);
            on(scenario,ResourceEditorLegacyXmlConsistencyTest::assertB);
            assertTrue(read(xml(fixture.id,"strings.xml")).contains("saved_new_b"));
        });
    }
    @Test public void partialXmlAndPersistentMigrationFailureKeepFailedAAndWrittenColorB() throws Exception {
        withLegacy(false,true,(fixture,scenario,old)->{
            File blocked=new File(xml(fixture.id,"strings.xml").getParentFile(),"blocked-target");
            assertTrue(blocked.mkdir());Files.write(new File(blocked,"child").toPath(),new byte[]{1});
            on(scenario,a->{
                a.stringsEditor.addString("saved_new_b","unsaved B","");a.stringsEditor.filePath=blocked.getPath();
                var color=a.colorsEditor.colorList.stream().filter(row->"legacy_color".equals(row.getColorName())).findFirst().orElseThrow();
                color.setColorValue("#778899");a.colorsEditor.markDraftChanged(color);
                assertTrue(((ResourceEditorSaveResult)call(a,"saveEditorsChanges")).canSaveXmlOnly);
                call(a,"performXmlOnlySave",new Object[]{null});
            });
            assertTrue(read(xml(fixture.id,"colors.xml")).contains("#778899"));
            assertFalse(read(xml(fixture.id,"strings.xml")).contains("saved_new_b"));
            restoreOld(fixture.id,scenario,old); // before_manifest keeps failing for the unsaved Legacy page.
            on(scenario,a->{
                assertTrue(a.stringsEditor.hasUnsavedChanges);
                assertTrue(a.stringsEditor.listmap.stream().anyMatch(row->"legacy_only".equals(row.get("key"))));
                assertFalse(a.stringsEditor.listmap.stream().anyMatch(row->"saved_new_b".equals(row.get("key"))));
                assertTrue(a.colorsEditor.colorList.stream().anyMatch(row->"legacy_color".equals(row.getColorName())&&"#778899".equals(row.getColorValue())));
                assertFalse(a.colorsEditor.hasUnsavedChanges);
            });
            fault(p->{});on(scenario,a->assertTrue(((ResourceEditorSaveResult)call(a,"saveEditorsChanges")).navigationAllowed));
            assertTrue(read(xml(fixture.id,"colors.xml")).contains("#778899"));
        });
    }

    private interface Check {void run(ProjectEditorPersistenceTest.Fixture fixture,ActivityScenario<ResourcesEditorActivity> scenario,Bundle old) throws Exception;}
    private interface Action {void run(ResourcesEditorActivity activity) throws Exception;}
    private void withLegacy(boolean keepSession,boolean colors,Check check) throws Exception {
        try(ProjectEditorPersistenceTest.Fixture fixture=new ProjectEditorPersistenceTest.Fixture()) {
            fixture.seed(false,false);helper("seedResources",fixture.id);ProjectDataManager.reloadProjectFromData(fixture.id);
            try(ActivityScenario<ResourcesEditorActivity> scenario=ActivityScenario.launch(new Intent(
                    InstrumentationRegistry.getInstrumentation().getTargetContext(),ResourcesEditorActivity.class).putExtra("sc_id",fixture.id))) {
                await(scenario);on(scenario,a->helper("savedState",a));
                fault(p->{if(p.equals("before_manifest"))throw new RuntimeException("Injected legacy failure");});
                Bundle legacy=(Bundle)helper("legacyBundle",true,colors);
                var inject=(Application.ActivityLifecycleCallbacks)helper("injectLegacy",fixture.id,legacy,keepSession);
                Application app=application();app.registerActivityLifecycleCallbacks(inject);
                try{scenario.recreate();}finally{helper("unregister",app,inject);}await(scenario);
                Bundle[] old={null};on(scenario,a->{old[0]=(Bundle)helper("savedState",a);
                    assertTrue(old[0].containsKey("resource_editor_drafts"));
                    assertEquals(keepSession,old[0].getString("resource_draft_session")!=null);});
                check.run(fixture,scenario,old[0]);
            } finally {fault(p->{});}
        }
    }
    private static void restoreOld(String id,ActivityScenario<ResourcesEditorActivity> scenario,Bundle old) throws Exception {
        var inject=(Application.ActivityLifecycleCallbacks)helper("injectLegacy",id,old.getBundle("resource_editor_drafts"),false);
        var callback=new Application.ActivityLifecycleCallbacks() {
            public void onActivityCreated(Activity a,Bundle s){} public void onActivityStarted(Activity a){}
            public void onActivityResumed(Activity a){} public void onActivityPaused(Activity a){}
            public void onActivityStopped(Activity a){} public void onActivitySaveInstanceState(Activity a,Bundle s){}
            public void onActivityDestroyed(Activity a){}
            @Override public void onActivityPostSaveInstanceState(Activity a,Bundle state) {
                if(!(a instanceof ResourcesEditorActivity editor)||!id.equals(editor.sc_id))return;
                inject.onActivityPostSaveInstanceState(a,state);
                if(old.getString("resource_draft_session")!=null)state.putString("resource_draft_session",old.getString("resource_draft_session"));
            }
        };
        Application app=application();app.registerActivityLifecycleCallbacks(callback);
        try{scenario.recreate();}finally{helper("unregister",app,callback);}await(scenario);
    }
    private static void assertB(ResourcesEditorActivity a) {
        assertTrue("Saved B must survive old Legacy",a.stringsEditor.listmap.stream().anyMatch(row->"saved_new_b".equals(row.get("key"))));
        assertFalse(a.stringsEditor.hasUnsavedChanges);
    }
    private static Application application(){return (Application)InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();}
    private static void on(ActivityScenario<ResourcesEditorActivity> scenario,Action action){scenario.onActivity(a->{try{action.run(a);}catch(Exception e){throw new AssertionError(e);}});}
    private static void await(ActivityScenario<ResourcesEditorActivity> scenario) throws Exception {
        on(scenario,a->a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));helper("awaitLoaded",scenario);
    }
    private static void fault(Consumer<String> fault) throws Exception {helper("setFault",fault);}
    private static Object helper(String name,Object...args) throws Exception{return invoke(ResourceEditorLegacyMigrationTest.class,null,name,args);}
    private static Object call(Object target,String name,Object...args) throws Exception{return invoke(target.getClass(),target,name,args);}
    private static Object invoke(Class<?> type,Object target,String name,Object[] args) throws Exception {
        for(Method method:type.getDeclaredMethods())if(method.getName().equals(name)&&method.getParameterCount()==args.length){method.setAccessible(true);return method.invoke(target,args);}
        throw new NoSuchMethodException(name);
    }
    private static File xml(String id,String name){return new File(SketchwarePaths.getProjectResourceValuesFilePath(id,name));}
    private static String read(File file) throws Exception{return new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8);}
}
