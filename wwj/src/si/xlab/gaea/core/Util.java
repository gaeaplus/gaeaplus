/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package si.xlab.gaea.core;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Various helper functions.
 *
 * @author marjan
 */
public class Util
{
    /**
     * Scales the image to the given size. The returned image has pixel type BufferedImage.TYPE_INT_ARGB
     * @param src source image
     * @param destWidth width to scale to
     * @param destHeight height to scale to
     * @return the scaled image
     */
    public static BufferedImage scaleImage(BufferedImage src, int destWidth, int destHeight)
    {
        BufferedImage dest = new BufferedImage(destWidth, destHeight,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dest.createGraphics();
        AffineTransform at = AffineTransform.getScaleInstance(
                (double) destWidth / src.getWidth(),
                (double) destHeight / src.getHeight());

        g.drawRenderedImage(src, at);
        return dest;
    }
  
    /**
     * Opens the URL in external browser.
     * 
     * @param url
     * @throws IOException 
     */
    public static void openURL(String url) throws IOException
    {
        String osName = System.getProperty("os.name");

        try
        {
            if (osName.startsWith("Mac OS"))
            {
                Class<?> fileMgr = Class.forName("com.apple.eio.FileManager");
                Method openURL = fileMgr.getDeclaredMethod("openURL",
                        new Class[]
                        {
                            String.class
                        });

                openURL.invoke(null, new Object[]
                        {
                            url
                        });
            } else if (osName.startsWith("Windows"))
            {
                Runtime.getRuntime().exec(
                        "rundll32 url.dll,FileProtocolHandler " + url);
            } else
            { // assume Unix or Linux
                String[] browsers =
                {
                    "firefox", "opera", "konqueror", "epiphany", "mozilla",
                    "netscape", "dillo"
                };
                String browser = null;

                for (int count = 0; count < browsers.length && browser == null; count++)
                {
                    String[] whichCmd = new String[]
                    {
                        "which", browsers[count]
                    };

                    if (Runtime.getRuntime().exec(whichCmd).waitFor() == 0)
                    {
                        browser = browsers[count];
                    }
                }

                if (browser == null)
                {
                    throw new IOException("Could not find any web browser");
                } else
                {
                    Runtime.getRuntime().exec(new String[]
                            {
                                browser, url
                            });
                }
            }
        } catch (Exception e)
        {
            throw new IOException(
                    "Error launching browser at URL " + url + "\n"
                    + e.getMessage());
        }
    }
    
}
