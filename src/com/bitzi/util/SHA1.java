/* @(#)SHA1.java    1.12 2004-08-08
 * This file was freely contributed to the LimeWire project and is covered
 * by its existing GPL licence, but it may be used individually as a public
 * domain implementation of a published algorithm (see below for references).
 * It was also freely contributed to the Bitzi public domain sources.
 * @author  Philippe Verdy
 */

/* Sun may wish to change the following package name, if integrating this
 * class in the Sun JCE Security Provider for Java 1.5 (code-named Tiger).
 *
 * You can include it in your own Security Provider by inserting
 * this property in your Provider derived class:
 * put("MessageDigest.SHA-1", "org.rodage.pub.java.security.SHA1");
 */
package com.bitzi.util;
import java.security.*;
//--+---+1--+---+--2+---+---+3--+---+--4+---+---+5--+---+--6+---+---+7--+---+--
//34567890123456789012345678901234567890123456789012345678901234567890123456789

/**
 * <p>The FIPS PUB 180-2 standard specifies four secure hash algorithms (SHA-1,
 * SHA-256, SHA-384 and SHA-512) for computing a condensed representation of
 * electronic data (message).  When a message of any length < 2^^64 bits (for
 * SHA-1 and SHA-256) or < 2^^128 bits (for SHA-384 and SHA-512) is input to
 * an algorithm, the result is an output called a message digest.  The message
 * digests range in length from 160 to 512 bits, depending on the algorithm.
 * Secure hash algorithms are typically used with other cryptographic
 * algorithms, such as digital signature algorithms and keyed-hash message
 * authentication codes, or in the generation of random numbers (bits).</p>
 *
 * <p>The four hash algorithms specified in this "SHS" standard are called
 * secure because, for a given algorithm, it is computationally infeasible
 * 1) to find a message that corresponds to a given message digest, or 2)
 * to find two different messages that produce the same message digest.  Any
 * change to a message will, with a very high probability, result in a
 * different message digest.  This will result in a verification failure when
 * the secure hash algorithm is used with a digital signature algorithm or a
 * keyed-hash message authentication algorithm.</p>
 *
 * <p>A "SHS change notice" adds a SHA-224 algorithm for interoperability,
 * which, like SHA-1 and SHA-256, operates on 512-bit blocks and 32-bit words,
 * but truncates the final digest and uses distinct initialization values.</p>
 *
 * <p><b>References:</b></p>
 * <ol>
 *   <li> NIST FIPS PUB 180-2, "Secure Hash Signature Standard (SHS) with
 *      change notice", National Institute of Standards and Technology (NIST),
 *      2002 August 1, and U.S. Department of Commerce, August 26.<br>
 *      <a href="http://csrc.ncsl.nist.gov/CryptoToolkit/Hash.html">
 *      http://csrc.ncsl.nist.gov/CryptoToolkit/Hash.html</a>
 *   <li> NIST FIPS PUB 180-1, "Secure Hash Standard",
 *      U.S. Department of Commerce, May 1993.<br>
 *      <a href="http://www.itl.nist.gov/div897/pubs/fip180-1.htm">
 *      http://www.itl.nist.gov/div897/pubs/fip180-1.htm</a></li>
 *   <li> Bruce Schneier, "Section 18.7 Secure Hash Algorithm (SHA)",
 *      <cite>Applied Cryptography, 2nd edition</cite>, <br>
 *      John Wiley &amp; Sons, 1996</li>
 * </ol>
 */
public final class SHA1 extends MessageDigest implements Cloneable {
    /**
     * Private contextual byte count, stored at end of the last block,
     * after the ending padded block.
     */
    private long bytes;

    /**
     * Private context for incomplete blocks and padded bytes.<br/>
     * INVARIANT: padded must be in 0..63.<br/>
     * When the padded reaches 64 bytes, a new block is computed.
     * Up to 56 last bytes are kept in the padded history.
     */
    private int padded;
    private byte[] pad;

    /**
     * Private context that contains the current digest key.
     */
    private int hA, hB, hC, hD, hE;

    /**
     * Creates a SHA1 object with default initial state.
     */
    public SHA1()
    {
        super("SHA-1");
        pad = new byte[64];
        init();
    }

    /**
     * Clones this object.
     */
    public Object clone() throws CloneNotSupportedException {
        final SHA1 that = (SHA1)super.clone();
        that.pad = this.pad.clone();
        return that;
    }

    /**
     * Reset then initialize the digest context.<br/>
     *
     * Overrides the protected abstract method of
     * <code>java.security.MessageDigestSpi</code>.
     * @modifies  this
     */
    public void engineReset() {
        int i = 60;
        final byte[] buf = pad;
        do {
           buf[i - 4] = (byte)0x00;
           buf[i - 3] = (byte)0x00;
           buf[i - 2] = (byte)0x00;
           buf[i - 1] = (byte)0x00;
           buf[i    ] = (byte)0x00;
           buf[i + 1] = (byte)0x00;
           buf[i + 2] = (byte)0x00;
           buf[i + 3] = (byte)0x00;
        } while ((i -= 8) >= 0);
        padded = 0;
        bytes = 0L;
        init();
    }

    /**
     * Initialize the digest context.
     * @modifies  this
     */
    protected void init() {
        /* Set to the first 32 bits of the fractional part of the square
         * root of the 5 first prime numbers (from 2 to 11). */
        hA = 0x67452301;
        hB = 0xefcdab89;
        hC = 0x98badcfe;
        hD = 0x10325476;
        hE = 0xc3d2e1f0;
    }

    /**
     * Updates the digest using the specified byte.
     * Requires internal buffering, and may be slow.<br/>
     *
     * Overrides the protected abstract method of
     * <code>java.security.MessageDigestSpi</code>.
     * @param input  the byte to use for the update.
     * @modifies  this
     */
    public void engineUpdate(final byte input) {
        bytes++;
        if (padded < 63) {
            pad[padded++] = input;
            return;
        }
        pad[63] = input;
        computeBlock(pad, padded = 0);
    }

