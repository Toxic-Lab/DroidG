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
package phex.security;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import phex.common.AbstractLifeCycle;
import phex.common.Environment;
import phex.common.EnvironmentConstants;
import phex.common.ExpiryDate;
import phex.common.Phex;
import phex.common.PhexVersion;
import phex.common.URN;
import phex.common.address.AddressUtils;
import phex.common.address.DestAddress;
import phex.common.address.IpAddress;
import phex.common.file.FileManager;
import phex.common.file.ManagedFile;
import phex.common.file.ManagedFileException;
import phex.event.ContainerEvent;
import phex.event.PhexEventTopics;
import phex.event.UserMessageListener;
import phex.prefs.core.SecurityPrefs;
import phex.share.SharedResource;
import phex.utils.StringUtils;
import phex.xml.sax.DPhex;
import phex.xml.sax.XMLBuilder;
import phex.xml.sax.security.DIpAccessRule;
import phex.xml.sax.security.DSecurity;
import phex.xml.sax.security.DSecurityRule;

public class PhexSecurityManager extends AbstractLifeCycle
{
    private static final String[] SHA1_FILES =
    {
        "BearShare and LimeWire Pro scams and worms.SHA1",
        "Fakes.SHA1",
        "SPAM - 0-8K bytes - How2 [open with notepad.avi].SHA1",
        "SPAM - 15,872, 70-170K bytes - EFreeClub.SHA1",
        "SPAM - 22 bytes - Empty zip files.SHA1",
        "SPAM - Various.SHA1",
        "TROJAN - 50-80K bytes - ISTbar.SHA1",
        "TROJAN - 61-62K bytes - Mainpean StarDialer.SHA1",
        "TROJAN - 81,964 bytes - WinVBIE Toolbar.SHA1",
        "TROJAN - 233472 bytes - Dropper.Generic.DZD.SHA1",
        "TROJAN - 783843 bytes - Crypt.B.SHA1",
        "WORM - 71,070 bytes - W32.Alcra.C.SHA1",
        "WORM - 123897 bytes - GEDZAC VBS-Israfel.SHA1",
        "WORM - 178861 bytes - IRC.Backdoor.SdBot.LFI.SHA1",
        "WORM - 202477 bytes - Generic.FX!CME-24.SHA1",
        "WORM - 535082 bytes - W32.Alcra.D.SHA1",
        "WORM - 643767 bytes - VB.FL.SHA1",
        "WORM - 872159 bytes - VB.CC.SHA1",
        "Phex Collected.SHA1"
    };
    
    /**
     * A list of all ip access rules.
     */
    private ArrayList<IpSecurityRule> ipAccessRuleList;
    
    /**
     * Ip list containing Phex default system rules. 
     */
    private final IpSystemRuleList ipSystemRuleList;
    
    /**
     * Ip list containing user rules. We mainly need an additional list for users
     * to distinguish between different AccessTypes.
     */
    private final IpSystemRuleList ipUserRuleList;
    
    private final Set<String> blockedUrnSet;

    private final HashMap<SharedResource, IpPortSystemRuleList> eligibleIpListMap;

    public PhexSecurityManager()
    {
        ipAccessRuleList = new ArrayList<IpSecurityRule>();
        ipSystemRuleList = new IpSystemRuleList();
        ipUserRuleList = new IpSystemRuleList();
        blockedUrnSet = new HashSet<String>();
        eligibleIpListMap = new HashMap<SharedResource, IpPortSystemRuleList>();
    }

    public void addIpSystemRuleListToSharedResource(Object key, String IpAddressStr, int port)
    {
        int ip = AddressUtils.parseDottedIpToInt(IpAddressStr);
        IpSystemSecurityRule rule = new IpSystemSecurityRule(ip, (byte)32);
        IpPortSystemRuleList rulesList = this.eligibleIpListMap.get(key);
        if (rulesList == null) {
            rulesList = new IpPortSystemRuleList();
            this.eligibleIpListMap.put((SharedResource)key, rulesList);
        }
        rulesList.add(new IpPortAddress(IpAddressStr, port), rule);                
    }

