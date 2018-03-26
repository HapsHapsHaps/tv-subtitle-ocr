package dk.kb.tvsubtitle.main.mainprocessor;

import com.google.common.base.Charsets;
import dk.kb.tvsubtitle.main.externalservice.SrtDomsClient;
import dk.kb.tvsubtitle.main.model.VideoInfo;
import dk.statsbiblioteket.doms.central.connectors.BackendMethodFailedException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * For processing a file that is a list of video UUIDs, and saving the srt results to files.
 */
public class ProcessFileListToFiles extends Processor {
    protected final String doneMessage = "Done processing from list of UUIDs to files.";
    protected boolean overWrite = false;
    protected SrtDomsClient domsClient;
    protected static Options options = new Options();

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

        log.info("Staring video with OCR to text, with processing from file with list of UUIDs to SRT files.");

        log.info("Setting up program.");
        ProcessFileListToFiles processFileListToFiles = new ProcessFileListToFiles();

        if(cmd.hasOption("f") || cmd.hasOption("force")) {
            processFileListToFiles.overWrite = true;
            log.info("force overwrite is enabled. Will overwrite SRT content that already exist.");
        }

        String inputFileString = cmd.getOptionValue("inputFile");
        File inputFile = getFile(Paths.get(inputFileString));

        log.info("Running program.");
        processFileListToFiles.process(inputFile);

        log.info("Program has gracefully shut down.");
        System.exit(0);
    }

    public ProcessFileListToFiles() {
        super();

        String serverAddress = "http://alhena:7980/fedora";
        String userName = "fedoraAdmin";
        String password = "fedoraAdminPass";
        try {
            this.domsClient = new SrtDomsClient(serverAddress, userName, password);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Wrong server address for doms server", e);
        }
    }

    public void process(File inputFile) {

        List<VideoInfo> videos = getVideosToProcess(inputFile);

        process(videos, resultHandler(), this.doneMessage);

        log.info("Result SRT files can be found in the following directory: {}", super.getSrtOutputDirPath().toString());
    }

    protected List<VideoInfo> getVideosToProcess(File inputFile) {
        List<VideoInfo> result = new ArrayList<>();
        List<UUID> videoUuids;

        try {
            videoUuids = getUUUIDsFromFile(inputFile);
        } catch (IOException e) {
            throw new RuntimeException("Error reading file content: " + inputFile.getAbsolutePath(), e);
        }

        for (UUID uuid : videoUuids) {
            boolean append = true;
            if ( ! overWrite) {
                try {
                    append = !domsClient.exist(uuid);
                } catch (BackendMethodFailedException e) {
                    throw new RuntimeException("Communication with Doms server failed. When getting info about videoUUID: " + uuid, e);
                }
            }

            if (append) {
                VideoInfo videoInfo = new VideoInfo(uuid, "undefined", null);
                result.add(videoInfo);
            }
        }

        return result;
    }

    protected List<UUID> getUUUIDsFromFile(File file) throws IOException {
        final List<UUID> result = new ArrayList<>();

        List<String> strings = FileUtils.readLines(file, Charsets.UTF_8);
        for (String s : strings) {
            String sTrimmed = s.trim();
            if( ! sTrimmed.isEmpty()) {
                try {
                    UUID uuid = UUID.fromString(sTrimmed);
                    result.add(uuid);
                } catch (IllegalArgumentException e) {
                    log.error("Error parsing UUID {} to a UUID", s);
                }
            }
        }

        return result;
    }

    public static File getFile(Path filePath) {
        if( ! Files.exists(filePath)) {
            throw new RuntimeException("The file with list of video UUIDs doesn't exist. Path: " + filePath.toString());
        }
        else if( ! Files.isReadable(filePath)) {
            throw new RuntimeException("The file with list of video UUIDs isn't readable. Path: " + filePath.toString());
        }

        return filePath.toFile();
    }
}
