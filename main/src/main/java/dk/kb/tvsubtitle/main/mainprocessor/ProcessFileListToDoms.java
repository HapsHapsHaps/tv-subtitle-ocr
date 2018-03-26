package dk.kb.tvsubtitle.main.mainprocessor;

import dk.kb.tvsubtitle.main.model.VideoInfo;
import dk.kb.tvsubtitle.main.model.VideoResultHandler;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

public class ProcessFileListToDoms extends ProcessFileListToFiles {
    ProcessIndexToDoms processIndexToDoms;
    /**
     * Not for development. Is used to run the program from run script. Use disabled test instead.
     * @param args
     */
    public static void main(String[] args) throws ParseException {
        Option forceOption = Option.builder("f")
                .longOpt("force")
                .hasArg(false)
                .desc("Force overwrite option, to replace any that already exist.")
                .build();
        Option inpuFileOption = Option.builder("in")
                .longOpt("inputFile")
                .hasArg(true)
                .desc("text file with a list of videoUUIDs to process.")
                .required(true)
                .build();
        CommandLine cmd = handleProgramArguments(args, forceOption, inpuFileOption);

        log.info("Staring video with OCR to text, with processing from file with list of UUIDs to Doms.");

        log.info("Setting up program.");
        ProcessFileListToDoms processFileListToDoms = new ProcessFileListToDoms();

        if(cmd.hasOption("f") || cmd.hasOption("force")) {
            processFileListToDoms.overWrite = true;
            log.info("force overwrite is enabled. Will add SRT content to video that already have SRT.");
        }

        String inputFileString = cmd.getOptionValue("inputFile");
        File inputFile = getFile(Paths.get(inputFileString));

        log.info("Running program.");
        processFileListToDoms.process(inputFile);

        log.info("Program has gracefully shut down.");
        System.exit(0);
    }

    ProcessFileListToDoms() {
        super();
        this.processIndexToDoms = new ProcessIndexToDoms();
    }

    @Override
    public void process(File inputFile) {
        List<VideoInfo> videos = getVideosToProcess(inputFile);

        processVideos(videos, resultHandler());

        processedSuccess = processIndexToDoms.processedSuccess;
        processedError = processIndexToDoms.processedError;

        logFinalProcessingInformation(videos.size(), this.doneMessage);

        log.info("Result SRT files can be found in the following directory: {}", super.getSrtOutputDirPath().toString());
    }

    @Override
    protected VideoResultHandler resultHandler() {
        return processIndexToDoms.resultHandler();
    }
}
