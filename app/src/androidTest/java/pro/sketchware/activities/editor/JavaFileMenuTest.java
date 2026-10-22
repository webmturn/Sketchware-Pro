package pro.sketchware.activities.editor;

import static org.junit.Assert.*;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inspector.WindowInspector;
import android.widget.ListView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;
import pro.sketchware.R;
import pro.sketchware.core.project.FileResConfig;
import pro.sketchware.core.project.SketchwarePaths;

@RunWith(AndroidJUnit4.class)
public class JavaFileMenuTest {
    @Test public void openingJavaAndKotlinMenusReadsEachSourceOnce()throws Exception {
        try(Fixture f=new Fixture()) {
            for(String name:List.of("Foo.java","Foo.kt")) {
                File file=f.source(name,"package test.menu"+(name.endsWith("java")?";":"")+"\nclass Foo {}\n");
                f.select(file);f.open();
                f.run(a->{assertEquals(1,f.adapter.reads);assertEquals(Set.of(1,3,5,6,7,8),menuIds());});f.dismiss();
                assertEquals("package test.menu"+(name.endsWith("java")?";":"")+"\nclass Foo {}\n",new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8));
            }
        }
    }
    @Test public void foldersDoNotResolveAClassName()throws Exception {
        try(Fixture f=new Fixture()) {
            File folder=new File(f.javaDir,"folder");assertTrue(folder.mkdir());f.select(folder);f.open();
            f.run(a->{assertEquals(0,f.adapter.reads);assertEquals(Set.of(7,8),menuIds());});
        }
    }
    @Test public void manifestMembershipUsesTheSameResolvedName()throws Exception {
        try(Fixture f=new Fixture()) {
            File file=f.source("Foo.java","package test.menu;\nclass Foo {}\n");f.select(file);
            f.run(a->f.config(a).getJavaManifestList().add("test.menu.Foo"));f.open();
            f.run(a->{assertEquals(1,f.adapter.reads);assertEquals(Set.of(2,5,6,7,8),menuIds());});f.dismiss();
            f.run(a->{f.config(a).getJavaManifestList().clear();f.config(a).getServiceManifestList().add("test.menu.Foo");});f.open();
            f.run(a->{assertEquals(2,f.adapter.reads);assertEquals(Set.of(4,5,6,7,8),menuIds());});
        }
    }
    @Test public void reopeningMenuReadsModifiedSourceAndNoPackageUsesFileName()throws Exception {
        try(Fixture f=new Fixture()) {
            File file=f.source("Foo.kt","package old.name\nclass Foo {}\n");f.select(file);
            f.run(a->f.config(a).getJavaManifestList().add("old.name.Foo"));f.open();f.run(a->assertTrue(menuIds().contains(2)));f.dismiss();
            Files.write(file.toPath(),"package new.name\nclass Foo {}\n".getBytes(StandardCharsets.UTF_8));
            f.run(a->f.config(a).getServiceManifestList().add("new.name.Foo"));f.open();f.run(a->assertEquals(Set.of(4,5,6,7,8),menuIds()));f.dismiss();
            Files.write(file.toPath(),"class Foo {}".getBytes(StandardCharsets.UTF_8));
            f.run(a->f.config(a).getJavaManifestList().add("Foo"));f.open();
            f.run(a->{assertEquals(3,f.adapter.reads);assertEquals(Set.of(2,5,6,7,8),menuIds());});
        }
    }
    @Test public void selectingManifestActionRereadsExternalChanges()throws Exception {
        try(Fixture f=new Fixture()) {
            File file=f.source("Foo.java","package old.name;\nclass Foo {}\n");f.select(file);f.open();
            Files.write(file.toPath(),"package new.name;\nclass Foo {}\n".getBytes(StandardCharsets.UTF_8));
            f.run(a->{click(1);assertEquals(2,f.adapter.reads);assertTrue(f.config(a).getJavaManifestList().contains("new.name.Foo"));assertFalse(f.config(a).getJavaManifestList().contains("old.name.Foo"));});
        }
    }
    static Set<Integer> menuIds(){Set<Integer> result=new HashSet<>();ListView list=popup();assertNotNull(list);for(int i=0;i<list.getAdapter().getCount();i++)result.add(((MenuItem)list.getAdapter().getItem(i)).getItemId());return result;}
    static void click(int id){ListView list=popup();assertNotNull(list);for(int i=0;i<list.getAdapter().getCount();i++)if(((MenuItem)list.getAdapter().getItem(i)).getItemId()==id){assertTrue(list.performItemClick(list.getChildAt(i),i,list.getAdapter().getItemId(i)));return;}fail("Menu item not found: "+id);}
    static ListView popup(){List<View> roots=WindowInspector.getGlobalWindowViews();for(int i=roots.size()-1;i>=0;i--){View root=roots.get(i);if(root.isShown()){ListView found=find(root);if(found!=null)return found;}}return null;}
    static ListView find(View view){if(view instanceof ListView list)return list;if(view instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++){ListView found=find(group.getChildAt(i));if(found!=null)return found;}return null;}
    static final class TrackingAdapter extends ManageJavaActivity.FilesAdapter {
        int reads;
        TrackingAdapter(ManageJavaActivity owner,List<String> files){owner.super(files);}
        @Override public String getFullName(int position){reads++;return super.getFullName(position);}
    }
    static final class Fixture implements AutoCloseable {
        final String id="__java_menu_"+UUID.randomUUID();
        final File root=new File(SketchwarePaths.getDataPath(id));
        final File javaDir=new File(SketchwarePaths.getProjectJavaPath(id));
        final ActivityScenario<ManageJavaActivity> scenario;
        TrackingAdapter adapter;
        Fixture()throws Exception {
            assertFalse(root.exists());assertTrue(root.mkdirs());
            scenario=ActivityScenario.launch(new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(),ManageJavaActivity.class).putExtra("sc_id",id).putExtra("pkgName","test.java.menu"));
            run(a->a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
        }
        File source(String name,String body)throws Exception{File file=new File(javaDir,name);Files.write(file.toPath(),body.getBytes(StandardCharsets.UTF_8));return file;}
        void select(File file){run(a->{adapter=new TrackingAdapter(a,new ArrayList<>(List.of(file.getPath())));try{Field field=ManageJavaActivity.class.getDeclaredField("filesAdapter");field.setAccessible(true);field.set(a,adapter);}catch(Exception e){throw new AssertionError(e);}a.binding.filesListRecyclerView.setAdapter(adapter);a.binding.noContentLayout.setVisibility(View.GONE);});}
        void open(){run(a->{var holder=(ManageJavaActivity.FilesAdapter.ViewHolder)a.binding.filesListRecyclerView.findViewHolderForAdapterPosition(0);assertNotNull(holder);assertTrue(holder.binding.more.performClick());});}
        FileResConfig config(ManageJavaActivity a){try{Field f=ManageJavaActivity.class.getDeclaredField("frc");f.setAccessible(true);return (FileResConfig)f.get(a);}catch(Exception e){throw new AssertionError(e);}}
        void dismiss(){run(a->{ListView list=popup();if(list!=null){View root=list.getRootView();assertTrue(root.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_BACK)));assertTrue(root.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BACK)));}});}
        void run(Consumer<ManageJavaActivity> action){AtomicReference<Throwable> error=new AtomicReference<>();scenario.onActivity(a->{try{action.accept(a);}catch(Throwable e){error.set(e);}});if(error.get()!=null)throw new AssertionError(error.get());InstrumentationRegistry.getInstrumentation().waitForIdleSync();}
        @Override public void close()throws Exception{try{try{dismiss();}finally{scenario.close();}}finally{assertEquals(id,root.getName());var owned=root.getCanonicalFile().toPath();try(Stream<java.nio.file.Path> paths=Files.walk(owned)){for(var path:(Iterable<java.nio.file.Path>)paths.sorted(Comparator.reverseOrder())::iterator){assertTrue(path.startsWith(owned));Files.delete(path);}}}}
    }
}
