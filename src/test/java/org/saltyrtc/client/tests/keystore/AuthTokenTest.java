/*
 * Copyright (c) 2016-2018 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.tests.keystore;

import org.junit.Before;
import org.junit.Test;
import org.saltyrtc.client.crypto.CryptoException;
import org.saltyrtc.client.crypto.CryptoProvider;
import org.saltyrtc.client.crypto.JnaclCryptoProvider;
import org.saltyrtc.client.exceptions.InvalidKeyException;
import org.saltyrtc.client.keystore.AuthToken;
import org.saltyrtc.client.keystore.Box;

import java.security.SecureRandom;

import static org.junit.Assert.*;

public class AuthTokenTest {

    private AuthToken at;
    private SecureRandom random = new SecureRandom();

    @Before
    public void setUp() {
        this.at = new AuthToken(new JnaclCryptoProvider());
    }

    @Test
    public void testEncrypt() throws CryptoException {
        final byte[] in = "hello".getBytes();
        final byte[] nonce = new byte[CryptoProvider.NONCEBYTES];
        this.random.nextBytes(nonce);
        final Box box = this.at.encrypt(in, nonce);
        assertEquals(nonce, box.getNonce());
        assertNotEquals(in, box.getData());
    }

    @Test
    public void testDecrypt() throws InvalidKeyException, CryptoException {
        // The following values have been generated by logging the values in the encrypt test.
        final byte[] authToken = {-112, 35, -83, -54, 12, 59, -73, 114, 38, -63, 58, -38, -19, -57, -95, -102, -18, -38, 5, 13, -125, -55, 87, 7, 11, -123, 31, -86, 109, -35, 5, 87, };
        final byte[] nonce = {-87, -88, -82, 13, 100, -66, -61, 29, 26, -15, 104, -71, 9, 31, -18, -56, 23, -127, -21, 58, 18, -54, 107, 94, };
        final byte[] encrypted = {85, 79, -97, 53, -9, -64, -105, 64, -89, 52, -100, -122, -55, -71, 54, 102, -83, 15, -6, -78, 15, };
        // Decrypt
        this.at = new AuthToken(new JnaclCryptoProvider(), authToken);
        final Box box = new Box(nonce, encrypted);
        final byte[] decrypted = this.at.decrypt(box);
        assertArrayEquals("hello".getBytes(), decrypted);
    }

    @Test(expected= CryptoException.class)
    public void testDecryptFails() throws CryptoException {
        // Encrypt data
        final byte[] in = "hello".getBytes();
        final byte[] nonce = new byte[CryptoProvider.NONCEBYTES];
        this.random.nextBytes(nonce);
        final Box box = this.at.encrypt(in, nonce);

        // Decrypt successfully
        try {
            final byte[] decrypted1 = this.at.decrypt(box);
            assertArrayEquals("hello".getBytes(), decrypted1);
        } catch (CryptoException e) {
            fail("Decryption failed, but shouldn't");
        }

        // Now decrypt with wrong keys
        this.at = new AuthToken(new JnaclCryptoProvider());
        // This will now fail with a CryptoException
        this.at.decrypt(box);
    }

    @Test(expected=InvalidKeyException.class)
    public void testInvalidKey() throws InvalidKeyException {
        final byte[] token = {42};
        new AuthToken(new JnaclCryptoProvider(), token);
    }

}
