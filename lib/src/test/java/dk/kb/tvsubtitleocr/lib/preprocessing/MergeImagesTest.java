package dk.kb.tvsubtitleocr.lib.preprocessing;
import dk.kb.tvsubtitleocr.lib.common.PropertiesFactory;
import dk.kb.tvsubtitleocr.lib.common.RuntimeProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class MergeImagesTest {
    final static Logger log = LoggerFactory.getLogger(MergeImagesTest.class);
    private RuntimeProperties properties;
    private static Path fileLocation = Paths.get("/path/to/folderOfInputFrames/");
    private static Path output = Paths.get("/path/to/resultOutput/");

    @BeforeEach
    void setUp() throws IOException {
        this.properties = PropertiesFactory.getProperties();
    }

    @Test
    @Disabled("For development experimentation and debugging only")
    public void Main() throws IOException {

        System.out.println("Preparing....");

        Instant timeStart;
        Instant timeStop;

        List<BufferedImage> images = getFrames(fileLocation);

        Files.createDirectories(output);


        timeStart = Instant.now();
        System.out.println("Starting");

        performanceTesting(images);
//        mergeMultiple2Images(images);
//        mergeAndSave(images);
//        merge2Images(img1, img2);
//        punctureImages(images);
//        mergeMultipleImages(images);
        System.out.println("The End");
        timeStop = Instant.now();

        log.info("Processing time: " +
                (Duration.between(timeStart, timeStop)));
    }

    /**
     * Purpose is to see how long it takes to process a full data set.
     * @param images List containing BufferedImages, from full dataset.
     */
    private void performanceTesting(List<BufferedImage> images) {

        boolean finished = false;
        int idx = 0;
        int subIdx = 0;

        // We assume that it takes an average of 3 to 5 images to preprocess an image.
        Random rnd = new Random();
        int rndHigh = 5;
        int rndLow = 3;
        int result;
        while (!finished) {

            result = rnd.nextInt(rndHigh - rndLow) + rndLow;
            List<BufferedImage> subImages = new ArrayList<>();
            for (int i = 0; i < result; i++) {
                subIdx = idx + i;
                subImages.add(images.get(subIdx));
                MergeImages.mergeImages(subImages);
            }
            MergeImages.mergeImages(subImages);

            if(idx == images.size())
                finished = true;
            idx++;
        }

    }

    private void mergeAndSave(List<BufferedImage> images) throws IOException {
        BufferedImage working = images.get(0);
        for (int i = 1; i < images.size(); i++) {
            BufferedImage img = MergeImages.mergeImages(Arrays.asList(images.get(i), working));

//            BufferedImage img = MergeImages.mergeImages(images);
//            ImageIO.write(img, "png", Paths.get(output.toString(), i + ".png").toFile());
            ImageIO.write(img, "png", Paths.get(output.toString(), i + ".png").toFile());
        }

        ImageIO.write(MergeImages.mergeImages(images), "png", Paths.get(output.toString(), "allMerged.png").toFile());

    }

    private void mergeMultiple2Images(Collection<BufferedImage> images) throws IOException {
        Iterator<BufferedImage> itr = images.iterator();
        BufferedImage working = images.iterator().next();
        int i = 0;
        while (itr.hasNext()) {
            working = MergeImages.mergeImages(Arrays.asList(working, itr.next()));
            ImageIO.write(working, "png", new File("output/" + i + ".png"));
            i++;
            int ii = 0;
        }
    }

    //    private void punctureImages(BufferedImage img1, BufferedImage img2) {
    private void punctureImages(Collection<BufferedImage> images) {
        Iterator<BufferedImage> itr = images.iterator();
        BufferedImage working = images.iterator().next();
        while (itr.hasNext()) {
            working = MergeImages.punctureImage(itr.next(), working);
        }
        int i = 0;

    }

    private void merge2Images(BufferedImage img, BufferedImage img2) {
        BufferedImage image = MergeImages.mergeImages(img, img2);
        int i = 0;
    }

    private void mergeMultipleImages(List<BufferedImage> images) throws IOException {
        BufferedImage image = MergeImages.mergeImages(images);
        ImageIO.write(image, "png", new File("1.png"));
        int i = 0;
    }

    private List<BufferedImage> getFrames(Path path) {
        List<Path> paths;
        try {
            paths = Files.list(path).filter(Files::isRegularFile).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        paths.sort(Path::compareTo);
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
        return frames;
    }
}
