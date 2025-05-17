package com.example.nippou.controller;

import java.time.Duration;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.nippou.model.User;
import com.example.nippou.repository.UserRepository;
import com.example.nippou.service.MailService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AuthController {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final MailService mailService;

    @Value("${jwt.secret}")
    private String secretKey;

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
        user.setName(request.name());
        user.setTeam(request.team());
        user.setEmail(request.email());
        user.setPasswordHash(hashedPassword);
        user.setVerified(false);
        user.setVerificationToken(verificationToken);
        userRepository.save(user);

        String verifyLink = baseUrl + "/api/auth/verify?token=" + verificationToken;
        mailService.sendEmail(user.getEmail(), "メール認証", "以下のリンクをクリックして認証を完了してください:\n" + verifyLink);

        return "認証メールを送信しました";
    }

    @GetMapping("/verify")
    public String verifyUser(@RequestParam String token) {
        var userOpt = userRepository.findByVerificationToken(token);
        if (userOpt.isEmpty()) {
            return "無効なトークンです。";
        }

        User user = userOpt.get();
        user.setVerified(true);
        user.setVerificationToken(null);
        userRepository.save(user);

        return "認証が完了しました。ログインしてください。";
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        Optional<User> userOpt = userRepository.findByEmail(request.email());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("メールアドレスまたはパスワードが間違っています");
        }

        User user = userOpt.get();

        if (!user.isVerified()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("メール認証が完了していません");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("メールアドレスまたはパスワードが間違っています");
        }

        // JWT発行
        String jwt = Jwts.builder()
                .setSubject(user.getId())
                .claim("name", user.getName())
                .claim("email", user.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();

        // Cookie設定
        ResponseCookie cookie = ResponseCookie.from("auth_token", jwt)
                .httpOnly(true)
                .secure(false) // 本番では true にする
                .path("/")
                .maxAge(Duration.ofDays(1))
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(
                new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getTeam())
        );

    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe(@CookieValue(name = "auth_token", required = false) String token) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("未ログインです");
        }

        try {
            var claims = Jwts.parser()
                    .setSigningKey(secretKey)
                    .parseClaimsJws(token)
                    .getBody();

            String userId = claims.getSubject();
            Optional<User> userOpt = userRepository.findById(userId);

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("無効なユーザーです");
            }

            User user = userOpt.get();
            return ResponseEntity.ok(
                    new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getTeam())
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("認証エラー");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        // JWTを消去するために空のCookieを設定（maxAge=0）
        ResponseCookie cookie = ResponseCookie.from("auth_token", "")
                .httpOnly(true)
                .secure(false) // 本番は true
                .path("/")
                .maxAge(0) //  これでCookie削除
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok("ログアウトしました");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) throws MessagingException {
        Optional<User> userOpt = userRepository.findByEmail(request.email());

        if (userOpt.isEmpty()) {
            // セキュリティのため存在の有無は返さない
            return ResponseEntity.ok("パスワード再設定メールを送信しました");
        }

        User user = userOpt.get();
        String resetToken = UUID.randomUUID().toString();

        user.setResetPasswordToken(resetToken);
        userRepository.save(user);
        System.out.println("★ resetToken = " + resetToken);

        String resetLink = "http://localhost:3000" + "/reset-password?token=" + resetToken;
        mailService.sendEmail(user.getEmail(), "パスワード再設定", "以下のリンクから新しいパスワードを設定してください:\n" + resetLink);

        return ResponseEntity.ok("パスワード再設定メールを送信しました");
    }

    @GetMapping("/verify-reset-token")
    public ResponseEntity<?> verifyResetToken(@RequestParam String token) {
        System.out.println("🔎 reset token = " + token);
        Optional<User> userOpt = userRepository.findByResetPasswordToken(token);

        if (userOpt.isEmpty()) {
            System.out.println("❌ ユーザーが見つかりませんでした");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("無効なトークンです");
        }

        return ResponseEntity.ok("有効なトークンです");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        Optional<User> userOpt = userRepository.findByResetPasswordToken(request.token());

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("無効なトークンです");
        }

        User user = userOpt.get();
        String hashedPassword = passwordEncoder.encode(request.newPassword());

        user.setPasswordHash(hashedPassword);
        user.setResetPasswordToken(null); // トークンは使い捨て
        userRepository.save(user);

        return ResponseEntity.ok("パスワードを再設定しました");
    }

    public record ResetPasswordRequest(String token, String newPassword) {

    }

    public record ForgotPasswordRequest(String email) {

    }

    public record RegisterRequest(String email, String password, String name, String team) {

    }

    public record LoginRequest(String email, String password) {

    }

    public record UserResponse(String id, String name, String email, String team) {

    }

}
