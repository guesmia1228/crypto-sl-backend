package com.nefentus.api.Services;

import com.nefentus.api.entities.User;
import com.nefentus.api.repositories.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import javax.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.InternetAddress;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
	@Value("${spring.mail.username}")
	private String username;

	@Autowired
	private JavaMailSender mailSender;

	private final UserRepository userRepository;

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

		Optional<User> userOptional = userRepository.findUserByEmail(toEmail);
		if (userOptional.isPresent()) {
			log.error("User with email " + toEmail + " exists!");
			var user = userOptional.get();
			log.error("Anti Phishing Code: " + user.getAntiPhishingCode());

			if (user.getAntiPhishingCode() == null || user.getAntiPhishingCode().isEmpty()) {
				body = body.replace("anti-phishing-code", "");
			} else {
				body = body.replace("anti-phishing-code", "Anti Phishing Code: " + user.getAntiPhishingCode());
			}
		} else {
			body = body.replace("anti-phishing-code", "");
		}

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
