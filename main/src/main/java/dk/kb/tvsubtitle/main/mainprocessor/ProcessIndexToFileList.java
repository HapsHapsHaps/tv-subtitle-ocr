package dk.kb.tvsubtitle.main.mainprocessor;

import dk.kb.tvsubtitle.main.model.VideoInfo;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class ProcessIndexToFileList extends ProcessIndexToFiles {
    private final static Logger log = LoggerFactory.getLogger(ProcessIndexToFileList.class);
    protected Integer maxToProcess;
    protected int amountPerRun = 50;
    protected int startRow = 0;
    protected boolean exit = false;

    public static void main(String[] args) throws ParseException {
        Option processAmountOption = Option.builder("m")
                .longOpt("max")
                .desc("The maximum amount to process this run, this is optional")
                .hasArg(true)
                .argName("AMOUNT")
                .build();
        Option outputFileOption = Option.builder("out")
                .longOpt("outputFile")
                .hasArg(true)
                .desc("Full path with filename, for where to save the file with a list of videoUUIDs with srt content.")
                .required(true)
                .build();
        CommandLine cmd = handleProgramArguments(args, processAmountOption, outputFileOption);

        log.info("Starting getting all video UUIDs with an SRT file, and saving them in a file list."); // Todo: Figure out wording

        Integer amountToProcess = null;
        if( cmd.hasOption( "processAmount" )) {
            amountToProcess = Integer.parseInt(cmd.getOptionValue("processAmount"));
            log.info("Will process {} videos.", amountToProcess.toString());
        }

        log.info("Will fetch {} videos with SRT", amountToProcess == null ? "all" : String.valueOf(amountToProcess));

        log.info("Setting up program.");
        ProcessIndexToFileList processIndexToFileList = new ProcessIndexToFileList();
        processIndexToFileList.maxToProcess = amountToProcess;
        Path outputFilePath = Paths.get(cmd.getOptionValue("outputFile"));

        log.info("Running program.");
        processIndexToFileList.process(outputFilePath);

        log.info("Program has gracefully shut down.");
        System.exit(0);
    }

    /**
     * If {@link #maxToProcess}, set in program arguments is null:
     * This method will fetch all tv elements with an SRT field set
     *
     * If {@link #maxToProcess}, set in program arguments is not null:
     * This method will fetch {@link #maxToProcess} amount of elements with an SRT field set.
     */
    public void process(Path outputFilePath) {
        List<VideoInfo> videoInfoLists = new LinkedList<>();

        if (maxToProcess != null && amountPerRun > maxToProcess) {
            amountPerRun = maxToProcess;
        }

        if(Files.exists(outputFilePath)) {
            try {
                Files.delete(outputFilePath);
            } catch (IOException e) {
                throw new RuntimeException("Couldn't delete existing outputFilePath. Path: " + outputFilePath.toString());
            }
        }

        while (!this.exit) {
            List<VideoInfo> videoInfos = find(this.startRow, this.amountPerRun);
            videoInfoLists.addAll(videoInfos);
            this.startRow += amountPerRun; // Increments the rowCount to start next index request at.
            log.info("Fetched {} more. Total fetched is {}", this.amountPerRun, this.startRow);
            if (videoInfos.size() != amountPerRun || videoInfos.stream().anyMatch(Objects::isNull))
                exit = true;
        }
        write(videoInfoLists, outputFilePath);


    }

    protected void write(List<VideoInfo> videoInfoLists, Path path) {
        List<String> uuids = new LinkedList<>();
        videoInfoLists.forEach(videoInfo -> uuids.add(videoInfo.getUuid().toString()));
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(path.toFile()));
            for (String s : uuids) {
                writer.write(s + "\n");
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected List<VideoInfo> find(int startRow, int rowCount) {
        return videoIndexClient.getVideosWithSRT(startRow, rowCount);
    }
}
