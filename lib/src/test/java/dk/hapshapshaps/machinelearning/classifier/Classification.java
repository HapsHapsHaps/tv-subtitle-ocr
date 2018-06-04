package dk.hapshapshaps.machinelearning.classifier;

import dk.hapshapshaps.machinelearning.classifier.models.ClassifyRecognition;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.modelmapper.internal.util.Assert;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class Classification {

    private File graph = Paths.get("/home/andreas/Projects/machine-learning/Classification/output_graph.pb").toFile();
    private File label = Paths.get("/home/andreas/Projects/machine-learning/Classification/output_labels.txt").toFile();

    @Disabled
    @Test
    public void canCreateClassifier() throws IOException {
        //CustomClassifier classifier = new CustomClassifier(graph, label);
        Classifier classifier = new CustomClassifier(graph, label);
    }

    @Disabled
    @Test
    public void canClassify() throws IOException {
        //Arrange
        Classifier classifier = new CustomClassifier(graph, label);
        BufferedImage image = ImageIO.read(Paths.get("/home/andreas/Pictures/frameExtraction/00019.png").toFile());

        // Act
        ClassifyRecognition classifyRecognition = classifier.classifyImage(image);

        // Assert
        Assert.isTrue(classifyRecognition.getLabel().toLowerCase().equals("sub"));

    }
}
