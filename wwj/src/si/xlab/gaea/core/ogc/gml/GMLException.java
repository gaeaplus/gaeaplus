package si.xlab.gaea.core.ogc.gml;

public class GMLException extends Exception
{
    private static final long serialVersionUID = 1L;

    public GMLException(String message)
    {
    	super(message);
    }
	
    public GMLException(String message, Throwable cause)
    {
        super(message + detailMessage(cause), cause);
    }
    
    private static String detailMessage(Throwable cause)
    {
    	StringBuilder ret = new StringBuilder();
    	while (cause != null) {
    		if (cause.getMessage() != null) 
    		{
    			ret.append("; ");
    			ret.append(cause.getMessage());
    		}
            if (cause == cause.getCause())
                cause = null;
            else
                cause = cause.getCause();
    	}
    	return ret.toString();
    }
}
