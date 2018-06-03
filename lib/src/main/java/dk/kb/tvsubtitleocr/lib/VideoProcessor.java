package dk.kb.tvsubtitleocr.lib;

import dk.kb.tvsubtitleocr.lib.common.FileAndPathUtilities;
import dk.kb.tvsubtitleocr.lib.common.RuntimeProperties;
import dk.kb.tvsubtitleocr.lib.common.Timer;
import dk.kb.tvsubtitleocr.lib.common.Utility;
import dk.kb.tvsubtitleocr.lib.frameextraction.FrameExtractionProcessor;
import dk.kb.tvsubtitleocr.lib.frameextraction.IFrameExtractionProcessor;
import dk.kb.tvsubtitleocr.lib.frameextraction.VideoFrame;
import dk.kb.tvsubtitleocr.lib.frameextraction.VideoInformation;
import dk.kb.tvsubtitleocr.lib.ocrprocessing.IOCRProcessor;
import dk.kb.tvsubtitleocr.lib.ocrprocessing.OCRProcessorFactory;
import dk.kb.tvsubtitleocr.lib.postprocessing.FrameSubtitle;
import dk.kb.tvsubtitleocr.lib.postprocessing.GenerateSRT;
import dk.kb.tvsubtitleocr.lib.postprocessing.TextProcessor;
import dk.kb.tvsubtitleocr.lib.preprocessing.FramePreProcessor;
import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

public class VideoProcessor {
    final static Logger log = LoggerFactory.getLogger(VideoProcessor.class);
    private final static String ocrWorkDirName = "ocrDir";
    private final static String frameExtractionWorkDirName = "frameExtraction";
    private final GenerateSRT srtProcessor;
    // workerThreads is the total amount of workerThreads it can dedicate to working on high usage work like doing OCR on an image.
    private int workerThreads = 1;
    private final IFrameExtractionProcessor frameExtractionProcessor;
    private final FramePreProcessor preProcessor;
    private final OCRProcessorFactory ocrProcessorFactory;
    private final TextProcessor textProcessor;
    private final RuntimeProperties properties;
    private final File workDir;
    private final boolean debug;

    public VideoProcessor(RuntimeProperties properties) throws IOException {
        this(
                properties,
                Paths.get(properties.getProperty(RuntimeProperties.ResourceName.sharedWorkDir)).toFile(),
                Paths.get(properties.getProperty(RuntimeProperties.ResourceName.objectModelPath)).toFile(),
                Paths.get(properties.getProperty(RuntimeProperties.ResourceName.objectLabelPath)).toFile(),
                Paths.get(properties.getProperty(RuntimeProperties.ResourceName.classifyModelPath)).toFile(),
                Paths.get(properties.getProperty(RuntimeProperties.ResourceName.classifyLabelPath)).toFile());
    }

    public VideoProcessor(RuntimeProperties properties, File workDir, File modelfile, File labelfile, File classifyModelfile, File classifyLabelfile) throws IOException {
        this.properties = properties;
        this.workDir = workDir;
        this.debug = properties.getDebug();
        handleConfiguration(properties);
        frameExtractionProcessor = createFrameExtractionProcessor();
        srtProcessor = new GenerateSRT();
        preProcessor = new FramePreProcessor(workerThreads, modelfile, labelfile, classifyModelfile, classifyLabelfile);
        ocrProcessorFactory = createOcrProcessorFactory();
        textProcessor = new TextProcessor();
    }

    private void handleConfiguration(RuntimeProperties configuration) throws RuntimeException {
        String workDir = configuration.getProperty(RuntimeProperties.ResourceName.sharedWorkDir);
        String modelPath = configuration.getProperty(RuntimeProperties.ResourceName.objectModelPath);
        String labelPath = configuration.getProperty(RuntimeProperties.ResourceName.objectLabelPath);

        if (workDir == null) {
            throw new RuntimeException("sharedWorkDir not defined in properties file");
        }

        Path workDirPath = Paths.get(workDir);
        if (Files.exists(workDirPath) && !Files.isDirectory(workDirPath)) {
            throw new RuntimeException("The sharedWorkDir isn't a directory.");
        } else if (Files.exists(workDirPath) && Files.isDirectory(workDirPath) && !Files.isWritable(workDirPath)) {
            throw new RuntimeException("Doesn't have write access to the sharedWorkDir.");
        }

        this.workerThreads = Integer.parseInt(configuration.getProperty(RuntimeProperties.ResourceName.workerThreads));
    }

