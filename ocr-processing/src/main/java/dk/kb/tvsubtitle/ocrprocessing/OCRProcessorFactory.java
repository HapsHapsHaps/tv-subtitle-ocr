package dk.kb.tvsubtitle.ocrprocessing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

public class OCRProcessorFactory {
    final static Logger log = LoggerFactory.getLogger(OCRProcessorFactory.class);

    private final String language;
    private final static String resourcesTesseractExecutable = "tesseract";
    private final static String tesseractConfigFileName = "tesseractConfig.txt";
    private final static int tesseractProcessThreadLimit = 1; // Setting this to 1, disables tesseract internal multithreading.
    private final Random random = new Random((long) LocalDateTime.now().getNano());
    private File workDir;
    private Map<String, String> environmentVariables;
    private List<String> defaultArguments;

    public OCRProcessorFactory(File workDir, int tesseractPageSegmentation, int tesseractEngineMode, String trainedDataLanguage){
        this(workDir, tesseractPageSegmentation, tesseractEngineMode, trainedDataLanguage, null);
    }

    public OCRProcessorFactory(File workDir, int tesseractPageSegmentation, int tesseractEngineMode, String trainedDataLanguage, Path tesseractDataDir) {
        ClassLoader classLoader = getClass().getClassLoader();
        this.language = trainedDataLanguage;

        URL configResource = classLoader.getResource(tesseractConfigFileName);
        if(configResource == null) {
            throw new RuntimeException(tesseractConfigFileName + " not found in class Path.");
        }
        File tesseractConfig = new File(configResource.getPath());

        setDefaultEnvironmentVariables(tesseractDataDir);
        setDefaultArguments(tesseractPageSegmentation, tesseractEngineMode, tesseractConfig);
        this.workDir = workDir;
    }

    public IOCRProcessor createOCRProcessor() {
        log.debug("Tesseract 4 worker started processing");
        return new Tesseract4Worker(random, resourcesTesseractExecutable, defaultArguments, environmentVariables, workDir);
    }

    private void setDefaultEnvironmentVariables(Path tesseractDataDir) {
        Map<String, String> result = new HashMap<>();

        if(tesseractDataDir != null) {
            result.put("TESSDATA_PREFIX", tesseractDataDir.toString());
        }

        result.put("OMP_THREAD_LIMIT", String.valueOf(tesseractProcessThreadLimit));

        this.environmentVariables = result;
    }

    private void setDefaultArguments(Integer pageSegmentation, Integer ocrEngineMode, File tesseractConfig) {

        this.defaultArguments = new LinkedList<>();
        this.defaultArguments.add("-l");
        this.defaultArguments.add(language);

        if(pageSegmentation != null) {
            this.defaultArguments.add("--psm");
            this.defaultArguments.add(String.valueOf(pageSegmentation));
        }
        if(ocrEngineMode != null) {
            this.defaultArguments.add("--oem");
            this.defaultArguments.add(String.valueOf(ocrEngineMode));
        }
        if(tesseractConfig != null) {
            this.defaultArguments.add(tesseractConfig.toString()); //Need absolute path.
        }
    }
}