    /**
     * Updates the digest using the specified array of bytes,
     * starting at the specified offset.<br/>
     *
     * Input length can be any size. May require internal buffering,
     * if input blocks are not multiple of 64 bytes.<br/>
     *
     * Overrides the protected abstract method of
     * <code>java.security.MessageDigestSpi</code>.
     * @param input  the array of bytes to use for the update.
     * @param offset  the offset to start from in the array of bytes.
     * @param length  the number of bytes to use, starting at offset.
     * @modifies  this
     */
    public void engineUpdate(byte[] input, int offset, int length) {
        if (offset >= 0 && length >= 0 && offset + length <= input.length) {
            bytes += length;
            /* Terminate the previous block. */
            if (padded > 0 && padded + length >= 64) {
                final int remaining;
                System.arraycopy(input, offset, pad, padded,
                    remaining = 64 - padded);
                computeBlock(pad, padded = 0);
                offset += remaining;
                length -= remaining;
            }
            /* Loop on large sets of complete blocks. */
            while (length >= 512) {
                computeBlock(input, offset);
                computeBlock(input, offset + 64);
                computeBlock(input, offset + 128);
                computeBlock(input, offset + 192);
                computeBlock(input, offset + 256);
                computeBlock(input, offset + 320);
                computeBlock(input, offset + 384);
                computeBlock(input, offset + 448);
                offset += 512;
                length -= 512;
            }
            /* Loop on remaining complete blocks. */
            while (length >= 64) {
                computeBlock(input, offset);
                offset += 64;
                length -= 64;
            }
            /* remaining bytes kept for next block. */
            if (length > 0) {
                System.arraycopy(input, offset, pad, padded, length);
                padded += length;
            }
            return;
        }
        throw new ArrayIndexOutOfBoundsException(offset);
    }

    /**
     * Completes the hash computation by performing final operations
     * such as padding. Computes the final hash and returns the final
     * value as a byte[20] array. Once engineDigest has been called,
     * the engine will be automatically reset as specified in the
     * Java Security MessageDigest specification.<br/>
     *
     * For faster operations with multiple digests, allocate your own
     * array and use engineDigest(byte[], int offset, int len).<br/>
     *
     * Overrides the protected abstract method of
     * <code>java.security.MessageDigestSpi</code>.
     * @return the length of the digest stored in the output buffer.
     * @modifies  this
     */
    public byte[] engineDigest() {
        try {
            final byte hashvalue[] = new byte[20]; /* digest length in bytes */
            engineDigest(hashvalue, 0, 20); /* digest length in bytes */
            return hashvalue;
        } catch (DigestException e) {
            return null;
        }
    }

    /**
     * Returns the digest length in bytes. Can be used to allocate your own
     * output buffer when computing multiple digests.<br/>
     *
     * Overrides the protected abstract method of
     * <code>java.security.MessageDigestSpi</code>.
     * @return  the digest length in bytes.
     */
    public int engineGetDigestLength() {
        return 20; /* digest length in bytes */
    }