    public void removeIpSystemRuleListFromSharedResource(Object key)
    {
        // The key is an instance of SharedResource object
        IpPortSystemRuleList rulesList = eligibleIpListMap.get(key);
        if (rulesList != null) {
            eligibleIpListMap.remove(key);
            rulesList.removeAll();
        }        
    }

    public boolean isEligibleIpAddress(byte[] hostIP, Object key)
    {
        IpPortSystemRuleList rulesList = null;
        IpCidrPair checkPair = null;        

        // hostIP is null if PHEX_EXTENDED_ORIGIN attribute is not sent at all.
        if (hostIP != null) {
            checkPair = new IpCidrPair( AddressUtils.byteIpToIntIp(hostIP) );
        }

        // The key is an instance of SharedResource object
        rulesList = this.eligibleIpListMap.get(key);
        if (rulesList == null) {
            // assumption1: if no ipRulesList is defined for this SharedResource the return back the specific resource
            return true;
        }

        if (checkPair == null) {
            // assumption2: if no PHEX_EXTENDED_ORIGIN attribute is sent, do not return this resource.
            return false;
        }

        return (rulesList.containsRuleAndPort(checkPair, new IpPortAddress(hostIP)));
    }

    public AccessType controlUrnAccess( URN urn )
    {
        if ( urn.isSha1Nid() )
        {
            if( blockedUrnSet.contains( urn.getSHA1Nss() ) )
            {
                return AccessType.ACCESS_STRONGLY_DENIED;
            }
        }
        return AccessType.ACCESS_GRANTED;
    }
    
    private void loadHostileSha1List()
    {
        if ( !SecurityPrefs.LoadHostileSha1List.get().booleanValue() )
        {
            return;
        }
        try
        {
            //NLogger.debug( PhexSecurityManager.class,
            //    "Load hostile sha1 files." );
            long start = System.currentTimeMillis();
            for ( int i = 0; i < SHA1_FILES.length; i++ )
            {
                InputStream inStream = ClassLoader.getSystemResourceAsStream(
                    "phex/resources/sha1/" + SHA1_FILES[i]  );
                BufferedReader br;
                if ( inStream != null )
                {
                    br = new BufferedReader( new InputStreamReader( inStream ) );
                }
                else
                {
                    //NLogger.debug( PhexSecurityManager.class,
                    //    "Hostile sha1 file not found: " + SHA1_FILES[i] );
                    continue;
                }

                String line;
                while ( (line = br.readLine()) != null)
                {
                    if ( StringUtils.isEmpty(line) || line.startsWith("#") )
                    {
                        continue;
                    }
                    if ( line.length() != 32 )
                    {
                        //NLogger.warn( PhexSecurityManager.class,
                        //    "Skip invalid line: " + line + " in " + SHA1_FILES[i]);
                        continue;
                    }
                    boolean succ = blockedUrnSet.add(line);
//                    if ( !succ && //NLogger.isDebugEnabled( PhexSecurityManager.class ) )
//                    {
//                        //NLogger.warn( PhexSecurityManager.class,
//                            "Found dupplicate: " + line + " in " + SHA1_FILES[i] );
//                    }
                }
                br.close();
            }
            long end = System.currentTimeMillis();
//            //NLogger.debug( PhexSecurityManager.class,
//                "Loaded hostile sha1 file: " + (end-start) );
        }
        catch ( IOException exp )
        {
            //NLogger.warn( PhexSecurityManager.class, exp, exp );
        }
    }
    
    
    /////////////////// IP access rules //////////////////////

    public int getIPAccessRuleCount()
    {
        synchronized( ipAccessRuleList )
        {
            return ipAccessRuleList.size();
        }
    }

    public IpSecurityRule getIPAccessRule( int index )
    {
        synchronized( ipAccessRuleList )
        {
            if ( index < 0 || index >= ipAccessRuleList.size() )
            {
                return null;
            }
            return ipAccessRuleList.get( index );
        }
    }

