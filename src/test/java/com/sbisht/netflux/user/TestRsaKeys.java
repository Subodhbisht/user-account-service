package com.sbisht.netflux.user;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

public final class TestRsaKeys {

    private static final Path PRIVATE_KEY_PATH = Path.of("target", "test-rsa-private-key.pem");
    private static final KeyPair KEY_PAIR = generateKeyPair();

    private TestRsaKeys() {
    }

    public static RSAPublicKey publicKey() {
        return (RSAPublicKey) KEY_PAIR.getPublic();
    }

    public static String writePrivateKeyPem() {
        var encoded = Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(KEY_PAIR.getPrivate().getEncoded());
        var pem = "-----BEGIN PRIVATE KEY-----\n" + encoded + "\n-----END PRIVATE KEY-----\n";
        try {
            Files.createDirectories(PRIVATE_KEY_PATH.getParent());
            Files.writeString(PRIVATE_KEY_PATH, pem);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return PRIVATE_KEY_PATH.toAbsolutePath().toString();
    }

    private static KeyPair generateKeyPair() {
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
