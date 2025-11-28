package com.nexus.core.controller;

import com.nexus.core.model.User;
import com.nexus.core.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody User user) throws IOException {
        if (user.getUsername() == null || user.getUsername().isBlank())
            return ResponseEntity.badRequest().body("Username cannot be blank");
        // Strong password check
        if (!userService.isStrongPassword(user.getPassword()))
            return ResponseEntity.badRequest().body("Password too weak! Must contain letters, numbers, and special chars.");

        boolean added = userService.addUser(user);
        if (added)
            return ResponseEntity.ok("User registered successfully");
        else
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Username already exists");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody User user) throws IOException {
        // Check if already logged in?
        boolean ok = userService.login(user.getUsername(), user.getPassword());
        if (ok)
            return ResponseEntity.ok("Login successful");
        else
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestParam String username) {
        if (userService.isLoggedIn(username)) {
            userService.logout(username);
            return ResponseEntity.ok("Logged out successfully");
        }
        else
            return ResponseEntity.badRequest().body("User is not logged in");
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteUser(@RequestBody User user) throws IOException {
        boolean deleted = userService.deleteUser(user.getUsername(), user.getPassword());
        if (deleted)
            return ResponseEntity.ok("User deleted successfully");
        else
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Deletion failed. Check credentials.");
    }
}