package com.nefentus.api.controller;

import com.nefentus.api.Errors.*;
import com.nefentus.api.Services.TransactionService;
import com.nefentus.api.Services.OtpService;
import com.nefentus.api.Services.UserService;
import com.nefentus.api.Services.WalletService;
import com.nefentus.api.entities.KycImageType;
import com.nefentus.api.entities.User;
import com.nefentus.api.payload.request.*;
import com.nefentus.api.payload.response.*;
import com.nefentus.api.repositories.KycImageRepository;
import com.nefentus.api.repositories.UserRepository;

import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.Base64;
import java.util.Optional;

/*
Class Description:
This class is the REST controller for handling user authentication and registration requests. It provides two endpoints, /register for user registration and /login for user login. It uses Spring Security for authentication and authorization and JWT for token-based authentication.

Attribute Description:

userRepository: A repository for User entities.
passwordEncoder: A Spring Security password encoder.
jwtTokenProvider: A JWT token provider for generating and validating JWT tokens.
authenticationManager: A Spring Security authentication manager.
roleRepository: A repository for Role entities.
Method Descriptions:

register(SignUpRequest authRequest): This method is an HTTP POST endpoint for registering a new user. It takes a SignUpRequest object as input, which contains the user's email, password, and roles. If the email is already registered, it returns a bad request response. Otherwise, it creates a new User object, sets its properties, encodes the password, and saves it to the repository. It returns the created user as a response entity.

login(AuthRequest authRequest): This method is an HTTP POST endpoint for user login. It takes an AuthRequest object as input, which contains the user's email and password. It uses the authentication manager to authenticate the user and generates a JWT token using the token provider. It returns the generated token as a response entity.

setRoles(Set<String> strRoles): This method takes a set of string roles and returns a set of Role objects. It maps the string roles to the corresponding Role enum values and retrieves them from the repository. If a role is not found, it throws a runtime exception. Finally, it returns the set of Role objects.
 */

