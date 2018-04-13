package dk.kb.tvsubtitleocr.lib.postprocessing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestGenerateSRT {
    private GenerateSRT generateSRT;

    @BeforeEach
    void setUp() {
        this.generateSRT = new GenerateSRT();
    }

    @Test
    public void generateSRTFileContent_generates_correct_format() {
        // Arrange
        List<FrameSubtitle> testData = new ArrayList<>();
        testData.add(new FrameSubtitle(100, 200, Collections.singletonList("first part")));
        testData.add(new FrameSubtitle(200, 300, Collections.singletonList("second part")));

        // Act
        List<String> srtFileContent = generateSRT.generateSRTFileContent(testData);

        // Assert
        String expectedFirst = "1\n00:00:00,100 --> 00:00:00,200\nfirst part\n";
        String expectedSecond = "2\n00:00:00,200 --> 00:00:00,300\nsecond part\n";

        String actualFirst = srtFileContent.get(0);
        String actualSecond = srtFileContent.get(1);

        assertEquals(expectedFirst, actualFirst);
        assertEquals(expectedSecond, actualSecond);
    }

    @Test
    public void formatTime_generates_correct_format() {
        // Arrange
        int startTime = 100;
        int endTime = 200;

        // Act
        String formattedTime = generateSRT.formatTime(startTime, endTime);

        // Assert
        String expected = "00:00:00,100 --> 00:00:00,200";

        assertEquals(expected, formattedTime);
    }
}
