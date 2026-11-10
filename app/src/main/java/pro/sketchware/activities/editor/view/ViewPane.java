package pro.sketchware.activities.editor.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewParent;
import android.widget.CalendarView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.resources.MaterialResources;
import com.google.android.material.shape.ShapeAppearance;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.android.material.shape.StateListShapeAppearanceModel;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.theme.overlay.MaterialThemeOverlay;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import pro.sketchware.R;
import pro.sketchware.activities.editor.view.item.ItemAdView;
import pro.sketchware.activities.editor.view.item.ItemAnalogClock;
import pro.sketchware.activities.editor.view.item.ItemAutoCompleteTextView;
import pro.sketchware.activities.editor.view.item.ItemBadgeView;
import pro.sketchware.activities.editor.view.item.ItemBottomNavigationView;
import pro.sketchware.activities.editor.view.item.ItemButton;
import pro.sketchware.activities.editor.view.item.ItemCalendarView;
import pro.sketchware.activities.editor.view.item.ItemCardView;
import pro.sketchware.activities.editor.view.item.ItemCheckBox;
import pro.sketchware.activities.editor.view.item.ItemCircleImageView;
import pro.sketchware.activities.editor.view.item.ItemCodeView;
import pro.sketchware.activities.editor.view.item.ItemDatePicker;
import pro.sketchware.activities.editor.view.item.ItemDigitalClock;
import pro.sketchware.activities.editor.view.item.ItemEditText;
import pro.sketchware.activities.editor.view.item.ItemFloatingActionButton;
import pro.sketchware.activities.editor.view.item.ItemGridView;
import pro.sketchware.activities.editor.view.item.ItemHorizontalScrollView;
import pro.sketchware.activities.editor.view.item.ItemImageView;
import pro.sketchware.activities.editor.view.item.ItemLinearLayout;
import pro.sketchware.activities.editor.view.item.ItemListView;
import pro.sketchware.activities.editor.view.item.ItemLottieAnimation;
import pro.sketchware.activities.editor.view.item.ItemMapView;
import pro.sketchware.activities.editor.view.item.ItemMaterialButton;
import pro.sketchware.activities.editor.view.item.ItemMultiAutoCompleteTextView;
import pro.sketchware.activities.editor.view.item.ItemOTPView;
import pro.sketchware.activities.editor.view.item.ItemPatternLockView;
import pro.sketchware.activities.editor.view.item.ItemProgressBar;
import pro.sketchware.activities.editor.view.item.ItemRadioButton;
import pro.sketchware.activities.editor.view.item.ItemRatingBar;
import pro.sketchware.activities.editor.view.item.ItemRecyclerView;
import pro.sketchware.activities.editor.view.item.ItemRelativeLayout;
import pro.sketchware.activities.editor.view.item.ItemSearchView;
import pro.sketchware.activities.editor.view.item.ItemSeekBar;
import pro.sketchware.activities.editor.view.item.ItemSignInButton;
import pro.sketchware.activities.editor.view.item.ItemSpinner;
import pro.sketchware.activities.editor.view.item.ItemSwitch;
import pro.sketchware.activities.editor.view.item.ItemTabLayout;
import pro.sketchware.activities.editor.view.item.ItemTextView;
import pro.sketchware.activities.editor.view.item.ItemTimePicker;
import pro.sketchware.activities.editor.view.item.ItemVerticalScrollView;
import pro.sketchware.activities.editor.view.item.ItemVideoView;
import pro.sketchware.activities.editor.view.item.ItemViewPager;
import pro.sketchware.activities.editor.view.item.ItemWaveSideBar;
import pro.sketchware.activities.editor.view.item.ItemWebView;
import pro.sketchware.activities.editor.view.item.ItemYoutubePlayer;
import pro.sketchware.beans.ImageBean;
import pro.sketchware.beans.LayoutBean;
import pro.sketchware.beans.ProjectFileBean;
import pro.sketchware.beans.ProjectResourceBean;
import pro.sketchware.beans.ViewBean;
import pro.sketchware.beans.ViewBeans;
import pro.sketchware.core.codegen.AppCompatInjection;
import pro.sketchware.core.layout.InjectAttributeHandler;
import pro.sketchware.core.layout.MaterialButtonAttributes;
import pro.sketchware.core.layout.RootLayoutConverter;
import pro.sketchware.core.library.Material3LibraryManager;
import pro.sketchware.core.project.ClassInfo;
import pro.sketchware.core.project.InjectRootLayoutManager;
import pro.sketchware.core.project.ProjectListManager;
import pro.sketchware.core.project.ProjectThemeColors;
import pro.sketchware.core.project.ResourceManager;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.core.resources.ColorResourceResolver;
import pro.sketchware.core.resources.StringResourceResolver;
import pro.sketchware.graphics.NinePatchDecoder;
import pro.sketchware.graphics.SvgUtils;
import pro.sketchware.graphics.VectorDrawableLoader;
import pro.sketchware.util.FileUtil;
import pro.sketchware.util.InvokeUtil;
import pro.sketchware.util.LogUtil;
import pro.sketchware.util.MapValueHelper;
import pro.sketchware.util.PropertiesUtil;
import pro.sketchware.util.ThemeUtils;
import pro.sketchware.util.ViewUtil;

public class ViewPane extends RelativeLayout {
    private final String stringsStart = "@string/";
    private final FirebaseCrashlytics crashlytics = getFirebaseCrashlytics();
    private Context context;
    private ViewGroup rootLayout;
    private int viewIdCounter = 99;
    private ArrayList<ViewInfo> viewInfos = new ArrayList<>();
    private ViewInfo viewInfo;
    private String draggedViewId;
    private TextView highlightedTextView;
    private ResourceManager resourcesManager;
    private String sc_id;
    private SvgUtils svgUtils;
    private ColorResourceResolver colorResourceResolver;
    private PreviewValues previewValues;
    private Material3LibraryManager material3LibraryManager;
    private HashMap<String, String> xmlStringCache = null;
    private String xmlStringCacheScId = null;
    private HashMap<String, String> importedFontPathCache = null;
    private String importedFontPathCacheScId = null;

    public ViewPane(Context context) {
        super(context);
    }

