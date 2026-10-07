package pro.sketchware.core.layout;

/** Stable ID prefixes used by imported and newly created widgets. */
public final class ViewTypeNames {
    private ViewTypeNames() {
    }

    public static String getIdPrefix(int type) {
        return switch (type) {
            case 0 -> "linear";
            case 1 -> "relativelayout";
            case 2 -> "hscroll";
            case 3 -> "button";
            case 4 -> "textview";
            case 5 -> "edittext";
            case 6 -> "imageview";
            case 7 -> "webview";
            case 8 -> "progressbar";
            case 9 -> "listview";
            case 10 -> "spinner";
            case 11 -> "checkbox";
            case 12 -> "vscroll";
            case 13 -> "switch";
            case 14 -> "seekbar";
            case 15 -> "calendarview";
            case 16 -> "fab";
            case 17 -> "adview";
            case 18 -> "mapview";
            case 19 -> "radiobutton";
            case 20 -> "ratingbar";
            case 21 -> "videoview";
            case 22 -> "searchview";
            case 23 -> "autocomplete";
            case 24 -> "multiautocomplete";
            case 25 -> "gridview";
            case 26 -> "analogclock";
            case 27 -> "datepicker";
            case 28 -> "timepicker";
            case 29 -> "digitalclock";
            case 30 -> "tablayout";
            case 31 -> "viewpager";
            case 32 -> "bottomnavigation";
            case 33 -> "badgeview";
            case 34 -> "patternlockview";
            case 35 -> "wavesidebar";
            case 36 -> "cardview";
            case 37 -> "collapsingtoolbar";
            case 38 -> "textinputlayout";
            case 39 -> "swiperefreshlayout";
            case 40 -> "radiogroup";
            case 41 -> "materialbutton";
            case 42 -> "signinbutton";
            case 43 -> "circleimageview";
            case 44 -> "lottie";
            case 45 -> "youtube";
            case 46 -> "otpview";
            case 47 -> "codeview";
            case 48 -> "recyclerview";
            default -> "widget";
        };
    }
}
