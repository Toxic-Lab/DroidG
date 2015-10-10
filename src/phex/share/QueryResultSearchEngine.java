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
package phex.share;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import phex.common.URN;
import phex.common.collections.IntSet;
import phex.common.collections.IntSet.IntSetIterator;
import phex.msg.QueryMsg;
import phex.prefs.core.LibraryPrefs;
import phex.security.PhexSecurityManager;
import phex.servent.Servent;
import phex.utils.StringUtils;

public class QueryResultSearchEngine
{
    public static final String INDEX_QUERY_STRING = "    ";
    
    private final SharedFilesService sharedFilesService;
    private final Servent servent;
    
    public QueryResultSearchEngine( Servent servent, SharedFilesService sharedFilesService )
    {
        this.servent = servent;
        this.sharedFilesService = sharedFilesService;
    }
    
    /**
     * Search the sharefile database and get groups of sharefiles.
     * It returns empty results if the query source and the local host
     * are both firewalled. 
     * @param queryMsg the query
     * @return the found results.
     */
    public List<ShareFile> handleQuery( QueryMsg queryMsg )
    {
        // Perform search on my list.
        
        // Only send results if we support the possible query feature.
        if ( queryMsg.getFeatureQuerySelector() > QueryMsg.WHAT_IS_NEW_FEATURE_QUERY_SELECTOR )
        {
            return Collections.emptyList();
        }

        // If the query source and the local host are both firewalled, return no results
        // as per http://groups.yahoo.com/group/the_gdf/files/Proposals/MinSpeed.html
        if ( queryMsg.isRequesterFirewalled() && servent.isFirewalled() )
        {
            return Collections.emptyList();
        }
        // if all upload slots are filled dont return any search results.
        // This holds away unnecessary connection attempts.
        if ( servent.isUploadLimitReached() )
        {
            return Collections.emptyList();
        }
        
        if ( queryMsg.isWhatsNewQuery() )
        {// we handle what is new queries special and return the 3 newest files
            return sharedFilesService.getNewestFiles( 3 );
        }

        String searchStr = queryMsg.getSearchString();
        if ( searchStr.equals( INDEX_QUERY_STRING ) )
        {
//            NLogger.debug( QueryResultSearchEngine.class,
//                "Index query detected." );
            return sharedFilesService.getSharedFiles();
        }

        // first check for URN query...
        List<ShareFile> urnMatches = null;
        URN[] urns = queryMsg.getQueryURNs();
        if ( urns.length > 0 )
        {
            urnMatches = sharedFilesService.getFilesByURNs( urns );
            if ( urnMatches.size() == urns.length )
            {// we found all requested files by URN.
                // return results and be happy that we are already finished.
                return provideResultData( urnMatches, null, queryMsg.getOriginIpAddress() );
            }
        }
        
        // if there are no urns or not all urns have a hit check for 
        // keyword matches...
        IntSet keyWordMatches = handleKeywordSearch( searchStr );
        
        return provideResultData( urnMatches, keyWordMatches, queryMsg.getOriginIpAddress() );
    }
    
    /**
     * Performs the keyword query on the search string.
     * @param searchStr the search string
     * @return an IntSet containing the result file index positions.
     */
    private IntSet handleKeywordSearch( String searchStr )
    {
        IntSet searchMatches = null;
        
        int searchStrLength = searchStr.length();
        int startPos = 0;
        int endPos;
        Iterator<IntSet> indexIterator;
        while ( startPos < searchStrLength ) 
        {
            if ( StringUtils.isFileDelimiter( searchStr.charAt( startPos ) ) ) 
            {
                startPos ++;
                continue;
            }
            for ( endPos = startPos + 1; endPos < searchStrLength; endPos ++ ) 
            {
                if ( StringUtils.isFileDelimiter( searchStr.charAt( endPos ) ) )
                {
                    break;
                }
            }
            
            // search for prefixed keyword using word delimiters.
            indexIterator = sharedFilesService.getIndexIteratorForPrefixTerm( 
                searchStr, startPos, endPos );
            if ( indexIterator.hasNext() )
            {
                IntSet keywordMatches = null;
                while ( indexIterator.hasNext() ) 
                {                
                    IntSet match = indexIterator.next();
                    if ( keywordMatches == null ) 
                    {
                        if ( startPos == 0 && endPos == searchStrLength && !indexIterator.hasNext() )
                        {
                            return match;
                        }
                        keywordMatches = new IntSet();
                    }
                    keywordMatches.addAll( match );
                }

                if ( searchMatches == null )
                {
                    searchMatches = keywordMatches;
                }
                else
                {
                    searchMatches.retainAll( keywordMatches );
                    if ( searchMatches.size() == 0 )
                    {
                        // no match we can abort the keyword search
                        return null;
                    }
                }
            } 
            else 
            {
                // no match we can abort the complete keyword search
                return null;
            }
            startPos = endPos;
        }
        
        return searchMatches;
    }
    
