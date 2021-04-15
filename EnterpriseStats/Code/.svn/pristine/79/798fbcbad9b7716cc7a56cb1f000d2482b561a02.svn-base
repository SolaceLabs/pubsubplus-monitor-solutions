/**
 * Copyright 2016-2017 Solace Corporation. All rights reserved.
 *
 * http://www.solace.com
 *
 * This source is distributed under the terms and conditions of any contract or
 * contracts between Solace Corporation ("Solace") and you or your company. If
 * there are no contracts in place use of this source is not authorized. No
 * support is provided and no distribution, sharing with others or re-use of 
 * this source is authorized unless specifically stated in the contracts 
 * referred to above.
 *
 * This software is custom built to specifications provided by you, and is 
 * provided under a paid service engagement or statement of work signed between
 * you and Solace. This product is provided as is and is not supported by 
 * Solace unless such support is provided for under an agreement signed between
 * you and Solace.
 */
package com.solace.psg.util;

import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author dlangayan
 *
 */
public class AESTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testEncryptDecryptWithKey() throws Exception {
        String plainText = "testPlainText";
        String encryptionKey = "d2u3l5p5s3g2k3e0";
        byte[] cipher = AES.encrypt(plainText, encryptionKey);
        String decrypted = AES.decrypt(cipher, encryptionKey);

        assertEquals(plainText, decrypted);
    }

    @Test
    public void testEncryptNullKey() throws Exception {
        thrown.expect(EncryptionException.class);
        String plainText = "testPlainText";
        String encryptionKey = null;
        @SuppressWarnings("unused")
        byte[] cipher = AES.encrypt(plainText, encryptionKey);
    }

    @Test
    public void testDecryptNullKey() throws Exception {
        thrown.expect(EncryptionException.class);
        byte[] cipher = "someCipher".getBytes();
        String encryptionKey = null;
        @SuppressWarnings("unused")
        String decrypted = AES.decrypt(cipher, encryptionKey);
    }

    @Test
    public void testDecryptNullCipher() throws Exception {
        thrown.expect(EncryptionException.class);
        byte[] cipher = null;
        String encryptionKey = "d2u3l5p5s3g2k3e0";
        @SuppressWarnings("unused")
        String decrypted = AES.decrypt(cipher, encryptionKey);
    }

    @Test
    public void testEncryptDecryptString() throws Exception {
        String plainText = "testPlainText";
        String cipher = AES.encrypt(plainText);
        String decrypted = AES.decrypt(cipher);

        assertEquals(plainText, decrypted);
    }
}
