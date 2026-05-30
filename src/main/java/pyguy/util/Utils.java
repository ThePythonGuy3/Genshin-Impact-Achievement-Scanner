package pyguy.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;

public class Utils
{
    // SOURCE: https://www.baeldung.com/java-levenshtein-distance
    public static int LevenshteinDistance(String x, String y)
    {
        int[][] dp = new int[x.length() + 1][y.length() + 1];

        for (int i = 0; i <= x.length(); i++)
        {
            for (int j = 0; j <= y.length(); j++)
            {
                if (i == 0)
                    dp[i][j] = j;
                else if (j == 0)
                    dp[i][j] = i;
                else
                {
                    dp[i][j] = Min(
                        dp[i - 1][j - 1] + CostOfSubstitution(x.charAt(i - 1), y.charAt(j - 1)),
                        dp[i - 1][j] + 1,
                        dp[i][j - 1] + 1
                    );
                }
            }
        }

        return dp[x.length()][y.length()];
    }

    private static int CostOfSubstitution(char a, char b)
    {
        return a == b ? 0 : 1;
    }

    private static int Min(int... numbers)
    {
        return Arrays.stream(numbers).min().orElse(Integer.MAX_VALUE);
    }

    private static BufferedImage Downscale(BufferedImage src, int targetW, int targetH) {
        BufferedImage img = src;

        int w = img.getWidth();
        int h = img.getHeight();

        while (w / 2 >= targetW && h / 2 >= targetH) {
            w /= 2;
            h /= 2;

            BufferedImage tmp = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = tmp.createGraphics();

            g2d.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR
            );
            g2d.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY
            );

            g2d.drawImage(img, 0, 0, w, h, null);
            g2d.dispose();

            img = tmp;
        }

        return img;
    }

    public static BufferedImage SizeToFit(BufferedImage bufferedImage, int w, int h)
    {
        bufferedImage = Downscale(bufferedImage, w, h);

        double scale = Math.min(
            (double) w / bufferedImage.getWidth(),
            (double) h / bufferedImage.getHeight()
        );

        int newWidth  = (int) Math.round(bufferedImage.getWidth()  * scale);
        int newHeight = (int) Math.round(bufferedImage.getHeight() * scale);

        BufferedImage output = new BufferedImage(
            w,
            h,
            bufferedImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : bufferedImage.getType()
        );

        Graphics2D g2d = output.createGraphics();

        g2d.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BILINEAR
        );

        g2d.setRenderingHint(
            RenderingHints.KEY_RENDERING,
            RenderingHints.VALUE_RENDER_QUALITY
        );

        g2d.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_OFF
        );

        g2d.setRenderingHint(
            RenderingHints.KEY_ALPHA_INTERPOLATION,
            RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY
        );

        g2d.drawImage(bufferedImage, (w - newWidth) / 2, (h - newHeight) / 2, newWidth, newHeight, null);
        g2d.dispose();

        return output;
    }
}