    /**
     * Completes the hash computation by performing final operations
     * such as padding. Once engineDigest has been called, the engine
     * will be automatically reset (see engineReset).<br/>
     *
     * Overrides the protected abstract method of
     * <code>java.security.MessageDigestSpi</code>.
     * @param hashvalue  the output buffer in which to store the digest.
     * @param offset  offset to start from in the output buffer
     * @param length  number of bytes within buf allotted for the digest.
     *                Both this default implementation and the SUN provider
     *                do not return partial digests.  The presence of this
     *                parameter is solely for consistency in our API's.
     *                If the value of this parameter is less than the
     *                actual digest length, the method will throw a
     *                DigestException.  This parameter is ignored if its
     *                value is greater than or equal to the actual digest
     *                length.
     * @return  the length of the digest stored in the output buffer.
     * @modifies  this
     */
    public int engineDigest(final byte[] hashvalue, int offset,
            final int length) throws DigestException {
        if (length >= 20) { /* digest length in bytes */
            if (hashvalue.length - offset >= 20) { /* digest length in bytes */
                /* Flush the trailing bytes, adding padded bytes into last
                 * blocks. */
                int i;
                /* Add padded null bytes but replace the last 8 padded bytes
                 * by the little-endian 64-bit digested message bit-length. */
                final byte[] buf;
                (buf=pad)[i=padded] = (byte)0x80;/* required 1st padded byte */
                /* Check if 8 bytes are available in pad to store the total
                 * message size */
                switch (i) { /* INVARIANT: i must be in [0..63] */
                case 56: buf[57] = (byte)0x00; /* no break; falls thru */
                case 57: buf[58] = (byte)0x00; /* no break; falls thru */
                case 58: buf[59] = (byte)0x00; /* no break; falls thru */
                case 59: buf[60] = (byte)0x00; /* no break; falls thru */
                case 60: buf[61] = (byte)0x00; /* no break; falls thru */
                case 61: buf[62] = (byte)0x00; /* no break; falls thru */
                case 62: buf[63] = (byte)0x00; /* no break; falls thru */
                case 63: computeBlock(buf, 0);
                         i = -1;
                }
                /* Clear the rest of the 56 first bytes of pad[]. */
                switch (i & 7) {
                case 7:      i-=3;
                        break;
                case 6: buf[(i-=2) + 3] = (byte)0x00;
                        break;
                case 5: buf[(i-=1) + 2] = (byte)0x00;
                        buf[ i     + 3] = (byte)0x00;
                        break;
                case 4: buf[ i     + 1] = (byte)0x00;
                        buf[ i     + 2] = (byte)0x00;
                        buf[ i     + 3] = (byte)0x00;
                        break;
                case 3: buf[(i+=1)    ] = (byte)0x00;
                        buf[ i     + 1] = (byte)0x00;
                        buf[ i     + 2] = (byte)0x00;
                        buf[ i     + 3] = (byte)0x00;
                        break;
                case 2: buf[(i+=2) - 1] = (byte)0x00;
                        buf[ i        ] = (byte)0x00;
                        buf[ i     + 1] = (byte)0x00;
                        buf[ i     + 2] = (byte)0x00;
                        buf[ i     + 3] = (byte)0x00;
                        break;
                case 1: buf[(i+=3) - 2] = (byte)0x00;
                        buf[ i     - 1] = (byte)0x00;
                        buf[ i        ] = (byte)0x00;
                        buf[ i     + 1] = (byte)0x00;
                        buf[ i     + 2] = (byte)0x00;
                        buf[ i     + 3] = (byte)0x00;
                        break;
                case 0: buf[(i+=4) - 3] = (byte)0x00;
                        buf[ i     - 2] = (byte)0x00;
                        buf[ i     - 1] = (byte)0x00;
                        buf[ i        ] = (byte)0x00;
                        buf[ i     + 1] = (byte)0x00;
                        buf[ i     + 2] = (byte)0x00;
                        buf[ i     + 3] = (byte)0x00;
                        break;
                }
                while ((i += 8) <= 52) {
                    buf[i - 4] = (byte)0x00;
                    buf[i - 3] = (byte)0x00;
                    buf[i - 2] = (byte)0x00;
                    buf[i - 1] = (byte)0x00;
                    buf[i    ] = (byte)0x00;
                    buf[i + 1] = (byte)0x00;
                    buf[i + 2] = (byte)0x00;
                    buf[i + 3] = (byte)0x00;
                }
                /* Convert the message size from bytes to big-endian bits. */
                buf[56] = (byte)((i = (int)(bytes >>> 29)) >> 24);
                buf[57] = (byte)(i >>> 16);
                buf[58] = (byte)(i >>> 8);
                buf[59] = (byte)i;
                buf[60] = (byte)((i = (int)bytes << 3) >> 24);
                buf[61] = (byte)(i >>> 16);
                buf[62] = (byte)(i >>> 8);
                buf[63] = (byte)i;
                computeBlock(pad, 0);
                /* Return the computed digest in big-endian byte order. */
                hashvalue[ offset         ] = (byte)((i = hA) >>> 24);
                hashvalue[ offset      + 1] = (byte)(i >>> 16);
                hashvalue[ offset      + 2] = (byte)(i >>> 8);
                hashvalue[ offset      + 3] = (byte)i;
                hashvalue[ offset      + 4] = (byte)((i = hB) >>> 24);
                hashvalue[ offset      + 5] = (byte)(i >>> 16);
                hashvalue[(offset+=10) - 4] = (byte)(i >>> 8);
                hashvalue[ offset      - 3] = (byte)i;
                hashvalue[ offset      - 2] = (byte)((i = hC) >>> 24);
                hashvalue[ offset      - 1] = (byte)(i >>> 16);
                hashvalue[ offset         ] = (byte)(i >>> 8);
                hashvalue[ offset      + 1] = (byte)i;
                hashvalue[ offset      + 2] = (byte)((i = hD) >>> 24);
                hashvalue[ offset      + 3] = (byte)(i >>> 16);
                hashvalue[ offset      + 4] = (byte)(i >>> 8);
                hashvalue[ offset      + 5] = (byte)i;
                hashvalue[(offset+=6)     ] = (byte)((i = hE) >>> 24);
                hashvalue[ offset      + 1] = (byte)(i >>> 16);
                hashvalue[ offset      + 2] = (byte)(i >>> 8);
                hashvalue[ offset      + 3] = (byte)i;
                engineReset(); /* clear the evidence */
                return 20; /* digest length in bytes */
            }
            throw new DigestException(
                "insufficient space in output buffer to store the digest");
        }
        throw new DigestException("partial digests not returned");
    }

