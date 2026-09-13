package com.cadence.service;

import com.cadence.dto.AuthResponse;
import com.cadence.dto.LoginRequest;
import com.cadence.dto.RegisterRequest;
import com.cadence.entity.Role;
import com.cadence.entity.User;
import com.cadence.repository.RoleRepository;
import com.cadence.repository.UserRepository;
import com.cadence.security.JwtService;
import com.cadence.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            // Caught by GlobalExceptionHandler and turned into a 409.
            throw new IllegalStateException("An account with this email already exists");
        }

        Role role = roleRepository.findByRoleName(request.getRole())
                .orElseThrow(() -> new IllegalStateException(
                        "Role not seeded: " + request.getRole() + " - check DataSeeder ran on startup"));

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();

        userRepository.save(user);

        UserPrincipal principal = new UserPrincipal(user);
        String token = jwtService.generateToken(principal);

        return new AuthResponse(token, user.getUserId(), user.getName(), user.getEmail(), role.getRoleName().name());
    }

    public AuthResponse login(LoginRequest request) {
        // Delegates to the DaoAuthenticationProvider wired in SecurityConfig -
        // this is what actually checks the password against the BCrypt hash.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalStateException("User not found after successful authentication"));

        UserPrincipal principal = new UserPrincipal(user);
        String token = jwtService.generateToken(principal);

        return new AuthResponse(token, user.getUserId(), user.getName(), user.getEmail(),
                user.getRole().getRoleName().name());
    }
}
