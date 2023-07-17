package com.nefentus.api.Services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import javax.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.InternetAddress;

@Service
@Slf4j
public class EmailService {
	@Value("${spring.mail.username}")
	private String username;

	@Autowired
	private JavaMailSender mailSender;

	@PostConstruct
	public void initEmailService() {
		try {
			MimeMessage message = mailSender.createMimeMessage();
		} catch (Exception e) {
			log.error("Error init connect to email host", e);
		}
	}

	@Async
	public void sendEmail(String toEmail, String subject, String body) {
		MimeMessage message = mailSender.createMimeMessage();

		try {
			message.setFrom(new InternetAddress(username, "Nefentus"));
			message.setRecipients(MimeMessage.RecipientType.TO, InternetAddress.parse(toEmail));
			message.setSubject(subject);
			message.setContent(body, "text/html; charset=utf-8");
			mailSender.send(message);
		} catch (Exception e) {
			log.error("Email error", e);
			return;
		}

		log.info("Email sent successfully to {}", toEmail);
	}
}