    private List<ShareFile> provideResultData( List<ShareFile> urnMatches, 
        IntSet keywordMatches, byte[] originIpAddress )
    {
        int count = 0;
        if ( urnMatches != null )
        {
            count += urnMatches.size();
        }
        if ( keywordMatches != null )
        {
            count += keywordMatches.size();
        }
        if ( count == 0 )
        {
            return Collections.emptyList();
        }
        
        PhexSecurityManager securityService = servent.getSecurityService();
        List<ShareFile> resultList = new ArrayList<ShareFile>();
        final int maxResults = LibraryPrefs.MaxResultsPerQuery.get().intValue();
        int resultListSize = 0;
        if ( urnMatches != null && urnMatches.size() > 0 )
        {
            for ( ShareFile shareFile : urnMatches )
            {
                try 
                {
                    SharedDirectory dir = sharedFilesService.getSharedDirectory(shareFile.getSystemFile().getParentFile());
                    if (!securityService.isEligibleIpAddress(originIpAddress, dir))
                    {
                        continue;
                    }
                }
                catch (Exception exp)
                {
                    //NLogger.warn( QueryResultSearchEngine.class, exp, exp );
                }

                // increment search count for files in list
                shareFile.incSearchCount();
                resultList.add( shareFile );
                resultListSize ++;
                if ( resultListSize >= maxResults )
                {
                    break;
                }
            }
        }
        if ( keywordMatches != null && keywordMatches.size() > 0 
          && resultListSize < maxResults)
        {
            IntSetIterator iterator = keywordMatches.iterator();
            while( iterator.hasNext() )
            {
                int index = iterator.next();
                ShareFile shareFile = sharedFilesService.getFileByIndex( index );
                if ( shareFile != null )
                {
                    try 
                    {
                        SharedDirectory dir = sharedFilesService.getSharedDirectory(shareFile.getSystemFile().getParentFile());
                        if (!securityService.isEligibleIpAddress(originIpAddress, dir))
                        {
                            continue;
                        }
                    }
                    catch (Exception exp)
                    {
                        //NLogger.warn( QueryResultSearchEngine.class, exp, exp );
                    }

                    // increment search count for files in list
                    shareFile.incSearchCount();
                    resultList.add( shareFile );
                    resultListSize ++;
                    if ( resultListSize >= maxResults )
                    {
                        break;
                    }
                }
            }
        }
        
        assert( resultList.size() <= maxResults );
                
        return resultList;
    }
        

//    private ArrayList<ShareFile> handleOldKeywordSearch( String searchStr )
//    {
//        // if there are no urns or not all urns have a hit check for string query...
//        StringTokenizer tokenizer = new StringTokenizer( searchStr );
//        ArrayList<String> tokenList = new ArrayList<String>( Math.min( 10,
//            tokenizer.countTokens() ) );
//        String term;
//        // Build search term, max up to 10 terms.
//        while (tokenList.size() < 10 && tokenizer.hasMoreElements())
//        {
//            term = tokenizer.nextToken();
//            // ignore terms with less then 2 char length
//            if ( term.length() >= DynamicQueryConstants.MIN_SEARCH_TERM_LENGTH )
//            {
//                tokenList.add( term.toLowerCase() );
//            }
//        }
//
//        if ( tokenList.size() == 0 )
//        {// no string search to do
//            return null;
//        }
//
//        ShareFile[] shareFiles = sharedFilesService.getSharedFiles();
//        SearchEngine searchEngine = new SearchEngine();
//
//        // searches through the files for each search term. Drops all files that
//        // dont match a search term from the possible result list.
//
//        //long start1 = System.currentTimeMillis();
//        // all files are possible results
//        ArrayList<ShareFile> leftFiles = new ArrayList<ShareFile>( Arrays.asList( shareFiles ) );
//
//        // go through each term
//        for (int i = 0; i < tokenList.size() && leftFiles.size() > 0; i++)
//        {
//            searchEngine.setPattern( tokenList.get( i ), true );
//
//            // go through each left file in the files array
//            for (int j = leftFiles.size() - 1; j >= 0; j--)
//            {
//                ShareFile shareFile = leftFiles.get( j );
//                // dont share files without calculated urn..
//                if ( shareFile.getURN() == null )
//                {
//                    leftFiles.remove( j );
//                    continue;
//                }
//                searchEngine.setText( shareFile.getSearchCompareTerm(), true );
//                if ( !searchEngine.match() )
//                {
//                    // a term dosn't match remove possible result.
//                    leftFiles.remove( j );
//                }
//            }
//        }
//        
//        return leftFiles;
//    }

}