    public IpSecurityRule[] getIPAccessRulesAt( int[] indices )
    {
        synchronized( ipAccessRuleList )
        {
            int length = indices.length;
            IpSecurityRule[] rules = new IpSecurityRule[ length ];
            int listSize = ipAccessRuleList.size();
            for ( int i = 0; i < length; i++ )
            {
                if ( indices[i] < 0 || indices[i] >= listSize )
                {
                    rules[i] = null;
                }
                else
                {
                    rules[i] = ipAccessRuleList.get( indices[i] );
                }
            }
            return rules;
        }
    }

    public IpUserSecurityRule createIPAccessRule( String description,
        byte[] ip, byte cidr, boolean isDisabled, ExpiryDate expiryDate, 
        boolean isDeletedOnExpiry )
    {
        IpUserSecurityRule rule = new IpUserSecurityRule( description,
            ip, cidr, isDisabled, isDeletedOnExpiry, expiryDate );

        int position;
        synchronized( ipAccessRuleList )
        {
            position = ipAccessRuleList.size();
            ipAccessRuleList.add( rule );
        }
        ipUserRuleList.add( rule );
        fireSecurityRuleAdded( rule, position );
        return rule;
    }
    
    public IpUserSecurityRule updateIpUserSecurityRule( IpUserSecurityRule oldRule,
        String description, byte[] ip, byte cidr, boolean isDisabled, 
        ExpiryDate expiryDate, boolean isDeletedOnExpiry )
    {
        IpUserSecurityRule rule = new IpUserSecurityRule( description, 
            ip, cidr, isDisabled, isDeletedOnExpiry, expiryDate );
        rule.setTriggerCount( oldRule.getTriggerCount() );
        
        removeSecurityRule( oldRule );
        
        int position;
        synchronized( ipAccessRuleList )
        {
            position = ipAccessRuleList.size();
            ipAccessRuleList.add( rule );
        }
        ipUserRuleList.add( rule );
        fireSecurityRuleAdded( rule, position );
        return rule;
    }

    public void removeSecurityRule( SecurityRule rule )
    {
        int idx;
        synchronized( ipAccessRuleList )
        {
            idx = ipAccessRuleList.indexOf( rule );
            if ( idx != -1 )
            {
                ipAccessRuleList.remove( idx );
            }
        }
        if ( idx != -1 )
        {
            ipUserRuleList.remove( (IpSecurityRule)rule );
            fireSecurityRuleRemoved( rule, idx );
        }
    }

    public AccessType controlHostAddressAccess( DestAddress address )
    {
        IpAddress ipAddress = address.getIpAddress();
        if ( ipAddress == null )
        {// no ip address... security is not checking not ip based  addresses.
            return AccessType.ACCESS_GRANTED;
        }
        byte[] hostIP = ipAddress.getHostIP();
        return controlHostIPAccess( hostIP );
    }

    public AccessType controlHostIPAccess( byte[] hostIP )
    {
        IpCidrPair checkPair = new IpCidrPair( AddressUtils.byteIpToIntIp(hostIP) );
        boolean contains = ipSystemRuleList.contains( checkPair );
        if ( contains )
        {
            return AccessType.ACCESS_STRONGLY_DENIED;
        }
        
        contains = ipUserRuleList.contains( checkPair );
        if ( contains )
        {
            return AccessType.ACCESS_DENIED;
        }
        return AccessType.ACCESS_GRANTED;
    }

