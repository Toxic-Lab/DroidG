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
package phex.common;

//Modified version of org.mortbay.util.MultiException
//Copyright by Mort Bay Consulting Pty. Ltd.

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MultipleException extends Exception
{
    private List<Throwable> list;
    
    public MultipleException()
    {
        super( "Multiple exceptions" );
    }

    public void add( Throwable th )
    {
        if ( list == null )
        {
            list = new ArrayList<Throwable>();
        }
        list.add( th );
    }

    public int size()
    {
        if ( list == null )
        {
            return 0;
        }
        return list.size();
    }
    
    public List<Throwable> getThrowables()
    {
        if ( list == null )
        {
            return Collections.emptyList();
        }
        return list;
    }
    
    public Throwable getThrowable(int i)
    {
        if ( list == null )
        {
            throw new IndexOutOfBoundsException();
        }
        return list.get( i );
    }

    public void throwPossibleExp() throws Exception
    {
        if ( list == null )
        {
            return;
        }
        if ( list.size() == 1 )
        {
            Throwable th = list.get( 0 );
            if ( th instanceof Error )
            {
                throw (Error)th;
            }
            if ( th instanceof Exception )
            {
                throw (Exception)th;
            }
        }
        throw this;
    }
    
    public void throwPossibleExpAsRuntime()
        throws Error
    {
        if ( list == null )
        {
            return;
        }
        if ( list.size() == 1 )
        {
            Throwable th = list.get( 0 );
            if ( th instanceof Error )
            {
                throw (Error)th;
            }
            if ( th instanceof RuntimeException )
            {
                throw (RuntimeException)th;
            }
            throw new RuntimeException( th );
        }
        throw new RuntimeException( this );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        if ( list == null )
        {
            return "phex.common.MultipleException[]";
        }
        return "phex.common.MultipleException" + list.toString();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void printStackTrace()
    {
        super.printStackTrace();
        if ( list == null )
        {
            return;
        }
        for (Throwable th : list )
        {
            th.printStackTrace();
        }
    }
   
    /**
     * {@inheritDoc}
     */
    @Override
    public void printStackTrace(PrintStream out)
    {
        super.printStackTrace(out);
        if ( list == null )
        {
            return;
        }
        for ( Throwable th : list )
        {
            th.printStackTrace( out );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void printStackTrace(PrintWriter out)
    {
        super.printStackTrace(out);
        if ( list == null )
        {
            return;
        }
        for ( Throwable th : list )
        {
            th.printStackTrace( out );
        }
    }
}
