/* (PD) 2001 The Bitzi Corporation
* Please see http://bitzi.com/publicdomain for more info.
*
* Base32.java
*/

package com.bitzi.util;

/**
* Base32 - encodes and decodes 'Canonical' Base32 strings.
*
* @author  Robert Kaye & Gordon Mohr for original work on Base32
* @author  Philippe Verdy for optimizations and canonical checking
*/
public class Base32 {
    
    /* Lookup table used to canonically encode() groups of data bits */
    private static final char[] canonicalChars =
    {'A','B','C','D','E','F','G','H','I','J','K','L','M' // 00..12
    ,'N','O','P','Q','R','S','T','U','V','W','X','Y','Z' // 13..25
    ,'2','3','4','5','6','7'                             // 26..31
    };

    /* First valid character that can be indexed in decode lookup table */
    private static final int charDigitsBase = (int)'2';
    
    /* Lookup table used to decode() characters in encoded strings */
    private static final int[] charDigits =
    {      26,27,28,29,30,31,-1,-1,-1,-1,-1,-1,-1,-1 //   23456789:;<=>?
    ,-1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9,10,11,12,13,14 // @ABCDEFGHIJKLMNO
    ,15,16,17,18,19,20,21,22,23,24,25,-1,-1,-1,-1,-1 // PQRSTUVWXYZ[\]^_
    ,-1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9,10,11,12,13,14 // `abcdefghijklmno
    ,15,16,17,18,19,20,21,22,23,24,25                // pqrstuvwxyz
    };
    
    /* Messsages for Illegal Parameter Exceptions in decode() */
    private static final String errorNonCanonicalLength
        = "non canonical Base32-encoded string length";
    private static final String errorInvalidChar
        = "invalid character in Base32-encoded string";
    private static final String errorNonCanonicalEnd
        = "non canonical bits at end of Base32-encoded string";
    
    /**
    * Encode an array of binary bytes into a Base32 string.
    * Should not fail (the only possible exception is that the
    * returned string cannot be allocated in memory)
    */
    static public String encode(final byte[] bytes) {
        int bytesOffset = 0, bytesLen = bytes.length;
        int charsOffset = 0, charsLen = ((bytesLen << 3) + 4) / 5;
        final char[] chars = new char[charsLen];
        while (bytesLen != 0) {
            int digit, lastDigit;
            // INVARIANTS FOR EACH STEP n in [0..5[; digit in [0..31[;
            // The remaining n bits are already aligned on top positions
            // of the 5 least bits of digit, the other bits are 0.
            ////// STEP n = 0: insert new 5 bits, leave 3 bits
            digit = bytes[bytesOffset    ] & 255;
            chars[charsOffset    ] = canonicalChars[digit >>> 3];
            lastDigit = (digit & 7) << 2;
            if (bytesLen == 1) { // put the last 3 bits
                chars[charsOffset + 1] = canonicalChars[lastDigit];
                break;
            }
            ////// STEP n = 3: insert 2 new bits, then 5 bits, leave 1 bit
            digit = bytes[bytesOffset + 1] & 255;
            chars[charsOffset + 1] = canonicalChars[(digit >>> 6) | lastDigit];
            chars[charsOffset + 2] = canonicalChars[(digit >>> 1) & 31];
            lastDigit = (digit & 1) << 4;
            if (bytesLen == 2) { // put the last 1 bit
                chars[charsOffset + 3] = canonicalChars[lastDigit];
                break;
            }
            ////// STEP n = 1: insert 4 new bits, leave 4 bit
            digit = bytes[bytesOffset + 2] & 255;
            chars[charsOffset + 3] = canonicalChars[(digit >>> 4) | lastDigit];
            lastDigit = (digit & 15) << 1;
            if (bytesLen == 3) { // put the last 1 bits
                chars[charsOffset + 4] = canonicalChars[lastDigit];
                break;
            }
            ////// STEP n = 4: insert 1 new bit, then 5 bits, leave 2 bits
            digit = bytes[bytesOffset + 3] & 255;
            chars[charsOffset + 4] = canonicalChars[(digit >>> 7) | lastDigit];
            chars[charsOffset + 5] = canonicalChars[(digit >>> 2) & 31];
            lastDigit = (digit & 3) << 3;
            if (bytesLen == 4) { // put the last 2 bits
                chars[charsOffset + 6] = canonicalChars[lastDigit];
                break;
            }
            ////// STEP n = 2: insert 3 new bits, then 5 bits, leave 0 bit
            digit = bytes[bytesOffset + 4] & 255;
            chars[charsOffset + 6] = canonicalChars[(digit >>> 5) | lastDigit];
            chars[charsOffset + 7] = canonicalChars[digit & 31];
            //// This point is always reached for bytes.length multiple of 5
            bytesOffset += 5;
            charsOffset += 8;
            bytesLen -= 5;
        }
        return new String(chars);
    }
    
