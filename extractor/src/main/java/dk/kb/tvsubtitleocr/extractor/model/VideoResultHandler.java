package dk.kb.tvsubtitleocr.extractor.model;

import java.io.File;

public interface VideoResultHandler {
    void onSuccess(VideoInfo videoInfo, File srtFile);
    void onError(VideoInfo videoInfo, Exception e);
}
