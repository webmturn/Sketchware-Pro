package com.besome.sketch.editor.view;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.appcompat.content.res.AppCompatResources;

import mod.hey.studios.util.Helper;
import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.beans.ProjectResourceBean;
import com.besome.sketch.beans.ViewBean;
import com.besome.sketch.beans.WidgetCollectionBean;
import com.besome.sketch.editor.view.item.ItemHorizontalScrollView;
import com.besome.sketch.editor.view.item.ItemVerticalScrollView;
import com.besome.sketch.editor.view.palette.IconAdView;
import com.besome.sketch.editor.view.palette.IconBase;
import com.besome.sketch.editor.view.palette.IconLinearHorizontal;
import com.besome.sketch.editor.view.palette.IconLinearVertical;
import com.besome.sketch.editor.view.palette.IconMapView;
import com.besome.sketch.editor.view.palette.PaletteFavorite;
import com.besome.sketch.editor.view.palette.PaletteWidget;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import pro.sketchware.core.SharedPrefsHelper;
import pro.sketchware.core.DeviceUtil;
import pro.sketchware.core.ViewEditorCallback;
import pro.sketchware.core.ImageCollectionManager;
import pro.sketchware.core.WidgetCollectionManager;
import pro.sketchware.core.SimpleCallback;
import pro.sketchware.core.SketchToast;
import pro.sketchware.core.ViewHistoryManager;
import pro.sketchware.core.BuildCallback;
import pro.sketchware.core.ProjectDataManager;
import pro.sketchware.core.EncryptedFileUtil;
import pro.sketchware.core.WidgetPaletteIcon;
import pro.sketchware.core.ViewUtil;
import pro.sketchware.core.SketchwarePaths;
import mod.agus.jcoderz.beans.ViewBeans;
import mod.hey.studios.util.ProjectFile;
import mod.jbk.util.LogUtil;
import pro.sketchware.R;
import pro.sketchware.utility.ThemeUtils;
import pro.sketchware.utility.UI;
import pro.sketchware.widgets.IconCustomWidget;
import pro.sketchware.widgets.WidgetsCreatorManager;

@SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
public class ViewEditor extends RelativeLayout implements View.OnClickListener, View.OnTouchListener {
    private final int[] posDummy = new int[2];
    private final Handler handler = new Handler(Looper.getMainLooper());
    public boolean isLayoutChanged = true;
    public PaletteWidget paletteWidget;
    public WidgetsCreatorManager widgetsCreatorManager;
    private ObjectAnimator animatorTranslateX;
    private boolean isAnimating = false;
    private boolean isPaletteVisible = false;
    private boolean isDeleteActive = false;
    private ItemView selectedItem;
    private int defaultIconWidth = 50;
    private int defaultIconHeight = 30;
    private boolean useVibrate;
    private BuildCallback widgetSelectedListener;
    private ViewEditorCallback propertyClickListener;
    private DraggingListener draggingListener;
    private SimpleCallback historyChangeListener;
    private ProjectFileBean projectFileBean;
    private boolean hasToolbar = true;
    private boolean isFullscreen = false;
    private LinearLayout paletteGroup;
    private String scId;
    private LinearLayout screenContainer;
    private String xmlName;
    private int screenType;
    private boolean isAdLoaded = true;
    private int[] countItems = new int[99];
    private float dip = 0;
    private int displayWidth;
    private int displayHeight;
    private PaletteFavorite paletteFavorite;
    private LinearLayout bgStatus;
    private TextView fileName;
    private ImageView imgPhoneTopBg;
    private LinearLayout toolbar;
    private ViewPane viewPane;
    private Vibrator vibrator;
    private View currentTouchedView = null;
    private boolean isDragged = false;
    private float posInitX = 0;
    private float posInitY = 0;
    private int minDist = 0;
    private ViewDummy dummyView;
    private ImageView deleteIcon;
    private TextView deleteText;
    private MaterialCardView deleteView;
    private ObjectAnimator animatorTranslateY;
    private int colorSurfaceContainerHighest;
    private int colorCoolGreenContainer;
    private int colorCoolGreen;
    private int colorError;
    private final Runnable longPressRunnable = this::handleDragStart;
    private int colorErrorContainer;

    public ViewEditor(Context context) {
        this(context, null);
    }

    public ViewEditor(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public static void shakeView(View view) {
        ObjectAnimator
                .ofFloat(view, "translationX", 0, 35, -35, 35, -35, 25, -25, 12, -12, 0)
                .setDuration(200)
                .start();
    }

    private void animateUpDown() {
        animatorTranslateY = ObjectAnimator.ofFloat(deleteView, "TranslationY", 0.0f);
        animatorTranslateY.setDuration(500L);
        animatorTranslateY.setInterpolator(new OvershootInterpolator());
        animatorTranslateX = ObjectAnimator.ofFloat(deleteView, "TranslationY", deleteView.getHeight() * 2);
        animatorTranslateX.setDuration(500L);
        animatorTranslateX.setInterpolator(new OvershootInterpolator());
        isAnimating = true;
    }

    private void addPaletteGroupItems() {
        LinearLayout.LayoutParams paletteLayoutParams =
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT);
        paletteLayoutParams.weight = 1f;

        PaletteGroupItem basicPalette = new PaletteGroupItem(getContext());
        basicPalette.setLayoutParams(paletteLayoutParams);
        basicPalette.setPaletteGroup(PaletteGroup.BASIC);
        basicPalette.setSelected(true);

        PaletteGroupItem favoritePalette = new PaletteGroupItem(getContext());
        favoritePalette.setLayoutParams(paletteLayoutParams);
        favoritePalette.setPaletteGroup(PaletteGroup.FAVORITE);
        favoritePalette.setSelected(false);
        favoritePalette.animate().scaleX(0.9f).scaleY(0.9f).alpha(0.6f).start();

        basicPalette.setOnClickListener(v -> {
            showPaletteWidget();
            basicPalette.animate().scaleX(1).scaleY(1).alpha(1).start();
            favoritePalette.animate().scaleX(0.9f).scaleY(0.9f).alpha(0.6f).start();
            basicPalette.setSelected(true);
            favoritePalette.setSelected(false);
        });

        favoritePalette.setOnClickListener(v -> {
            showPaletteFavorite();
            basicPalette.animate().scaleX(0.9f).scaleY(0.9f).alpha(0.6f).start();
            favoritePalette.animate().scaleX(1).scaleY(1).alpha(1).start();
            basicPalette.setSelected(false);
            favoritePalette.setSelected(true);
        });

        paletteGroup.addView(basicPalette);
        paletteGroup.addView(favoritePalette);
    }

