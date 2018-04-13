package dk.kb.tvsubtitleocr.lib.preprocessing;
import com.sun.imageio.plugins.png.PNGMetadata;
import dk.kb.tvsubtitleocr.lib.common.PropertiesFactory;
import dk.kb.tvsubtitleocr.lib.common.RuntimeProperties;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public class TestRun {

    private static FrameProcessor frameProcessor;
    private static final Path imageLocation = Paths.get("frame-processing/src/main/resources/testdata/");
    private static RuntimeProperties properties;
//    private static final Path imageLocation = Paths.get("/home/armo/Documents/temp/");

    @Test
    @Disabled
    void main() throws IOException {

        properties = PropertiesFactory.getProperties();
        frameProcessor = new FrameProcessor();
        TestRun tr = new TestRun();


        Instant t0, t1;
        t0 = Instant.now();
        System.out.println("Initializing");
        tr.testRun();
        t1 = Instant.now();
        System.out.println("That took: " + Duration.between(t0, t1).getSeconds() + " seconds");
        //frameProcessor.processFrame(imageLocation);
    }

    private void testRun() {
        System.out.println("Start");
        List<Path> paths = getFrames(imageLocation);
        System.out.println(paths.size() + " number of frames\n" +
                "Populating frame list");
        List<BufferedImage> frames = new ArrayList<>();
        for (Path p : paths) {
            BufferedImage img = null;
            try {
                img = ImageIO.read(p.toFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
            frames.add(img);
        }

        Instant t0, t1;

        System.out.println("Processing images: " + frames.size());
        t0 = Instant.now();
        frames.forEach(p -> frameProcessor.processFrame(p));
        //List<BufferedImage> processed = frames.stream().map(this::processFrame).collect(Collectors.toList());
        t1 = Instant.now();
        System.out.println("Processing done: " + Duration.between(t0, t1).getSeconds());


    }

    private void testRun(Path path) {

        int processed = 0;
        List<Path> frames = getFrames(path);
        BufferedImage image;
        checkWorkingDirectory(path);
        for (Path p : frames) {
            //if (p.endsWith("testing.png"))
            try {
                image = frameProcessor.processFrame(ImageIO.read(p.toFile()));

                PNGMetadata metadata = new PNGMetadata();
                setDPI(metadata);


                File output = new File(path.toString() + String.format("/out/processed-%1$d_%2$s", processed, p.getFileName()));


                final String formatName = "png";

                for (Iterator<ImageWriter> iw = ImageIO.getImageWritersByFormatName(formatName); iw.hasNext(); ) {
                    ImageWriter writer = iw.next();

                    try (final ImageOutputStream stream = ImageIO.createImageOutputStream(output)) {
                        writer.setOutput(stream);
                        ImageWriteParam writeParam = writer.getDefaultWriteParam();
                        writer.write(metadata, new IIOImage(image, null, metadata), writeParam);
                    } finally {
                        writer.dispose();
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            processed++;
        }
    }

    @SuppressWarnings("Duplicates")
    private void setDPI(IIOMetadata metadata) throws IIOInvalidTreeException {

        // for PMG, it's dots per millimeter

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

    /**
     * Generates a Path list of files inside the directory path points to.
     *
     * @param path Path to directory
     * @return List of Paths inside path Directory
     */
    private List<Path> getFrames(Path path) {
        List<Path> fileNames = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get(path.toString()))) {
            paths
                    .filter(Files::isRegularFile)
                    .forEach(fileNames::add);


        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileNames;
    }

    private boolean checkWorkingDirectory(Path path) {
        if (Files.exists(Paths.get(path.toString() + "/out/")))
            return true;
        else {
            try {
                Files.createDirectory(Paths.get(path.toString() + "/out/"));
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }
}