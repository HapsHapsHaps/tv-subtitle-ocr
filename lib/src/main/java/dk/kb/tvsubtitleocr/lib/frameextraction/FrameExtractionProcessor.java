package dk.kb.tvsubtitleocr.lib.frameextraction;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FrameExtractionProcessor implements IFrameExtractionProcessor {

    //properties (Maybe send some of these to the properties module later on)
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final static String subWorkDir = "frameExtraction";
    private FFprobe ffprobe;
    private FFmpeg ffmpeg;
    private File workDir;
//    private double framesPerSecond = 1;
    private double framesPerSecond = 0.5;
    private String output; //decides name(%d for digits) + extention(.png) for the output
    //    private String ffmpegPath = "/usr/bin/ffmpeg";
//    private String ffprobePath = "/usr/bin/ffprobe";
    //debug = true, when you don't want it to delete the files, and generate them if the folder is empty.
    protected final Boolean debug;

    public FrameExtractionProcessor(File workDir, String ffmpegPath, String ffprobePath, boolean debug) throws IOException {
        this.workDir = workDir;
        this.ffprobe = new FFprobe(ffprobePath);
        this.ffmpeg = new FFmpeg(ffmpegPath);
        this.debug = debug;
    }

    public FrameExtractionProcessor(File workDir) throws IOException {
        this.workDir = workDir;
        this.ffprobe = new FFprobe("/usr/bin/ffprobe");
        this.ffmpeg = new FFmpeg("/usr/bin/ffmpeg");
        this.debug = false;
    }

    @Override
    public VideoInformation extractFrames(File video) throws IOException {
        VideoInformation returnInformation;
        int videoLength = 0;
        File outPutDir = this.workDir;

        if (!debug || (debug && outPutDir.listFiles().length == 0)) {
            extractVideoFramesToDisk(video, framesPerSecond, outPutDir);
        } else {
            log.info("{} is in debug with files in output folder. Frames wont be extracted.", getClass().getName());
        }

        videoLength = getVideoLength(video);

        File[] frameFiles = getFrameFiles(outPutDir);
        List<VideoFrame> videoFrames = frameFilesToVideoFrame(frameFiles, videoLength, framesPerSecond);

        if (!debug) {
            Arrays.asList(frameFiles).forEach(File::delete);
        } else {
            log.info("{} is in debug. Frames wont be deleted.", getClass().getName());
        }

        returnInformation = createVideoInformation(videoFrames, videoLength, video);

        return returnInformation;
    }

    public void extractVideoFramesToDisk(File video, double framesPerSecond, File outputDir) throws IOException {
        String output = outputDir.getAbsolutePath() + "/" + "%05d.png";
        FFmpegProbeResult probeResult = getVideoProbeResults(video);
        FFmpegBuilder builder = new FFmpegBuilder()
                .setVerbosity(FFmpegBuilder.Verbosity.WARNING)
                .setInput(probeResult)     // Filename, or a FFmpegProbeResult
                .addOutput(output)   // Filename for the destination
                .setVideoQuality(1)
                .disableAudio()
//                .addExtraArgs("-vf", "fps=1/2")// + framesPerSecond)
                .addExtraArgs("-vf", "fps=" + framesPerSecond)
                .done();

        FFmpegExecutor executor = new FFmpegExecutor(this.ffmpeg, this.ffprobe);

        // Run FFMPEG ->

        executor.createJob(builder).run();
    }

    /**
     * Gets the video length in milliseconds from the input video file.
     *
     * @param video
     * @return Video length in milliseconds.
     * @throws IOException
     */
    public int getVideoLength(File video) throws IOException {
        FFmpegProbeResult probeResult = ffprobe.probe(video.getPath());
        return (int) probeResult.getFormat().duration * 1000;
    }

    public FFmpegProbeResult getVideoProbeResults(File video) throws IOException {
        return ffprobe.probe(video.getPath());
    }

    public File[] getFrameFiles(File folder) {
        File[] listOfFiles = folder.listFiles(f -> f.getName().endsWith(".png"));
        Arrays.sort(listOfFiles);
        return listOfFiles;
    }

    public List<VideoFrame> frameFilesToVideoFrame(File[] files, int videoLength, double framesPerSecond) {
        List<VideoFrame> frames = new ArrayList<>();
        double timeInterval = 1000 / framesPerSecond;
        int frameMillisecondsStart = 0;
        int frameMillisecondStop = (int) (frameMillisecondsStart + timeInterval);

        for (int i = 0; i < files.length; i++) {
            //i*1000 because of the milliseconds and videoLength * framesPerSecond because with 2 fps we get double the amount of frames
            if (i * 1000 >= videoLength * framesPerSecond) {
                log.info("Ignoring: " + files[i].getName());
//                files = ArrayUtils.remove(files, i);
                // Ignore it.
                continue;
            }
            if (files[i].isFile()) {
                Path path = Paths.get(files[i].getPath());
                String frameName = files[i].getName();
                BufferedImage frameInstance = null;
                try {
                    frameInstance = ImageIO.read(files[i]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (frameInstance != null) {
                    VideoFrame frame = new VideoFrame(frameInstance, frameName, frameMillisecondsStart, frameMillisecondStop);
                    frames.add(frame);
                }

                frameMillisecondsStart += timeInterval;
                frameMillisecondStop += timeInterval;
            }
        }

        return frames;
    }

    private VideoInformation createVideoInformation(List<VideoFrame> frames, int milliseconds, File input) {
        VideoInformation resultInformation;
        String uuid = input.getName().substring(0, input.getName().indexOf("."));
        resultInformation = new VideoInformation(frames, uuid, milliseconds);

        return resultInformation;
    }
}
