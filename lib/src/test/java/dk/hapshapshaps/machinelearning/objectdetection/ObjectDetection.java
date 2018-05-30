package dk.hapshapshaps.machinelearning.objectdetection;


import dk.hapshapshaps.machinelearning.objectdetection.CustomObjectDetector;
import dk.hapshapshaps.machinelearning.objectdetection.models.Box;
import dk.hapshapshaps.machinelearning.objectdetection.models.ObjectRecognition;
import dk.hapshapshaps.machinelearning.objectdetection.models.RectFloats;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ObjectDetection {
    // If tests fail, make sure to point to your model and label files
    private File modelfile = new File("/home/andreas/Projects/machine-learning/Object-detection-files/frozen_inference_graph.pb"); // Trained model
    private File labelfile = new File("/home/andreas/Projects/machine-learning/Object-detection-files/object-detection.pbtxt");

    File inputDirectory = new File("/home/andreas/Projects/subtitle-work/frameExtraction/");
    File resultDirectory = new File("/home/andreas/Projects/subtitle-work/out/"); // Test image -> Output

    @Test
    public void canStartCustomDetector() throws IOException {
        CustomObjectDetector detector = new CustomObjectDetector(modelfile, labelfile);
    }

    @Test
    public void canDetect() throws IOException {

        // Arrange
        CustomObjectDetector detector = new CustomObjectDetector(modelfile, labelfile);
        BufferedImage testImage = ImageIO.read(new File("/home/andreas/Projects/subtitle-work/frameExtraction/00057.png"));

        // Act
        ArrayList<ObjectRecognition> recognitions = detector.classifyImage(testImage);
        List<Box> boxes = toBoxes(recognitions);
        BufferedImage bufferedImage = drawBoxes(testImage, boxes);

        //Assert -> Manual test, check output image
        ImageIO.write(bufferedImage, "png", new File(resultDirectory + "out.png"));
    }

    @Disabled
    @Test
    public void canDetectDirectory() throws IOException {

        // Arrange
        CustomObjectDetector detector = new CustomObjectDetector(modelfile, labelfile);
        File[] files = inputDirectory.listFiles();
        Arrays.sort(files);

        // Act
        for (File f :
                files) {
            BufferedImage input = ImageIO.read(f);
            List<ObjectRecognition> recognitions = detector.classifyImage(input);
            BufferedImage resultFrame = drawBoxes(input, toBoxes(recognitions));
            ImageIO.write(resultFrame, "png", Paths.get(resultDirectory.getAbsolutePath(), f.getName()).toFile());
        }
        // Assert -> Manual test, check output directory
        int i = 0;
    }

    @Disabled
    @Test
    public void canDetectDirectoryMultithreadded() throws IOException {

        // Arrange
        ExecutorService executors = Executors.newFixedThreadPool(4);
        List<Future<BufferedImage>> futures = new LinkedList<>();
        CustomObjectDetector detector = new CustomObjectDetector(modelfile, labelfile);
        File[] files = inputDirectory.listFiles();
        Arrays.sort(files);

        // Act
        for (File f : files) {

            Callable callable = (Callable<BufferedImage>) () -> {
                BufferedImage input = ImageIO.read(f);
                ArrayList<ObjectRecognition> recognitions = detector.classifyImage(input);
                return drawBoxes(input, toBoxes(recognitions));
            };
            Future future = executors.submit(callable);
            futures.add(future);
        }
        AtomicInteger i = new AtomicInteger();
        futures.forEach(bufferedImageFuture -> {
            try {
                ImageIO.write(bufferedImageFuture.get(), "png", Paths.get(resultDirectory.getAbsolutePath(), String.valueOf(i.getAndIncrement() + ".png")).toFile());
            } catch (InterruptedException | ExecutionException | IOException e) {
                e.printStackTrace();
            }
        });



        // Assert -> Manual test, check output directory
        i.set(0);
    }

    @Disabled
    @Test
    public void canDetectDirectoryMultithreaddedMultiDetector() throws IOException {

        // Arrange
        ExecutorService executors = Executors.newFixedThreadPool(4);
        List<Future<BufferedImage>> futures = new LinkedList<>();
        File[] files = inputDirectory.listFiles();
        Arrays.sort(files);

        // Act
        for (File f : files) {

            Callable callable = (Callable<BufferedImage>) () -> {
                CustomObjectDetector detector = new CustomObjectDetector(modelfile, labelfile); // Moved
                BufferedImage input = ImageIO.read(f);
                ArrayList<ObjectRecognition> recognitions = detector.classifyImage(input);
                return drawBoxes(input, toBoxes(recognitions));
            };
            Future future = executors.submit(callable);
            futures.add(future);
        }
        AtomicInteger i = new AtomicInteger();
        futures.forEach(bufferedImageFuture -> {
            try {
                ImageIO.write(bufferedImageFuture.get(), "png", Paths.get(resultDirectory.getAbsolutePath(), String.valueOf(i.getAndIncrement() + ".png")).toFile());
            } catch (InterruptedException | ExecutionException | IOException e) {
                e.printStackTrace();
            }
        });



        // Assert -> Manual test, check output directory
        i.set(0);
    }


    // Util
    private static BufferedImage drawBoxes(BufferedImage image, List<Box> boxes) {
        Graphics2D graph = image.createGraphics();
        graph.setColor(Color.green);

        for (Box box : boxes) {
            graph.drawRect(box.x, box.y, box.width, box.height);
        }

        graph.dispose();
        return image;
    }

    private static List<Box> toBoxes(List<ObjectRecognition> recognitions) {
        List<Box> boxes = new ArrayList<>();
        for (ObjectRecognition recognition : recognitions) {
            if (recognition.getConfidence() > 0.05f && recognition.getTitle().toLowerCase().equals("sub")) {
                RectFloats location = recognition.getLocation();
                int x = (int) location.getX();
                int y = (int) location.getY();
                int width = (int) location.getWidth() - x;
                int height = (int) location.getHeight() - y;

                boxes.add(new Box(x, y, width, height));
            }
        }
        return boxes;
    }

}