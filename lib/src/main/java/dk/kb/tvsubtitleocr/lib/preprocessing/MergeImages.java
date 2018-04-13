package dk.kb.tvsubtitleocr.lib.preprocessing;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class MergeImages {

    /**
     * Combining all images into one Merged image based on input
     *
     * @param images Arbitrary collection of Images
     * @return Merged / Combined image.
     */
    public static BufferedImage mergeImages(Collection<BufferedImage> images) {
        //error check
        BufferedImage img = images.iterator().next();
        int height = img.getHeight();
        int width = img.getWidth();
        int type = img.getType();

        if (!images.stream().allMatch(image -> {
            boolean match = true;
            if (!(image.getHeight() == height && image.getWidth() == width && image.getType() == type))
                match = false;
            return match;
        })) {
            throw new IllegalArgumentException("Image properties does not match all elements");
        }

        //average image
        List<Integer> r = new ArrayList<>();
        List<Integer> g = new ArrayList<>();
        List<Integer> b = new ArrayList<>();
        images.forEach(image -> {
            int[] lineData = new int[width];
            for (int y = 0; y < height; y++) {

                image.getRGB(0, y, width, 1, lineData, 0, image.getWidth());

                for (int x = 0; x < width; x++) {
                    int color = lineData[x];
                    r.add((color & 0x00ff0000) >> 16);
                    g.add((color & 0x0000ff00) >> 8);
                    b.add(color & 0x000000ff);

                }
            }
        });

        int sizeOffset = width * height;
        BufferedImage returnImage = new BufferedImage(width, height, type);
        // SizeOffset is equal to amount of pixels, thus this loop iterates over all pixels for one frame.
        for (int j = 0; j < sizeOffset; j++) {
            int colr = 0;
            int colg = 0;
            int colb = 0;

            for (int i = 0; i < images.size(); i++) {
                // (sizeoffset  * current image) + current pixel
                colr += r.get((sizeOffset * i) + j);
                colg += g.get((sizeOffset * i) + j);
                colb += b.get((sizeOffset * i) + j);
            }
            int col = ((colr / images.size()) << 16) | ((colg / images.size()) << 8) | colb / images.size();
            int x;
            int y;
            if (j == 0) {
                y = 0;
                x = 0;
            } else {
                x = j % width;
                y = j / width;
            }

            returnImage.setRGB(x, y, col);
        }
        return returnImage;
    }

    /**
     * Merging two images
     *
     * @param image1
     * @param image2
     * @return Combined image of params.
     */
    public static BufferedImage mergeImages(BufferedImage image1, BufferedImage image2) {
        return mergeImages(Arrays.asList(image1, image2));
    }

    //TODO: Javadoc.
    public static BufferedImage punctureImage(BufferedImage image1, BufferedImage image2) {
        //Error check
        if (!(image1.getWidth() == image2.getWidth()
                && image1.getHeight() == image2.getHeight()
                && image1.getType() == image2.getType())) {
            return null;
        }
        int type = image1.getType();
        int width = image1.getWidth();
        int height = image2.getHeight();
        BufferedImage returnImage = new BufferedImage(width, height, type);
        int[] linedata1 = new int[width];
        int[] linedata2 = new int[width];
        for (int y = 0; y < height; y++) {

            image1.getRGB(0, y, width, 1, linedata1, 0, width);
            image2.getRGB(0, y, width, 1, linedata2, 0, width);

            for (int x = 0; x < width; x++) {
                int clr1 = linedata1[x];
                int clr2 = linedata2[x];
                if (clr1 == clr2) {
                    returnImage.setRGB(x, y, clr1);
                }
            }
        }
        return returnImage;
    }

}
