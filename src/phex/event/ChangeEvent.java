/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2007 Phex Development Group
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
 *  --- SVN Information ---
 *  $Id$
 */
package phex.event;

/**
 * Generic change event class holding a source, the old value and the new value.
 * To indication what has changed the event topic of the event service should be
 * used. 
 */
public class ChangeEvent
{
    private final Object source;
    private final Object oldValue;
    private final Object newValue;
    
    public ChangeEvent(Object source, Object oldValue, Object newValue)
    {
        super();
        this.source = source;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public Object getSource()
    {
        return source;
    }

    public Object getOldValue()
    {
        return oldValue;
    }

    public Object getNewValue()
    {
        return newValue;
    }
}
