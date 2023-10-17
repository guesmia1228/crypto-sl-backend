package com.nefentus.api.Services;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class ContextProvider {
    public static Context loadHtmlFile(String token, String appUrl) throws IOException {
        Context context = new Context();
        context.setVariable("title", "Verify account");
        context.setVariable("headline", "Congratulations! Your account is ready.");
        context.setVariable("text", "Please click on the following button to verify your account.");
        context.setVariable("buttontext", "Verify");        
        context.setVariable("buttonlink", appUrl + token);

        return context;
    }

    public static Context loadHtmlFileReset(String token, String appUrl) throws IOException {
        Context context = new Context();
        context.setVariable("title", "Reset Password");
        context.setVariable("headline", "You requested a password reset!");
        context.setVariable("text", "Please click on the following link to reset your password!");
        context.setVariable("buttontext", "Reset password");        
        context.setVariable("buttonlink", appUrl + token);

        return context;
    }

    public static Context loadHtmlEmailChange(String token) throws IOException {
        Context context = new Context();
        context.setVariable("title", "Change Email");
        context.setVariable("headline", "You requested a email change!");
        context.setVariable("text", "Here is your code to confirm email change!");
        context.setVariable("buttontext", token);        

        return context;
    }

    public static Context loadResetTokenMail(String token) throws IOException {
        Context context = new Context();
        context.setVariable("title", "Reset Password");
        context.setVariable("headline", "You requested to change your password!");
        context.setVariable("text", "Here is your code to change your password:");
        context.setVariable("buttontext", token);        

        return context;
    }

    public static Context loadOTPPasswordMail(String code) {
        Context context = new Context();
        context.setVariable("title", "One-Time Password");
        context.setVariable("headline", "Confirm login to your Nefentus account");
        context.setVariable("text", "Your confirmation code is below. Enter it in the web browser to log in to your account.");
        context.setVariable("buttontext", code);

        return context;
    }
}
