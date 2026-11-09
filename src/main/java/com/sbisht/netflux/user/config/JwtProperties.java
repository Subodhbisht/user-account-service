package com.sbisht.netflux.user.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("jwt")
public record JwtProperties(@DefaultValue("1h") Duration expiration) {
}
