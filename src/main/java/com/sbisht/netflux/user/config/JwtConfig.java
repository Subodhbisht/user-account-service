package com.sbisht.netflux.user.config;

import com.sbisht.netflux.user.security.RsaJwtEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class JwtConfig {

    @Bean
    public JwtEncoder jwtEncoder(RsaKeyProperties properties, JsonMapper jsonMapper) {
        return new RsaJwtEncoder(properties.privateKey(), jsonMapper);
    }
}
