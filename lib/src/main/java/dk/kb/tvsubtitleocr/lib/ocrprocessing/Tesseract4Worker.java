package dk.kb.tvsubtitleocr.lib.ocrprocessing;
import com.sun.imageio.plugins.png.PNGMetadata;
import dk.statsbiblioteket.util.console.ProcessRunner;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Tesseract4Worker implements IOCRProcessor {
    final static Logger log = LoggerFactory.getLogger(Tesseract4Worker.class);
    private final static String imageFileName = "tesseractInputImage.png";
    private final String tesseractExecutablePath;
    private final List<String> defaultArguments;
    private final Map<String, String> environmentVariables;
    private final File parentWorkDir;

    public Tesseract4Worker(
            Random random, String tesseractExecutablePath,
            List<String> defaultArguments,
            Map<String, String> environmentVariables,
            File parentWorkDir) {
        this.tesseractExecutablePath = tesseractExecutablePath;
        this.defaultArguments = defaultArguments;
        this.environmentVariables = environmentVariables;

        this.parentWorkDir = parentWorkDir;
    }

    @Override
    public List<String> ocrImage(BufferedImage sourceImage) throws IOException {
        List<String> result;

        Path taskWorkDir = Files.createTempDirectory(parentWorkDir.toPath(), "ocrWorker");

        Path imagePath = Paths.get(taskWorkDir.toString(), imageFileName);

        File imageFile = saveImageToDisk(sourceImage, imagePath);

        result = ocrImage(imageFile, taskWorkDir);

        //Cleanup
        Boolean cleanupResult = FileUtils.deleteQuietly(taskWorkDir.toAbsolutePath().toFile());
        if( ! cleanupResult) {
            log.error("Couldn't cleanup instance taskWorkDir: " + taskWorkDir);
        }
        return result;
    }

    public List<String> ocrImage(File sourceImage, Path taskWorkDir) throws IOException {
        String tesseractExtensionPre = ".tesseractResult";
        //For some reason Tesseract insists on adding .txt to the result file.
        String tesseractExtensionPost = tesseractExtensionPre + ".txt";

        File resultTextFile = new File(taskWorkDir.toString(),
                sourceImage.getName() + tesseractExtensionPost);

        List<String> arguments = new LinkedList<>(Arrays.asList(
                sourceImage.getAbsolutePath(),
                resultTextFile.getAbsolutePath().replace(tesseractExtensionPost, tesseractExtensionPre)
        ));

        arguments.addAll(defaultArguments); // Adds all predefined default arguments to these instance arguments.

        ProcessStreamOutputs processOutput = runProcess(tesseractExecutablePath, arguments, environmentVariables);

        // The tesseract process failed during processing.
        handleProcessOutput(processOutput, sourceImage);

        List<String> result = textFileToNonEmptyStringList(resultTextFile);

        //Cleanup
        if( ! resultTextFile.delete() && Files.exists(resultTextFile.toPath())) {
            log.warn("File Couldn't be removed from storage medium. Path: {}", resultTextFile.getAbsolutePath());
        }

        return result;
    }

    /**
     * Takes a ProcessStreamOutputs model and handles the output + errors that may have occurred.
     * This errors and outputs that need to be logged, is getting logged. Non-error messages is skipped.
     * @param processOutput
     */
    private void handleProcessOutput(ProcessStreamOutputs processOutput, File sourceImage) {
        if(processOutput.getReturnCode() != 0) {
            log.error("Tesseract process failed during processing for image {}.", sourceImage);
        }
        processOutput.getErrorInput().forEach(it -> {
            if(it.contains("Tesseract Open Source OCR Engine") && it.contains("with Leptonica")) {
                log.debug(it);
            }
            else {
                log.error(it);
            }
        });
        processOutput.getStandardOutput().forEach(it -> {
            if(!it.equals(""))
            {
                log.info(it);
            }
        });
    }

    /**
     * Goes trough every line in the input file, where it trims and adds non empty lines to a list.
     * @param resultTextFile The file to parse through.
     * @return The content of the resultTextFile.
     * @throws IOException if an I/O error occurs opening the file
     */
    private static List<String> textFileToNonEmptyStringList(File resultTextFile) throws IOException {
        List<String> result;

        try (Stream<String> stream = Files.lines(resultTextFile.toPath())) {
            result = stream.map(String::trim).filter(newS -> !newS.isEmpty()).collect(Collectors.toCollection(LinkedList::new));
        }

        return result;
    }

    /**
     * Saves the given BufferedImage to the location of the given path.
     * @param image The BufferedImage to write to disk
     * @param imageFile The filesystem location to save the image to.
     * @return The File representation of the images location in the filesystem.
     * @throws IOException Setting image meta data or writing the image to disk failed.
     */
    private File saveImageToDisk(BufferedImage image, Path imageFile) throws IOException {
        PNGMetadata metadata = new PNGMetadata();
        setDPI(metadata); // Defines DPI information for the PNG image format.

        String formatName = "png";
        File resultFile = imageFile.toFile();

        for (Iterator< ImageWriter > iw = ImageIO.getImageWritersByFormatName(formatName); iw.hasNext(); ) {
            ImageWriter writer = iw.next();

            try (final ImageOutputStream stream = ImageIO.createImageOutputStream(resultFile)) {
                writer.setOutput(stream);
                ImageWriteParam writeParam = writer.getDefaultWriteParam();
                writer.write(metadata, new IIOImage(image, null, metadata), writeParam);
            } finally {
                writer.dispose();
            }
        }

        return resultFile;
    }

    /**
     * Defines the dpi values for the PNG file type.
     * @param metadata the instance to add the PNG DPI data to.
     * @throws IIOInvalidTreeException if the tree cannot be parsed successfully using the rules of the given format.
     */
    private static void setDPI(IIOMetadata metadata) throws IIOInvalidTreeException {

        // for PNG, it's dots per millimeter

        double dotsPerMilli = 1.0 * 300 / 10 / 2.541f;
        IIOMetadataNode horiz = new IIOMetadataNode("HorizontalPixelSize");
        horiz.setAttribute("value", Double.toString(dotsPerMilli));

        IIOMetadataNode vert = new IIOMetadataNode("VerticalPixelSize");
        vert.setAttribute("value", Double.toString(dotsPerMilli));

        IIOMetadataNode dim = new IIOMetadataNode("Dimension");
        dim.appendChild(horiz);
        dim.appendChild(vert);

        IIOMetadataNode root = new IIOMetadataNode("javax_imageio_1.0");
        root.appendChild(dim);

        metadata.mergeTree("javax_imageio_1.0", root);
    }

    protected static ProcessStreamOutputs runProcess(
            String command, List<String> arguments, Map<String, String> environmentVariables) {

        //Build the process.
        ProcessRunner runner = new ProcessRunner(command, arguments);

        runner.setErrorCollectionByteSize(-1);
        runner.setOutputCollectionByteSize(-1);

        runner.setEnviroment(environmentVariables);

        runner.run();

        //Prepare the returned result with the stdin and stderr stream data.
        ProcessStreamOutputs result =  new ProcessStreamOutputs(
                Arrays.asList(runner.getProcessOutputAsString().split("\n")),
                Arrays.asList(runner.getProcessErrorAsString().split("\n")),
                runner.getReturnCode()
        );
//        processWatch(result);
        return result;
    }

    /**
     * Prints the content of the STDOUT and STDERR output Lists to console.
     * @param output The ProcessStreamOutputs instance to print all content from.
     */
    private static void processWatch(ProcessStreamOutputs output) {
            output.getStandardOutput().forEach(s -> {
                if( ! s.isEmpty()) log.info(s);
            });
            output.getErrorInput().forEach(s -> {
                if( ! s.isEmpty()) log.error(s);
            });
    }
}
