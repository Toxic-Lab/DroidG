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
 */
package phex.common;

public interface EnvironmentConstants
{
    public static final String CORE_PREFERENCES_FILE_NAME = "phexCorePrefs.properties";
    public static final String GUI_PREFERENCES_FILE_NAME = "phexGuiPrefs.properties";
    public static final String LOG_FILE_NAME = "phex.log";
    public static final String ERROR_LOG_FILE_NAME = "phex.error.log";
    public static final String AUTOCONNECT_HOSTS_FILE_NAME = "autoconnecthosts.cfg";
    public static final String G_WEB_CACHE_FILE_NAME = "gwebcache.cfg";
    public static final String UDP_HOST_CACHE_FILE_NAME = "udphostcache.cfg";
    public static final String HOSTS_FILE_NAME = "phex.hosts";
    public static final String XML_DOWNLOAD_FILE_NAME = "phexdownload.xml";
    public static final String XML_RESEARCH_SERVICE_FILE_NAME = "phexresearch.xml";
    public static final String XML_GUI_SETTINGS_FILE_NAME = "phexgui.xml";
    public static final String XML_SECURITY_FILE_NAME = "security.xml";
    public static final String XML_SHARED_LIBRARY_FILE_NAME = "sharedlibrary.xml";
    public static final String XML_FILTER_LIST_FILE_NAME = "filterlist.xml";
    public static final String XML_FAVORITES_FILE_NAME = "favorites.xml";
    public static final String XML_SEARCH_FILTER_FILE_NAME = "searchfilters.xml";
}