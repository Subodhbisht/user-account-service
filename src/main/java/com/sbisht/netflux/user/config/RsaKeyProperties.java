package com.sbisht.netflux.user.config;

import jakarta.validation.constraints.NotNull;
import java.security.interfaces.RSAPrivateKey;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("rsa")
public record RsaKeyProperties(@NotNull RSAPrivateKey privateKey) {
}
