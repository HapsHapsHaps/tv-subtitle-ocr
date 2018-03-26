package dk.kb.tvsubtitle.main.mainprocessor;

import dk.kb.tvsubtitle.common.PropertiesFactory;
import dk.kb.tvsubtitle.common.RuntimeProperties;
import dk.kb.tvsubtitle.main.VideoMassProcessor;
import dk.kb.tvsubtitle.main.model.VideoInfo;
import dk.kb.tvsubtitle.main.model.VideoResultHandler;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.collections4.map.LinkedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Abstract implementation of processing Video UUID from external Index.
 * What the final output will be, is up to the deriving implementation.
 */
public abstract class Processor {
    protected final static Logger log = LoggerFactory.getLogger(Processor.class);
    protected RuntimeProperties properties;
    protected final VideoMassProcessor videoMassProcessor;
    protected int processedSuccess = 0;
    protected List<VideoInfo> processedError = new LinkedList<>();

    public Processor() {
        this.properties = PropertiesFactory.getProperties();
        this.videoMassProcessor = new VideoMassProcessor(properties);
    }

    protected static CommandLine handleProgramArguments(String[] args, Option... options) throws ParseException {

        Option help = Option.builder("?")
                .longOpt("help")
                .hasArg(false)
                .desc("This message")
                .build();

        Options optionsBuilder = new Options();

        //adding args to options
        optionsBuilder.addOption(help);
        for(Option option: options) {
            optionsBuilder.addOption(option);
        }

        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse( optionsBuilder, args );

        if( commandLine.hasOption( "help" ) ) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "ant", optionsBuilder );
            System.exit(1);
        }

        return commandLine;
    }

    public void process(List<VideoInfo> videos, VideoResultHandler resultHandler, String doneMessage) {
        processVideos(videos, resultHandler);

        logFinalProcessingInformation(videos.size(), doneMessage);
    }

    /**
     * Logs the final result of processing the video list in a nice way.
     * Includes mentioning each video that it failed processing.
     * @param indexVideosSize The size of the processed list.
     * @param doneMessage A message to log that describes what succeeded.
     */
    protected void logFinalProcessingInformation(int indexVideosSize, String doneMessage) {
        try {
            Thread.sleep(100); // Avoids the logging being out of order from processing videos.
            // Necessary because of multiple logging threads, that in this specific scenario,
            // sometimes creates a race condition from the method calling this one.
        } catch (InterruptedException e) {
            log.error("Error pausing thread", e);
        }
        log.info(doneMessage + " " +
                        "\nProcessed {} / {} sucessfully, with {} errors.",
                indexVideosSize,
                processedSuccess,
                processedError.size());

        if( ! processedError.isEmpty()) {
            log.warn("The following videos failed processing:");
            processedError.forEach(it -> {
                log.warn("Failed for UUID: {}, Title: {}", it.getUuid(), it.getTitle());
            });
        }
    }

    /**
     * The result handler for videos processed using this class.
     * This defines what happens upon success or error for processing a video,
     * in the context of the job originating from implementing class that haven't overridden this.
     * @return The class to handle result of tasks started from this abstract class.
     */
    protected VideoResultHandler resultHandler() {

        Processor processIndex = this;

        return new VideoResultHandler() {

            @Override
            public void onSuccess(VideoInfo videoInfo, File srtFile) {
                processIndex.processedSuccess++;
            }

            @Override
            public void onError(VideoInfo videoInfo, Exception e) {
                processIndex.processedError.add(videoInfo);
            }
        };
    }

    protected static Integer getAmountToProcess(String arg0) {
        Integer amountToProcess = null;
        if(arg0 != null) {
            try  {
                amountToProcess = Integer.parseInt(arg0);
                log.info("Will process {} videos", amountToProcess);
            } catch (NumberFormatException e) {
                log.warn("Couldn't parse input parameter to int. Continuing without limit.");
                amountToProcess = null;
            }
        }
        return amountToProcess;
    }

    public void processVideos(final List<VideoInfo> videos, VideoResultHandler handler) {
        List<UUID> videosToProcess = new LinkedList<>();
        videos.forEach(it -> videosToProcess.add(it.getUuid()));

        log.info("Processing videos with output to: {}", videoMassProcessor.getSrtOutputDirPath().toString());
        LinkedMap<UUID, Future<File>> processedVideosResult = videoMassProcessor.processVideosAsync(videosToProcess);

        processedVideosResult.forEach((UUID k, Future<File> v) -> {
            VideoInfo videoInfo = firstVideoWithUUID(videos, k);
            log.info("Waiting for UUID: {} - Title: {}, to be done..", k.toString(), videoInfo.getTitle());

            File srtFile;
            try {
                srtFile = v.get();
                log.info("Done processing video UUID: {} - Title: {}", k.toString(), videoInfo.getTitle());
                handler.onSuccess(videoInfo, srtFile);

            } catch (InterruptedException e) {
                log.error("Process interrupted during processing of the video UUID: {} - Title: {}", k.toString(), videoInfo.getTitle(), e);
                handler.onError(videoInfo, e);
            } catch (ExecutionException e) {
                log.error("Error processing the video UUID: {} - Title: {}", k.toString(), videoInfo.getTitle(), e);
                handler.onError(videoInfo, e);
            }
        });
    }

    VideoInfo firstVideoWithUUID(List<VideoInfo> videoList, UUID uuid) {
        return videoList.stream().filter(it -> it.getUuid() == uuid).findFirst().get();
    }

    public Path getSrtOutputDirPath() {
        return videoMassProcessor.getSrtOutputDirPath();
    }
}
