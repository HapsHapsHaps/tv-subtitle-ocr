package dk.kb.tvsubtitleocr.extractor.model;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class VideoInfo {
    private UUID uuid;
    private String title;
    private ZonedDateTime startTime;

    public VideoInfo(UUID uuid, String title, ZonedDateTime startTime) {
        this.uuid = uuid;
        this.title = title;
        this.startTime = startTime;
    }

    public VideoInfo(String uuid, String title, String startTime) {

        String uuidSubString = uuid.substring(uuid.lastIndexOf(":") + 1);
        this.uuid = UUID.fromString(uuidSubString);
        this.title = title;
        this.startTime = parseDateTime(startTime);
    }

    protected ZonedDateTime parseDateTime(String dateTime) {
        final String format = "yyyy-MM-dd'T'HH:mm:ssZ";

        ZonedDateTime result = ZonedDateTime.parse(dateTime, DateTimeFormatter.ofPattern(format));
        return result;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getTitle() {
        return title;
    }

    public ZonedDateTime getStartTime() {
        return startTime;
    }

    @Override
    public String toString() {
        return "Info: {" +
                "uuid='" + uuid + '\'' +
                ", title='" + title + '\'' +
                ", startTime='" + startTime + '\'' +
                '}';
    }
}