    public void processVideo(File videoFile, Path srtOutputFile) throws IOException {

        try(Timer timer = new Timer("Processing video", log)) { // Starts a timer.

            Instant extractFramesStart = Instant.now();
            // Gets frame as images from video.
            log.info("Extracting frames for video file: {}", videoFile.getAbsolutePath());
            if (!videoFile.exists()){
                throw new IOException("File not found: " + videoFile.getAbsolutePath());
            }
            VideoInformation videoInformation = frameExtractionProcessor.extractFrames(videoFile);
            Instant extractFramesStop = Instant.now();

            // preprocess here!
            Instant classificationStart = Instant.now();
            log.info("Classification: Sorting frames with subtitles");
//            List<VideoFrame> classifiedFrames = preProcessor.classify(videoInformation.getFrames());
            Instant classificationStop = Instant.now();

            Instant objectDetectionStart = Instant.now();
            log.info("Object Detection: Finding subtitles");
//            List<VideoFrame> subtitleFrames = preProcessor.detectSubtitles(classifiedFrames);
            List<VideoFrame> subtitleFrames = preProcessor.detectSubtitles(videoInformation.getFrames());
            Instant objectDetectionStop = Instant.now();

            System.gc(); // Suggest to the Garbage collector to consider removing all the previously cached image content

            Instant ocrStart = Instant.now();
            // Get the text from the images by Pre-processing and running them trough Tesseract.
            log.info("Processing frames for video Uuid: {}", videoInformation.getUuid());
            LinkedMap<VideoFrame, List<String>> ocrResults = ocr(subtitleFrames);
            //similarFramesLists = null;
            Instant ocrStop = Instant.now();

            log.info("Processing times:\n " +
                            "ExtractFrames: {} seconds.\n" +
                            "Classify images: {} seconds.\n" +
                            "Object Detect: {} seconds\n" +
                            "Tesseract: {} seconds\n",
                    (Duration.between(extractFramesStart, extractFramesStop)).getSeconds(),
                    (Duration.between(classificationStart, classificationStop)).getSeconds(),
                    (Duration.between(objectDetectionStart, objectDetectionStop).getSeconds()),
                    (Duration.between(ocrStart, ocrStop).getSeconds()));

            // Process TesseractResults.
            ocrResults = postProcessText(ocrResults);

            generateSRT(ocrResults, srtOutputFile);

        } // Stops the timer and logs result.
    }



    private void generateSRT(LinkedMap<VideoFrame, List<String>> ocrResults, Path srtOutputFile) throws IOException {
        List<FrameSubtitle> subs = new LinkedList<>();

        // ocrResults is a LinkedMap, and as such is already sorted.
        ocrResults.forEach((k, v) -> subs.add(new FrameSubtitle(k.getStartTime(), k.getEndTime(), v)));
        srtProcessor.createSRT(srtOutputFile, subs);
    }

    private LinkedMap<VideoFrame, List<String>> ocr(List<VideoFrame> videoFrames) {
        LinkedMap<VideoFrame, Future<List<String>>> futureResult = new LinkedMap<>();

        ExecutorService executorService = Executors.newFixedThreadPool(workerThreads);

        for (VideoFrame videoFrame: videoFrames) {
            // Pre-process image.
            //VideoFrame videoFrame = preProcessor.mergeAndClipoutFrame(frameSubList);

            // OCRProcessor must implement the interface Callable<> to be handled as a future.
            Callable<List<String>> ocrProcessor = ocrProcessorAsCallable(videoFrame.getFrame(), videoFrame);

            // Submit image to be ocr processed when a thread is available.
            Future<List<String>> future = executorService.submit(ocrProcessor);

            futureResult.put(videoFrame, future);
            log.debug("queue size: {}", futureResult.size());
        }

        LinkedMap<VideoFrame, List<String>> result = new LinkedMap<>();

        // Wait for all ocrProcessor instances to be done, and get the results.
        log.info("Started populating frames");
        futureResult.forEach((k, v) -> {
            try {
                List<String> value = v.get();
                if (!value.isEmpty()) {
                    result.put(k, v.get()); // .get is where it awaits the future to be finished.
                }
            } catch (InterruptedException e) {
                log.error("A ocrProcessor thread was interrupted before it could finish for frame {} at {} ",
                        k.getFileName(),
                        k.getStartTime(),
                        e);
            } catch (ExecutionException e) { //Exceptions thrown by the Callable will be here...
                log.warn("A ocrProcessor thread threw an exception when getting its result for frame {} at {} ",
                        k.getFileName(),
                        k.getStartTime(),
                        e);
            }
        });

        // Makes sure that the executor doesn't take any more tasks and is stopped when done.
        executorService.shutdown();

        return result;
    }

