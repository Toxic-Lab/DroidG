/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2005 Phex Development Group
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
 *  Created on 06.10.2006
 *  --- CVS Information ---
 *  $Id$
 */
package phex.prefs.core;

import phex.prefs.api.PreferencesFactory;
import phex.prefs.api.Setting;

public class SystemToolsPrefs extends PhexCorePrefs
{
    /**
     * The command to execute for video media types on systems other then
     * Windows and Mac OSX
     */
    public static final Setting<String> OpenVideoCmdOtherOS;
    
    /**
     * The command to execute for image media types on systems other then
     * Windows and Mac OSX (usually Unix systems).
     */
    public static final Setting<String> OpenImageCmdOtherOS;
    
    /**
     * The command to execute for audio media types on systems other then
     * Windows and Mac OSX (usually Unix systems).
     */
    public static final Setting<String> OpenAudioCmdOtherOS;
        
    /**
     * The browser command to execute for none video, image, audio media types 
     * on systems other then Windows and Mac OSX (usually Unix systems).
     */
    public static final Setting<String> OpenBrowserCmdOtherOS;
    
    static
    {
        OpenVideoCmdOtherOS = PreferencesFactory.createStringSetting( 
            "SystemTools.OpenVideoCmdOtherOS", "xterm -e mplayer %filepath%", instance );
        OpenImageCmdOtherOS = PreferencesFactory.createStringSetting( 
            "SystemTools.OpenImageCmdOtherOS", "ee %filepath%", instance );
        OpenAudioCmdOtherOS = PreferencesFactory.createStringSetting( 
            "SystemTools.OpenAudioCmdOtherOS", "xterm -e mplayer %filepath%", instance );
        OpenBrowserCmdOtherOS = PreferencesFactory.createStringSetting( 
            "SystemTools.OpenBrowserCmdOtherOS", "mozilla -remote openURL(%filepath%,new-window)", instance );
    }
}
