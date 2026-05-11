package pro.sketchware.activities.main.fragments.projects;

public interface CallBackTask {

    void onCopyPreExecute();

    void onCopyProgressUpdate(int progress);

    void onCopyPostExecute(String path, boolean wasSuccessful, String reason);
}