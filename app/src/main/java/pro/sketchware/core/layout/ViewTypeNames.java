package pro.sketchware.core.layout;

import pro.sketchware.beans.ViewBean;
import pro.sketchware.beans.ViewBeans;

/** Stable ID prefixes used by imported and newly created widgets. */
public final class ViewTypeNames {
    private ViewTypeNames() {
    }

    public static String getIdPrefix(int type) {
        return switch (type) {
            case ViewBean.VIEW_TYPE_LAYOUT_LINEAR -> "linear";
            case ViewBean.VIEW_TYPE_LAYOUT_RELATIVE -> "relativelayout";
            case ViewBean.VIEW_TYPE_LAYOUT_HSCROLLVIEW -> "hscroll";
            case ViewBean.VIEW_TYPE_WIDGET_BUTTON -> "button";
            case ViewBean.VIEW_TYPE_WIDGET_TEXTVIEW -> "textview";
            case ViewBean.VIEW_TYPE_WIDGET_EDITTEXT -> "edittext";
            case ViewBean.VIEW_TYPE_WIDGET_IMAGEVIEW -> "imageview";
            case ViewBean.VIEW_TYPE_WIDGET_WEBVIEW -> "webview";
            case ViewBean.VIEW_TYPE_WIDGET_PROGRESSBAR -> "progressbar";
            case ViewBean.VIEW_TYPE_WIDGET_LISTVIEW -> "listview";
            case ViewBean.VIEW_TYPE_WIDGET_SPINNER -> "spinner";
            case ViewBean.VIEW_TYPE_WIDGET_CHECKBOX -> "checkbox";
            case ViewBean.VIEW_TYPE_LAYOUT_VSCROLLVIEW -> "vscroll";
            case ViewBean.VIEW_TYPE_WIDGET_SWITCH -> "switch";
            case ViewBean.VIEW_TYPE_WIDGET_SEEKBAR -> "seekbar";
            case ViewBean.VIEW_TYPE_WIDGET_CALENDARVIEW -> "calendarview";
            case ViewBean.VIEW_TYPE_WIDGET_FAB -> "fab";
            case ViewBean.VIEW_TYPE_WIDGET_ADVIEW -> "adview";
            case ViewBean.VIEW_TYPE_WIDGET_MAPVIEW -> "mapview";
            case ViewBeans.VIEW_TYPE_WIDGET_RADIOBUTTON -> "radiobutton";
            case ViewBeans.VIEW_TYPE_WIDGET_RATINGBAR -> "ratingbar";
            case ViewBeans.VIEW_TYPE_WIDGET_VIDEOVIEW -> "videoview";
            case ViewBeans.VIEW_TYPE_WIDGET_SEARCHVIEW -> "searchview";
            case ViewBeans.VIEW_TYPE_WIDGET_AUTOCOMPLETETEXTVIEW -> "autocomplete";
            case ViewBeans.VIEW_TYPE_WIDGET_MULTIAUTOCOMPLETETEXTVIEW -> "multiautocomplete";
            case ViewBeans.VIEW_TYPE_WIDGET_GRIDVIEW -> "gridview";
            case ViewBeans.VIEW_TYPE_WIDGET_ANALOGCLOCK -> "analogclock";
            case ViewBeans.VIEW_TYPE_WIDGET_DATEPICKER -> "datepicker";
            case ViewBeans.VIEW_TYPE_WIDGET_TIMEPICKER -> "timepicker";
            case ViewBeans.VIEW_TYPE_WIDGET_DIGITALCLOCK -> "digitalclock";
            case ViewBeans.VIEW_TYPE_LAYOUT_TABLAYOUT -> "tablayout";
            case ViewBeans.VIEW_TYPE_LAYOUT_VIEWPAGER -> "viewpager";
            case ViewBeans.VIEW_TYPE_LAYOUT_BOTTOMNAVIGATIONVIEW -> "bottomnavigation";
            case ViewBeans.VIEW_TYPE_WIDGET_BADGEVIEW -> "badgeview";
            case ViewBeans.VIEW_TYPE_WIDGET_PATTERNLOCKVIEW -> "patternlockview";
            case ViewBeans.VIEW_TYPE_WIDGET_WAVESIDEBAR -> "wavesidebar";
            case ViewBeans.VIEW_TYPE_LAYOUT_CARDVIEW -> "cardview";
            case ViewBeans.VIEW_TYPE_LAYOUT_COLLAPSINGTOOLBARLAYOUT -> "collapsingtoolbar";
            case ViewBeans.VIEW_TYPE_LAYOUT_TEXTINPUTLAYOUT -> "textinputlayout";
            case ViewBeans.VIEW_TYPE_LAYOUT_SWIPEREFRESHLAYOUT -> "swiperefreshlayout";
            case ViewBeans.VIEW_TYPE_LAYOUT_RADIOGROUP -> "radiogroup";
            case ViewBeans.VIEW_TYPE_WIDGET_MATERIALBUTTON -> "materialbutton";
            case ViewBeans.VIEW_TYPE_WIDGET_SIGNINBUTTON -> "signinbutton";
            case ViewBeans.VIEW_TYPE_WIDGET_CIRCLEIMAGEVIEW -> "circleimageview";
            case ViewBeans.VIEW_TYPE_WIDGET_LOTTIEANIMATIONVIEW -> "lottie";
            case ViewBeans.VIEW_TYPE_WIDGET_YOUTUBEPLAYERVIEW -> "youtube";
            case ViewBeans.VIEW_TYPE_WIDGET_OTPVIEW -> "otpview";
            case ViewBeans.VIEW_TYPE_WIDGET_CODEVIEW -> "codeview";
            case ViewBeans.VIEW_TYPE_WIDGET_RECYCLERVIEW -> "recyclerview";
            default -> "widget";
        };
    }
}
