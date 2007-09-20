/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2006) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.genepattern.server.user.User;

public class EncryptionUtil {
    private static Logger log = Logger.getLogger(EncryptionUtil.class);
    /**
     * Property name for the key used to lookup the dynamically generated key for mapping a
     */
    public static final String PROP_PIPELINE_USER_KEY = "pipeline.user.key";

    private EncryptionUtil() {
    }

    private static EncryptionUtil instance = null;
    public static EncryptionUtil getInstance() {
        if (instance == null) {
            instance = new EncryptionUtil();
        }
        return instance;
    }

    /**
     * Map a system generated key to a user's encrypted password.
     */
    private Map<String,byte[]> pipelineUserKeys = new HashMap<String,byte[]>();

    /**
     * Save the user's encrypted password so that is is retrievable via the generated key.
     * Generates a unique key and stores the user's encrypted password into system memory.
     * The intention is remove this key and associated value from memory when it is no longer needed.
     * This is so that nested pipelines will work when user passwords are required.
     * 
     * @param user
     * @return the generated key.
     */
    public String pushPipelineUserKey(User user) {
        String key = user.getUserId() + "." + System.currentTimeMillis();
        try {
            byte[] encryptedKey = encrypt(key);
            key = convertToString( encryptedKey );
        }
        catch (NoSuchAlgorithmException e) {
            log.error("Warning: unable to encrypt system generated key", e);
        }
        pipelineUserKeys.put(key, user.getPassword());
        return key;
    }

    /**
     * Retrieve the user's encrypted password given the key generated by pushPipelineUserKey.
     * @param key
     * @return a user's encrypted password
     * 
     * @see {@link #pushPipelineUserKey(User)}
     */
    public byte[] getPipelineUserEncryptedPassword(String key) {
        return pipelineUserKeys.get(key);
    }

    /**
     * Remove the user's encrypted password associated with the given key.
     * @param key generated by {@link #pushPipelineUserKey(User)}
     */
    public void removePipelineUserKey(String key) {
        Object val = pipelineUserKeys.remove(key);
        if (val == null) {
            log.error("Unknown key in removePipelineUserKey: "+key);
        }
    }

    /**
     * Encrypts the clear text. The returned byte array is exactly 255 bytes,
     * the size of the password field.
     * 
     * @param clearText
     *            the unencrypted text
     * @return The encrypted text as a byte array
     * @throws NoSuchAlgorithmException
     */
    public static byte[] encrypt(String clearText)
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.reset();
        try {
            md.update(clearText.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            log.error(e);
        }

        byte[] encryptedString = new byte[255];
        Arrays.fill(encryptedString, (byte) 0);
        byte[] digestedString = md.digest();
        for (int i = 0, length = Math.min(encryptedString.length,
                digestedString.length); i < length; i++) {
            encryptedString[i] = digestedString[i];
        }
        return encryptedString;
    }

    /**
     * Encode the given array of byte as a String to be passed as an argument 
     * on the java command line.
     * 
     * e.g.
     * Given byte[] b;
     * b equals convertToByteArray(convertToString(b));
     * 
     * @param byte_arr
     * @return a String which can be passed as a command line argument.
     * 
     * @author pcarr
     */
    public static String convertToString(final byte[] byte_arr) {
        BigInteger bigInteger = new BigInteger(byte_arr);
        return bigInteger.toString(16);        
    }
    
    /**
     * Convert the given String representation back to is byte array.
     * @param arg
     * @return a byte array matching exactly 
     * 
     * @author pcarr
     */
    public static byte[] convertToByteArray(String arg) {
        BigInteger bi = new BigInteger(arg, 16);
        return bi.toByteArray();
    }

    public static void main(String[] args) throws Exception {
        String txt = "a";
        byte[] a1 = EncryptionUtil.encrypt(txt);
        byte[] a2 = EncryptionUtil.encrypt(txt);
        System.out.println(a1.length);
        System.out.println(java.util.Arrays.equals(a1, a2));
    }

}
