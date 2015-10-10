// Embedding a jython interpreter.

package phex.utils; 

import java.util.Properties;
import java.io.*;

public class JythonInterpreter {
   // protected InteractiveConsole interp;

    public JythonInterpreter() {
//        if (System.getProperty("python.home") == null) {
//            System.setProperty("python.home", "");
//        }
//        InteractiveConsole.initialize(System.getProperties(),
//                                      null, new String[0]);
//        interp = new InteractiveConsole();
    }

    public static void main(String[] args) {
//        JythonInterpreter con = new JythonInterpreter();
//        con.startConsole();
    }

    public void startConsole() {
//        // System.out.println("Hello");
//	// First add some info as __doc__ string
//	interp.push("__doc__ = 'The Phex Python interpreter. To get a list of available commands, please call help().'");
//	// Imports
//        interp.push("from phex.servent import Servent");
//        interp.push("from phex.net.server import OIOServer");
//	interp.push("from phex.gui.common import GUIRegistry");
//	interp.push("from java.awt import GraphicsEnvironment"); 
//	interp.push("from java.lang import System"); 
//        // offer a servent and a server object for controlling Phex
//        interp.push("servent = Servent.getInstance()");
//        interp.push("server = OIOServer( servent )");
//	// A help action
//	interp.push("help = dir");
//	// A quit action. 
//	// interp.push("quit = GUIRegistry.getInstance().getGlobalAction( GUIRegistry.EXIT_PHEX_ACTION).shutdown");
//	// interp.push("quit = servent.stop");
//	// interp.push("System.exit( True )");
//	interp.push("def quit():");
//	interp.push(" if not GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadless():");
//	interp.push("  GUIRegistry.getInstance().getGlobalAction( GUIRegistry.EXIT_PHEX_ACTION).shutdown()");
//	interp.push(" else:");
//	interp.push("  servent.stop()");
//	interp.push("  System.exit( True )");
//	interp.push("");
//        // Now start the interactive console
//        interp.interact("Hello from console. Call help() for available commands.", null);
    }
}
