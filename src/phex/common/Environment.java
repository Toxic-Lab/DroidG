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

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;

import org.apache.commons.lang.SystemUtils;

import phex.event.UserMessageListener;
import phex.utils.SystemProperties;

/**
 * This class can not be implemented as a manager since manager initialization
 * routines relay on the availability of this class during initialization.
 */
public class Environment
{
    //private static final Logger logger = LoggerFactory.getLogger( Environment.class );
    
    private UserMessageListener userMessageListener;

    /**
     * The TimerService is a single thread that will handle multiple TimerTask.
     * Therefore each task has to make sure it is not performing a long blocking
     * operation.
     */
    private final Timer timerService;
    
    private final JThreadPool threadPool;

    private Environment()
    {
        timerService = new Timer( true );
        threadPool = new JThreadPool();
    }

    static private class Holder
    {
        static protected final Environment environment = new Environment();
    }

    static public Environment getInstance()
    {
        return Environment.Holder.environment;
    }

    /**
     * Returns the File representing the complete path to the configuration file
     * with the given configFileName.
     * @param configFileName the name of the config file to determine the complete
     *        path for.
     * @return the File representing the complete path to the configuration file
     *         with the given configFileName.
     */
    public File getPhexConfigFile( String configFileName )
    {
        return new File( SystemProperties.getPhexConfigRoot(), configFileName );
    }

    /**
     * Schedules the specified task for repeated fixed-delay execution,
     * beginning after the specified delay. Subsequent executions take place at
     * approximately regular intervals separated by the specified period.
     *
     * The TimerService is a single thread that will handle multiple TimerTask.
     * Therefore each task has to make sure it is not performing a long blocking
     * operation.
     *
     * @param task The task to be scheduled.
     * @param delay The delay in milliseconds before task is to be executed.
     * @param period The time in milliseconds between successive task executions.
     */
    public void scheduleTimerTask(TimerTask task, long delay, long period )
    {
        timerService.schedule( task, delay, period );
    }
    
    /**
     * Schedules the specified task for execution after the specified delay.
     * 
     * The TimerService is a single thread that will handle multiple TimerTask.
     * Therefore each task has to make sure it is not performing a long blocking
     * operation.
     *
     * @param task The task to be scheduled.
     * @param delay The delay in milliseconds before task is to be executed.
     */
    public void scheduleTimerTask(TimerTask task, long delay )
    {
        timerService.schedule( task, delay );
    }
    
    /**
     * Executes the Runnable on the shared Phex thread pool
     * @param runnable the runnable to execute
     * @param name the name.
     */
    public void executeOnThreadPool( Runnable runnable, String name )
    {
        threadPool.executeNamed( runnable, name );
    }
    
    public Executor getThreadPool()
    {
        return threadPool.getThreadPool();
    }

    /**
     * Returns true if the system is a ultrapeer os, false otherwise.
     * @return true if the system is a ultrapeer os, false otherwise.
     */
    public boolean isUltrapeerOS()
    {
        // accept all none windows systems (MacOSX, Unix...) or Windows 2000 or XP.
//        return !SystemUtils.IS_OS_WINDOWS || 
//            SystemUtils.IS_OS_WINDOWS_2000 || SystemUtils.IS_OS_WINDOWS_XP;
    	return false; //do not expecting android to become an ultrapeer :)
    }
    
    public void setUserMessageListener( UserMessageListener listener )
    {
        userMessageListener = listener;
    }
    
    public void fireDisplayUserMessage( String userMessageId )
    {
        // if initialized
        if ( userMessageListener != null )
        {
            userMessageListener.displayUserMessage( userMessageId, null );
        }
    }
    
    public void fireDisplayUserMessage( String userMessageId, String[] args )
    {
        // if initialized
        if ( userMessageListener != null )
        {
            userMessageListener.displayUserMessage( userMessageId, args );
        }
    }
}