package dk.kb.tvsubtitle.frameextraction;
import java.util.List;

public class VideoInformation {
    private List<VideoFrame> frames;
    private String uuid;
    private int duration;

    public VideoInformation(List<VideoFrame> frames, String uuid, int duration) {
        this.frames = frames;
        this.uuid = uuid;
        this.duration = duration;
    }

    public List<VideoFrame> getFrames() {
        return frames;
    }

    public void setFrames(List<VideoFrame> frames) {
        this.frames = frames;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
}
