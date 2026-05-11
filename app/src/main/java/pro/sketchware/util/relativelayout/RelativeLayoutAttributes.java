package pro.sketchware.util.relativelayout;

import java.util.Arrays;
import java.util.List;

public class RelativeLayoutAttributes {

    public static final List<String> RELATIVE_ID_ATTRIBUTES = Arrays.asList(
            "android:layout_alignStart", "android:layout_alignEnd",
            "android:layout_alignLeft", "android:layout_alignRight",
            "android:layout_alignTop", "android:layout_alignBottom",

            "android:layout_alignBaseline",

            "android:layout_toStartOf", "android:layout_toEndOf",
            "android:layout_toLeftOf", "android:layout_toRightOf",
            "android:layout_above", "android:layout_below"
    );
}
