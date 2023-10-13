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

    public static String loadOTPPasswordMail(String code) throws IOException {
        ClassPathResource resource = new ClassPathResource("otpPassword.html");
        InputStream inputStream = resource.getInputStream();
        try {
            return StreamUtils.copyToString(inputStream, Charset.defaultCharset()).replaceAll("OTP_CODE", code);
        } finally {
            inputStream.close();
        }
    }


    public static String loadSanctionEmail(String name, String email, String phone, String country) throws IOException {
        ClassPathResource resource = new ClassPathResource("sanctionSignup.html");
        InputStream inputStream = resource.getInputStream();
        try {
            return StreamUtils.copyToString(inputStream, Charset.defaultCharset()).replace("/StaticLoginLinkToChange", name).replace("/StaticEmail", email).replace("/StaticPhone", phone).replace("/StaticCountry", country);
        } finally {
            inputStream.close();
        }
    }

    public static String loadSanctionEmailOnUpdate(String name, String email, String phone, String country, String business) throws IOException {
        ClassPathResource resource = new ClassPathResource("sanctionUpdate.html");
        InputStream inputStream = resource.getInputStream();
        try {
            return StreamUtils.copyToString(inputStream, Charset.defaultCharset()).replace("/StaticLoginLinkToChange", name).replace("/StaticEmail", email).replace("/StaticPhone", phone).replace("/StaticCountry", country).replace("/StaticBusiness", business);
        } finally {
            inputStream.close();
        }
    }
}
