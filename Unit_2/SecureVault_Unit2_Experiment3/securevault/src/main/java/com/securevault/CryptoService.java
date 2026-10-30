package com.securevault;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

/** AES-256-GCM authenticates ciphertext and binds it to its owner and record ID. */
@Service
public class CryptoService {
    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();
    public CryptoService(KeyMaterial material) { key = new SecretKeySpec(material.aes, "AES"); }
    public String encrypt(String plaintext, String context) {
        try {
            byte[] nonce = new byte[12]; random.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, nonce));
            cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return "v1:" + Base64.getEncoder().encodeToString(ByteBuffer.allocate(12 + ciphertext.length).put(nonce).put(ciphertext).array());
        } catch (Exception e) { throw new IllegalStateException("Encryption failed", e); }
    }
    public String decrypt(String value, String context) {
        try {
            if (!value.startsWith("v1:")) throw new IllegalArgumentException("Unknown encryption version");
            ByteBuffer buffer = ByteBuffer.wrap(Base64.getDecoder().decode(value.substring(3)));
            byte[] nonce = new byte[12]; buffer.get(nonce);
            byte[] ciphertext = new byte[buffer.remaining()]; buffer.get(ciphertext);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, nonce));
            cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) { throw new IllegalStateException("Credential integrity check failed", e); }
    }
}