    public ProjectFileBean getProjectFile() {
        return projectFileBean;
    }

    public void refreshResourceManager() {
        viewPane.setResourceManager(ProjectDataManager.getResourceManager(scId));
    }

    public void clearSelection() {
        if (selectedItem != null) {
            selectedItem.setSelection(false);
            selectedItem = null;
        }
        if (widgetSelectedListener != null) widgetSelectedListener.onViewSelectedWithProperty(false, "");
    }

    public void resetViewPane() {
        viewPane.updateRootLayout(scId, projectFileBean.getXmlName());
        viewPane.clearViewPane();
        resetItemCounters();
        clearSelection();
    }

    public void removeFab() {
        viewPane.removeFabView();
    }

    public void resetItemCounters() {
        countItems = new int[99];
    }

    private void showMoreProperties() {
        if (propertyClickListener != null) propertyClickListener.onPropertyRequested(xmlName, selectedItem.getBean());
    }

    private void showPaletteFavorite() {
        paletteWidget.animate()
                .alpha(0f)
                .setDuration(100)
                .withEndAction(() -> {
                    paletteWidget.setVisibility(View.GONE);
                    paletteFavorite.setAlpha(0f);
                    paletteFavorite.setVisibility(View.VISIBLE);
                    paletteFavorite.animate()
                            .alpha(1f)
                            .setDuration(100)
                            .start();
                })
                .start();
    }

