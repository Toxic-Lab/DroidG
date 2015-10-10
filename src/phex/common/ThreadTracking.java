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




/**
 * This class assists in tracking Thread use.
 */
public class ThreadTracking
{
    public static ThreadGroup threadPoolGroup;
    public static ThreadGroup rootThreadGroup;

    private static ThreadGroup systemGroup;

    public static void initialize()
    {
        // we want the system thread group
        systemGroup = Thread.currentThread().getThreadGroup();
        while ( systemGroup.getParent() != null )
        {// not the system thread group.. go up one step
            systemGroup = systemGroup.getParent();
        }
        
        prepareUncaughtExceptionHandler();
        
        // TODO in the future all thread creation should go through this class
        // to consistently use uncaught exception handling.
        rootThreadGroup = new PhexThreadGroup( "PhexRoot" );
        threadPoolGroup = new PhexThreadGroup( "PhexThreadPool" );
    }
    
    private static void prepareUncaughtExceptionHandler()
    {
    	Thread.UncaughtExceptionHandler ucExpHandler = 
    		new Thread.UncaughtExceptionHandler()
    	{
			public void uncaughtException(Thread thread, Throwable throwable)
			{
//                NLogger.error( ThreadTracking.class, 
//                    "Uncaught exception: " + throwable.getMessage() + " in Thread: " 
//                    + thread.getName(), throwable );
			}
    		
    	};
    	Thread.setDefaultUncaughtExceptionHandler( ucExpHandler );
    }

    private static class PhexThreadGroup extends ThreadGroup
    {
        public PhexThreadGroup( String name )
        {
            super( systemGroup, name );
            
        }
        
        @Override
        public void uncaughtException(Thread t, Throwable e)
        {
            super.uncaughtException(t, e);
//            NLogger.error( PhexThreadGroup.class, 
//                "Uncaught exception: " + e.getMessage() + " in Thread: " 
//                + t.getName(), e );
        }
    }

    /*public static void dumpFullThreadLog()
    {
        if ( !Logger.isLevelLogged( Logger.FINEST ) )
        {
            return;
        }
        int count = systemGroup.activeCount();
        Thread[] threads = new Thread[ count ];
        count = systemGroup.enumerate( threads, true );
        Logger.logMessage( Logger.FINEST, Logger.GLOBAL,
            "------------------- Start Full Thread Dump -------------------" );
        for ( int i = 0; i < count; i++ )
        {
            Logger.logMessage( Logger.FINEST, Logger.GLOBAL, threads[ i ].toString() );
        }
        Logger.logMessage( Logger.FINEST, Logger.GLOBAL,
            "-------------------- End Full Thread Dump --------------------" );
    }*/
}