    /**
    * Decode a Base32 encoded string into an array of binary bytes.
    * May fail if the parameter is a non canonical Base32 string
    * (the only other possible exception is that the
    * returned array cannot be allocated in memory)
    */
    static public byte[] decode(final String encoded)
            throws IllegalArgumentException {
        final char[] chars = encoded.toCharArray(); // avoids using charAt()
        int charsLen = chars.length;
        // Note that the code below detects could detect non canonical
        // Base32 length within the loop. However canonical Base32 length
        // can be tested before entering the loop.
        // A canonical Base32 length modulo 8 cannot be:
        // 1 (aborts discarding 5 bits at STEP n=0 which produces no byte),
        // 3 (aborts discarding 7 bits at STEP n=2 which produces no byte),
        // 6 (aborts discarding 6 bits at STEP n=1 which produces no byte).
        switch (charsLen & 7) { // test the length of last subblock
        case 1: //  5 bits in subblock:  0 useful bits but 5 discarded
        case 3: // 15 bits in subblock:  8 useful bits but 7 discarded
        case 6: // 30 bits in subblock: 24 useful bits but 6 discarded
            throw new IllegalArgumentException(errorNonCanonicalLength);
        }
        final int charDigitsLen = charDigits.length;
        final int bytesLen = (charsLen * 5) >>> 3;
        final byte[] bytes = new byte[bytesLen];
        int bytesOffset = 0, charsOffset = 0;
        // Also the code below does test that other discarded bits
        // (1 to 4 bits at end) are effectively 0.
        while (charsLen > 0) {
            int digit, lastDigit;
            // STEP n = 0: Read the 1st Char in a 8-Chars subblock
            // Leave 5 bits, asserting there's another encoding Char
            if ((digit = (int)chars[charsOffset] - charDigitsBase) < 0
                || digit >= charDigitsLen
                || (digit = charDigits[digit]) == -1)
                throw new IllegalArgumentException(errorInvalidChar);
            lastDigit = digit << 3;
            // STEP n = 5: Read the 2nd Char in a 8-Chars subblock
            // Insert 3 bits, leave 2 bits, possibly trailing if no more Char
            if ((digit = (int)chars[charsOffset + 1] - charDigitsBase) < 0
                || digit >= charDigitsLen
                || (digit = charDigits[digit]) == -1)
                throw new IllegalArgumentException(errorInvalidChar);
            bytes[bytesOffset] = (byte)((digit >> 2) | lastDigit);
            lastDigit = (digit & 3) << 6;
            if (charsLen == 2) {
                if (lastDigit != 0)
                    throw new IllegalArgumentException(errorNonCanonicalEnd);
                break; // discard the 2 trailing null bits
            }
            // STEP n = 2: Read the 3rd Char in a 8-Chars subblock
            // Leave 7 bits, asserting there's another encoding Char
            if ((digit = (int)chars[charsOffset + 2] - charDigitsBase) < 0
                || digit >= charDigitsLen
                || (digit = charDigits[digit]) == -1)
                throw new IllegalArgumentException(errorInvalidChar);
            lastDigit |= (byte)(digit << 1);
            // STEP n = 7: Read the 4th Char in a 8-chars Subblock
            // Insert 1 bit, leave 4 bits, possibly trailing if no more Char
            if ((digit = (int)chars[charsOffset + 3] - charDigitsBase) < 0
                || digit >= charDigitsLen
                || (digit = charDigits[digit]) == -1)
                throw new IllegalArgumentException(errorInvalidChar);
            bytes[bytesOffset + 1] = (byte)((digit >> 4) | lastDigit);
            lastDigit = (byte)((digit & 15) << 4);
            if (charsLen == 4) {
                if (lastDigit != 0)
                    throw new IllegalArgumentException(errorNonCanonicalEnd);
                break; // discard the 4 trailing null bits
            }
            // STEP n = 4: Read the 5th Char in a 8-Chars subblock
            // Insert 4 bits, leave 1 bit, possibly trailing if no more Char
            if ((digit = (int)chars[charsOffset + 4] - charDigitsBase) < 0
                || digit >= charDigitsLen
                || (digit = charDigits[digit]) == -1)
                throw new IllegalArgumentException(errorInvalidChar);
            bytes[bytesOffset + 2] = (byte)((digit >> 1) | lastDigit);
            lastDigit = (byte)((digit & 1) << 7);
            if (charsLen == 5) {
                if (lastDigit != 0)
                    throw new IllegalArgumentException(errorNonCanonicalEnd);
                break; // discard the 1 trailing null bit
            }
            // STEP n = 1: Read the 6th Char in a 8-Chars subblock
            // Leave 6 bits, asserting there's another encoding Char
            if ((digit = (int)chars[charsOffset + 5] - charDigitsBase) < 0
                || digit >= charDigitsLen
                || (digit = charDigits[digit]) == -1)
                throw new IllegalArgumentException(errorInvalidChar);
            lastDigit |= (byte)(digit << 2);
            // STEP n = 6: Read the 7th Char in a 8-Chars subblock
            // Insert 2 bits, leave 3 bits, possibly trailing if no more Char
            if ((digit = (int)chars[charsOffset + 6] - charDigitsBase) < 0
                || digit >= charDigitsLen
                || (digit = charDigits[digit]) == -1)
                throw new IllegalArgumentException(errorInvalidChar);
            bytes[bytesOffset + 3] = (byte)((digit >> 3) | lastDigit);
            lastDigit = (byte)((digit & 7) << 5);
            if (charsLen == 7) {
                if (lastDigit != 0)
                    throw new IllegalArgumentException(errorNonCanonicalEnd);
                break; // discard the 3 trailing null bits
            }
            // STEP n = 3: Read the 8th Char in a 8-Chars subblock
            // Insert 5 bits, leave 0 bit, next encoding Char may not exist
            if ((digit = (int)chars[charsOffset + 7] - charDigitsBase) < 0
                || digit >= charDigitsLen
                || (digit = charDigits[digit]) == -1)
                throw new IllegalArgumentException(errorInvalidChar);
            bytes[bytesOffset + 4] = (byte)(digit | lastDigit);
            //// This point is always reached for chars.length multiple of 8
            charsOffset += 8;
            bytesOffset += 5;
            charsLen -= 8;
        }
        // On loop exit, discard the n trailing null bits
        return bytes;
    }
    
/*
    // For tests only, take a command-line argument in Base32,
    // decode, print in hex, encode, print, and check performance.
    static Object noOp(final Object o) {return null;}
    static public void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Supply a Base32-encoded argument...");
            return;
        }
        System.out.print  (" Original string: ");
        System.out.println(args[0]);
        //// decode() test. May fail if parameter not canonical
        byte[] bytes = decode(args[0]);
        System.out.print  ("   Decoded bytes:");
        for(int i = 0; i < bytes.length ; i++) {
            int b = bytes[i] & 255;
            System.out.print(' ');
            System.out.print((Integer.toHexString(b + 256)).substring(1));
        }
        System.out.println();
        //// encode() test (should match input parameter string)
        System.out.print  ("Reencoded string: ");
        String reencoded = encode(bytes);
        System.out.println(reencoded);
        //// decode test (should also match previously decoded bytes)
        System.out.print  (" Redecoded bytes:");
        byte[] redecoded = decode(reencoded);
        for(int i = 0; i < bytes.length ; i++) {
            int b = redecoded[i] & 255;
            System.out.print(' ');
            System.out.print((Integer.toHexString(b + 256)).substring(1));
        }
        System.out.println();
        //// Performance test
        System.out.print("Computing number of loops for performance tests...");
        Object dummy = null;
        long before = 0, after = 0;
        double ms = 0, extra;
        int loopsTest;
        // Dynamically find the number of loops needed to reach a total
        // time of at least 4000ms, so that we compute the precise average
        // extra time to perform a loop of static calls doing nothing.
        // This is guaranteed not to require more than either 8 seconds or
        // the time to perform 1000 dummy loops, and we won't continue
        // if we've got already 1.4 billion dummy loops.
        for (loopsTest = 1000;
        loopsTest < 1400000000 && after - before < 4000;
        loopsTest += loopsTest / 2)  {
            before = System.currentTimeMillis();
            for (int loop = loopsTest; --loop >= 0; ) dummy = noOp(dummy);
            after = System.currentTimeMillis();
            ms = (double)(after - before) / (double)loopsTest;
            System.out.print('.');
        }
        System.out.println();
        System.out.println("Average time per static call: " +
            new Double(ms).toString() + "ms (computed on " +
            Integer.valueOf(loopsTest).toString() + " loops)");
        // Cap the number of loops found above so that the above dummy
        // tests would not have taken more than 500ms to perform
        extra = 500.0 / ms;
        System.out.println("Average static call performance: " +
            new Double(extra / 500.0 * 1000).toString() + " calls/s");
        if ((double)loopsTest >= extra)
            loopsTest = (int)extra;
        // Keep this precise extra time per loop of static dummy calls
        extra = ms;
        System.out.println("Starting performance tests on " +
            Integer.valueOf(loopsTest).toString() + " call loops.");
        //// encode() performance test
        before = System.currentTimeMillis();
        for (int loop = loopsTest; --loop >= 0; ) reencoded = encode(bytes);
        after = System.currentTimeMillis();
        ms = (double)(after - before) / (double)loopsTest - extra;
        System.out.println("encode() time: " +
            new Double(ms).toString() + "ms per call");
        System.out.println("encode() input: " +
            new Double(bytes.length / ms).toString() + " bytes/ms");
        //// decode() performance test
        before = System.currentTimeMillis();
        for (int loop = loopsTest; --loop >= 0; ) bytes = decode(reencoded);
        after = System.currentTimeMillis();
        ms = (double)(after - before) / (double)loopsTest - extra;
        System.out.println("decode() time: " +
            new Double(ms).toString() + "ms per call");
        System.out.println("decode() output: " +
            new Double(bytes.length / ms).toString() + " bytes/ms");
    }
*/
}