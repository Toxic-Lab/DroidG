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
 *  Created on 20.11.2006
 *  --- SVN Information ---
 *  $Id$
 */
package phex.xml.sax.security;

import org.xml.sax.SAXException;

import phex.xml.sax.DElement;
import phex.xml.sax.PhexXmlSaxWriter;

public abstract class DSecurityRule implements DElement
{
    private String description;
    
    private boolean hasDenyingRule;
    private boolean isDenyingRule;
    
    private boolean hasDisabled;
    private boolean isDisabled;
    
    private boolean hasSystemRule;
    private boolean isSystemRule;
    
    private boolean hasTriggerCount;
    private int triggerCount;
    
    private boolean hasExpiryDate;
    private long expiryDate;
    
    private boolean hasDeletedOnExpiry;
    private boolean isDeletedOnExpiry;

    /**
     * @return the description
     */
    public String getDescription()
    {
        return description;
    }
    /**
     * @param description the description to set
     */
    public void setDescription( String description )
    {
        this.description = description;
    }
    /**
     * @return the expiryDate
     */
    public long getExpiryDate()
    {
        return expiryDate;
    }
    /**
     * @param expiryDate the expiryDate to set
     */
    public void setExpiryDate( long expiryDate )
    {
        hasExpiryDate = true;
        this.expiryDate = expiryDate;
    }
    /**
     * @return the isDeletedOnExpiry
     */
    public boolean isDeletedOnExpiry()
    {
        return isDeletedOnExpiry;
    }
    /**
     * @param isDeletedOnExpiry the isDeletedOnExpiry to set
     */
    public void setDeletedOnExpiry( boolean isDeletedOnExpiry )
    {
        hasDeletedOnExpiry = true;
        this.isDeletedOnExpiry = isDeletedOnExpiry;
    }
    /**
     * @return the isDenyingRule
     */
    public boolean isDenyingRule()
    {
        return isDenyingRule;
    }
    /**
     * @param isDenyingRule the isDenyingRule to set
     */
    public void setDenyingRule( boolean isDenyingRule )
    {
        hasDenyingRule = true;
        this.isDenyingRule = isDenyingRule;
    }
    public boolean hasDenyingRule()
    {
        return hasDenyingRule;
    }
    /**
     * @return the isDisabled
     */
    public boolean isDisabled()
    {
        return isDisabled;
    }
    /**
     * @param isDisabled the isDisabled to set
     */
    public void setDisabled( boolean isDisabled )
    {
        hasDisabled = true;
        this.isDisabled = isDisabled;
    }
    /**
     * @return the isSystemRule
     */
    public boolean isSystemRule()
    {
        return isSystemRule;
    }
    /**
     * @param isSystemRule the isSystemRule to set
     */
    public void setSystemRule( boolean isSystemRule )
    {
        hasSystemRule = true;
        this.isSystemRule = isSystemRule;
    }
    /**
     * @return the triggerCount
     */
    public int getTriggerCount()
    {
        return triggerCount;
    }
    /**
     * @param triggerCount the triggerCount to set
     */
    public void setTriggerCount( int triggerCount )
    {
        hasTriggerCount = true;
        this.triggerCount = triggerCount;
    }
    
    protected void serializeSecurityRuleElements( PhexXmlSaxWriter writer )
        throws SAXException
    {
        if( description != null )
        {
            writer.startElm( "description", null );
            writer.elmText( description );
            writer.endElm( "description" );
        }
        if( hasDenyingRule )
        {
            writer.startElm( "isDenyingRule", null );
            writer.elmBol( isDenyingRule );
            writer.endElm( "isDenyingRule" );
        }
        if( hasDisabled )
        {
            writer.startElm( "isDisabled", null );
            writer.elmBol( isDisabled );
            writer.endElm( "isDisabled" );
        }
        if( hasSystemRule )
        {
            writer.startElm( "isSystemRule", null );
            writer.elmBol( isSystemRule );
            writer.endElm( "isSystemRule" );
        }
        if( hasTriggerCount )
        {
            writer.startElm( "triggerCount", null );
            writer.elmInt( triggerCount );
            writer.endElm( "triggerCount" );
        }
        if( hasExpiryDate )
        {
            writer.startElm( "expiryDate", null );
            writer.elmLong( expiryDate );
            writer.endElm( "expiryDate" );
        }
        if( hasDeletedOnExpiry )
        {
            writer.startElm( "isDeletedOnExpiry", null );
            writer.elmBol( isDeletedOnExpiry );
            writer.endElm( "isDeletedOnExpiry" );
        }
    }
}