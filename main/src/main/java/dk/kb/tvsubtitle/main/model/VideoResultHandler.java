package dk.kb.tvsubtitle.main.model;
import dk.kb.tvsubtitle.main.model.VideoInfo;

import java.io.File;

public interface VideoResultHandler {
    void onSuccess(VideoInfo videoInfo, File srtFile);
    void onError(VideoInfo videoInfo, Exception e);
}
