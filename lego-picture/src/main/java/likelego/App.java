package likelego;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Put file names pic.png to working directory
 * press Run and enjoy
 */
public class App 
{
    /**
     * name of original image
     *  @see <a href="https://imageio.readthedocs.io/en/v2.8.0/formats.html">supported formats</a>
     */
    public static final String ORIGINAL_IMAGE_FILENAME = "pic.png";
    /**
     * Size of one pallet in lego elements
     * 16 means  16x16
     */
    public static int PALLET_SIZE = 16;
    /**
     * How many pallets has whole canvas
     * 3 means 3x3
     */
    public static int PALLETS_IN_CANVAS = 3;
    public static int CANVAS_SIZE = PALLET_SIZE * PALLETS_IN_CANVAS;
    /**
     * Pixel size of one lego element
     */
    public static int EXTEND_FACTOR = 20;
    /**
     * Pixel size of boarder around lego element
     */
    public static int EXTEND_FACTOR_BOARDER = 2;
    /**
     * List of colors available in LEGO set
     * first element is default color and used to fill empty slots
     */
    public static List<Color> AVAILABLE_COLORS = List.of(
        new Color(255, 255, 255),
        new Color(248, 5, 5),
        new Color(246, 132, 61),
        new Color(214, 252, 43),
        new Color(73, 190, 46),
        new Color(0, 197, 238)
    );
    public static void main( String[] args ) throws IOException {
        //read original image
        BufferedImage img = ImageIO.read(new File(ORIGINAL_IMAGE_FILENAME));
        /*
          Size of original image, in case if image is not rectangular odd part would be filled with default color
          to avoid such behaviour use {@link Math#min(long, long)} instead then would crop the image
         */
        int size = Math.max(img.getHeight(), img.getWidth());
        int pixelSize = size / CANVAS_SIZE;
        BufferedImage originalColors = new BufferedImage(CANVAS_SIZE, CANVAS_SIZE, BufferedImage.TYPE_INT_RGB);
        BufferedImage adaptedColors = new BufferedImage(CANVAS_SIZE, CANVAS_SIZE, BufferedImage.TYPE_INT_RGB);
        //calculate color of every single lego element and write it to images
        for (int x=0; x < CANVAS_SIZE; x++){
            for (int y=0; y < CANVAS_SIZE; y++){
                Color avgColor = calculateAvgColor(getSubImage(img, pixelSize, x, y));
                Color adaptedColor = getAdaptedColor(avgColor);
                originalColors.setRGB(x, y, avgColor.getRGB());
                adaptedColors.setRGB(x, y, adaptedColor.getRGB());
            }
        }
        ImageIO.write(originalColors, "jpg", new File("small_original_colors.jpg"));
        ImageIO.write(adaptedColors, "jpg", new File("small_adapted_colors.jpg"));
        // since calculated images are quite small, extend image for convenience
        BufferedImage extended = extendImage(adaptedColors);
        ImageIO.write(extended, "jpg", new File("canvas.jpg"));
        // split image to pallets
        splitToPallets(extended);
        System.out.println("Done");
    }

    /**
     * Split extended image to pallets and write them to disk
     * @param image {@link BufferedImage}
     * @throws IOException
     */
    private static void splitToPallets(BufferedImage image) throws IOException {
        for (int x=0; x < PALLETS_IN_CANVAS; x++){
            for (int y=0; y < PALLETS_IN_CANVAS; y++){
                int imageSize = EXTEND_FACTOR * PALLET_SIZE;
                BufferedImage pallet = image.getSubimage(x * imageSize, y * imageSize, imageSize, imageSize);
                ImageIO.write(pallet, "jpg", new File(String.format("pallet_%s%s.jpg", x,y)));

            }
        }
    }

    /**
     * Based on calculated image increase size and write each pixel as a circle
     * @param toExtend {@link BufferedImage}
     * @return {@link BufferedImage}
     */
    private static BufferedImage extendImage(BufferedImage toExtend) {
        BufferedImage extended = new BufferedImage(toExtend.getWidth() * EXTEND_FACTOR, toExtend.getHeight() * EXTEND_FACTOR, BufferedImage.TYPE_INT_RGB);

        for (int x=0; x < toExtend.getWidth(); x++){
            for (int y=0; y < toExtend.getHeight(); y++){
                Color color = new Color(toExtend.getRGB(x,y));
                Graphics2D graphics = extended.createGraphics();
                graphics.setPaint(color);
                int circleDiameter = EXTEND_FACTOR - 2 * EXTEND_FACTOR_BOARDER;
                graphics.fillOval(x * EXTEND_FACTOR + EXTEND_FACTOR_BOARDER, y * EXTEND_FACTOR + EXTEND_FACTOR_BOARDER, circleDiameter, circleDiameter);
            }
        }

        return extended;
    }

    /**
     * Select closest color from available
     * @param avgColor {@link Color}
     * @return {@link Color}
     */
    private static Color getAdaptedColor(Color avgColor) {
        int minDiff = AVAILABLE_COLORS.stream().mapToInt(c -> getColorsDiffFactor(c, avgColor)).min().getAsInt();
        return AVAILABLE_COLORS.stream().filter(c -> minDiff == getColorsDiffFactor(c, avgColor)).findFirst().get();
    }

    /**
     * Calculates difference between colors in number representation
     * @param a {@link Color}
     * @param b {@link Color}
     * @return difference factor
     */
    private static int getColorsDiffFactor(Color a, Color b) {
        return Math.abs(a.getRed() - b.getRed()) +
            Math.abs(a.getGreen() - b.getGreen()) +
                Math.abs(a.getBlue() - b.getBlue());
    }

    /**
     * Calculates average color of whole image
     * @param img
     * @return average color of whole image
     */
    private static Color calculateAvgColor(BufferedImage img) {
        if (img == null) {
            return AVAILABLE_COLORS.get(0);
        } else {
            int sumR = 0;
            int sumG = 0;
            int sumB = 0;
            for (int x=0; x < img.getWidth(); x++){
                for (int y=0; y < img.getHeight(); y++){
                    Color pixelColor = new Color(img.getRGB(x, y));
                    sumR += pixelColor.getRed();
                    sumG += pixelColor.getGreen();
                    sumB += pixelColor.getBlue();
                }
            }
            int pixelCount = img.getWidth() * img.getHeight();
            return new Color(sumR / pixelCount, sumG / pixelCount, sumB / pixelCount);
        }

    }

    /**
     * Get sub image which represent one lego element
     * @param img original image
     * @param pixelSize size of one lego element in pixels
     * @param x x coordinate of lego element
     * @param y y coordinate of lego element
     * @return sub image which represent one lego element
     */
    private static BufferedImage getSubImage(BufferedImage img, int pixelSize, int x, int y) {
        int xStart = x * pixelSize;
        int yStart = y * pixelSize;
        if (xStart < img.getWidth() && yStart < img.getHeight()) {
            return img.getSubimage(xStart, yStart, Math.min(CANVAS_SIZE, img.getWidth() - xStart), Math.min(CANVAS_SIZE, img.getHeight() - yStart));
        } else {
            return null;
        }
    }


}
