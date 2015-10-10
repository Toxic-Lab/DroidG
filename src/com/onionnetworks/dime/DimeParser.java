//Dime 1.0.3 2003-03-05 http://www.onionnetworks/developers
package com.onionnetworks.dime;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Extracts DIME records from a single DIME message drawn from a provided
 * stream.  The DIME specification can be found at:
 * http://search.ietf.org/internet-drafts/draft-nielsen-dime-02.txt .  Make 
 * sure you read the javadoc for getNext
 *
 * @author Ry4an (ry4an@onionnetworks.com)
 */

public class DimeParser implements Iterator {

    private Object nextStoplight = new Object();
    private InputStream in;
    private DimeRecord prev = null;
    
    /** Creates a DimeParser that will turn the provided InputStream into
     * a series of DIME records.  Only a single DIME message is extracted from
     * the stream.  The stream is not closed and the fileposition is left
     * immediately after the message.
     *
     * @param is the stream to extract a DIME message from
     */
    public DimeParser(InputStream is) {
        in = is;
    }

    /**
     * Each call to this returns the next DIME record in the message.
     * <p>
     * <b>Important:</b> This method can't be called again until you've
     * completely consumed the previously returned DimeRecord's payload by
     * calling getPayload.  You have to eat your record before you can get
     * seconds.
     * <p>
     * Records are returned in order they existed in the DIME message stream.
     * Use isLast() to see if the returned DimeRecord was the last in the
     * message.
     *
     * @see DimeRecord#getPayload
     * @see DimeRecord#isLast
     * @return dr the next DIME record in the message
     * @throws NoSuchElementException if all records have been returned or on
     *          IO error
     * @throws IllegalStateException if previous record wasn't consumed
     */
    public synchronized Object next() {
        try {
            return nextRecord();
        } catch (IOException doh) {
            doh.printStackTrace();
            throw new NoSuchElementException("IOException");
        }
    }

    /**
     * Each call to this returns the next DIME record in the message.
     * <p>
     * <b>Important:</b> This method can't be called again until you've
     * completely consumed the previously returned DimeRecord's payload by
     * calling getPayload.  You have to eat your record before you can get
     * seconds.
     * <p>
     * Records are returned in order they existed in the DIME message stream.
     * Use isLast() to see if the returned DimeRecord was the last in the
     * message.  Use isContinued to see if you need to append the next record
     * to get the full original record.
     *
     * @see DimeRecord#getPayload
     * @see DimeRecord#isLast
     * @see DimeRecord#isContinued
     * @return dr the next DIME record in the message
     * @throws NoSuchElementException if all records have been returned
     * @throws IllegalStateException if previous record wasn't consumed
     * @throws IOException on failure to read;
     */
    public synchronized DimeRecord nextRecord() throws IOException {
        if (! hasNext()) {
            throw new NoSuchElementException("Last Message already returned");
        }
        return (prev = DimeRecord.extract(in));
    }

    /**
     * Lets one know if there are more records in this message.  It is safe
     * to call this even if you have not fully consumed the previously returned
     * message.
     * @return true if there are more records
     */
    public synchronized boolean hasNext() {
        return ((prev == null) || !(prev.isLast()));
    }

    /**
     * @throws UnsupportedOperationException always
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Return a list of all possible records we can still read from the stream.
     * 
     * If all records are already read, returns an empty list.
     */
    public static List<DimeRecord> getAllRecords(DimeParser parser)
            throws IOException
    {
        List<DimeRecord> records = new LinkedList<DimeRecord>();

        while (parser.hasNext())
        {
            records.add(parser.nextRecord());
        }
        return records;
    }

    /**
     * This parser demo shows how to stitch chunked payloads back together.
     */
    public static void main(String[] args) throws Exception {
        DimeParser dp = new DimeParser(new FileInputStream(new File(args[0])));
        int nextRecord = 0;
        int nextPart = 0;
        FileOutputStream fos = null;
        while (dp.hasNext()) {
            System.out.println("-----[ Record: " + nextRecord++);
            DimeRecord dr = dp.nextRecord();
            long pl = dr.getData().length;
            System.out.println("Payload Length: " + pl);
            System.out.println("Id: "+ dr.getId());
            System.out.println("Type: "
                + ((dr.getType() != null)?dr.getType():"<null>"));
            System.out.println("TNF: " +dr.getTypeNameFormat().toInt());
            System.out.println("First:" + dr.isFirst());
            System.out.println("Last:" + dr.isLast());

            if (fos == null) {
                fos = new FileOutputStream(new File("part-" + ++nextPart));
            }
            byte[] data = dr.getData();
            fos.write( data, 0, data.length);
            fos.flush();
            fos.close();
            fos = null;
            System.out.println("Payload: <part-" + nextPart + ">");
        }
    }
}
