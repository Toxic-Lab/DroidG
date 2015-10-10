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

// this class uses source based on the IPList implementation of Limewire
// (1.16 - 2007-01-25)

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import phex.common.collections.PatriciaTrie;
import phex.common.collections.Trie;
import phex.common.collections.PatriciaTrie.KeyAnalyzer;
import phex.common.collections.Trie.Cursor;

public class IpSystemRuleList
{
    /**
     * IP trie for system rules.
     */
    private PatriciaTrie<IpCidrPair, IpCidrPair> ipTrie;

    public IpSystemRuleList()
    {
        ipTrie = new PatriciaTrie<IpCidrPair, IpCidrPair>(
            new IpKeyAnalyzer() );
    }
    
    public void add( IpSecurityRule rule ) 
    {
        HittingIpCidrPair addPair = rule.getIpCidrPair();
        
        // If we already had it (or an address that contained it),
        // then don't include.  Also remove any IPs we encountered
        // that are contained by this new IP.
        // These two properties are necessary to allow the optimization
        // in Lookup to exit when the distance is greater than 1.
        AddFilter filter = new AddFilter( addPair );
        Map.Entry<? extends IpCidrPair, ? extends IpCidrPair> entry = ipTrie.select( addPair, filter );
        if(entry != null) 
        {
            if( !entry.getKey().contains( addPair ) ) 
            {
                for( IpCidrPair obsolete : filter.getContained() ) 
                {
                    ipTrie.remove(obsolete);
                }
                ipTrie.put( addPair, addPair );
            }
        } 
        else 
        {
            ipTrie.put( addPair, addPair );
        }
    }
    
    public void remove( IpSecurityRule rule ) 
    {
        HittingIpCidrPair removePair = rule.getIpCidrPair();
        
        // Remove this and any IPs we encountered that are contained by this IP.
        // We use the same funcunality AddFilter provides for finding IPs
        AddFilter filter = new AddFilter( removePair );
        Map.Entry<? extends IpCidrPair, ? extends IpCidrPair> entry = ipTrie.select( removePair, filter );
        if(entry != null) 
        {
            if( !entry.getKey().contains( removePair ) ) 
            {
                for( IpCidrPair obsolete : filter.getContained() ) 
                {
                    ipTrie.remove(obsolete);
                }
            }
        } 
        ipTrie.remove( removePair );
    }
    
    /**
     * @param String equal to an IP
     * @returns true if ip_address is contained somewhere in the list of IPs
     */
    public boolean contains( IpSystemSecurityRule rule ) 
    {
        HittingIpCidrPair lookupPair = rule.getIpCidrPair();
        return contains(lookupPair);
    }
    
    /**
     * @param String equal to an IP
     * @returns true if ip_address is contained somewhere in the list of IPs
     */
    public boolean contains( IpCidrPair lookupPair ) 
    {
        HittingIpCidrPair pair = (HittingIpCidrPair)ipTrie.select( lookupPair );
        if ( pair == null )
        {
            return false;
        }
        if ( !pair.contains( lookupPair ) )
        {
            return false;
        }
        pair.countHit();
        return true;
    }
    
    /**
     * A filter for adding IPs -- stores IPs we encountered that
     * are contained by the to-be-added IP, so they can later
     * be removed.
     */
    private static class AddFilter implements Trie.Cursor<IpCidrPair, IpCidrPair> 
    {
        private final IpCidrPair lookup;
        private List<IpCidrPair> contained;
        
        AddFilter(IpCidrPair lookup) {
            this.lookup = lookup;
        }
        
        /**
         * Returns all the IPs we encountered while selecting
         * that were contained by the IP being added.
         */
        public List<IpCidrPair> getContained() {
            if(contained == null)
                return Collections.emptyList();
            else
                return contained;
        }
        
        public Cursor.SelectStatus select(Map.Entry<? extends IpCidrPair, ? extends IpCidrPair> entry) {
            IpCidrPair compare = entry.getKey();
            if (compare.contains( lookup )) 
            {
                return Cursor.SelectStatus.EXIT; // Terminate
            }
            
            if(lookup.contains(compare)) 
            {
                if(contained == null)
                {
                    contained = new ArrayList<IpCidrPair>();
                }
                contained.add(compare);
                return SelectStatus.CONTINUE;
            } 
            else 
            {
                // Because select traverses in XOR closeness,
                // the first time we encounter an item that's
                // not contained, we know we've exhausted all
                // possible containing values.
                return SelectStatus.EXIT;
            }
        }
    }
    
    private static class IpKeyAnalyzer 
        implements KeyAnalyzer<IpCidrPair>
    {

        private static final int[] createIntBitMask(final int bitCount)
        {
            int[] bits = new int[bitCount];
            for ( int i = 0; i < bitCount; i++ )
            {
                bits[i] = 1 << (bitCount - i - 1);
            }
            return bits;
        }

        private static final int[] BITS = createIntBitMask(32);

        public int length( IpCidrPair key )
        {
            return 32;
        }

        public boolean isBitSet( IpCidrPair key, int keyLength, int bitIndex)
        {
            int maddr = key.ipAddr & key.getNetMask();
            return (maddr & BITS[bitIndex]) != 0;
        }

        public int bitIndex( IpCidrPair key, int keyOff, int keyLength, IpCidrPair found,
            int foundOff, int foundKeyLength)
        {
            int maddr1 = key.ipAddr & key.getNetMask();
            int maddr2 = (found != null) ? found.ipAddr & found.getNetMask() : 0;

            if ( keyOff != 0 || foundOff != 0 )
                throw new IllegalArgumentException(
                    "offsets must be 0 for fixed-size keys");

            int length = Math.max(keyLength, foundKeyLength);

            boolean allNull = true;
            for ( int i = 0; i < length; i++ )
            {
                int a = maddr1 & BITS[i];
                int b = maddr2 & BITS[i];

                if ( allNull && a != 0 )
                {
                    allNull = false;
                }

                if ( a != b )
                {
                    return i;
                }
            }

            if ( allNull )
            {
                return KeyAnalyzer.NULL_BIT_KEY;
            }

            return KeyAnalyzer.EQUAL_BIT_KEY;
        }

        public int compare( IpCidrPair o1, IpCidrPair o2)
        {
            int addr1 = o1.ipAddr & o1.getNetMask();
            int addr2 = o2.ipAddr & o2.getNetMask();
            if ( addr1 > addr2 )
                return 1;
            else if ( addr1 < addr2 )
                return -1;
            else
                return 0;

        }

        // This method is generally intended for variable length keys.
        // Fixed-length keys, such as an IP address (32 bits) tend to
        // look at each element as a bit, thus 1 element == 1 bit.
        public int bitsPerElement()
        {
            return 1;
        }

        public boolean isPrefix( IpCidrPair prefix, int offset, int length, IpCidrPair key)
        {
            int addr1 = prefix.ipAddr & prefix.getNetMask();
            int addr2 = key.ipAddr & key.getNetMask();
            addr1 = addr1 << offset;

            int mask = 0;
            for ( int i = 0; i < length; i++ )
            {
                mask |= (0x1 << i);
            }

            addr1 &= mask;
            addr2 &= mask;

            return addr1 == addr2;
        }
    }
}