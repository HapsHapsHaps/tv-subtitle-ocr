package dk.kb.tvsubtitleocr.lib.preprocessing;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.stream.Collectors;

public class FrameProcessorOpenCV implements IFrameProcessor {
    final static Logger log = LoggerFactory.getLogger(FrameProcessorOpenCV.class);

    public FrameProcessorOpenCV() {

    }

    /**
     * Inspiration, Rewritten java example of SO post:
     * https://stackoverflow.com/questions/34271356/extracting-text-opencv-java
     *
     * Takes a mat as parameter, double up the size of the picture, finding rects with another method,
     * finds and returns the bounding/gathered rect.
     * @param main
     * @return
     */
    public opencv_core.Rect findText(opencv_core.Mat main) {
        List<opencv_core.Rect> imgRects;

        opencv_core.Mat rgb = newMat();
        //double size the image and put into a new mat.
        doPyrUp(main, rgb);

        //find all text
        imgRects = imgToRects(main);
        for (opencv_core.Rect r:
                imgRects) {
            drawRectangle(rgb, r, opencv_core.Scalar.CYAN);
        }

        //returning the rect enclosing the subtitle for the cropping method.
        return gatherRects(imgRects);
    }

    /**
     * The brains, the method implemented from Interface.
     * Due to testing and various abusing of this class, this method changes a lot.
     * This method takes a bufferedimage and converts it to a mat. OpenCV is using the mat for processing the image
     * and finding text.
     *
     * Finding the bounding box, and removing all textareas that is not contained by the bounding box, crops the image
     * and returns a cropped image only containing what is presumed to be the subtitle.
     * @param image (type: bufferedimage)
     * @return image cropped (type: bufferedimage)
     */
    public BufferedImage processFrame(BufferedImage image) {
        opencv_core.Mat mat = bufferedImageToMat(image);

        opencv_core.Rect rect = findText(mat);
        List<opencv_core.Rect> rectList = imgToRects(mat);

        rectList.removeIf(x -> !isContained(rect, x));

        BufferedImage returnFrame;
        if (rect.area() > 0) {
            returnFrame = crop(image, rect);
        } else {
            returnFrame = image;
        }

        return returnFrame;

    }

    /**
     * Generates RectangularData based on the BufferedImage input.
     * It generates two Points, which contains the overall Rectangle containing the sublist of Rectangles
     * @param image BufferedImage input
     * @return RectangularData
     */
    public RectangularData generateData(BufferedImage image) {

        // Convert BufferedImage to Mat, to utilize JavaCV functionality
        opencv_core.Mat mat = bufferedImageToMat(image);

        // Find the bounding Rect
        opencv_core.Rect rect = findText(mat);

        // Find all Rectangles
        doPyrUp(mat, mat);
        List<opencv_core.Rect> rectList = imgToRects(mat);

        // Remove rectangles, which are NOT included within the bounding rect
        rectList.removeIf(x -> !isContained(rect, x));

        // Convert org.bytedeco.javacpp.opencv_core.Rect to RectangularData
        List<RectangularData> rectDataList = rectList.stream().map(
                rectValue -> new RectangularData(
                        new java.awt.Point(rectValue.tl().x(), rectValue.tl().y()),
                        new java.awt.Point(rectValue.br().x(), rectValue.br().y())))
                .collect(Collectors.toList());
        double overallArea;

        //size of the bounding rect
        overallArea = rect.area();
        double rectWithinArea = 0d;

        //calculates the combined area of all the small textareas inside the bounding box.
        for (opencv_core.Rect r:
                rectList) {
            rectWithinArea += r.area();
        }

        //precision is the part of the bounding box that is covered by text. Gives a decimal, but can be above 1.
        //Since the textboxes can be overlapping.
        double precision = rectWithinArea / overallArea;

        //returns a dataset based on the bounding box with precision, sice, textbox-count within
        //and a pair of points (TL, BR)
        return new RectangularData(
                new java.awt.Point(rect.tl().x(), rect.tl().y()),
                new java.awt.Point(rect.br().x(), rect.br().y()),
                precision,
                rectDataList,
                matToBufferedImage(mat));
    }

