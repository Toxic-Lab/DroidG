/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2006 Phex Development Group
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 *  Created on 12.12.2005
 *  --- CVS Information ---
 *  $Id$
 */
package phex.xml.sax.rules;

import org.xml.sax.SAXException;

import phex.xml.sax.DElement;
import phex.xml.sax.PhexXmlSaxWriter;

public class DSearchRule implements DElement
{
    public static final String ELEMENT_NAME = "search-rule";
    private String name;
    private String description;
    private String notes;
    private String id;
    
    private boolean hasPermanentlyEnabled;
    private boolean isPermanentlyEnabled;
    
    private DAndConcatCondition andConcatCondition;
    private DConsequencesList consequencesList;

    public boolean isPermanentlyEnabled()
    {
        return isPermanentlyEnabled;
    }

    public void setPermanentlyEnabled( boolean isPermanentlyEnabled )
    {
        this.isPermanentlyEnabled = isPermanentlyEnabled;
        hasPermanentlyEnabled = true;
    }

    public boolean isHasPermanentlyEnabled()
    {
        return hasPermanentlyEnabled;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }
    
    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    public String getNotes() 
    {
    	return notes;
    }
    
    public void setNotes(String notes) 
    {
    	this.notes = notes;
    }
    
    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public DAndConcatCondition getAndConcatCondition()
    {
        return andConcatCondition;
    }

    public void setAndConcatCondition( DAndConcatCondition ruleCondition )
    {
        this.andConcatCondition = ruleCondition;
    }

    public DConsequencesList getConsequencesList()
    {
        return consequencesList;
    }
    
    public void setConsequencesList( DConsequencesList consequencesList )
    {
        this.consequencesList = consequencesList;
    }
    
    public void serialize( PhexXmlSaxWriter writer ) throws SAXException
    {
        writer.startElm( ELEMENT_NAME, null );
        
        if( name != null )
        {
            writer.startElm( "name", null );
            writer.elmText( name );
            writer.endElm( "name" );
        }
        
        if( description != null )
        {
            writer.startElm( "description", null );
            writer.elmText( description );
            writer.endElm( "description" );
        }
        
        if( id != null )
        {
            writer.startElm( "id", null );
            writer.elmText( id );
            writer.endElm( "id" );
        }

        if( notes != null )
        {
            writer.startElm( "notes", null );
            writer.elmText( notes );
            writer.endElm( "notes" );
        }
        if( hasPermanentlyEnabled )
        {
            writer.startElm( "permanently-enabled", null );
            writer.elmBol( isPermanentlyEnabled );
            writer.endElm( "permanently-enabled" );
        }
        
        if ( andConcatCondition != null )
        {
            andConcatCondition.serialize( writer );
        }
        
        if ( consequencesList != null )
        {
            consequencesList.serialize(writer);
        }
        
        writer.endElm( ELEMENT_NAME );
    }
}
