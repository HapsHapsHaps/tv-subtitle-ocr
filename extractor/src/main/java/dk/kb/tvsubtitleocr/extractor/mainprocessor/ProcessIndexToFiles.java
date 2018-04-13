package dk.kb.tvsubtitleocr.extractor.mainprocessor;

import dk.kb.tvsubtitleocr.extractor.externalservice.VideoIndexClient;
import dk.kb.tvsubtitleocr.lib.common.RuntimeProperties;
import dk.kb.tvsubtitleocr.extractor.model.VideoInfo;
import dk.kb.tvsubtitleocr.extractor.model.VideoResultHandler;
import org.apache.commons.cli.*;

import java.util.List;

public class ProcessIndexToFiles extends Processor {
    protected final String doneMessage = "Done processing from index to files.";
    protected VideoIndexClient videoIndexClient;
    protected static Options options = new Options();

    /**
     * Not for development. Is used to run the program from run script. Use disabled test instead.
     * @param args
     */
    public static void main(String[] args) throws ParseException {
        Option processAmountOption = Option.builder("m")
                .longOpt("max")
                .desc("The maximum amount to process this run, this is required")
                .hasArg(true)
                .argName("AMOUNT")
                .required()
                .build();
        CommandLine cmd = handleProgramArguments(args, processAmountOption);

        log.info("Staring video with OCR to text, with processing from Index to files on disk.");

        String processAmountString = cmd.getOptionValue("processAmount"); // Required to be set.
        Integer amountToProcess = Integer.parseInt(processAmountString);

        log.info("Will process {} videos", amountToProcess);

        log.info("Setting up program.");
        ProcessIndexToFiles processIndexToFiles = new ProcessIndexToFiles();

        log.info("Running program.");
        processIndexToFiles.process(0, amountToProcess);

        log.info("Program has shut down.");
        System.exit(0);
    }

    public ProcessIndexToFiles() {
        super();
        String indexServerUrl = properties.getProperty(RuntimeProperties.ResourceName.indexServerUrl);;
        this.videoIndexClient = new VideoIndexClient(indexServerUrl);
    }

    public void process(int startRow, int amount) {
        List<VideoInfo> indexVideos = videoIndexClient.getVideosWithoutSRT(startRow, amount);

        process(indexVideos, super.resultHandler(), this.doneMessage);

        log.info("Result SRT files can be found in the following directory: {}", super.getSrtOutputDirPath().toString());
    }

    @Override
    public void process(List<VideoInfo> videos, VideoResultHandler resultHandler, String doneMessage) {
        super.process(videos, resultHandler, doneMessage);
    }
}
