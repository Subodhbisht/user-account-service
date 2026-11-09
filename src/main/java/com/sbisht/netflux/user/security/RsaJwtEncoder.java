package com.sbisht.netflux.user.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtEncodingException;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@link JwtEncoder} producing compact JWS tokens signed with RS256 (SHA256withRSA).
 */
public class RsaJwtEncoder implements JwtEncoder {

    private static final String RS256 = SignatureAlgorithm.RS256.getName();
    private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();

    private final RSAPrivateKey privateKey;
    private final JsonMapper jsonMapper;

    public RsaJwtEncoder(RSAPrivateKey privateKey, JsonMapper jsonMapper) {
        this.privateKey = privateKey;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public Jwt encode(JwtEncoderParameters parameters) {
        var headers = buildHeaders(parameters);
        var claims = parameters.getClaims().getClaims();

        var signingInput = encodeJson(headers) + "." + encodeJson(toJsonClaims(claims));
        var tokenValue = signingInput + "." + BASE64_URL.encodeToString(sign(signingInput));

        return Jwt.withTokenValue(tokenValue)
                .headers(jwtHeaders -> jwtHeaders.putAll(headers))
                .claims(jwtClaims -> jwtClaims.putAll(claims))
                .build();
    }

    private static Map<String, Object> buildHeaders(JwtEncoderParameters parameters) {
        var headers = new LinkedHashMap<String, Object>();
        if (Objects.nonNull(parameters.getJwsHeader())) {
            headers.putAll(parameters.getJwsHeader().getHeaders());
        }
        var algorithm = headers.getOrDefault("alg", SignatureAlgorithm.RS256);
        if (!RS256.equals(algorithmName(algorithm))) {
            throw new JwtEncodingException("Unsupported JWS algorithm: " + algorithm);
        }
        headers.put("alg", RS256);
        headers.putIfAbsent("typ", "JWT");
        return headers;
    }

    private static String algorithmName(Object algorithm) {
        return algorithm instanceof SignatureAlgorithm signatureAlgorithm
                ? signatureAlgorithm.getName()
                : String.valueOf(algorithm);
    }

    private static Map<String, Object> toJsonClaims(Map<String, Object> claims) {
        return claims.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> toJsonValue(entry.getValue()),
                        (first, second) -> second,
                        LinkedHashMap::new
                ));
    }

    private static Object toJsonValue(Object value) {
        return value instanceof Instant instant ? instant.getEpochSecond() : value;
    }

    private String encodeJson(Map<String, Object> value) {
        return BASE64_URL.encodeToString(jsonMapper.writeValueAsBytes(value));
    }

    private byte[] sign(String signingInput) {
        try {
            var signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(signingInput.getBytes(StandardCharsets.US_ASCII));
            return signature.sign();
        } catch (GeneralSecurityException ex) {
            throw new JwtEncodingException("Failed to sign JWT", ex);
        }
    }
}