    /**
     * Gets the ocrProcessor as Callable, so it'll work with futures and the ExecutorService.
     *
     * @param image the image to process
     * @param frame the VideoFrame that holds information about this frame that'll be processed.
     * @return The IOCRProcessor as callable for the ocrImage method.
     */
    private Callable<List<String>> ocrProcessorAsCallable(BufferedImage image, VideoFrame frame) {
        return new Callable<List<String>>()  {
            IOCRProcessor processor = ocrProcessorFactory.createOCRProcessor();
            BufferedImage internalImage = image;
            VideoFrame internalFrame = frame;
            @Override
            public List<String> call() throws IOException {
                    Thread.currentThread().setName(internalFrame.getFileName() + " - Time: " + internalFrame.getStartTime());
                    return processor.ocrImage(internalImage);
            }
        };
    }

    protected LinkedMap<VideoFrame, List<String>> postProcessText(LinkedMap<VideoFrame, List<String>> ocrResults) {

        ocrResults = removeInvalidText(ocrResults);
        return ocrResults;
    }

    protected LinkedMap<VideoFrame, List<String>> removeInvalidText(LinkedMap<VideoFrame, List<String>> text) {
        LinkedMap<VideoFrame, List<String>> newText = new LinkedMap<>();

        text.forEach((k, v) -> {
            List<String> replacement;
            replacement = textProcessor.removeInvalidChars(v);
            replacement = textProcessor.removeInvalidText(replacement);
            if (replacement != null) {
                newText.put(k, replacement);
            }
        });

        return newText;
    }

    public OCRProcessorFactory createOcrProcessorFactory() {
        OCRProcessorFactory result;
        File ocrWorkDir = FileAndPathUtilities.createSubWorkDir(this.workDir, ocrWorkDirName);

        Path tesseractDataDir = Utility.stringAsNullOrPath(
                properties.getProperty(RuntimeProperties.ResourceName.tesseractDataFolderPath));

        String trainedDataLanguage = properties.getProperty(RuntimeProperties.ResourceName.tesseractTrainedDataLanguage);

        String pageSegmentationString = properties.getProperty(RuntimeProperties.ResourceName.tesseractPageSegmentation);
        int pageSegmentation = Integer.parseInt(pageSegmentationString);

        String engineModeString = properties.getProperty(RuntimeProperties.ResourceName.tesseractOCREngineMode);
        int engineMode = Integer.parseInt(engineModeString);

        if(tesseractDataDir != null) {
            // With data dir specified
            result = new OCRProcessorFactory(
                    ocrWorkDir,
                    pageSegmentation,
                    engineMode,
                    trainedDataLanguage,
                    tesseractDataDir);
        }
        else {
            // Without data dir specified
            result = new OCRProcessorFactory(
                    ocrWorkDir,
                    pageSegmentation,
                    engineMode,
                    trainedDataLanguage);
        }

        return result;
    }

    protected IFrameExtractionProcessor createFrameExtractionProcessor() throws IOException {
        FrameExtractionProcessor result;
        Path workDirPath = Paths.get(this.workDir.getAbsolutePath(), frameExtractionWorkDirName);
        File workDir;
        boolean workDirExists = Files.exists(workDirPath);

        if(debug && ! workDirExists) {
            workDir = Files.createDirectory(workDirPath).toFile();
        }
        else if( ! debug ) {
            if(workDirExists) FileUtils.forceDelete(workDirPath.toFile());
            workDir = Files.createDirectory(workDirPath).toFile();
        }
        else {
            workDir = workDirPath.toFile();
        }

        String ffmpegPath = properties.getProperty(RuntimeProperties.ResourceName.ffmpegPath);
        String ffprobePath = properties.getProperty(RuntimeProperties.ResourceName.ffprobePath);

        try {
            result = new FrameExtractionProcessor(
                    workDir,
                    ffmpegPath,
                    ffprobePath,
                    this.debug
            );
        } catch (IOException e) {
            // The two ff paths is validated in the properties factory..
            // This shouldn't be possible..
            throw new RuntimeException("Error instantiating FrameExtractionProcessor", e);
        }

        return result;
    }
}
