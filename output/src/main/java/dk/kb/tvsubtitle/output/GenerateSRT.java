package dk.kb.tvsubtitle.output;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.IOUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;


public class GenerateSRT {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Create a SRT formatted File from the given FrameSubtitles.
     * @param resultFile The path to write the srt file to.
     * @param frameSubtitles The subtitle content to process.
     * @return A File object that represents the result SRT-File on disk.
     * @throws IOException
     */
    public File createSRT(Path resultFile, List<FrameSubtitle> frameSubtitles) throws IOException {
        List<String> lines = generateSRTFileContent(frameSubtitles);
        Files.write(resultFile, lines, Charset.forName("UTF-8"), StandardOpenOption.CREATE_NEW);

        return resultFile.toFile();
    }

    /**
     * Generates a list with the input content formatted according to the SRT format.
     * Each line represent a line in a SRT-file.
     * @param frameSubtitles The content to generate the SRT from.
     * @return The SRT formatted result list.
     */
    public List<String> generateSRTFileContent(List<FrameSubtitle> frameSubtitles) {
//        String newLine = System.getProperty("line.separator");
        String newLine = "\n";

        List<String> subtitleStatements = new LinkedList<>();
        int statementNumber = 1;
        for (FrameSubtitle sub : frameSubtitles) {
            StringBuilder subTitleStatement = new StringBuilder();
            subTitleStatement.append("").append(statementNumber).append(newLine);
            subTitleStatement.append(formatTime(sub.getStartTime(), sub.getEndTime())).append(newLine);

            // Ads all the lines for this subtitle to its statement.
            for (String line : sub.getText()) {
                subTitleStatement.append(line).append(newLine);
            }

            subtitleStatements.add(subTitleStatement.toString());
            statementNumber++;
        }
        return subtitleStatements;
    }

    /**
     * Takes startTime and endTime from a videoFrame in milliseconds and runs a dataformat for printing to a .SRT file.
     * @param timeStartMillis
     * @param timeEndMillis
     * @return a formatted "SRT TIME"-string based on two milliseconds ints.
     */
    public String formatTime(int timeStartMillis, int timeEndMillis){

        Date dStart = new Date(timeStartMillis);
        Date dEnd = new Date(timeEndMillis);

        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss,SSS");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));

        String start = df.format(dStart);
        String end = df.format(dEnd);
        String returnFormat = start + " --> " + end;

        return returnFormat;
    }
}
