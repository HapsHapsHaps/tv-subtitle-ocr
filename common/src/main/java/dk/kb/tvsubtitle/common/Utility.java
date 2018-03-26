package dk.kb.tvsubtitle.common;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Utility {
    /**
     * Checks if the input is null or a Path and returns the equivalent.
     * Ths way you avoid the InvalidPathException if path is null.
     * @param path the input path to check.
     * @return null if input is null. Else its Path object instance.
     */
    public static Path stringAsNullOrPath(String path) {
        Path result = null;
        if(path != null) result = Paths.get(path);
        return result;
    }

    /**
     * Checks if the input is null or a file and returns the equivalent.
     * Ths way you avoid the NullPointerException if path is null.
     * @param path the input path to check.
     * @return null if input is null. Else its File object instance.
     */
    public static File stringAsNullOrFile(String path) {
        File result = null;
        if(path != null) result = new File(path);
        return result;
    }
}
