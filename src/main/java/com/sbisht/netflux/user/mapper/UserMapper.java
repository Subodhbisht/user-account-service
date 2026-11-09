package com.sbisht.netflux.user.mapper;

import com.sbisht.netflux.user.dto.RegisterRequest;
import com.sbisht.netflux.user.entity.User;
import com.sbisht.netflux.user.security.UserPrincipal;

public class UserMapper {

    private UserMapper() {
    }

    public static User toEntity(RegisterRequest request, String encodedPassword) {
        var user = new User();
        user.setUsername(request.username());
        user.setPassword(encodedPassword);
        user.setName(request.name());
        return user;
    }

    public static UserPrincipal toPrincipal(User user) {
        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getName()
        );
    }
}
