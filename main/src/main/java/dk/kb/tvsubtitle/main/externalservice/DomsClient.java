package dk.kb.tvsubtitle.main.externalservice;

import dk.statsbiblioteket.doms.central.connectors.BackendInvalidCredsException;
import dk.statsbiblioteket.doms.central.connectors.BackendInvalidResourceException;
import dk.statsbiblioteket.doms.central.connectors.BackendMethodFailedException;
import dk.statsbiblioteket.doms.central.connectors.fedora.Fedora;
import dk.statsbiblioteket.doms.central.connectors.fedora.FedoraRest;
import dk.statsbiblioteket.doms.webservices.authentication.Credentials;

import java.net.MalformedURLException;

public class DomsClient {
    protected String serverAddress, username, password;
    protected Fedora fedoraServer;
    public enum objectState { I, A } // I for inactive (Being updated), A for active (Not being updated).

    DomsClient(String serverAddress, String username, String password) throws MalformedURLException {
        this.username = username;
        this.password = password;
        this.serverAddress = serverAddress;
        Credentials creds = new Credentials(this.username, this.password);
        String serverLocation = this.serverAddress.replaceFirst("/(objects)?/?$", "");
        this.fedoraServer = new FedoraRest(creds, serverLocation);
    }

    /**
     * Gets the matching content for in the defined document from the Doms server.
     * @param objectIdentifier The document identifier to get content from.
     * @param contentTypeId The content identifier for the content to retrieve.
     * @param asOfDateTime Perhaps since a given date?
     * @return If the content and document exists, returns the content as a String. Else Null.
     * @throws BackendInvalidCredsException Wrong server credentials
     * @throws BackendMethodFailedException If the Doms server itself failed.
     */
    public String getContent(
            String objectIdentifier,
            String contentTypeId,
            Long asOfDateTime)
            throws BackendInvalidCredsException, BackendMethodFailedException {

        String result;
        try {
            result = this.fedoraServer.getXMLDatastreamContents(objectIdentifier, contentTypeId, asOfDateTime);
        } catch (BackendInvalidResourceException e) {
            //object or datastream not found
            result = null;
        }
        return result;
    }

    /**
     * Adds the given content to the specified Doms document.
     * @param objectIdentifier The document identifier to add the content to.
     * @param contentTypeId The content identifier to add the content as.
     * @param content The content itself to be added to the document.
     * @param actionMessage A descriptive log message for the action taken.
     * @throws BackendInvalidResourceException If no document with the given identifier can be found.
     * @throws BackendInvalidCredsException Wrong server credentials.
     * @throws BackendMethodFailedException If the Doms server itself failed.
     */
    public void addContent(
            String objectIdentifier,
            String contentTypeId,
            String content,
            String actionMessage)
            throws BackendInvalidResourceException, BackendInvalidCredsException, BackendMethodFailedException {

        // POST set inactive.
        this.fedoraServer.modifyObjectState(
                objectIdentifier,
                objectState.I.toString(),
                programVersion() + " Set to inactive for " + actionMessage
        );

        // POST add/replace content.
        String actionComment = programVersion() + "-" + actionMessage;
        this.fedoraServer.modifyDatastreamByValue(
                objectIdentifier,
                contentTypeId,
                content,
                actionComment
        );

        // POST set active.
        this.fedoraServer.modifyObjectState(
                objectIdentifier,
                objectState.A.toString(),
                programVersion() + " Set to active for " + actionMessage
        );
    }

    /**
     * Gets the current version of the application.
     * @implNote For maven DefaultImplementationEntries and DefaultSpecificationEntries must be set to true in the maven-jar-plugin manifest.
     * @return The current program version.
     */
    public String programVersion() {
        String programVersion = getClass().getPackage().getImplementationVersion();
        if(programVersion == null) {
            // Will only be set when packed and ran as standalone jar. For development this will replace the version.
            programVersion = "TestDebug-" + getClass().getPackage().getName();
        }
        return programVersion;
    }
}
