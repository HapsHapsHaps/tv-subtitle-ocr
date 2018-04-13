package dk.kb.tvsubtitleocr.lib.frameextraction;
import java.awt.image.BufferedImage;

public class VideoFrame {
    private BufferedImage image;
    private String fileName;
    private int startTime;
    private int endTime;

    public VideoFrame(BufferedImage image, int startTime, int endTime) {
        this.image = image;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public VideoFrame(BufferedImage image, String fileName, int startTime, int endTime) {
        this.image= image;
        this.fileName = fileName;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public BufferedImage getFrame() {
        return image;
    }

    public void setFrame(BufferedImage image) {
        this.image = image;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getStartTime() {
        return startTime;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    public int getEndTime() {
        return endTime;
    }

    public void setEndTime(int endTime) {
        this.endTime = endTime;
    }
}
