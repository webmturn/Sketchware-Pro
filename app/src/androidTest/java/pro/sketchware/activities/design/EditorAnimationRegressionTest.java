package pro.sketchware.activities.design;

import static org.junit.Assert.*;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.MotionEvent;
import android.view.InputDevice;
import android.view.inspector.WindowInspector;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.filters.SdkSuppress;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.json.JSONObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import pro.sketchware.R;
import pro.sketchware.activities.design.fragments.ViewEditorFragment;
import pro.sketchware.activities.editor.LogicEditorActivity;
import pro.sketchware.activities.editor.event.AddEventActivity;
import pro.sketchware.activities.editor.property.PropertyInputItem;
import pro.sketchware.activities.editor.view.ViewEditor;
import pro.sketchware.activities.main.fragments.projects_store.ProjectPreviewActivity;
import pro.sketchware.beans.ComponentBean;
import pro.sketchware.beans.ProjectFileBean;
import pro.sketchware.core.project.ProjectDataManager;
import pro.sketchware.databinding.FragmentStoreProjectPreviewBinding;
import pro.sketchware.databinding.LogicPopupAddEventBinding;
import pro.sketchware.graphics.AnimationUtil;
import pro.sketchware.widgets.CustomHorizontalScrollView;
import pro.sketchware.widgets.base.CollapsibleViewHolder;
import com.google.android.material.slider.Slider;

