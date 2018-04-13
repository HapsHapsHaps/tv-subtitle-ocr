package dk.kb.tvsubtitleocr.lib.common;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * A class designed to Load RunTime properties based on config.properties.
 * @author  Silas Jeppe Christensen, sjch@kb.dk (Intern)
 * @author  Jacob Pedersen, jape@kb.dk (Intern)
 * @author  Andreas Reng Mogensen, armo@kb.dk (Intern)
 * @since   2018-02-08
 */
public class PropertiesFactory {

    private final static Logger log = LoggerFactory.getLogger(PropertiesFactory.class);
    private final static String fileName = "config.properties";

    public static RuntimeProperties getProperties() {
        Map<RuntimeProperties.ResourceName, String> resultList = new Hashtable<>();

        Properties prop = new Properties();
        try(InputStream propFile = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName)) {
            if (propFile == null) {
                throw new RuntimeException(fileName + " not found in classpath.");
            }

            prop.load(new InputStreamReader(propFile));
        } catch (IOException e) {
            throw new RuntimeException("Couldn't load properties from: " + fileName, e);
        }

        // Loops through all the defined enums and adds them to the result.
        for (RuntimeProperties.ResourceName e : RuntimeProperties.ResourceName.values()) {
            if (!resultList.containsKey(e)) {
                String propertyValue = prop.getProperty(e.toString());
                if (propertyValue != null && !propertyValue.isEmpty()) {
                    resultList.put(e, prop.getProperty(e.toString()));
                } else {
                    if( ! e.equals(RuntimeProperties.ResourceName.tesseractDataFolderPath)) {
                        log.info(e.toString() + " is not defined");
                    }
                }
            }
        }

        RuntimeProperties runtimeProperties = new RuntimeProperties(resultList);

        verifyProperties(runtimeProperties);

