package pro.sketchware.activities.design;

import static org.junit.Assert.*;
import android.content.Intent;
import android.os.*;
import android.view.View;
import android.view.inspector.WindowInspector;
import android.widget.EditText;
import pro.sketchware.R;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.gson.JsonParser;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import pro.sketchware.activities.resourceseditor.*;
import pro.sketchware.activities.resourceseditor.components.fragments.ResourceEditorFragment;
import pro.sketchware.activities.resourceseditor.components.fragments.ArraysEditor;
import pro.sketchware.activities.resourceseditor.components.models.*;
import pro.sketchware.core.resources.ColorModel;
import pro.sketchware.core.project.*;

@RunWith(AndroidJUnit4.class)
public class ResourceEditorSnapshotConsistencyTest {
    @After public void resetFault() {fault(p -> {});}
    @Test public void fivePagesReuseSnapshotsAcrossEditingRecreationAndEmptyPages() throws Exception {
        withEditor((fixture,scenario)->{
            scenario.onActivity(a->{
                for(int page=1;page<6;page++) {var e=editor(a,page);var list=rows(e,page);list.clear();for(int i=0;i<1024;i++)list.add(model(page,i));notes(e).clear();notes(e).put(700,"note A");notes(e).put(1024,"tail A");e.markDraftChanged();}protect(a,true);
                for(int page=1;page<6;page++) {var e=editor(a,page);mutate(rows(e,page).get(700),"B");notes(e).put(700,"note B");notes(e).put(1024,"tail B");e.markDraftChanged(rows(e,page).get(700));}protect(a,true);
            });
            scenario.recreate();await(scenario);
            scenario.onActivity(a->{for(int page=1;page<6;page++){var e=editor(a,page);assertValue(rows(e,page).get(700),"B");assertEquals("note B",notes(e).get(700));assertEquals("tail B",notes(e).get(1024));mutate(rows(e,page).get(10),"C");e.markDraftChanged(rows(e,page).get(10));}protect(a,true);});
            scenario.recreate();await(scenario);
            scenario.onActivity(a->{for(int page=1;page<6;page++){var e=editor(a,page);assertValue(rows(e,page).get(700),"B");assertValue(rows(e,page).get(10),"C");rows(e,page).clear();notes(e).clear();notes(e).put(0,"empty tail");e.markDraftChanged();}protect(a,true);});
            scenario.recreate();await(scenario);
            scenario.onActivity(a->{for(int page=1;page<6;page++){var e=editor(a,page);assertTrue(rows(e,page).isEmpty());assertEquals("empty tail",notes(e).get(0));assertTrue(e.hasDraftChanges());}});
        });
    }
    @Test public void writingUsesFrozenContentAndRapidEditsDoNotQueueMorePrewrites() throws Exception {
        withEditor((fixture,scenario)->{
            CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);AtomicBoolean block=new AtomicBoolean(true);
            CompletableFuture<?>[] future=new CompletableFuture<?>[1];File[] directory=new File[1];
            fault(point->{if(point.equals("before_encode")&&block.getAndSet(false)){entered.countDown();try{if(!release.await(10,TimeUnit.SECONDS))throw new AssertionError("latch timeout");}catch(InterruptedException e){throw new AssertionError(e);}}});
            try {
                scenario.onActivity(a->{var e=a.stylesEditor;rows(e,2).add(model(2,0));e.markDraftChanged();pause(a);Object controller=field(a,"draftController");future[0]=(CompletableFuture<?>)call(controller,"prewrite",new Class[]{ResourceEditorDraftState.Owner[].class},(Object)owners(a));directory[0]=(File)field(field(((CompletableFuture<?>)field(controller,"opening")).join(),"store"),"directory");});
                assertTrue(entered.await(5,TimeUnit.SECONDS));
                scenario.onActivity(a->{for(int i=0;i<20;i++){mutate(rows(a.stylesEditor,2).get(0),"B"+i);a.stylesEditor.markDraftChanged(rows(a.stylesEditor,2).get(0));call(a,"prewriteDrafts",new Class[0]);}pause(a);assertSame("one in-flight transaction",future[0],field(field(a,"draftController"),"writing"));});
                release.countDown();future[0].get(10,TimeUnit.SECONDS);
                File[] pages=new File(directory[0],"pages").listFiles();assertNotNull(pages);assertEquals(1,pages.length);
                var payload=JsonParser.parseString(read(pages[0])).getAsJsonObject();assertEquals("A",payload.getAsJsonArray("entries").get(0).getAsJsonObject().getAsJsonObject("attributes").get("attr").getAsString());
                scenario.onActivity(a->protect(a,true));scenario.recreate();await(scenario);scenario.onActivity(a->assertValue(rows(a.stylesEditor,2).get(0),"B19"));
            } finally {release.countDown();}
        });
    }
    @Test public void failedManifestDoesNotPromoteCacheAndLatestContentCanBeRetried() throws Exception {
        withEditor((fixture,scenario)->{
            scenario.onActivity(a->{rows(a.themesEditor,3).add(model(3,0));a.themesEditor.markDraftChanged();protect(a,true);});
            fault(point->{if(point.equals("before_manifest"))throw new IllegalStateException("injected manifest failure");});
            scenario.onActivity(a->{mutate(rows(a.themesEditor,3).get(0),"failed B");a.themesEditor.markDraftChanged(rows(a.themesEditor,3).get(0));protect(a,false);mutate(rows(a.themesEditor,3).get(0),"latest C");notes(a.themesEditor).put(1,"latest tail");a.themesEditor.markDraftChanged(rows(a.themesEditor,3).get(0));});
            fault(p->{});scenario.onActivity(a->protect(a,true));scenario.recreate();await(scenario);scenario.onActivity(a->{assertValue(rows(a.themesEditor,3).get(0),"latest C");assertEquals("latest tail",notes(a.themesEditor).get(1));});
        });
    }
    @Test public void replaceSkipAndMergeImportsPreserveCurrentSnapshotContent() throws Exception {
        withEditor((fixture,scenario)->{
            for(int mode=0;mode<3;mode++) {
                final int selected=mode;File imported=new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir(),"task4-import-"+fixture.id+".xml");
                Files.write(imported.toPath(),("<resources><!-- imported note --><style name=\"Imported\" parent=\"Parent\"><item name=\"attr\">mode"+mode+"</item></style></resources>").getBytes(StandardCharsets.UTF_8));
                try {scenario.onActivity(a->{a.stylesEditor.updateStylesList(imported.getAbsolutePath(),selected,true);protect(a,true);});scenario.recreate();await(scenario);scenario.onActivity(a->{var list=rows(a.stylesEditor,2);assertEquals(1,list.size());assertEquals(selected==1?"mode0":"mode"+selected,((StyleModel)list.get(0)).getAttribute("attr"));assertEquals("imported note",notes(a.stylesEditor).get(0));});}finally{assertTrue(imported.delete());}
            }
        });
    }
    @Test public void realAttributeDialogsInvalidateOnlyTheEditedModel() throws Exception {
        withEditor((fixture,scenario)->{
            for(int page=2;page<=4;page++) {
                final int current=page;
                ResourceEditorDraftState.Snapshot[] before={null};
                scenario.onActivity(a->{
                    var e=editor(a,current);var list=rows(e,current);
                    list.add(model(current,0));list.add(model(current,1));e.markDraftChanged();protect(a,true);
                    before[0]=e.snapshotDraft();
                    Object row=list.get(0);
                    call(e,current==2?"showStyleAttributesDialog":current==3?"showThemeAttributesDialog":"showArrayAttributesDialog",new Class[]{row.getClass()},row);
                    call(e,"showAttributeDialog",new Class[]{row.getClass(),String.class},row,"attr");
                    View dialog=dialog(current==4?R.id.item_value:R.id.attr_value);
                    ((EditText)dialog.findViewById(current==4?R.id.item_value:R.id.attr_value)).setText("UI B");
                    assertTrue(dialog.findViewById(android.R.id.button1).performClick());
                });
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                scenario.onActivity(a->{
                    var e=editor(a,current);var list=rows(e,current);
                    assertValue(list.get(0),"UI B");
                    var after=e.snapshotDraft();
                    var beforeRows=(List<?>)field(before[0],"entries");var afterRows=(List<?>)field(after,"entries");
                    assertNotSame(beforeRows.get(0),afterRows.get(0));
                    assertSame("unchanged neighbour must be reused by the actual dialog",beforeRows.get(1),afterRows.get(1));
                    assertValue(list.get(0),"UI B");protect(a,true);
                });
                InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
            }
            scenario.recreate();await(scenario);
            scenario.onActivity(a->{for(int page=2;page<=4;page++)assertValue(rows(editor(a,page),page).get(0),"UI B");});
        });
    }
    @Test public void realColorAndDimensionDialogsPreserveNeighbourSnapshotsAndNotes() throws Exception {
        withEditor((fixture,scenario)->{
            for(int page:new int[]{1,5}) {
                final int current=page;
                ResourceEditorDraftState.Snapshot[] before={null};
                scenario.onActivity(a->{
                    var e=editor(a,current);var list=rows(e,current);
                    list.add(current==1?new ColorModel("probe_color","#112233"):new DimenModel("probe_size","1dp"));
                    list.add(current==1?new ColorModel("other_color","#334455"):new DimenModel("other_size","2dp"));
                    e.markDraftChanged();protect(a,true);before[0]=e.snapshotDraft();
                    Object row=list.get(0);
                    call(e,current==1?"showColorEditDialog":"showDimenEditDialog",new Class[]{row.getClass(),int.class},row,0);
                    View root=dialog(current==1?R.id.color_value_input:R.id.dimen_value_input);
                    ((EditText)root.findViewById(current==1?R.id.color_value_input:R.id.dimen_value_input)).setText(current==1?"#445566":"3dp");
                    ((EditText)root.findViewById(R.id.string_header_input)).setText("UI note");
                    assertTrue(root.findViewById(android.R.id.button1).performClick());
                });
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                scenario.onActivity(a->{
                    var e=editor(a,current);var after=e.snapshotDraft();
                    var beforeRows=(List<?>)field(before[0],"entries");var afterRows=(List<?>)field(after,"entries");
                    assertNotSame(beforeRows.get(0),afterRows.get(0));assertSame(beforeRows.get(1),afterRows.get(1));
                    assertValue(rows(e,current).get(0),current==1?"#445566":"3dp");assertEquals("UI note",notes(e).get(0));protect(a,true);
                });
            }
            scenario.recreate();await(scenario);
            scenario.onActivity(a->{assertValue(rows(a.colorsEditor,1).get(0),"#445566");assertValue(rows(a.dimensEditor,5).get(0),"3dp");assertEquals("UI note",notes(a.colorsEditor).get(0));assertEquals("UI note",notes(a.dimensEditor).get(0));});
        });
    }
    private static View dialog(int fieldId) {
        for(View root:WindowInspector.getGlobalWindowViews())if(root.findViewById(fieldId)!=null)return root;
        throw new AssertionError("Missing real edit dialog");
    }
    static Object model(int page,int index) {var attr=new LinkedHashMap<String,String>();attr.put("attr","A");return switch(page){case 1->new ColorModel("color_"+index,"A");case 2,3->new StyleModel("Row"+index,"Parent",attr);case 4->new ArrayModel("array_"+index,ArraysEditor.ARRAYS_TYPES.STRING,attr);default->new DimenModel("size_"+index,"A");};}
    static void mutate(Object row,String value){if(row instanceof StyleModel s)s.getAttributes().entrySet().iterator().next().setValue(value);else if(row instanceof ArrayModel a)a.getAttributes().compute("attr",(k,v)->value);else if(row instanceof ColorModel c)c.setColorValue(value);else ((DimenModel)row).setDimenValue(value);}
    static void assertValue(Object row,String value){assertEquals(value,row instanceof StyleModel s?s.getAttribute("attr"):row instanceof ArrayModel a?a.getAttribute("attr"):row instanceof ColorModel c?c.getColorValue():((DimenModel)row).getDimenValue());}
    static ResourceEditorFragment editor(ResourcesEditorActivity a,int page){return (ResourceEditorFragment)field(a,new String[]{"stringsEditor","colorsEditor","stylesEditor","themesEditor","arraysEditor","dimensEditor"}[page]);}
    @SuppressWarnings("unchecked") static List<Object> rows(Object e,int page){return (List<Object>)field(e,new String[]{"listmap","colorList","stylesList","themesList","arraysList","dimenList"}[page]);}
    @SuppressWarnings("unchecked") static Map<Integer,String> notes(Object e){return (Map<Integer,String>)field(e,"notesMap");}
    static ResourceEditorDraftState.Owner[] owners(ResourcesEditorActivity a){ResourceEditorDraftState.Owner[] result=new ResourceEditorDraftState.Owner[6];for(int i=0;i<6;i++)result[i]=editor(a,i);return result;}
    static Object field(Object o,String n){try{Class<?> c=o.getClass();while(c!=null){try{Field f=c.getDeclaredField(n);f.setAccessible(true);return f.get(o);}catch(NoSuchFieldException e){c=c.getSuperclass();}}throw new NoSuchFieldException(n);}catch(Exception e){throw new AssertionError(e);}}
    static Object call(Object o,String n,Class<?>[] types,Object... args){try{Method m=o.getClass().getDeclaredMethod(n,types);m.setAccessible(true);return m.invoke(o,args);}catch(Exception e){throw new AssertionError(e);}}
    static void pause(ResourcesEditorActivity a){((Handler)field(a,"resourceHandler")).removeCallbacks((Runnable)field(a,"prewriteDraftTask"));}
    static void protect(ResourcesEditorActivity a,boolean expected){assertEquals(expected,call(a,"protectDrafts",new Class[0]));}
    static void fault(Consumer<String> fault){try{Field f=Class.forName("pro.sketchware.activities.resourceseditor.ResourceEditorDraftStore").getDeclaredField("testFault");f.setAccessible(true);f.set(null,fault);}catch(Exception e){throw new AssertionError(e);}}
    static String read(File file)throws Exception{return new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8);}
    static void await(ActivityScenario<ResourcesEditorActivity> scenario){long end=SystemClock.uptimeMillis()+15000;while(SystemClock.uptimeMillis()<end){boolean[] loaded={false};scenario.onActivity(a->{a.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);loaded[0]=(Boolean)field(a,"editorsLoaded");});if(loaded[0]){InstrumentationRegistry.getInstrumentation().waitForIdleSync();return;}SystemClock.sleep(20);}fail("editor load timeout");}
    interface Check {void run(ProjectEditorPersistenceTest.Fixture fixture,ActivityScenario<ResourcesEditorActivity> scenario)throws Exception;}
    static void withEditor(Check check)throws Exception{try(ProjectEditorPersistenceTest.Fixture fixture=new ProjectEditorPersistenceTest.Fixture()){fixture.seed(false,false);for(String name:new String[]{"strings.xml","colors.xml","styles.xml","themes.xml","arrays.xml","dimens.xml"}){File file=new File(SketchwarePaths.getProjectResourceValuesFilePath(fixture.id,name));file.getParentFile().mkdirs();Files.write(file.toPath(),(name.equals("strings.xml")?"<resources><string name=\"app_name\">App</string></resources>":"<resources/>").getBytes(StandardCharsets.UTF_8));}ProjectDataManager.reloadProjectFromData(fixture.id);try(ActivityScenario<ResourcesEditorActivity> scenario=ActivityScenario.launch(new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(),ResourcesEditorActivity.class).putExtra("sc_id",fixture.id))){await(scenario);check.run(fixture,scenario);}}}
}
