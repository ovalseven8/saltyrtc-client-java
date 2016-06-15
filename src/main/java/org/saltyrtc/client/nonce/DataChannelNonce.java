/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.nonce;

import java.nio.ByteBuffer;

/**
 * A SaltyRTC data channel nonce.
 *
 * Nonce structure:
 *
 * |CCCCCCCCCCCCCCCC|II|OO|QQQQ|
 *
 * - C: Cookie (16 byte)
 * - I: Data channel ID (2 byte)
 * - O: Overflow number (2 byte)
 * - Q: Sequence number (4 byte)
 */
public class DataChannelNonce extends Nonce {

    private int channelId;

    /**
     * Create a new nonce.
     *
     * Note that due to the lack of unsigned data types in Java, we'll use
     * larger signed types. That means that the user must check that the values
     * are in the correct range. If the arguments are out of range, an
     * unsigned `IllegalArgumentException` is thrown.
     *
     * See also: http://stackoverflow.com/a/397997/284318.
     */
    public DataChannelNonce(byte[] cookie, int channelId, int overflow, long sequence) {
        validateCookie(cookie);
        validateChannelId(channelId);
        validateOverflow(overflow);
        validateSequence(sequence);
        this.cookie = cookie;
        this.channelId = channelId;
        this.overflow = overflow;
        this.sequence = sequence;
    }

    /**
     * Create a new nonce from raw binary data.
     */
    public DataChannelNonce(ByteBuffer buf) {
        if (buf.limit() < TOTAL_LENGTH) {
            throw new IllegalArgumentException("Buffer limit must be at least " + TOTAL_LENGTH);
        }

        final byte[] cookie = new byte[COOKIE_LENGTH];
        buf.get(cookie, 0, COOKIE_LENGTH);
        validateCookie(cookie);

        final int channelId = ((int)buf.getShort()) & 0x0000FFFF;
        validateChannelId(channelId);

        final int overflow = ((int)buf.getShort()) & 0x0000FFFF;
        validateOverflow(overflow);

        final long sequence = ((long)buf.getInt()) & 0x00000000FFFFFFFFL;
        validateSequence(sequence);

        this.cookie = cookie;
        this.channelId = channelId;
        this.overflow = overflow;
        this.sequence = sequence;
    }

    /**
     * A channel id should be an uint16.
     */
    private void validateChannelId(int channelId) {
        if (channelId < 0 || channelId >= (1 << 16)) {
            throw new IllegalArgumentException("channelId must be between 0 and 2**16-1");
        }
    }

    /**
     * Return the channel id.
     */
    public int getChannelId() {
        return this.channelId;
    }

}