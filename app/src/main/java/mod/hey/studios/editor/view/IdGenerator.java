package mod.hey.studios.editor.view;

public class IdGenerator {

    public static String getLastPath(String path) {
        if (!path.contains(".")) {
            return path;
        }

        String[] split = path.split("\\.");
        return split[split.length - 1];
    }
}
