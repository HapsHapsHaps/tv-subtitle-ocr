package dk.kb.tvsubtitleocr.lib.preprocessing;

import dk.hapshapshaps.machinelearning.classifier.Classifier;
import dk.hapshapshaps.machinelearning.classifier.CustomClassifier;
import dk.hapshapshaps.machinelearning.classifier.models.ClassifyRecognition;
import dk.hapshapshaps.machinelearning.objectdetection.*;
import dk.hapshapshaps.machinelearning.objectdetection.models.*;
import dk.kb.tvsubtitleocr.lib.common.Named;
import dk.kb.tvsubtitleocr.lib.frameextraction.VideoFrame;
import dk.statsbiblioteket.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class FramePreProcessor {
    final static Logger log = LoggerFactory.getLogger(FramePreProcessor.class);
    private final int workerThreads;
    @Deprecated
    private final FrameProcessorOpenCV frameProcessorOpenCV;
    private final CustomObjectDetector detector;
    private final Classifier classifier;
    private final ExecutorService executorService;


    public FramePreProcessor(int workerThreads, File objectModelfile, File objectLabelfile, File classifyModelfile, File classifyLabelfile) throws IOException {
        this.workerThreads = workerThreads;
        this.executorService = Executors.newFixedThreadPool(workerThreads);
        this.frameProcessorOpenCV = new FrameProcessorOpenCV();
        this.detector = new CustomObjectDetector(objectModelfile, objectLabelfile);
        this.classifier = new CustomClassifier(classifyModelfile, classifyLabelfile);
    }

    public FramePreProcessor(int workerThreads) {
        this.executorService = Executors.newFixedThreadPool(workerThreads);
        this.workerThreads = workerThreads;
        this.frameProcessorOpenCV = new FrameProcessorOpenCV();
        this.detector = null;
        this.classifier = null;
    }

    public BufferedImage clipoutFrame(BufferedImage frame, Box box) {
        return frame.getSubimage(
                box.x,
                box.y,
                box.width,
                box.height
        );
    }

    public List<VideoFrame> detectSubtitles(List<VideoFrame> frames) {
        List<VideoFrame> resultFrames = new LinkedList<>();
        List<Future<VideoFrame>> futureFrames = new LinkedList<>();


        for (VideoFrame frame :
                frames) {
            Callable<VideoFrame> detectSubtitlesProcessor = detectSubtitlesAsCallable(frame);
            Future<VideoFrame> futureFrame = executorService.submit(detectSubtitlesProcessor);
            futureFrames.add(futureFrame);
        }

        futureFrames.forEach(videoFrameFuture -> {
            try {
                resultFrames.add(videoFrameFuture.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });

        resultFrames.removeIf(videoFrame -> videoFrame.getFrame() == null);
        return resultFrames;
    }

    public List<VideoFrame> classify(List<VideoFrame> frames) {
        List<Pair<VideoFrame, Boolean>> resultFrames = new LinkedList<>();
        List<Future<Pair<VideoFrame, Boolean>>> futures = new LinkedList<>();

        frames.forEach(frame -> {
            Callable<Pair<VideoFrame, Boolean>> classifiedSubFrame = classifyAsCallable(frame);
            futures.add(executorService.submit(classifiedSubFrame));
        });

        futures.forEach(pairFuture -> {
            try {
                resultFrames.add(pairFuture.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
        resultFrames.removeIf(videoFrameBooleanPair -> !videoFrameBooleanPair.getRight());
        return resultFrames.stream().map(Pair::getLeft).collect(Collectors.toList());
    }

    private Callable<Pair<VideoFrame, Boolean>> classifyAsCallable(VideoFrame frame){
        return () -> {
            ClassifyRecognition classifyRecognition = classifier.classifyImage(frame.getFrame());
            if(classifyRecognition.getLabel().equals("sub") && classifyRecognition.getConfidence() > 0.5f){
                return new Pair<>(frame, true);
            } else {
                return new Pair<>(frame, false);
            }
        };
    }

    private Callable<VideoFrame> detectSubtitlesAsCallable(VideoFrame frame) {
        return () -> {
            ArrayList<ObjectRecognition> recognitions = detector.classifyImage(frame.getFrame());
            List<Box> boxes = toBoxes(recognitions);
            if (boxes.size() >= 1) {
                //frame.setFrame(drawBoxes(frame.getFrame(), Collections.singletonList(boxes.get(0))));
                BufferedImage oldframe = frame.getFrame();
                try {
                    frame.setFrame(clipoutFrame(frame.getFrame(), boxes.get(0)));
                } catch (RasterFormatException e){
                    e.printStackTrace();
                }
            } else {
                frame.setFrame(null);
            }
            return frame;
        };
    }

    private static BufferedImage drawBoxes(BufferedImage image, List<Box> boxes) {
        Graphics2D graph = image.createGraphics();
        graph.setColor(Color.green);

        for (Box box : boxes) {
            graph.drawRect(box.x, box.y, box.width, box.height);
        }

        graph.dispose();
        return image;
    }

    private static List<Box> toBoxes(List<ObjectRecognition> recognitions) {
        List<Box> boxes = new ArrayList<>();
        for (ObjectRecognition recognition : recognitions) {
            if (recognition.getConfidence() > 0.05f && recognition.getTitle().toLowerCase().equals("sub")) {
                RectFloats location = recognition.getLocation();
                int x = (int) location.getX();
                int y = (int) location.getY();
                int width = (int) location.getWidth() - x;
                int height = (int) location.getHeight() - y;

                boxes.add(new Box(x, y, width, height));
            }
        }
        return boxes;
    }


    @Deprecated
    public VideoFrame mergeAndClipoutFrame(List<VideoFrame> frames) {
        VideoFrame returnFrame;
        if (frames.size() >= 2) {

            List<BufferedImage> images = frames.stream().map(VideoFrame::getFrame).collect(Collectors.toList());
            BufferedImage frame = MergeImages.mergeImages(images);
            frame = frameProcessorOpenCV.processFrame(frame);
            returnFrame = new VideoFrame(
                    frame,
                    frames.get(0).getStartTime(),
                    frames.get(frames.size() - 1).getEndTime());
        } else if (frames.size() == 1) {
            returnFrame = frames.get(0);
        } else {
            returnFrame = null;
        }


        return returnFrame;
    }

    private Callable<VideoFrame> videoFrameCallable(Pair<VideoFrame, VideoFrame> framePair) {
        return () -> new VideoFrame(MergeImages.mergeImages(framePair.getLeft().getFrame(), framePair.getRight().getFrame()),
                framePair.getLeft().getStartTime(), framePair.getRight().getEndTime());
    }

    /**
     * Merges all frames in a two and two manner, with no decision on which, what, or how they're merged. Just merges
     * the first with the next, until the end.
     *
     * @param frames
     * @return
     */
    public List<VideoFrame> mergeFramesInPairs(List<VideoFrame> frames) {
        log.info("Preprocess Pre merging frames {}. ", frames.size());
        ExecutorService executors = Executors.newFixedThreadPool(workerThreads);

        List<Future<VideoFrame>> futures = new ArrayList<>();
        List<VideoFrame> resultFrames = new ArrayList<>();

        for (int i = 1; i < frames.size(); i = i + 2) {
            Pair<VideoFrame, VideoFrame> framePair = new Pair<>(frames.get(i - 1), frames.get(i));

            // frames set to null, so garbage collection can remove them as they are no longer needed.
            frames.set(i - 1, null);
            frames.set(i, null);
            Future<VideoFrame> callable = executors.submit(videoFrameCallable(framePair));
            futures.add(callable);
        }

        for (Future<VideoFrame> future :
                futures) {
            try {
                resultFrames.add(future.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

        }


        return resultFrames;
    }


    /**
     * Takes an ordered list of VideoFrames, finds frames with similar subtitles and merges them together into a new list
     * just containing video frames with Updated Start and end times for that subtitle along with the Frame inside the
     * VideoFrame is just containing the cropped subtitle.
     * <p>
     * Loop should work as follows for all frames in input:
     * <p>
     * first if: Merge two frames together. If Frame 1 and Merged Frame have similar or better RectangularData (isNiceMerge), add
     * them to result list. If the merge is not nice, Loose frames should also have a chance, so if the precision is
     * above 50% AND there are more than 2 (presumed) words / rectangles, add it to resultList.
     * <p>
     * second if: Keep on merging frames together, if newRectangularData is better or just about equal to the previous,
     * add the frame to the mergedFrames and continue. Otherwise, generate result VideoFrame
     *
     * @param frames An ordered list of VideoFrames in order
     * @return An ordered List of VideoFrames, with no duplicates and matching start and end times.
     */
    @Deprecated
    public List<List<VideoFrame>> findSimilarFrames(List<VideoFrame> frames) {
        List<List<VideoFrame>> resultFrames = new LinkedList<>(); // List of merged images, with "optimal" box count
        List<VideoFrame> mergedFrames = new LinkedList<>(); // List of unmerged Frames, to be merged into one whole frame
        List<BufferedImage> mergedImages = new LinkedList<>(); // List of unmerged Frames, to be merged into one whole frame


        //Loop variables
        int loopIdx = 0;
        BufferedImage workingFrame = null;
        RectangularData mergeCollectionData = null;     // consistent with best
        RectangularData newMergedCollectionData;        // Temporary

        // While loop Index is less than number of input frames.
        while (loopIdx < frames.size()) {
            try (Named threadNamed = new Named("Preprocessing: " + frames.get(loopIdx).getFileName())) {
                if (mergedImages.size() == 0 && loopIdx + 2 < frames.size()) {
                    log.debug("Preprocess starting new merge set. Frame {} / {}", loopIdx, frames.size());

                    VideoFrame videoFrame1, videoFrame2;
                    videoFrame1 = frames.get(loopIdx);
                    videoFrame2 = frames.get(1 + loopIdx);

                    mergedFrames.addAll(Arrays.asList(videoFrame1, videoFrame2));

                    mergedImages.add(videoFrame1.getFrame());
                    mergedImages.add(videoFrame2.getFrame());

                    RectangularData data1, mergeData;

                    workingFrame = MergeImages.mergeImages(mergedImages);
                    data1 = frameProcessorOpenCV.generateData(videoFrame1.getFrame());
                    mergeData = frameProcessorOpenCV.generateData(workingFrame);


                    //Continue to the next frame, if text is not recognized at same position for data1 and data2
                    if (FramePreProcessor.this.isNiceMerge(data1, mergeData)) {
                        mergeCollectionData = mergeData;
                        loopIdx = loopIdx + 2;
                    } else {
                        // Loose frames should also have a chance.
                        if (data1.getRectangularDataList().size() >= 1 && data1.getArea() > 100) {
                            log.debug("Caught loose frame? {} / {}", loopIdx, frames.size());
                            mergedFrames.remove(mergedFrames.size() - 1);

                            resultFrames.add(mergedFrames);
                        }
                        workingFrame = null;
                        mergedFrames = new LinkedList<>();
                        mergedImages = new LinkedList<>();
                        loopIdx++;
                    }

                } else if (mergedImages.size() >= 2 && loopIdx + 1 < frames.size()) {
                    log.debug("Preprocess adding to merge set. Frame {} / {}", loopIdx, frames.size());


                    BufferedImage frame;
                    frame = frames.get(loopIdx).getFrame();
                    mergedImages.add(frame);
                    workingFrame = MergeImages.mergeImages(frame, workingFrame);
                    newMergedCollectionData = frameProcessorOpenCV.generateData(workingFrame);

                    // If not nice merge, add merged frame to result list.
                    if (!FramePreProcessor.this.isNiceMerge(mergeCollectionData, newMergedCollectionData)) {
                        mergedImages.remove(mergedImages.size() - 1);

                        // Could write image to disk, for debugging purposes

                        resultFrames.add(mergedFrames);
                        workingFrame = null;
                        mergeCollectionData = null;
                        mergedFrames = new LinkedList<>();
                        mergedImages = new LinkedList<>();

                    } else {
                        mergeCollectionData = newMergedCollectionData;
                        loopIdx++;
                    }

                } else {
                    // Last frame discarded, step towards termination.
                    loopIdx++;
                }
            }
        }

        return resultFrames;
    }

    /**
     * Determines if two frames are a nice merge based on a lot of different parameters, otherwise documented inside
     * this method.
     * Currently making decisions on number of Rectangles, if bounding rectangles of before and after is somewhat
     * contained or overlapping. Also deciding on behalf if there's a dive in number of rectangles on before and after.
     *
     * @param beforeMerge Original
     * @param afterMerge  Original with Merge!
     * @return true, if the merge is defined as a nice merge. Otherwise false.
     */
    @Deprecated
    protected boolean isNiceMerge(RectangularData beforeMerge, RectangularData afterMerge) {
        boolean partlyInside = false;
        boolean precision = false;
        boolean dive = true;

        // If bounding rectangles actually contains rectangles
        if (beforeMerge.getRectangularDataList().size() > 0 && afterMerge.getRectangularDataList().size() > 0) {

            // Detect if boxes are overlapping each other (similar, positions, overlapping, etc.)
            partlyInside = partlyInside(beforeMerge, afterMerge);
        }
        double originalPrecision = 0.0d, newPrecision = 0.0d;
        DecimalFormat df = new DecimalFormat("#.###");
        try {
            originalPrecision = Double.valueOf(df.format(beforeMerge.getPrecision()));
        } catch (NumberFormatException ignored) {
            // Ignored. Set to 0.0
        }
        try {
            newPrecision = Double.valueOf(df.format(afterMerge.getPrecision()));
        } catch (NumberFormatException ignored) {
            // ignored. Set to 0.0
        }


        if (newPrecision >= originalPrecision)
            precision = true;

        if (!precision) {
            dive = false;
        } else {
            if (afterMerge.getRectangularDataList().size() >= beforeMerge.getRectangularDataList().size()) {
                dive = false;
            }
        }


        return (partlyInside && precision) && !dive;
    }

    @Deprecated
    protected boolean partlyInside(RectangularData beforeMerge, RectangularData afterMerge) {
        boolean partlyInside = false;
        // Partly inside
        if (beforeMerge.getTl().getY() >= afterMerge.getTl().getY() && beforeMerge.getTl().getY() <= afterMerge.getBr().getY()) {
            partlyInside = true;
        } else if (beforeMerge.getBr().getY() >= afterMerge.getTl().getY() && beforeMerge.getBr().getY() <= afterMerge.getBr().getY()) {
            partlyInside = true;
        } else if (afterMerge.getTl().getY() >= beforeMerge.getTl().getY() && afterMerge.getTl().getY() <= beforeMerge.getBr().getY()) {
            partlyInside = true;
        } else if (afterMerge.getBr().getY() >= beforeMerge.getTl().getY() && afterMerge.getBr().getY() <= beforeMerge.getBr().getY()) {
            partlyInside = true;
        } else if (afterMerge.getTl().getX() >= beforeMerge.getTl().getX() && afterMerge.getTl().getX() <= beforeMerge.getBr().getX()) {
            partlyInside = true;
        } else if (afterMerge.getBr().getX() >= beforeMerge.getTl().getX() && afterMerge.getBr().getX() <= beforeMerge.getBr().getX()) {
            partlyInside = true;
        } else if (beforeMerge.getTl().getX() >= afterMerge.getTl().getX() && beforeMerge.getTl().getX() <= afterMerge.getBr().getX()) {
            partlyInside = true;
        } else if (beforeMerge.getBr().getX() >= afterMerge.getTl().getX() && beforeMerge.getBr().getX() <= afterMerge.getBr().getX()) {
            partlyInside = true;
        }
        return partlyInside;
    }
}