        return runtimeProperties;
    }

    protected static void verifyProperties(RuntimeProperties properties) {
        verifyWorkDirProperty(properties);
        verifyFrameExtractionProperties(properties);
        verifyTesseractProperties(properties);
        verifyGeneralProperties(properties);
        verifyIndexProperties(properties);
        verifyDomsProperties(properties);
    }

    protected static void verifyDomsProperties(RuntimeProperties properties) {
        String domsServerAddress = properties.getProperty(RuntimeProperties.ResourceName.domsServerAddress);
        String domsUserName = properties.getProperty(RuntimeProperties.ResourceName.domsUserName);
        String domsPassword = properties.getProperty(RuntimeProperties.ResourceName.domsPassword);

        if(domsServerAddress == null || domsServerAddress.isEmpty()) {
            throw new RuntimeException(RuntimeProperties.ResourceName.domsServerAddress.toString() + " isn't set in config file.");
        }
        if(domsUserName == null || domsUserName.isEmpty()) {
            throw new RuntimeException(RuntimeProperties.ResourceName.domsUserName.toString() + " isn't set in config file.");
        }
        if(domsPassword == null || domsPassword.isEmpty()) {
            throw new RuntimeException(RuntimeProperties.ResourceName.domsPassword.toString() + " isn't set in config file.");
        }
    }

    protected static void verifyIndexProperties(RuntimeProperties properties) {
        String indexServerUrl = properties.getProperty(RuntimeProperties.ResourceName.indexServerUrl);
        if(indexServerUrl == null || indexServerUrl.isEmpty()) {
            throw new RuntimeException(RuntimeProperties.ResourceName.indexServerUrl.toString() + " isn't set in config file.");
        }
    }

    protected static void verifyGeneralProperties(RuntimeProperties properties) {

        // videoSourceDir
        Path videoSourceDir = Utility.stringAsNullOrPath(properties.getProperty(RuntimeProperties.ResourceName.videoSourceDir));
        if(videoSourceDir == null) {
            String videoSourceDirString = RuntimeProperties.ResourceName.videoSourceDir.toString();
            throw new RuntimeException(videoSourceDirString + " is not defined in properties config.");
        }
        if(Files.notExists(videoSourceDir)) {
            throw new RuntimeException("The given videoSourceDir doesn't exists: " + videoSourceDir);
        }
        if( ! Files.isReadable(videoSourceDir)) {
            throw new RuntimeException("The given videoSourceDir isn't readable: " + videoSourceDir);
        }
        if( ! Files.isDirectory(videoSourceDir)) {
            throw new RuntimeException("The given videoSourceDir isn't a directory: " + videoSourceDir);
        }

        // workerThreads
        String workerThreads = properties.getProperty(RuntimeProperties.ResourceName.workerThreads);
        if (workerThreads == null) {
            throw new RuntimeException("workerThreads is not set.");
        }
        try {
            Integer.parseInt(workerThreads);
        }
        catch (NumberFormatException e) {
            throw new RuntimeException("Couldn't parse workerThreads property value to int.", e);
        }
    }

    protected static void verifyWorkDirProperty(RuntimeProperties properties) {
        // Verify workDir.
        Path workDir = Utility.stringAsNullOrPath(properties.getProperty(RuntimeProperties.ResourceName.sharedWorkDir));
        if(workDir == null) {
            throw new RuntimeException("workDir is not defined in properties config.");
        }
        if(Files.notExists(workDir)) {
            throw new RuntimeException("The given workDir doesn't exists: " + workDir);
        }
        if( ! Files.isWritable(workDir)) {
            throw new RuntimeException("The given workDir isn't writable: " + workDir);
        }
        if( ! Files.isDirectory(workDir)) {
            throw new RuntimeException("The given workDir isn't a directory: " + workDir);
        }

        for (File file : FileUtils.listFilesAndDirs(workDir.toFile(), FileFileFilter.FILE, TrueFileFilter.TRUE)) {
            if( ! Files.isWritable(file.toPath())) {
                throw new RuntimeException("Not all files in workDir is writable. File: " + file.getAbsolutePath());
            }
        }
    }

    protected static void verifyFrameExtractionProperties(RuntimeProperties properties) {
        Path ffmpegPath = Utility.stringAsNullOrPath(properties.getProperty(RuntimeProperties.ResourceName.ffmpegPath));
        Path ffprobePath = Utility.stringAsNullOrPath(properties.getProperty(RuntimeProperties.ResourceName.ffprobePath));

        if(ffmpegPath == null) {
            throw new RuntimeException(RuntimeProperties.ResourceName.ffmpegPath.toString() + " is not set.");
        }
        if(ffprobePath == null) {
            throw new RuntimeException(RuntimeProperties.ResourceName.ffprobePath.toString() + " is not set.");
        }
        if(Files.notExists(ffmpegPath)) {
            throw new RuntimeException(RuntimeProperties.ResourceName.ffprobePath.toString() + " doesn't exist.");
        }
        if(Files.notExists(ffprobePath)) {
            throw new RuntimeException(RuntimeProperties.ResourceName.ffprobePath.toString() + " doesn't exist.");
        }

    }

    protected static void verifyTesseractProperties(RuntimeProperties properties) {
        String pageSegmentationString = properties.getProperty(RuntimeProperties.ResourceName.tesseractPageSegmentation);
        try {
            Integer.parseInt(pageSegmentationString);
        }
        catch (NumberFormatException e) {
            throw new RuntimeException("Couldn't parse pageSegmentation property value to int.", e);
        }

        String engineModeString = properties.getProperty(RuntimeProperties.ResourceName.tesseractOCREngineMode);
        try {
            Integer.parseInt(engineModeString);
        }
        catch (NumberFormatException e) {
            throw new RuntimeException("Couldn't parse engineMode property value to int.", e);
        }

        Path tesseractDataDir = Utility.stringAsNullOrPath(properties.getProperty(
                RuntimeProperties.ResourceName.tesseractDataFolderPath));
        String trainedDataLanguageFile = properties.getProperty(
                RuntimeProperties.ResourceName.tesseractTrainedDataLanguage) + ".traineddata";

        if(tesseractDataDir != null) {
            if (!Files.exists(tesseractDataDir)) {
                throw new RuntimeException("The defined TesseractDataDir doesn't exist. Path: " + tesseractDataDir);
            } else if (!Files.isDirectory(tesseractDataDir)) {
                throw new RuntimeException("The defined TesseractDataDir isn't a directory. Path: " + tesseractDataDir);
            }
            if(Files.notExists(Paths.get(tesseractDataDir.toString(), trainedDataLanguageFile))) {
                throw new RuntimeException("There's no trained data file with the name: "
                        + trainedDataLanguageFile
                        + " in the directory " + tesseractDataDir);
            }
        }
    }
}
