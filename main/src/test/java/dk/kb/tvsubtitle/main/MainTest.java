package dk.kb.tvsubtitle.main;
import dk.kb.tvsubtitle.common.PropertiesFactory;
import dk.kb.tvsubtitle.common.RuntimeProperties;
import dk.kb.tvsubtitle.main.mainprocessor.ProcessFileListToFiles;
import dk.kb.tvsubtitle.main.mainprocessor.ProcessIndexToDoms;
import dk.kb.tvsubtitle.main.mainprocessor.ProcessIndexToFiles;
import org.apache.commons.collections4.map.LinkedMap;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * This class is not for actual unit testing, but for development with experimentation and debugging.
 * So instead of spawning a billion main args all over the place. You can experiment with this as your starting point.
 */
public class MainTest {
    final static Logger log = LoggerFactory.getLogger(MainTest.class);
    protected final String uuidListFileName = "uuidList.txt";

    @Test
    @Disabled("For manual debugging only.")
    public void runVideoProcessor() throws IOException {

        log.info("Starting.");
        log.info("Setting up program.");
        RuntimeProperties properties = PropertiesFactory.getProperties();
        VideoProcessor videoProcessor = new VideoProcessor(properties);

        log.info("Running program.");
        File video = new File("/some/path/file.mp4");

        Path workDir = Paths.get(properties.getProperty(RuntimeProperties.ResourceName.sharedWorkDir));
        Path srtPath = Paths.get(workDir.toString(), "/subtitles.srt");

        try {
            Files.deleteIfExists(srtPath);
        } catch (IOException e) {
            throw new RuntimeException("Something happened when deleting the SRT file. FilePath: " + srtPath, e);
        }

        videoProcessor.processVideo(video, srtPath);

        log.info("The End.");
    }

    @Test
    @Disabled("For manual debugging only.")
    public void runVideoMassProcessor() throws IOException, ExecutionException, InterruptedException {
        log.info("Starting.");
        log.info("Setting up program.");
        VideoMassProcessor processor = new VideoMassProcessor();

        List<UUID> uuidList = new LinkedList<>();
        uuidList.add(UUID.fromString("371157ee-b120-4504-bfaf-364c15a4137c"));

        String uuid1 = uuidList.get(0).toString();

        log.info("Running program.");
        LinkedMap<UUID, Future<File>> processedVideosFuture = processor.processVideosAsync(uuidList);

        File srtFile = processedVideosFuture.getValue(0).get();
        String absolutePath = srtFile.getAbsolutePath();

        log.info("The End.");

        String s = "";
    }

    @Test
    @Disabled("For manual debugging only.")
    void runProcessIndexToFiles() {
        log.info("Setting up program.");
        ProcessIndexToFiles processIndexToFiles = new ProcessIndexToFiles();

        log.info("Running program.");
        processIndexToFiles.process(0, 1);

        log.info("Program has shut down.");
    }

    @Test
    @Disabled("For manual debugging only.")
    void runProcessIndexToDoms() {
        log.info("Setting up program.");
        ProcessIndexToDoms processIndexToDoms = new ProcessIndexToDoms();
        processIndexToDoms.setAmountPerRun(5);

        log.info("Running program.");
        processIndexToDoms.process();

        log.info("Program has shut down.");
    }

    @Test
    @Disabled("For manual debugging only.")
    void runProcessFileListToFiles() {
        log.info("Setting up program.");
        ProcessFileListToFiles processFileListToFiles = new ProcessFileListToFiles();

        URL fileURL = getClass().getClassLoader().getResource(uuidListFileName);
        if(fileURL == null) {
            throw new RuntimeException("The following inputFile to get video UUIDs from isn't in classpath: " + uuidListFileName);
        }

        File inputFile = ProcessFileListToFiles.getFile( Paths.get( fileURL.getPath() ));

        log.info("Running program.");
        processFileListToFiles.process(inputFile);

        log.info("Program has shut down.");
    }

    @Test
    @Disabled("For manual debugging only.")
    void createWorkDir() throws IOException {
        final String configFileName = "config.properties";
        Properties prop = new Properties();
        try(InputStream propFile = Thread.currentThread().getContextClassLoader().getResourceAsStream(configFileName)) {
            prop.load(new InputStreamReader(propFile));
        }
        String workDirString = prop.getProperty(RuntimeProperties.ResourceName.sharedWorkDir.toString());
        Path workDirPath = Paths.get(workDirString);

        Files.createDirectories(workDirPath);
    }

}
