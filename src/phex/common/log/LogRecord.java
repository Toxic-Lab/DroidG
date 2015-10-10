/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2008 Phex Development Group
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
package phex.common.log;

/**
 *
 */
public class LogRecord
{
    private long timestamp;
    private Object owner;
    private String message;
    
    /**
     * @param message
     */
    public LogRecord(Object owner, String message)
    {
        if ( owner == null || message == null )
        {
            throw new NullPointerException();
        }
        this.owner = owner;
        timestamp = System.currentTimeMillis();
        this.message = message;
    }
    
    public long getTimestamp()
    {
        return timestamp;
    }
    
    public void setTimestamp(long date)
    {
        this.timestamp = date;
    }
    
    public String getMessage()
    {
        return message;
    }
    
    public void setMessage(String message)
    {
        this.message = message;
    }
    
    public int getSize()
    {
        return message.length() + 8;
    }
    
    public Object getOwner()
    {
        return owner;
    }
}