@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class AuthenticationController {
	UserService userService;
	WalletService walletService;
	KycImageRepository imageRepository;
	TransactionService transactionService;
	private final UserRepository userRepository;
	OtpService otpService;

	@GetMapping(value = "/checkJWTCookie")
	@PreAuthorize("isAuthenticated()")
	public void checkJwt() {
	}

	@GetMapping("/profilePic")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<?> getProfilePicture(Principal principal) {
		log.info("Request to get profile picture");
		var profilePicture = userService.getProfilePicture(principal.getName());
		return ResponseEntity.ok(profilePicture);
	}

	@PostMapping(value = "/register")
	public ResponseEntity<?> register(@RequestBody SignUpRequest authRequest)
			throws UserAlreadyExistsException, AuthenticationException, BadRequestException {
		log.info("Request to register new user! ");
		User created = userService.registerNewUser(authRequest);

		// Make wallets (if not existing until now)
		try {
			walletService.makeWallets(authRequest.getEmail(), authRequest.getPassword());
		} catch (UserNotFoundException e) {
			log.error("User not found: " + authRequest.getEmail());
			return ResponseEntity.badRequest().body("User not found");
		} catch (Exception e) {
			log.error("Error making wallets: " + e.getMessage());
			e.printStackTrace();
			return ResponseEntity.badRequest().body("Error making wallets");
		}

		return ResponseEntity.ok(created);
	}

	@PostMapping(value = "/login")
	public ResponseEntity<?> login(@RequestBody AuthRequest authRequest,
			HttpServletResponse response) throws InactiveUserException, AuthenticationException {
		LoginResponse loginResponse = null;
		try {
			loginResponse = userService.loginUser(authRequest, response);
			// Make wallets (if not existing until now)
			walletService.makeWallets(authRequest.getEmail(), authRequest.getPassword());

			if (loginResponse.isHasOtp()) {
				otpService.generateOtp(loginResponse.email);
			}
		} catch (BadCredentialsException e) {
			log.error("Bad credentials for user: " + authRequest.getEmail());
			return ResponseEntity.badRequest().body("Bad credentials");
		} catch (InactiveUserException e) {
			log.error("Inactive user: " + authRequest.getEmail());
			return ResponseEntity.badRequest().body("Inactive User");
		} catch (UserNotFoundException e) {
			log.error("User not found: " + authRequest.getEmail());
			return ResponseEntity.badRequest().body("User not found");
		} catch (Exception e) {
			log.error("Error making wallets: " + e.getMessage());
			e.printStackTrace();
			return ResponseEntity.badRequest().body("Error making wallets");
		}

		return ResponseEntity.ok(loginResponse);
	}

	@PostMapping("/{userId}/upload_kyc")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<?> saveKycImage(@PathVariable Long userId,
			@RequestParam("type") KycImageType type,
			@RequestPart("file") MultipartFile file,
			Principal principal) throws UserNotFoundException, IOException {
		log.info("Request to save upload KYC");
		userService.uploadKYCImage(type, principal.getName(), file);
		return ResponseEntity.ok(new MessageResponse("successfull!"));
	}

	@GetMapping("/{userId}/kyc-image-url")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ResultObjectInfo<KycResponse>> getKycImage(@PathVariable Long userId,
			@RequestParam("type") KycImageType type,
			Principal principal) throws UserNotFoundException, IOException {
		log.info("Request to save upload KYC");
		KycResponse kyc = userService.getKycUrl(type, userId);
		Optional<User> user = userRepository.findById(userId);

		BigDecimal threshold = transactionService.getIncomeForUser(user.orElse(null));

		if ((threshold.compareTo(new BigDecimal("10000000")) > 0) &&
				(type == KycImageType.UTILITY_BILL || type == KycImageType.ADRESS || type == KycImageType.PASSPORT
						|| type == KycImageType.PERSONAL_PICTURE || type == KycImageType.COMPANY_REGISTRATION)) {
			kyc.setRequired(true);
		} else if ((threshold.compareTo(new BigDecimal("1000000")) > 0) &&
				(type == KycImageType.ADRESS || type == KycImageType.PASSPORT || type == KycImageType.PERSONAL_PICTURE
						|| type == KycImageType.COMPANY_REGISTRATION)) {
			kyc.setRequired(true);
		} else if ((threshold.compareTo(new BigDecimal("10000")) > 0) &&
				(type == KycImageType.PASSPORT ||
						type == KycImageType.PERSONAL_PICTURE ||
						type == KycImageType.COMPANY_REGISTRATION)) {
			kyc.setRequired(true);
		} else {
			kyc.setRequired(false);
		}

		return new ResponseEntity<>(
				ResultObjectInfo.<KycResponse>builder()
						.data(kyc)
						.message("success")
						.build(),
				HttpStatus.OK);
	}

	@GetMapping("/{userId}/kyc-level")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ResultObjectInfo<KycLevelResponse>> getKycLevel(@PathVariable Long userId,
			Principal principal) throws UserNotFoundException, IOException {
		log.info("Request to get KYC level");
		Optional<User> user = userRepository.findById(userId);

		KycLevelResponse kycLevel = KycLevelResponse.builder().build();
		BigDecimal threshold = transactionService.getIncomeForUser(user.orElse(null));
		if (threshold.compareTo(new BigDecimal("10000000")) > 0) {
			kycLevel.setKycLevel(3);
		} else if (threshold.compareTo(new BigDecimal("1000000")) > 0) {
			kycLevel.setKycLevel(2);
		} else if (threshold.compareTo(new BigDecimal("10000")) > 0) {
			kycLevel.setKycLevel(1);
		} else {
			kycLevel.setKycLevel(0);
		}
		return new ResponseEntity<>(
				ResultObjectInfo.<KycLevelResponse>builder()
						.data(kycLevel)
						.message("success")
						.build(),
				HttpStatus.OK);
	}

	@PostMapping("/upload")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<?> handleFileUpload(@RequestParam("file") MultipartFile file,
			@RequestHeader("Authorization") String authorization,
			Principal principal) throws UserNotFoundException, IOException {

		String email = principal.getName();
		log.info("Request upload file KYC from user with email= {}", email);
		String url = userService.uploadProfilePicture(file, email);
		return ResponseEntity.ok(new MessageResponse(url));
	}

	@GetMapping("/getBlob")
	@PreAuthorize("permitAll()")
	public ResponseEntity<?> getBlob() {
		var image = imageRepository.findById(1L).get();
		byte[] blobData = image.getData();
		String base64Data = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(blobData);
		log.info("Handle request get blobData");
		return ResponseEntity.ok(base64Data);
	}

	@PatchMapping("/update")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<?> updateUser(@RequestBody UpdatetUserRequest updatetUserRequest,
			Principal principal) throws UserNotFoundException, BadRequestException {
		String email = principal.getName();
		log.info("Request update user from email= {}", email);
		UpdateResponse updateResponse = userService.updateUser(updatetUserRequest, email);
		return ResponseEntity.ok(updateResponse);

	}

	@PatchMapping("/change-email")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<?> changeEmail(@RequestBody String newEmail,
			Principal principal) throws UserNotFoundException, EmailSendException {
		String email = principal.getName();
		log.info("Request update user from email= {}", email);
		userService.changeEmail(newEmail, email);
		return ResponseEntity.ok(new MessageResponse("Email change email sent!"));
	}

	@PostMapping("/confirm-email")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<?> confirmEmail(@RequestBody ChangeEmailRequest changeEmailRequest,
			Principal principal) throws UserNotFoundException, EmailSendException {
		String email = principal.getName();
		log.info("Request update user from email= {}", email);
		userService.confirmEmail(changeEmailRequest, email);
		return ResponseEntity.ok(new MessageResponse("Email changed successfully!"));
	}

	@PostMapping("/deleteImage")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<?> deleteUserImage(Principal principal) {
		log.info("Delete User image! ");

		try {
			return ResponseEntity.ok(userService.deleteProfileImage(principal.getName()));
		} catch (UserNotFoundException e) {
			log.error("User not found: " + principal.getName());
			return ResponseEntity.badRequest().body("User not found");
		}
	}

	@PostMapping("/signout")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<?> logoutUser() {
		return ResponseEntity.ok()
				.body(new MessageResponse("You've been signed out!"));
	}

	@PatchMapping("/activate")
	public ResponseEntity<?> activate(@RequestBody String token) throws TokenNotFoundException {
		log.info("Request to activate user");
		userService.activateUser(token);
		return ResponseEntity.ok().body(new MessageResponse("Your account has been activated!"));
	}

	@PostMapping("/forgot-password")
	public ResponseEntity<?> forgotPassword(@RequestBody String email)
			throws UserNotFoundException, EmailSendException {
		log.info("Handle request forgot password from user with email= {}", email);
		userService.forgotPassword(email);
		return ResponseEntity.ok(new MessageResponse("Password reset email sent!"));
	}

	@PostMapping("/reset-password")
	public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) throws InvalidTokenException {
		log.info("Handle request reset password ");
		userService.resetPassword(request.getToken(), request.getNewPassword());
		return ResponseEntity.ok(new MessageResponse("Password reset successful!"));
	}

	@PostMapping("/reset-password-email")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<?> setResetEmail(@RequestBody DashboardPasswordReset request, Principal principal)
			throws UserNotFoundException, IncorrectPasswordException, EmailSendException {
		log.info("Request to reset email");
		userService.resetEmail(principal.getName(), request.getOldPassword(), request.getNewPassword());
		return ResponseEntity.ok(new MessageResponse("Password reset email sent!"));
	}

	@PostMapping("/reset-password-auth")
	public ResponseEntity<?> setNewPassword(@RequestBody DashboardPasswordRequestAuth requestAuth, Principal principal)
			throws UserNotFoundException, InvalidTokenException {
		log.info("Request to change password from email= {}", principal.getName());
		userService.setNewPassword(principal.getName(), requestAuth.getToken());
		return ResponseEntity.ok(new MessageResponse("Password reset successful!"));
	}

	@PostMapping(value = "/setup/totp", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<?> add2fa(@RequestBody twoFARequest payload, Principal principal)
			throws UserNotFoundException {
		log.info("Handle request two factor from email= {} ", principal.getName());
		var Uri = userService.setMfa(principal.getName(), payload.isActive());
		return ResponseEntity.ok().body(new twoFAResponse(Uri));
	}

	@PostMapping("/verify/otp")
	public ResponseEntity<?> verifyOTPCode(@RequestBody VerifyOTPCodeRequest verifyOtpCodeRequest)
			throws UserNotFoundException, BadRequestException, InternalServerException {
		log.info("Request to verify code from email= {} ", verifyOtpCodeRequest.getEmail());
		var loginResponse = userService.verifyOTP(verifyOtpCodeRequest.getEmail(), verifyOtpCodeRequest.getCode(),
				verifyOtpCodeRequest.isRememberMe());
		return ResponseEntity.ok(loginResponse);
	}

	@PostMapping("/verify/totp")
	public ResponseEntity<?> verifyCode(@RequestBody VerifyCodeRequest verifyCodeRequest)
			throws UserNotFoundException, BadRequestException, InternalServerException {
		log.info("Request to verify code from email= {} ", verifyCodeRequest.getEmail());
		var loginResponse = userService.verify(verifyCodeRequest.getEmail(), verifyCodeRequest.getToken(),
				verifyCodeRequest.isRememberMe());
		return ResponseEntity.ok(loginResponse);
	}

	@PostMapping("/checkPassword")
	public ResponseEntity<?> checkPassword(@RequestBody CheckPasswordRequest request, Principal principal)
			throws UserNotFoundException, BadRequestException, InternalServerException {
		log.info("Check password for email= {} ", principal.getName());
		boolean success = userService.checkPassword(request, principal.getName());
		if (success) {
			return ResponseEntity.ok(success);
		} else {
			return ResponseEntity.badRequest().body(success);
		}
	}

	@PostMapping("/setup/getToken")
	public ResponseEntity<?> getToken(Principal principal) throws UserNotFoundException, InternalServerException {
		String email = principal.getName();
		try {
			String token = userService.generateJwtToken(email, false);
			return ResponseEntity.ok(token);
		} catch (UserNotFoundException e) {
			log.error("User not found: " + email);
			return ResponseEntity.badRequest().body("User not found");
		} catch (Exception e) {
			log.error("Error making wallets: " + e.getMessage());
			e.printStackTrace();
			return ResponseEntity.badRequest().body("Error making wallets");
		}
	}
}
