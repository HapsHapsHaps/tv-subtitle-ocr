package dk.kb.tvsubtitle.common;
import java.util.Map;

public class RuntimeProperties {

    private Map<ResourceName, String> properties;
    private final Boolean debug;

    public enum ResourceName {
        sharedWorkDir,
        videoSourceDir,
        workerThreads,
        debug,
        ffmpegPath,
        ffprobePath,
        tesseractPath,
        tesseractDataFolderPath,
        tesseractPageSegmentation,
        tesseractOCREngineMode,
        tesseractTrainedDataLanguage,
        indexServerUrl,
        domsServerAddress,
        domsUserName,
        domsPassword
    }

    public RuntimeProperties(Map<ResourceName, String> properties) {
        this.properties = properties;
        String debugValue = getProperty(ResourceName.debug);
        debug = Boolean.parseBoolean(debugValue);
    }

    public String getProperty(ResourceName resourceName) {
        return properties.get(resourceName);
    }

    public Boolean getDebug() {
        return debug;
    }

    /**
     * Gets all the properties/resourceLinks? with name and value.
     * @return The string concatenation with name and value of the properties.
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("ResourceLinks defined as:");
        for ( ResourceName e : ResourceName.values()) {
            result.append("\n").append(e.toString()).append(": ").append(properties.get(e));
        }
        return result.toString();
    }

}
