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

//import phex.common.log.//NLogger;


/**
 * Simple implementation of a life cycle.
 */
public abstract class AbstractLifeCycle implements LifeCycle
{
    public enum Status
    {
        STOPPED, STARTING, STARTED, STOPPING, FAILED
    }

    private Status status = Status.STOPPED;

    
    protected void doStart() throws Exception
    {
    }

    protected void doStop() throws Exception
    {
    }

    public final void start() throws Exception
    {
        try
        {
            if ( status == Status.STARTED )
                return;
            status = Status.STARTING;
            doStart();
            //NLogger.debug( getClass(), "lifecycle started" );
            status = Status.STARTED;
        }
        catch ( Exception exp )
        {
            //NLogger.error( getClass(), exp );
            status = Status.FAILED;
            throw exp;
        }
        catch ( Error err )
        {
            //NLogger.error( getClass(), err );
            status = Status.FAILED;
            throw err;
        }
    }

    public final void stop() throws Exception
    {
        try
        {
            if ( status == Status.STOPPING || status == Status.STOPPED )
                return;
            status = Status.STOPPING;
            doStop();
            //NLogger.debug( getClass(), "lifecycle stopped" );
            status = Status.STOPPED;
        }
        catch ( Exception exp )
        {
            //NLogger.error( getClass(), exp );
            status = Status.FAILED;
            throw exp;
        }
        catch ( Error err )
        {
            //NLogger.error( getClass(), err );
            status = Status.FAILED;
            throw err;
        }
    }

    public boolean isRunning()
    {
        return status == Status.STARTED || status == Status.STARTING;
    }

    public boolean isStarted()
    {
        return status == Status.STARTED;
    }

    public boolean isStarting()
    {
        return status == Status.STARTING;
    }

    public boolean isStopping()
    {
        return status == Status.STOPPING;
    }

    public boolean isStopped()
    {
        return status == Status.STOPPED;
    }

    public boolean isFailed()
    {
        return status == Status.FAILED;
    }
}