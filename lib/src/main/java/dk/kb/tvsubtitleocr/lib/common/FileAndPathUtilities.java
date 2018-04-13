package dk.kb.tvsubtitleocr.lib.common;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileAndPathUtilities {
    public static File createSubWorkDir(File parentWorkDir, String subFolderName) {
        Path subWorkDirPath = Paths.get(parentWorkDir.getAbsolutePath(), subFolderName);

        if(Files.exists(subWorkDirPath) && ! Files.isWritable(subWorkDirPath)) {
            throw new RuntimeException("Doesn't have write permission to the following directory: " + subWorkDirPath.toString());
        }

        try {
            if(Files.exists(subWorkDirPath)) {
                Files.delete(subWorkDirPath);
            }

            Path subWorkDir = Files.createDirectory(subWorkDirPath);

            return subWorkDir.toFile();
        } catch (IOException e) {
            // This shouldn't be possible..
            throw new RuntimeException("Something went wrong while creating the subWorkDir: " + subWorkDirPath.toString(), e);
        }
    }

    public static void resetDirectory(Path workDir) throws IOException {
        if(Files.exists(workDir)) {
            deleteDirectoryAndContent(workDir);
        }
        Files.createDirectories(workDir);
    }

    public static void deleteDirectoryAndContent(Path path) throws IOException {
        FileUtils.forceDelete(path.toFile());
    }
}