    private void loadHostileHostList( Map<String, DIpAccessRule> systemRuleMap )
    {
        if ( !SecurityPrefs.LoadHostileHostList.get().booleanValue() )
        {
            return;
        }
        try
        {
            //NLogger.debug( PhexSecurityManager.class,
            //    "Load hostile hosts file." );
            long start = System.currentTimeMillis();
            InputStream inStream = ClassLoader.getSystemResourceAsStream(
                "phex/resources/hostiles/gtkg-hostiles.txt" );
                //"phex/resources/hostilehosts.gz" );
            BufferedReader br;
            if ( inStream != null )
            {
                br = new BufferedReader( new InputStreamReader( /*new GZIPInputStream(*/ inStream /*)*/ ) );
            }
            else
            {
                //NLogger.debug( PhexSecurityManager.class,
                //    "Hostile hosts file not found." );
                return;
            }

            String line;
            int ip;
            byte cidr;
            IpSystemSecurityRule rule;
            while ( (line = br.readLine()) != null)
            {
                if ( line.startsWith("#") )
                {
                    continue;
                }
                line = line.trim();
                if ( StringUtils.isEmpty(line) )
                {
                    continue;
                }
                int slashIdx = line.indexOf( '/' );
                
                if ( slashIdx == -1 )
                {// single ip...
                    ip = AddressUtils.parseDottedIpToInt( line );
                    cidr = 32;
                }
                else
                {
                    String ipStr = line.substring( 0, slashIdx ).trim();
                    String extensionStr = line.substring( slashIdx + 1 ).trim();
                    ip = AddressUtils.parseDottedIpToInt( ipStr );
                    cidr = AddressUtils.parseNetmaskToCidr( extensionStr );
                }
                rule = new IpSystemSecurityRule( ip, cidr );
                
                // adjust hit count..
                DSecurityRule xjbRule = findSystemXJBRule( systemRuleMap, ip,
                    cidr );
                if ( xjbRule != null )
                {
                    rule.setTriggerCount( xjbRule.getTriggerCount() );
                }
                ipAccessRuleList.add( rule );
                ipSystemRuleList.add( rule );
                //if ( ipAccessRuleList.size()%10000==0)
                //{
                //    long end = System.currentTimeMillis();
                //    //NLogger.debug( NLoggerNames.Security,
                //        "Part: " + ((double)(end-start)/(double)ipAccessRuleList.size()) + " " + ipAccessRuleList.size() );
                //}
            }
            br.close();
            long end = System.currentTimeMillis();
            //NLogger.debug( PhexSecurityManager.class,
            //    "Loaded hostile hosts file: " + (end-start) );
        }
        catch ( IOException exp )
        {
            //NLogger.warn( PhexSecurityManager.class, exp, exp );
        }
    }

    private DSecurityRule findSystemXJBRule( Map<String, DIpAccessRule> systemRuleMap, int ip, 
        byte cidr )
    {
        DSecurityRule xjbRule = systemRuleMap.get( 
            AddressUtils.ip2string(ip) + "/" + cidr );
        if ( xjbRule == null || !xjbRule.isSystemRule())
        {
            return null;
        }
        return xjbRule;
    }

