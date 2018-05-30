package dk.hapshapshaps.machinelearning.objectdetection.models;

public class RectFloats {
    private final float x, y, width, height;

    public RectFloats(RectFloats rectFloats) {
        this.x = rectFloats.getX();
        this.y = rectFloats.getY();
        this.width = rectFloats.getWidth();
        this.height = rectFloats.getHeight();
    }

    public RectFloats(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }
}