    public ViewPane(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public void clearViews() {
        previewValues = null;
        resetView(true);
        viewInfos = new ArrayList<>();
        draggedViewId = null;
        if (rootLayout != null) {
            ((ScrollContainer) rootLayout).setChildScrollEnabled(true);
        }
    }

    public void setResourceManager(ResourceManager resourcesManager) {
        this.resourcesManager = resourcesManager;
        previewValues = null;
        invalidateImportedFontPathCache();
    }

    private void invalidateImportedFontPathCache() {
        importedFontPathCache = null;
        importedFontPathCacheScId = sc_id;
    }

    private void initTextView() {
        highlightedTextView = new TextView(getContext());
        highlightedTextView.setBackgroundResource(R.drawable.highlight);
        highlightedTextView.setLayoutParams(
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        highlightedTextView.setVisibility(GONE);
    }

    public void clearViewPane() {
        if (rootLayout != null) {
            rootLayout.removeAllViews();
        }
    }

    public void removeFabView() {
        View findViewWithTag = findViewWithTag("_fab");
        if (findViewWithTag == null) {
            return;
        }
        removeView(findViewWithTag);
    }

    public void removeView(ViewBean viewBean) {
        ViewGroup viewGroup = rootLayout.findViewWithTag(viewBean.parent);
        viewGroup.removeView(rootLayout.findViewWithTag(viewBean.id));
        if (viewGroup instanceof ScrollContainer) {
            ((ScrollContainer) viewGroup).reindexChildren();
        }
    }

    public ItemView updateView(ViewBean viewBean) {
        View findViewWithTag;
        String preId = viewBean.preId;
        if (preId != null && !preId.isEmpty() && !preId.equals(viewBean.id)) {
            View preView = rootLayout.findViewWithTag(preId);
            if (preView != null) preView.setTag(viewBean.id);
            viewBean.preId = "";
        }
        if (viewBean.id != null && !viewBean.id.isEmpty() && viewBean.id.charAt(0) == '_') {
            findViewWithTag = findViewWithTag(viewBean.id);
        } else {
            findViewWithTag = rootLayout.findViewWithTag(viewBean.id);
        }
        updateItemView(findViewWithTag, viewBean);
        return (ItemView) findViewWithTag;
    }

    public ItemView moveView(ViewBean viewBean) {
        View findViewWithTag = rootLayout.findViewWithTag(viewBean.id);
        if (viewBean.id != null && !viewBean.id.isEmpty() && viewBean.id.charAt(0) == '_') {
            findViewWithTag = findViewWithTag(viewBean.id);
        }
        String preParent = viewBean.preParent;
        if (preParent != null
                && !preParent.isEmpty()
                && !viewBean.parent.equals(viewBean.preParent)) {
            ViewGroup viewGroup = rootLayout.findViewWithTag(viewBean.preParent);
            viewGroup.removeView(findViewWithTag);
            ((ScrollContainer) viewGroup).reindexChildren();
            addViewAndUpdateIndex(findViewWithTag);
        } else if (viewBean.index != viewBean.preIndex) {
            ((ViewGroup) rootLayout.findViewWithTag(viewBean.parent)).removeView(findViewWithTag);
            addViewAndUpdateIndex(findViewWithTag);
        }
        viewBean.preId = "";
        viewBean.preIndex = -1;
        viewBean.preParent = "";
        viewBean.preParentType = -1;
        updateVisibility(findViewWithTag, new InjectAttributeHandler(viewBean));
        return (ItemView) findViewWithTag;
    }

    public void initialize(String sc_id, boolean isPreviewMode) {
        this.sc_id = sc_id;
        if (importedFontPathCacheScId == null || !importedFontPathCacheScId.equals(sc_id)) {
            invalidateImportedFontPathCache();
        }
        material3LibraryManager = new Material3LibraryManager(sc_id);
        colorResourceResolver = new ColorResourceResolver(sc_id);
        previewValues = null;
        context = material3LibraryManager.createPreviewContext(getContext());
        if (rootLayout != null) {
            removeView(rootLayout);
            rootLayout = null;
        }
        svgUtils = new SvgUtils(context);
        svgUtils.initImageLoader();
        if (!isMaterial3Preview()) {
            setBackgroundColor(Color.WHITE);
        } else if (isPreviewMode) {
            setBackgroundColor(ThemeUtils.getColor(context, android.R.attr.colorBackground));
        } else {
            setBackground(AppCompatResources.getDrawable(context, R.drawable.bg_view_pane));
        }
        // addRootLayout();
        initTextView();
    }

    boolean isMaterial3Preview() {
        return material3LibraryManager != null && material3LibraryManager.isMaterial3Enabled();
    }

    Context getPreviewContext() {
        return context;
    }

    record ToolbarColors(int background, int title, int navigation) {}

    ToolbarColors getPreviewToolbarColors(ProjectFileBean file) {
        var injection = new AppCompatInjection(sc_id, file);
        Map<String, String> appBar = injection.getAttributes("AppBarLayout");
        Map<String, String> toolbar = injection.getAttributes("Toolbar");
        PreviewValues resources = previewValues();

        Map<String, String> theme = previewProjectTheme();
        theme.putAll(resources.resolveStyle(viewTheme(appBar)).attributes());
        theme.putAll(resources.resolveStyle(viewTheme(toolbar)).attributes());

        Map<String, String> style = resources.resolveStyle(theme.get("toolbarStyle")).attributes();
        style.putAll(resources.resolveStyle(toolbar.get("style")).attributes());
        String background =
                toolbar.getOrDefault("android:background", style.get("android:background"));
        int backgroundColor =
                background == null || background.equals("@null")
                        ? Color.TRANSPARENT
                        : resolvePreviewColor(resources, background, theme, R.attr.colorPrimary);

        String title = toolbar.getOrDefault("app:titleTextColor", style.get("titleTextColor"));
        if (title == null || title.equals("@null")) {
            String appearance =
                    toolbar.getOrDefault(
                            "app:titleTextAppearance", style.get("titleTextAppearance"));
            title =
                    resources
                            .resolveStyle(appearance)
                            .attributes()
                            .getOrDefault("android:textColor", "?attr/colorOnSurface");
        }
        String navigation =
                toolbar.getOrDefault("app:navigationIconTint", style.get("navigationIconTint"));
        if (navigation == null || navigation.equals("@null")) {
            navigation = "?attr/colorControlNormal";
        }
        return new ToolbarColors(
                backgroundColor,
                resolvePreviewColor(resources, title, theme, R.attr.colorOnSurface),
                resolvePreviewColor(resources, navigation, theme, R.attr.colorControlNormal));
    }

    private Map<String, String> previewProjectTheme() {
        PreviewValues values = previewValues();
        Map<String, String> theme = values.resolveStyle("@style/AppTheme").attributes();
        values.removeAttributesOverriddenBy(
                theme, material3LibraryManager.getDynamicColorOverlay(context));
        return theme;
    }

    private int resolvePreviewColor(
            PreviewValues resources, String raw, Map<String, String> theme, int fallback) {
        Integer color = projectColor(resources, resources.resolveThemeValue(raw, theme));
        return color != null ? color : ThemeUtils.getColor(context, fallback);
    }

    private Integer projectColor(PreviewValues resources, String reference) {
        Integer color = resources.color(reference);
        if (color == null && reference != null && reference.startsWith("@color/")) {
            color =
                    ProjectThemeColors.generatedColors(ProjectListManager.getProjectById(sc_id))
                            .get(reference.substring(7));
        }
        return color;
    }

    private static String viewTheme(Map<String, String> attributes) {
        return attributes.getOrDefault("android:theme", attributes.get("app:theme"));
    }

    public void addRootLayout(ViewBean viewBean) {
        viewInfo = null;
        draggedViewId = viewBean == null ? null : viewBean.id;
        if (rootLayout != null) {
            if (rootLayout instanceof ItemLinearLayout linearLayout) {
                addDroppableForLinearLayout(viewBean, linearLayout);
            } else {
                addDroppableForViewGroup(viewBean, rootLayout);
            }
            ((ScrollContainer) rootLayout).setChildScrollEnabled(false);
        }
    }

    private int calculateViewDepth(View view) {
        View currentView = view;
        int depth = 0;
        while (currentView != null && currentView != rootLayout) {
            depth++;
            currentView = (View) currentView.getParent();
        }
        return depth * 2;
    }

    public View createItemView(ViewBean viewBean) {
        View item =
                switch (viewBean.type) {
                    case ViewBean.VIEW_TYPE_LAYOUT_LINEAR,
                            ViewBeans.VIEW_TYPE_LAYOUT_COLLAPSINGTOOLBARLAYOUT,
                            ViewBeans.VIEW_TYPE_LAYOUT_TEXTINPUTLAYOUT,
                            ViewBeans.VIEW_TYPE_LAYOUT_SWIPEREFRESHLAYOUT,
                            ViewBeans.VIEW_TYPE_LAYOUT_RADIOGROUP ->
                            new ItemLinearLayout(context);
                    case ViewBean.VIEW_TYPE_LAYOUT_RELATIVE -> new ItemRelativeLayout(context);
                    case ViewBeans.VIEW_TYPE_LAYOUT_CARDVIEW -> new ItemCardView(context);
                    case ViewBean.VIEW_TYPE_LAYOUT_HSCROLLVIEW ->
                            new ItemHorizontalScrollView(context);
                    case ViewBean.VIEW_TYPE_WIDGET_BUTTON ->
                            isMaterial3Preview()
                                    ? new ItemMaterialButton(context)
                                    : new ItemButton(context);
                    case ViewBean.VIEW_TYPE_WIDGET_TEXTVIEW -> new ItemTextView(context);
                    case ViewBean.VIEW_TYPE_WIDGET_EDITTEXT -> new ItemEditText(context);
                    case ViewBean.VIEW_TYPE_WIDGET_IMAGEVIEW -> new ItemImageView(context);
                    case ViewBean.VIEW_TYPE_WIDGET_WEBVIEW -> new ItemWebView(context);
                    case ViewBean.VIEW_TYPE_WIDGET_PROGRESSBAR -> new ItemProgressBar(context);
                    case ViewBean.VIEW_TYPE_WIDGET_LISTVIEW -> new ItemListView(context);
                    case ViewBean.VIEW_TYPE_WIDGET_SPINNER -> new ItemSpinner(context);
                    case ViewBean.VIEW_TYPE_WIDGET_CHECKBOX -> new ItemCheckBox(context);
                    case ViewBean.VIEW_TYPE_LAYOUT_VSCROLLVIEW ->
                            new ItemVerticalScrollView(context);
                    case ViewBean.VIEW_TYPE_WIDGET_SWITCH -> new ItemSwitch(context);
                    case ViewBean.VIEW_TYPE_WIDGET_SEEKBAR -> new ItemSeekBar(context);
                    case ViewBean.VIEW_TYPE_WIDGET_CALENDARVIEW -> new ItemCalendarView(context);
                    case ViewBean.VIEW_TYPE_WIDGET_ADVIEW -> new ItemAdView(context);
                    case ViewBean.VIEW_TYPE_WIDGET_MAPVIEW -> new ItemMapView(context);
                    case ViewBeans.VIEW_TYPE_WIDGET_RADIOBUTTON -> new ItemRadioButton(context);
                    case ViewBeans.VIEW_TYPE_WIDGET_RATINGBAR -> new ItemRatingBar(context);
                    case ViewBeans.VIEW_TYPE_WIDGET_VIDEOVIEW -> new ItemVideoView(context);
                    case ViewBeans.VIEW_TYPE_WIDGET_SEARCHVIEW -> new ItemSearchView(context);
                    case ViewBeans.VIEW_TYPE_WIDGET_AUTOCOMPLETETEXTVIEW ->
                            new ItemAutoCompleteTextView(context);
                    case ViewBeans.VIEW_TYPE_WIDGET_MULTIAUTOCOMPLETETEXTVIEW ->
                            new ItemMultiAutoCompleteTextView(context);
                    case ViewBeans.VIEW_TYPE_WIDGET_GRIDVIEW -> new ItemGridView(context);
                    case ViewBeans.VIEW_TYPE_WIDGET_ANALOGCLOCK -> new ItemAnalogClock(context);
                    case ViewBeans.VIEW_TYPE_WIDGET_DATEPICKER -> new ItemDatePicker(context);
                    case ViewBeans.VIEW_TYPE_WIDGET_TIMEPICKER -> new ItemTimePicker(context);
                    case ViewBeans.VIEW_TYPE_WIDGET_DIGITALCLOCK -> new ItemDigitalClock(context);
                    case ViewBeans.VIEW_TYPE_LAYOUT_TABLAYOUT -> new ItemTabLayout(context);
                    case ViewBeans.VIEW_TYPE_LAYOUT_VIEWPAGER -> new ItemViewPager(context);
                    case ViewBeans.VIEW_TYPE_LAYOUT_BOTTOMNAVIGATIONVIEW ->
                            new ItemBottomNavigationView(context);
                    case ViewBeans.VIEW_TYPE_WIDGET_BADGEVIEW -> new ItemBadgeView(context);
                    case ViewBeans.VIEW_TYPE_WIDGET_PATTERNLOCKVIEW ->
                            new ItemPatternLockView(context);
                    case ViewBeans.VIEW_TYPE_WIDGET_WAVESIDEBAR -> new ItemWaveSideBar(context);
                    case ViewBeans.VIEW_TYPE_WIDGET_MATERIALBUTTON ->
                            new ItemMaterialButton(context);
                    case ViewBeans.VIEW_TYPE_WIDGET_SIGNINBUTTON -> new ItemSignInButton(context);
                    case ViewBeans.VIEW_TYPE_WIDGET_CIRCLEIMAGEVIEW ->
                            new ItemCircleImageView(context);
                    case ViewBeans.VIEW_TYPE_WIDGET_LOTTIEANIMATIONVIEW ->
                            new ItemLottieAnimation(context);
                    case ViewBeans.VIEW_TYPE_WIDGET_YOUTUBEPLAYERVIEW ->
                            new ItemYoutubePlayer(context);
                    case ViewBeans.VIEW_TYPE_WIDGET_OTPVIEW -> new ItemOTPView(context);
                    case ViewBeans.VIEW_TYPE_WIDGET_CODEVIEW -> new ItemCodeView(context);
                    case ViewBeans.VIEW_TYPE_WIDGET_RECYCLERVIEW -> new ItemRecyclerView(context);
                    default -> getUnknownItemView(viewBean);
                };
        item.setId(++viewIdCounter);
        item.setTag(viewBean.id);
        if (item instanceof TextView textView) {
            textView.setTag(R.id.preview_default_text_colors, textView.getTextColors());
        }
        if (item instanceof EditText editText) {
            editText.setTag(R.id.preview_default_hint_colors, editText.getHintTextColors());
        }
        ((ItemView) item).setBean(viewBean);
        updateItemView(item, viewBean);
        return item;
    }

    private View getUnknownItemView(ViewBean bean) {
        LogUtil.w(
                "ViewPane",
                "Unknown widget type "
                        + bean.type
                        + " for view '"
                        + bean.id
                        + "'; showing as LinearLayout placeholder");
        return new ItemLinearLayout(context);
    }

    public void updateRootLayout(String sc_id, String fileName) {
        InjectRootLayoutManager manager = new InjectRootLayoutManager(sc_id);
        var currentBean = RootLayoutConverter.toBean(manager.getLayoutByFileName(fileName));
        View rootView = createItemView(currentBean);
        if (rootView instanceof ItemView sy) {
            sy.setFixed(true);
        }
        if (rootLayout != null) {
            removeView(rootLayout);
        }
        if (!(rootLayout instanceof ItemView sy) || !currentBean.isEqual(sy.getBean())) {
            rootLayout = (ViewGroup) rootView;
        }
        addView(rootLayout);
    }

    private void updateItemView(View view, ViewBean viewBean) {
        ImageBean imageBean;
        String resName;
        var injectHandler = new InjectAttributeHandler(viewBean);
        updateVisibility(view, injectHandler);
        if (viewBean.id != null && !viewBean.id.isEmpty() && viewBean.id.charAt(0) == '_') {
            LayoutParams layoutParams =
                    new LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.leftMargin = layoutDpToPixels(viewBean.layout.marginLeft);
            layoutParams.topMargin = layoutDpToPixels(viewBean.layout.marginTop);
            layoutParams.rightMargin = layoutDpToPixels(viewBean.layout.marginRight);
            layoutParams.bottomMargin = layoutDpToPixels(viewBean.layout.marginBottom);
            int layoutGravity = viewBean.layout.layoutGravity;
            if ((layoutGravity & Gravity.LEFT) == Gravity.LEFT) {
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            }
            if ((layoutGravity & Gravity.TOP) == Gravity.TOP) {
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            }
            if ((layoutGravity & Gravity.RIGHT) == Gravity.RIGHT) {
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            }
            if ((layoutGravity & Gravity.BOTTOM) == Gravity.BOTTOM) {
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            }
            if ((layoutGravity & Gravity.CENTER_HORIZONTAL) == Gravity.CENTER_HORIZONTAL) {
                layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
            }
            if ((layoutGravity & Gravity.CENTER_VERTICAL) == Gravity.CENTER_VERTICAL) {
                layoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
            }
            if ((layoutGravity & Gravity.CENTER) == Gravity.CENTER) {
                layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
            }
            view.setLayoutParams(layoutParams);
            if (viewBean.getClassInfo().isExactType("FloatingActionButton")
                    && (imageBean = viewBean.image) != null
                    && (resName = imageBean.resName) != null
                    && !resName.isEmpty()) {
                try {
                    if (crashlytics != null)
                        crashlytics.log("ViewPane: trying to set image to FAB");
                    FloatingActionButton fab = (FloatingActionButton) view;
                    if (resourcesManager.getImageResType(viewBean.image.resName)
                            == ProjectResourceBean.PROJECT_RES_TYPE_RESOURCE) {
                        int resourceId =
                                getContext()
                                        .getResources()
                                        .getIdentifier(
                                                viewBean.image.resName,
                                                "drawable",
                                                getContext().getPackageName());
                        if (resourceId != 0) {
                            fab.setImageResource(resourceId);
                        }
                    } else if (viewBean.image.resName.equals("default_image")) {
                        fab.setImageResource(R.drawable.default_image);
                    } else {
                        String imagePath = resourcesManager.getImagePath(viewBean.image.resName);
                        if (imagePath == null) return;
                        File imageFile = new File(imagePath);

                        if (imageFile.exists()) {
                            int scaleFactor =
                                    Math.round(getResources().getDisplayMetrics().density / 2.0f);

                            if (imagePath.endsWith(".xml")) {
                                if (crashlytics != null)
                                    crashlytics.log("ViewPane: loading scaled XML/SVG image");
                                svgUtils.loadScaledSvgIntoImageView(
                                        new AppCompatImageView(getContext()) {
                                            @Override
                                            public void setImageBitmap(Bitmap bitmap) {
                                                fab.setImageBitmap(bitmap);
                                            }
                                        },
                                        SketchwarePaths.getProjectConvertedVectorFilePath(
                                                sc_id, viewBean.image.resName),
                                        scaleFactor);
                            } else {
                                Bitmap bitmap =
                                        FileUtil.decodeSampleBitmapFromPath(imagePath, 256, 256);
                                if (bitmap != null) {
                                    fab.setImageBitmap(bitmap);
                                }
                            }
                        } else {
                            if (crashlytics != null)
                                crashlytics.log("ViewPane: converting XML to SVG for FAB");
                            VectorDrawableLoader vectorDrawableLoader = new VectorDrawableLoader();
                            ImageView tempImageView =
                                    new AppCompatImageView(getContext()) {
                                        @Override
                                        public void setImageDrawable(Drawable drawable) {
                                            fab.setImageDrawable(drawable);
                                        }
                                    };
                            vectorDrawableLoader.setImageVectorFromFile(
                                    tempImageView,
                                    vectorDrawableLoader.getVectorFullPath(
                                            sc_id, viewBean.image.resName),
                                    sc_id);
                        }
                    }
                } catch (Exception exception) {
                    if (crashlytics != null) crashlytics.recordException(exception);
                }
            }
            view.setRotation(viewBean.image.rotate);
            view.setAlpha(viewBean.alpha);
            view.setTranslationX(ViewUtil.dpToPx(getContext(), viewBean.translationX));
            view.setTranslationY(ViewUtil.dpToPx(getContext(), viewBean.translationY));
            view.setScaleX(viewBean.scaleX);
            view.setScaleY(viewBean.scaleY);
            applyElevation(view, resolvePreviewElevation(injectHandler, viewBean.layout.elevation));
            return;
        }
        updatePreviewBackgroundColor(view, injectHandler);
        updateLayoutParams(view, viewBean);
        view.setRotation(viewBean.image.rotate);
        view.setAlpha(viewBean.alpha);
        view.setTranslationX(ViewUtil.dpToPx(getContext(), viewBean.translationX));
        view.setTranslationY(ViewUtil.dpToPx(getContext(), viewBean.translationY));
        view.setScaleX(viewBean.scaleX);
        view.setScaleY(viewBean.scaleY);
        updatePreviewBackgroundImage(view, viewBean.layout.backgroundResource);
        ClassInfo classInfo = viewBean.getClassInfo();
        if (classInfo.matchesType("LinearLayout")) {
            LinearLayout linearLayout = (LinearLayout) view;
            linearLayout.setOrientation(viewBean.layout.orientation);
            linearLayout.setWeightSum(viewBean.layout.weightSum);
            if (view instanceof ItemLinearLayout) {
                ((ItemLinearLayout) view).setLayoutGravity(viewBean.layout.gravity);
            }
        }
        if (getActualParentType(view, viewBean.parentType) == ViewBean.VIEW_TYPE_LAYOUT_RELATIVE) {
            updateRelative(view, injectHandler);
        }
        if (classInfo.matchesType("TextView")) {
            TextView textView = (TextView) view;
            updateTextView(textView, viewBean);
            if (!classInfo.isExactType("Button") && !classInfo.isExactType("Switch")) {
                textView.setGravity(viewBean.layout.gravity);
            } else {
                int gravity = viewBean.layout.gravity;
                if (gravity == LayoutBean.GRAVITY_NONE) {
                    textView.setGravity(Gravity.CENTER);
                } else {
                    textView.setGravity(gravity);
                }
            }
        }
        if (classInfo.matchesType("EditText")) {
            updateEditText((EditText) view, viewBean);
        }
        if (classInfo.matchesType("ImageView")) {
            String imgResName = viewBean.image.resName;
            if (TextUtils.isEmpty(imgResName) || "NONE".equalsIgnoreCase(imgResName)) {
                ((ImageView) view).setImageResource(R.drawable.default_image);
            } else if (resourcesManager.getImageResType(imgResName)
                    == ProjectResourceBean.PROJECT_RES_TYPE_RESOURCE) {
                int imgResId =
                        getContext()
                                .getResources()
                                .getIdentifier(
                                        imgResName, "drawable", getContext().getPackageName());
                if (imgResId != 0) {
                    ((ImageView) view).setImageResource(imgResId);
                }
            } else if ("default_image".equals(imgResName)) {
                ((ImageView) view).setImageResource(R.drawable.default_image);
            } else {
                try {
                    String imagelocation = resourcesManager.getImagePath(imgResName);
                    if (imagelocation != null) {
                        File file = new File(imagelocation);
                        if (file.exists() && file.length() > 0) {
                            int imageScale =
                                    Math.round(getResources().getDisplayMetrics().density / 2.0f);
                            if (imagelocation.endsWith(".xml")) {
                                svgUtils.loadScaledSvgIntoImageView(
                                        (ImageView) view,
                                        SketchwarePaths.getProjectConvertedVectorFilePath(
                                                sc_id, imgResName),
                                        imageScale);
                            } else {
                                Bitmap imageBitmap =
                                        FileUtil.decodeSampleBitmapFromPath(
                                                imagelocation, 512, 512);
                                if (imageBitmap != null) {
                                    ((ImageView) view).setImageBitmap(imageBitmap);
                                } else {
                                    ((ImageView) view).setImageResource(R.drawable.default_image);
                                }
                            }
                        } else {
                            VectorDrawableLoader vectorDrawableLoader = new VectorDrawableLoader();
                            vectorDrawableLoader.setImageVectorFromFile(
                                    (ImageView) view,
                                    vectorDrawableLoader.getVectorFullPath(sc_id, imgResName),
                                    sc_id);
                        }
                    }
                } catch (Exception vectorException) {
                    Log.w("ViewPane", "Failed to preview image: " + imgResName, vectorException);
                    // A preview fallback must preserve both the source file and its model
                    // reference.
                    ((ImageView) view).setImageResource(R.drawable.default_image);
                }
            }
            if (classInfo.isExactType("CircleImageView")) {
                updateCircleImageView((ItemCircleImageView) view, injectHandler);
            } else {
                String scaleTypeName = viewBean.image.scaleType;
                if (TextUtils.isEmpty(scaleTypeName)) {
                    scaleTypeName = ImageBean.SCALE_TYPE_CENTER;
                }
                try {
                    ((ImageView) view).setScaleType(ImageView.ScaleType.valueOf(scaleTypeName));
                } catch (IllegalArgumentException ignored) {
                    ((ImageView) view).setScaleType(ImageView.ScaleType.CENTER);
                }
            }
        }
        if (classInfo.matchesType("CompoundButton")) {
            ((CompoundButton) view).setChecked(viewBean.checked != 0);
        }
        if (classInfo.isExactType("SeekBar")) {
            SeekBar seekBar = (SeekBar) view;
            seekBar.setProgress(viewBean.progress);
            seekBar.setMax(viewBean.max);
        }
        if (classInfo.isExactType("CalendarView")) {
            ((CalendarView) view).setFirstDayOfWeek(viewBean.firstDayOfWeek);
        }
        if (classInfo.isExactType("AdView")) {
            ((ItemAdView) view).setAdSize(viewBean.adSize);
        }
        if (classInfo.isExactType("TabLayout")) {
            updateTabLayout((ItemTabLayout) view, injectHandler);
        }
        if (view instanceof ItemMaterialButton materialButton) {
            if (isMaterial3Preview()) {
                updateMaterial3Button(materialButton, injectHandler);
            } else {
                updateMaterialButton(materialButton, injectHandler);
            }
        }
        if (classInfo.isExactType("SignInButton")) {
            ItemSignInButton button = (ItemSignInButton) view;
            boolean hasButtonSize = false;
            boolean hasColorScheme = false;
            for (String line : viewBean.inject.split("\n")) {
                if (line.contains("buttonSize")) {
                    String buttonSize = extractAttrValue(line, "app:buttonSize");
                    if (!buttonSize.startsWith("@")) {
                        hasButtonSize = true;
                        switch (buttonSize) {
                            case "icon_only":
                                button.setSize(ItemSignInButton.ButtonSize.ICON_ONLY);
                                break;
                            case "wide":
                                button.setSize(ItemSignInButton.ButtonSize.WIDE);
                                break;
                            case "standard":
                            default:
                                button.setSize(ItemSignInButton.ButtonSize.STANDARD);
                                break;
                        }
                    }
                }
                if (line.contains("colorScheme")) {
                    String colorScheme = extractAttrValue(line, "app:colorScheme");
                    if (!colorScheme.startsWith("@")) {
                        hasColorScheme = true;
                        switch (colorScheme) {
                            case "dark":
                                button.setColorScheme(ItemSignInButton.ColorScheme.DARK);
                                break;
                            case "auto":
                            case "light":
                            default:
                                button.setColorScheme(ItemSignInButton.ColorScheme.LIGHT);
                                break;
                        }
                    }
                }
            }
            if (!hasButtonSize) button.setSize(ItemSignInButton.ButtonSize.STANDARD);
            if (!hasColorScheme) button.setColorScheme(ItemSignInButton.ColorScheme.LIGHT);
        }
        if (!classInfo.isExactType("CardView")) {
            applyElevation(view, resolvePreviewElevation(injectHandler, viewBean.layout.elevation));
        }
        PreviewPadding.apply(view, viewBean);
        if (view instanceof ItemCodeView codeView) {
            codeView.applyPreviewAttributes(
                    previewValues().resolveAttributes(injectHandler.getNamespacedAttributes()));
        } else if (view instanceof ItemProgressBar progressBar) {
            progressBar.applyPreviewAttributes(
                    viewBean, injectHandler.getNamespacedAttributes(), previewValues());
        } else if (view instanceof ItemPatternLockView patternLockView) {
            PreviewValues values = previewValues();
            patternLockView.applyPreviewAttributes(
                    values.resolveAttributes(injectHandler.getNamespacedAttributes()),
                    values::color);
        }
        if (view instanceof EditorListItem listItem) {
            String listitem = injectHandler.getAttributeValueOf("listitem");
            String itemCount = injectHandler.getAttributeValueOf("itemCount");
            if (!TextUtils.isEmpty(listitem)) {
                listItem.setListItem(android.R.layout.simple_list_item_1);
            }
            if (crashlytics != null)
                crashlytics.log("ViewPane: setting item count to EditorListItem");
            if (!TextUtils.isEmpty(itemCount)) {
                if (TextUtils.isEmpty(listitem)) {
                    try {
                        listItem.setItemCount(Integer.parseInt(itemCount));
                    } catch (NumberFormatException exception) {
                        if (crashlytics != null) crashlytics.recordException(exception);
                    }
                }
            }
        }
    }

    private void updatePreviewBackgroundImage(View view, String resource) {
        if (resource == null) return;
        try {
            if (resourcesManager.getImageResType(resource)
                    == ProjectResourceBean.PROJECT_RES_TYPE_RESOURCE) {
                int id =
                        getResources()
                                .getIdentifier(resource, "drawable", getContext().getPackageName());
                if (id == 0) return;
                if (view instanceof ItemCardView || view instanceof ItemMaterialButton) {
                    setPreviewBackground(
                            view, AppCompatResources.getDrawable(view.getContext(), id));
                } else {
                    view.setBackgroundResource(id);
                }
                return;
            }

            String path = resourcesManager.getImagePath(resource);
            if (path == null || !new File(path).exists()) return;
            if (path.endsWith(".9.png")) {
                Drawable background =
                        NinePatchDecoder.decodeDrawable(
                                getResources(), path, android.util.DisplayMetrics.DENSITY_XHIGH);
                if (background != null) setPreviewBackground(view, background);
            } else {
                Bitmap bitmap = FileUtil.decodeSampleBitmapFromPath(path, 512, 512);
                if (bitmap != null) {
                    setPreviewBackground(view, new BitmapDrawable(getResources(), bitmap));
                }
            }
        } catch (Exception e) {
            Log.e("ViewPane", e.getMessage(), e);
        }
    }

    private void setPreviewBackground(View view, Drawable drawable) {
        if (view instanceof ItemCardView card) {
            card.setPreviewBackgroundImage(drawable);
        } else if (view instanceof ItemMaterialButton button) {
            button.setPreviewBackgroundImage(drawable);
        } else {
            view.setBackground(drawable);
        }
    }

    private PreviewValues previewValues() {
        if (previewValues == null) {
            File defaults =
                    sc_id == null || sc_id.isEmpty()
                            ? null
                            : new File(SketchwarePaths.getProjectResourceValuesPath(sc_id));
            File night =
                    defaults != null && material3LibraryManager.usesNightResources()
                            ? new File(
                                    SketchwarePaths.getProjectResourceValuesPath(sc_id, "-night"))
                            : null;
            previewValues = new PreviewValues(context, defaults, night, colorResourceResolver);
        }
        return previewValues;
    }

    private void updateVisibility(View view, InjectAttributeHandler injectHandler) {
        String visibility = injectHandler.getNamespacedAttributes().get("android:visibility");
        if (TextUtils.isEmpty(visibility)) {
            visibility = injectHandler.getBean().parentAttributes.get("android:visibility");
        }
        if ("invisible".equals(visibility)) {
            view.setVisibility(INVISIBLE);
        } else if ("gone".equals(visibility)) {
            view.setVisibility(GONE);
        } else {
            view.setVisibility(VISIBLE);
        }
    }

    public ItemView findItemViewByTag(String tag) {
        if (tag == null || tag.isEmpty()) {
            return null;
        }
        View findViewWithTag = null;
        if (tag.charAt(0) == '_') {
            findViewWithTag = findViewWithTag(tag);
        } else {
            if (rootLayout != null) {
                findViewWithTag = rootLayout.findViewWithTag(tag);
            }
        }
        if (findViewWithTag instanceof ItemView) {
            return (ItemView) findViewWithTag;
        }
        return null;
    }

    public void updateViewBeanProperties(ViewBean viewBean, int index, int depth) {
        if (viewInfo != null) {
            View view = viewInfo.view();
            if (view instanceof LinearLayout) {
                viewBean.preIndex = viewBean.index;
                viewBean.index = viewInfo.index();
                viewBean.preParent = viewBean.parent;
                viewBean.parent = view.getTag() != null ? view.getTag().toString() : "";
                viewBean.preParentType = viewBean.parentType;
                viewBean.parentType = ViewBean.VIEW_TYPE_LAYOUT_LINEAR;
            } else if (view instanceof ItemVerticalScrollView) {
                viewBean.preIndex = viewBean.index;
                viewBean.index = viewInfo.index();
                viewBean.preParent = viewBean.parent;
                viewBean.parent = view.getTag() != null ? view.getTag().toString() : "";
                viewBean.preParentType = viewBean.parentType;
                viewBean.parentType = ViewBean.VIEW_TYPE_LAYOUT_VSCROLLVIEW;
                viewBean.layout.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            } else if (view instanceof ItemHorizontalScrollView) {
                viewBean.preIndex = viewBean.index;
                viewBean.index = viewInfo.index();
                viewBean.preParent = viewBean.parent;
                viewBean.parent = view.getTag() != null ? view.getTag().toString() : "";
                viewBean.preParentType = viewBean.parentType;
                viewBean.parentType = ViewBean.VIEW_TYPE_LAYOUT_HSCROLLVIEW;
                viewBean.layout.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            } else if (view instanceof ItemCardView) {
                viewBean.preIndex = viewBean.index;
                viewBean.index = viewInfo.index();
                viewBean.preParent = viewBean.parent;
                viewBean.parent = view.getTag() != null ? view.getTag().toString() : "";
                viewBean.preParentType = viewBean.parentType;
                viewBean.parentType = ViewBeans.VIEW_TYPE_LAYOUT_CARDVIEW;
                viewBean.layout.width = ViewGroup.LayoutParams.MATCH_PARENT;
            } else if (view instanceof ItemRelativeLayout) {
                viewBean.preIndex = viewBean.index;
                viewBean.index = viewInfo.index();
                viewBean.preParent = viewBean.parent;
                viewBean.parent = view.getTag() != null ? view.getTag().toString() : "";
                viewBean.preParentType = viewBean.parentType;
                viewBean.parentType = ViewBean.VIEW_TYPE_LAYOUT_RELATIVE;
            }
        } else {
            viewBean.preIndex = viewBean.index;
            viewBean.preParent = viewBean.parent;
            viewBean.parent = "root";
            viewBean.preParentType = viewBean.parentType;
            if (rootLayout instanceof ItemView sy) {
                viewBean.parentType = sy.getBean().type;
            } else {
                viewBean.parentType = ViewBean.VIEW_TYPE_LAYOUT_LINEAR;
            }
            viewBean.index = -1;
        }
    }

    public View addFab(ViewBean viewBean) {
        View findViewWithTag = findViewWithTag("_fab");
        if (findViewWithTag != null) {
            return findViewWithTag;
        }
        ItemFloatingActionButton itemFloatingActionButton = new ItemFloatingActionButton(context);
        itemFloatingActionButton.setTag("_fab");
        itemFloatingActionButton.setLayoutParams(
                new LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        if (!isMaterial3Preview()) {
            itemFloatingActionButton.setMainColor(
                    ProjectThemeColors.getColor(sc_id, ProjectThemeColors.COLOR_ACCENT));
        }
        itemFloatingActionButton.setFixed(true);
        if (viewBean == null) {
            ViewBean defaultFabBean = new ViewBean("_fab", ViewBean.VIEW_TYPE_WIDGET_FAB);
            LayoutBean layoutBean = defaultFabBean.layout;
            layoutBean.marginLeft = 16;
            layoutBean.marginTop = 16;
            layoutBean.marginRight = 16;
            layoutBean.marginBottom = 16;
            layoutBean.layoutGravity = Gravity.RIGHT | Gravity.BOTTOM;
            itemFloatingActionButton.setBean(defaultFabBean);
        } else {
            itemFloatingActionButton.setBean(viewBean);
        }
        addView(itemFloatingActionButton);
        updateItemView(itemFloatingActionButton, itemFloatingActionButton.getBean());
        return itemFloatingActionButton;
    }

    public void resetView(boolean shouldClearViewInfo) {
        highlightedTextView.setVisibility(View.GONE);
        ViewParent parent = highlightedTextView.getParent();
        if (parent != null) {
            ((ViewGroup) parent).removeView(highlightedTextView);
        }
        if (shouldClearViewInfo) {
            viewInfo = null;
        }
    }

    public void updateView(int x, int y, int width, int height) {
        ViewInfo viewInfo = getViewInfo(x, y);
        if (viewInfo == null) {
            resetView(true);
        } else if (this.viewInfo != viewInfo) {
            resetView(true);
            ViewGroup viewGroup = (ViewGroup) viewInfo.view();
            if (viewGroup instanceof ItemLinearLayout linearLayout) {
                int childIndex =
                        getLinearInsertionChildIndex(linearLayout, viewInfo.index(), draggedViewId);
                // Use the actual child position, without the legacy addView overload's GONE
                // adjustment.
                linearLayout.addView(
                        highlightedTextView,
                        childIndex,
                        new LinearLayout.LayoutParams(width, height));
            } else {
                viewGroup.addView(highlightedTextView, viewInfo.index());
                if (viewGroup instanceof FrameLayout) {
                    highlightedTextView.setLayoutParams(
                            new FrameLayout.LayoutParams(width, height));
                } else {
                    highlightedTextView.setLayoutParams(new LayoutParams(width, height));
                }
            }
            highlightedTextView.setVisibility(View.VISIBLE);
            this.viewInfo = viewInfo;
        }
    }

    private ViewInfo getViewInfo(int x, int y) {
        ViewInfo result = null;
        int highestPriority = -1;
        for (ViewInfo viewInfo : viewInfos) {
            if (viewInfo.rect().contains(x, y) && highestPriority < viewInfo.depth()) {
                highestPriority = viewInfo.depth();
                result = viewInfo;
            }
        }
        return result;
    }

    private void addDroppableForLinearLayout(ViewBean draggedView, ItemLinearLayout linearLayout) {
        Rect parentRect = getBoundsOnScreen(linearLayout);
        int depth = calculateViewDepth(linearLayout);
        addViewInfo(parentRect, linearLayout, -1, depth);
        boolean vertical = linearLayout.getOrientation() == LinearLayout.VERTICAL;
        boolean rtl = !vertical && linearLayout.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        float scale =
                vertical
                        ? parentRect.height() / (float) Math.max(1, linearLayout.getHeight())
                        : parentRect.width() / (float) Math.max(1, linearLayout.getWidth());
        int previousEnd = vertical ? parentRect.top : rtl ? parentRect.right : parentRect.left;
        int childIndex = 0;
        for (int i = 0; i < linearLayout.getChildCount(); i++) {
            View child = linearLayout.getChildAt(i);
            if (!(child instanceof ItemView)
                    || (draggedView != null
                            && draggedView.id != null
                            && draggedView.id.equals(child.getTag()))) {
                continue;
            }
            if (child.getVisibility() == View.VISIBLE) {
                Rect childRect = getBoundsOnScreen(child);
                LinearLayout.LayoutParams params =
                        (LinearLayout.LayoutParams) child.getLayoutParams();
                int end =
                        vertical
                                ? childRect.bottom + Math.round(params.bottomMargin * scale)
                                : rtl
                                        ? childRect.left - Math.round(params.leftMargin * scale)
                                        : childRect.right + Math.round(params.rightMargin * scale);
                Rect insertionRect =
                        vertical
                                ? new Rect(
                                        parentRect.left,
                                        Math.min(previousEnd, childRect.top),
                                        parentRect.right,
                                        end)
                                : rtl
                                        ? new Rect(
                                                end,
                                                parentRect.top,
                                                Math.max(previousEnd, childRect.right),
                                                parentRect.bottom)
                                        : new Rect(
                                                Math.min(previousEnd, childRect.left),
                                                parentRect.top,
                                                end,
                                                parentRect.bottom);
                addViewInfo(insertionRect, linearLayout, childIndex, depth + 1);
                previousEnd = end;

                if (child instanceof ItemLinearLayout layout) {
                    addDroppableForLinearLayout(draggedView, layout);
                } else if (child instanceof ItemHorizontalScrollView
                        || child instanceof ItemVerticalScrollView
                        || child instanceof ItemCardView) {
                    addDroppableForScrollableContainer(draggedView, (ViewGroup) child);
                } else if (child instanceof ItemRelativeLayout layout) {
                    addDroppableForViewGroup(draggedView, layout);
                }
            }
            // GONE and INVISIBLE items still occupy a logical position in the saved child order.
            childIndex++;
        }
    }

    private Rect getBoundsOnScreen(View view) {
        Matrix transform = new Matrix();
        View current = view;
        while (true) {
            transform.postConcat(current.getMatrix());
            if (!(current.getParent() instanceof View parent)) break;
            current = parent;
        }
        RectF bounds = new RectF(0, 0, view.getWidth(), view.getHeight());
        transform.mapRect(bounds);
        float[] origin = {0, 0};
        transform.mapPoints(origin);
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        // getLocationOnScreen supplies ancestor offsets and scrolling; the matrix supplies size and
        // scale.
        bounds.offset(location[0] - origin[0], location[1] - origin[1]);
        Rect result = new Rect();
        bounds.roundOut(result);
        return result;
    }

    private int getLinearInsertionChildIndex(
            ItemLinearLayout layout, int index, String excludedId) {
        if (index < 0) return -1;
        int itemIndex = 0;
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if (child instanceof ItemView
                    && (excludedId == null || !excludedId.equals(child.getTag()))) {
                if (itemIndex == index) return i;
                itemIndex++;
            }
        }
        return -1;
    }

    private void addDroppableForViewGroup(ViewBean viewBean, ViewGroup viewGroup) {
        addViewInfo(getBoundsOnScreen(viewGroup), viewGroup, -1, calculateViewDepth(viewGroup));
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View childAt = viewGroup.getChildAt(i);
            if (childAt != null
                    && childAt.getTag() != null
                    && (viewBean == null
                            || viewBean.id == null
                            || !childAt.getTag().equals(viewBean.id))
                    && childAt.getVisibility() == View.VISIBLE) {
                if (childAt instanceof ItemLinearLayout) {
                    addDroppableForLinearLayout(viewBean, (ItemLinearLayout) childAt);
                } else if (childAt instanceof ItemHorizontalScrollView) {
                    addDroppableForScrollableContainer(viewBean, (ViewGroup) childAt);
                } else if (childAt instanceof ItemVerticalScrollView) {
                    addDroppableForScrollableContainer(viewBean, (ViewGroup) childAt);
                } else if (childAt instanceof ItemCardView) {
                    addDroppableForScrollableContainer(viewBean, (ViewGroup) childAt);
                } else if (childAt instanceof ItemRelativeLayout relativeLayout) {
                    addDroppableForViewGroup(viewBean, relativeLayout);
                }
            }
        }
    }

    private void addDroppableForScrollableContainer(ViewBean viewBean, ViewGroup viewGroup) {
        int childCount = viewGroup.getChildCount();
        int index = 0;
        for (int i = 0; i < childCount; i++) {
            View childAt = viewGroup.getChildAt(i);
            if (childAt != null
                    && childAt.getTag() != null
                    && (viewBean == null
                            || viewBean.id == null
                            || !childAt.getTag().equals(viewBean.id))
                    && childAt.getVisibility() == View.VISIBLE) {
                index++;
                if (childAt instanceof ItemLinearLayout) {
                    addDroppableForLinearLayout(viewBean, (ItemLinearLayout) childAt);
                } else if (childAt instanceof ItemHorizontalScrollView) {
                    addDroppableForScrollableContainer(viewBean, (ViewGroup) childAt);
                } else if (childAt instanceof ItemVerticalScrollView) {
                    addDroppableForScrollableContainer(viewBean, (ViewGroup) childAt);
                } else if (childAt instanceof ItemCardView) {
                    addDroppableForScrollableContainer(viewBean, (ViewGroup) childAt);
                } else if (childAt instanceof ItemRelativeLayout relativeLayout) {
                    addDroppableForViewGroup(viewBean, relativeLayout);
                }
            }
        }
        if (index < 1) {
            addViewInfo(getBoundsOnScreen(viewGroup), viewGroup, -1, calculateViewDepth(viewGroup));
        }
    }

    private void addViewInfo(Rect rect, View view, int index, int depth) {
        viewInfos.add(new ViewInfo(rect, view, index, depth));
    }

    public void addViewAndUpdateIndex(View view) {
        ViewBean bean = ((ItemView) view).getBean();
        if (rootLayout != null) {
            ViewGroup viewGroup = rootLayout.findViewWithTag(bean.parent);
            if (viewGroup instanceof ItemLinearLayout linearLayout) {
                int childIndex = getLinearInsertionChildIndex(linearLayout, bean.index, null);
                linearLayout.addView(view, childIndex, view.getLayoutParams());
            } else {
                viewGroup.addView(view, bean.index);
            }
            if (viewGroup instanceof RelativeLayout) {
                updateRelativeParentViews(view, new InjectAttributeHandler(bean));
            }
            if (viewGroup instanceof ScrollContainer scrollContainer) {
                scrollContainer.reindexChildren();
            }
        }
    }

    private int getActualParentType(View view, int defaultValue) {
        var parent = view.getParent();
        if (parent != null) {
            if (parent instanceof LinearLayout) {
                return ViewBean.VIEW_TYPE_LAYOUT_LINEAR;
            } else if (parent instanceof RelativeLayout) {
                return ViewBean.VIEW_TYPE_LAYOUT_RELATIVE;
            } else if (parent instanceof ItemCardView) {
                return ViewBeans.VIEW_TYPE_LAYOUT_CARDVIEW;
            } else if (parent instanceof ItemHorizontalScrollView) {
                return ViewBean.VIEW_TYPE_LAYOUT_HSCROLLVIEW;
            } else if (parent instanceof ItemVerticalScrollView) {
                return ViewBean.VIEW_TYPE_LAYOUT_VSCROLLVIEW;
            }
        }
        return defaultValue;
    }

    private void applyElevation(View view, int elevationDp) {
        if (elevationDp > 0) {
            float elevationPx = ViewUtil.dpToPx(getContext(), elevationDp);
            view.setElevation(elevationPx);
            Drawable bg = view.getBackground();
            boolean useBoundsOutline = shouldUseBoundsOutline(view, bg);
            if (!useBoundsOutline && bg instanceof ColorDrawable colorDrawable) {
                GradientDrawable gd = new GradientDrawable();
                gd.setColor(colorDrawable.getColor());
                gd.setShape(GradientDrawable.RECTANGLE);
                view.setBackground(gd);
            }
            view.setOutlineProvider(
                    useBoundsOutline ? ViewOutlineProvider.BOUNDS : ViewOutlineProvider.BACKGROUND);
            view.invalidateOutline();
            if (view.getParent() != null) {
                disableClipInAncestors(view);
            } else {
                view.addOnAttachStateChangeListener(
                        new View.OnAttachStateChangeListener() {
                            @Override
                            public void onViewAttachedToWindow(View v) {
                                disableClipInAncestors(v);
                                v.removeOnAttachStateChangeListener(this);
                            }

                            @Override
                            public void onViewDetachedFromWindow(View v) {}
                        });
            }
        } else if (view.getElevation() != 0) {
            view.setElevation(0);
            view.invalidateOutline();
        }
    }

    private int getInjectedElevationDp(InjectAttributeHandler handler) {
        String elevation = handler.getAttributeValueOf("elevation");
        return elevation.isEmpty() ? -1 : PropertiesUtil.resolveSize(elevation, 0);
    }

    private int resolvePreviewElevation(InjectAttributeHandler handler, int fallbackDp) {
        int injectedElevation = getInjectedElevationDp(handler);
        return injectedElevation >= 0 ? injectedElevation : fallbackDp;
    }

    private boolean shouldUseBoundsOutline(View view, Drawable background) {
        if (background == null) {
            return true;
        }
        if (background instanceof ColorDrawable colorDrawable) {
            return Color.alpha(colorDrawable.getColor()) == 0;
        }
        return background.getAlpha() == 0;
    }

    private void disableClipInAncestors(View view) {
        ViewGroup parent =
                view.getParent() instanceof ViewGroup ? (ViewGroup) view.getParent() : null;
        int maxHops = 16;
        while (parent != null && maxHops-- > 0) {
            parent.setClipChildren(false);
            parent.setClipToPadding(false);
            if (parent == this) {
                break;
            }
            parent =
                    parent.getParent() instanceof ViewGroup ? (ViewGroup) parent.getParent() : null;
        }
    }

    private int layoutDpToPixels(int dp) {
        // LayoutBean stores whole dp. Match Android's positive dimension pixel-size rounding.
        float pixels = ViewUtil.dpToPx(getContext(), (float) dp);
        if (dp <= 0) return (int) pixels;
        return Math.max(1, (int) (pixels + 0.5f));
    }

    private void updatePreviewBackgroundColor(View view, InjectAttributeHandler handler) {
        LayoutBean layout = handler.getBean().layout;
        boolean material3Button = view instanceof ItemMaterialButton && isMaterial3Preview();
        // Clear the old image before restoring the color or loading a new image.
        if (view instanceof ItemMaterialButton button) {
            button.setPreviewBackgroundImage(null);
        }
        if (view instanceof ItemCardView card) {
            card.setPreviewBackgroundImage(null);
            updateCardView(card, handler);
        } else if (!material3Button && layout.backgroundResColor == null) {
            int color = layout.backgroundColor;
            boolean keepsWidgetBackground =
                    view instanceof ItemButton
                            || view instanceof ItemEditText
                            || view instanceof ItemMaterialButton;
            if ((color == 0xffffff || color == 0) && !keepsWidgetBackground) {
                view.setBackground(null);
            } else {
                view.setBackgroundColor(color);
            }
        } else if (!material3Button) {
            view.setBackgroundColor(
                    PropertiesUtil.parseColor(
                            colorResourceResolver.getColorValue(
                                    context,
                                    layout.backgroundResColor,
                                    3,
                                    material3LibraryManager.usesNightResources())));
        }
    }

    private void updateLayoutParams(View view, ViewBean viewBean) {
        if (crashlytics != null) crashlytics.log("ViewPane: Updating layout");
        LayoutBean layout = viewBean.layout;
        int width = layout.width > 0 ? layoutDpToPixels(layout.width) : layout.width;
        int height = layout.height > 0 ? layoutDpToPixels(layout.height) : layout.height;
        int leftMargin = layoutDpToPixels(layout.marginLeft);
        int topMargin = layoutDpToPixels(layout.marginTop);
        int rightMargin = layoutDpToPixels(layout.marginRight);
        int bottomMargin = layoutDpToPixels(layout.marginBottom);

        // Attached children must use their real parent's parameters, even if stored metadata is
        // stale.
        int effectiveParentType = getActualParentType(view, viewBean.parentType);
        switch (effectiveParentType) {
            case ViewBeans.VIEW_TYPE_LAYOUT_COLLAPSINGTOOLBARLAYOUT,
                    ViewBeans.VIEW_TYPE_LAYOUT_TEXTINPUTLAYOUT,
                    ViewBeans.VIEW_TYPE_LAYOUT_SWIPEREFRESHLAYOUT,
                    ViewBeans.VIEW_TYPE_LAYOUT_RADIOGROUP ->
                    effectiveParentType = ViewBean.VIEW_TYPE_LAYOUT_LINEAR;
        }

        if (effectiveParentType == ViewBean.VIEW_TYPE_LAYOUT_LINEAR) {
            LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(width, height);
            layoutParams2.setMargins(leftMargin, topMargin, rightMargin, bottomMargin);
            int layoutGravity = viewBean.layout.layoutGravity;
            if (layoutGravity != LayoutBean.GRAVITY_NONE) {
                layoutParams2.gravity = layoutGravity;
            }
            layoutParams2.weight = viewBean.layout.weight;
            view.setLayoutParams(layoutParams2);
        } else if (effectiveParentType == ViewBean.VIEW_TYPE_LAYOUT_RELATIVE) {
            RelativeLayout.LayoutParams layoutParams2 =
                    new RelativeLayout.LayoutParams(width, height);
            layoutParams2.setMargins(leftMargin, topMargin, rightMargin, bottomMargin);
            view.setLayoutParams(layoutParams2);
        } else {
            FrameLayout.LayoutParams layoutParams3 = new FrameLayout.LayoutParams(width, height);
            layoutParams3.setMargins(leftMargin, topMargin, rightMargin, bottomMargin);
            int layoutGravity = viewBean.layout.layoutGravity;
            if (layoutGravity != LayoutBean.GRAVITY_NONE) {
                layoutParams3.gravity = layoutGravity;
            }
            view.setLayoutParams(layoutParams3);
        }
    }

    private void updateRelativeParentViews(View view, InjectAttributeHandler handler) {
        var viewBean = handler.getBean();
        updateRelative(view, handler);

        ViewGroup parent = rootLayout.findViewWithTag(viewBean.parent);
        if (parent == null) {
            return;
        }

        for (int i = 0; i < parent.getChildCount(); i++) {
            var child = parent.getChildAt(i);
            if (child instanceof ItemView editorItem) {
                updateRelative(child, new InjectAttributeHandler(editorItem.getBean()));
            }
        }
    }

    private void updateRelative(View view, InjectAttributeHandler handler) {
        String layout_centerInParent = handler.getAttributeValueOf("layout_centerInParent");
        String layout_centerVertical = handler.getAttributeValueOf("layout_centerVertical");
        String layout_centerHorizontal = handler.getAttributeValueOf("layout_centerHorizontal");

        var bean = handler.getBean();
        var parent = bean.parentAttributes;
        if (Boolean.parseBoolean(layout_centerInParent)
                || parent.containsKey("android:layout_centerInParent")
                        && Boolean.parseBoolean(parent.get("android:layout_centerInParent")))
            InvokeUtil.invoke(
                    view.getLayoutParams(),
                    "addRule",
                    new Class[] {int.class},
                    RelativeLayout.CENTER_IN_PARENT);

        if (Boolean.parseBoolean(layout_centerVertical)
                || parent.containsKey("android:layout_centerVertical")
                        && Boolean.parseBoolean(parent.get("android:layout_centerVertical")))
            InvokeUtil.invoke(
                    view.getLayoutParams(),
                    "addRule",
                    new Class[] {int.class},
                    RelativeLayout.CENTER_VERTICAL);

        if (Boolean.parseBoolean(layout_centerHorizontal)
                || parent.containsKey("android:layout_centerHorizontal")
                        && Boolean.parseBoolean(parent.get("android:layout_centerHorizontal")))
            InvokeUtil.invoke(
                    view.getLayoutParams(),
                    "addRule",
                    new Class[] {int.class},
                    RelativeLayout.CENTER_HORIZONTAL);

        String layout_alignParentStart = handler.getAttributeValueOf("layout_alignParentStart");
        String layout_alignParentRight = handler.getAttributeValueOf("layout_alignParentRight");
        String layout_alignParentTop = handler.getAttributeValueOf("layout_alignParentTop");
        String layout_alignParentEnd = handler.getAttributeValueOf("layout_alignParentEnd");
        String layout_alignParentLeft = handler.getAttributeValueOf("layout_alignParentLeft");
        String layout_alignParentBottom = handler.getAttributeValueOf("layout_alignParentBottom");

        if (Boolean.parseBoolean(layout_alignParentStart)
                || parent.containsKey("android:layout_alignParentStart")
                        && Boolean.parseBoolean(parent.get("android:layout_alignParentStart"))) {
            InvokeUtil.invoke(
                    view.getLayoutParams(),
                    "addRule",
                    new Class[] {int.class},
                    RelativeLayout.ALIGN_PARENT_START);
        }

        if (Boolean.parseBoolean(layout_alignParentRight)
                || parent.containsKey("android:layout_alignParentRight")
                        && Boolean.parseBoolean(parent.get("android:layout_alignParentRight"))) {
            InvokeUtil.invoke(
                    view.getLayoutParams(),
                    "addRule",
                    new Class[] {int.class},
                    RelativeLayout.ALIGN_PARENT_RIGHT);
        }

        if (Boolean.parseBoolean(layout_alignParentTop)
                || parent.containsKey("android:layout_alignParentTop")
                        && Boolean.parseBoolean(parent.get("android:layout_alignParentTop"))) {
            InvokeUtil.invoke(
                    view.getLayoutParams(),
                    "addRule",
                    new Class[] {int.class},
                    RelativeLayout.ALIGN_PARENT_TOP);
        }

        if (Boolean.parseBoolean(layout_alignParentEnd)
                || parent.containsKey("android:layout_alignParentEnd")
                        && Boolean.parseBoolean(parent.get("android:layout_alignParentEnd"))) {
            InvokeUtil.invoke(
                    view.getLayoutParams(),
                    "addRule",
                    new Class[] {int.class},
                    RelativeLayout.ALIGN_PARENT_END);
        }

        if (Boolean.parseBoolean(layout_alignParentLeft)
                || parent.containsKey("android:layout_alignParentLeft")
                        && Boolean.parseBoolean(parent.get("android:layout_alignParentLeft"))) {
            InvokeUtil.invoke(
                    view.getLayoutParams(),
                    "addRule",
                    new Class[] {int.class},
                    RelativeLayout.ALIGN_PARENT_LEFT);
        }

        if (Boolean.parseBoolean(layout_alignParentBottom)
                || parent.containsKey("android:layout_alignParentBottom")
                        && Boolean.parseBoolean(parent.get("android:layout_alignParentBottom"))) {
            InvokeUtil.invoke(
                    view.getLayoutParams(),
                    "addRule",
                    new Class[] {int.class},
                    RelativeLayout.ALIGN_PARENT_BOTTOM);
        }

        if (parent.containsKey("android:layout_alignStart")) {
            setRelativeRule(
                    view, parent.get("android:layout_alignStart"), RelativeLayout.ALIGN_START);
        } else setRelativeRule(view, handler, "layout_alignStart", RelativeLayout.ALIGN_START);
        if (parent.containsKey("android:layout_alignRight")) {
            setRelativeRule(
                    view, parent.get("android:layout_alignRight"), RelativeLayout.ALIGN_RIGHT);
        } else setRelativeRule(view, handler, "layout_alignRight", RelativeLayout.ALIGN_RIGHT);
        if (parent.containsKey("android:layout_alignTop")) {
            setRelativeRule(view, parent.get("android:layout_alignTop"), RelativeLayout.ALIGN_TOP);
        } else setRelativeRule(view, handler, "layout_alignTop", RelativeLayout.ALIGN_TOP);
        if (parent.containsKey("android:layout_alignEnd")) {
            setRelativeRule(view, parent.get("android:layout_alignEnd"), RelativeLayout.ALIGN_END);
        } else setRelativeRule(view, handler, "layout_alignEnd", RelativeLayout.ALIGN_END);
        if (parent.containsKey("android:layout_alignLeft")) {
            setRelativeRule(
                    view, parent.get("android:layout_alignLeft"), RelativeLayout.ALIGN_LEFT);
        } else setRelativeRule(view, handler, "layout_alignLeft", RelativeLayout.ALIGN_LEFT);
        if (parent.containsKey("android:layout_alignBottom")) {
            setRelativeRule(
                    view, parent.get("android:layout_alignBottom"), RelativeLayout.ALIGN_BOTTOM);
        } else setRelativeRule(view, handler, "layout_alignBottom", RelativeLayout.ALIGN_BOTTOM);
        if (parent.containsKey("android:layout_alignBaseline")) {
            setRelativeRule(
                    view,
                    parent.get("android:layout_alignBaseline"),
                    RelativeLayout.ALIGN_BASELINE);
        } else
            setRelativeRule(view, handler, "layout_alignBaseline", RelativeLayout.ALIGN_BASELINE);

        if (parent.containsKey("android:layout_above")) {
            setRelativeRule(view, parent.get("android:layout_above"), RelativeLayout.ABOVE);
        } else setRelativeRule(view, handler, "layout_above", RelativeLayout.ABOVE);
        if (parent.containsKey("android:layout_below")) {
            setRelativeRule(view, parent.get("android:layout_below"), RelativeLayout.BELOW);
        } else setRelativeRule(view, handler, "layout_below", RelativeLayout.BELOW);
        if (parent.containsKey("android:layout_toStartOf")) {
            setRelativeRule(view, parent.get("android:layout_toStartOf"), RelativeLayout.START_OF);
        } else setRelativeRule(view, handler, "layout_toStartOf", RelativeLayout.START_OF);
        if (parent.containsKey("android:layout_toRightOf")) {
            setRelativeRule(view, parent.get("android:layout_toRightOf"), RelativeLayout.RIGHT_OF);
        } else setRelativeRule(view, handler, "layout_toRightOf", RelativeLayout.RIGHT_OF);
        if (parent.containsKey("android:layout_toEndOf")) {
            setRelativeRule(view, parent.get("android:layout_toEndOf"), RelativeLayout.END_OF);
        } else setRelativeRule(view, handler, "layout_toEndOf", RelativeLayout.END_OF);
        if (parent.containsKey("android:layout_toLeftOf")) {
            setRelativeRule(view, parent.get("android:layout_toLeftOf"), RelativeLayout.LEFT_OF);
        } else setRelativeRule(view, handler, "layout_toLeftOf", RelativeLayout.LEFT_OF);
    }

    private void setRelativeRule(
            View view, InjectAttributeHandler handler, String attribute, int rule) {
        String referenceId = handler.getAttributeValueOf(attribute);
        if (referenceId != null && !referenceId.isEmpty()) {
            var reference = PropertiesUtil.getUnitOrPrefix(referenceId);
            if (reference != null) {
                setRelativeRule(view, reference.second, rule);
            }
        }
    }

    private void setRelativeRule(View view, String id, int rule) {
        View refView = rootLayout.findViewWithTag(id);
        if (refView != null) {
            InvokeUtil.invoke(
                    view.getLayoutParams(),
                    "addRule",
                    new Class[] {int.class, int.class},
                    rule,
                    refView.getId());
        }
    }

    private String getImportedFontPath(String fontName) {
        if (importedFontPathCache == null) {
            importedFontPathCache = loadImportedFontPaths();
        }
        return importedFontPathCache.getOrDefault(fontName, "");
    }

    private HashMap<String, String> loadImportedFontPaths() {
        HashMap<String, String> fontPaths = new HashMap<>();
        File fontDirectory = new File(SketchwarePaths.getProjectResourcePath(sc_id), "font");
        File[] fontFiles = fontDirectory.listFiles();
        if (fontFiles == null) {
            return fontPaths;
        }
        for (File fontFile : fontFiles) {
            if (fontFile.isFile()) {
                fontPaths.put(
                        FileUtil.getFileNameNoExtension(fontFile.getName()),
                        fontFile.getAbsolutePath());
            }
        }
        return fontPaths;
    }

    private void updateTextView(TextView textView, ViewBean viewBean) {
        String textContent = viewBean.text.text;
        if (textContent != null && textContent.contains("\\n")) {
            textContent = viewBean.text.text.replace("\\n", "\n");
        }
        textView.setText(
                textContent.startsWith(stringsStart) ? getXmlString(textContent) : textContent);
        String textFont = new InjectAttributeHandler(viewBean).getAttributeValueOf("fontFamily");
        if (textFont != null && !textFont.isEmpty()) {
            if (textFont.startsWith("@font/")) {
                textFont = textFont.substring(6);
                String textFontPath = getImportedFontPath(textFont);
                textView.setTypeface(
                        textFontPath != null
                                        && !textFontPath.isEmpty()
                                        && new File(textFontPath).exists()
                                ? Typeface.createFromFile(textFontPath)
                                : null,
                        viewBean.text.textType);
            } else {
                textView.setTypeface(null, viewBean.text.textType);
            }
        } else {
            textView.setTypeface(null, viewBean.text.textType);
        }
        if (viewBean.text.resTextColor == null) {
            if (viewBean.text.textColor == 0xffffff) {
                textView.setTextColor(
                        (ColorStateList) textView.getTag(R.id.preview_default_text_colors));
            } else {
                textView.setTextColor(viewBean.text.textColor);
            }
        } else {
            textView.setTextColor(
                    PropertiesUtil.parseColor(
                            colorResourceResolver.getColorValue(
                                    context,
                                    viewBean.text.resTextColor,
                                    3,
                                    material3LibraryManager.usesNightResources())));
        }
        textView.setTextSize(viewBean.text.textSize);
        textView.setLines(viewBean.text.line);
        textView.setSingleLine(viewBean.text.singleLine != 0);
    }

    public String getXmlString(String key) {
        if (sc_id == null) {
            return key;
        }
        if (xmlStringCache == null || !sc_id.equals(xmlStringCacheScId)) {
            xmlStringCache = new HashMap<>();
            xmlStringCacheScId = sc_id;
            String filePath =
                    SketchwarePaths.getProjectResourceValuesFilePath(sc_id, "strings.xml");
            ArrayList<HashMap<String, Object>> stringsListMap = new ArrayList<>();
            StringResourceResolver stringResourceResolver = new StringResourceResolver();
            stringResourceResolver.convertXmlStringsToListMap(
                    FileUtil.readFileIfExist(filePath), stringsListMap);
            for (HashMap<String, Object> map : stringsListMap) {
                Object keyObj = map.get("key");
                Object textObj = map.get("text");
                if (keyObj != null && textObj != null) {
                    String k = stringsStart + keyObj.toString().trim();
                    xmlStringCache.put(k, textObj.toString());
                }
            }
        }
        if (key.equals("@string/app_name") && !xmlStringCache.containsKey(key)) {
            return MapValueHelper.getString(
                    ProjectListManager.getProjectById(sc_id), "my_app_name");
        }
        String value = xmlStringCache.get(key);
        return value != null ? value : key;
    }

    public void invalidateXmlStringCache() {
        xmlStringCache = null;
    }

    private void updateEditText(EditText editText, ViewBean viewBean) {
        String hintText = viewBean.text.hint;
        editText.setHint(hintText.startsWith(stringsStart) ? getXmlString(hintText) : hintText);
        if (viewBean.text.resHintColor == null) {
            if (viewBean.text.hintColor == 0xffffff) {
                editText.setHintTextColor(
                        (ColorStateList) editText.getTag(R.id.preview_default_hint_colors));
            } else {
                editText.setHintTextColor(viewBean.text.hintColor);
            }
        } else {
            editText.setHintTextColor(
                    PropertiesUtil.parseColor(
                            colorResourceResolver.getColorValue(
                                    context,
                                    viewBean.text.resHintColor,
                                    3,
                                    material3LibraryManager.usesNightResources())));
        }
    }

    private void updateCardView(ItemCardView cardView, InjectAttributeHandler handler) {
        var bean = handler.getBean();
        String cardBackgroundColor = handler.getAttributeValueOf("cardBackgroundColor");
        String cardElevation = handler.getAttributeValueOf("cardElevation");
        String cardCornerRadius = handler.getAttributeValueOf("cardCornerRadius");
        String compatPadding = handler.getAttributeValueOf("cardUseCompatPadding");
        String strokeColor = handler.getAttributeValueOf("strokeColor");
        String strokeWidth = handler.getAttributeValueOf("strokeWidth");

        if (cardBackgroundColor.isEmpty()) {
            if (bean.layout.backgroundResColor == null) {
                if (bean.layout.backgroundColor == 0xffffff) {
                    cardView.restoreDefaultCardColor();
                } else {
                    cardView.setCardBackgroundColor(bean.layout.backgroundColor);
                }
            } else {
                cardView.setCardBackgroundColor(
                        PropertiesUtil.parseColor(
                                colorResourceResolver.getColorValue(
                                        context,
                                        bean.layout.backgroundResColor,
                                        3,
                                        material3LibraryManager.usesNightResources())));
            }
        } else {
            cardView.setCardBackgroundColor(
                    PropertiesUtil.parseColor(
                            colorResourceResolver.getColorValue(
                                    context,
                                    cardBackgroundColor,
                                    3,
                                    material3LibraryManager.usesNightResources())));
        }

        int fallbackElevation = bean.layout.elevation > 0 ? bean.layout.elevation : 4;
        int elevation =
                layoutDpToPixels(PropertiesUtil.resolveSize(cardElevation, fallbackElevation));
        int maxElevation =
                layoutDpToPixels(
                        PropertiesUtil.resolveSize(
                                handler.getAttributeValueOf("cardMaxElevation"), 0));
        cardView.setMaxCardElevation(Math.max(elevation, maxElevation));
        cardView.setCardElevation(elevation);
        cardView.setRadius(layoutDpToPixels(PropertiesUtil.resolveSize(cardCornerRadius, 8)));
        cardView.setUseCompatPadding(
                Boolean.parseBoolean(TextUtils.isEmpty(compatPadding) ? "false" : compatPadding));
        cardView.setStrokeWidth(layoutDpToPixels(PropertiesUtil.resolveSize(strokeWidth, 0)));
        cardView.setStrokeColor(
                PropertiesUtil.isHexColor(strokeColor)
                        ? PropertiesUtil.parseColor(strokeColor)
                        : Color.WHITE);
    }

    private void updateCircleImageView(
            ItemCircleImageView imageView, InjectAttributeHandler handler) {
        String borderColor = handler.getAttributeValueOf("civ_border_color");
        String backgroundColor = handler.getAttributeValueOf("civ_circle_background_color");
        String borderWidth = handler.getAttributeValueOf("civ_border_width");
        String borderOverlay = handler.getAttributeValueOf("civ_border_overlay");

        imageView.setBorderColor(
                PropertiesUtil.isHexColor(borderColor)
                        ? PropertiesUtil.parseColor(borderColor)
                        : ContextCompat.getColor(getContext(), R.color.editor_default_border));
        imageView.setCircleBackgroundColor(
                PropertiesUtil.isHexColor(backgroundColor)
                        ? PropertiesUtil.parseColor(backgroundColor)
                        : ContextCompat.getColor(getContext(), R.color.editor_default_border));
        imageView.setBorderWidth(PropertiesUtil.resolveSize(borderWidth, 3));
        imageView.setBorderOverlay(
                Boolean.parseBoolean(TextUtils.isEmpty(borderOverlay) ? "false" : borderOverlay));
    }

    private void updateTabLayout(ItemTabLayout tabLayout, InjectAttributeHandler handler) {
        String gravity = handler.getAttributeValueOf("tabGravity");
        String mode = handler.getAttributeValueOf("tabMode");
        String indicatorHeight = handler.getAttributeValueOf("tabIndicatorHeight");
        String indicatorColor = handler.getAttributeValueOf("tabIndicatorColor");
        String textColor = handler.getAttributeValueOf("tabTextColor");
        String selectedTextColor = handler.getAttributeValueOf("tabSelectedTextColor");

        tabLayout.setTabGravity(
                switch (gravity) {
                    case "center" -> TabLayout.GRAVITY_CENTER;
                    case "start" -> TabLayout.GRAVITY_START;
                    default -> TabLayout.GRAVITY_FILL;
                });
        tabLayout.setTabMode(
                switch (mode) {
                    case "auto" -> TabLayout.MODE_AUTO;
                    case "scrollable" -> TabLayout.MODE_SCROLLABLE;
                    default -> TabLayout.MODE_FIXED;
                });
        tabLayout.setSelectedTabIndicatorHeight(PropertiesUtil.resolveSize(indicatorHeight, 3));
        tabLayout.setSelectedTabIndicatorColor(
                PropertiesUtil.isHexColor(indicatorColor)
                        ? PropertiesUtil.parseColor(indicatorColor)
                        : ContextCompat.getColor(getContext(), R.color.editor_tab_indicator));
        int tabTextColor =
                PropertiesUtil.isHexColor(textColor)
                        ? PropertiesUtil.parseColor(textColor)
                        : ContextCompat.getColor(getContext(), R.color.editor_tab_text);
        int tabSelectedTextColor =
                PropertiesUtil.isHexColor(selectedTextColor)
                        ? PropertiesUtil.parseColor(selectedTextColor)
                        : Color.WHITE;
        tabLayout.setTabTextColors(tabTextColor, tabSelectedTextColor);
    }

    private void updateMaterialButton(
            ItemMaterialButton materialButton, InjectAttributeHandler handler) {
        String backgroundTint = handler.getAttributeValueOf("backgroundTint");
        String radius = handler.getAttributeValueOf("cornerRadius");
        String stroke = handler.getAttributeValueOf("strokeWidth");
        if (!backgroundTint.isEmpty()) {
            Integer tintColor =
                    colorResourceResolver.resolveColorInt(
                            context,
                            backgroundTint,
                            3,
                            material3LibraryManager.usesNightResources());
            if (tintColor != null) {
                materialButton.setBackgroundTintList(ColorStateList.valueOf(tintColor));
            }
        }
        materialButton.setStrokeWidth(PropertiesUtil.resolveSize(stroke, 0));
        materialButton.setCornerRadius(PropertiesUtil.resolveSize(radius, 8));
    }

    private void updateMaterial3Button(ItemMaterialButton button, InjectAttributeHandler handler) {
        Map<String, String> injected = handler.getNamespacedAttributes();
        PreviewValues values = previewValues();
        Map<String, String> theme = previewProjectTheme();
        String styleValue = injected.getOrDefault("style", theme.get("materialButtonStyle"));
        PreviewValues.Style style =
                values.resolveStyle(values.resolveThemeValue(styleValue, theme));
        int styleRes = values.styleResource(style.base());
        int styleAttr = styleRes == 0 ? R.attr.materialButtonStyle : 0;
        Context buttonContext =
                MaterialThemeOverlay.wrap(
                        context, null, styleAttr, styleRes, new int[] {R.attr.materialSizeOverlay});
        String styleName =
                styleRes == 0
                        ? "Widget.Material3.Button"
                        : getResources().getResourceEntryName(styleRes);
        ButtonColorRoles roles = buttonColorRoles(styleName);
        Map<String, String> merged = new HashMap<>(style.attributes());
        merged.putAll(injected);
        Map<String, String> attributes = values.resolveAttributes(merged);
        var bean = handler.getBean();

        TypedArray defaults =
                buttonContext.obtainStyledAttributes(
                        null, R.styleable.MaterialButton, styleAttr, styleRes);
        try {
            ShapeAppearance shape =
                    StateListShapeAppearanceModel.create(
                            buttonContext, defaults, R.styleable.MaterialButton_shapeAppearance);
            if (shape == null) {
                shape =
                        ShapeAppearanceModel.builder(buttonContext, null, styleAttr, styleRes)
                                .build();
            }
            int radius =
                    resolveButtonDimension(
                            attributes,
                            "cornerRadius",
                            defaults.getDimensionPixelSize(
                                    R.styleable.MaterialButton_cornerRadius, -1));
            button.setShapeAppearance(radius < 0 ? shape : shape.withCornerSize(radius));
            button.setStrokeWidth(
                    resolveButtonDimension(
                            attributes,
                            "strokeWidth",
                            defaults.getDimensionPixelSize(
                                    R.styleable.MaterialButton_strokeWidth, 0)));
            ColorStateList stroke =
                    MaterialResources.getColorStateList(
                            buttonContext, defaults, R.styleable.MaterialButton_strokeColor);
            String strokeColor =
                    attributes.getOrDefault("app:strokeColor", attributes.get("strokeColor"));
            button.setStrokeColor(
                    previewButtonColors(stroke, strokeColor, roles.stroke(), values, theme));

            String tint = buttonBackgroundTint(bean, injected, style.attributes());
            ColorStateList defaultTint =
                    MaterialResources.getColorStateList(
                            buttonContext, defaults, R.styleable.MaterialButton_backgroundTint);
            ColorStateList backgroundTint =
                    "@null".equals(tint)
                            ? null
                            : previewButtonColors(
                                    defaultTint, tint, roles.background(), values, theme);
            button.setBackgroundTintList(backgroundTint);
        } finally {
            defaults.recycle();
        }

        if (injected.containsKey("android:textColor")
                || (bean.text.resTextColor == null && bean.text.textColor == 0xffffff)) {
            ColorStateList textColors =
                    buttonTextColors(button, buttonContext, styleAttr, styleRes);
            button.setTextColor(
                    previewButtonColors(
                            textColors,
                            attributes.get("android:textColor"),
                            roles.text(),
                            values,
                            theme));
        }
    }

    private record ButtonColorRoles(String background, String stroke, String text) {}

    private static ButtonColorRoles buttonColorRoles(String styleName) {
        boolean expressive = styleName.startsWith("Widget.Material3Expressive.");
        String base = expressive ? "Widget.Material3Expressive.Button" : "Widget.Material3.Button";
        if (styleName.equals(base) || styleName.equals(base + ".Icon")) {
            return new ButtonColorRoles("colorPrimary", null, "colorOnPrimary");
        }
        if (styleName.startsWith(base + ".OutlinedButton")) {
            return expressive
                    ? new ButtonColorRoles(null, "colorOutlineVariant", "colorOnSurfaceVariant")
                    : new ButtonColorRoles(null, "colorOutline", "colorPrimary");
        }
        String text = styleName.startsWith(base + ".TextButton") ? "colorPrimary" : null;
        return new ButtonColorRoles(null, null, text);
    }

    private static ColorStateList buttonTextColors(
            ItemMaterialButton button, Context context, int styleAttr, int styleRes) {
        TypedArray textStyle =
                context.obtainStyledAttributes(
                        null, new int[] {android.R.attr.textColor}, styleAttr, styleRes);
        try {
            ColorStateList colors = MaterialResources.getColorStateList(context, textStyle, 0);
            return colors != null
                    ? colors
                    : (ColorStateList) button.getTag(R.id.preview_default_text_colors);
        } finally {
            textStyle.recycle();
        }
    }

    private static String buttonBackgroundTint(
            ViewBean bean, Map<String, String> injected, Map<String, String> style) {
        String tint = backgroundTintAttribute(injected);
        if (tint != null) return tint;

        tint = MaterialButtonAttributes.generatedBackgroundTint(bean.layout, injected);
        return tint != null ? tint : backgroundTintAttribute(style);
    }

    private static String backgroundTintAttribute(Map<String, String> attributes) {
        return attributes.getOrDefault(
                "app:backgroundTint",
                attributes.getOrDefault(
                        "android:backgroundTint", attributes.get("backgroundTint")));
    }

    private int resolveButtonDimension(Map<String, String> attributes, String name, int fallback) {
        String raw = attributes.getOrDefault("app:" + name, attributes.get(name));
        int dp = raw == null ? -1 : PropertiesUtil.resolveSize(raw, -1);
        return dp < 0 ? fallback : layoutDpToPixels(dp);
    }

    private ColorStateList previewButtonColors(
            ColorStateList defaults,
            String explicit,
            String themeRole,
            PreviewValues values,
            Map<String, String> theme) {
        String value = explicit;
        if (value == null) {
            if (themeRole == null || !theme.containsKey(themeRole)) return defaults;
            value = "?attr/" + themeRole;
        }
        String reference = values.resolveThemeValue(value, theme);
        ColorStateList selector =
                values.colorSelector(reference, theme, resolved -> projectColor(values, resolved));
        if (selector != null) return selector;

        Integer color = projectColor(values, reference);
        if (color == null) return defaults;
        if (explicit != null || defaults == null || !defaults.isStateful()) {
            return ColorStateList.valueOf(color);
        }
        // A plain theme color replaces the enabled color while keeping the library's disabled tint.
        int[] disabled = {-android.R.attr.state_enabled};
        return new ColorStateList(
                new int[][] {disabled, new int[0]},
                new int[] {defaults.getColorForState(disabled, defaults.getDefaultColor()), color});
    }

    private String extractAttrValue(String line, String attribute) {
        Matcher matcher = Pattern.compile("=\"([^\"]*)\"").matcher(line);
        return matcher.find() ? matcher.group(1) : "";
    }

    @NonNull
    @Override
    public String toString() {
        return getClass().getName() + "@" + Integer.toHexString(hashCode());
    }

    private record ViewInfo(Rect rect, View view, int index, int depth) {}

    private static FirebaseCrashlytics getFirebaseCrashlytics() {
        try {
            return FirebaseCrashlytics.getInstance();
        } catch (IllegalStateException e) {
            return null;
        }
    }
}
