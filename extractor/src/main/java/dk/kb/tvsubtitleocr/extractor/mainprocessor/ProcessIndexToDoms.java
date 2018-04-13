package dk.kb.tvsubtitleocr.extractor.mainprocessor;
import dk.kb.tvsubtitleocr.extractor.externalservice.SrtDomsClient;
import dk.kb.tvsubtitleocr.lib.common.RuntimeProperties;
import dk.kb.tvsubtitleocr.extractor.model.VideoInfo;
import dk.kb.tvsubtitleocr.extractor.model.VideoResultHandler;
import dk.statsbiblioteket.doms.central.connectors.BackendInvalidResourceException;
import dk.statsbiblioteket.doms.central.connectors.BackendMethodFailedException;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.stream.Collectors;

public class ProcessIndexToDoms extends ProcessIndexToFiles {
    private final static Logger log = LoggerFactory.getLogger(ProcessIndexToDoms.class);
    protected final String doneMessage = "Done processing from index to Doms.";
    private boolean exit = false;
    private final boolean replaceExistingSRT;
    protected SrtDomsClient domsClient;
    protected int amountPerRun = 50;
    protected int startRow = 0;
    protected Integer maxToProcess;
    protected static Options options = new Options();

    /**
     * Not for development. Is used to run the program from run script. Use disabled test instead.
     * @param args
     */
    public static void main(String[] args) throws ParseException {
        Option processAmountOption = Option.builder("m")
                .longOpt("max")
                .desc("The maximum amount to process this run, this is optional")
                .hasArg(true)
                .argName("AMOUNT")
                .build();
        CommandLine cmd = handleProgramArguments(args, processAmountOption);

        log.info("Staring video with OCR to text, with processing from Index to DOMS.");

        Integer amountToProcess = null;
        String processAmountString = cmd.getOptionValue("processAmount");
        if(processAmountString != null) {
            amountToProcess = Integer.parseInt(processAmountString);
            log.info("Will process {} videos.", processAmountString);
        }

        log.info("Setting up program.");
        ProcessIndexToDoms processIndexToDoms = new ProcessIndexToDoms();
        processIndexToDoms.maxToProcess = amountToProcess;

        log.info("Running program.");
        processIndexToDoms.process();

        log.info("Program has gracefully shut down.");
        System.exit(0);
    }

    public ProcessIndexToDoms() {
        super();
        this.replaceExistingSRT = false;

        String serverAddress = properties.getProperty(RuntimeProperties.ResourceName.domsServerAddress);
        String userName = properties.getProperty(RuntimeProperties.ResourceName.domsUserName);
        String password = properties.getProperty(RuntimeProperties.ResourceName.domsPassword);
        try {
            this.domsClient = new SrtDomsClient(serverAddress, userName, password);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Wrong server address for doms server", e);
        }
    }

    /**
     * Keeps getting new video UUIDs to process every time its done with a batch.
     * This way the program stays running, and works its way through the index.
     */
    public void process() {

        if(maxToProcess != null && amountPerRun > maxToProcess) {
            amountPerRun = maxToProcess;
        }

        while( ! this.exit ) {
            process(this.startRow, this.amountPerRun);
            this.startRow += amountPerRun; // Increments the rowCount to start next index request at.
            log.info("Processed {} more. Total processed is {}", this.amountPerRun, this.startRow);

            if (this.maxToProcess != null && this.maxToProcess <= this.startRow) {
                this.exit = true;
                break;
            }
        }
    }

    @Override
    public void process(int startRow, int amount) {
        List<VideoInfo> indexVideos;
        if(replaceExistingSRT) {
            indexVideos = videoIndexClient.getVideosWithSRT(startRow, amount);
        }
        else {
            indexVideos = videoIndexClient.getVideosWithoutSRT(startRow, amount);
            indexVideos = removeAlreadyExistInDoms(indexVideos);
        }

        if(indexVideos.size() == 0) {
            // No more videos to process.
            log.info("No more videos left to process. Exiting gracefully.");
            this.exit = true;
            return;
        }

        super.process(indexVideos, this.resultHandler(), this.doneMessage);
    }

    /**
     * Goes through the given list with Video UUID, and removes all those that already have SRT content in Doms.
     * @param indexVideos The list with Video UUID to go through.
     * @return The originally ordered list without items that already have SRT in Doms.
     */
    public List<VideoInfo> removeAlreadyExistInDoms(List<VideoInfo> indexVideos) {
        List<VideoInfo> result = indexVideos.stream().filter(it -> {
            try {
                return domsClient.getSRTContent(it.getUuid()) == null; // null when it doesn't have SRT for UUID.
            } catch (BackendMethodFailedException e) {
                throw new RuntimeException("Failed getting srt information from Doms server. VideoUUID: " + it.getUuid(), e);
            }
        }).collect(Collectors.toList());
        return result;
    }

    /**
     * The result handler for videos processed using this class.
     * This defines what happens upon success or error for processing a video,
     * in the context of the job originating from this class.
     * @return The class to handle result of tasks started from this parent class.
     */
    @Override
    public VideoResultHandler resultHandler() {
        ProcessIndexToDoms processIndexToDoms = this;

        return new VideoResultHandler() {

            @Override
            public void onSuccess(VideoInfo videoInfo, File srtFile) {

                try {
                    processIndexToDoms.domsClient.addContent(videoInfo.getUuid(), srtFile);
                    processIndexToDoms.processedSuccess++;

                    FileUtils.deleteQuietly(srtFile);
                } catch (BackendInvalidResourceException e) {
                    log.error("No document resource in doms server for videoUUID: {}. " +
                            "Declaring video as failed.", videoInfo.getUuid(), e);
                    this.onError(videoInfo, e);
                } catch (BackendMethodFailedException e) {
                    log.error("Something very unexpected happened with the doms server while processing videoUUID: {}. " +
                            "It might have crashed? Declaring video as failed", videoInfo.getUuid(), e);
                    this.onError(videoInfo, e);
                } catch (IOException e) {
                    log.error("IOException when reading SRT file to add to doms, for videoUUID: {}", videoInfo.getUuid(), e);
                    this.onError(videoInfo, e);
                }
            }

            @Override
            public void onError(VideoInfo videoInfo, Exception e) {
                processIndexToDoms.processedError.add(videoInfo);
            }
        };
    }

    public int getAmountPerRun() {
        return amountPerRun;
    }

    public void setAmountPerRun(int amountPerRun) {
        this.amountPerRun = amountPerRun;
    }
}
