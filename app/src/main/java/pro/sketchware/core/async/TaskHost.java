package pro.sketchware.core.async;

import android.app.Activity;

import androidx.fragment.app.Fragment;

import java.lang.ref.WeakReference;

public abstract class TaskHost {

    private TaskHost() {
    }

    public static TaskHost of(Activity activity) {
        return new ActivityTaskHost(activity);
    }

    public static TaskHost of(Fragment fragment) {
        return new FragmentTaskHost(fragment);
    }

    public abstract boolean isAlive();

    public final void postToUi(Runnable action) {
        if (action == null) {
            return;
        }
        BackgroundTasks.postMain(() -> {
            if (isAlive()) {
                action.run();
            }
        });
    }

    private static final class ActivityTaskHost extends TaskHost {
        private final WeakReference<Activity> activityRef;

        private ActivityTaskHost(Activity activity) {
            activityRef = new WeakReference<>(activity);
        }

        @Override
        public boolean isAlive() {
            Activity activity = activityRef.get();
            return activity != null && !activity.isFinishing() && !activity.isDestroyed();
        }
    }

    private static final class FragmentTaskHost extends TaskHost {
        private final WeakReference<Fragment> fragmentRef;

        private FragmentTaskHost(Fragment fragment) {
            fragmentRef = new WeakReference<>(fragment);
        }

        @Override
        public boolean isAlive() {
            Fragment fragment = fragmentRef.get();
            if (fragment == null || !fragment.isAdded()) {
                return false;
            }
            Activity activity = fragment.getActivity();
            return activity != null && !activity.isFinishing() && !activity.isDestroyed();
        }
    }
}
