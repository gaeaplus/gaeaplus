/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package si.xlab.gaea.core.retrieve;

import java.net.URL;

/**
 * This is needed so that resource retrieval process can be changed if needed.
 * For example, in a NetBeans-based application, a custom retriever is needed
 * that can access all NetBeans modules.
 * 
 * The default behaviour is OK if the whole application uses a single class loader. * 
 * 
 * To change behaviour:
 * - subclass this
 * - override getResource
 * - call ResourceRetriever.setDefaultRetriever
 * 
 * @author marjan
 */
public class ResourceRetriever
{
    private static ResourceRetriever defaultRetriever = null;
    
    public static URL getResource(String name)
    {
        return getDefaultRetriever().doGetResource(name);
    }

    public static synchronized void setDefaultRetriever(ResourceRetriever retriever)
    {
        defaultRetriever = retriever;
    }

    protected URL doGetResource(String name)
    {
        return ResourceRetriever.class.getResource(name);
    }
                        
    private static synchronized ResourceRetriever getDefaultRetriever()
    {
        if (defaultRetriever == null)
            defaultRetriever = new ResourceRetriever();
        return defaultRetriever;
    }    
}