/** Exercises real editor controls using disposable projects; no user project is modified. */
@RunWith(AndroidJUnit4.class)
public class EditorAnimationRegressionTest {
    @Test public void logicSideDrawerAndDragToolbar() throws Exception {
        try(ProjectEditorPersistenceTest.Fixture f = new ProjectEditorPersistenceTest.Fixture()) {
            f.seed(false,false);
            ProjectDataManager.reloadProjectFromData(f.id);
            Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(),LogicEditorActivity.class)
                    .putExtra("sc_id",f.id).putExtra("id","onCreate").putExtra("event","initializeLogic")
                    .putExtra("event_text","Animation regression")
                    .putExtra("project_file",new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY,"main"));
            try(ActivityScenario<LogicEditorActivity> scenario = ActivityScenario.launch(intent)) {
                ready(scenario);
                scenario.onActivity(a -> a.toggleDrawerVisibility(true));
                pause(600);
                scenario.onActivity(a -> a.toggleDrawerVisibility(false));
                pause(350);
                scenario.onActivity(a -> {
                    View drawer=a.findViewById(R.id.right_drawer);
                    assertEquals("Drawer should travel by its width", drawer.getWidth(), drawer.getTranslationX(), 1f);
                });
                reverse(scenario,R.id.right_drawer,a -> ((LogicEditorActivity)a).toggleDrawerVisibility(true),
                        a -> ((LogicEditorActivity)a).toggleDrawerVisibility(false),true,"drawer_reverse",600,350);
                reverse(scenario,R.id.top_menu,a -> ((LogicEditorActivity)a).toggleTopMenuVisibility(true),
                        a -> ((LogicEditorActivity)a).toggleTopMenuVisibility(false),false,"logic_drag_toolbar_reverse",600,350);
            }
        }
    }

    @Test public void designPropertyAndDragPanels() throws Exception {
        withDesign(scenario -> {
            reverse(scenario,R.id.view_property,a -> fragment((DesignActivity)a).togglePropertyView(true),
                    a -> fragment((DesignActivity)a).togglePropertyView(false),false,"property_panel_reverse",800,350);
            scenario.onActivity(a -> fragment(a).togglePropertyView(true));
            pause(800);
            scenario.onActivity(a -> {
                ViewEditor editor=fragment(a).viewEditor;
                call(editor,"showDeleteView",new Class<?>[]{boolean.class,boolean.class},true,false);
            });
            pause(600);
            scenario.onActivity(a -> call(fragment(a).viewEditor,"showDeleteView",new Class<?>[]{boolean.class,boolean.class},false,false));
            pause(600);
            scenario.onActivity(a -> call(fragment(a).viewEditor,"showDeleteView",new Class<?>[]{boolean.class,boolean.class},true,false));
            pause(600);
            scenario.onActivity(a -> call(fragment(a).viewEditor,"showDeleteView",new Class<?>[]{boolean.class,boolean.class},false,false));
            pause(100);
            scenario.onActivity(a -> {
                View target=a.findViewById(R.id.delete_view);
                float before=target.getTranslationY();
                call(fragment(a).viewEditor,"showDeleteView",new Class<?>[]{boolean.class,boolean.class},true,false);
                float after=target.getTranslationY();
                assertEquals("Drag actions must reverse from the current position",before,after,1f);
            });
            pause(600);
            scenario.onActivity(a -> {
                CustomHorizontalScrollView scroll=a.findViewById(R.id.hcv_property);
                scroll.scrollTo(10,0);
                scroll.scrollTo(scroll.getChildAt(0).getWidth(),0);
            });
            pause(250);
            scenario.onActivity(a -> ((CustomHorizontalScrollView)a.findViewById(R.id.hcv_property)).scrollTo(10,0));
            pause(500);
            scenario.onActivity(a -> {
                CustomHorizontalScrollView scroll=a.findViewById(R.id.hcv_property);
                scroll.scrollTo(scroll.getChildAt(0).getWidth(),0);
            });
            pause(70);
            scenario.onActivity(a -> {
                View target=a.findViewById(R.id.layout_property_see_all);
                float before=target.getTranslationY();
                ((CustomHorizontalScrollView)a.findViewById(R.id.hcv_property)).scrollTo(10,0);
                float after=target.getTranslationY();
                assertEquals("Property shortcut must reverse from the current position",before,after,1f);
            });
            pause(500);
            scenario.onActivity(a -> {
                CustomHorizontalScrollView scroll=a.findViewById(R.id.hcv_property);
                scroll.scrollTo(scroll.getChildAt(0).getWidth(),0);
            });
            pause(300);
            scenario.onActivity(a -> ((CustomHorizontalScrollView)a.findViewById(R.id.hcv_property)).scrollTo(0,0));
            pause(500);
            scenario.onActivity(a -> {
                View target=a.findViewById(R.id.layout_property_see_all);
                boolean visible=(Boolean)field(a.findViewById(R.id.view_property),"showAllVisible");
                assertTrue("Property shortcut must return when scrolled back to zero",visible);
                assertEquals(0f,target.getTranslationY(),1f);
            });
        });
    }

    @Test public void basicAndFavoritePaletteSwitching() throws Exception {
        withDesign(scenario -> {
            scenario.onActivity(a -> {
                ViewGroup group=fragment(a).viewEditor.findViewById(R.id.palette_group);
                group.getChildAt(1).performClick();
            });
            pause(35);
            scenario.onActivity(a -> {
                ViewGroup group=fragment(a).viewEditor.findViewById(R.id.palette_group);
                group.getChildAt(0).performClick();
            });
            pause(500);
            scenario.onActivity(a -> {
                ViewEditor editor=fragment(a).viewEditor;
                View basic=editor.findViewById(R.id.palette_widget),favorite=editor.findViewById(R.id.palette_favorite);
                ViewGroup group=editor.findViewById(R.id.palette_group);
                boolean incorrect=group.getChildAt(0).isSelected() && (basic.getVisibility()!=View.VISIBLE || basic.getAlpha()<0.99f
                        || favorite.getVisibility()!=View.GONE);
                assertTrue(group.getChildAt(0).isSelected());
                assertFalse("Visible palette must match the last selected tab",incorrect);
            });
            scenario.onActivity(a -> selectPalette(a,1));
            pause(300);
            scenario.onActivity(a -> selectPalette(a,0));
            pause(35);
            scenario.onActivity(a -> { selectPalette(a,1); selectPalette(a,1); });
            pause(300);
            scenario.onActivity(a -> {
                ViewEditor editor=fragment(a).viewEditor;
                assertTrue(((ViewGroup)editor.findViewById(R.id.palette_group)).getChildAt(1).isSelected());
                assertEquals(View.GONE,editor.findViewById(R.id.palette_widget).getVisibility());
                assertEquals(View.VISIBLE,editor.findViewById(R.id.palette_favorite).getVisibility());
                assertEquals(1f,editor.findViewById(R.id.palette_favorite).getAlpha(),0.01f);
            });
        });
    }

    @Test public void componentCollapseReversedByExpand() throws Exception {
        try(ProjectEditorPersistenceTest.Fixture f = new ProjectEditorPersistenceTest.Fixture()) {
            f.seed(false,false);
            ProjectDataManager.reloadProjectFromData(f.id);
            ProjectDataManager.getProjectDataManager(f.id).addComponent(ProjectFileBean.DEFAULT_JAVA_NAME,
                    ComponentBean.COMPONENT_TYPE_INTENT,"animationIntent");
            assertTrue(ProjectDataManager.getProjectDataManager(f.id).saveAllData());
            try(ActivityScenario<DesignActivity> scenario=f.launch()) {
                ready(scenario);
                ProjectEditorPersistenceTest.waitForEditor(scenario);
                scenario.onActivity(a -> ((androidx.viewpager.widget.ViewPager)a.findViewById(R.id.viewpager)).setCurrentItem(2,false));
                RecyclerView.ViewHolder[] holder={null};
                long deadline=SystemClock.uptimeMillis()+10000;
                while(SystemClock.uptimeMillis()<deadline) {
                    scenario.onActivity(a -> {
                        RecyclerView list=a.findViewById(R.id.component_list);
                        if(list!=null)holder[0]=list.findViewHolderForAdapterPosition(0);
                    });
                    if(holder[0]!=null)break;
                    pause(30);
                }
                assertNotNull("Component row did not load",holder[0]);
                scenario.onActivity(a -> ((CollapsibleViewHolder)holder[0]).expand());
                pause(400);
                scenario.onActivity(a -> {
                    holder[0]=((RecyclerView)a.findViewById(R.id.component_list)).findViewHolderForAdapterPosition(0);
                    assertNotNull(holder[0]);assertEquals(0,holder[0].getLayoutPosition());
                    ((CollapsibleViewHolder)holder[0]).collapse();
                    assertTrue(ProjectDataManager.getProjectDataManager(f.id).getComponents(ProjectFileBean.DEFAULT_JAVA_NAME).get(0).isCollapsed);
                    ((CollapsibleViewHolder)holder[0]).expand();
                });
                pause(400);
                scenario.onActivity(a -> {
                    View options=holder[0].itemView.findViewById(R.id.component_option_layout);
                    boolean collapsed=ProjectDataManager.getProjectDataManager(f.id).getComponents(ProjectFileBean.DEFAULT_JAVA_NAME).get(0).isCollapsed;
                    assertFalse(collapsed);
                    assertEquals("Old collapse callback must not hide the expanded row",View.VISIBLE,options.getVisibility());
                    assertTrue(options.getHeight()>0);
                    assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT,options.getLayoutParams().height);
                });
                // Rebinding an expanded model must discard an earlier collapse animation.
                scenario.onActivity(a -> {
                    RecyclerView list=a.findViewById(R.id.component_list);
                    CollapsibleViewHolder current=(CollapsibleViewHolder)list.findViewHolderForAdapterPosition(0);
                    assertNotNull(current);
                    current.collapse();
                    ProjectDataManager.getProjectDataManager(f.id).getComponents(ProjectFileBean.DEFAULT_JAVA_NAME).get(0).isCollapsed=false;
                    list.getAdapter().notifyItemChanged(0);
                });
                pause(400);
                scenario.onActivity(a -> assertExpandedRow(a,R.id.component_list,R.id.component_option_layout));
            }
        }
    }

    @Test public void eventRowReversalAndRebindingKeepActionsVisible() throws Exception {
        withDesign(scenario -> {
            scenario.onActivity(a -> ((androidx.viewpager.widget.ViewPager)a.findViewById(R.id.viewpager)).setCurrentItem(1,false));
            awaitRow(scenario,R.id.event_list);
            scenario.onActivity(a -> ((CollapsibleViewHolder)row(a,R.id.event_list)).expand());
            pause(400);
            scenario.onActivity(a -> {
                CollapsibleViewHolder holder=(CollapsibleViewHolder)row(a,R.id.event_list);
                holder.collapse();
                holder.expand();
                ((RecyclerView)a.findViewById(R.id.event_list)).getAdapter().notifyItemChanged(0);
            });
            pause(400);
            scenario.onActivity(a -> assertExpandedRow(a,R.id.event_list,R.id.event_option_layout));
        });
    }

    @Test public void reselectingLastEventKeepsSelectionPreviewVisible() throws Exception {
        try(ProjectEditorPersistenceTest.Fixture f=new ProjectEditorPersistenceTest.Fixture()) {
            f.seed(false,false);
            ProjectDataManager.reloadProjectFromData(f.id);
            Intent intent=new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(),AddEventActivity.class)
                    .putExtra("sc_id",f.id).putExtra("project_file",new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY,"main"));
            try(ActivityScenario<AddEventActivity> scenario=ActivityScenario.launch(intent)) {
                ready(scenario);
                awaitRow(scenario,R.id.event_list);
                scenario.onActivity(a -> ((CheckBox)row(a,R.id.event_list).itemView.findViewById(R.id.checkbox)).setChecked(true));
                pause(400);
                scenario.onActivity(a -> {
                    CheckBox checkbox=row(a,R.id.event_list).itemView.findViewById(R.id.checkbox);
                    checkbox.setChecked(false);
                    checkbox.setChecked(true);
                });
                pause(400);
                scenario.onActivity(a -> {
                    LogicPopupAddEventBinding b=(LogicPopupAddEventBinding)field(a,"binding");
                    assertFalse((Boolean)field(a,"isPreviewCollapsed"));
                    assertEquals(1,b.eventsPreview.getAdapter().getItemCount());
                    assertEquals(View.VISIBLE,b.eventsPreview.getVisibility());
                    assertTrue("Selected event must have a visible preview",b.eventsPreview.getHeight()>0);
                    assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT,b.eventsPreview.getLayoutParams().height);
                });
            }
        }
    }

    @Test public void expandedContentCanGrowAfterAnimation() throws Exception {
        withDesign(scenario -> {
            LinearLayout[] content={null};
            scenario.onActivity(a -> content[0]=addHeightFixture(a));
            pause(100);
            scenario.onActivity(a -> {
                AnimationUtil.collapseView(content[0],300,null);
                AnimationUtil.expandView(content[0],300,null);
            });
            pause(400);
            int[] height={0};
            scenario.onActivity(a -> {
                height[0]=content[0].getHeight();
                assertTrue(height[0]>0);
                content[0].addView(new View(a),new LinearLayout.LayoutParams(100,55));
            });
            pause(100);
            scenario.onActivity(a -> assertEquals("Expanded content must retain its natural size",height[0]+55,content[0].getHeight()));
        });
    }

    @Test public void detachedContentDoesNotReceiveOldCollapseCompletion() throws Exception {
        withDesign(scenario -> {
            LinearLayout[] content={null};
            int[] completed={0};
            scenario.onActivity(a -> content[0]=addHeightFixture(a));
            pause(100);
            scenario.onActivity(a -> {
                AnimationUtil.collapseView(content[0],300,new AnimatorListenerAdapter() {
                    @Override public void onAnimationEnd(Animator animation) { completed[0]++; }
                });
                ((ViewGroup)content[0].getParent()).removeView(content[0]);
                content[0].getLayoutParams().height=ViewGroup.LayoutParams.WRAP_CONTENT;
                ((ViewGroup)a.findViewById(android.R.id.content)).addView(content[0]);
            });
            pause(400);
            scenario.onActivity(a -> {
                assertEquals("A detached animation must not complete against reused content",0,completed[0]);
                assertEquals(100,content[0].getHeight());
            });
        });
    }

    @Test @SdkSuppress(minSdkVersion=29)
    public void sliderEntryVersusResetAndManualInput() throws Exception {
        withDesign(scenario -> {
            scenario.onActivity(a -> {
                PropertyInputItem item=new PropertyInputItem(a,false);
                item.setKey("property_rotate");
                call(item,"showHybridSliderDialog",new Class<?>[]{String.class,float.class,float.class,float.class,float.class,boolean.class},
                        "Animation regression",90f,-180f,180f,1f,true);
            });
            pause(100);
            scenario.onActivity(a -> {
                View root=dialogRoot();
                ((EditText)root.findViewById(R.id.ed_input)).setText("30");
            });
            pause(900);
            scenario.onActivity(a -> {
                View root=dialogRoot(); Slider slider=root.findViewById(R.id.slider);
                String input=((EditText)root.findViewById(R.id.ed_input)).getText().toString();
                assertEquals("30",input);
                assertEquals("Entry animation must not overwrite manual input",30f,slider.getValue(),0.01f);
                assertEquals("30°",((TextView)root.findViewById(R.id.tv_current_value)).getText().toString());
            });
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
            pause(200);
            scenario.onActivity(a -> {
                PropertyInputItem item=new PropertyInputItem(a,false);item.setKey("property_rotate");
                call(item,"showHybridSliderDialog",new Class<?>[]{String.class,float.class,float.class,float.class,float.class,boolean.class},
                        "Animation regression",90f,-180f,180f,1f,true);
            });
            pause(100);
            scenario.onActivity(a -> dialogRoot().findViewById(android.R.id.button2).performClick());
            pause(1000);
            scenario.onActivity(a -> {
                View root=dialogRoot();Slider slider=root.findViewById(R.id.slider);
                String input=((EditText)root.findViewById(R.id.ed_input)).getText().toString();
                assertEquals("0",input);
                assertEquals("Reset must win over the entry animation",0f,slider.getValue(),0.01f);
            });
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
        });
    }

    @Test @SdkSuppress(minSdkVersion=29)
    public void grabbingCurrentThumbKeepsDisplayedAndSavedValuesEqual() throws Exception {
        withDesign(scenario -> {
            float[] touched = {0};
            java.util.concurrent.CountDownLatch grabbed = new java.util.concurrent.CountDownLatch(1);
            scenario.onActivity(a -> {
                PropertyInputItem item = new PropertyInputItem(a, false);
                item.setKey("property_rotate");
                call(item, "showHybridSliderDialog", new Class<?>[]{String.class, float.class, float.class,
                        float.class, float.class, boolean.class}, "Animation regression", 90f, -180f, 180f, 1f, true);
                View root = dialogRoot();
                root.postDelayed(() -> {
                    Slider slider = root.findViewById(R.id.slider);
                    touched[0] = slider.getValue();
                    assertTrue("Grab the thumb before entry reaches its target", touched[0] < 90);
                    float x = slider.getTrackSidePadding() + slider.getTrackWidth()
                            * (slider.getValue() - slider.getValueFrom()) / (slider.getValueTo() - slider.getValueFrom());
                    float y = slider.getHeight() / 2f;
                    long time = SystemClock.uptimeMillis();
                    for (int action : new int[]{MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP}) {
                        MotionEvent event = MotionEvent.obtain(time, time, action, x, y, 0);
                        event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
                        try {
                            slider.dispatchTouchEvent(event);
                            if (action == MotionEvent.ACTION_DOWN) assertTrue("Touch must grab the thumb", slider.isPressed());
                        } finally {
                            event.recycle();
                        }
                    }
                    grabbed.countDown();
                }, 100);
            });
            assertTrue("Thumb grab did not execute", grabbed.await(5, java.util.concurrent.TimeUnit.SECONDS));
            pause(900);
            scenario.onActivity(a -> {
                View root = dialogRoot();
                assertEquals(touched[0], ((Slider) root.findViewById(R.id.slider)).getValue(), 0.01f);
                assertEquals("Saving reads the same value the slider displays", touched[0],
                        Float.parseFloat(((EditText) root.findViewById(R.id.ed_input)).getText().toString()), 0.01f);
            });
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
        });
    }

    @Test @SdkSuppress(minSdkVersion=29)
    public void touchingSliderStopsEntryAnimation() throws Exception {
        withDesign(scenario -> {
            scenario.onActivity(a -> {
                PropertyInputItem item=new PropertyInputItem(a,false);
                item.setKey("property_rotate");
                call(item,"showHybridSliderDialog",new Class<?>[]{String.class,float.class,float.class,float.class,float.class,boolean.class},
                        "Animation regression",90f,-180f,180f,1f,true);
            });
            pause(100);
            float[] point=new float[2];
            scenario.onActivity(a -> {
                Slider slider=dialogRoot().findViewById(R.id.slider);
                int[] location=new int[2];
                slider.getLocationOnScreen(location);
                point[0]=location[0]+slider.getWidth()*0.6f;
                point[1]=location[1]+slider.getHeight()*0.5f;
            });
            long downTime=SystemClock.uptimeMillis();
            for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}) {
                MotionEvent event=MotionEvent.obtain(downTime,SystemClock.uptimeMillis(),action,point[0],point[1],0);
                try { InstrumentationRegistry.getInstrumentation().sendPointerSync(event); }
                finally { event.recycle(); }
            }
            float[] selected={0};
            scenario.onActivity(a -> {
                selected[0]=((Slider)dialogRoot().findViewById(R.id.slider)).getValue();
                assertTrue("Touch must choose a value distinct from the entry target",Math.abs(selected[0]-90)>5);
            });
            pause(900);
            scenario.onActivity(a -> {
                View root=dialogRoot();
                assertEquals("Entry animation must yield to slider touch",selected[0],((Slider)root.findViewById(R.id.slider)).getValue(),0.01f);
                assertEquals(selected[0],Float.parseFloat(((EditText)root.findViewById(R.id.ed_input)).getText().toString()),0.01f);
            });
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
        });
    }

    @Test public void storeTitleScrollReversal() throws Exception {
        JSONObject json=new JSONObject();
        for(String key:new String[]{"id","title","description","whatsnew","category","project_type","demo_link","video_url",
                "icon","screenshot1","screenshot2","screenshot3","screenshot4","screenshot5","project_size","likes","comments",
                "downloads","uid","timestamp","published_timestamp","is_verified","is_editor_choice","user_name","user_profile_pic","user_badge"}) json.put(key,"");
        json.put("id","animation-regression");json.put("title","Animation regression");json.put("user_name","Fixture");
        json.put("is_verified","0");json.put("is_editor_choice","0");json.put("published_timestamp","0");
        json.put("description","Read-only animation sample\n".repeat(150));
        Intent intent=new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(),ProjectPreviewActivity.class).putExtra("project_json",json.toString());
        try(ActivityScenario<ProjectPreviewActivity> scenario=ActivityScenario.launch(intent)) {
            ready(scenario);
            scenario.onActivity(a -> ((FragmentStoreProjectPreviewBinding)field(a,"binding")).scrollView.scrollTo(0,1000));
            pause(250);
            scenario.onActivity(a -> assertTrue((Boolean)field(a,"isTitleContainerShown")));
            scenario.onActivity(a -> ((FragmentStoreProjectPreviewBinding)field(a,"binding")).scrollView.scrollTo(0,0));
            pause(50);
            scenario.onActivity(a -> {
                FragmentStoreProjectPreviewBinding b=(FragmentStoreProjectPreviewBinding)field(a,"binding");
                float before=b.toolbarTitleContainer.getTranslationY();
                b.scrollView.scrollTo(0,1000);
                float after=b.toolbarTitleContainer.getTranslationY();
                assertEquals("Title must reverse without jumping to its original offset",before,after,1f);
            });
            pause(250);
            scenario.onActivity(a -> {
                FragmentStoreProjectPreviewBinding b=(FragmentStoreProjectPreviewBinding)field(a,"binding");
                assertEquals(0f,b.toolbarTitleContainer.getTranslationY(),1f);
                assertEquals(1f,b.toolbarTitleContainer.getAlpha(),0.01f);
            });
        }
    }

    private static View dialogRoot() {
        for(View v:WindowInspector.getGlobalWindowViews())if(v.findViewById(R.id.slider)!=null)return v;
        throw new AssertionError("Slider dialog missing");
    }
    private static void selectPalette(DesignActivity a,int index) {
        ((ViewGroup)fragment(a).viewEditor.findViewById(R.id.palette_group)).getChildAt(index).performClick();
    }
    private static LinearLayout addHeightFixture(Activity a) {
        LinearLayout content=new LinearLayout(a);
        content.setOrientation(LinearLayout.VERTICAL);
        content.addView(new View(a),new LinearLayout.LayoutParams(100,100));
        ((ViewGroup)a.findViewById(android.R.id.content)).addView(content,
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        return content;
    }
    private static RecyclerView.ViewHolder row(Activity a,int id) {
        RecyclerView.ViewHolder holder=((RecyclerView)a.findViewById(id)).findViewHolderForAdapterPosition(0);
        assertNotNull("First row must be attached",holder);
        assertEquals(0,holder.getLayoutPosition());
        return holder;
    }
    private static void assertExpandedRow(Activity a,int listId,int optionsId) {
        View options=row(a,listId).itemView.findViewById(optionsId);
        assertEquals("Expanded row must show its actions",View.VISIBLE,options.getVisibility());
        assertTrue(options.getHeight()>0);
        assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT,options.getLayoutParams().height);
    }
    private static void awaitRow(ActivityScenario<? extends Activity> scenario,int id) {
        long deadline=SystemClock.uptimeMillis()+10000;
        while(SystemClock.uptimeMillis()<deadline) {
            boolean[] loaded={false};
            scenario.onActivity(a -> {
                RecyclerView list=a.findViewById(id);
                loaded[0]=list!=null && list.findViewHolderForAdapterPosition(0)!=null;
            });
            if(loaded[0])return;
            pause(30);
        }
        fail("List did not load: "+id);
    }
    private static void reverse(ActivityScenario<? extends Activity> scenario,int id,Consumer<Activity> show,
                                Consumer<Activity> hide,boolean horizontal,String name,int openWait,int closeWait) {
        scenario.onActivity(show::accept);pause(openWait);
        scenario.onActivity(hide::accept);pause(closeWait);
        scenario.onActivity(show::accept);pause(openWait);
        scenario.onActivity(hide::accept);pause(100);
        scenario.onActivity(a -> {
            View v=a.findViewById(id);float before=horizontal?v.getTranslationX():v.getTranslationY();
            show.accept(a);float after=horizontal?v.getTranslationX():v.getTranslationY();
            assertEquals(name+" must continue from its current position",before,after,1f);
        });pause(openWait);
        scenario.onActivity(a -> {
            View v=a.findViewById(id);
            assertEquals(name+" must finish at the visible position",0f,horizontal?v.getTranslationX():v.getTranslationY(),1f);
        });
    }
    private static ViewEditorFragment fragment(DesignActivity a) { return (ViewEditorFragment)field(a,"viewTabAdapter"); }
    private static Object field(Object target,String name) {
        for(Class<?> c=target.getClass();c!=null;c=c.getSuperclass())try {
            Field f=c.getDeclaredField(name);f.setAccessible(true);return f.get(target);
        } catch(NoSuchFieldException e) {} catch(Exception e) { throw new AssertionError(e); }
        throw new AssertionError("Missing field "+name+" on "+target.getClass());
    }
    private static void call(Object target,String name,Class<?>[] types,Object... args) {
        try { Method m=target.getClass().getDeclaredMethod(name,types);m.setAccessible(true);m.invoke(target,args); }
        catch(Exception e) { throw new AssertionError(e); }
    }
    private static void ready(ActivityScenario<? extends Activity> scenario) {
        scenario.onActivity(a -> a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));pause(1000);
    }
    private static void pause(int ms) { SystemClock.sleep(ms); }
    private static void withDesign(DesignCheck check) throws Exception {
        try(ProjectEditorPersistenceTest.Fixture f=new ProjectEditorPersistenceTest.Fixture()) {
            f.seed(false,false);
            try(ActivityScenario<DesignActivity> scenario=f.launch()) {
                ready(scenario);ProjectEditorPersistenceTest.waitForEditor(scenario);
                check.run(scenario);
            }
        }
    }
    private interface DesignCheck { void run(ActivityScenario<DesignActivity> scenario) throws Exception; }
}
