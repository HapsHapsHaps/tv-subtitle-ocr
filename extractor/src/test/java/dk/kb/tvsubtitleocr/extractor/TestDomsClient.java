package dk.kb.tvsubtitleocr.extractor;

import dk.kb.tvsubtitle.common.PropertiesFactory;
import dk.kb.tvsubtitle.common.RuntimeProperties;
import dk.kb.tvsubtitleocr.extractor.externalservice.SrtDomsClient;
import dk.statsbiblioteket.doms.central.connectors.BackendMethodFailedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.UUID;

public class TestDomsClient {

    private SrtDomsClient domsClient;

    @BeforeEach
    void setUp() throws IOException {
        RuntimeProperties properties = PropertiesFactory.getProperties();
        String serverAddress = properties.getProperty(RuntimeProperties.ResourceName.domsServerAddress);
        String userName = properties.getProperty(RuntimeProperties.ResourceName.domsUserName);
        String password = properties.getProperty(RuntimeProperties.ResourceName.domsPassword);
        try {
            this.domsClient = new SrtDomsClient(serverAddress, userName, password);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Wrong server address for doms server", e);
        }
    }

    @AfterEach
    void tearDown() throws IOException {

    }

    @Test
    void can_connect_to_doms() throws BackendMethodFailedException {
        // Arrange
        UUID testUUID = UUID.fromString("d11c2f49-4e6f-47bd-b04f-7ee6293520ea");

        // Act
        String srtContent = domsClient.getSRTContent(testUUID);

        // Assert
        // If it gives an exception, then it failed.
    }
}
