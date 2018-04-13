package dk.kb.tvsubtitleocr.lib.frameextraction;
import java.io.File;
import java.io.IOException;

public interface IFrameExtractionProcessor {
    VideoInformation extractFrames(File input) throws IOException;
}
