package dk.hapshapshaps.machinelearning.classifier;

import dk.hapshapshaps.machinelearning.classifier.models.ClassifyRecognition;
import org.tensorflow.Graph;
import org.tensorflow.Output;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

public class CustomClassifier implements AutoCloseable {

    private final Graph graph;
    private final List<String> labels;

    public CustomClassifier(File graphFile, File labelFile) throws IOException {
        byte[] graphBytes = Files.readAllBytes(graphFile.toPath());
        this.graph = loadGraph(graphBytes);
        this.labels = Files.readAllLines(labelFile.toPath());
    }

    public ClassifyRecognition classifyImage(BufferedImage image) {
        Tensor<Float> imageTensor = normalizeImage(image);

        float[] graphResults = executeGraph(imageTensor);

        int labelIndex = bestProbabilityIndex(graphResults);

        String label = labels.get(labelIndex);

        float confidence = graphResults[labelIndex];

        ClassifyRecognition recognition = new ClassifyRecognition(labelIndex, label, confidence);

        imageTensor.close();

        return recognition;
    }

    private Tensor<Float> normalizeImage(BufferedImage image) {
        try(Graph graph = new Graph()) {

            byte[] imageBytes = ((DataBufferByte) image.getData().getDataBuffer()).getData();
            bgr2rgb(imageBytes);

            GraphBuilder builder = new GraphBuilder(graph);

            // - The model was trained with images scaled to 224x224 pixels.
            // - The colors, represented as R, G, B in 1-byte each were converted to
            //   float using (value - Mean)/Scale.
            final int Height = 299;
            final int Width = 299;
            final float mean = 0f;
            final float scale = 255f;

            // Since the graph is being constructed once per execution here, we can use a constant for the
            // input image. If the graph were to be re-used for multiple input images, a placeholder would
            // have been more appropriate.
            final Output<String> input = builder.constant("input", imageBytes);

            final Output<Float> resizedImage = builder.resizeBilinear(
                    builder.expandDims(
                            builder.cast(builder.decodeJpeg(input, 3), Float.class),
                            builder.constant("make_batch", 0)),
                    builder.constant("size", new int[]{Height, Width}));

            final Output<Float> output =
                    builder.div(
                            builder.sub(resizedImage, builder.constant("mean", mean)),
                            builder.constant("scale", scale));
            try (Session session = new Session(graph)) {
                return session.runner().fetch(output.op().name()).run().get(0).expect(Float.class); // casts the Tensor<?> to Tensor<Float>.
            }
        }
    }

    /**
     * Executes graph on the given preprocessed image
     * @param image preprocessed image
     * @return output tensor returned by tensorFlow
     */
    private float[] executeGraph(final Tensor<Float> image) {
        try (Session s = new Session(graph);
             Tensor<Float> resultTensor =
                     s.runner()
                             .feed("Mul", image)
                             .fetch("final_result")
                             .run()
                             .get(0)
                             .expect(Float.class)) {
            final long[] rshape = resultTensor.shape();
            if (resultTensor.numDimensions() != 2 || rshape[0] != 1) {
                throw new RuntimeException(
                        String.format(
                                "Expected model to produce a [1 N] shaped tensor where N is the number of labels, instead it produced one with shape %s",
                                Arrays.toString(rshape)));
            }
            int nlabels = (int) rshape[1];
            float[] resultFloat = resultTensor.copyTo(new float[1][nlabels])[0];

            resultTensor.close();

            return resultFloat;
        }
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

    private Graph loadGraph(byte[] graphBytes) {
        Graph graph = new Graph();
        graph.importGraphDef(graphBytes);
        return graph;
    }

    private static int bestProbabilityIndex(float[] probabilities) {
        int best = 0;
        for (int i = 1; i < probabilities.length; ++i) {
            if (probabilities[i] > probabilities[best]) {
                best = i;
            }
        }
        return best;
    }

    @Override
    public void close() throws Exception {
        this.graph.close();
    }
}
