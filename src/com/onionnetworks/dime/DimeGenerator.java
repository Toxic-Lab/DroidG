//Dime 1.0.3 2003-03-05 http://www.onionnetworks/developers
package com.onionnetworks.dime;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;

import phex.utils.IOUtil;

/**
 * Creates a DIME Message on the fly and sends it down a provided Stream.  This
 * avoids the necessity of ever storing the full DIME message in memory.  The
 * DIME specification can be found at:
 * http://search.ietf.org/internet-drafts/draft-nielsen-dime-02.txt .  
 *
 * @author Ry4an (ry4an@onionnetworks.com)
 */

public class DimeGenerator {

    private long autoChunkSize;

    private OutputStream out;

    private boolean first; // true if the first has been sent
    private boolean last; // true if the last has been sent

    private HashSet ids;

    /** 
     * Create s single use generator that will send a valid DIME message
     * down the specified OutputStream  The stream is not closed.  Record
     * payloads will be capped at DimeRecord.DEFAULT_MAX_PAYLOAD_SIZE
     *
     * @param os the stream down which to send the DIME message
     * @see DimeRecord#DEFAULT_MAX_PAYLOAD_SIZE
     */
    public DimeGenerator(OutputStream os) {
        this(os, DimeRecord.DEFAULT_MAX_PAYLOAD_SIZE);
    }

    /** 
     * Create s single use generator that will send a valid DIME message
     * down the specified OutputStream  The stream is not closed.
     *
     * @param os the stream down which to send the DIME message
     * @param acs the maximum size of a DIME record payload in the message.  May
     *            not be bigger than DimeRecord.MAX_MAX_PAYLOAD_SIZE
     * @see DimeRecord#MAX_MAX_PAYLOAD_SIZE
     */
    public DimeGenerator(OutputStream os, long acs) {
        if (acs > DimeRecord.MAX_MAX_PAYLOAD_SIZE) {
            throw new IllegalArgumentException
                ("auto chunk size cannot be bigger than "
                 + DimeRecord.MAX_MAX_PAYLOAD_SIZE);
        }
        autoChunkSize = acs;
        out = os;
        ids = new HashSet();
    }

    /**
     * Add data to the outgoing DIME stream.  Throws an exception if
     * a DimeRecord with an identical id has already been added.
     *
     * @param dr the DimeRecord to add
     * @param l true if this is the last Record
     * @throws IllegalStateException on duplicate id
     * @throws IOException on failure to write
     */
    public void addRecord(DimeRecord dr, boolean l) throws IOException {
        if (last) {
            throw new IllegalStateException("DIME message closed");
        }
        if (l) {
            last = true;
        }
        // insure no duplicate IDs
        if (dr.getId() != null) {
            synchronized (ids) {
                if (ids.contains(dr.getId())) {
                    throw new IllegalStateException
                        ("Two records can't have the same id");
                }
                ids.add(dr.getId());
            }
        }
        dr.produce(out, autoChunkSize, first?false:(first=true), l);
    }

    /** 
     * For testing only.
     * @param args a list of filenames to make dime parts
     * @throws IOException on read/write error
     */
    public static void main(String[] args) throws IOException {
        FileOutputStream out = new FileOutputStream(new File("output.dime"));
        DimeGenerator dg = new DimeGenerator(out);
        String type = "Ry4an type";
        for (int i=0; i < args.length; i++) {
            File f = new File(args[i]);
            byte[] data = IOUtil.toByteArray(new FileInputStream(f));
            DimeRecord dr = new DimeRecord(data, DimeRecord.TypeNameFormat.UNKNOWN, type, null);
            dg.addRecord(dr, (i == args.length-1));
            System.out.println("Added: " + dr);
        }
        out.flush();
        out.close();
    }
}
