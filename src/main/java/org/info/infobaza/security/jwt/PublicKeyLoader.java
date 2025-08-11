package org.info.infobaza.security.jwt;


import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class PublicKeyLoader {

    public static PublicKey loadPublicKey(String publicKeyPEM) {
        String publicKeyPEMStart = "-----BEGIN PUBLIC KEY-----";
        String publicKeyPEMEnd = "-----END PUBLIC KEY-----";

        publicKeyPEM = publicKeyPEM
                .replace(publicKeyPEMStart, "")
                .replace(publicKeyPEMEnd, "")
                .replaceAll("\\s+", "");

        byte[] decodedBytes = Base64.getDecoder().decode(publicKeyPEM);
        try {
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load public key", e);
        }
    }
}