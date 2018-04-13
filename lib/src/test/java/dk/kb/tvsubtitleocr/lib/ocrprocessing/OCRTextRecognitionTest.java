package dk.kb.tvsubtitleocr.lib.ocrprocessing;
import static org.junit.jupiter.api.Assertions.assertEquals;

import dk.kb.tvsubtitleocr.lib.common.FileAndPathUtilities;
import dk.kb.tvsubtitleocr.lib.common.PropertiesFactory;
import dk.kb.tvsubtitleocr.lib.common.RuntimeProperties;
import dk.kb.tvsubtitleocr.lib.common.Utility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

class OCRTextRecognitionTest {
    final static Logger log = LoggerFactory.getLogger(OCRTextRecognitionTest.class);
    private File workDir;
    private BufferedImage testImage;
    private OCRProcessorFactory ocrFactory;

    @BeforeEach
    void setUp() throws IOException {
        RuntimeProperties properties = PropertiesFactory.getProperties();
        String parentWorkDirString = properties.getProperty(RuntimeProperties.ResourceName.sharedWorkDir);
        File parentWorkDirPath = Paths.get(parentWorkDirString).toFile();
        this.workDir = FileAndPathUtilities.createSubWorkDir(parentWorkDirPath, "ocrTest");

        ocrFactory = createOCRProcessorFactory(properties, this.workDir);

        URL imageUrl = getClass().getClassLoader().getResource("testImage.png");
        File imageFile = new File(imageUrl.getFile());

        testImage = ImageIO.read(imageFile); //GET TEST IMAGE FROM RESOURCES DIR
    }

    protected OCRProcessorFactory createOCRProcessorFactory(RuntimeProperties properties, File workDir) {
        OCRProcessorFactory result;

        Path tesseractDataDir = Utility.stringAsNullOrPath(
                properties.getProperty(RuntimeProperties.ResourceName.tesseractDataFolderPath));

        String trainedDataLanguage = properties.getProperty(RuntimeProperties.ResourceName.tesseractTrainedDataLanguage);

        String pageSegmentationString = properties.getProperty(RuntimeProperties.ResourceName.tesseractPageSegmentation);
        int pageSegmentation = Integer.parseInt(pageSegmentationString);

        String engineModeString = properties.getProperty(RuntimeProperties.ResourceName.tesseractOCREngineMode);
        int engineMode = Integer.parseInt(engineModeString);

        if(tesseractDataDir != null) {
            // With data dir specified
            result = new OCRProcessorFactory(
                    workDir,
                    pageSegmentation,
                    engineMode,
                    trainedDataLanguage,
                    tesseractDataDir);
        }
        else {
            // Without data dir specified
            result = new OCRProcessorFactory(
                    workDir,
                    pageSegmentation,
                    engineMode,
                    trainedDataLanguage);
        }

        return result;
    }

    @AfterEach
    void tearDown() throws IOException {
        FileAndPathUtilities.deleteDirectoryAndContent(this.workDir.toPath());
    }

    @Test
    void can_get_text_from_image() throws Exception {
        // Arrange
        IOCRProcessor processor = ocrFactory.createOCRProcessor();

        // Act
        List<String> result = processor.ocrImage(testImage);

        // Assert
        final String expectedLine01 = "Hej med dig - test punktum . nej";
        assertEquals(expectedLine01, result.get(0));
    }
}
