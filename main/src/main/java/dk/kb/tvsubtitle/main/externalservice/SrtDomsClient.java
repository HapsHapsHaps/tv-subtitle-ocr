package dk.kb.tvsubtitle.main.externalservice;

import dk.statsbiblioteket.doms.central.connectors.BackendInvalidCredsException;
import dk.statsbiblioteket.doms.central.connectors.BackendInvalidResourceException;
import dk.statsbiblioteket.doms.central.connectors.BackendMethodFailedException;
import dk.statsbiblioteket.util.Strings;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.UUID;

public class SrtDomsClient extends DomsClient {
    private final static String CONTENT_TYPE = "SRT";
    private final static String PRE_OBJECT_IDENTIFIER = "uuid:";
    private final String contentType = CONTENT_TYPE;
    private final String preObjectIdentifier = PRE_OBJECT_IDENTIFIER;

    public SrtDomsClient(String serverAddress, String username, String password) throws MalformedURLException {
        super(serverAddress, username, password);
    }

    /**
     * Gets the srt content currently saved in Doms for the given videoUuid.
     * @param vidUuid the UUID of the video.
     * @return The content as a String if it exists. Null if not.
     * @throws BackendMethodFailedException If the Doms server itself failed.
     */
    public String getSRTContent(UUID vidUuid) throws BackendMethodFailedException {
        try {
            return super.getContent(preObjectIdentifier + vidUuid.toString(), contentType, null);
        } catch (BackendInvalidCredsException e) {
            throw new RuntimeException("Wrong credentials for doms server. Username: " + this.username + " password: " + this.password, e);
        }
    }

    /**
     * Updates the Doms document for the Video UUID, with the given srtFile content.
     * @param videoUuid The video UUID to update SRT data for
     * @param srtFile The SRT file to add to the video document
     * @throws BackendInvalidResourceException If no document with the given Video UUID can be found
     * @throws BackendMethodFailedException If the Doms server itself failed.
     * @throws IOException If there's an issue reading the srtFile.
     */
    public void addContent(UUID videoUuid, File srtFile)
            throws BackendInvalidResourceException, BackendMethodFailedException, IOException {

        // Read SRT content from file.
        InputStream resourceAsStream = FileUtils.openInputStream(srtFile);
        InputStreamReader inputStreamReader = new InputStreamReader(resourceAsStream);
        String srtContents = Strings.flush(inputStreamReader); // Returns file content as String and closes Stream.

        String content = SrtToXML(srtContents); // Makes it look like an XML document, so it can be added to doms.
        String actionMessage = "Added SRT file for video " + videoUuid.toString();

        // Add content to document.
        try {
            super.addContent(preObjectIdentifier + videoUuid.toString(), contentType, content, actionMessage);
        } catch (BackendInvalidCredsException e) {
            throw new RuntimeException("Wrong credentials for doms server. Username: " + this.username + " password: " + this.password, e);
        }
    }

    public boolean exist(UUID videoUuid) throws BackendMethodFailedException {
        return getSRTContent(videoUuid) != null;
    }

    /**
     * Updates the SRT content to act as an XML node, so it can be added to a Doms xml document.
     * @param srtContent The srt content.
     * @return The srt content packed in as a xml node.
     */
    public String SrtToXML(String srtContent) {
        // This is kind off a hack. But it works..
        String escaped = srtContent.replaceAll("]]>", "]]>]]><![CDATA[");
        String xml = "<srt><![CDATA[" + escaped + "]]></srt>";
        return xml;
    }
}
