package com.example.nippou.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.nippou.model.User;
import com.example.nippou.repository.UserRepository;
import com.example.nippou.service.MailService;

import jakarta.mail.MessagingException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final MailService mailService;

    @Value("${app.base-url}")
    private String baseUrl;

    public AuthController(UserRepository userRepository, MailService mailService) {
        this.userRepository = userRepository;
        this.mailService = mailService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @PostMapping("/register")
    public String register(@RequestBody RegisterRequest request) throws MessagingException {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            return "既に登録済みのメールアドレスです。";
        }

        String hashedPassword = passwordEncoder.encode(request.password());
        String verificationToken = UUID.randomUUID().toString();

        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(request.email());
        user.setPasswordHash(hashedPassword);
        user.setVerified(false);
        user.setVerificationToken(verificationToken);
        userRepository.save(user);

        String verifyLink = baseUrl + "/api/auth/verify?token=" + verificationToken;
        mailService.sendEmail(user.getEmail(), "メール認証", "以下のリンクをクリックして認証を完了してください:\n" + verifyLink);

        return "認証メールを送信しました";
    }

    public record RegisterRequest(String email, String password) {

    }

    @GetMapping("/verify")
    public String verifyUser(@RequestParam String token) {
        var userOpt = userRepository.findByVerificationToken(token);
        if (userOpt.isEmpty()) {
            return "無効なトークンです。";
        }

        User user = userOpt.get();
        user.setVerified(true);
        user.setVerificationToken(null); // 一度きりのトークンにする
        userRepository.save(user);

        return "認証が完了しました。ログインしてください。";
    }

}
