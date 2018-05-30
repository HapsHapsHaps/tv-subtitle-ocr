package dk.hapshapshaps.machinelearning.objectdetection.models;

public class Box {
    public final int x;
    public final int y;
    public final int width;
    public final int height;

    public Box(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
}