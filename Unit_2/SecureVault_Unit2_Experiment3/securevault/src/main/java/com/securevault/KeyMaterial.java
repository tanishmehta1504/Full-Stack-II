package com.securevault;

import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Separate keys for signing and encryption. Local keys survive application restarts. */
@Component
public class KeyMaterial {
    final byte[] jwt;
    final byte[] aes;
    public KeyMaterial(@Value("${app.keys-dir}") String directory,
                       @Value("${app.jwt-key}") String jwtKey, @Value("${app.aes-key}") String aesKey) throws Exception {
        Path dir = Path.of(directory);
        if (jwtKey.isBlank() || aesKey.isBlank()) {
            Files.createDirectories(dir);
            permissions(dir, "rwx------");
        }
        jwt = load(dir.resolve("jwt.key"), jwtKey);
        aes = load(dir.resolve("aes.key"), aesKey);
    }
    private byte[] load(Path path, String supplied) throws Exception {
        if (!supplied.isBlank()) return decode(supplied);
        if (!Files.exists(path)) {
            byte[] bytes = new byte[32]; new SecureRandom().nextBytes(bytes);
            try {
                Files.createFile(path, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
            } catch (UnsupportedOperationException e) { Files.createFile(path); }
            Files.writeString(path, Base64.getEncoder().encodeToString(bytes));
        }
        permissions(path, "rw-------");
        return decode(Files.readString(path).trim());
    }
    private byte[] decode(String input) {
        byte[] bytes = Base64.getDecoder().decode(input);
        if (bytes.length != 32) throw new IllegalArgumentException("Keys must be Base64-encoded 32-byte values");
        return bytes;
    }
    private void permissions(Path path, String mode) throws Exception {
        try { Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(mode)); }
        catch (UnsupportedOperationException ignored) { /* Windows: protect the directory with NTFS permissions. */ }
    }
}
