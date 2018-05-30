package dk.hapshapshaps.machinelearning.objectdetection;

import dk.hapshapshaps.machinelearning.objectdetection.models.ObjectRecognition;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

public interface ObjectDetector extends AutoCloseable {
    ArrayList<ObjectRecognition> classifyImage(BufferedImage image);
}
