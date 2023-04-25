package com.nefentus.api.Services;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class HtmlProvider {

    public static String loadHtmlFile(String token) throws IOException {
        ClassPathResource resource = new ClassPathResource("verify-account.html");
        InputStream inputStream = resource.getInputStream();
        try {
            return StreamUtils.copyToString(inputStream, Charset.defaultCharset()).replace("/StaticLoginLinkToChange", "https://nefentus.com/login?token=" + token);
        } finally {
            inputStream.close();
        }
    }

    public static String loadHtmlFileReset(String token) throws IOException {
        ClassPathResource resource = new ClassPathResource("resetPassword.html");
        InputStream inputStream = resource.getInputStream();
        try {
            return StreamUtils.copyToString(inputStream, Charset.defaultCharset()).replace("/StaticLoginLinkToChange", "https://nefentus.com/reset-password?token=" + token);
        } finally {
            inputStream.close();
        }
    }

    public static String loadResetTokenMail(String token) throws IOException {
        ClassPathResource resource = new ClassPathResource("passwordToken.html");
        InputStream inputStream = resource.getInputStream();
        try {
            return StreamUtils.copyToString(inputStream, Charset.defaultCharset()).replace("/StaticLoginLinkToChange", token);
        } finally {
            inputStream.close();
        }
    }

}