    /**
     * This method takes a image(type: mat), processes the image for easier text detection. The processing is giveing
     * a list of contours, which is used to analyse and find the rects(boxes) that is presumed to contain text.
     * @param rgb (image, type: mat)
     * @return list of rects.
     */
    public List<opencv_core.Rect> imgToRects(opencv_core.Mat rgb) {

        //grayscaling the picture
        opencv_core.Mat grayscaled = makeCvtColor(rgb, org.bytedeco.javacpp.opencv_imgproc.COLOR_RGB2GRAY);

        //constructs and returns the structuring element, preparing for the morphologyEx method.
        opencv_core.Mat morphKernel = morphKernel(org.bytedeco.javacpp.opencv_imgproc.MORPH_ELLIPSE, new opencv_core.Size(3, 3));

        //morphological transformations using an erosion and dilation as basic operations.
        opencv_core.Mat gradiented = doMorphology(grayscaled, morphKernel, org.bytedeco.javacpp.opencv_imgproc.MORPH_GRADIENT);

        //Using a threshold value to make the picture black and white (either black or white based on the value).
        opencv_core.Mat blackWhite = doThreshold(gradiented, 0.0, 255.0, org.bytedeco.javacpp.opencv_imgproc.THRESH_OTSU);

        //constructs and returns the structuring element, preparing for the morphologyEx method.
        morphKernel = morphKernel(org.bytedeco.javacpp.opencv_imgproc.MORPH_RECT, new opencv_core.Size(9, 1));

        //morphological transformations using an erosion and dilation as basic operations.
        opencv_core.Mat connected = doMorphology(blackWhite, morphKernel, org.bytedeco.javacpp.opencv_imgproc.MORPH_CLOSE);

        //finding the contours in the picture, used for finding text.
        opencv_core.MatVector contours = doFindContours(connected, org.bytedeco.javacpp.opencv_imgproc.RETR_CCOMP, org.bytedeco.javacpp.opencv_imgproc.CHAIN_APPROX_SIMPLE, new opencv_core.Point(0, 0));

        //returns a list of rect that is presumed to text based on contour sizes.
        return loopContours(rgb, blackWhite, contours);
    }

    /**
     * Select relevant contours
     * @param rgb
     * @param contours
     * @return
     */
    private List<opencv_core.Rect> loopContours(opencv_core.Mat rgb, opencv_core.Mat bw, opencv_core.MatVector contours) {
        List<opencv_core.Rect> rects = new LinkedList<>();

        for (int idx = 0; idx < contours.size(); idx++) {

            opencv_core.Mat points = contours.get(idx);

            opencv_core.Rect rect = makeBoundingRect(points);

            if (rect.area() < (rgb.size().area() * 0.25)) {
                if ((rect.height() > 20 && rect.width() > 16)) {
                    if (rect.height() < rect.width()) {
                        rects.add(rect);
                    }
                }
            }
        }
        return rects;
    }

    /**
     * Takes a image(type: mat) and and list of rectangles within the image.
     * With these parameters, the most likely rect including the subtitle is calculated
     *
     * For this method we are using a F1-score-method for an analysis of what is most likely to be a subtitle.
     *
     * @param imgRects (Type: List of rects)
     * @return A Rect (bounding/gathered rect)
     */
    public opencv_core.Rect gatherRects(List<opencv_core.Rect> imgRects) {

        opencv_core.Rect returnRect = new opencv_core.Rect();
        Map<opencv_core.Rect, List<opencv_core.Rect>> boxes = f1(imgRects);
        List<opencv_core.Rect> bsd = boxes.entrySet().stream().map(box -> {
            List<opencv_core.Rect> value = box.getValue();
            opencv_core.Point topLeft = getTopLeft(value);
            opencv_core.Point bottomRight = getBottomRight(value);
            return new opencv_core.Rect(topLeft, bottomRight);
        }).collect(Collectors.toList());

        if (bsd.stream().max(Comparator.comparingInt(r -> area(r))).isPresent()) {
            opencv_core.Rect rar = bsd.stream().max(Comparator.comparingInt(r -> area(r))).get();
            returnRect = new opencv_core.Rect(new opencv_core.Point(rar.br().x(), rar.br().y()), new opencv_core.Point(rar.tl().x(), rar.tl().y()));
        }
        return returnRect;
    }

