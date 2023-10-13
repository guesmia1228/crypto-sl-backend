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

import org.apache.http.protocol.HTTP;
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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
	private final OtpService otpService;
	private final KycImageRepository kycImageRepository;
	private final HierarchyRepository hierarchyRepository;
	private final WalletRepository walletRepository;
	@Autowired
	private S3Service s3Service;
	static ClassLoader classLoader = UserService.class.getClassLoader();
	EUSanctionList sanctionList = new EUSanctionList();

	public DashboardNumberResponse calculateRegistrations() {
		long totalClicks = userRepository.count();
		long lastMonthClicks = userRepository
				.countByCreatedAtAfter(Timestamp.valueOf(LocalDateTime.now().minusDays(30)));
		long last30 = totalClicks - lastMonthClicks;
		double percentageIncrease = lastMonthClicks == 0 ? totalClicks * 100
				: ((double) last30 / lastMonthClicks) * 100;
		DashboardNumberResponse totalClicksDto = new DashboardNumberResponse();
		totalClicksDto.setNumber(new BigDecimal(totalClicks));
		totalClicksDto.setPercentage(new BigDecimal(percentageIncrease));
		log.info("Success calculate total users total= {} ", totalClicks);
		return totalClicksDto;
	}

	public DashboardNumberResponse calculateRegistrations(String email) {
		long totalClicks = this.getChildrenRecursive(email).size();
		log.info("TOTAL!!!= {} ", totalClicks);
		long lastMonthClicks = this.calculateRegistrationsForUserBetween(email, 30);
		log.info("LAST MONTH!!!= {} ", lastMonthClicks);
		long last30 = totalClicks - lastMonthClicks;
		double percentageIncrease = lastMonthClicks == 0 ? totalClicks * 100
				: ((double) last30 / lastMonthClicks) * 100;
		DashboardNumberResponse totalClicksDto = new DashboardNumberResponse();
		totalClicksDto.setNumber(new BigDecimal(totalClicks));
		totalClicksDto.setPercentage(new BigDecimal(percentageIncrease));
		log.info("Success to count total users total= {} ", totalClicks);
		return totalClicksDto;
	}

	public long calculateRegistrationsForUserBetween(String email, int daysEnd) {
		LocalDateTime end = LocalDateTime.now().minusDays(daysEnd);

		List<User> users = this.getChildrenRecursive(email);
		int numberOfUsers = 0;
		for (User user : users) {
			LocalDateTime createdAt = user.getCreatedAt().toLocalDateTime();
			if (createdAt.isBefore(end)) {
				numberOfUsers++;
			}
		}

		return numberOfUsers;
	}

	public String getProfilePicture(String email) {
		var user = userRepository.findUserByEmail(email).get();
		log.info("Get profile picture from email= {}", email);
		return Base64.getEncoder().encodeToString(user.getProfilepic());
	}

	public List<User> getChildrenRecursive(String email) {
		List<User> result = new ArrayList<>();

		// Direct children
		List<User> children = hierarchyRepository.findChildByParentEmail(email);
		result.addAll(children);

		// Children of children, etc.
		for (User child : children) {
			result.addAll(getChildrenRecursive(child.getEmail()));
		}
		return result;
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

		List<User> users = this.getChildrenRecursive(email);
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

	public User addUser(AddUserRequest addUserRequest) throws UserAlreadyExistsException, BadRequestException {
		Optional<User> userOptional = userRepository.findUserByEmail(addUserRequest.getEmail());
		if (userOptional.isPresent()) {
			log.error("User with email " + addUserRequest.getEmail() + " already exists!");
			throw new UserAlreadyExistsException("User with email " + addUserRequest.getEmail() + " already exists. ",
					HttpStatus.CONFLICT);
		}

		List<String> csvData = sanctionList.getCSVData();

		for (String csvLine : csvData) {
			if ((csvLine.toLowerCase().contains(addUserRequest.getLastName().toLowerCase()) &&
					csvLine.toLowerCase().contains(addUserRequest.getFirstName().toLowerCase()))
					|| csvLine.toLowerCase().contains(addUserRequest.getEmail().toLowerCase())) {
				log.info("Person {} {} found in sanctions list", addUserRequest.getFirstName(),
						addUserRequest.getLastName());

				log.info("Sanction email sent");
				sendSanctionEmail(addUserRequest.getFirstName() + " " + addUserRequest.getLastName(),
						addUserRequest.getEmail(), "", "");
				throw new BadRequestException("Person found in sanctions list", HttpStatus.FORBIDDEN);
			}
		}

		var user = new User();
		user.setEmail(addUserRequest.getEmail());
		user.setFirstName(addUserRequest.getFirstName());
		user.setLastName(addUserRequest.getLastName());
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
			sendConfirmationEmail(created.getEmail(), created.getToken());
		} catch (Exception e) {
			userRepository.delete(created);
			throw new RuntimeException("Failed to send Confirmation email. Please try again.", e);
		}
		log.info("Create new user with email= {}", addUserRequest.getEmail());
		return created;
	}

	public User addUser(AddUserRequest addUserRequest, String email)
			throws UserAlreadyExistsException, BadRequestException {
		Optional<User> userOptional = userRepository.findUserByEmail(addUserRequest.getEmail());
		if (userOptional.isPresent()) {
			throw new UserAlreadyExistsException("User with email " + addUserRequest.getEmail() + " already exists.",
					HttpStatus.CONFLICT);
		}

		List<String> csvData = sanctionList.getCSVData();

		for (String csvLine : csvData) {
			if ((csvLine.toLowerCase().contains(addUserRequest.getLastName().toLowerCase()) &&
					csvLine.toLowerCase().contains(addUserRequest.getFirstName().toLowerCase()))
					|| csvLine.toLowerCase().contains(addUserRequest.getEmail().toLowerCase())) {
				log.info("Person {} {} found in sanctions list", addUserRequest.getFirstName(),
						addUserRequest.getLastName());

				log.info("Sanction email sent");
				sendSanctionEmail(addUserRequest.getFirstName() + " " + addUserRequest.getLastName(),
						addUserRequest.getEmail(), "", "");
				throw new BadRequestException("Person found in sanctions list", HttpStatus.FORBIDDEN);
			}
		}

		var admin = userRepository.findUserByEmail(email).get();

		var user = new User();
		user.setEmail(addUserRequest.getEmail());
		user.setFirstName(addUserRequest.getFirstName());
		user.setLastName(addUserRequest.getLastName());
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

	public User updateUserAdmin(AddUserRequest addUserRequest) throws UserFoundException, BadRequestException {
		Optional<User> userOptional = userRepository.findUserByEmail(addUserRequest.getEmail());
		if (userOptional.isEmpty()) {
			log.error("User with email " + addUserRequest.getEmail() + " does not exist!");
			throw new UserFoundException("User with email " + addUserRequest.getEmail() + " does not exist. ",
					HttpStatus.CONFLICT);
		}

		return updateUserWithAddRequest(addUserRequest, userOptional.get());
	}

	public User updateUserAdmin(AddUserRequest addUserRequest, String email)
			throws UserFoundException, UserNotFoundException, BadRequestException {
		Optional<User> userOptional = userRepository.findUserByEmail(addUserRequest.getEmail());
		if (userOptional.isEmpty()) {
			log.error("User with email " + addUserRequest.getEmail() + " does not exist!");
			throw new UserFoundException("User with email " + addUserRequest.getEmail() + " does not exist. ",
					HttpStatus.CONFLICT);
		}

		Optional<User> callerOptional = userRepository.findUserByEmail(email);
		if (callerOptional.isEmpty()) {
			log.error("User with email " + email + " does not exist!");
			throw new UserFoundException("User with email " + email + " does not exist. ",
					HttpStatus.CONFLICT);
		}

		User userToUpdate = userOptional.get();
		List<User> parents = this.getParents(userToUpdate);

		if (parents.contains(callerOptional.get())) {
			return updateUserWithAddRequest(addUserRequest, userToUpdate);
		} else {
			throw new BadRequestException("You are not allowed to update this user", HttpStatus.BAD_REQUEST);
		}
	}

	public User updateUserWithAddRequest(AddUserRequest addUserRequest, User userToUpdate) throws BadRequestException {
		userToUpdate.setFirstName(addUserRequest.getFirstName());
		userToUpdate.setLastName(addUserRequest.getLastName());
		userToUpdate.setRoles(setRoles(addUserRequest.getRoles()));
		userToUpdate.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));

		List<String> csvData = sanctionList.getCSVData();

		for (String csvLine : csvData) {
			if ((csvLine.toLowerCase().contains(addUserRequest.getLastName().toLowerCase()) &&
					csvLine.toLowerCase().contains(addUserRequest.getFirstName().toLowerCase()))
					|| csvLine.toLowerCase().contains(addUserRequest.getEmail().toLowerCase())) {
				log.info("Person {} {} found in sanctions list", addUserRequest.getFirstName(),
						addUserRequest.getLastName());

				log.info("Sanction email sent");
				sendSanctionEmailOnUpdate(addUserRequest.getFirstName() + " " + addUserRequest.getLastName(),
						addUserRequest.getEmail().length() > 0 ? addUserRequest.getEmail() : "",
						userToUpdate.getTel().length() > 0 ? userToUpdate.getTel() : "", "",
						userToUpdate.getBusiness().length() > 0 ? userToUpdate.getBusiness() : "");
				userToUpdate.setActive(false);
				userRepository.save(userToUpdate);
				throw new BadRequestException("Person found in sanctions list", HttpStatus.FORBIDDEN);
			}
		}

		User created = userRepository.save(userToUpdate);
		return created;
	}

	private void sendSanctionEmail(String name, String email, String phone, String country) {
		try {
			var html = HtmlProvider.loadSanctionEmail(name, email, phone, country);
			emailService.sendEmail("office@nefentus.com", "Sanction Person!", html);
			emailService.sendEmail("steven@nefentus.com", "Sanction Person!", html);
		} catch (IOException e) {
			log.error("sendSanctionEmail", e);
		}
	}

	private void sendSanctionEmailOnUpdate(String name, String email, String phone, String country, String business) {
		try {
			var html = HtmlProvider.loadSanctionEmailOnUpdate(name, email, phone, country, business);
			emailService.sendEmail("office@nefentus.com", "Sanction Person!", html);
			emailService.sendEmail("steven@nefentus.com", "Sanction Person!", html);
		} catch (IOException e) {
			log.error("sendSanctionEmail", e);
		}
	}

	public User registerNewUser(SignUpRequest authRequest)
			throws UserAlreadyExistsException, AuthenticationException, BadRequestException {
		// Überprüfen, ob ein Benutzer mit der angegebenen E-Mail-Adresse bereits
		// existiert
		Optional<User> userOptional = userRepository.findUserByEmail(authRequest.getEmail());
		if (userOptional.isPresent()) {
			log.error("User with email " + authRequest.getEmail() + " already exists.");
			throw new UserAlreadyExistsException("User with email " + authRequest.getEmail() + " already exists.",
					HttpStatus.CONFLICT);
		}

		List<String> csvData = sanctionList.getCSVData();

		for (String csvLine : csvData) {
			if ((csvLine.toLowerCase().contains(authRequest.getLastName().toLowerCase()) &&
					csvLine.toLowerCase().contains(authRequest.getFirstName().toLowerCase()))
					|| csvLine.toLowerCase().contains(authRequest.getEmail().toLowerCase())
					|| csvLine.toLowerCase().contains(authRequest.getTelNr().toLowerCase())) {
				log.info("Person {} {} found in sanctions list", authRequest.getFirstName(), authRequest.getLastName());

				log.info("Sanction email sent");
				sendSanctionEmail(authRequest.getFirstName() + " " + authRequest.getLastName(), authRequest.getEmail(),
						authRequest.getTelNr(), authRequest.getCountry());
				throw new BadRequestException("Person found in sanctions list", HttpStatus.FORBIDDEN);
			}
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
			user.setToken(null);
			userRepository.save(user);
			log.info("Activate user Successful");
			return user;
		} else {
			log.error("Can not found user with this token! ");
			throw new TokenNotFoundException("Token is not found!", HttpStatus.BAD_REQUEST);
		}
	}

	public boolean activateUserByEmail(String email) {
		Optional<User> userOptional = userRepository.findUserByEmail(email);
		if (userOptional.isPresent()) {
			User user = userOptional.get();
			user.setActive(true);
			userRepository.save(user);
			log.info("Activate user successful");
			return true;
		} else {
			log.error("Can not find user with this email!");
		}
		return false;
	}

	public boolean deactivateUserByEmail(String email) {
		Optional<User> userOptional = userRepository.findUserByEmail(email);
		if (userOptional.isPresent()) {
			User user = userOptional.get();
			user.setActive(false);
			userRepository.save(user);
			log.info("Deactivate user successful");
			return true;
		} else {
			log.error("Can not find user with this email!");
		}
		return false;
	}

	public boolean deleteUser(String email) {
		Optional<User> userOptional = userRepository.findUserByEmail(email);
		if (userOptional.isPresent()) {
			// Delete wallets before deleting user
			List<Wallet> wallets = walletRepository.findByOwner(userOptional.get());
			for (Wallet wallet : wallets) {
				walletRepository.delete(wallet);
			}

			// Delete user
			userRepository.delete(userOptional.get());
			log.info("Deleted user {} successful", email);
			return true;
		} else {
			log.error("Can not delete user with email {}", email);
		}
		return false;
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
		if (!user.isMfa() && !user.isRequireOtp()) {
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
					user.isRequireOtp(),
					user.getAntiPhishingCode(),
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
					user.isRequireOtp(),
					"",
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
			throws UserNotFoundException, BadRequestException {
		// Benutzer suchen
		User user = userRepository.findUserByEmail(email)
				.orElseThrow(() -> new UserNotFoundException("User not found", HttpStatus.NOT_FOUND));

		List<String> csvData = sanctionList.getCSVData();

		// Benutzer aktualisieren
		user.setBusiness(updatetUserRequest.getBusiness());
		user.setFirstName(updatetUserRequest.getFirstName());
		user.setLastName(updatetUserRequest.getLastName());
		user.setTel(updatetUserRequest.getPhoneNumber());
		user.setMfa(updatetUserRequest.isMfa());
		user.setRequireOtp(updatetUserRequest.isRequireOtp());

		for (String csvLine : csvData) {
			if ((csvLine.toLowerCase().contains(updatetUserRequest.getLastName().toLowerCase()) &&
					csvLine.toLowerCase().contains(updatetUserRequest.getFirstName().toLowerCase())
					|| csvLine.toLowerCase().contains(updatetUserRequest.getBusiness().toLowerCase()))
					|| csvLine.toLowerCase().contains(user.getEmail().toLowerCase())
					|| csvLine.toLowerCase().contains(updatetUserRequest.getPhoneNumber().toLowerCase())) {
				log.info("Person {} {} found in sanctions list", updatetUserRequest.getFirstName(),
						updatetUserRequest.getLastName());

				log.info("Sanction email sent");
				sendSanctionEmailOnUpdate(updatetUserRequest.getFirstName() + " " + updatetUserRequest.getLastName(),
						user.getEmail(), updatetUserRequest.getPhoneNumber(), user.getCountry(),
						updatetUserRequest.getBusiness());
				user.setActive(false);
				userRepository.save(user);
				throw new BadRequestException("Person found in sanctions list", HttpStatus.FORBIDDEN);
			}
		}
		user.setAntiPhishingCode(updatetUserRequest.getAntiPhishingCode());
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
				"",
				savedUser.getAntiPhishingCode());
	}

	public boolean deleteProfileImage(String email)
			throws UserNotFoundException {
		User user = userRepository.findUserByEmail(email)
				.orElseThrow(() -> new UserNotFoundException("User not found", HttpStatus.NOT_FOUND));

		// Delete image from S3
		String s3key = user.getS3Url();
		if (s3key != null && !s3key.isEmpty()) {
			s3Service.delete(s3key);
			user.setS3Url(null);
			userRepository.save(user);
		}

		return true;
	}

	public static String generateRandomCode(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder code = new StringBuilder();

        Random random = new Random();

        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(characters.length());
            char randomChar = characters.charAt(randomIndex);
            code.append(randomChar);
        }

        return code.toString();
    }

	public void changeEmail(String newEmail, String oldEmail)
			throws UserNotFoundException,
			EmailSendException {
		User user = userRepository.findUserByEmail(oldEmail)
				.orElseThrow(() -> new UserNotFoundException("User not found", HttpStatus.BAD_REQUEST));

		String resetToken = generateRandomCode(8);
		user.setResetToken(resetToken);
		log.info("Found user with email= {}", oldEmail);
		userRepository.save(user);

		Optional<User> usedEmail= userRepository.findUserByEmail(newEmail);

		if(usedEmail.isPresent()){
			throw new UserNotFoundException("Email is already used", HttpStatus.BAD_REQUEST);
		}

		try {
			log.info("Email change email send to user");
			sendChangeEmail(oldEmail, resetToken);
		} catch (Exception e) {
			log.error("Failed to send email change email={}", oldEmail);
			throw new EmailSendException("Failed to send email change email", HttpStatus.BAD_REQUEST);
		}
	}

	private void sendChangeEmail(String email, String token) {
		try {
			var html = HtmlProvider.loadHtmlEmailChange(token);
			emailService.sendEmail(email, "Confirm email change!", html);
		} catch (IOException e) {
			log.error("sendEmailChangeEmail", e);
		}
	}

	public UpdateResponse confirmEmail(ChangeEmailRequest changeEmailRequest, String email)
		throws UserNotFoundException {
		User user = userRepository.findUserByEmail(email)
				.orElseThrow(() -> new UserNotFoundException("User not found", HttpStatus.NOT_FOUND));

		if(!changeEmailRequest.token.equals(user.getResetToken())){
			log.info("Token provided"+changeEmailRequest.token);
			log.info("Token in db"+user.getResetToken());
			log.info(changeEmailRequest.token==user.getResetToken()? "true":"false");
			throw new UserNotFoundException("Token not found", HttpStatus.BAD_REQUEST);
		}
		if(changeEmailRequest.newEmail.length()==0){
			throw new UserNotFoundException("New email is empty", HttpStatus.BAD_REQUEST);
		}
		log.info("New email is: {}", changeEmailRequest.newEmail);
		user.setEmail(changeEmailRequest.newEmail);
		User savedUser = userRepository.save(user);
		log.info("Successfully changed email for user with email= {}", savedUser.getEmail());
		return new UpdateResponse(
				savedUser.getEmail(),
				savedUser.getEmail(),
				savedUser.getFirstName(),
				savedUser.getLastName(),
				savedUser.getProfilePicturepath(),
				savedUser.getBusiness(),
				savedUser.getTel(),
				"", email);
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

	public void resetPassword(String token, String newPassword)
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

	public LoginResponse verifyOTP(String email,
			Integer code,
			boolean longToken)
			throws UserNotFoundException,
			BadRequestException,
			InternalServerException {
		User user = userRepository
				.findUserByEmail(email)
				.orElseThrow(() -> new UserNotFoundException(String.format("email %s", email), HttpStatus.BAD_REQUEST));
		if (!otpService.validateOTP(email, code)) {
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

		return new LoginResponse(
				optUser,
				email,
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
				user.isRequireOtp(),
				user.getAntiPhishingCode(),
				user.getId());

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
				user.isRequireOtp(),
				user.getAntiPhishingCode(),
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
			emailService.sendEmail(email, "Change your password", html);
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
					case "IB-Leader", "IB Leader", "Leader" -> {
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
			addresses.setSellerAddress(wallets.get(0).getAddress());
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
							addresses.setAffiliateAddress(wallets.get(0).getAddress());
						break;
					case ROLE_BROKER:
						if (addresses.getBrokerAddress() == null)
							addresses.setBrokerAddress(wallets.get(0).getAddress());
						break;
					case ROLE_SENIOR_BROKER:
						if (addresses.getSeniorBrokerAddress() == null)
							addresses.setSeniorBrokerAddress(wallets.get(0).getAddress());
						break;
					case ROLE_LEADER:
						if (addresses.getLeaderAddress() == null)
							addresses.setLeaderAddress(wallets.get(0).getAddress());
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

	public List<User> getParents(User user) throws UserNotFoundException {
		List<User> parents = new ArrayList<User>();

		Optional<User> optParent = hierarchyRepository.findParentByChildId(user.getId());
		while (optParent.isPresent()) {
			User parent = optParent.get();
			parents.add(parent);

			// Get the new parent
			optParent = hierarchyRepository.findParentByChildId(parent.getId());
		}

		return parents;
	}

	public boolean acceptKYC(Long id) {
		Optional<User> userOptional = userRepository.findById(id);
		log.info("Found user with id= {}", id);
		if (userOptional.isPresent()) {
			for(KycImageType imageType: KycImageType.values()){
				Optional<KycImage> kycImageOpt = Optional
				.ofNullable(kycImageRepository.findKycImageByTypeAndUser_Id(imageType, id));
				if (kycImageOpt.isPresent()) {
					if(kycImageOpt.get().getS3Key()==null){
						continue;
					}
					if(kycImageOpt.get().getS3Key().length()==0 || kycImageOpt.get().getConfirmed()==true || kycImageOpt.get().getS3Key()==null){
						continue;
					}
					kycImageOpt.get().setConfirmed(true);
					kycImageRepository.save(kycImageOpt.get());
                	log.info("KYC approved for user with id = {} and image type = {}", id, imageType);
				} 
			}
			return true;
		} else {
        	log.error("User with id = {} not found!", id);
		}
		return false;
	}	
	
	public boolean declineKYC(Long id) {
		Optional<User> userOptional = userRepository.findById(id);
		log.info("Found user with id= {}", id);
		if (userOptional.isPresent()) {
			for(KycImageType imageType: KycImageType.values()){
				Optional<KycImage> kycImageOpt = Optional
				.ofNullable(kycImageRepository.findKycImageByTypeAndUser_Id(imageType, id));
				if (kycImageOpt.isPresent()) {
					if(kycImageOpt.get().getConfirmed()!=true){
						kycImageRepository.delete(kycImageOpt.get());
					}

                	log.info("KYC deleted for user with id = {} and image type = {}", id, imageType);
				} 
			}
			return true;
		} else {
        	log.error("User with id = {} not found!", id);
		}
		return false;
	}
}
