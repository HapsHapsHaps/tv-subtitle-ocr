package dk.kb.tvsubtitleocr.lib.ocrprocessing;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

public interface IOCRProcessor{
    List<String> ocrImage(BufferedImage image) throws IOException;
}
