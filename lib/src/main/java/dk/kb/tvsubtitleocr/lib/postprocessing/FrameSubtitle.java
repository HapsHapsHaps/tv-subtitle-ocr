package dk.kb.tvsubtitleocr.lib.postprocessing;
import java.util.List;

public class FrameSubtitle {
    private int startTime;
    private int endTime;
    List<String> text;

    public FrameSubtitle(int startTime, int endTime, List<String> text) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.text = text;
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

    public List<String> getText() {
        return text;
    }

    public void setText(List<String> text) {
        this.text = text;
    }
}
