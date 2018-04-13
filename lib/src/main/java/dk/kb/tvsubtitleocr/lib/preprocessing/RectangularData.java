package dk.kb.tvsubtitleocr.lib.preprocessing;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class RectangularData {
    private Point tl;
    private Point br;
    private int width;
    private int height;
    private int area;
    private double precision;

    private List<RectangularData> rectangularDataList;
    private BufferedImage frame;

    public RectangularData(Point tl, Point br) {
        this.tl = tl;
        this.br = br;
        setHeight(br.y - tl.y);
        setWidth(br.x - tl.x);
        setArea(getWidth() * getHeight());
    }

    public RectangularData(Point tl, Point br, List<RectangularData> rectangularDataList) {
        this.tl = tl;
        this.br = br;
        this.rectangularDataList = rectangularDataList;
        setHeight(br.y - tl.y);
        setWidth(br.x - tl.x);
        setArea(getWidth() * getHeight());
    }

    public RectangularData(Point tl, Point br, List<RectangularData> rectangularDataList, BufferedImage frame) {

        this.tl = tl;
        this.br = br;
        this.rectangularDataList = rectangularDataList;
        this.frame = frame;
        setHeight(br.y - tl.y);
        setWidth(br.x - tl.x);
        setArea(getWidth() * getHeight());
    }

    public RectangularData(Point tl, Point br, double precision, List<RectangularData> rectangularDataList, BufferedImage frame) {
        this.tl = tl;
        this.br = br;
        this.precision = precision;
        this.rectangularDataList = rectangularDataList;
        this.frame = frame;
        setHeight(br.y - tl.y);
        setWidth(br.x - tl.x);
        setArea(getWidth() * getHeight());
    }

    public double getPrecision() {
        return precision;
    }

    public void setPrecision(double precision) {
        this.precision = precision;
    }

    public BufferedImage getFrame() {
        return frame;
    }

    public void setFrame(BufferedImage frame) {
        this.frame = frame;
    }

    public Point getTl() {
        return tl;
    }

    public void setTl(Point tl) {
        this.tl = tl;
    }

    public Point getBr() {
        return br;
    }

    public void setBr(Point br) {
        this.br = br;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getArea() {
        return area;
    }

    public void setArea(int area) {
        this.area = area;
    }

    public List<RectangularData> getRectangularDataList() {
        return rectangularDataList;
    }

    public void setRectangularDataList(List<RectangularData> rectangularDataList) {
        this.rectangularDataList = rectangularDataList;
    }
}
