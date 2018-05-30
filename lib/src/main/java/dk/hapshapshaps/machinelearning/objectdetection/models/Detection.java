package dk.hapshapshaps.machinelearning.objectdetection.models;

public class Detection {

    private float[] num_detections;
    private float[] detection_boxes;
    private float[] detection_scores;
    private float[] detection_classes;

    public Detection(float[] num_detections, float[] detection_boxes, float[] detection_scores, float[] detection_classes) {
        this.num_detections = num_detections;
        this.detection_boxes = detection_boxes;
        this.detection_scores = detection_scores;
        this.detection_classes = detection_classes;
    }

    public float[] getNum_detections() {
        return num_detections;
    }

    public float[] getDetection_boxes() {
        return detection_boxes;
    }

    public float[] getDetection_scores() {
        return detection_scores;
    }

    public float[] getDetection_classes() {
        return detection_classes;
    }
}
