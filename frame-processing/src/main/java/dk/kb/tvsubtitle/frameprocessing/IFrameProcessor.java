package dk.kb.tvsubtitle.frameprocessing;
import java.awt.image.BufferedImage;

public interface IFrameProcessor {
    BufferedImage processFrame(BufferedImage image);

//    BufferedImage processFrame(BufferedImage frame, File file);
}