    private void loadSecurityRuleList()
    {
        //NLogger.debug( PhexSecurityManager.class,
        //    "Loading security rule list..." );
        
        DPhex dPhex;
        try
        {
            File securityFile = Environment.getInstance().getPhexConfigFile(
                EnvironmentConstants.XML_SECURITY_FILE_NAME );
            if ( securityFile.exists() )
            {
                FileManager fileMgr = Phex.getFileManager();
                ManagedFile managedFile = fileMgr.getReadWriteManagedFile( securityFile );
                dPhex = XMLBuilder.loadDPhexFromFile( managedFile );
            }
            else
            {
                dPhex = new DPhex();
            }            
            DSecurity dSecurity = dPhex.getSecurityList();
            if ( dSecurity == null )
            {
                //NLogger.debug( PhexSecurityManager.class,
                //    "No security definition found." );
                dSecurity = new DSecurity();
            }
            
            synchronized( ipAccessRuleList )
            {
                List<DSecurityRule> dRuleList = dSecurity.getIpAccessRuleList();
                Map<String, DIpAccessRule> systemRuleMap = new HashMap<String, DIpAccessRule>();
                for( DSecurityRule dRule : dRuleList )
                {
                    // currently we only have DIpAccessRules
                    DIpAccessRule dIpRule = (DIpAccessRule)dRule;
                    if( dIpRule.hasDenyingRule() && !dIpRule.isDenyingRule() )
                    {
                        // currently we don't support ACCEPT IP rules
                        // this is done to improve performance in the lookup 
                        // process. A different solution would be to inverse
                        // all ACCEPT rules to achieve the same effect. Is it 
                        // worth the extra work? TODO3
                        continue;
                    }
                    if ( !dRule.isSystemRule() )
                    {
                        if ( dIpRule.hasCidr() )
                        {
                            IpUserSecurityRule rule = new IpUserSecurityRule( 
                                dIpRule.getDescription(), dIpRule.getIp(),
                                dIpRule.getCidr(), dIpRule.isDisabled(),
                                dIpRule.isDeletedOnExpiry(), dIpRule.getExpiryDate() );
                            rule.setTriggerCount( dIpRule.getTriggerCount() );
                            ipAccessRuleList.add( rule );
                            ipUserRuleList.add( rule );
                        }
                        else
                        {
                            if ( dIpRule.getAddressType() == DIpAccessRule.NETWORK_MASK  )
                            {
                                IpUserSecurityRule rule = new IpUserSecurityRule( 
                                    dIpRule.getDescription(), dIpRule.getIp(), 
                                    AddressUtils.calculateCidr( dIpRule.getCompareIp() ),
                                    dIpRule.isDisabled(), dIpRule.isDeletedOnExpiry(), 
                                    dIpRule.getExpiryDate() );
                                rule.setTriggerCount( dIpRule.getTriggerCount() );
                                ipAccessRuleList.add( rule );
                                ipUserRuleList.add( rule );
                            }
                            else if ( dIpRule.getAddressType() == DIpAccessRule.SINGLE_ADDRESS  )
                            {
                                IpUserSecurityRule rule = new IpUserSecurityRule( 
                                    dIpRule.getDescription(), dIpRule.getIp(), 
                                    (byte)32, dIpRule.isDisabled(),
                                    dIpRule.isDeletedOnExpiry(), dIpRule.getExpiryDate() );
                                rule.setTriggerCount( dIpRule.getTriggerCount() );
                                ipAccessRuleList.add( rule );
                                ipUserRuleList.add( rule );
                            }
                            else if ( dIpRule.getAddressType() == DIpAccessRule.NETWORK_RANGE  )
                            {
                                
                                List<IpCidrPair> pairList = AddressUtils.range2cidr( 
                                    dIpRule.getIp(), dIpRule.getCompareIp() );
                                for ( IpCidrPair pair : pairList )
                                {
                                    IpUserSecurityRule rule = new IpUserSecurityRule(
                                        dIpRule.getDescription(), pair.ipAddr, 
                                        pair.cidr, dIpRule.isDisabled(),
                                        dIpRule.isDeletedOnExpiry(), dIpRule.getExpiryDate() );
                                    rule.setTriggerCount( dIpRule.getTriggerCount() );
                                    ipAccessRuleList.add( rule );
                                    ipUserRuleList.add( rule );
                                }
                            }
                        }
                    }
                    else
                    {
                        if ( dIpRule.hasCidr() )
                        {
                            String keyStr = AddressUtils.ip2string( dIpRule.getIp() ) 
                                + "/" + String.valueOf( dIpRule.getCidr() );
                            systemRuleMap.put( keyStr, dIpRule);
                        }
                        else
                        {
                            StringBuffer keyBuf = new StringBuffer( AddressUtils.ip2string( dIpRule.getIp() ) );
                            keyBuf.append( "/" );
                            if ( dIpRule.getCompareIp() == null )
                            {
                                keyBuf.append( "32" );
                            }
                            else
                            {
                                keyBuf.append( AddressUtils.calculateCidr( 
                                        dIpRule.getCompareIp() ) );
                            }
                            systemRuleMap.put( keyBuf.toString(), dIpRule);
                        }
                    }
                }
                loadHostileHostList( systemRuleMap );
                
                // optimize ipAccessRuleList
                ipAccessRuleList.trimToSize();
            }
        }
        catch ( IOException exp )
        {
            //NLogger.error( PhexSecurityManager.class, exp, exp );
            Environment.getInstance().fireDisplayUserMessage( 
                UserMessageListener.SecuritySettingsLoadFailed, 
                new String[]{ exp.toString() } );
            return;
        }
        catch ( ManagedFileException exp )
        {
            //NLogger.error( PhexSecurityManager.class, exp, exp );
            Environment.getInstance().fireDisplayUserMessage( 
                UserMessageListener.SecuritySettingsLoadFailed, 
                new String[]{ exp.toString() } );
            return;
        }
    }
    