    /**
     * Takes a bufferedimage as parameter, proccesses the image and counts the rects enclosing text, calculates the
     * rect gathering the subtitle and then counts the individual text boxes within this gathered box.
     *
     * @param image
     * @return
     */
    public int rectCount(BufferedImage image) {
        int count = 0;

        opencv_core.Mat mat = bufferedImageToMat(image);
        opencv_core.Mat rgb = newMat();
        doPyrUp(mat, rgb);

        //find all text
        List<opencv_core.Rect> rects = imgToRects(rgb);

        //find the bounding / gathered rect including the subtitle
        opencv_core.Rect gatheredRect = gatherRects(rects);

        //counting the boxes that is presumed to be text.
        for (opencv_core.Rect rect : rects) {
            if (isContained(gatheredRect, rect)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Takes two rects, the gathered rect and another rect. This method checks whether the rects is included in the
     * gathered rect or not. (used in the rectCount-Method).
     *
     * @param gatheredBox
     * @param rect
     * @return
     */
    public boolean isContained(opencv_core.Rect gatheredBox, opencv_core.Rect rect) {
        boolean inside = false;
        if ((gatheredBox.tl().x() <= rect.tl().x() && gatheredBox.tl().y() <= rect.tl().y())
                && (gatheredBox.br().x() >= rect.br().x() && gatheredBox.br().y() >= rect.br().y())) {
            inside = true;
        }
        return inside;
    }

    /**
     * F1 Score is a method used for statistical analysis, where we are testing wheater or not a textbox should be considered
     * as a subtitle or not.
     * This method is a measuring of accuracy and probability of the individual box of being a subtitle.
     * More information upon the F1-score theroy: https://en.wikipedia.org/wiki/F1_score
     *
     * @param rects
     * @return
     */
    public Map<opencv_core.Rect, List<opencv_core.Rect>> f1(List<opencv_core.Rect> rects) {
        Map<opencv_core.Rect, List<opencv_core.Rect>> results = new HashMap<>();

        for (opencv_core.Rect startNode : rects) {

            boolean somethingIncludedThisTimeRound = false;
            List<opencv_core.Rect> includes = new LinkedList<>(Arrays.asList(startNode));
            Set<opencv_core.Rect> nodes = new HashSet<>(rects);
            nodes.remove(startNode);

            do {
                somethingIncludedThisTimeRound = false;

                for (opencv_core.Rect node : nodes) {

                    if (goodToInclude(node, includes)) {
                        somethingIncludedThisTimeRound = true;
                        includes.add(node);
                    }
                }
                nodes.removeAll(includes);

            } while (somethingIncludedThisTimeRound);
            results.put(startNode, includes);
        }

        return results;
        //#choose largest box
    }

    /**
     * The measuring of probability of being a subtitle. This method is making the decision upon some "buffervalues".
     * The buffervalues is introduced by the assessment that we would rather include a little too much text than
     * cutting off some of the subtitles.
     *
     * @param node
     * @param includes
     * @return
     */
    private boolean goodToInclude(opencv_core.Rect node, List<opencv_core.Rect> includes) {


        int areaBeforeInclude = area(includes);

        int areaOfNode = area(node.tl(), node.br());

        int areaAfterInclude = area(includes, node);

        int trashSpace = areaAfterInclude - areaBeforeInclude - areaOfNode;

        return (areaOfNode * 3 > trashSpace * 1.1);

    }

    /**
     * Takes in the raw image, scaling the image up to match the proccessed frame and rect.
     * Then we are cropping the image based on the guessed subtitle-position.
     *
     * @param image
     * @param rect
     * @return
     */
    public BufferedImage crop(BufferedImage image, opencv_core.Rect rect) {
        opencv_core.Mat mat = bufferedImageToMat(image);
        doPyrUp(mat, mat);
        image = matToBufferedImage(mat);
        //All sizes is divided by two - OpenCV is used then finding the text.
        //OpenCV double the size of the image when finding the text.
        BufferedImage cropped = image.getSubimage(
                rect.x(),
                rect.y(),
                rect.width(),
                rect.height());
        return cropped;
    }

    /**
     * Calculates the area size of a rect
     *
     * @param rect
     * @return
     */
    private int area(opencv_core.Rect rect) {
        opencv_core.Point topLeft = rect.tl();
        opencv_core.Point bottomRight = rect.br();
        return area(topLeft, bottomRight);
    }

    /**
     * Calculates the area size of one or more rects
     *
     * @param rects
     * @param extracs
     * @return
     */
    private int area(List<opencv_core.Rect> rects, opencv_core.Rect... extracs) {
        opencv_core.Point topLeft = getTopLeft(rects, extracs);
        opencv_core.Point bottomRight = getBottomRight(rects, extracs);
        return area(topLeft, bottomRight);
    }

    /**
     * Takes two points and calculates the area between them
     *
     * @param tl
     * @param br
     * @return
     */
    private int area(opencv_core.Point tl, opencv_core.Point br) {
        int height = br.y() - tl.y();
        int width = br.x() - tl.x();
        return width * height;
    }

    /**
     * Calculates the TOP-LEFT corner of a rect (used for measuring the gathered box)
     *
     * @param rects
     * @param extras
     * @return
     */
    private opencv_core.Point getTopLeft(List<opencv_core.Rect> rects, opencv_core.Rect... extras) {
        int lowestX = Integer.MAX_VALUE, lowestY = Integer.MAX_VALUE;
        for (opencv_core.Rect rect : rects) {
            if (rect.tl().x() < lowestX) {
                lowestX = rect.tl().x();
            }
            if (rect.tl().y() < lowestY) {
                lowestY = rect.tl().y();
            }
        }
        for (opencv_core.Rect rect : extras) {
            if (rect.tl().x() < lowestX) {
                lowestX = rect.tl().x();
            }
            if (rect.tl().y() < lowestY) {
                lowestY = rect.tl().y();
            }
        }
        return new opencv_core.Point(lowestX, lowestY);
    }

    /**
     * Calculates the BOTTOM-RIGHT corner of a rect (used for measuring the gathered box)
     *
     * @param rects
     * @param extras
     * @return
     */
    private opencv_core.Point getBottomRight(List<opencv_core.Rect> rects, opencv_core.Rect... extras) {
        int highestX = Integer.MIN_VALUE, highestY = Integer.MIN_VALUE;
        for (opencv_core.Rect rect : rects) {
            if (rect.br().x() > highestX) {
                highestX = rect.br().x();
            }
            if (rect.br().y() > highestY) {
                highestY = rect.br().y();
            }
        }
        for (opencv_core.Rect rect : extras) {
            if (rect.br().x() > highestX) {
                highestX = rect.br().x();
            }
            if (rect.br().y() > highestY) {
                highestY = rect.br().y();
            }
        }
        return new opencv_core.Point(highestX, highestY);
    }

    /**
     * Takes an Image, a RectVector and a Color. Draws all Rectangles from RectVector onto Mat with the given Color
     *
     * @param src    Original Image, also output
     * @param groups RectVector (Vector<Rect>) containing all Rect(angles) to be drawn
     * @param color  Color to draw Rect(angle)s with, must be Scalar.(COLOR!)
     */
    private void groups_draw(opencv_core.Mat src, opencv_core.RectVector groups, opencv_core.Scalar color) {

        for (int i = (int) groups.size() - 1; i >= 0; i--) {
            if (src.type() == opencv_core.CV_8UC3)
                org.bytedeco.javacpp.opencv_imgproc.rectangle(
                        src,
                        groups.get(i).tl(),
                        groups.get(i).br(),
                        color,//Scalar( 0, 255, 255 ),
                        3,
                        org.bytedeco.javacpp.opencv_core.LINE_8,
                        0);
            else
                org.bytedeco.javacpp.opencv_imgproc.rectangle(
                        src,
                        groups.get(i).tl(),
                        groups.get(i).br(),
                        opencv_core.Scalar.WHITE,
                        3,
                        org.bytedeco.javacpp.opencv_core.LINE_8,
                        0);
        }
    }

    /**
     * Converts a BufferedImage to opencv_core.Mat
     *
     * @param bufferedImage Image to be converted
     * @return The converted Mat from BufferedImage
     */
    private opencv_core.Mat bufferedImageToMat(BufferedImage bufferedImage) {
        OpenCVFrameConverter.ToMat cv = new OpenCVFrameConverter.ToMat();
        return cv.convertToMat(new Java2DFrameConverter().convert(bufferedImage));
    }

    /**
     * Converts a opencv_core.Mat to BufferedImage
     *
     * @param mat Mat to be converted
     * @return Converted BufferedImage
     */
    private BufferedImage matToBufferedImage(opencv_core.Mat mat) {
        Java2DFrameConverter converter = new Java2DFrameConverter();
        return converter.getBufferedImage(new OpenCVFrameConverter.ToMat().convert(mat));
    }


    private void doPyrUp(opencv_core.Mat src, opencv_core.Mat dst) {
        org.bytedeco.javacpp.opencv_imgproc.pyrUp(src, dst);
    }

    private void copy(opencv_core.Mat rgb, opencv_core.Mat mask, opencv_core.Mat maskROI) {
        rgb.copyTo(maskROI, mask);
    }

    private opencv_core.Mat makeMaskROI(opencv_core.Mat mask, opencv_core.Rect rect) {
        return new opencv_core.Mat(mask, rect);
    }

    private void drawRectangle(opencv_core.Mat image, opencv_core.Rect rect, opencv_core.Scalar colour) {
        org.bytedeco.javacpp.opencv_imgproc.rectangle(image,
                rect.br(),
                new opencv_core.Point(
                        rect.br().x() - rect.width(),
                        rect.br().y() - rect.height()),
                colour);
    }

    private opencv_core.Mat makeCvtColor(opencv_core.Mat src, int colorRgb2gray) {
        opencv_core.Mat dst = newMat();
        org.bytedeco.javacpp.opencv_imgproc.cvtColor(src, dst, colorRgb2gray);
        return dst;
    }

    private opencv_core.MatVector doFindContours(opencv_core.Mat image, int mode, int method, opencv_core.Point offset) {
        opencv_core.Mat hierarchy = newMat();
        opencv_core.MatVector contours = newMatVector();

        org.bytedeco.javacpp.opencv_imgproc.findContours(image, contours, hierarchy, mode, method, offset);
        return contours;
    }

    private opencv_core.Rect makeBoundingRect(opencv_core.Mat points) {
        return org.bytedeco.javacpp.opencv_imgproc.boundingRect(points);
    }

    private opencv_core.Mat makeMask(opencv_core.Mat bw) {
        return opencv_core.Mat.zeros(bw.size(), opencv_core.CV_8UC1).asMat();
    }

    private opencv_core.Mat doThreshold(opencv_core.Mat src, double thresh, double maxval, int type) {
        opencv_core.Mat dst = newMat();
        org.bytedeco.javacpp.opencv_imgproc.threshold(src, dst, thresh, maxval, type);
        return dst;
    }

    private opencv_core.Mat doMorphology(opencv_core.Mat src, opencv_core.Mat morphKernel, int morphGradient) {
        opencv_core.Mat dst = newMat();
        org.bytedeco.javacpp.opencv_imgproc.morphologyEx(src, dst, morphGradient, morphKernel);
        return dst;
    }

    private opencv_core.Mat morphKernel(int shape, opencv_core.Size size) {
        return org.bytedeco.javacpp.opencv_imgproc.getStructuringElement(shape, size);
    }

    private opencv_core.MatVector newMatVector() {
        return new opencv_core.MatVector();
    }

    private opencv_core.Mat newMat() {
        return new opencv_core.Mat();
    }
}
