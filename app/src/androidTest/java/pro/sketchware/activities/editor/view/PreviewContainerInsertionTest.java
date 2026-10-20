package pro.sketchware.activities.editor.view;

import static org.junit.Assert.*;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import pro.sketchware.activities.base.PermissionResultTestActivity;
import pro.sketchware.activities.editor.view.item.ItemCardView;
import pro.sketchware.activities.editor.view.item.ItemHorizontalScrollView;
import pro.sketchware.activities.editor.view.item.ItemImageView;
import pro.sketchware.activities.editor.view.item.ItemLinearLayout;
import pro.sketchware.activities.editor.view.item.ItemRelativeLayout;
import pro.sketchware.activities.editor.view.item.ItemVerticalScrollView;
import pro.sketchware.beans.ViewBean;

@RunWith(AndroidJUnit4.class)
public class PreviewContainerInsertionTest {
    @Test(timeout = 30000)
    public void insertingAtChildCountAppendsWhenChildrenAreGone() {
        onHost(context -> {
            for (int type = 0; type < 5; type++) {
                for (int hidden : new int[]{0, 1, 2}) {
                    ViewGroup parent = seeded(context, type);
                    parent.getChildAt(hidden).setVisibility(View.GONE);
                    View added = child(context, "added");
                    parent.addView(added, parent.getChildCount());
                    assertOrder(parent, "first", "second", "third", "added");
                    assertEquals(View.GONE, parent.getChildAt(hidden).getVisibility());
                    assertSame(parent, added.getParent());
                }
            }
        });
    }

    @Test(timeout = 30000)
    public void outOfRangeAndImplicitAppendPreserveExistingChildren() {
        onHost(context -> {
            for (int type = 0; type < 5; type++) {
                for (int index : new int[]{Integer.MIN_VALUE, -2, -1, 4, Integer.MAX_VALUE}) {
                    ViewGroup parent = seeded(context, type);
                    parent.getChildAt(0).setVisibility(View.GONE);
                    parent.getChildAt(1).setVisibility(View.INVISIBLE);
                    parent.addView(child(context, "added"), index);
                    parent.addView(child(context, "implicit"));
                    assertOrder(parent, "first", "second", "third", "added", "implicit");
                }
            }
        });
    }

    @Test(timeout = 30000)
    public void cardAndRelativeContainersKeepActualChildIndices() {
        onHost(context -> {
            for (int type : new int[]{3, 4}) {
                ViewGroup parent = seeded(context, type);
                parent.getChildAt(0).setVisibility(View.GONE);
                parent.addView(child(context, "added"), 1);
                assertOrder(parent, "first", "added", "second", "third");
                ((ScrollContainer) parent).reindexChildren();
                assertBeanIndices(parent);
            }
        });
    }

    @Test(timeout = 30000)
    public void legacyLinearAndScrollOverloadsKeepTheirInteriorGoneOffset() {
        onHost(context -> {
            for (int type : new int[]{0, 1, 2}) {
                ViewGroup parent = seeded(context, type);
                parent.getChildAt(0).setVisibility(View.GONE);
                parent.addView(child(context, "added"), 1);
                assertOrder(parent, "first", "second", "added", "third");
            }
        });
    }

    @Test(timeout = 30000)
    public void explicitLayoutParamsKeepActualIndicesWithMultipleHiddenChildren() {
        onHost(context -> {
            for (int type = 0; type < 5; type++) {
                ViewGroup parent = seeded(context, type);
                parent.getChildAt(0).setVisibility(View.GONE);
                parent.getChildAt(1).setVisibility(View.GONE);
                parent.getChildAt(2).setVisibility(View.INVISIBLE);
                View added = child(context, "added");
                parent.addView(added, 2, new ViewGroup.LayoutParams(17, 29));
                assertOrder(parent, "first", "second", "added", "third");
                assertEquals(17, added.getLayoutParams().width);
                assertEquals(29, added.getLayoutParams().height);
                ((ScrollContainer) parent).reindexChildren();
                assertBeanIndices(parent);
            }
        });
    }

    @Test(timeout = 30000)
    public void emptyAndFullyHiddenContainersStillAppendAndReindex() {
        onHost(context -> {
            for (int type = 0; type < 5; type++) {
                for (int index : new int[]{-1, 0, Integer.MAX_VALUE}) {
                    ViewGroup parent = container(context, type);
                    parent.addView(child(context, "first"), index);
                    parent.getChildAt(0).setVisibility(View.GONE);
                    parent.addView(child(context, "second"), parent.getChildCount());
                    parent.getChildAt(1).setVisibility(View.GONE);
                    parent.addView(child(context, "third"), parent.getChildCount());
                    assertOrder(parent, "first", "second", "third");
                    ((ScrollContainer) parent).reindexChildren();
                    assertBeanIndices(parent);
                }
            }
        });
    }

    private static ViewGroup seeded(Context context, int type) {
        ViewGroup parent = container(context, type);
        for (String name : new String[]{"first", "second", "third"}) {
            parent.addView(child(context, name), parent.getChildCount(), new ViewGroup.LayoutParams(10, 10));
        }
        return parent;
    }

    private static ViewGroup container(Context context, int type) {
        return switch (type) {
            case 0 -> new ItemLinearLayout(context);
            case 1 -> new ItemHorizontalScrollView(context);
            case 2 -> new ItemVerticalScrollView(context);
            case 3 -> new ItemCardView(context);
            case 4 -> new ItemRelativeLayout(context);
            default -> throw new AssertionError(type);
        };
    }

    private static View child(Context context, String name) {
        ItemImageView child = new ItemImageView(context);
        ViewBean bean = new ViewBean(name, ViewBean.VIEW_TYPE_WIDGET_IMAGEVIEW);
        child.setBean(bean);
        child.setTag(name);
        return child;
    }

    private static void assertOrder(ViewGroup parent, String... names) {
        List<String> actual = new ArrayList<>();
        for (int i = 0; i < parent.getChildCount(); i++) actual.add((String) parent.getChildAt(i).getTag());
        assertEquals(parent.getClass().getSimpleName(), Arrays.asList(names), actual);
    }

    private static void assertBeanIndices(ViewGroup parent) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            assertEquals(i, ((ItemView) parent.getChildAt(i)).getBean().index);
        }
    }

    private static void onHost(Consumer<Context> action) {
        try (ActivityScenario<PermissionResultTestActivity> scenario = ActivityScenario.launch(PermissionResultTestActivity.class)) {
            scenario.onActivity(activity -> {
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                action.accept(activity);
            });
        }
    }
}
