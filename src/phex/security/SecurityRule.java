/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2007 Phex Development Group
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
package phex.security;

import phex.common.ExpiryDate;
import phex.xml.sax.security.DSecurityRule;

public interface SecurityRule
{
    /**
     * Returns the description of the rule.
     * @return the description of the rule.
     */
    String getDescription();
    
    /**
     * Returns the expiry date that indicates when this rule expires. It can be a
     * at a time, at the end of the session or never.
     * @return the expiry date that indicates when this rule expires.
     */
    ExpiryDate getExpiryDate();
    
    /**
     * Returns the number of times the rule was triggered. All checks that
     * match the rule will increment the trigger.
     * @return the number of times the rule was triggered.
     */
    int getTriggerCount();
    
    /**
     * Sets the number of times the rule was triggered. All checks that
     * match the rule will increment the trigger.
     */
    void setTriggerCount( int count );
    
    boolean isDeletedOnExpiry();
    
    boolean isSystemRule();
    
    boolean isDisabled();
    
    /**
     * Returns true when checks that match the rule will fail.
     * Returns false when all checks that do not match the rule will fail.
     * @return true when checks that match the rule will fail,
     *         false when all checks that do not match the rule will fail.
     */
    boolean isDenyingRule();
    
    DSecurityRule createDSecurityRule();
}