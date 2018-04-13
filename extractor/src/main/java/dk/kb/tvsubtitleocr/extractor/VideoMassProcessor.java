package dk.kb.tvsubtitleocr.extractor;
import dk.kb.tvsubtitleocr.lib.common.PropertiesFactory;
import dk.kb.tvsubtitleocr.lib.common.RuntimeProperties;
import dk.kb.tvsubtitleocr.lib.VideoProcessor;
import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class VideoMassProcessor {
    private final static Logger log = LoggerFactory.getLogger(VideoMassProcessor.class);
    private final static String videoFileExtension = "mp4";
    private static boolean debug;
    private RuntimeProperties properties;
    private ExecutorService executorService;
    private File sharedWorkDir;
    private File videoSourceDir;
    private File srtOutputDir;

    public VideoMassProcessor() {
        this(PropertiesFactory.getProperties());
    }

    public VideoMassProcessor(RuntimeProperties properties) {
        this(properties, Paths.get(properties.getProperty(RuntimeProperties.ResourceName.sharedWorkDir)).toFile());
    }

    public VideoMassProcessor(RuntimeProperties properties, File workDir) {
        this.properties = properties;
        debug = properties.getDebug();
        this.videoSourceDir = Paths.get(properties.getProperty(RuntimeProperties.ResourceName.videoSourceDir)).toFile();

        this.sharedWorkDir = workDir;
        this.srtOutputDir = createSrtOutputDir(workDir);
        this.executorService = Executors.newSingleThreadExecutor(); // They will be processed sequentially.
    }

    public LinkedMap<UUID, Future<File>> processVideosAsync(List<UUID> videos) {
        LinkedMap<UUID, Future<File>> result = new LinkedMap<>(videos.size());

        for(UUID videoUUID : videos) {

            // Where to save the srt file.
            Path srtPath = Paths.get(srtOutputDir.getAbsolutePath(), videoUUID + ".srt");
            try {
                Files.deleteIfExists(srtPath);
            } catch (IOException e) {
                throw  new RuntimeException("This shouldn't be possible.. Couldn't delete srt file: " + srtPath.toString(), e);
            }

            Callable<File> processVideoToSRTCallable = processVideoToSRTCallable(videoUUID, srtPath);

            Future<File> futureSrt = executorService.submit(processVideoToSRTCallable);

            result.put(videoUUID, futureSrt);
        }

        return result;
    }

    protected File getFileFromVideoUUID(UUID video) throws IOException {
//        File result = null;

        CharSequence dirSequence = video.toString().subSequence(0, 4);
        String dir1 = String.valueOf(dirSequence.charAt(0));
        String dir2 = String.valueOf(dirSequence.charAt(1));
        String dir3 = String.valueOf(dirSequence.charAt(2));
        String dir4 = String.valueOf(dirSequence.charAt(3));
        Path videoPath = Paths.get(videoSourceDir.getAbsolutePath(), dir1, dir2, dir3, dir4,
                String.join(".", video.toString(), videoFileExtension)
        );

        if(Files.notExists(videoPath)) {
            throw new IOException("No video file at the expected location: " + videoPath.toString());
        }

        return videoPath.toFile();
    }

    protected File createSrtOutputDir(File sharedWorkDir) {
        Path newSRTOutputDir = Paths.get(sharedWorkDir.getAbsolutePath(), "srtOutputs");
        if(Files.notExists(newSRTOutputDir)) {
            try {
                Files.createDirectory(newSRTOutputDir);
            } catch (IOException e) {
                // This shouldn't happen..
                throw new RuntimeException("Couldn't create directory: " + newSRTOutputDir.toString(), e);
            }
        }
        else if( ! Files.isWritable(newSRTOutputDir)) {
            // This shouldn't happen..
            throw new RuntimeException("The following directory is not writable: " + newSRTOutputDir.toString());
        }

        return newSRTOutputDir.toFile();
    }

    protected Callable<File> processVideoToSRTCallable(UUID videoUUID, Path srtPath) {
        return new Callable<File>() {
            UUID videoUUIDInternal = videoUUID;
            Path srtPathInternal = srtPath;

            @Override
            public File call() throws IOException {
                File video = getFileFromVideoUUID(videoUUIDInternal);
                File taskWorkDir;

                try {
                    Path workDirPath;

                    workDirPath = Files.createTempDirectory(sharedWorkDir.toPath(), videoUUID.toString());

                    taskWorkDir = workDirPath.toFile();
                } catch (IOException e) {
                    // This shouldn't be possible..
                    throw new RuntimeException("Failed creating temporary taskWorkDir for videoUUID: " + videoUUIDInternal, e);
                }

                // Processes video through videoProcessor and get SRT file.
                VideoProcessor videoProcessor = null;
                try {
                    videoProcessor = new VideoProcessor(properties, taskWorkDir);
                    videoProcessor.processVideo(video, srtPathInternal);

                    return srtPathInternal.toFile();
                } catch (IOException e) {
                    log.error("Error processing video {}", videoUUID, e);

                    if(Files.exists(srtPath)) {
                        FileUtils.deleteQuietly(srtPath.toFile());
                    }

                    return null;
                }
                finally {
                    FileUtils.deleteQuietly(taskWorkDir);
                }
            }
        };
    }

    public Path getSharedWorkDirPath() {
        return sharedWorkDir.toPath();
    }

    public Path getVideoSourceDirPath() {
        return videoSourceDir.toPath();
    }

    public Path getSrtOutputDirPath() {
        return srtOutputDir.toPath();
    }
}
