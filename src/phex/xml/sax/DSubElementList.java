package phex.xml.sax;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.xml.sax.SAXException;

public class DSubElementList<E extends DElement> implements DElement
{
    private String elementName;
    private List<E> subElementList;
    
    /**
     * Creates an unnamed subelement list, without surounding tags.
     */
    public DSubElementList( )
    {
        elementName = null;
    }
    
    /**
     * Creates an named subelement list, with surounding tags.
     */
    public DSubElementList( String name )
    {
        elementName = name;
    }

    public List<E> getSubElementList()
    {
        if ( subElementList == null )
        {
            subElementList = new ArrayList<E>();
        }
        return subElementList;
    }

    public void serialize( PhexXmlSaxWriter writer )
        throws SAXException
    {
        if ( elementName != null )
        {
            writer.startElm( elementName, null );
        }

        Iterator<E> iterator = subElementList.iterator();
        while ( iterator.hasNext() )
        {
            E subElement = iterator.next();
            subElement.serialize( writer );
        }

        if ( elementName != null )
        {
            writer.endElm( elementName );
        }
    }
}
