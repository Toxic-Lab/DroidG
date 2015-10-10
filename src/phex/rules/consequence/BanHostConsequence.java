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
 *  Created on 18.11.2005
 *  --- CVS Information ---
 *  $Id$
 */
package phex.rules.consequence;

import org.apache.commons.lang.time.DateUtils;

import phex.common.ExpiryDate;
import phex.download.RemoteFile;
import phex.query.Search;
import phex.security.BanHostBatch;
import phex.servent.Servent;
import phex.xml.sax.rules.DBanHostConsequence;
import phex.xml.sax.rules.DConsequence;

public class BanHostConsequence implements Consequence
{
    public static final BanHostConsequence INSTANCE = new BanHostConsequence();

    private static final DBanHostConsequence DELEMENT = new DBanHostConsequence();

    public void invoke(Search search, final RemoteFile remoteFile, Servent servent)
    {
        // handle a ban like a remove we dont like to see the RemoteHost
        // again in this consequence
        remoteFile.setFilteredRemoved(true);

        ExpiryDate expDate = ExpiryDate.getExpiryDate(System
                .currentTimeMillis()
                + DateUtils.MILLIS_PER_DAY * 7);
        BanHostBatch.addDestAddress(remoteFile.getHostAddress(), expDate,
            servent.getSecurityService() );
    }

    @Override
    public Object clone()
    {
        // there is only one instance...
        return INSTANCE;
    }

    public DConsequence createDConsequence()
    {
        return DELEMENT;
    }
}
