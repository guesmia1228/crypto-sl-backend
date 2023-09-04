package com.nefentus.api.Services;

import com.nefentus.api.Errors.*;
import com.nefentus.api.entities.*;
import com.nefentus.api.payload.request.*;
import com.nefentus.api.payload.response.*;
import com.nefentus.api.repositories.*;
import com.nefentus.api.security.CustomUserDetails;
import com.nefentus.api.security.JwtTokenProvider;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
	@Value("${app.name.url-confirmation-email}")
	private String appUrlConfirmationEmail;
	@Value("${app.name.url-reset-password-email}")
	private String appUrlPasswordResetEmail;

	@Autowired
	private EmailService emailService;

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final AuthenticationManager authenticationManager;
	private final RoleRepository roleRepository;
	private final PasswordResetTokenService resetTokenService;
	private final PasswordResetTokenRepository resetTokenRepository;
	private final TotpManager totpManager;
	private final KycImageRepository kycImageRepository;
	private final HierarchyRepository hierarchyRepository;
	private final WalletRepository walletRepository;
	@Autowired
	private S3Service s3Service;

	public DashboardNumberResponse calculateTotalClicks() {
		long totalClicks = userRepository.count();
		long lastMonthClicks = userRepository
				.countByCreatedAtAfter(Timestamp.valueOf(LocalDateTime.now().minusDays(30)));
		long last30 = totalClicks - lastMonthClicks;
		double percentageIncrease = last30 == 0 ? totalClicks * 100
				: ((double) (lastMonthClicks - last30) / last30) * 100;
		DashboardNumberResponse totalClicksDto = new DashboardNumberResponse();
		totalClicksDto.setNumber(new BigDecimal(totalClicks));
		totalClicksDto.setPercentage(new BigDecimal(percentageIncrease));
		log.info("Success calculate total users total= {} ", totalClicks);
		return totalClicksDto;
	}

	public DashboardNumberResponse calculateTotalClicks(String email) {
		long totalClicks = hierarchyRepository.countByParentEmail(email);
		long lastMonthClicks = hierarchyRepository
				.countByCreatedAtAfterAndParentEmail(Timestamp.valueOf(LocalDateTime.now().minusDays(30)), email);
		long last30 = totalClicks - lastMonthClicks;
		double percentageIncrease = last30 == 0 ? totalClicks * 100
				: ((double) (lastMonthClicks - last30) / last30) * 100;
		DashboardNumberResponse totalClicksDto = new DashboardNumberResponse();
		totalClicksDto.setNumber(new BigDecimal(totalClicks));
		totalClicksDto.setPercentage(new BigDecimal(percentageIncrease));
		log.info("Success to count total users total= {} ", totalClicks);
		return totalClicksDto;
	}

	public String getProfilePicture(String email) {
		var user = userRepository.findUserByEmail(email).get();
		log.info("Get profile picture from email= {}", email);
		return Base64.getEncoder().encodeToString(user.getProfilepic());
	}

	public List<Map<String, Object>> getRolesStatus() {
		List<User> users = userRepository.findAll();
		return rolesMapToList(getRolesStatus(users));
	}

	public List<Map<String, Object>> getRolesStatus(String email) {
		User user = userRepository.findUserByEmail(email)
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));
		ERole highestRole = user.getRoles().stream()
				.map(Role::getName)
				.max(Comparator.comparingInt(ERole::ordinal))
				.orElseThrow(() -> new RuntimeException("User has no role"));

		List<User> users = hierarchyRepository.findChildByParentEmail(email);
		Map<ERole, Map<String, Object>> response = getRolesStatus(users);

		// Filter out roles with a lower "ranking"
		Map<ERole, Map<String, Object>> responseFiltered = response.entrySet()
				.stream().filter(x -> x.getKey().ordinal() < highestRole.ordinal())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		return rolesMapToList(responseFiltered);
	}

	private List<Map<String, Object>> rolesMapToList(Map<ERole, Map<String, Object>> roles) {
		Set<ERole> keys = roles.keySet();

		List<Map<String, Object>> rolesList = new ArrayList<>();
		for (ERole role : ERole.values()) {
			if (keys.contains(role)) {
				rolesList.add(roles.get(role));
			}
		}

		return rolesList;
	}

	private Map<ERole, Map<String, Object>> getRolesStatus(List<User> users) {
		Map<ERole, Map<String, Object>> report = new HashMap<>();
		for (ERole role : ERole.values()) {
			Map<String, Object> roleData = new HashMap<>();
			roleData.put("role", role.label.substring(role.label.indexOf("_")).toLowerCase().replace("_", ""));
			roleData.put("percentage", 0);
			roleData.put("count", 0);
			report.put(role, roleData);
		}

		int totalRoles = 0;
		for (User user : users) {
			Set<Role> roles = user.getRoles();
			totalRoles += user.getRoles().size();
			for (Role role : roles) {
				Map<String, Object> roleData = report.get(role.getName());
				roleData.put("count", (Integer) roleData.get("count") + 1);
			}
		}

		// Calculate the total percentages and add the report data to the list
		if (totalRoles > 0) {
			for (ERole role : ERole.values()) {
				Map<String, Object> roleData = report.get(role);
				roleData.put("percentage", ((Integer) roleData.get("count") * 100) / totalRoles);
			}
		}

		log.info("Successful to make a report with totalUser= {} ", totalRoles);
		return report;
	}

	public Long countUsers() {
		return userRepository.count();
	}

	public User changeUserState(ChangeUserStateRequest changeUserStateRequest) throws UserNotFoundException {
		var optUser = userRepository.findUserByEmail(changeUserStateRequest.getUseremail());
		if (optUser.isEmpty()) {
			log.error("User with email +  " + changeUserStateRequest.getUseremail() + " is not found");
			throw new UserNotFoundException(
					"User with email +  " + changeUserStateRequest.getUseremail() + " is not found",
					HttpStatus.BAD_REQUEST);
		}
		var user = optUser.get();
		user.setActive(!user.getActive());
		log.info("Change user state success with email= {} ", changeUserStateRequest.getUseremail());
		return userRepository.save(user);
	}

	public User addUser(AddUserRequest addUserRequest) throws UserAlreadyExistsException {
		Optional<User> userOptional = userRepository.findUserByEmail(addUserRequest.getEmail());
		if (userOptional.isPresent()) {
			log.error("User with email " + addUserRequest.getEmail() + " already exists!");
			throw new UserAlreadyExistsException("User with email " + addUserRequest.getEmail() + " already exists. ",
					HttpStatus.BAD_REQUEST);
		}

		var user = new User();
		user.setEmail(addUserRequest.getEmail());
		user.setFirstName("");
		user.setLastName("");
		user.setTel("");
		user.setPassword(passwordEncoder.encode(addUserRequest.getPassword()));
		user.setRoles(setRoles(addUserRequest.getRoles()));
		user.setToken(passwordEncoder.encode(user.getEmail()));
		user.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
		user.setUpdatedAt(user.getCreatedAt());
		user.setActive(false);
		user.setAffiliateLink(EmailHashGenerator.generateHash(user.getEmail()));
		user.setBusiness("");
		user.setProfilePicturepath("");
		User created = userRepository.save(user);

		try {
			// sendConfirmationEmail(created.getEmail(), created.getToken());
		} catch (Exception e) {
			userRepository.delete(created);
			throw new RuntimeException("Failed to send Confirmation email. Please try again.", e);
		}
		log.info("Create new user with email= {}", addUserRequest.getEmail());
		return created;
	}

	public User addUser(AddUserRequest addUserRequest, String email) throws UserAlreadyExistsException {
		Optional<User> userOptional = userRepository.findUserByEmail(addUserRequest.getEmail());
		if (userOptional.isPresent()) {
			throw new UserAlreadyExistsException("User with email " + addUserRequest.getEmail() + " already exists.",
					HttpStatus.BAD_REQUEST);
		}

		var admin = userRepository.findUserByEmail(email).get();

		var user = new User();
		user.setEmail(addUserRequest.getEmail());
		user.setFirstName("");
		user.setLastName("");
		user.setTel("");
		user.setPassword(passwordEncoder.encode(addUserRequest.getPassword()));
		user.setRoles(setRoles(addUserRequest.getRoles()));
		user.setToken(passwordEncoder.encode(user.getEmail()));
		user.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
		user.setUpdatedAt(user.getCreatedAt());
		user.setActive(false);
		user.setAffiliateLink(EmailHashGenerator.generateHash(user.getEmail()));
		user.setBusiness("");
		user.setProfilePicturepath("");
		User created = userRepository.save(user);

		Hierarchy hierarchy = new Hierarchy();
		hierarchy.setChild(created);
		hierarchy.setParent(admin);
		hierarchy.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
		hierarchy.setCommissionRate(CommissionRate.getAccordingToHighestRole(user.getRoles(), admin.getRoles()));
		Hierarchy savedHierarchy = hierarchyRepository.save(hierarchy);

		try {
			sendConfirmationEmail(created.getEmail(), created.getToken());
		} catch (Exception e) {
			hierarchyRepository.delete(savedHierarchy);
			userRepository.delete(created);
			throw new RuntimeException("Failed to send Confirmation email. Please try again.", e);
		}

		return created;
	}

	public User registerNewUser(SignUpRequest authRequest)
			throws UserAlreadyExistsException, AuthenticationException {
		// Überprüfen, ob ein Benutzer mit der angegebenen E-Mail-Adresse bereits
		// existiert
		Optional<User> userOptional = userRepository.findUserByEmail(authRequest.getEmail());
		if (userOptional.isPresent()) {
			log.error("User with email " + authRequest.getEmail() + " already exists.");
			throw new UserAlreadyExistsException("User with email " + authRequest.getEmail() + " already exists.",
					HttpStatus.BAD_REQUEST);
		}

		List<String> countryList = Arrays.stream(ECountry.values())
				.map(it -> it.name().replace("_", " "))
				.toList();
		User user = new User();
		user.setEmail(authRequest.getEmail());
		user.setFirstName(authRequest.getFirstName());
		user.setLastName(authRequest.getLastName());
		user.setTel(authRequest.getTelNr());
		user.setPassword(passwordEncoder.encode(authRequest.getPassword()));
		user.setRoles(setRoles(authRequest.getRoles()));
		user.setToken(passwordEncoder.encode(user.getEmail()));
		user.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
		user.setUpdatedAt(user.getCreatedAt());
		user.setActive(false);
		user.setAffiliateLink(EmailHashGenerator.generateHash(user.getEmail()));
		user.setBusiness("");
		user.setProfilePicturepath("");
		user.setCountry(authRequest.getCountry());
		if (countryList.contains(authRequest.getCountry().toUpperCase())) {
			user.setRequireKYC(true);
		} else {
			user.setRequireKYC(false);
		}
		User created = userRepository.save(user);

		// Affiliate erstellen und speichern, falls vorhanden
		if (authRequest.getAffiliateLink() != null && !authRequest.getAffiliateLink().isEmpty()) {
			Optional<User> optParent = userRepository.findByAffiliateLink(authRequest.getAffiliateLink());
			if (optParent.isPresent()) {
				Hierarchy hierarchy = new Hierarchy();
				hierarchy.setChild(created);
				hierarchy.setParent(optParent.get());
				hierarchy.setCreatedAt(new Timestamp(new Date().getTime()));
				hierarchy.setCommissionRate(
						CommissionRate.get(roleRepository.findByName(ERole.ROLE_VENDOR).get(),
								roleRepository.findByName(ERole.ROLE_AFFILIATE).get()));
				hierarchyRepository.save(hierarchy);
			}
		}

		// Bestätigungsemail senden
		try {
			log.info("Send mail to confirmation register with email= {}", authRequest.getEmail());
			sendConfirmationEmail(created.getEmail(), created.getToken());
		} catch (Exception e) {
			userRepository.delete(created);
			log.error("Failed to send Confirmation email. Please try again.");
			throw new RuntimeException("Failed to send Confirmation email. Please try again.", e);
		}

		log.info("Successful to register new user with email= {}", authRequest.getEmail());
		return created;
	}

	public User activateUser(String token)
			throws TokenNotFoundException {
		Optional<User> userOptional = userRepository.findByToken(token);
		if (userOptional.isPresent()) {
			User user = userOptional.get();
			user.setActive(true);
			userRepository.save(user);
			log.info("Activate user Successful");
			return user;
		} else {
			log.error("Can not found user with this token! ");
			throw new TokenNotFoundException("Token is not found!", HttpStatus.BAD_REQUEST);
		}
	}

	public LoginResponse loginUser(AuthRequest authRequest,
			HttpServletResponse response)
			throws AuthenticationException, InactiveUserException, UserNotFoundException {
		// Authentifizierung des Benutzers
		log.info("Request to Login with email= {}", authRequest.getEmail());
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(
						authRequest.getEmail(),
						authRequest.getPassword()));

		// Überprüfen, ob der Benutzer aktiv ist
		User user = userRepository.findUserByEmail(authRequest.getEmail())
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));
		if (!user.getActive()) {
			log.error("Login fail with status account is not activate");
			throw new InactiveUserException("Please activate your account!", HttpStatus.BAD_REQUEST);
		}

		log.info("Found user with email= {}", authRequest.getEmail());
		// LoginResponse erstellen
		if (!user.isMfa()) {
			log.info("login success with return jwt");
			return new LoginResponse(
					jwtTokenProvider.generateToken(authentication, authRequest.rememberMe),
					authRequest.getEmail(),
					user.getFirstName(),
					user.getLastName(),
					user.getAffiliateLink(),
					Base64.getEncoder()
							.encodeToString(user.getProfilepic() != null ? user.getProfilepic() : new byte[] {}),
					user.getBusiness(),
					user.getTel(),
					user.getRoles().stream()
							.map(Role::getName)
							.map(Enum::name)
							.toArray(String[]::new),
					user.isMfa(),
					user.getS3Url(),
					user.getCountry(),
					user.isRequireKYC(),
					user.getId()

			);
		} else {
			log.info("login success without return jwt");
			return new LoginResponse(
					"",
					user.getEmail(),
					"",
					"",
					"",
					"",
					"",
					"",
					new String[] {},
					user.isMfa(),
					user.getS3Url(),
					user.getCountry(),
					user.isRequireKYC(),
					null);
		}
	}

	public void uploadKYCImage(KycImageType type, String email, MultipartFile file)
			throws UserNotFoundException, IOException {
		User user = userRepository.findUserByEmail(email)
				.orElseThrow(() -> new UserNotFoundException("User not found", HttpStatus.BAD_REQUEST));
		Optional<KycImage> kycImgOpt = Optional
				.ofNullable(kycImageRepository.findKycImageByTypeAndUser_Id(type, user.getId()));
		String filename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename())).replace(" ", "");
		String s3Key = UUID.randomUUID().toString().concat("_").concat(filename);
		byte[] data = file.getBytes();
		s3Service.uploadToS3Bucket(new ByteArrayInputStream(data), s3Key);
		if (kycImgOpt.isPresent()) {
			kycImgOpt.get().setS3Key(s3Key);
			kycImageRepository.save(kycImgOpt.get());
		} else {
			KycImage kycImage = KycImage.builder()
					.confirmed(false)
					.user(user)
					.type(type)
					.s3Key(s3Key)
					.build();
			kycImageRepository.save(kycImage);
		}

		log.info("Upload KYC image from user with email= {}", email);

	}

	public KycResponse getKycUrl(KycImageType type, Long userId) {
		Optional<KycImage> kycImageOpt = Optional
				.ofNullable(kycImageRepository.findKycImageByTypeAndUser_Id(type, userId));
		if (kycImageOpt.isPresent()) {
			String url = s3Service.presignedURL(kycImageOpt.get().getS3Key());
			return KycResponse.builder()
					.isVerify(kycImageOpt.get().getConfirmed())
					.url(url)
					.build();
		}
		return KycResponse.builder().build();

	}

	public String uploadProfilePicture(MultipartFile file, String email) throws IOException, UserNotFoundException {
		// Benutzer suchen
		try {

			User user = userRepository.findUserByEmail(email)
					.orElseThrow(() -> new UserNotFoundException("User not found", HttpStatus.NOT_FOUND));
			String filename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename())).replace(" ",
					"");
			String s3Key = UUID.randomUUID().toString().concat("_").concat(filename);
			byte[] data = file.getBytes();
			String url = s3Service.uploadToS3BucketProfilePic(new ByteArrayInputStream(data), s3Key);

			user.setS3Url(url);
			userRepository.save(user);
			log.info("Successful upload profile Picture");
			return url;
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	public UpdateResponse updateUser(UpdatetUserRequest updatetUserRequest,
			String email)
			throws UserNotFoundException {
		// Benutzer suchen
		User user = userRepository.findUserByEmail(email)
				.orElseThrow(() -> new UserNotFoundException("User not found", HttpStatus.NOT_FOUND));

		// Benutzer aktualisieren
		user.setBusiness(updatetUserRequest.getBusiness());
		String[] names = updatetUserRequest.getFullName().split(" ");
		if (names.length >= 2) {
			user.setFirstName(names[0]);
			String lastName = String.join(" ", Arrays.copyOfRange(names, 1, names.length));
			user.setLastName(lastName);
		} else {
			user.setFirstName(names[0]);
			user.setLastName("");
		}
		user.setTel(updatetUserRequest.getPhoneNumber());
		user.setEmail(updatetUserRequest.getEmail());
		// Benutzer speichern und UpdateResponse zurückgeben
		User savedUser = userRepository.save(user);
		log.info("Successful update User from user with email= {}", savedUser.getEmail());
		return new UpdateResponse(
				savedUser.getEmail(),
				savedUser.getEmail(),
				savedUser.getFirstName(),
				savedUser.getLastName(),
				savedUser.getProfilePicturepath(),
				savedUser.getBusiness(),
				savedUser.getTel(),
				"");
	}

	public void forgotPassword(String email)
			throws UserNotFoundException,
			EmailSendException {
		// Benutzer suchen
		User user = userRepository.findUserByEmail(email)
				.orElseThrow(() -> new UserNotFoundException("User not found", HttpStatus.BAD_REQUEST));

		// Reset-Token generieren und in der Datenbank speichern
		String resetToken = passwordEncoder.encode(user.getEmail() + LocalDateTime.now().toString());
		user.setResetToken(resetToken);
		log.info("Found user with email= {}", email);
		userRepository.save(user);

		// Reset-Passwort-Email senden
		try {
			log.info("password reset email send to user");
			sendResetPasswordEmail(user.getEmail(), resetToken);
		} catch (Exception e) {
			log.error("Failed to send reset password email={}", email);
			throw new EmailSendException("Failed to send reset password email", HttpStatus.BAD_REQUEST);
		}
	}

	public void resetPassword(String token,
			String newPassword)
			throws InvalidTokenException {
		// Benutzer suchen
		User user = userRepository.findByResetToken(token)
				.orElseThrow(() -> new InvalidTokenException("Invalid reset token", HttpStatus.BAD_REQUEST));

		// Passwort aktualisieren und Reset-Token entfernen
		user.setPassword(passwordEncoder.encode(newPassword));
		user.setResetToken(null);
		log.info("Successful reset password!");
		userRepository.save(user);
	}

	public void resetEmail(String email,
			String oldPassword,
			String newPassword)
			throws UserNotFoundException,
			IncorrectPasswordException,
			EmailSendException {
		// Benutzer suchen
		User user = userRepository.findUserByEmail(email)
				.orElseThrow(() -> new UserNotFoundException("User not found", HttpStatus.BAD_REQUEST));

		// Überprüfen, ob das alte Passwort korrekt ist
		if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
			throw new IncorrectPasswordException("Old password is not correct", HttpStatus.BAD_REQUEST);
		}

		log.info("Found user with email= {}", email);
		// Passwort-Reset-Token erstellen und in der Datenbank speichern
		String resetToken = passwordEncoder.encode(user.getEmail() + LocalDateTime.now().toString());
		PasswordResetToken createdToken = resetTokenService.createPasswordResetTokenForUser(user,
				passwordEncoder.encode(newPassword));

		// Reset-Password-Email senden
		try {
			log.info("Send reset password to email= {}", email);
			sendResetEmail(user.getEmail(), createdToken.getToken());
		} catch (Exception e) {
			log.error("Failed to send reset password email= {}", email);
			throw new EmailSendException("Failed to send reset password email", HttpStatus.BAD_REQUEST);
		}
	}

	public void setNewPassword(String email,
			String token)
			throws UserNotFoundException,
			InvalidTokenException {
		// Benutzer suchen
		User user = userRepository.findUserByEmail(email)
				.orElseThrow(() -> new UserNotFoundException("User not found", HttpStatus.BAD_REQUEST));

		// Überprüfen, ob das Token vorhanden und noch gültig ist
		Optional<PasswordResetToken> tokenOptional = resetTokenRepository.findByToken(token);
		if (tokenOptional.isEmpty() || tokenOptional.get().getExpiryDate().isBefore(LocalDateTime.now())) {
			throw new InvalidTokenException("Invalid reset token", HttpStatus.BAD_REQUEST);
		}

		log.info("Found user with email= {}", email);
		// Passwort aktualisieren und Reset-Token entfernen
		PasswordResetToken resetToken = tokenOptional.get();
		user.setPassword(resetToken.getNewPassword());
		userRepository.save(user);
		log.info("Successful to set new password");
		resetTokenRepository.delete(resetToken);
	}

	public String setMfa(String email,
			boolean isActive)
			throws UserNotFoundException {
		// Benutzer suchen
		User user = userRepository.findUserByEmail(email)
				.orElseThrow(() -> new UserNotFoundException("User not found", HttpStatus.BAD_REQUEST));
		// 2FA-Status und geheimes Schlüssel aktualisieren, falls aktiviert

		log.info("Found user with email= {}", email);
		user.setMfa(isActive);
		if (isActive) {
			user.setSecret(totpManager.generateSecret());
		}
		userRepository.save(user);
		log.info("Successful set Mfa");
		return totpManager.getUriForImage(user.getSecret(), user.getFirstName() + " " + user.getLastName());
	}

	public LoginResponse verify(String email,
			String code,
			boolean longToken)
			throws UserNotFoundException,
			BadRequestException,
			InternalServerException {
		User user = userRepository
				.findUserByEmail(email)
				.orElseThrow(() -> new UserNotFoundException(String.format("email %s", email), HttpStatus.BAD_REQUEST));
		if (!totpManager.verifyCode(code, user.getSecret())) {
			log.error("Code is incorrect");
			throw new BadRequestException("Code is incorrect", HttpStatus.BAD_REQUEST);
		}
		var optUser = Optional.of(user)
				.map(CustomUserDetails::build)
				.map(userDetailsToken -> new UsernamePasswordAuthenticationToken(
						userDetailsToken, null, userDetailsToken.getAuthorities()))
				.map(userDetailsToken -> jwtTokenProvider.generateToken(userDetailsToken, longToken))
				.orElseThrow(() -> new InternalServerException("unable to generate access token"));

		if (optUser.isEmpty()) {
			log.error("User not found");
			throw new UserNotFoundException("User not found", HttpStatus.BAD_REQUEST);
		}
		log.info("Found user with email= {}", email);
		return new LoginResponse(
				optUser,
				user.getEmail(),
				user.getFirstName(),
				user.getLastName(),
				user.getAffiliateLink(),
				Base64.getEncoder().encodeToString(user.getProfilepic()),
				user.getBusiness(),
				user.getTel(),
				user.getRoles().stream()
						.map(Role::getName)
						.map(Enum::name)
						.toArray(String[]::new),
				user.isMfa(),
				user.getS3Url(),
				user.getCountry(),
				user.isRequireKYC(),
				user.getId());

	}

	public boolean checkPassword(CheckPasswordRequest request, String username)
			throws UserNotFoundException {
		// Überprüfen, ob das PAsswort übereinstimmt
		User user = userRepository.findUserByEmail(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));

		if (passwordEncoder.matches(request.getPassword(), user.getPassword())) {
			log.info("Password is correct");
			return true;
		} else {
			log.error("Password is incorrect");
			return false;
		}
	}

	private void sendResetEmail(String email,
			String token) {
		try {
			var html = HtmlProvider.loadResetTokenMail(token);
			emailService.sendEmail(email, "Please activate your account", html);
		} catch (IOException e) {
			log.error("sendResetEmail", e);
		}
	}

	private void sendResetPasswordEmail(String email,
			String token) {

		try {
			var html = HtmlProvider.loadHtmlFileReset(token, appUrlPasswordResetEmail);
			emailService.sendEmail(email, "Reset your password!", html);
		} catch (IOException e) {
			log.error("sendResetPasswordEmail", e);
		}
	}

	private void sendConfirmationEmail(String email,
			String token) {

		try {
			var html = HtmlProvider.loadHtmlFile(token, appUrlConfirmationEmail);
			emailService.sendEmail(email, "Please activate your account", html);
		} catch (IOException e) {
			log.error("sendConfirmationEmail", e);
		}
	}

	public Set<Role> setRoles(Set<String> strRoles) {

		Set<Role> roles = new HashSet<>();
		if (strRoles != null) {
			strRoles.forEach(role -> {
				switch (role) {
					case "Affiliate" -> {
						Role affRole = roleRepository.findByName(ERole.ROLE_AFFILIATE)
								.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
						roles.add(affRole);
					}
					case "Gold-Partner", "Gold Partner", "IB", "Broker" -> {
						Role broker = roleRepository.findByName(ERole.ROLE_BROKER)
								.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
						roles.add(broker);
					}
					case "Diamond-Partner", "Diamond Partner", "Senior IB", "Senior Broker" -> {
						Role seniorBroker = roleRepository.findByName(ERole.ROLE_SENIOR_BROKER)
								.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
						roles.add(seniorBroker);
					}
					case "IB-Leader", "IB Leader" -> {
						Role leader = roleRepository.findByName(ERole.ROLE_LEADER)
								.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
						roles.add(leader);
					}
					case "Vendor" -> {
						Role vendor = roleRepository.findByName(ERole.ROLE_VENDOR)
								.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
						roles.add(vendor);
					}
					default -> {
						log.error("Error: Role {} is not found.", role);
					}
				}
			});
		}
		return roles;
	}

	public ParentWalletAddresses getParentWalletAddresses(long userId) throws UserNotFoundException {
		ParentWalletAddresses addresses = new ParentWalletAddresses();

		// Find seller address
		Optional<User> optUser = userRepository.findById(userId);
		if (optUser.isPresent()) {
			List<Wallet> wallets = walletRepository.findByOwner(optUser.get());
			addresses.setSellerAddress("0x" + wallets.get(0).getAddress());
		} else {
			throw new UserNotFoundException("User with id " + userId + " not found", HttpStatus.BAD_REQUEST);
		}

		// Find affiliate, broker, and leader
		Optional<User> optParent = hierarchyRepository.findParentByChildId(userId);
		while (optParent.isPresent()) {
			User parent = optParent.get();
			List<Wallet> wallets = walletRepository.findByOwner(parent);

			Set<Role> roles = parent.getRoles();
			for (Role role : roles) {
				switch (role.getName()) {
					case ROLE_AFFILIATE:
						if (addresses.getAffiliateAddress() == null)
							addresses.setAffiliateAddress("0x" + wallets.get(0).getAddress());
						break;
					case ROLE_BROKER:
						if (addresses.getBrokerAddress() == null)
							addresses.setBrokerAddress("0x" + wallets.get(0).getAddress());
						break;
					case ROLE_SENIOR_BROKER:
						if (addresses.getSeniorBrokerAddress() == null)
							addresses.setSeniorBrokerAddress("0x" + wallets.get(0).getAddress());
						break;
					case ROLE_LEADER:
						if (addresses.getLeaderAddress() == null)
							addresses.setLeaderAddress("0x" + wallets.get(0).getAddress());
						break;
					default:
						break;
				}
			}

			// Get the new parent
			optParent = hierarchyRepository.findParentByChildId(parent.getId());
		}

		return addresses;
	}
}
