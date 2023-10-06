package com.nefentus.api.Services;

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
     */
    public void generateOtp(String email)
    {
        // generate otp
        Integer otpValue = otpGenerator.generateOTP(email);
        if (otpValue == -1)
        {
            log.error("OTP generator is not working...");
        }

        log.info("Generated OTP: {}", otpValue);

        emailService.sendEmail(email, "Nefentus OTP Password", String.valueOf(otpValue));
    }

    /**
     * Method for validating provided OTP
     *
     * @param email - provided email
     * @param otpNumber - provided OTP number
     * @return boolean value (true|false)
     */
    public Boolean validateOTP(String email, Integer otpNumber)
    {
        // get OTP from cache
        Integer cacheOTP = otpGenerator.getOPTByKey(email);
        log.info("Cache OTP Code: {}", cacheOTP);
        log.info("OTP Number: {}", otpNumber);
        if (cacheOTP!=null && cacheOTP.equals(otpNumber))
        {
            otpGenerator.clearOTPFromCache(email);
            return true;
        }
        return false;
    }
}
