package dk.kb.tvsubtitleocr.extractor;
import dk.kb.tvsubtitleocr.lib.common.PropertiesFactory;
import dk.kb.tvsubtitleocr.lib.common.RuntimeProperties;
import dk.kb.tvsubtitleocr.extractor.externalservice.VideoIndexClient;
import dk.kb.tvsubtitleocr.extractor.model.VideoInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestVideoIndexClient {
    final static Logger log = LoggerFactory.getLogger(TestVideoIndexClient.class);
    private VideoIndexClient indexAdapter;

    @BeforeEach
    void setUp() {
        RuntimeProperties properties = PropertiesFactory.getProperties();
        String serverAddress = properties.getProperty(RuntimeProperties.ResourceName.indexServerUrl);
        this.indexAdapter = new VideoIndexClient(serverAddress);
    }

    @AfterEach
    void tearDown() {
        indexAdapter.close();
    }

    @Test
    void can_get_video_without_srt_from_index() {
        List<VideoInfo> videos = indexAdapter.getVideosWithoutSRT(0, 1);
        VideoInfo video = videos.get(0);

        // Assert
        assertNotNull(video);
    }

    @Test
    void can_get_video_info_from_index() {
        List<VideoInfo> videos = indexAdapter.getVideos(indexAdapter.getBaseQuery(), 0, 1);
        VideoInfo video = videos.get(0);

        // Assert
        assertNotNull(video);
    }

    @Test
    void can_get_a_list_of_video_info_from_index() {
        int rowCount = 10;
        List<VideoInfo> videos = indexAdapter.getVideos(indexAdapter.getBaseQuery(), 0, rowCount);

        // Assert
        assertTrue(videos.size() == rowCount);
    }

    @Test
    void getVideo_Wont_go_over_1000_rows() {
        int rowCount = 1001;
        List<VideoInfo> videos = indexAdapter.getVideos(indexAdapter.getBaseQuery(), 0, rowCount);

        // Assert
        assertNull(videos);
    }

    @Test
    @Disabled("For manual debugging only.")
    void printOutIndexVideoInfo(){

        List<VideoInfo> videos = indexAdapter.getVideos(indexAdapter.getBaseQuery(),0, 500);

        for (VideoInfo video : videos) {
            log.info(video.toString());
        }
    }
}
