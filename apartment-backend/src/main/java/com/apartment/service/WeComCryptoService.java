package com.apartment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * WeChat Work (企业微信) message encryption/decryption and signature verification service.
 *
 * Implements the WeChat Work callback encryption scheme:
 * - AESKey = Base64Decode(EncodingAESKey + "=") → 32-byte AES key
 * - IV = first 16 bytes of AESKey
 * - Encryption: random(16B) + msg_len(4B, network order) + msg + CorpID → AES-256-CBC → Base64
 * - Signature: SHA1(sort([token, timestamp, nonce, encrypted_msg]))
 *
 * Reference: https://developer.work.weixin.qq.com/document/path/90930
 */
@Service
public class WeComCryptoService {

    @Value("${wecom.corpid}")
    private String corpId;

    @Value("${wecom.token}")
    private String token;

    @Value("${wecom.encoding-aes-key}")
    private String encodingAesKey;

    private byte[] aesKey;
    private IvParameterSpec ivSpec;

    /**
     * Initialize AES key and IV from EncodingAESKey.
     * EncodingAESKey is 43 chars, Base64 decode with appended "=" gives 32 bytes.
     */
    private void initKeys() {
        if (aesKey != null) return;
        aesKey = Base64.getDecoder().decode(encodingAesKey + "=");
        ivSpec = new IvParameterSpec(Arrays.copyOfRange(aesKey, 0, 16));
    }

    /**
     * Verify signature: SHA1(sort([token, timestamp, nonce, encryptedMsg])) == msgSignature
     */
    public boolean verifySignature(String msgSignature, String timestamp, String nonce, String encryptedMsg) {
        String calculated = calculateSignature(timestamp, nonce, encryptedMsg);
        return calculated.equals(msgSignature);
    }

    /**
     * Calculate signature: SHA1(sort([token, timestamp, nonce, encryptedMsg]))
     */
    public String calculateSignature(String timestamp, String nonce, String encryptedMsg) {
        String[] arr = new String[]{token, timestamp, nonce, encryptedMsg};
        Arrays.sort(arr);
        StringBuilder sb = new StringBuilder();
        for (String s : arr) {
            sb.append(s);
        }
        return sha1(sb.toString());
    }

    /**
     * Decrypt an encrypted message (echostr or Encrypt field).
     * Returns the plaintext msg content.
     */
    public String decrypt(String encryptedMsg) {
        initKeys();
        try {
            byte[] encrypted = Base64.getDecoder().decode(encryptedMsg);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"), ivSpec);
            byte[] decrypted = cipher.doFinal(encrypted);

            // Format: random(16B) + msg_len(4B) + msg + receiveid
            // msg_len is in network byte order (big endian)
            int msgLen = ((decrypted[16] & 0xFF) << 24) |
                         ((decrypted[17] & 0xFF) << 16) |
                         ((decrypted[18] & 0xFF) << 8) |
                         (decrypted[19] & 0xFF);

            int msgStart = 20;
            int msgEnd = msgStart + msgLen;

            String msg = new String(decrypted, msgStart, msgLen, StandardCharsets.UTF_8);

            // receiveid follows msg, should match corpId
            // int receiveIdStart = msgEnd;
            // String receiveId = new String(decrypted, receiveIdStart, decrypted.length - receiveIdStart, StandardCharsets.UTF_8);

            return msg;
        } catch (Exception e) {
            throw new RuntimeException("WeChat Work message decryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Encrypt a plaintext message for passive reply.
     * Format: random(16B) + msg_len(4B) + msg + CorpID → AES-256-CBC → Base64
     */
    public String encrypt(String plaintextMsg) {
        initKeys();
        try {
            byte[] randomBytes = generateRandomBytes(16);
            byte[] msgBytes = plaintextMsg.getBytes(StandardCharsets.UTF_8);
            byte[] msgLenBytes = intToNetworkBytes(msgBytes.length);
            byte[] corpIdBytes = corpId.getBytes(StandardCharsets.UTF_8);

            // Concatenate: random + msg_len + msg + corpId
            byte[] plaintext = new byte[randomBytes.length + msgLenBytes.length + msgBytes.length + corpIdBytes.length];
            System.arraycopy(randomBytes, 0, plaintext, 0, randomBytes.length);
            System.arraycopy(msgLenBytes, 0, plaintext, randomBytes.length, msgLenBytes.length);
            System.arraycopy(msgBytes, 0, plaintext, randomBytes.length + msgLenBytes.length, msgBytes.length);
            System.arraycopy(corpIdBytes, 0, plaintext, randomBytes.length + msgLenBytes.length + msgBytes.length, corpIdBytes.length);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(aesKey, "AES"), ivSpec);
            byte[] encrypted = cipher.doFinal(plaintext);

            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("WeChat Work message encryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Generate a full encrypted response XML for passive reply messages.
     */
    public String generateEncryptedResponse(String plaintextMsg, String timestamp, String nonce) {
        String encryptedMsg = encrypt(plaintextMsg);
        String signature = calculateSignature(timestamp, nonce, encryptedMsg);
        return "<xml>\n" +
               "<Encrypt><![CDATA[" + encryptedMsg + "]]></Encrypt>\n" +
               "<MsgSignature><![CDATA[" + signature + "]]></MsgSignature>\n" +
               "<TimeStamp>" + timestamp + "</TimeStamp>\n" +
               "<Nonce><![CDATA[" + nonce + "]]></Nonce>\n" +
               "</xml>";
    }

    private String sha1(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA1 calculation failed", e);
        }
    }

    private byte[] intToNetworkBytes(int value) {
        return new byte[]{
            (byte) ((value >> 24) & 0xFF),
            (byte) ((value >> 16) & 0xFF),
            (byte) ((value >> 8) & 0xFF),
            (byte) (value & 0xFF)
        };
    }

    private byte[] generateRandomBytes(int length) {
        byte[] bytes = new byte[length];
        new java.security.SecureRandom().nextBytes(bytes);
        return bytes;
    }
}