package dk.kb.tvsubtitleocr.lib.ocrprocessing;
import dk.kb.tvsubtitleocr.lib.common.PropertiesFactory;
import dk.kb.tvsubtitleocr.lib.common.RuntimeProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * This class is not for actual unit testing, but for development with experimentation and debugging.
 * So instead of spawning a billion main args all over the place. You can experiment with this as your starting point.
 */
public class OCRProcessingTest {
    final static Logger log = LoggerFactory.getLogger(OCRProcessingTest.class);
    private RuntimeProperties properties;

    @BeforeEach
    void setUp() throws IOException {
        this.properties = PropertiesFactory.getProperties();
    }

    @SuppressWarnings("Duplicates")
    @Test
    @Disabled("For development experimentation and debugging only")
    public void Main() {

        Instant timeStart;
        Instant timeStop;

        timeStart = Instant.now();
        System.out.println("Starting");

        Test1();
//        Test2();
//        getTextFromImage();

        System.out.println("The End");
        timeStop = Instant.now();

        log.info("Processing time: " +
                (Duration.between(timeStart, timeStop)));
    }

    private void Test1() {

    }

    private void Test2() {

    }

    private static void PrintEnvironmentVariablesToConsole() {
        ProcessBuilder builder = new ProcessBuilder("/bin/sh");
        builder.environment().forEach((k,v) ->System.out.println("Key = " + k + "\nValue = " + v));
        Map<String, String> s = builder.environment();
        System.out.println("LD_LIBRARY_PATH: " + s.get("LD_LIBRARY_PATH"));

    }

    /*private void getTextFromImage() throws IOException, InterruptedException {
        String imageName = "udenBoks.png";
        URL resource = getClass().getClassLoader().getResource(imageName);
        BufferedImage image = ImageIO.read(resource);

        OCRProcessorFactory ocrProcessorFactory = new OCRProcessorFactory(properties);
        IOCRProcessor ocrProcessor = ocrProcessorFactory.createOCRProcessor();
        List<String> strings = ocrProcessor.ocrImage(image);
        String s = "";

    }*/

}
