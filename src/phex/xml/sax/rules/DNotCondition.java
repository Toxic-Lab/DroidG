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

import phex.rules.condition.Condition;
import phex.rules.condition.NotCondition;
import phex.xml.sax.PhexXmlSaxWriter;

public class DNotCondition implements DCondition
{
    static public final String ELEMENT_NAME = "not-condition";

    protected DCondition dCondition;

    public DCondition getDCondition()
    {
        return dCondition;
    }

    public void setDCondition( DCondition condition )
    {
        this.dCondition = condition;
    }

    public void serialize( PhexXmlSaxWriter writer ) throws SAXException
    {
        writer.startElm( ELEMENT_NAME, null );

        if ( dCondition != null )
        {
            dCondition.serialize( writer );
        }

        writer.endElm( ELEMENT_NAME );
    }

    public Condition createCondition()
    {
        Condition condition = dCondition.createCondition();
        NotCondition cond = new NotCondition( condition );
        return cond;
    }
}
