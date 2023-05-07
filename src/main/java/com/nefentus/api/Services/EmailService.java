package com.nefentus.api.Services;

import lombok.extern.slf4j.Slf4j;
import microsoft.exchange.webservices.data.autodiscover.IAutodiscoverRedirectionUrl;
import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion;
import microsoft.exchange.webservices.data.core.service.item.EmailMessage;
import microsoft.exchange.webservices.data.credential.ExchangeCredentials;
import microsoft.exchange.webservices.data.credential.WebCredentials;
import microsoft.exchange.webservices.data.property.complex.MessageBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
@Slf4j
public class EmailService {

    private ExchangeService service;
    @Value("${spring.mail.username}")
    private String defaultEmail;
    @Value("${spring.mail.password}")
    private String defaultEmailPass;

    @PostConstruct
    public void initEmailService() {
        try {
            log.info("Init connect to host mail...");
            service = new ExchangeService(ExchangeVersion.Exchange2010_SP2);
            ExchangeCredentials credentials = new WebCredentials(defaultEmail, defaultEmailPass);
            service.setCredentials(credentials);
            service.autodiscoverUrl(defaultEmail, new RedirectionUrlCallback());
        } catch (Exception e) {
            log.error("Error init connect to email host", e);
        }
    }

    static class RedirectionUrlCallback implements IAutodiscoverRedirectionUrl {
        public boolean autodiscoverRedirectionUrlValidationCallback(
                String redirectionUrl) {
            return redirectionUrl.toLowerCase().startsWith("https://");
        }
    }

    @Async
    public void sendEmail(String toEmail, String subject, String body) {
        log.info("Start send email to {}", toEmail);
        EmailMessage msg;
        try {
            msg = new EmailMessage(service);
            msg.setSubject(subject);
            msg.setBody(MessageBody.getMessageBodyFromText(body));
            msg.getToRecipients().add(toEmail);
            msg.send();
        } catch (Exception e) {
            log.error("Email error", e);
        }
    }
}
