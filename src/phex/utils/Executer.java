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
 */
package phex.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import phex.common.ThreadTracking;


public class Executer implements Runnable
{
    File file;
    URL url;
    String command;

    /*
     * Prepare to execute the given command on the given file
     * by first substituting the file into the command.
     * Returns immediately.
     */
    public Executer ( File file, String command )
    {
        this.url = null;
        this.file = file;
        this.command = command;
    }

    /*
     * Invoke this URL using the given command
     */
    public Executer ( URL url, String command )
    {
        this.url = url;
        this.file = null;
        this.command = command;
    }

    /*
     * Execute the command without any substitution
     */
    public Executer ( String command )
    {
        this.url = null;
        this.file = null;
        this.command = command;
    }
    public void run ()
    {
        if ( command == null ) return;
        // Do this before keyword expansion so that the grouping/splitting is done properly
        String[] commands = parseCommandLine ( command );
        System.out.println( commands ); // nxf

        StringBuffer absFilename = null;
        if ( file != null )
        {
            // The replaceAll operator eats backslashes, the stupid thing
            absFilename = new StringBuffer(file.getAbsolutePath().length() + 20);
            try {
                for (int offset=0; offset < file.getAbsolutePath().length() ; offset++)
                {
                    char currentChar = file.getAbsolutePath().charAt(offset);
                    if ( currentChar == '\\' )
                        absFilename.append("\\\\");
                    else
                        absFilename.append( currentChar );
                }
            }
            catch (IndexOutOfBoundsException ex)
            {
            }
        }
        if ( file != null || url != null ) // this is just an optimisation
        {
            for (int i = 0; i < commands.length ; i++)
            {
                if ( file != null )
                {
                    commands[i] = commands[i].replaceAll( "%s", absFilename.toString() );
                    try {
                        commands[i] = commands[i].replaceAll( "%u", file.toURI().toURL().toExternalForm() );
                    } catch (Exception ex) {} // URL cannot be malformed, nothing to handle
                } 
                if ( url != null )
                {
                    commands[i] = commands[i].replaceAll( "%u", url.toExternalForm() );
                }
            }
        }
        try {
            if ( commands.length > 0 )
            {
            	//NLogger.error( Executer.class, "About to invoke " + Arrays.asList( commands ) );
                Runner r = new Runner(commands); 
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            //NLogger.error( Executer.class, "Cannot invoke previewer." );
            if ( commands != null )
            {
            	//NLogger.error( Executer.class,
                 //   "Actual command was '" + commands + "'");
            }
        }
    }

    public static String getViewCommand ( String suffix )
    {
        return getViewCommand ( suffix , true );
    }

    public static String getViewCommand ( String suffix, boolean allowFallback)
    {
        String cmd = null;
//        if ( allowFallback )
//            cmd = ServiceManager.sCfg.fallbackPreviewMethod;
//        // Cycle through the keys seeing if any is an RE-match for this suffix
//        Set mySet = ServiceManager.sCfg.previewMethod.keySet();
//        String key = matches( mySet.iterator() , suffix );
//        if ( key != null ) 
//        {
//            cmd = (String) ServiceManager.sCfg.previewMethod.get ( key );
//        }
        return cmd;
    }

    /*
     * Iterate through a group of regexes, and return a
     * matching regex, or null if none match.
     */
    public static String matches(Iterator regexes, String name)
    {
        StringBuffer regex;
        while (regexes.hasNext())
        {
            String entry = (String) regexes.next();
            if ( entry.startsWith("^") && entry.endsWith("$") ) // a 'true' RE
            {
                regex = new StringBuffer(entry);
            } else {
                regex = new StringBuffer();
                regex.append("^.*"); // any character string may precede it
                regex.append(entry);
                regex.append("$"); // but it must be at the end of the filename
            }

            Pattern p = Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(name);
            if ( m.matches() )
                return entry;
        }
        return null;
    }

    /*
     * Splits the given string at whitespace, respecting
     * quoted strings
     */
    public static String[] parseCommandLine ( String commandLine )
    {
        // Define a pattern which will split on spaces but respect quotes
        Pattern p = Pattern.compile("(\".*?\"|\\S+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(commandLine);
        List tokens = new LinkedList();
        String[] result;
        int counter = 0;
        while ( m.find() )
        {
            tokens.add( m.group(1) );
            counter++;
        }
        result = new String[counter];
        Iterator it = tokens.iterator();
        String quoteString = "\"";
        for (int i = 0; i < counter ; i++)
        {
            String field = (String) it.next();
            if ( field.startsWith("\"") || true ) // ie: unix
            {
                result[i] = field;
            } else
            {
                result[i] = new String (quoteString + field + quoteString);
            }
        }
        return result;
    }


    private class Runner
    {
        public int result;

        // Suck all the output from the given process and return when it's complete
        public Runner ( String commandLine )
        {
            go ( parseCommandLine (commandLine) );
        }

        public Runner ( String[] commandLine )
        {
            go ( commandLine );
        }

        public void go ( String[] commands )
        {
            Process proc = null;
            try
            {
                proc = java.lang.Runtime.getRuntime().exec( commands );
            }
            catch ( IOException ex )
            {
                ex.printStackTrace();
                return;
            }
            //prepare buffers for process output and error streams
            StringBuffer err=new StringBuffer();
            StringBuffer out=new StringBuffer();    
            try
                {
                //create thread for reading inputStream (process' stdout)
                StreamReaderThread outThread=new StreamReaderThread(proc.getInputStream(),out);
                //create thread for reading errorStream (process' stderr)
                StreamReaderThread errThread=new StreamReaderThread(proc.getErrorStream(),err);
                //start both threads
                outThread.start();
                errThread.start();
                //wait for process to end
                result=proc.waitFor();
                //finish reading whatever's left in the buffers
                outThread.join();
                errThread.join();

                if (result!=0) 
                {
                    System.out.println("Process " +  " returned non-zero value:" + result);
                    System.out.println("Process output:\n" + out.toString());
                    System.out.println("Process error:\n" + err.toString());
                } else {
                    System.out.println("Process " +  " executed successfully");
                    System.out.println("Process output:\n" + out.toString());
                    System.out.println("Process error:\n" + err.toString());
                }
            }
            catch (Exception e)
            {
                System.out.println("Error executing. ");
                e.printStackTrace();
                //throw e;
            }
        }

 
        private String[] parseCommandLineOld ( String commandLine )
        {
            Reader r = new StringReader ( commandLine );
            StreamTokenizer t = new StreamTokenizer( r );
            t.wordChars(0x0000, 0x00FF); 
            t.quoteChar('"');
            t.whitespaceChars(' ', ' ');

            List tokens = new LinkedList();
            String[] result;
            int counter = 0;
            try
            {
                while ( t.nextToken() != StreamTokenizer.TT_EOF )
                {
                    tokens.add( t.sval );
                    counter++;
                }
            }
            catch ( IOException ex )
            {
                ex.printStackTrace();
            }

            result = new String[counter];
            Iterator it = tokens.iterator();
            String quoteString = "\"";
            for (int i = 0; i < counter ; i++)
            {
                result[i] = quoteString + (String) it.next() + quoteString;
            }
            return result;
        }

        public class StreamReaderThread extends Thread
        {
            StringBuffer mOut;
            InputStreamReader mIn;
            
            public StreamReaderThread(InputStream in, StringBuffer out)
            {
                super( ThreadTracking.rootThreadGroup, "StreamReaderThread" );
                mOut=out;
                mIn=new InputStreamReader(in);
            }
                
            public void run()
            {
                int ch;
                try
                {
                    while(-1 != (ch=mIn.read()))
                        mOut.append((char)ch);
                }
                catch (Exception e)
                {
                    mOut.append("\nRead error:" + e.getMessage());
                }
            }
        }
    }
}
