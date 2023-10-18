package com.nefentus.api.Services;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

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
        context.setVariable("text",
                "Your confirmation code is below. Enter it in the web browser to log in to your account.");
        context.setVariable("buttontext", code);

        return context;
    }

    public static Context loadSanctionEmail(String name, String email, String phone, String country)
            throws IOException {
        Context context = new Context();
        context.setVariable("title", "Person in sanction list");
        context.setVariable("headline", "Person in sanction list tried to register!");
        context.setVariable("text", "This person that is in EU sanction list tried to register to Nefentus:");
        context.setVariable("subtitle", name);

        List<String> contents = new ArrayList<>();
        contents.add("Email: " + email);
        contents.add("Phone: " + phone);
        contents.add("Country: " + country);

        context.setVariable("subcontents", contents);

        return context;
    }

    public static Context loadSanctionEmailOnUpdate(String name, String email, String phone, String country,
            String business) throws IOException {
        Context context = new Context();
        context.setVariable("title", "Person in sanction list");
        context.setVariable("headline", "Turns out that " + name + " is in sanction list!");
        context.setVariable("text", "Turns out that Nefentus user " + name
                + " is in EU sanction list! User was blocked. If there was an error, you can always unblock him.");
        context.setVariable("subtitle", name);

        List<String> contents = new ArrayList<>();
        contents.add("Email: " + email);
        contents.add("Phone: " + phone);
        contents.add("Business: " + business);
        contents.add("Country: " + country);

        context.setVariable("subcontents", contents);

        return context;
    }
}
