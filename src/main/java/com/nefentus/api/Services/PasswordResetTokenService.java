package com.nefentus.api.Services;

import com.nefentus.api.entities.PasswordResetToken;
import com.nefentus.api.entities.User;
import com.nefentus.api.repositories.PasswordResetTokenRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Random;

@Service
@Getter
@AllArgsConstructor
public class PasswordResetTokenService {
    private PasswordResetTokenRepository resetTokenRepository;


    public PasswordResetToken createPasswordResetTokenForUser(User user, String newPassword) {
        String token = generateToken();
        LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(1);
        PasswordResetToken passwordResetToken = new PasswordResetToken(null, user, token, expiryDate, newPassword);
        return resetTokenRepository.save(passwordResetToken);
    }

    private String generateToken() {
        int length = 6; // Die Länge des Tokens
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"; // Die Zeichen, aus denen das Token generiert wird
        Random random = new SecureRandom();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            builder.append(characters.charAt(index));
        }
        return builder.toString();
    }


}
