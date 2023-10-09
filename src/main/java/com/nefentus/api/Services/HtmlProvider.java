package com.nefentus.api.Services;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class HtmlProvider {
    public static String loadHtmlFile(String token, String appUrl) throws IOException {
        ClassPathResource resource = new ClassPathResource("verify-account.html");
        try (InputStream inputStream = resource.getInputStream()) {
            return StreamUtils.copyToString(inputStream, Charset.defaultCharset()).replace("/StaticLoginLinkToChange", appUrl + token);
        }
    }

    public static String loadHtmlFileReset(String token, String appUrl) throws IOException {
        ClassPathResource resource = new ClassPathResource("resetPassword.html");
        InputStream inputStream = resource.getInputStream();
        try {
            return StreamUtils.copyToString(inputStream, Charset.defaultCharset()).replace("/StaticLoginLinkToChange", appUrl + token);
        } finally {
            inputStream.close();
        }
    }

    public static String loadHtmlEmailChange(String token) throws IOException {
        ClassPathResource resource = new ClassPathResource("changeEmail.html");
        InputStream inputStream = resource.getInputStream();
        try {
            return StreamUtils.copyToString(inputStream, Charset.defaultCharset()).replace("/StaticLoginLinkToChange", token);
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
