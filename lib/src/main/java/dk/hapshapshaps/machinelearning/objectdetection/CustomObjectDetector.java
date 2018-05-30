package dk.hapshapshaps.machinelearning.objectdetection;

import org.tensorflow.*;
import dk.hapshapshaps.machinelearning.objectdetection.models.ObjectRecognition;
import dk.hapshapshaps.machinelearning.objectdetection.models.RectFloats;
import dk.hapshapshaps.machinelearning.objectdetection.models.Detection;
import org.tensorflow.types.UInt8;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class CustomObjectDetector implements ObjectDetector {
    private static final String INPUT_NAME = "image_tensor";
    private static final int MAX_RESULTS = 10;

//    private byte[] graphBytes;
    private Graph graph;
    private List<String> labels;

    /**
     * Find object classification in images with a pre-trained graph model, and a pbtxt label file
     * @param graphFile The graph model file to load the pre-trained graph from.
     * @param labelFile The .pbtxt model label file, with the possible object labels.
     * @throws IOException if an I/O error occurs.
     */
    public CustomObjectDetector(File graphFile, File labelFile) throws IOException {
        InputStream graphInputStream = Files.newInputStream(graphFile.toPath());
        List<String> labels = loadLabels(labelFile);
        setup(graphInputStream, labels);
    }

    /**
     * Find object classification in images with a pre-trained graph model, and a ordered list of the possible labels.
     * @param graphFile The graph model stream to load the pre-trained graph from.
     * @param labels an ordered list of with the possible object labels that can be detected.
     * @throws IOException if an I/O error occurs.
     */
    public CustomObjectDetector(InputStream graphFile, List<String> labels) throws IOException {
        setup(graphFile, labels);
    }

    private void setup(InputStream graphFile, List<String> labels) throws IOException {
        byte[] graphBytes = loadGraph(graphFile);
        this.graph = loadGraph(graphBytes);
        this.labels = labels;
    }

    /**
     * Loads the graph from the pre-trained model file.
     * @param graphFile the trained graph/model file data to load from.
     * @return The graph model data.
     * @throws IOException if an I/O error occurs.
     */
    private byte[] loadGraph(InputStream graphFile) throws IOException {

        int baosInitSize = graphFile.available() > 16384 ? graphFile.available() : 16384;
        ByteArrayOutputStream baos = new ByteArrayOutputStream(baosInitSize);
        int numBytesRead;
        byte[] buf = new byte[16384];
        while ((numBytesRead = graphFile.read(buf, 0, buf.length)) != -1) {
            baos.write(buf, 0, numBytesRead);
        }

        return baos.toByteArray();
    }

    /**
     * Read label names from label file.
     * @param labelFile the label file to read
     * @throws IOException
     */
    private List<String> loadLabels(File labelFile) throws IOException {
        List<String> labels = new ArrayList<>(2);
        List<String> fileLines = Files.readAllLines(labelFile.toPath());

        for (String line : fileLines) {
            if(line.contains("name:")) {
                int i = line.indexOf("'");
                String substring = line.substring(i + 1, line.length() - 1);
                labels.add(substring);
            }
        }

        return labels;
    }

    @Override
    public ArrayList<ObjectRecognition> classifyImage(BufferedImage image) {

        int width = image.getWidth();
        int height = image.getHeight();
//        width = 200;
//        height = 200;

        Tensor<UInt8> imageTensor = normalizeImage_UInt8(image, width, height);

        Detection detection = executeGraph(imageTensor);

        ArrayList<ObjectRecognition> objectRecognitions = processDetections(detection, width, height);

        imageTensor.close();

        return objectRecognitions;
    }

    public Tensor<UInt8> normalizeImage_UInt8(BufferedImage image, int width, int height) {
//        int[] imageInts = new int[width * height];
//        byte[] byteValues = new byte[width * height * 3];

//        image.getRGB(0,0, image.getWidth(), image.getHeight(), imageInts, 0, image.getWidth());

//        for (int i = 0; i < imageInts.length; ++i) {
//            byteValues[i * 3 + 2] = (byte) (imageInts[i] & 0xFF);
//            byteValues[i * 3 + 1] = (byte) ((imageInts[i] >> 8) & 0xFF);
//            byteValues[i * 3 + 0] = (byte) ((imageInts[i] >> 16) & 0xFF);
//        }

//        if (image.getType() != BufferedImage.TYPE_3BYTE_BGR) {
//            throw new RuntimeException("Expected 3-byte BGR encoding in BufferedImage, found " + image.getType());
//        }

        byte[] data = ((DataBufferByte) image.getData().getDataBuffer()).getData();
        bgr2rgb(data);

        final long BATCH_SIZE = 1;
        final long CHANNELS = 3;
        long[] shape = new long[] {BATCH_SIZE, image.getHeight(), image.getWidth(), CHANNELS};
        return Tensor.create(UInt8.class, shape, ByteBuffer.wrap(data));
    }

    /**
     * Converts image pixels from the type BGR to RGB
     * @param data
     */
    private static void bgr2rgb(byte[] data) {
        for (int i = 0; i < data.length; i += 3) {
            byte tmp = data[i];
            data[i] = data[i + 2];
            data[i + 2] = tmp;
        }
    }

    /**
     * Executes graph on the given preprocessed image
     * @param image preprocessed image
     * @return output tensor returned by tensorFlow
     */
    private Detection executeGraph(final Tensor<?> image) {

        try(CustomGraphProcessor classifier = new CustomGraphProcessor(this.graph)) {
            classifier.feed(INPUT_NAME, image);
            classifier.run();

//            float[] num_detections = classifier.get_num_detections();
//            float[] detection_boxes = classifier.get_detection_boxes();
//            float[] detection_scores = classifier.get_detection_scores();
//            float[] detection_classes = classifier.get_detection_classes();
//
//            ClassifyRecognition detection = new ClassifyRecognition(num_detections, detection_boxes, detection_scores, detection_classes);
//
//            System.out.println(num_detections);

            return classifier.detections();
        }
    }

    private Graph loadGraph(byte[] graphBytes) {
        Graph graph = new Graph();
        graph.importGraphDef(graphBytes);
        return graph;
    }

    private ArrayList<ObjectRecognition> processDetections(Detection detection, int width, int height) {
        // Find the best detections.
        final PriorityQueue<ObjectRecognition> priorityQueue =
                new PriorityQueue<ObjectRecognition>(1, new RecognitionComparator());

        float[] detection_boxes = detection.getDetection_boxes();
        float[] detection_scores = detection.getDetection_scores();
        float[] detection_classes = detection.getDetection_classes();

        // Scale them back to the input size.
        for (int i = 0; i < detection_scores.length; ++i) {
            final RectFloats rectDetection =
                    new RectFloats(
                            detection_boxes[4 * i + 1] * width,
                            detection_boxes[4 * i] * height,
                            detection_boxes[4 * i + 3] * width,
                            detection_boxes[4 * i + 2] * height);
            priorityQueue.add(
                    new ObjectRecognition("" + i, labels.get(( (int) detection_classes[i] ) - 1), detection_scores[i], rectDetection));
        }

        final ArrayList<ObjectRecognition> objectRecognitions = new ArrayList<>();
        for (int i = 0; i < Math.min(priorityQueue.size(), MAX_RESULTS); ++i) {
            objectRecognitions.add(priorityQueue.poll());
        }

        return objectRecognitions;
    }

    @Override
    public void close() throws Exception {
        this.graph.close();
    }

    /**
     * Used to make sure the detections with highest confidence, is placed highest in queue.
     */
    class RecognitionComparator implements Comparator<ObjectRecognition> {
        @Override
        public int compare(final ObjectRecognition objectRecognitionA, final ObjectRecognition objectRecognitionB) {
            return Float.compare(objectRecognitionB.getConfidence(), objectRecognitionA.getConfidence());
        }
    }
}
