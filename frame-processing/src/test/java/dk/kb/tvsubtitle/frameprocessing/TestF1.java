package dk.kb.tvsubtitle.frameprocessing;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.bytedeco.javacpp.opencv_core.LINE_8;
import static org.bytedeco.javacpp.opencv_imgproc.rectangle;

public class TestF1 {

    private static final Path outputLocation = Paths.get("/path/to/resultOutput");
    private static final Path inputLocation = Paths.get("/path/to/folderOfInputFrames/");

    @Test
    @Disabled("For development experimentation only")
    public void f1() throws URISyntaxException, IOException {

        Map<String, List<Rect>> imgsToRects = getImgToRects();

        System.out.println(imgsToRects);

        Map<String, Set<Rect>> foundBoxes = new HashMap<>();
        for (Map.Entry<String, List<Rect>> fileToRects : imgsToRects.entrySet()) {
            Map<Rect, Set<Rect>> boxes = f1(fileToRects.getKey(), fileToRects.getValue());
            Set<Rect> bsd = boxes.entrySet().stream().map(box -> {
                Set<Rect> value = box.getValue();
                Point topLeft = getTopLeft(value);
                Point bottomRight = getBottomRight(value);
                return new Rect(topLeft, bottomRight);
            }).collect(Collectors.toSet());



            foundBoxes.put(fileToRects.getKey(),bsd);


            opencv_core.Mat rgb = bufferedImageToMat(ImageIO.read(new File(inputLocation.toFile(),new File(fileToRects.getKey()).getName())));
            int rgbCenter = rgb.cols()/2;
            int maxLengthToCenter = 0;

            if(bsd.stream().max(Comparator.comparingInt(r -> area(r))).isPresent()) {
                Rect rar = bsd.stream().max(Comparator.comparingInt(r -> area(r))).get();
                rectangle(rgb, new opencv_core.Rect(new opencv_core.Point(rar.br.x, rar.br.y), new opencv_core.Point(rar.tl.x, rar.tl.y)), opencv_core.Scalar.BLUE, 3, LINE_8, 0);

            }

            for (Rect rect : fileToRects.getValue()) {
                rectangle(rgb, new opencv_core.Rect(new opencv_core.Point(rect.br.x,rect.br.y) , new opencv_core.Point(rect.tl.x,rect.tl.y)), opencv_core.Scalar.YELLOW, 1, LINE_8, 0);
            }

            try{
                String key = new File(fileToRects.getKey()).getName();
                ImageIO.write(matToBufferedImage(rgb), "png", new File(outputLocation.toAbsolutePath().toFile(), key));
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println(fileToRects.getKey());
            System.out.println(bsd);
            System.out.println();
        }
    }

    private Map<String, List<Rect>> getImgToRects() throws IOException, URISyntaxException {
        Map<String,List<Rect>> imgsToRects = new HashMap<>();

        List<String> lines = Files.readAllLines(new File(Thread.currentThread().getContextClassLoader().getResource("testfile.csv").toURI()).toPath());
        for (String line : lines) {
            String[] splits = line.split(",");
            String imgName = splits[0];
            try {
                List<Rect> rects = new ArrayList<>();
                for (int i = 1; i < splits.length; i++) {
                    String coll = splits[i];
                    String[] twos = coll.split("-");
                    String tlString = twos[0];
                    String brString = twos[1];

                    Point tl = getPoint(tlString);
                    Point br = getPoint(brString);

                    Rect rect = new Rect(tl, br);
                    rects.add(rect);
                }
                imgsToRects.put(imgName, rects);
            } catch (RuntimeException e){
                e.printStackTrace();
                System.out.println(imgName);
                throw new RuntimeException(e);
            }
        }
        return imgsToRects;
    }

    public Map<Rect, Set<Rect>> f1(String key, List<Rect> rects) {

        int rectsPrArea = 0;

        Map<Rect, Set<Rect>> results = new HashMap<>();

        for (Rect startNode : rects) {

            boolean somethingIncludedThisTimeRound = false;

            Set<Rect> includes = new HashSet<>(Arrays.asList(startNode));

            Set<Rect> nodes = new HashSet<>(rects);
            nodes.remove(startNode);

            do {
                somethingIncludedThisTimeRound = false;

                for (Rect node : nodes) {

                    if (good_to_include(node,includes)) {

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

    private boolean good_to_include(Rect node, Set<Rect> includes) {

        int areaBeforeInclude = area(includes);

        int areaOfNode = area(node.tl, node.br);

        int areaAfterInclude = area(includes, node);

        int trashSpace = areaAfterInclude - areaBeforeInclude - areaOfNode;

        return (areaOfNode*3 > trashSpace);

    }

    private int area(Rect rect){
        Point topLeft =  rect.tl;
        Point bottomRight = rect.br;
        return area(topLeft, bottomRight);
    }


    private int area(Set<Rect> rects, Rect... extracs){
        Point topLeft = getTopLeft(rects, extracs);
        Point bottomRight = getBottomRight(rects, extracs);
        return area(topLeft, bottomRight);
    }

    private int area(Point tl, Point br) {
        int height = br.y - tl.y;
        int width = br.x - tl.x;
        return width*height;
    }

    private Point getTopLeft(Set<Rect> rects, Rect... extras) {
        int lowestX = Integer.MAX_VALUE, lowestY = Integer.MAX_VALUE;
        for (Rect rect : rects) {
            if (rect.tl.x < lowestX){
                lowestX = rect.tl.x;
            }
            if (rect.tl.y < lowestY){
                lowestY = rect.tl.y;
            }
        }
        for (Rect rect : extras) {
            if (rect.tl.x < lowestX){
                lowestX = rect.tl.x;
            }
            if (rect.tl.y < lowestY){
                lowestY = rect.tl.y;
            }
        }
        return new Point(lowestX,lowestY);
    }

    private Point getBottomRight(Set<Rect> rects, Rect... extras) {
        int highestX = Integer.MIN_VALUE, highestY = Integer.MIN_VALUE;
        for (Rect rect : rects) {
            if (rect.br.x > highestX){
                highestX = rect.br.x;
            }
            if (rect.br.y > highestY){
                highestY = rect.br.y;
            }
        }
        for (Rect rect : extras) {
            if (rect.br.x > highestX){
                highestX = rect.br.x;
            }
            if (rect.br.y > highestY){
                highestY = rect.br.y;
            }

        }
        return new Point(highestX,highestY);
    }


    private Point getPoint(String string) {
        String[] s = string.split("/");
        return new Point(Integer.parseInt(s[0].trim()), Integer.parseInt(s[1].trim()));
    }

    public static class Rect {
        private Point tl, br;

        public Rect(Point tl, Point br) {
            this.tl = tl;
            this.br = br;
        }

        public Point getTl() {
            return tl;
        }

        public Point getBr() {
            return br;
        }

        @Override
        public String toString() {
            return "Rect{" +
                    "tl=" + tl +
                    ", br=" + br +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Rect rect = (Rect) o;
            return Objects.equals(tl, rect.tl) &&
                    Objects.equals(br, rect.br);
        }

        @Override
        public int hashCode() {

            return Objects.hash(tl, br);
        }
    }

    public static class Point {
        private int x,y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        @Override
        public String toString() {
            return "Point{" +
                    "x=" + x +
                    ", y=" + y +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Point point = (Point) o;
            return x == point.x &&
                    y == point.y;
        }

        @Override
        public int hashCode() {

            return Objects.hash(x, y);
        }
    }

    /**
     * Converts a BufferedImage to opencv_core.Mat
     * @param bufferedImage Image to be converted
     * @return The converted Mat from BufferedImage
     */
    private opencv_core.Mat bufferedImageToMat(BufferedImage bufferedImage) {
        OpenCVFrameConverter.ToMat cv = new OpenCVFrameConverter.ToMat();
        return cv.convertToMat(new Java2DFrameConverter().convert(bufferedImage));
    }

    /**
     * Converts a opencv_core.Mat to BufferedImage
     * @param mat Mat to be converted
     * @return Converted BufferedImage
     */
    private BufferedImage matToBufferedImage(opencv_core.Mat mat) {
        Java2DFrameConverter converter = new Java2DFrameConverter();
        return converter.getBufferedImage(new OpenCVFrameConverter.ToMat().convert(mat));
    }
}
