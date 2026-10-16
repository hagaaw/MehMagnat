package com.example.application.config;

import java.util.Random;

public class AppConfig {
    public static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    public static final Random random = new Random();
    public static final Integer len = 15;

    public static String generateRandomString(int size) {
        StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
