package si.xlab.gaea.core.retrieve;


import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.retrieve.HTTPRetriever;
import gov.nasa.worldwind.retrieve.RetrievalPostProcessor;
import gov.nasa.worldwind.retrieve.Retriever;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWIO;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

/**
 * ImageCache takes care of downloading the HTTP-referenced images to the file cache,
 * and using them from the cache.
 */
public class ImageCache
{

    /**
     * Returns the cached file, synchrounously loading it if required.
     * If image exists on local filesystem or in cache, returns the local or cached filename.
     * If not, downloads from internet, stores as .dds and returns the cached filename.
     * If local file doesn't exist or download fails, throws an exception.
     * @param source: image filename or URL
     * @return local or cached filename of the image
     */
    public static String getImage(String source) throws Exception
    {
        if (source.startsWith("http://"))
        {
            File cacheFile;

            synchronized (ImageCache.class)
            { // too restrictive, but we don't really expect much parallel downloading here				
                String cacheFilename = url2CacheFilename(source);
                URL cacheFileURL = WorldWind.getDataFileStore().findFile(cacheFilename, false);

                if (null != cacheFileURL)
                {
                	//image is cached, so return the path to the local file
                	
                	// dataFileStore.findFile() returns an URL instead of simply the filename (now whose bright idea was that!?)
                    // we thus need to convert it back to URI, whose getPath() un-HTML-encodes the filename
                    return cacheFileURL.toURI().getPath();
                }

                // not yet cached, so download and save it NOW
                cacheFile = WorldWind.getDataFileStore().newFile(cacheFilename);
                DownloadPostProcessor postProcessor = new DownloadPostProcessor(
                        cacheFile, null);
                HTTPRetriever retriever = new HTTPRetriever(new URL(source),
                        postProcessor);

                retriever.setConnectTimeout(CONNECT_TIMEOUT);
                retriever.setReadTimeout(READ_TIMEOUT);

                retriever.call();
	        	
                if (!postProcessor.success)
                {
                    throw new IOException(postProcessor.errorMessage);
                }
            }

            return cacheFile.toString();
        }
        else
        {
            // local file, just checks whether it's absolute path or refers to a resource
            File file = new File(source);

            if (file.exists())
            {
                return source;
            }
			
            URL resource = ResourceRetriever.getResource("/" + source);
            if (null != resource)
            {
                return source;
            }
			
            throw new IOException("File does not exist: " + source);
        }
    }
	
    /**
     * Calls getImage(source), and returns fallback if it fails.
     * @param imageSource URL or file path of the image
     * @param fallback the default image used if imageSource is invalid
     * @return file path of the local or cached copy, or fallback
     */
    public static String getImageWithFallback(String source, String fallback)
    {
        String rv;

        try
        {
            rv = getImage(source);
        }
        catch (Exception e)
        {
            Logging.logger().warning(
                    "Cannot load " + source + ", default used instead ("
			+ fallback + ")");
            rv = fallback;
        }

        return rv;
    }	

    /**
     * Calls postProcessor when file is available, which may be after downloading it from the net.
     * If loading fails the postProcessor is never called.
     * @param source image filename or URL
     * @param postProcessor the custom function to be called when file is cached
     */
    public static void ensureImageCached(String source, Runnable postProcessor)
    {
        if (requiresDownloading(source))
        {
            synchronized (ImageCache.class)
            { // too restrictive, but we don't really expect much parallel downloading here				
                String cacheFilename = url2CacheFilename(source);
                File cacheFile = WorldWind.getDataFileStore().newFile(
                        cacheFilename);
	
                // create a DownloadPostProcessor that calls the passed postProcessor
                DownloadPostProcessor downloadPostProcessor = new DownloadPostProcessor(
                        cacheFile, postProcessor);
                URL url;

                try
                {
                    url = new URL(source);
                }
                catch (MalformedURLException e)
                {
                    Logging.logger().severe("Malformed URL: " + source);
                    return;
                }
                HTTPRetriever retriever = new HTTPRetriever(url,
                        downloadPostProcessor);

                retriever.setConnectTimeout(CONNECT_TIMEOUT);
                retriever.setReadTimeout(READ_TIMEOUT);
	        	
                // start asynchronous download
                WorldWind.getRetrievalService().runRetriever(retriever,
                        Double.MAX_VALUE);
            }
        }
        else
        {
            // already cached, start post-processor now
            postProcessor.run();
        }
    }
	
    /**
     * @param source image filename or URL
     * @return true if source refers to http and is not yet cached
     */
    public static boolean requiresDownloading(String source)
    {
        if (source.startsWith("http://"))
        {
            String cacheFilename = url2CacheFilename(source);
            URL cacheFileURL = WorldWind.getDataFileStore().findFile(
                    cacheFilename, false);

            return (null == cacheFileURL);
        }
        else
        { 
            return false;
        }
    }
	
    private static final int CONNECT_TIMEOUT = 10000,
            READ_TIMEOUT = 20000;
    private static final String CACHE_FORMAT = "png"; // we cache everything as png
    // (not dds as it only supports image sizes divisible by 4;
    // not same format as the downloaded file as it's easier this way
	
    private static String url2CacheFilename(String url)
    {
        String path = "images" + File.separator
                + url.substring("http://".length()) + "." + CACHE_FORMAT;

        path = path.replaceAll("[:*?<>|]", "_");
        path = path.replaceAll("/\\.\\.", ""); // fix the likes of /home/var/foo/bar/../bur/a.jpg
        return path;
    }
	
    /**
     * @author marjan
     * When file is downloaded, saves it to cache in CACHE_FORMAT
     * and calls the given customPostProcessor (if not null)
     */
    private static class DownloadPostProcessor implements RetrievalPostProcessor
    {
        private final File outfile;
        private final Runnable customPostProcessor;
        
        protected boolean success = false;
        protected String errorMessage = "Post-processor not yet called";
        
        public DownloadPostProcessor(File outfile, Runnable customPostProcessor)
        {
            this.outfile = outfile;
            this.customPostProcessor = customPostProcessor;
        }
        
        public ByteBuffer run(Retriever retriever)
        {
            try
            {
                if (!retriever.getState().equals(
                        Retriever.RETRIEVER_STATE_SUCCESSFUL))
                {
                    throw new IOException("Retriever state unsuccessful");
                }

                HTTPRetriever htr = (HTTPRetriever) retriever;

                if (htr.getResponseCode() != HttpURLConnection.HTTP_OK)
                { 
                    throw new IOException("Retriever response code not HTTP_OK");
                }
                
                ByteBuffer buffer = htr.getBuffer();
                String contentType = htr.getContentType();

                if (!contentType.contains("image"))
                { 
                    throw new IOException("invalid HTTP content type");
                }
	            
                BufferedImage image = ImageIO.read(
                        WWIO.getInputStreamFromByteBuffer(buffer));

                ImageIO.write(image, CACHE_FORMAT, outfile);

                if (customPostProcessor != null)
                {
                    customPostProcessor.run();
                }
	            
                success = true;
                errorMessage = null;
                return buffer;
            }
            catch (Exception e)
            {
                success = false;
                errorMessage = e.getMessage();
                return null;
            }
        	
        }
    }
	
}	

