package dk.kb.tvsubtitleocr.lib.preprocessing;

import dk.kb.tvsubtitleocr.lib.frameextraction.FrameExtractionProcessor;
import dk.kb.tvsubtitleocr.lib.frameextraction.VideoFrame;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TestRunOpenCV {

    private static FrameProcessorOpenCV frameProcessor;
    //    private static final Path imageLocation = Paths.get("/home/armo/Projects/subtitles-test/testdata/output1145.png");
    private static final Path imageLocation = Paths.get("/home/andreas/Projects/subtitles-test/originals/");
    private static final Path outputLocation = Paths.get("/home/andreas/Projects/subtitles-test/out/");
    int i = 0;

    @Test
    @Disabled
    void main() {
        TestRunOpenCV testRunOpenCV = new TestRunOpenCV();
        frameProcessor = new FrameProcessorOpenCV();

        testRunOpenCV.testRun();

    }

    @Test
    @Disabled
    public void generateRects() throws IOException {
        System.out.println("Start");
        FrameProcessorOpenCV frameProcessorOpenCV = new FrameProcessorOpenCV();
        FrameExtractionProcessor frameExtractionProcessor = new FrameExtractionProcessor(imageLocation.toFile());
        List<VideoFrame> videoFrames = frameExtractionProcessor.frameFilesToVideoFrame(frameExtractionProcessor.getFrameFiles(imageLocation.toFile()), 2800 * 1000, 2);
        System.out.println("Writing images");
        i = 0;
        for (VideoFrame vf : videoFrames) {
            RectangularData rectangularData = frameProcessorOpenCV.generateData(vf.getFrame());
            ImageIO.write(rectangularData.getFrame(), "png", new File(outputLocation + vf.getFileName()));
            i++;
        }
        i = 0;

    }

    private void testRun() {
        System.out.println("Start");
        List<Path> paths = getFrames(imageLocation);
        paths.sort(Path::compareTo);
        System.out.println(paths.size() + " number of frames\n" + "Populating frame list");
        List<BufferedImage> frames = new LinkedList<>();

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
        AtomicInteger printIndex = new AtomicInteger();
        printIndex.set(1);
        frames.replaceAll((BufferedImage frame) -> {
            BufferedImage img = frameProcessor.processFrame(frame);
            try {
                ImageIO.write(img, "png", new File(outputLocation.toFile(), printIndex + ""));
            } catch (IOException e) {
                e.printStackTrace();
            }
            printIndex.getAndIncrement();
            return img;
        });

        t1 = Instant.now();
        System.out.println("Processing done: " + Duration.between(t0, t1).toString() + " \n" +
                "Writing to disk");
        try {
            Files.createDirectories(Paths.get(imageLocation + "/output"));
        } catch (IOException e) {
            e.printStackTrace();
        }
//        try {
//            Files.delete(outputLocation);
//            Files.createDirectories(outputLocation);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        frames.forEach(x -> {
//            try {
////                File output = new File(outputLocation + "/out/" + String.valueOf(frames.indexOf(x)) + ".png");
////                ImageIO.write(x, "png", output);
////                File output = new File(outputLocation.toFile(), String.valueOf(frames.indexOf(x)) + ".png");
////                System.out.println(output);
//                ImageIO.write(x, "png", output);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        });
        System.out.println("Done");


    }

    private List<Path> getFrames(Path path) {
        try {
            return Files.list(path).filter(Files::isRegularFile).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
