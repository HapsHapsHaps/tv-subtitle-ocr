package dk.kb.tvsubtitleocr.lib.preprocessing;
import java.awt.image.BufferedImage;

public interface IFrameProcessor {
    BufferedImage processFrame(BufferedImage image);

//    BufferedImage processFrame(BufferedImage frame, File file);
}
