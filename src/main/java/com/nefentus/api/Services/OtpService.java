package com.nefentus.api.Services;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

	@Autowired
	private final EmailService emailService;
	private final OtpGenerator otpGenerator;

	/**
	 * Method for generate OTP number
	 *
	 * @param email - provided email (email in this case)
	 * @return boolean value (true|false)
	 * @throws IOException
	 */
	public void generateOtp(String email) throws IOException {
		// generate otp
		Integer otpValue = otpGenerator.generateOTP(email);
		if (otpValue == -1) {
			log.error("OTP generator is not working...");
		}

		log.info("Generated OTP: {}", otpValue);

		var html = HtmlProvider.loadOTPPasswordMail(String.valueOf(otpValue));
		emailService.sendEmail(email, "Confirm login to Nefentus", html);
	}

	/**
	 * Method for validating provided OTP
	 *
	 * @param email     - provided email
	 * @param otpNumber - provided OTP number
	 * @return boolean value (true|false)
	 */
	public Boolean validateOTP(String email, Integer otpNumber) {
		// get OTP from cache
		Integer cacheOTP = otpGenerator.getOPTByKey(email);
		log.info("Cache OTP Code: {}", cacheOTP);
		log.info("OTP Number: {}", otpNumber);
		if (cacheOTP != null && cacheOTP.equals(otpNumber)) {
			otpGenerator.clearOTPFromCache(email);
			return true;
		}
		return false;
	}
}
