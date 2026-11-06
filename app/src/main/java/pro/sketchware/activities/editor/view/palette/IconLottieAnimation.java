package pro.sketchware.activities.editor.view.palette;

import android.content.Context;
import android.view.ViewGroup;
import pro.sketchware.R;
import pro.sketchware.beans.ViewBean;
import pro.sketchware.beans.ViewBeans;

public class IconLottieAnimation extends IconBase {

    public IconLottieAnimation(Context context) {
        super(context);
        setWidgetImage(R.drawable.ic_mtrl_animation);
        setWidgetName("Lottie");
    }

    @Override
    public ViewBean getBean() {
        ViewBean viewBean = new ViewBean();
        viewBean.type = ViewBeans.VIEW_TYPE_WIDGET_LOTTIEANIMATIONVIEW;
        viewBean.layout.orientation = VERTICAL;
        viewBean.layout.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        viewBean.layout.paddingLeft = 8;
        viewBean.layout.paddingTop = 8;
        viewBean.layout.paddingRight = 8;
        viewBean.layout.paddingBottom = 8;
        viewBean.convert = "com.airbnb.lottie.LottieAnimationView";
        viewBean.inject =
                "android:scaleType=\"fitCenter\"\nandroid:layout_centerInParent=\"true\"\napp:lottie_fileName=\"your_file.json\"\napp:lottie_loop=\"true\" \napp:lottie_autoPlay=\"true\"";
        return viewBean;
    }
}