    public void clearTriggerCount()
    {
        synchronized( ipAccessRuleList )
        {
            for ( IpSecurityRule rule : ipAccessRuleList )
            {
                rule.setTriggerCount(0);
                  
                //DSecurityRule dRule = rule.createDSecurityRule();
                //security.getIpAccessRuleList().add( dRule );
            }
        }
    }

    private void saveSecurityRuleList()
    {
        //NLogger.debug( PhexSecurityManager.class, "Saving security rule list..." );

        try
        {
            DPhex dPhex = new DPhex();
            dPhex.setPhexVersion( PhexVersion.getFullVersion() );
            
            DSecurity security = new DSecurity();
            dPhex.setSecurityList( security );

            synchronized( ipAccessRuleList )
            {
                for ( IpSecurityRule rule : ipAccessRuleList )
                {
                    if ( !rule.isSystemRule() && rule.isDeletedOnExpiry() && 
                         ( rule.getExpiryDate().isExpiringEndOfSession() ||
                           rule.getExpiryDate().isExpired() ) )
                    {// skip session expiry rules that get deleted on expiry...
                     // except if they are system rules
                        continue;
                    }

                    if ( rule.isSystemRule() && rule.getTriggerCount() == 0)
                    {// we don't care about system rules with no trigger count.
                        continue;
                    }
                    
                    DSecurityRule dRule = rule.createDSecurityRule();
                    security.getIpAccessRuleList().add( dRule );
                }
            }

            File securityFile = Environment.getInstance().getPhexConfigFile(
                EnvironmentConstants.XML_SECURITY_FILE_NAME );
            ManagedFile managedFile = Phex.getFileManager().getReadWriteManagedFile( securityFile );
            XMLBuilder.saveToFile( managedFile, dPhex );
        }
        catch ( IOException exp )
        {
            // TODO during close this message is never displayed since application
            // will exit too fast. A solution to delay exit process in case 
            // SlideInWindows are open needs to be found.
            //NLogger.error( PhexSecurityManager.class, exp, exp );
            Environment.getInstance().fireDisplayUserMessage( 
                UserMessageListener.SecuritySettingsSaveFailed, 
                new String[]{ exp.toString() } );
        }
        catch ( ManagedFileException exp )
        {
            // TODO during close this message is never displayed since application
            // will exit too fast. A solution to delay exit process in case 
            // SlideInWindows are open needs to be found.
            //NLogger.error( PhexSecurityManager.class, exp, exp );
            Environment.getInstance().fireDisplayUserMessage( 
                UserMessageListener.SecuritySettingsSaveFailed, 
                new String[]{ exp.toString() } );
        }
    }

    //////////////////////// Start LifeCycle Methods ///////////////////////////

    /**
     * {@inheritDoc}
     */
    @Override
    public void doStart()
    {
        loadSecurityRuleList();
        loadHostileSha1List();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doStop()
    {
        saveSecurityRuleList();
    }
    //////////////////////// End LifeCycle Methods ///////////////////////////

    ///////////////////// START event handling methods /////////////////////////

    private void fireSecurityRuleAdded( final SecurityRule rule, final int position )
    {
        Phex.getEventService().publish( PhexEventTopics.Security_Rule ,
            new ContainerEvent( ContainerEvent.Type.ADDED, rule, this, position ) );
    }

    private void fireSecurityRuleRemoved( final SecurityRule rule, final int position )
    {
        Phex.getEventService().publish( PhexEventTopics.Security_Rule ,
            new ContainerEvent( ContainerEvent.Type.REMOVED, rule, this, position ) );
    }
    ///////////////////// END event handling methods ////////////////////////
}