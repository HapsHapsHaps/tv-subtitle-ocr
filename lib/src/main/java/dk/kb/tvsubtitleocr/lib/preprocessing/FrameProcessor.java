package dk.kb.tvsubtitleocr.lib.preprocessing;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

@Deprecated
public class FrameProcessor implements IFrameProcessor {
    private static Boolean debug = false;

    public FrameProcessor() {}

    public BufferedImage processFrame(BufferedImage image) {
        BufferedImage img = image;

        img = crop(img);

        img = greyScale(img);

        img = resize(img, 2.0d);

        img = blur(img);

        img = sharpen(img);

        img = darken(img, (int) (255 * 0.8f));

        img = resize(img, 0.5d);

        return img;
    }

    private BufferedImage greyScale(BufferedImage img) {
        BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        ColorConvertOp op = new ColorConvertOp(img.getColorModel().getColorSpace(), ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
        op.filter(img, out);
        return out;
    }

    public BufferedImage blur(BufferedImage image) {
        // A 3x3 kernel that sharpens an image

        int width = 3;
        int height = width;

        float[] kernelArray = new float[width * height];
        Arrays.fill(kernelArray, 1.0f / (width * height));
        Kernel kernel = new Kernel(width, height, kernelArray);

        BufferedImageOp op = new ConvolveOp(kernel);
        image = op.filter(image, null);
        return image;
    }

    public BufferedImage sharpen(BufferedImage image) {
        // A 3x3 kernel that sharpens an image
        Kernel kernel = new Kernel(5, 5,
                new float[]{
                        -1, -1, -1, -1, -1,
                        -1, -1, -1, -1, -1,
                        -1, -1, 25, -1, -1,
                        -1, -1, -1, -1, -1,
                        -1, -1, -1, -1, -1,
                });

        BufferedImageOp op = new ConvolveOp(kernel);
        image = op.filter(image, null);
        return image;
    }

    /**
     * Optimizes a frame in such a way that it will be more OCR friendly.
     * 1. Cropping and leaving the bottom 25%
     * 2. Resizing to 2x size
     * 3. Sharpening to allow for better Thresholding
     * 4. Threshold
     *
     * @param frame
     * @return An optimized image (input frame)
     * @throws Exception If the input Frame could not be read or found, throws an exception.
     */
    public BufferedImage processFrame(Path frame) {
        File imageFile = new File(String.valueOf(frame));
        BufferedImage inProgress = null;
        try {
            inProgress = ImageIO.read(imageFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        inProgress = processFrame(inProgress);

        return inProgress;

    }

    public static BufferedImage resize(BufferedImage img, double resizeFactor) {
        //Calculate new size, multiplied by resizeFactor
        int resizedHeight = (int) (img.getHeight() * resizeFactor);
        int resizedWidth = (int) (img.getWidth() * resizeFactor);


        BufferedImage out = new BufferedImage(resizedWidth, resizedHeight, img.getType());
        Graphics2D g = out.createGraphics();
        g.drawImage(
                img,
                0, 0,
                resizedWidth,
                resizedHeight,
                null);
        return out;
    }

//    private BufferedImage darken(BufferedImage image) {
//        //RescaleOp darken = new RescaleOp(0.1f, 15f, null);
//        //return darken.filter(image, image);
//        return darken(image, (int) (255 * 0.2f));
//    }

//
//    private BufferedImage darken(BufferedImage img, int darkenPercentage){
//
//        BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
//        ColorConvertOp op = new ColorConvertOp(img.getColorModel().getColorSpace(), ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
//        op.filter(img, out);
//        return out;
//    }


    private BufferedImage darken(BufferedImage img, int darkenPercentage) {

        int width = img.getWidth();
        //Reason for bitshifting: https://en.wikipedia.org/wiki/RGBA_color_space
        int height = img.getHeight();
        int[] line = new int[width];
        for (int y = 0; y < height; y++) {

            img.getRGB(0, y, width, 1, line, 0, width);

            for (int x = 0; x < width; x++) {
                int clr = line[x];
                int red = (clr & 0x00ff0000) >> 16;
                int green = (clr & 0x0000ff00) >> 8;
                int blue = clr & 0x000000ff;
                int avg = (red + green + blue) / 3;
                if (avg <= darkenPercentage) {
                    line[x] = Color.MAGENTA.getRGB();
                } else {
                    line[x] = Color.black.getRGB();
//                    img.setRGB(x, y, );
                }
            }
            img.setRGB(0, y, width, 1, line, 0, width);
        }
        return img;
    }


    private BufferedImage greaying(BufferedImage img) {

        int width = img.getWidth();
        //Reason for bitshifting: https://en.wikipedia.org/wiki/RGBA_color_space
        int height = img.getHeight();
        int[] line = new int[width];
        for (int y = 0; y < height; y++) {

            img.getRGB(0, y, width, 1, line, 0, width);
            for (int x = 0; x < width; x++) {
                int clr = line[x]; //11 22 33 44
                int alpha = (clr & 0xff000000) >> 24; //11
                int red = (clr & 0x00ff0000) >> 16; //22
                int green = (clr & 0x0000ff00) >> 8; //33
                int blue = clr & 0x000000ff; //44
                int avg = (red + green + blue) / 3; //22

//                    11 <<24 + 22 << 16 + 22 << 8 + 22 //11 22 22 22
                int newAlpha = alpha << 24;
                int newRed = avg << 16;
                int newGreen = avg << 8;
                int newBlue = avg;
                line[x] = newAlpha + newRed + newGreen + newBlue;


            }
            img.setRGB(0, y, width, 1, line, 0, width);
        }
        return img;
    }

    public static BufferedImage crop(BufferedImage image) {
        BufferedImage cropped = image.getSubimage(
                0,
                (image.getHeight() / 4) * 3,
                image.getWidth(),
                image.getHeight() / 4);
        return cropped;
    }
}
