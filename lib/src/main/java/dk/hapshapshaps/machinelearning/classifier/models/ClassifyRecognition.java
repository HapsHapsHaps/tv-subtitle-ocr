package dk.hapshapshaps.machinelearning.classifier.models;

public class ClassifyRecognition {
    /**
     * Identifier for the detected label
     */
    private final int id;

    /**
     * Display name for the recognition.
     */
    private final String label;

    /**
     * A sortable score for how good the recognition is relative to others. Higher should be better.
     */
    private final Float confidence;

    public ClassifyRecognition(int id, String label, Float confidence) {
        this.id = id;
        this.label = label;
        this.confidence = confidence;
    }

    public int getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public Float getConfidence() {
        return confidence;
    }
}