    private void showPaletteWidget() {
        paletteFavorite.animate()
                .alpha(0f)
                .setDuration(100)
                .withEndAction(() -> {
                    paletteFavorite.setVisibility(View.GONE);
                    paletteWidget.setAlpha(0f);
                    paletteWidget.setVisibility(View.VISIBLE);
                    paletteWidget.animate()
                            .alpha(1f)
                            .setDuration(100)
                            .start();
                })
                .start();
    }


    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.btn_editproperties) {
            showMoreProperties();
        }
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (isLayoutChanged) updateLayoutPreview();
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        String parentId;
        int actionMasked = motionEvent.getActionMasked();
        if (motionEvent.getPointerId(motionEvent.getActionIndex()) > 0) {
            return true;
        }
        if (view == viewPane) {
            if (actionMasked == MotionEvent.ACTION_DOWN) {
                clearSelection();
                currentTouchedView = null;
            }
            return true;
        } else if (actionMasked == MotionEvent.ACTION_DOWN) {
            isDragged = false;
            posInitX = motionEvent.getRawX();
            posInitY = motionEvent.getRawY();
            currentTouchedView = view;
            if (view instanceof ItemView bean && bean.getFixed()) {
                return true;
            }
            if (isInsideItemScrollView(view) && draggingListener != null) {
                draggingListener.onDragStarted();
            }
            handler.postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout() / 2);
            return true;
        } else if (actionMasked != MotionEvent.ACTION_UP) {
            if (actionMasked != MotionEvent.ACTION_MOVE) {
                if (actionMasked == MotionEvent.ACTION_CANCEL || actionMasked == MotionEvent.ACTION_SCROLL) {
                    paletteWidget.setScrollEnabled(true);
                    paletteFavorite.setScrollEnabled(true);
                    if (draggingListener != null) {
                        draggingListener.onDragEnded();
                    }
                    showDeleteView(false, false);
                    dummyView.setDummyVisibility(View.GONE);
                    viewPane.clearViews();
                    handler.removeCallbacks(longPressRunnable);
                    isDragged = false;
                    return true;
                }
                return true;
            } else if (!isDragged) {
                if (Math.abs(posInitX - motionEvent.getRawX()) >= minDist || Math.abs(posInitY - motionEvent.getRawY()) >= minDist) {
                    currentTouchedView = null;
                    handler.removeCallbacks(longPressRunnable);
                    return true;
                }
                return true;
            } else {
                handler.removeCallbacks(longPressRunnable);
                dummyView.updateDummyPosition(view, motionEvent.getRawX(), motionEvent.getRawY(), posInitX, posInitY);
                if (hitTestIconDelete(motionEvent.getRawX(), motionEvent.getRawY())) {
                    dummyView.setAllow(true);
                    updateDeleteIcon(true, currentTouchedView instanceof IconCustomWidget);
                    return true;
                }
                if (isDeleteActive) updateDeleteIcon(false, currentTouchedView instanceof IconCustomWidget);
                if (hitTestToPane(motionEvent.getRawX(), motionEvent.getRawY())) {
                    dummyView.setAllow(true);
                    boolean isNotIcon = !isViewAnIconBase(currentTouchedView);
                    int width = isNotIcon ? currentTouchedView.getWidth() : currentTouchedView instanceof IconLinearHorizontal ?
                            ViewGroup.LayoutParams.MATCH_PARENT : defaultIconWidth;
                    int height = isNotIcon ? currentTouchedView.getHeight() : currentTouchedView instanceof IconLinearVertical ?
                            ViewGroup.LayoutParams.MATCH_PARENT : defaultIconHeight;
                    viewPane.updateView((int) motionEvent.getRawX(), (int) motionEvent.getRawY(), width, height);
                } else {
                    dummyView.setAllow(false);
                    viewPane.resetView(true);
                }
                return true;
            }
        } else if (!isDragged) {
            if (currentTouchedView instanceof ItemView sy) {
                setSelectedItem(sy, true);
            }
            if (draggingListener != null) {
                draggingListener.onDragEnded();
            }
            dummyView.setDummyVisibility(View.GONE);
            currentTouchedView = null;
            viewPane.clearViews();
            handler.removeCallbacks(longPressRunnable);
            return true;
        } else {
            lol:
            if (dummyView.getAllow()) {
                if (isDeleteActive && currentTouchedView instanceof ItemView widget) {
                    deleteWidget(widget.getBean());
                    break lol;
                }
                if (isDeleteActive && currentTouchedView instanceof WidgetPaletteIcon collectionWidget) {
                    deleteWidgetFromCollection(collectionWidget.getName());
                    break lol;
                }
                if (isDeleteActive && currentTouchedView instanceof IconCustomWidget) {
                    widgetsCreatorManager.showActionsDialog((int) view.getTag());
                    break lol;
                }
                viewPane.resetView(false);
                if (currentTouchedView instanceof WidgetPaletteIcon uyVar) {
                    ArrayList<ViewBean> widgetViews = new ArrayList<>();
                    EncryptedFileUtil fileUtil = new EncryptedFileUtil();
                    boolean areImagesAdded = false;
                    for (int i3 = 0; i3 < uyVar.getData().size(); i3++) {
                        ViewBean viewBean = uyVar.getData().get(i3);
                        widgetViews.add(viewBean.clone());
                        String backgroundResource = viewBean.layout.backgroundResource;
                        String resName = viewBean.image.resName;
                        if (!ProjectDataManager.getResourceManager(scId).hasImage(backgroundResource) && ImageCollectionManager.getInstance().hasResource(backgroundResource)) {
                            ProjectResourceBean a2 = ImageCollectionManager.getInstance().getResourceByName(backgroundResource);
                            try {
                                fileUtil.copyFile(SketchwarePaths.getCollectionPath() + File.separator + "image" + File.separator + "data" + File.separator + a2.resFullName, SketchwarePaths.getImagesPath() + File.separator + scId + File.separator + a2.resFullName);
                            } catch (Exception e) {
                                LogUtil.e("ViewEditor", "", e);
                            }
                            ProjectDataManager.getResourceManager(scId).images.add(a2);
                            areImagesAdded = true;
                        }
                        if (!ProjectDataManager.getResourceManager(scId).hasImage(resName) && ImageCollectionManager.getInstance().hasResource(resName)) {
                            ProjectResourceBean a3 = ImageCollectionManager.getInstance().getResourceByName(resName);
                            try {
                                fileUtil.copyFile(SketchwarePaths.getCollectionPath() + File.separator + "image" + File.separator + "data" + File.separator + a3.resFullName, SketchwarePaths.getImagesPath() + File.separator + scId + File.separator + a3.resFullName);
                            } catch (Exception e2) {
                                LogUtil.e("ViewEditor", "", e2);
                            }
                            ProjectDataManager.getResourceManager(scId).images.add(a3);
                            areImagesAdded = true;
                        }
                    }
                    if (areImagesAdded) {
                        SketchToast.toast(getContext(), Helper.getResString(R.string.view_widget_favorites_image_auto_added), SketchToast.TOAST_NORMAL).show();
                    }
                    if (!widgetViews.isEmpty()) {
                        HashMap<String, String> idMappings = new HashMap<>();
                        viewPane.updateViewBeanProperties(widgetViews.get(0), (int) motionEvent.getRawX(), (int) motionEvent.getRawY());
                        for (ViewBean next : widgetViews) {
                            if (ProjectDataManager.getProjectDataManager(scId).hasView(projectFileBean.getXmlName(), next.id)) {
                                idMappings.put(next.id, generateWidgetId(next));
                            } else {
                                idMappings.put(next.id, next.id);
                            }
                            next.id = idMappings.get(next.id);
                            if (widgetViews.indexOf(next) != 0 && (parentId = next.parent) != null && !parentId.isEmpty()) {
                                next.parent = idMappings.get(next.parent);
                            }
                            ProjectDataManager.getProjectDataManager(scId).addView(xmlName, next);
                        }
                        setSelectedItem(addViews(widgetViews, true), true);
                    }
                } else if (currentTouchedView instanceof IconBase icon) {
                    ViewBean bean = icon.getBean();
                    bean.id = generateWidgetId(bean);
                    viewPane.updateViewBeanProperties(bean, (int) motionEvent.getRawX(), (int) motionEvent.getRawY());
                    ProjectDataManager.getProjectDataManager(scId).addView(xmlName, bean);
                    if (bean.type == 3 && projectFileBean.fileType == ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY) {
                        ProjectDataManager.getProjectDataManager(scId).addEvent(projectFileBean.getJavaName(), 1, bean.type, bean.id, "onClick");
                    }
                    setSelectedItem(addView(bean, true), true);
                } else if (currentTouchedView instanceof ItemView sy) {
                    ViewBean bean = sy.getBean();
                    viewPane.updateViewBeanProperties(bean, (int) motionEvent.getRawX(), (int) motionEvent.getRawY());
                    setSelectedItem(moveView(bean, true), true);
                }
            } else {
                if (currentTouchedView instanceof ItemView) {
                    currentTouchedView.setVisibility(View.VISIBLE);
                }
            }
            paletteWidget.setScrollEnabled(true);
            paletteFavorite.setScrollEnabled(true);
            if (draggingListener != null) {
                draggingListener.onDragEnded();
            }
            showDeleteView(false, false);
            dummyView.setDummyVisibility(View.GONE);
            currentTouchedView = null;
            viewPane.clearViews();
            handler.removeCallbacks(longPressRunnable);
            isDragged = false;
            return true;
        }
    }

    public void deleteWidget(ViewBean viewBean) {
        ArrayList<ViewBean> b2 = ProjectDataManager.getProjectDataManager(scId).getViewWithChildren(xmlName, viewBean);
        for (int size = b2.size() - 1; size >= 0; size--) {
            ProjectDataManager.getProjectDataManager(scId).removeView(projectFileBean, b2.get(size));
        }
        removeViews(b2, true);
    }

    public void setFavoriteData(ArrayList<WidgetCollectionBean> collections) {
        clearCollectionWidget();
        for (WidgetCollectionBean next : collections) {
            addFavoriteViews(next.name, next.widgets);
        }
    }

    public void setIsAdLoaded(boolean loaded) {
        isAdLoaded = loaded;
    }

    public void setOnDraggingListener(DraggingListener dragListener) {
        draggingListener = dragListener;
    }

    public void setOnHistoryChangeListener(SimpleCallback ayVar) {
        historyChangeListener = ayVar;
    }

    public void setOnPropertyClickListener(ViewEditorCallback viewEditorCallback) {
        propertyClickListener = viewEditorCallback;
    }

    public void setOnWidgetSelectedListener(BuildCallback cyVar) {
        widgetSelectedListener = cyVar;
    }

    public void setPaletteLayoutVisible(int i) {
        paletteWidget.setLayoutVisible(i);
    }

    public void setScreenType(int i) {
        if (i == 1) {
            screenType = 0;
        } else {
            screenType = 1;
        }
    }

    private void initialize(Context context) {
        ViewUtil.inflateLayoutInto(context, this, R.layout.view_editor);

        paletteWidget = findViewById(R.id.palette_widget);
        paletteFavorite = findViewById(R.id.palette_favorite);
        dummyView = findViewById(R.id.dummy);
        deleteIcon = findViewById(R.id.icon_delete);
        deleteText = findViewById(R.id.text_delete);
        deleteView = findViewById(R.id.delete_view);
        FrameLayout shape = findViewById(R.id.shape);
        paletteGroup = findViewById(R.id.palette_group);

        addPaletteGroupItems();

        findViewById(R.id.btn_editproperties).setOnClickListener(this);
        findViewById(R.id.img_close).setOnClickListener(this);

        dip = ViewUtil.dpToPx(context, 1.0f);
        defaultIconWidth = (int) (defaultIconWidth * dip);
        defaultIconHeight = (int) (defaultIconHeight * dip);
        displayWidth = getResources().getDisplayMetrics().widthPixels;
        displayHeight = getResources().getDisplayMetrics().heightPixels;

        screenContainer = new LinearLayout(context);
        screenContainer.setOrientation(LinearLayout.VERTICAL);
        screenContainer.setGravity(Gravity.CENTER);
        screenContainer.setLayoutParams(new FrameLayout.LayoutParams(displayWidth, displayHeight));
        shape.addView(screenContainer);

        bgStatus = new LinearLayout(context);
        bgStatus.setBackgroundColor(0xff0084c2);
        bgStatus.setOrientation(LinearLayout.HORIZONTAL);
        bgStatus.setGravity(Gravity.CENTER_VERTICAL);
        bgStatus.setLayoutParams(new FrameLayout.LayoutParams(displayWidth, (int) (dip * 25f)));

        fileName = new TextView(context);
        fileName.setTextColor(Color.WHITE);
        fileName.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        fileName.setPadding((int) (dip * 8f), 0, 0, 0);
        fileName.setGravity(Gravity.CENTER_VERTICAL);
        bgStatus.addView(fileName);

        imgPhoneTopBg = new ImageView(context);
        imgPhoneTopBg.setImageResource(R.drawable.phone_bg_top);
        imgPhoneTopBg.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        imgPhoneTopBg.setScaleType(ImageView.ScaleType.FIT_END);
        bgStatus.addView(imgPhoneTopBg);
        shape.addView(bgStatus);

        toolbar = new LinearLayout(context);
        toolbar.setBackgroundColor(0xff008dcd);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setLayoutParams(new FrameLayout.LayoutParams(displayWidth, (int) (dip * 48f)));

        TextView tvToolbar = new TextView(context);
        tvToolbar.setTextColor(Color.WHITE);
        tvToolbar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        tvToolbar.setPadding((int) (dip * 16f), 0, 0, 0);
        tvToolbar.setGravity(Gravity.CENTER_VERTICAL);
        tvToolbar.setTextSize(15f);
        tvToolbar.setText("Toolbar");
        tvToolbar.setTypeface(null, Typeface.BOLD);
        toolbar.addView(tvToolbar);
        shape.addView(toolbar);

        viewPane = new ViewPane(getContext());
        viewPane.setLayoutParams(new FrameLayout.LayoutParams(displayWidth, displayHeight));
        viewPane.setOnTouchListener(this);
        shape.addView(viewPane);

        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        useVibrate = new SharedPrefsHelper(context, "P12").getBoolean("P12I0", true);
        minDist = ViewConfiguration.get(context).getScaledTouchSlop();

        paletteWidget.cardView.setOnClickListener(view -> widgetsCreatorManager.showWidgetsCreatorDialog(-1));

        colorSurfaceContainerHighest = ThemeUtils.getColor(deleteView, R.attr.colorSurfaceContainerHighest);
        colorCoolGreenContainer = ThemeUtils.getColor(deleteView, R.attr.colorCoolGreenContainer);
        colorCoolGreen = ThemeUtils.getColor(deleteView, R.attr.colorCoolGreen);
        colorErrorContainer = ThemeUtils.getColor(deleteView, R.attr.colorErrorContainer);
        colorError = ThemeUtils.getColor(deleteView, R.attr.colorOnErrorContainer);

        initialDeleteViewUi();
    }

    public void removeViews(ArrayList<ViewBean> views, boolean recordHistory) {
        if (recordHistory) {
            ViewHistoryManager.getInstance(scId).recordRemove(projectFileBean.getXmlName(), views);
            if (historyChangeListener != null) {
                historyChangeListener.onCallback();
            }
        }
        int size = views.size();
        while (true) {
            size--;
            if (size < 0) {
                return;
            }
            removeView(views.get(size));
        }
    }

    private void clearCollectionWidget() {
        paletteFavorite.removeAllWidgets();
    }

    public void removeWidgetsAndLayouts() {
        paletteWidget.removeWidgetLayouts();
        paletteWidget.removeWidgets();
    }

    public ItemView selectView(ViewBean viewBean) {
        ItemView g = viewPane.updateView(viewBean);
        widgetSelectedListener.onSelectionChanged();
        widgetSelectedListener.onViewSelected(viewBean.id);
        return g;
    }

    public void removeView(ViewBean viewBean) {
        viewPane.removeView(viewBean);
    }

    private void handleDragStart() {
        if (currentTouchedView == null) return;
        if (isViewAnIconBase(currentTouchedView)) {
            boolean isAppCompatEnabled = ProjectDataManager.getLibraryManager(scId).getCompat().isEnabled();
            if (currentTouchedView instanceof WidgetPaletteIcon collectionWidget) {
                var collectionData = collectionWidget.getData();
                boolean isAdViewUsed = false;
                for (ViewBean view : collectionData) {
                    if (view.type == ViewBean.VIEW_TYPE_WIDGET_ADVIEW) {
                        isAdViewUsed = true;
                        break;
                    }
                }
                if (isAdViewUsed && !draggingListener.isAdmobEnabled()) {
                    SketchToast.warning(getContext(), Helper.getResString(R.string.design_library_guide_setup_first), SketchToast.TOAST_NORMAL).show();
                    return;
                }

                boolean isMapViewUsed = false;
                for (ViewBean view : collectionData) {
                    if (view.type == ViewBean.VIEW_TYPE_WIDGET_MAPVIEW) {
                        isMapViewUsed = true;
                        break;
                    }
                }
                if (isMapViewUsed && !draggingListener.isGoogleMapEnabled()) {
                    SketchToast.warning(getContext(), Helper.getResString(R.string.design_library_guide_setup_first), SketchToast.TOAST_NORMAL).show();
                    return;
                }
                boolean isAppCompatViewUsed = false;
                for (ViewBean view : collectionData) {
                    switch (view.type) {
                        case ViewBeans.VIEW_TYPE_WIDGET_MATERIALBUTTON,
                             ViewBeans.VIEW_TYPE_WIDGET_RECYCLERVIEW,
                             ViewBeans.VIEW_TYPE_LAYOUT_BOTTOMNAVIGATIONVIEW,
                             ViewBeans.VIEW_TYPE_LAYOUT_TABLAYOUT,
                             ViewBeans.VIEW_TYPE_LAYOUT_VIEWPAGER,
                             ViewBeans.VIEW_TYPE_LAYOUT_COLLAPSINGTOOLBARLAYOUT,
                             ViewBeans.VIEW_TYPE_LAYOUT_TEXTINPUTLAYOUT,
                             ViewBeans.VIEW_TYPE_LAYOUT_SWIPEREFRESHLAYOUT,
                             ViewBeans.VIEW_TYPE_LAYOUT_CARDVIEW -> isAppCompatViewUsed = true;
                    }
                    if (isAppCompatViewUsed) {
                        break;
                    }
                }

                if (isAppCompatViewUsed && !isAppCompatEnabled) {
                    SketchToast.warning(getContext(), Helper.getResString(R.string.design_library_guide_setup_first), SketchToast.TOAST_NORMAL).show();
                    return;
                }
            } else if (currentTouchedView instanceof IconAdView && !draggingListener.isAdmobEnabled()) {
                SketchToast.warning(getContext(), Helper.getResString(R.string.design_library_guide_setup_first), SketchToast.TOAST_NORMAL).show();
                return;
            } else if (currentTouchedView instanceof IconMapView && !draggingListener.isGoogleMapEnabled()) {
                SketchToast.warning(getContext(), Helper.getResString(R.string.design_library_guide_setup_first), SketchToast.TOAST_NORMAL).show();
                return;
            } else if (currentTouchedView instanceof AndroidxOrMaterialView && !isAppCompatEnabled) {
                SketchToast.warning(getContext(), Helper.getResString(R.string.design_library_guide_setup_first), SketchToast.TOAST_NORMAL).show();
                return;
            }
        }
        paletteWidget.setScrollEnabled(false);
        paletteFavorite.setScrollEnabled(false);
        if (draggingListener != null) draggingListener.onDragStarted();
        if (useVibrate) vibrator.vibrate(100L);
        isDragged = true;
        dummyView.captureViewBitmap(currentTouchedView);
        dummyView.bringToFront();
        clearSelection();
        dummyView.updateDummyPosition(currentTouchedView, posInitX, posInitY, posInitX, posInitY);
        dummyView.getDummyLocation(posDummy);
        if (isViewAnIconBase(currentTouchedView)) {
            if (currentTouchedView instanceof WidgetPaletteIcon || currentTouchedView instanceof IconCustomWidget) {
                showDeleteView(true, currentTouchedView instanceof IconCustomWidget);
                viewPane.addRootLayout(null);
            } else {
                showDeleteView(false, false);
                viewPane.addRootLayout(null);
            }
        } else {
            currentTouchedView.setVisibility(View.GONE);
            showDeleteView(true, currentTouchedView instanceof IconCustomWidget);
            viewPane.addRootLayout(((ItemView) currentTouchedView).getBean());
        }
        if (hitTestToPane(posInitX, posInitY)) {
            dummyView.setAllow(true);
            boolean isNotIcon = !isViewAnIconBase(currentTouchedView);
            int width = isNotIcon ? currentTouchedView.getWidth() : currentTouchedView instanceof IconLinearHorizontal ?
                    ViewGroup.LayoutParams.MATCH_PARENT : defaultIconWidth;
            int height = isNotIcon ? currentTouchedView.getHeight() : currentTouchedView instanceof IconLinearVertical ?
                    ViewGroup.LayoutParams.MATCH_PARENT : defaultIconHeight;
            viewPane.updateView((int) posInitX, (int) posInitY, width, height);
            return;
        }
        dummyView.setAllow(false);
        viewPane.resetView(true);
    }

    public ItemView moveView(ViewBean viewBean, boolean recordHistory) {
        if (recordHistory) {
            ViewHistoryManager.getInstance(scId).recordMove(projectFileBean.getXmlName(), viewBean);
            if (historyChangeListener != null) {
                historyChangeListener.onCallback();
            }
        }
        return viewPane.moveView(viewBean);
    }

    public ItemView createAndAddView(ViewBean viewBean) {
        View itemView = viewPane.createItemView(viewBean);
        viewPane.addViewAndUpdateIndex(itemView);
        String generatedId = SketchwarePaths.getWidgetTypeName(viewBean.type);
        if (viewBean.id.indexOf(generatedId) == 0 && viewBean.id.length() > generatedId.length()) {
            try {
                int intValue = Integer.parseInt(viewBean.id.substring(generatedId.length()));
                if (countItems[viewBean.type] < intValue) {
                    countItems[viewBean.type] = intValue;
                }
            } catch (NumberFormatException e) {
                android.util.Log.d("ViewEditor", "Failed to parse numeric suffix for view ID: " + viewBean.id, e);
            }
        }
        itemView.setOnTouchListener(this);
        return (ItemView) itemView;
    }

    private boolean isInsideItemScrollView(View view) {
        for (ViewParent parent = view.getParent(); parent != null && parent != this; parent = parent.getParent()) {
            if (parent instanceof ItemVerticalScrollView || parent instanceof ItemHorizontalScrollView) {
                return true;
            }
        }
        return false;
    }

    private boolean hitTestToPane(float x, float y) {
        int[] locationOnScreen = new int[2];
        viewPane.getLocationOnScreen(locationOnScreen);
        if (!(x > locationOnScreen[0])) return false;
        if (!(x < locationOnScreen[0] + viewPane.getWidth() * viewPane.getScaleX())) return false;
        if (!(y > locationOnScreen[1])) return false;
        return y < locationOnScreen[1] + viewPane.getHeight() * viewPane.getScaleY();
    }

    private void deleteWidgetFromCollection(String widgetName) {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(getContext());
        dialog.setTitle(Helper.getResString(R.string.view_widget_favorites_delete_title));
        dialog.setIcon(R.drawable.ic_mtrl_delete);
        dialog.setMessage(Helper.getResString(R.string.view_widget_favorites_delete_message));
        dialog.setPositiveButton(Helper.getResString(R.string.common_word_delete), (v, which) -> {
            WidgetCollectionManager.getInstance().removeWidget(widgetName, true);
            setFavoriteData(WidgetCollectionManager.getInstance().getWidgets());
            v.dismiss();
        });
        dialog.setNegativeButton(Helper.getResString(R.string.common_word_cancel), null);
        dialog.show();
    }

    private String getString(@StringRes int res) {
        return getContext().getString(res);
    }

    private void cancelAnimation() {
        if (animatorTranslateY.isRunning()) animatorTranslateY.cancel();
        if (animatorTranslateX.isRunning()) animatorTranslateX.cancel();
    }

    private void setPreviewColors(String scId) {
        bgStatus.setBackgroundColor(ProjectFile.getColor(scId, ProjectFile.COLOR_PRIMARY_DARK));
        imgPhoneTopBg.setBackgroundColor(ProjectFile.getColor(scId, ProjectFile.COLOR_PRIMARY_DARK));
        toolbar.setBackgroundColor(ProjectFile.getColor(scId, ProjectFile.COLOR_PRIMARY));
    }

    private void showDeleteView(boolean show, boolean isCustomWidget) {
        if (isCustomWidget) {
            deleteIcon.setImageDrawable(AppCompatResources.getDrawable(getContext(), R.drawable.ic_mtrl_edit));
            deleteText.setText(Helper.getResString(R.string.editor_drag_to_actions));
        } else if (show) {
            deleteIcon.setImageDrawable(AppCompatResources.getDrawable(getContext(), R.drawable.ic_mtrl_delete));
            deleteText.setText(Helper.getResString(R.string.editor_drag_to_delete));
            setDeleteViewIconAndTextUi(false);
        }
        deleteView.bringToFront();
        if (!isAnimating) {
            animateUpDown();
        }
        if (isPaletteVisible == show) return;
        isPaletteVisible = show;
        cancelAnimation();
        if (show) {
            animatorTranslateY.start();
        } else {
            animatorTranslateX.start();
        }
    }

    public void initialize(String projectId, ProjectFileBean projectFileBean) {
        scId = projectId;
        setPreviewColors(projectId);
        if (viewPane != null) {
            viewPane.initialize(projectId, false);
        }
        this.projectFileBean = projectFileBean;
        xmlName = projectFileBean.getXmlName();
        if (projectFileBean.fileType == ProjectFileBean.PROJECT_FILE_TYPE_DRAWER) {
            fileName.setText(projectFileBean.fileName.substring(1));
        } else {
            fileName.setText(projectFileBean.getXmlName());
        }
        removeFab();
        if (projectFileBean.fileType == ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY) {
            hasToolbar = projectFileBean.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_TOOLBAR);
            isFullscreen = projectFileBean.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_FULLSCREEN);
            if (projectFileBean.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_FAB)) {
                addFab(ProjectDataManager.getProjectDataManager(projectId).getFabView(projectFileBean.getXmlName()));
            }
        } else {
            hasToolbar = false;
            isFullscreen = false;
        }
        isLayoutChanged = true;
    }

    public void updateSelection(String tag) {
        ItemView syVar;
        ItemView itemView = viewPane.findItemViewByTag(tag);
        if (itemView == null || (syVar = selectedItem) == itemView) {
            return;
        }
        if (syVar != null) {
            syVar.setSelection(false);
        }
        itemView.setSelection(true);
        selectedItem = itemView;
    }

    private void updateLayoutPreview() {
        toolbar.setVisibility(hasToolbar ? View.VISIBLE : View.GONE);
        bgStatus.setVisibility(isFullscreen ? View.GONE : View.VISIBLE);

        viewPane.setVisibility(View.VISIBLE);
        displayWidth = getResources().getDisplayMetrics().widthPixels;
        displayHeight = getResources().getDisplayMetrics().heightPixels;
        boolean isLandscapeMode = displayWidth > displayHeight;
        int var4 = (int) (dip * (!isLandscapeMode ? 12.0F : 24.0F));
        int var5 = (int) (dip * (!isLandscapeMode ? 20.0F : 10.0F));
        int statusBarHeight = UI.getStatusBarHeight(getContext());
        int toolBarHeight = DeviceUtil.getToolbarHeight(getContext());
        int var9 = displayWidth - (int) (120.0F * dip);
        int var8 = displayHeight - statusBarHeight - toolBarHeight - (int) (dip * 48.0F) - (int) (dip * 48.0F);
        if (screenType == 0 && isAdLoaded) {
            var8 -= (int) (dip * 56.0F);
        }

        float var11 = Math.min((float) var9 / (float) displayWidth, (float) var8 / (float) displayHeight);
        float var3 = Math.min((float) (var9 - var4 * 2) / (float) displayWidth, (float) (var8 - var5 * 2) / (float) displayHeight);

        screenContainer.setLayoutParams(new FrameLayout.LayoutParams(displayWidth, displayHeight));
        screenContainer.setScaleX(var11);
        screenContainer.setScaleY(var11);
        screenContainer.setX(-((int) ((displayWidth - displayWidth * var11) / 2.0F)));
        screenContainer.setY(-((int) ((displayHeight - displayHeight * var11) / 2.0F)));
        int var10 = var4 - (int) ((displayWidth - displayWidth * var3) / 2.0F);
        int var13 = var5;
        if (bgStatus.getVisibility() == View.VISIBLE) {
            bgStatus.setLayoutParams(new FrameLayout.LayoutParams(displayWidth, statusBarHeight));
            bgStatus.setScaleX(var3);
            bgStatus.setScaleY(var3);
            var11 = statusBarHeight;
            float var12 = var11 * var3;
            bgStatus.setX(var10);
            bgStatus.setY(var5 - (int) ((var11 - var12) / 2.0F));
            var13 = var5 + (int) var12;
        }

        var8 = var13;
        if (toolbar.getVisibility() == View.VISIBLE) {
            toolbar.setLayoutParams(new FrameLayout.LayoutParams(displayWidth, toolBarHeight));
            toolbar.setScaleX(var3);
            toolbar.setScaleY(var3);
            var11 = (float) toolBarHeight * var3;
            toolbar.setX(var10);
            toolbar.setY(var13 - (int) (((float) toolBarHeight - var11) / 2.0F));
            var8 = var13 + (int) var11;
        }

        var13 = displayHeight;
        if (bgStatus.getVisibility() == View.VISIBLE) {
            var13 = displayHeight - statusBarHeight;
        }

        var5 = var13;
        if (toolbar.getVisibility() == View.VISIBLE) {
            var5 = var13 - toolBarHeight;
        }

        viewPane.setLayoutParams(new FrameLayout.LayoutParams(displayWidth, var5));
        viewPane.setScaleX(var3);
        viewPane.setScaleY(var3);
        var11 = var5;
        viewPane.setX(var10);
        viewPane.setY(var8 - (int) ((var11 - var3 * var11) / 2.0F));
        isLayoutChanged = false;
    }

    public void addWidgetLayout(PaletteWidget.LayoutType aVar, String layoutName) {
        View widget = paletteWidget.addLayout(aVar, layoutName);
        widget.setClickable(true);
        widget.setOnTouchListener(this);
    }

    public void extraWidgetLayout(String tag, String name) {
        View extraWidgetLayout = paletteWidget.extraWidgetLayout(tag, name);
        extraWidgetLayout.setClickable(true);
        extraWidgetLayout.setOnTouchListener(this);
    }

    public void addWidget(PaletteWidget.WidgetType bVar, String tag, String text, String resourceName) {
        View widget = paletteWidget.addWidget(bVar, tag, text, resourceName);
        widget.setClickable(true);
        widget.setOnTouchListener(this);
    }

    public void extraWidget(String tag, String title, String name) {
        View extraWidget = paletteWidget.extraWidget(tag, title, name);
        extraWidget.setClickable(true);
        extraWidget.setOnTouchListener(this);
    }

    private boolean isViewAnIconBase(View view) {
        return view instanceof IconBase;
    }

    private void addFavoriteViews(String collectionName, ArrayList<ViewBean> widgetBeans) {
        View a2 = paletteFavorite.addWidgetCollection(collectionName, widgetBeans);
        a2.setClickable(true);
        a2.setOnTouchListener(this);
    }

    private String generateWidgetId(ViewBean bean) {
        int type = bean.type;
        String b2 = !bean.isCustomWidget ? SketchwarePaths.getWidgetTypeName(type) : widgetsCreatorManager.generateCustomWidgetId(bean.convert);
        StringBuilder sb = new StringBuilder();
        sb.append(b2);
        int counter = countItems[type] + 1;
        countItems[type] = counter;
        sb.append(counter);
        String sb2 = sb.toString();
        ArrayList<ViewBean> d = ProjectDataManager.getProjectDataManager(scId).getViews(xmlName);
        while (true) {
            boolean isIdUsed = false;
            for (ViewBean view : d) {
                if (sb2.equals(view.id)) {
                    isIdUsed = true;
                    break;
                }
            }
            if (!isIdUsed) {
                return sb2;
            }
            StringBuilder sb3 = new StringBuilder();
            sb3.append(b2);
            counter = countItems[type] + 1;
            countItems[type] = counter;
            sb3.append(counter);
            sb2 = sb3.toString();
        }
    }

    public ItemView addViews(ArrayList<ViewBean> viewBeans, boolean recordHistory) {
        if (recordHistory) {
            ViewHistoryManager.getInstance(scId).recordAddMultiple(projectFileBean.getXmlName(), viewBeans);
            if (historyChangeListener != null) {
                historyChangeListener.onCallback();
            }
        }
        ItemView syVar = null;
        for (ViewBean view : viewBeans) {
            if (viewBeans.indexOf(view) == 0) {
                syVar = createAndAddView(view);
            } else {
                createAndAddView(view);
            }
        }
        return syVar;
    }

    public ItemView addView(ViewBean viewBean, boolean isInHistory) {
        if (isInHistory) {
            ViewHistoryManager.getInstance(scId).recordAdd(projectFileBean.getXmlName(), viewBean);
            if (historyChangeListener != null) {
                historyChangeListener.onCallback();
            }
        }
        return createAndAddView(viewBean);
    }

    public void loadViews(ArrayList<ViewBean> viewBeans) {
        if (viewBeans == null || viewBeans.isEmpty()) {
            return;
        }
        for (ViewBean view : viewBeans) {
            createAndAddView(view);
        }
    }

    public void addFab(ViewBean viewBean) {
        viewPane.addFab(viewBean).setOnTouchListener(this);
    }

    public void setSelectedItem(ItemView syVar, boolean showProperties) {
        if (selectedItem != null) {
            selectedItem.setSelection(false);
        }
        selectedItem = syVar;
        selectedItem.setSelection(true);
        if (widgetSelectedListener != null) {
            widgetSelectedListener.onViewSelectedWithProperty(showProperties, selectedItem.getBean().id);
        }
    }

    private boolean hitTestIconDelete(float x, float y) {
        int[] locationOnScreen = new int[2];
        deleteView.getLocationOnScreen(locationOnScreen);
        if (!(x > locationOnScreen[0])) return false;
        if (!(x < locationOnScreen[0] + deleteView.getWidth())) return false;
        if (!(y > locationOnScreen[1])) return false;
        return y < locationOnScreen[1] + deleteView.getHeight();
    }

    private void updateDeleteIcon(boolean active, boolean isCustomWidget) {
        if (isDeleteActive == active) return;
        isDeleteActive = active;
        if (isDeleteActive) {
            setSelectedDeleteViewUi(isCustomWidget);
            shakeView(deleteView);
        } else {
            initialDeleteViewUi();
            setDeleteViewIconAndTextUi(isCustomWidget);
        }
        if (isCustomWidget) {
            deleteIcon.setImageDrawable(AppCompatResources.getDrawable(getContext(), R.drawable.ic_mtrl_edit));
            deleteText.setText(getString(isDeleteActive ? R.string.editor_release_to_actions : R.string.editor_drag_to_actions));
        } else {
            deleteIcon.setImageDrawable(AppCompatResources.getDrawable(getContext(), R.drawable.ic_mtrl_delete));
            deleteText.setText(getString(isDeleteActive ? R.string.editor_release_to_delete : R.string.editor_drag_to_delete));
        }
    }

    private void initialDeleteViewUi() {
        deleteView.setCardBackgroundColor(colorSurfaceContainerHighest);
    }

    private void setSelectedDeleteViewUi(boolean isCustomWidget) {
        deleteView.setCardBackgroundColor(isCustomWidget ? colorCoolGreenContainer : colorErrorContainer);
        setDeleteViewIconAndTextUi(isCustomWidget);
    }

    private void setDeleteViewIconAndTextUi(boolean isCustomWidget) {
        if (isCustomWidget) {
            deleteText.setTextColor(colorCoolGreen);
            deleteIcon.setColorFilter(colorCoolGreen);
        } else {
            deleteText.setTextColor(colorError);
            deleteIcon.setColorFilter(colorError);
        }
    }

    public void createCustomWidget(HashMap<String, Object> map) {
        View extraWidget = paletteWidget.customWidget(map);
        extraWidget.setClickable(true);
        Object position = map.get("position");
        int tagValue = 0;
        if (position instanceof Integer) {
            tagValue = (Integer) position;
        } else if (position instanceof Double) {
            tagValue = ((Double) position).intValue();
        }
        extraWidget.setTag(tagValue);
        extraWidget.setOnTouchListener(this);
    }

    enum PaletteGroup {
        BASIC,
        FAVORITE
    }

    static class PaletteGroupItem extends LinearLayout implements View.OnClickListener {
        private final ImageView imgGroup;

        public PaletteGroupItem(Context context) {
            super(context);

            ViewUtil.inflateLayoutInto(context, this, R.layout.palette_group_item);
            imgGroup = findViewById(R.id.img_group);
        }

        @Override
        public void onClick(View view) {
        }

        public void setPaletteGroup(PaletteGroup group) {
            imgGroup.setImageResource(group == PaletteGroup.BASIC ?
                    R.drawable.selector_palette_tab_ic_sketchware :
                    R.drawable.selector_palette_tab_ic_bookmark);
            setOnClickListener(this);
        }
    }
}
