package com.nefentus.api.Services;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class EmailHashGenerator {
    public static String generateHash(String email) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(email.getBytes());
            byte[] bytes = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().substring(0, 8); // returns first 8 characters of hex string
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }
    }
}

