package si.xlab.gaea.core.ogc.kml;

public class KMLParserException extends Exception
{
    private static final long serialVersionUID = 1L;
	
    public KMLParserException()
    {
        super();
    }
	
    public KMLParserException(String s)
    {
        super(s);
    }
    
    public KMLParserException(String message, Throwable cause)
    {
        super(message, cause);
    }
}

