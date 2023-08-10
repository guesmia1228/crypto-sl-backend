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
	private final AffiliateRepository affiliateRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final AuthenticationManager authenticationManager;
	private final RoleRepository roleRepository;
	private final PasswordResetTokenService resetTokenService;
	private final PasswordResetTokenRepository resetTokenRepository;
	private final TotpManager totpManager;
	private final KycImageRepository kycImageRepository;
	private final TransactionService transactionService;
	private final HierarchyRepository hierarchyRepository;
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
		var users = userRepository.findAll();
		List<Map<String, Object>> report = new ArrayList<>();
		// Initialize the report data
		Map<String, Object> vendorData = new HashMap<>();
		vendorData.put("role", "Vendor");
		vendorData.put("percentage", 0);
		vendorData.put("count", 0);

		Map<String, Object> affiliateData = new HashMap<>();
		affiliateData.put("role", "Affiliate");
		affiliateData.put("percentage", 0);
		affiliateData.put("count", 0);

		Map<String, Object> diamondData = new HashMap<>();
		diamondData.put("role", "Senior IB");
		diamondData.put("percentage", 0);
		diamondData.put("count", 0);

		Map<String, Object> goldData = new HashMap<>();
		goldData.put("role", "IB");
		goldData.put("percentage", 0);
		goldData.put("count", 0);

		Map<String, Object> others = new HashMap<>();
		others.put("role", "Others");
		others.put("percentage", 0);
		others.put("count", 0);

		Map<String, Object> ibLeaderData = new HashMap<>();
		ibLeaderData.put("role", "IB Leader");
		ibLeaderData.put("percentage", 0);
		ibLeaderData.put("count", 0);

		int totalUsers = users.size();
		for (User user : users) {
			Set<Role> roles = user.getRoles();
			totalUsers += user.getRoles().size() - 1;
			for (Role role : roles) {
				switch (role.getName().label) {
					case "ROLE_VENDOR":
						vendorData.put("count", (Integer) vendorData.get("count") + 1);
						break;
					case "ROLE_AFFILIATE":
						affiliateData.put("count", (Integer) affiliateData.get("count") + 1);
						break;
					case "ROLE_SENIOR_IB":
						diamondData.put("count", (Integer) diamondData.get("count") + 1);
						break;
					case "ROLE_IB":
						goldData.put("count", (Integer) goldData.get("count") + 1);
						break;
					case "ROLE_IB_LEADER":
						ibLeaderData.put("count", (Integer) ibLeaderData.get("count") + 1);
					default:
						others.put("count", (Integer) others.get("count") + 1);
						break;
				}
			}
		}

		// Calculate the total percentages and add the report data to the list
		if (totalUsers > 0) {
			log.info("Start process to calculate the total percentages and add the report data to the list! ");
			vendorData.put("percentage", ((Integer) vendorData.get("count") * 100) / totalUsers);
			affiliateData.put("percentage", ((Integer) affiliateData.get("count") * 100) / totalUsers);
			diamondData.put("percentage", ((Integer) diamondData.get("count") * 100) / totalUsers);
			goldData.put("percentage", ((Integer) goldData.get("count") * 100) / totalUsers);
			others.put("percentage", ((Integer) others.get("count") * 100) / totalUsers);
			ibLeaderData.put("percentage", ((Integer) ibLeaderData.get("count") * 100) / totalUsers);
		}

		report.add(vendorData);
		report.add(affiliateData);
		report.add(diamondData);
		report.add(goldData);
		report.add(ibLeaderData);
		report.add(others);
		log.info("Successful to make a report with totalUser= {} ", totalUsers);
		return report;
	}

	public List<Map<String, Object>> getRolesStatus(String email) {
		var users = hierarchyRepository.findChildByParentEmail(email);
		List<Map<String, Object>> report = new ArrayList<>();
		// Initialize the report data
		Map<String, Object> vendorData = new HashMap<>();
		vendorData.put("role", "Vendor");
		vendorData.put("percentage", 0);
		vendorData.put("count", 0);

		Map<String, Object> affiliateData = new HashMap<>();
		affiliateData.put("role", "Affiliate");
		affiliateData.put("percentage", 0);
		affiliateData.put("count", 0);

		Map<String, Object> diamondData = new HashMap<>();
		diamondData.put("role", "Senior IB");
		diamondData.put("percentage", 0);
		diamondData.put("count", 0);

		Map<String, Object> goldData = new HashMap<>();
		goldData.put("role", "IB");
		goldData.put("percentage", 0);
		goldData.put("count", 0);

		Map<String, Object> others = new HashMap<>();
		others.put("role", "Others");
		others.put("percentage", 0);
		others.put("count", 0);

		int totalUsers = users.size();
		for (User user : users) {
			Set<Role> roles = user.getRoles();
			totalUsers += user.getRoles().size() - 1;
			for (Role role : roles) {
				switch (role.getName().label) {
					case "ROLE_VENDOR":
						vendorData.put("count", (Integer) vendorData.get("count") + 1);
						break;
					case "ROLE_AFFILIATE":
						affiliateData.put("count", (Integer) affiliateData.get("count") + 1);
						break;
					case "ROLE_SENIOR_IB":
						diamondData.put("count", (Integer) diamondData.get("count") + 1);
						break;
					case "ROLE_IB":
						goldData.put("count", (Integer) goldData.get("count") + 1);
						break;
					default:
						others.put("count", (Integer) others.get("count") + 1);
						break;
				}
			}
		}

		// Calculate the total percentages and add the report data to the list
		if (totalUsers > 0) {
			vendorData.put("percentage", ((Integer) vendorData.get("count") * 100) / totalUsers);
			affiliateData.put("percentage", ((Integer) affiliateData.get("count") * 100) / totalUsers);
			diamondData.put("percentage", ((Integer) diamondData.get("count") * 100) / totalUsers);
			goldData.put("percentage", ((Integer) goldData.get("count") * 100) / totalUsers);
			others.put("percentage", ((Integer) others.get("count") * 100) / totalUsers);
		}

		report.add(vendorData);
		report.add(affiliateData);
		report.add(diamondData);
		report.add(goldData);
		report.add(others);
		log.info("Successful to make a report with totalUser= {} ", totalUsers);
		return report;
	}

	public List<UserDisplayAdminResponse> getAllUsers() {
		return userRepository.findAll().stream()
				.map(user -> {
					UserDisplayAdminResponse response = UserDisplayAdminResponse.fromUser(user);
					response.setIncome(transactionService.getTotalPriceByEmail(user.getEmail()));
					return response;
				})
				.collect(Collectors.toList());
	}

	public List<UserDisplayAdminResponse> getAllUsers(String email) {
		var users = hierarchyRepository.findChildByParentEmail(email);
		log.info("Successful query all users");
		return users.stream()
				.map(user -> {
					UserDisplayAdminResponse response = UserDisplayAdminResponse.fromUser(user);
					response.setIncome(transactionService.getTotalPriceByEmail(user.getEmail()));
					return response;
				})
				.collect(Collectors.toList());
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

		boolean hasRoleDiamondPartner = false;
		for (Role role : user.getRoles()) {
			if (role.getName() == ERole.ROLE_DIAMOND_PARTNER) {
				hasRoleDiamondPartner = true;
				break;
			}
		}

		var hierarchy = new Hierarchy();
		hierarchy.setChild(created);
		hierarchy.setParent(admin);
		hierarchy.setRelationshipType(hasRoleDiamondPartner ? RelationshipType.DIAMOND : RelationshipType.GOLD);
		hierarchy.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
		switch (hierarchy.getRelationshipType()) {
			case DIAMOND:
				hierarchy.setCommissionRate(new BigDecimal("0.0125"));
				break;
			case GOLD:
				hierarchy.setCommissionRate(new BigDecimal("0.0125"));
				break;
			// add cases for other RelationshipType values as needed
			default:
				hierarchy.setCommissionRate(new BigDecimal(0)); // set a default commission rate if no match is found
		}

		var savedHierarchy = hierarchyRepository.save(hierarchy);

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
		if (authRequest.getAffiliate() != null && !authRequest.getAffiliate().isEmpty()) {
			Affiliate aff = new Affiliate(null, authRequest.getAffiliate(), new BigDecimal("0.15"),
					Timestamp.valueOf(LocalDateTime.now()),
					created);
			affiliateRepository.save(aff);
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
			throws AuthenticationException, InactiveUserException {
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
		User user = userRepository.findUserByEmail(email)
				.orElseThrow(() -> new UserNotFoundException("User not found", HttpStatus.NOT_FOUND));
		String filename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename())).replace(" ", "");
		String s3Key = UUID.randomUUID().toString().concat("_").concat(filename);
		byte[] data = file.getBytes();
		String url = s3Service.uploadToS3BucketProfilePic(new ByteArrayInputStream(data), s3Key);

		user.setS3Url(url);
		userRepository.save(user);
		log.info("Successful upload profile Picture");
		return url;
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
		if (strRoles == null) {
			Role userRole = roleRepository.findByName(ERole.ROLE_USER)
					.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
			roles.add(userRole);
		} else {
			strRoles.forEach(role -> {
				switch (role) {
					case "Affiliate" -> {
						Role affRole = roleRepository.findByName(ERole.ROLE_AFFILIATE)
								.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
						roles.add(affRole);
					}
					case "Gold-Partner", "Gold Partner", "IB" -> {
						Role goldP = roleRepository.findByName(ERole.ROLE_GOLD_PARTNER)
								.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
						roles.add(goldP);
					}
					case "Diamond-Partner", "Diamond Partner", "Senior IB" -> {
						Role diaP = roleRepository.findByName(ERole.ROLE_DIAMOND_PARTNER)
								.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
						roles.add(diaP);
					}
					case "IB-Leader", "IB Leader" -> {
						Role IblP = roleRepository.findByName(ERole.ROLE_IB_LEADER)
								.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
						roles.add(IblP);
					}
					default -> {
						Role userRole = roleRepository.findByName(ERole.ROLE_VENDOR)
								.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
						roles.add(userRole);
					}
				}
			});
		}
		return roles;
	}

}
