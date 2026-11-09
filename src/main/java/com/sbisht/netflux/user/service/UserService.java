package com.sbisht.netflux.user.service;

import com.sbisht.netflux.user.dto.LoginRequest;
import com.sbisht.netflux.user.dto.LoginResponse;
import com.sbisht.netflux.user.dto.RegisterRequest;
import com.sbisht.netflux.user.exceptions.InvalidCredentialsException;
import com.sbisht.netflux.user.mapper.UserMapper;
import com.sbisht.netflux.user.repository.UserRepository;
import com.sbisht.netflux.user.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    @Transactional
    public void register(RegisterRequest request) {
        userRepository.save(UserMapper.toEntity(request, passwordEncoder.encode(request.password())));
    }

    public LoginResponse login(LoginRequest request) {
        var principal = authenticate(request);
        return new LoginResponse(
                tokenService.generateToken(principal.id()),
                principal.name()
        );
    }

    private UserPrincipal authenticate(LoginRequest request) {
        try {
            var authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
            return (UserPrincipal) authentication.getPrincipal();
        } catch (BadCredentialsException ex) {
            throw new InvalidCredentialsException();
        }
    }
}
