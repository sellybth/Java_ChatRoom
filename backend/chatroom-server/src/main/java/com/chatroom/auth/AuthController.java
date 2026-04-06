package com.chatroom.auth;

import com.chatroom.user.User;
import com.chatroom.user.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository,
                          JwtUtil jwtUtil,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    // POST /api/auth/register
    // Anyone can call this — no token needed
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {

        // Check if email already exists
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.status(400)
                .body(Map.of("error", "Email already registered"));
        }

        // Hash the password before saving — NEVER store plain text
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Save to Neon DB
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Registration successful"));
    }

    // POST /api/auth/login
    // Anyone can call this — no token needed
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {

        String email = request.get("email");
        String password = request.get("password");

        // Find user by email
        Optional<User> userOpt = userRepository.findByEmail(email);

        // Email not found
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401)
                .body(Map.of("error", "Invalid email or password"));
        }

        User user = userOpt.get();

        // Compare entered password with hashed password in DB
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(401)
                .body(Map.of("error", "Invalid email or password"));
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getUserId(), user.getEmail());

        // Return token + basic user info to Swing app
        return ResponseEntity.ok(Map.of(
            "token", token,
            "userId", user.getUserId(),
            "name", user.getName()
        ));
    }
}