    /**
     * Updates the digest using the specified array of bytes,
     * starting at the specified offset, but an implied length
     * of exactly 64 bytes.<br/>
     *
     * Requires no internal buffering, but assumes a fixed input size,
     * in which the required padded bytes may have been added.
     *
     * @param input  the array of bytes to use for the update.
     * @param offset  the offset to start from in the array of bytes.
     * @modifies  this
     */
    private void computeBlock(final byte[] input, int offset) {
        /* Local temporary work variables for intermediate digests. */
        int a, b, c, d, e;
        /* Cache the input block into the local working set of 32-bit
         * values, in big-endian byte order. Be careful when
         * widening bytes or integers due to sign extension! */
        int i0, i1, i2, i3, i4, i5, i6, i7,
            i8, i9, iA, iB, iC, iD, iE, iF;
        /* Use digest schedule function Ch (steps 0..19):
         *   Ch(x,y,z) = (x & y) ^ (~x & z) = ((y ^ z) & x) ^ z,
         * and K00 = .... = K19 = 0x5a827999. */
        /* First round, on big endian input (steps 0..15). */
        e = (i0 =  input[ offset       ] << 24
                | (input[ offset     +1] & 0xff) << 16
                | (input[ offset     +2] & 0xff) << 8
                | (input[ offset     +3] & 0xff)) /* W00 */
          + ((((c = hC)      ^ (d = hD)) & (b = hB)) ^ d) /* Ch(b,c,d) */
          + (((a = hA) << 5) | (a >>> 27)) + 0x5a827999 /* K00 */ + hE;
        d = (i1 =  input[ offset     +4] << 24
                | (input[ offset     +5] & 0xff) << 16
                | (input[(offset+=10)-4] & 0xff) << 8
                | (input[ offset     -3] & 0xff)) // W01
          + ((((b = (b << 30) | (b >>> 2)) ^ c) & a) ^ c) /* Ch(a,b,c) */
          + ((e << 5) | (e >>> 27)) + 0x5a827999 /* K01 */ + d;
        c = (i2 =  input[ offset     -2] << 24
                | (input[ offset     -1] & 0xff) << 16
                | (input[ offset       ] & 0xff) << 8
                | (input[ offset     +1] & 0xff)) /* W02 */
          + ((((a = (a << 30) | (a >>> 2)) ^ b) & e) ^ b) /* Ch(e,a,b) */
          + ((d << 5) | (d >>> 27)) + 0x5a827999 /* K02 */ + c;
        b = (i3 =  input[ offset     +2] << 24
                | (input[ offset     +3] & 0xff) << 16
                | (input[ offset     +4] & 0xff) << 8
                | (input[ offset     +5] & 0xff)) /* W03 */
          + ((((e = (e << 30) | (e >>> 2)) ^ a) & d) ^ a) /* Ch(d,e,a) */
          + ((c << 5) | (c >>> 27)) + 0x5a827999 /* K03 */ + b;
        a = (i4 =  input[(offset+=10)-4] << 24
                | (input[ offset     -3] & 0xff) << 16
                | (input[ offset     -2] & 0xff) << 8
                | (input[ offset     -1] & 0xff)) /* W04 */
          + ((((d = (d << 30) | (d >>> 2)) ^ e) & c) ^ e) /* Ch(c,d,e) */
          + ((b << 5) | (b >>> 27)) + 0x5a827999 /* K04 */ + a;
        e = (i5 =  input[ offset       ] << 24
                | (input[ offset     +1] & 0xff) << 16
                | (input[ offset     +2] & 0xff) << 8
                | (input[ offset     +3] & 0xff)) /* W05 */
          + ((((c = (c << 30) | (c >>> 2)) ^ d) & b) ^ d) /* Ch(b,c,d) */
          + ((a << 5) | (a >>> 27)) + 0x5a827999 /* K05 */ + e;
        d = (i6 =  input[ offset     +4] << 24
                | (input[ offset     +5] & 0xff) << 16
                | (input[(offset+=10)-4] & 0xff) << 8
                | (input[ offset     -3] & 0xff)) /* W06 */
          + ((((b = (b << 30) | (b >>> 2)) ^ c) & a) ^ c) /* Ch(a,b,c) */
          + ((e << 5) | (e >>> 27)) + 0x5a827999 /* K06 */ + d;
        c = (i7 =  input[ offset     -2] << 24
                | (input[ offset     -1] & 0xff) << 16
                | (input[ offset       ] & 0xff) << 8
                | (input[ offset     +1] & 0xff)) /* W07 */
          + ((((a = (a << 30) | (a >>> 2)) ^ b) & e) ^ b) /* Ch(e,a,b) */
          + ((d << 5) | (d >>> 27)) + 0x5a827999 /* K07 */ + c;
        b = (i8 =  input[ offset     +2] << 24
                | (input[ offset     +3] & 0xff) << 16
                | (input[ offset     +4] & 0xff) << 8
                | (input[ offset     +5] & 0xff)) /* W08 */
          + ((((e = (e << 30) | (e >>> 2)) ^ a) & d) ^ a) /* Ch(d,e,a) */
          + ((c << 5) | (c >>> 27)) + 0x5a827999 /* K08 */ + b;
        a = (i9 =  input[(offset+=10)-4] << 24
                | (input[ offset     -3] & 0xff) << 16
                | (input[ offset     -2] & 0xff) << 8
                | (input[ offset     -1] & 0xff)) /* W09 */
          + ((((d = (d << 30) | (d >>> 2)) ^ e) & c) ^ e) /* Ch(c,d,e) */
          + ((b << 5) | (b >>> 27)) + 0x5a827999 /* K09 */ + a;
        e = (iA =  input[ offset       ] << 24
                | (input[ offset     +1] & 0xff) << 16
                | (input[ offset     +2] & 0xff) << 8
                | (input[ offset     +3] & 0xff)) /* W10 */
          + ((((c = (c << 30) | (c >>> 2)) ^ d) & b) ^ d) /* Ch(b,c,d) */
          + ((a << 5) | (a >>> 27)) + 0x5a827999 /* K10 */ + e;
        d = (iB =  input[ offset     +4] << 24
                | (input[ offset     +5] & 0xff) << 16
                | (input[(offset+=10)-4] & 0xff) << 8
                | (input[ offset     -3] & 0xff)) /* W11 */
          + ((((b = (b << 30) | (b >>> 2)) ^ c) & a) ^ c) /* Ch(a,b,c) */
          + ((e << 5) | (e >>> 27)) + 0x5a827999 /* K11 */ + d;
        c = (iC =  input[ offset     -2] << 24
                | (input[ offset     -1] & 0xff) << 16
                | (input[ offset       ] & 0xff) << 8
                | (input[ offset     +1] & 0xff)) /* W12 */
          + ((((a = (a << 30) | (a >>> 2)) ^ b) & e) ^ b) /* Ch(e,a,b) */
          + ((d << 5) | (d >>> 27)) + 0x5a827999 /* K12 */ + c;
        b = (iD =  input[ offset     +2] << 24
                | (input[ offset     +3] & 0xff) << 16
                | (input[ offset     +4] & 0xff) << 8
                | (input[ offset     +5] & 0xff)) /* W13 */
          + ((((e = (e << 30) | (e >>> 2)) ^ a) & d) ^ a) /* Ch(d,e,a) */
          + ((c << 5) | (c >>> 27)) + 0x5a827999 /* K13 */ + b;
        a = (iE =  input[(offset+=10)-4] << 24
                | (input[ offset     -3] & 0xff) << 16
                | (input[ offset     -2] & 0xff) << 8
                | (input[ offset     -1] & 0xff)) /* W14 */
          + ((((d = (d << 30) | (d >>> 2)) ^ e) & c) ^ e) /* Ch(c,d,e) */
          + ((b << 5) | (b >>> 27)) + 0x5a827999 /* K14 */ + a;
        e = (iF =  input[ offset       ] << 24
                | (input[ offset     +1] & 0xff) << 16
                | (input[ offset     +2] & 0xff) << 8
                | (input[ offset     +3] & 0xff)) /* W15 */
          + ((((c = (c << 30) | (c >>> 2)) ^ d) & b) ^ d) /* Ch(b,c,d) */
          + ((a << 5) | (a >>> 27)) + 0x5a827999 /* K15 */ + e;
        /* Second round, on scheduled input (steps 16..31). */
        d = (i0 = ((i0 = i0 ^ i2 ^ i8 ^ iD) << 1) | (i0 >>> 31)) /* W16 */
          + ((((b = (b << 30) | (b >>> 2)) ^ c) & a) ^ c) /* Ch(a,b,c) */
          + ((e << 5) | (e >>> 27)) + 0x5a827999 /* K16 */ + d;
        c = (i1 = ((i1 = i1 ^ i3 ^ i9 ^ iE) << 1) | (i1 >>> 31)) /* W17 */
          + ((((a = (a << 30) | (a >>> 2)) ^ b) & e) ^ b) /* Ch(e,a,b) */
          + ((d << 5) | (d >>> 27)) + 0x5a827999 /* K17 */ + c;
        b = (i2 = ((i2 = i2 ^ i4 ^ iA ^ iF) << 1) | (i2 >>> 31)) /* W18 */
          + ((((e = (e << 30) | (e >>> 2)) ^ a) & d) ^ a) /* Ch(d,e,a) */
          + ((c << 5) | (c >>> 27)) + 0x5a827999 /* K18 */ + b;
        a = (i3 = ((i3 = i3 ^ i5 ^ iB ^ i0) << 1) | (i3 >>> 31)) /* W19 */
          + ((((d = (d << 30) | (d >>> 2)) ^ e) & c) ^ e) /* Ch(c,d,e) */
          + ((b << 5) | (b >>> 27)) + 0x5a827999 /* K19 */ + a;
        /* Use digest schedule function Parity (steps 20..39):
         *   Parity(x,y,z) = y ^ x ^ z,
         * and K20 = .... = K39 = 0x6ed9eba1. */
        e = (i4 = ((i4 = i4 ^ i6 ^ iC ^ i1) << 1) | (i4 >>> 31)) /* W20 */
          + ((c = (c << 30) | (c >>> 2)) ^ b ^ d) /* Parity(b,c,d) */
          + ((a << 5) | (a >>> 27)) + 0x6ed9eba1 /* K20 */ + e;
        d = (i5 = ((i5 = i5 ^ i7 ^ iD ^ i2) << 1) | (i5 >>> 31)) /* W21 */
          + ((b = (b << 30) | (b >>> 2)) ^ a ^ c) /* Parity(a,b,c) */
          + ((e << 5) | (e >>> 27)) + 0x6ed9eba1 /* K21 */ + d;
        c = (i6 = ((i6 = i6 ^ i8 ^ iE ^ i3) << 1) | (i6 >>> 31)) /* W22 */
          + ((a = (a << 30) | (a >>> 2)) ^ e ^ b) /* Parity(e,a,b) */
          + ((d << 5) | (d >>> 27)) + 0x6ed9eba1 /* K22 */ + c;
        b = (i7 = ((i7 = i7 ^ i9 ^ iF ^ i4) << 1) | (i7 >>> 31)) /* W23 */
          + ((e = (e << 30) | (e >>> 2)) ^ d ^ a) /* Parity(d,e,a) */
          + ((c << 5) | (c >>> 27)) + 0x6ed9eba1 /* K23 */ + b;
        a = (i8 = ((i8 = i8 ^ iA ^ i0 ^ i5) << 1) | (i8 >>> 31)) /* W24 */
          + ((d = (d << 30) | (d >>> 2)) ^ c ^ e) /* Parity(c,d,e) */
          + ((b << 5) | (b >>> 27)) + 0x6ed9eba1 /* K24 */ + a;
        e = (i9 = ((i9 = i9 ^ iB ^ i1 ^ i6) << 1) | (i9 >>> 31)) /* W25 */
          + ((c = (c << 30) | (c >>> 2)) ^ b ^ d) /* Parity(b,c,d) */
          + ((a << 5) | (a >>> 27)) + 0x6ed9eba1 /* K25 */ + e;
        d = (iA = ((iA = iA ^ iC ^ i2 ^ i7) << 1) | (iA >>> 31)) /* W26 */
          + ((b = (b << 30) | (b >>> 2)) ^ a ^ c) /* Parity(a,b,c) */
          + ((e << 5) | (e >>> 27)) + 0x6ed9eba1 /* K26 */ + d;
        c = (iB = ((iB = iB ^ iD ^ i3 ^ i8) << 1) | (iB >>> 31)) /* W27 */
          + ((a = (a << 30) | (a >>> 2)) ^ e ^ b) /* Parity(e,a,b) */
          + ((d << 5) | (d >>> 27)) + 0x6ed9eba1 /* K27 */ + c;
        b = (iC = ((iC = iC ^ iE ^ i4 ^ i9) << 1) | (iC >>> 31)) /* W28 */
          + ((e = (e << 30) | (e >>> 2)) ^ d ^ a) /* Parity(d,e,a) */
          + ((c << 5) | (c >>> 27)) + 0x6ed9eba1 /* K28 */ + b;
        a = (iD = ((iD = iD ^ iF ^ i5 ^ iA) << 1) | (iD >>> 31)) /* W29 */
          + ((d = (d << 30) | (d >>> 2)) ^ c ^ e) /* Parity(c,d,e) */
          + ((b << 5) | (b >>> 27)) + 0x6ed9eba1 /* K29 */ + a;
        e = (iE = ((iE = iE ^ i0 ^ i6 ^ iB) << 1) | (iE >>> 31)) /* W30 */
          + ((c = (c << 30) | (c >>> 2)) ^ b ^ d) /* Parity(b,c,d) */
          + ((a << 5) | (a >>> 27)) + 0x6ed9eba1 /* K30 */ + e;
        d = (iF = ((iF = iF ^ i1 ^ i7 ^ iC) << 1) | (iF >>> 31)) /* W31 */
          + ((b = (b << 30) | (b >>> 2)) ^ a ^ c) /* Parity(a,b,c) */
          + ((e << 5) | (e >>> 27)) + 0x6ed9eba1 /* K31 */ + d;
        /* Third round, on scheduled input (steps 32..47). */
        c = (i0 = ((i0 = i0 ^ i2 ^ i8 ^ iD) << 1) | (i0 >>> 31)) /* W32 */
          + ((a = (a << 30) | (a >>> 2)) ^ e ^ b) /* Parity(e,a,b) */
          + ((d << 5) | (d >>> 27)) + 0x6ed9eba1 /* K32 */ + c;
        b = (i1 = ((i1 = i1 ^ i3 ^ i9 ^ iE) << 1) | (i1 >>> 31)) /* W33 */
          + ((e = (e << 30) | (e >>> 2)) ^ d ^ a) /* Parity(d,e,a) */
          + ((c << 5) | (c >>> 27)) + 0x6ed9eba1 /* K33 */ + b;
        a = (i2 = ((i2 = i2 ^ i4 ^ iA ^ iF) << 1) | (i2 >>> 31)) /* W34 */
          + ((d = (d << 30) | (d >>> 2)) ^ c ^ e) /* Parity(c,d,e) */
          + ((b << 5) | (b >>> 27)) + 0x6ed9eba1 /* K34 */ + a;
        e = (i3 = ((i3 = i3 ^ i5 ^ iB ^ i0) << 1) | (i3 >>> 31)) /* W35 */
          + ((c = (c << 30) | (c >>> 2)) ^ b ^ d) /* Parity(b,c,d) */
          + ((a << 5) | (a >>> 27)) + 0x6ed9eba1 /* K35 */ + e;
        d = (i4 = ((i4 = i4 ^ i6 ^ iC ^ i1) << 1) | (i4 >>> 31)) /* W36 */
          + ((b = (b << 30) | (b >>> 2)) ^ a ^ c) /* Parity(a,b,c) */
          + ((e << 5) | (e >>> 27)) + 0x6ed9eba1 /* K36 */ + d;
        c = (i5 = ((i5 = i5 ^ i7 ^ iD ^ i2) << 1) | (i5 >>> 31)) /* W37 */
          + ((a = (a << 30) | (a >>> 2)) ^ e ^ b) /* Parity(e,a,b) */
          + ((d << 5) | (d >>> 27)) + 0x6ed9eba1 /* K37 */ + c;
        b = (i6 = ((i6 = i6 ^ i8 ^ iE ^ i3) << 1) | (i6 >>> 31)) /* W38 */
          + ((e = (e << 30) | (e >>> 2)) ^ d ^ a) /* Parity(d,e,a) */
          + ((c << 5) | (c >>> 27)) + 0x6ed9eba1 /* K38 */ + b;
        a = (i7 = ((i7 = i7 ^ i9 ^ iF ^ i4) << 1) | (i7 >>> 31)) /* W39 */
          + ((d = (d << 30) | (d >>> 2)) ^ c ^ e) /* Parity(c,d,e) */
          + ((b << 5) | (b >>> 27)) + 0x6ed9eba1 /* K39 */ + a;
        /* Use hash schedule function Maj (steps 40..59):
         *   Maj(x,y,z) = (x&y) ^ (x&z) ^ (y&z) = ((y | x) & z) | (y & x),
         * and K40 = .... = K59 = 0x8f1bbcdc. */
        e = (i8 = ((i8 = i8 ^ iA ^ i0 ^ i5) << 1) | (i8 >>> 31)) /* W40 */
          + ((((c = (c << 30) | (c >>> 2)) | b) & d) | (c & b)) /* Maj(b,c,d) */
          + ((a << 5) | (a >>> 27)) + 0x8f1bbcdc /* K40 */ + e;
        d = (i9 = ((i9 = i9 ^ iB ^ i1 ^ i6) << 1) | (i9 >>> 31)) /* W41 */
          + ((((b = (b << 30) | (b >>> 2)) | a) & c) | (b & a)) /* Maj(a,b,c) */
          + ((e << 5) | (e >>> 27)) + 0x8f1bbcdc /* K41 */ + d;
        c = (iA = ((iA = iA ^ iC ^ i2 ^ i7) << 1) | (iA >>> 31)) /* W42 */
          + ((((a = (a << 30) | (a >>> 2)) | e) & b) | (a & e)) /* Maj(e,a,b) */
          + ((d << 5) | (d >>> 27)) + 0x8f1bbcdc /* K42 */ + c;
        b = (iB = ((iB = iB ^ iD ^ i3 ^ i8) << 1) | (iB >>> 31)) /* W43 */
          + ((((e = (e << 30) | (e >>> 2)) | d) & a) | (e & d)) /* Maj(d,e,a) */
          + ((c << 5) | (c >>> 27)) + 0x8f1bbcdc /* K43 */ + b;
        a = (iC = ((iC = iC ^ iE ^ i4 ^ i9) << 1) | (iC >>> 31)) /* W44 */
          + ((((d = (d << 30) | (d >>> 2)) | c) & e) | (d & c)) /* Maj(c,d,e) */
          + ((b << 5) | (b >>> 27)) + 0x8f1bbcdc /* K44 */ + a;
        e = (iD = ((iD = iD ^ iF ^ i5 ^ iA) << 1) | (iD >>> 31)) /* W45 */
          + ((((c = (c << 30) | (c >>> 2)) | b) & d) | (c & b)) /* Maj(b,c,d) */
          + ((a << 5) | (a >>> 27)) + 0x8f1bbcdc /* K45 */ + e;
        d = (iE = ((iE = iE ^ i0 ^ i6 ^ iB) << 1) | (iE >>> 31)) /* W46 */
          + ((((b = (b << 30) | (b >>> 2)) | a) & c) | (b & a)) /* Maj(a,b,c) */
          + ((e << 5) | (e >>> 27)) + 0x8f1bbcdc /* K46 */ + d;
        c = (iF = ((iF = iF ^ i1 ^ i7 ^ iC) << 1) | (iF >>> 31)) /* W47 */
          + ((((a = (a << 30) | (a >>> 2)) | e) & b) | (a & e)) /* Maj(e,a,b) */
          + ((d << 5) | (d >>> 27)) + 0x8f1bbcdc /* K47 */ + c;
        /* Fourth round, on scheduled input (steps 48..63). */
        b = (i0 = ((i0 = i0 ^ i2 ^ i8 ^ iD) << 1) | (i0 >>> 31)) /* W48 */
          + ((((e = (e << 30) | (e >>> 2)) | d) & a) | (d & e)) /* Maj(d,e,a) */
          + ((c << 5) | (c >>> 27)) + 0x8f1bbcdc /* K48 */ + b;
        a = (i1 = ((i1 = i1 ^ i3 ^ i9 ^ iE) << 1) | (i1 >>> 31)) /* W49 */
          + ((((d = (d << 30) | (d >>> 2)) | c) & e) | (c & d)) /* Maj(c,d,e) */
          + ((b << 5) | (b >>> 27)) + 0x8f1bbcdc /* K49 */ + a;
        e = (i2 = ((i2 = i2 ^ i4 ^ iA ^ iF) << 1) | (i2 >>> 31)) /* W50 */
          + ((((c = (c << 30) | (c >>> 2)) | b) & d) | (b & c)) /* Maj(b,c,d) */
          + ((a << 5) | (a >>> 27)) + 0x8f1bbcdc /* K50 */ + e;
        d = (i3 = ((i3 = i3 ^ i5 ^ iB ^ i0) << 1) | (i3 >>> 31)) /* W51 */
          + ((((b = (b << 30) | (b >>> 2)) | a) & c) | (a & b)) /* Maj(a,b,c) */
          + ((e << 5) | (e >>> 27)) + 0x8f1bbcdc /* K51 */ + d;
        c = (i4 = ((i4 = i4 ^ i6 ^ iC ^ i1) << 1) | (i4 >>> 31)) /* W52 */
          + ((((a = (a << 30) | (a >>> 2)) | e) & b) | (e & a)) /* Maj(e,a,b) */
          + ((d << 5) | (d >>> 27)) + 0x8f1bbcdc /* K52 */ + c;
        b = (i5 = ((i5 = i5 ^ i7 ^ iD ^ i2) << 1) | (i5 >>> 31)) /* W53 */
          + ((((e = (e << 30) | (e >>> 2)) | d) & a) | (d & e)) /* Maj(d,e,a) */
          + ((c << 5) | (c >>> 27)) + 0x8f1bbcdc /* K53 */ + b;
        a = (i6 = ((i6 = i6 ^ i8 ^ iE ^ i3) << 1) | (i6 >>> 31)) /* W54 */
          + ((((d = (d << 30) | (d >>> 2)) | c) & e) | (c & d)) /* Maj(c,d,e) */
          + ((b << 5) | (b >>> 27)) + 0x8f1bbcdc /* K54 */ + a;
        e = (i7 = ((i7 = i7 ^ i9 ^ iF ^ i4) << 1) | (i7 >>> 31)) /* W55 */
          + ((((c = (c << 30) | (c >>> 2)) | b) & d) | (b & c)) /* Maj(b,c,d) */
          + ((a << 5) | (a >>> 27)) + 0x8f1bbcdc /* K55 */ + e;
        d = (i8 = ((i8 = i8 ^ iA ^ i0 ^ i5) << 1) | (i8 >>> 31)) /* W56 */
          + ((((b = (b << 30) | (b >>> 2)) | a) & c) | (a & b)) /* Maj(a,b,c) */
          + ((e << 5) | (e >>> 27)) + 0x8f1bbcdc /* K56 */ + d;
        c = (i9 = ((i9 = i9 ^ iB ^ i1 ^ i6) << 1) | (i9 >>> 31)) /* W57 */
          + ((((a = (a << 30) | (a >>> 2)) | e) & b) | (e & a)) /* Maj(e,a,b) */
          + ((d << 5) | (d >>> 27)) + 0x8f1bbcdc /* K57 */ + c;
        b = (iA = ((iA = iA ^ iC ^ i2 ^ i7) << 1) | (iA >>> 31)) /* W58 */
          + ((((e = (e << 30) | (e >>> 2)) | d) & a) | (d & e)) /* Maj(d,e,a) */
          + ((c << 5) | (c >>> 27)) + 0x8f1bbcdc /* K58 */ + b;
        a = (iB = ((iB = iB ^ iD ^ i3 ^ i8) << 1) | (iB >>> 31)) /* W59 */
          + ((((d = (d << 30) | (d >>> 2)) | c) & e) | (c & d)) /* Maj(c,d,e) */
          + ((b << 5) | (b >>> 27)) + 0x8f1bbcdc /* K59 */ + a;
        /* Use hash schedule function Parity (steps 60..79):
         *   Parity(x,y,z) = y ^ x ^ z,
         * and K60 = .... = K79 = 0xca62c1d6. */
        e = (iC = ((iC = iC ^ iE ^ i4 ^ i9) << 1) | (iC >>> 31)) /* W60 */
          + ((c = (c << 30) | (c >>> 2)) ^ b ^ d) /* Parity(b,c,d) */
          + ((a << 5) | (a >>> 27)) + 0xca62c1d6 /* K60 */ + e;
        d = (iD = ((iD = iD ^ iF ^ i5 ^ iA) << 1) | (iD >>> 31)) /* W61 */
          + ((b = (b << 30) | (b >>> 2)) ^ a ^ c) /* Parity(a,b,c) */
          + ((e << 5) | (e >>> 27)) + 0xca62c1d6 /* K61 */ + d;
        c = (iE = ((iE = iE ^ i0 ^ i6 ^ iB) << 1) | (iE >>> 31)) /* W62 */
          + ((a = (a << 30) | (a >>> 2)) ^ e ^ b) /* Parity(e,a,b) */
          + ((d << 5) | (d >>> 27)) + 0xca62c1d6 /* K62 */ + c;
        b = (iF = ((iF = iF ^ i1 ^ i7 ^ iC) << 1) | (iF >>> 31)) /* W63 */
          + ((e = (e << 30) | (e >>> 2)) ^ d ^ a) /* Parity(d,e,a) */
          + ((c << 5) | (c >>> 27)) + 0xca62c1d6 /* K63 */ + b;
        /* Fifth round, on scheduled input (steps 64..79). */
        a = (i0 = ((i0 = i0 ^ i2 ^ i8 ^ iD) << 1) | (i0 >>> 31)) /* W64 */
          + ((d = (d << 30) | (d >>> 2)) ^ c ^ e) /* Parity(c,d,e) */
          + ((b << 5) | (b >>> 27)) + 0xca62c1d6 /* K64 */ + a;
        e = (i1 = ((i1 = i1 ^ i3 ^ i9 ^ iE) << 1) | (i1 >>> 31)) /* W65 */
          + ((c = (c << 30) | (c >>> 2)) ^ b ^ d) /* Parity(b,c,d) */
          + ((a << 5) | (a >>> 27)) + 0xca62c1d6 /* K65 */ + e;
        d = (i2 = ((i2 = i2 ^ i4 ^ iA ^ iF) << 1) | (i2 >>> 31)) /* W66 */
          + ((b = (b << 30) | (b >>> 2)) ^ a ^ c) /* Parity(a,b,c) */
          + ((e << 5) | (e >>> 27)) + 0xca62c1d6 /* K66 */ + d;
        c = (i3 = ((i3 = i3 ^ i5 ^ iB ^ i0) << 1) | (i3 >>> 31)) /* W67 */
          + ((a = (a << 30) | (a >>> 2)) ^ e ^ b) /* Parity(e,a,b) */
          + ((d << 5) | (d >>> 27)) + 0xca62c1d6 /* K67 */ + c;
        b = (i4 = ((i4 = i4 ^ i6 ^ iC ^ i1) << 1) | (i4 >>> 31)) /* W68 */
          + ((e = (e << 30) | (e >>> 2)) ^ d ^ a) /* Parity(d,e,a) */
          + ((c << 5) | (c >>> 27)) + 0xca62c1d6 /* K68 */ + b;
        a = (i5 = ((i5 = i5 ^ i7 ^ iD ^ i2) << 1) | (i5 >>> 31)) /* W69 */
          + ((d = (d << 30) | (d >>> 2)) ^ c ^ e) /* Parity(c,d,e) */
          + ((b << 5) | (b >>> 27)) + 0xca62c1d6 /* K69 */ + a;
        e = (i6 = ((i6 = i6 ^ i8 ^ iE ^ i3) << 1) | (i6 >>> 31)) /* W70 */
          + ((c = (c << 30) | (c >>> 2)) ^ b ^ d) /* Parity(b,c,d) */
          + ((a << 5) | (a >>> 27)) + 0xca62c1d6 /* K70 */ + e;
        d = (i7 = ((i7 = i7 ^ i9 ^ iF ^ i4) << 1) | (i7 >>> 31)) /* W71 */
          + ((b = (b << 30) | (b >>> 2)) ^ a ^ c) /* Parity(a,b,c) */
          + ((e << 5) | (e >>> 27)) + 0xca62c1d6 /* K71 */ + d;
        c = (i8 = ((i8 = i8 ^ iA ^ i0 ^ i5) << 1) | (i8 >>> 31)) /* W72 */
          + ((a = (a << 30) | (a >>> 2)) ^ e ^ b) /* Parity(e,a,b) */
          + ((d << 5) | (d >>> 27)) + 0xca62c1d6 /* K72 */ + c;
        b = (i9 = ((i9 = i9 ^ iB ^ i1 ^ i6) << 1) | (i9 >>> 31)) /* W73 */
          + ((e = (e << 30) | (e >>> 2)) ^ d ^ a) /* Parity(d,e,a) */
          + ((c << 5) | (c >>> 27)) + 0xca62c1d6 /* K73 */ + b;
        a = (iA = ((iA = iA ^ iC ^ i2 ^ i7) << 1) | (iA >>> 31)) /* W74 */
          + ((d = (d << 30) | (d >>> 2)) ^ c ^ e) /* Parity(c,d,e) */
          + ((b << 5) | (b >>> 27)) + 0xca62c1d6 /* K74 */ + a;
        e = (iB = ((iB = iB ^ iD ^ i3 ^ i8) << 1) | (iB >>> 31)) /* W75 */
          + ((c = (c << 30) | (c >>> 2)) ^ b ^ d) /* Parity(b,c,d) */
          + ((a << 5) | (a >>> 27)) + 0xca62c1d6 /* K75 */ + e;
        d = (iC = ((iC = iC ^ iE ^ i4 ^ i9) << 1) | (iC >>> 31)) /* W76 */
          + ((b = (b << 30) | (b >>> 2)) ^ a ^ c) /* Parity(a,b,c) */
          + ((e << 5) | (e >>> 27)) + 0xca62c1d6 /* K76 */ + d;
        c = (iD = ((iD = iD ^ iF ^ i5 ^ iA) << 1) | (iD >>> 31)) /* W77 */
          + ((a = (a << 30) | (a >>> 2)) ^ e ^ b) /* Parity(e,a,b) */
          + ((d << 5) | (d >>> 27)) + 0xca62c1d6 /* K77 */ + c;
        /* Terminate the last two steps of fifth round,
         * feeding the final digest on the fly. */
        hB = (b =
            (iE = ((iE = iE ^ i0 ^ i6 ^ iB) << 1) | (iE >>> 31)) /* W78 */
          + ((e = (e << 30) | (e >>> 2)) ^ d ^ a) // Parity(d,e,a) */
          + ((c << 5) | (c >>> 27)) + 0xca62c1d6 /* K78 */ + b) + hB;
        hA = (/* a = */
            (iF = ((iF = iF ^ i1 ^ i7 ^ iC) << 1) | (iF >>> 31)) /* W79 */
          + ((d = (d << 30) | (d >>> 2)) ^ c ^ e) /* Parity(c,d,e) */
          + ((b << 5) | (b >>> 27)) + 0xca62c1d6 /* K79 */ + a) + hA;
        hE = e + hE;
        hD = d + hD;
        hC = (/* c = */ (c << 30) | (c >>> 2)) + hC;
    }
}