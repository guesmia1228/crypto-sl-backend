package com.nefentus.api.Services;

import ch.qos.logback.core.encoder.EchoEncoder;
import com.nefentus.api.Errors.*;
import com.nefentus.api.entities.*;
import com.nefentus.api.payload.request.*;
import com.nefentus.api.payload.response.*;
import com.nefentus.api.repositories.*;
import com.nefentus.api.security.CustomUserDetails;
import com.nefentus.api.security.JwtTokenProvider;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UserService {
    private UserRepository userRepository;
    private AffiliateRepository affiliateRepository;
    private PasswordEncoder passwordEncoder;
    private JavaMailSender mailSender;
    private JwtTokenProvider jwtTokenProvider;
    private AuthenticationManager authenticationManager;
    private RoleRepository roleRepository;
    private PasswordResetTokenService resetTokenService;
    private PasswordResetTokenRepository resetTokenRepository;
    private TotpManager totpManager;
    private KycImageRepository kycImageRepository;
    private TransactionService transactionService;
    private HierarchyRepository hierarchyRepository;

    public DashboardNumberResponse calculateTotalClicks() {
        long totalClicks = userRepository.count();
        long lastMonthClicks = userRepository.countByCreatedAtAfter(Timestamp.valueOf(LocalDateTime.now().minusDays(30)));
        long last30 = totalClicks - lastMonthClicks;
        double percentageIncrease = last30 == 0 ? totalClicks * 100 : ((double) (lastMonthClicks - last30) / last30) * 100;
        DashboardNumberResponse totalClicksDto = new DashboardNumberResponse();
        totalClicksDto.setNumber(totalClicks);
        totalClicksDto.setPercentage(percentageIncrease);
        return totalClicksDto;
    }

    public DashboardNumberResponse calculateTotalClicks(String email) {
        long totalClicks = hierarchyRepository.countByParentEmail(email);
        long lastMonthClicks = hierarchyRepository.countByCreatedAtAfterAndParentEmail(Timestamp.valueOf(LocalDateTime.now().minusDays(30)), email);
        long last30 = totalClicks - lastMonthClicks;
        double percentageIncrease = last30 == 0 ? totalClicks * 100 : ((double) (lastMonthClicks - last30) / last30) * 100;
        DashboardNumberResponse totalClicksDto = new DashboardNumberResponse();
        totalClicksDto.setNumber(totalClicks);
        totalClicksDto.setPercentage(percentageIncrease);
        return totalClicksDto;
    }

    public String getProfilePicture(String email){
        var user = userRepository.findUserByEmail(email).get();
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
        diamondData.put("role", "Diamond");
        diamondData.put("percentage", 0);
        diamondData.put("count", 0);

        Map<String, Object> goldData = new HashMap<>();
        goldData.put("role", "Gold");
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
                switch (role.getName()) {
                    case ROLE_VENDOR:
                        vendorData.put("count", (Integer) vendorData.get("count") + 1);
                        break;
                    case ROLE_AFFILIATE:
                        affiliateData.put("count", (Integer) affiliateData.get("count") + 1);
                        break;
                    case ROLE_DIAMOND_PARTNER:
                        diamondData.put("count", (Integer) diamondData.get("count") + 1);
                        break;
                    case ROLE_GOLD_PARTNER:
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
        diamondData.put("role", "Diamond");
        diamondData.put("percentage", 0);
        diamondData.put("count", 0);

        Map<String, Object> goldData = new HashMap<>();
        goldData.put("role", "Gold");
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
                switch (role.getName()) {
                    case ROLE_VENDOR:
                        vendorData.put("count", (Integer) vendorData.get("count") + 1);
                        break;
                    case ROLE_AFFILIATE:
                        affiliateData.put("count", (Integer) affiliateData.get("count") + 1);
                        break;
                    case ROLE_DIAMOND_PARTNER:
                        diamondData.put("count", (Integer) diamondData.get("count") + 1);
                        break;
                    case ROLE_GOLD_PARTNER:
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
            throw new UserNotFoundException("User with email +  " + changeUserStateRequest.getUseremail() + " is not found");
        }
        var user = optUser.get();
        user.setActive(!user.getActive());
        return userRepository.save(user);
    }

    public User addUser(AddUserRequest addUserRequest) throws UserAlreadyExistsException {
        Optional<User> userOptional = userRepository.findUserByEmail(addUserRequest.getEmail());
        if (userOptional.isPresent()) {
            throw new UserAlreadyExistsException("User with email " + addUserRequest.getEmail() + " already exists.");
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
        //    sendConfirmationEmail(created.getEmail(), created.getToken());
        } catch (Exception e) {
            userRepository.delete(created);
            throw new RuntimeException("Failed to send Confirmation email. Please try again.", e);
        }

        return created;
    }


    public User addUser(AddUserRequest addUserRequest, String email) throws UserAlreadyExistsException {
        Optional<User> userOptional = userRepository.findUserByEmail(addUserRequest.getEmail());
        if (userOptional.isPresent()) {
            throw new UserAlreadyExistsException("User with email " + addUserRequest.getEmail() + " already exists.");
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
                hierarchy.setCommissionRate(1.25f);
                break;
            case GOLD:
                hierarchy.setCommissionRate(1.25f);
                break;
            // add cases for other RelationshipType values as needed
            default:
                hierarchy.setCommissionRate(0.0f); // set a default commission rate if no match is found
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
            throws UserAlreadyExistsException {
        // Überprüfen, ob ein Benutzer mit der angegebenen E-Mail-Adresse bereits existiert
        Optional<User> userOptional = userRepository.findUserByEmail(authRequest.getEmail());
        if (userOptional.isPresent()) {
            throw new UserAlreadyExistsException("User with email " + authRequest.getEmail() + " already exists.");
        }
        // Benutzer erstellen und speichern
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
        User created = userRepository.save(user);

        // Affiliate erstellen und speichern, falls vorhanden
        if (authRequest.getAffiliate() != null && !authRequest.getAffiliate().isEmpty()) {
            Affiliate aff = new Affiliate(null, authRequest.getAffiliate(), 15F, Timestamp.valueOf(LocalDateTime.now()), created);
            affiliateRepository.save(aff);
        }

        // Bestätigungsemail senden
        try {
           // sendConfirmationEmail(created.getEmail(), created.getToken());
        } catch (Exception e) {
            userRepository.delete(created);
            throw new RuntimeException("Failed to send Confirmation email. Please try again.", e);
        }

        return created;
    }

    public User activateUser(String token)
            throws TokenNotFoundException {
        Optional<User> userOptional = userRepository.findByToken(token);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setActive(true);
            userRepository.save(user);
            return user;
        } else {
            throw new TokenNotFoundException("Token is not found!");
        }
    }

    public LoginResponse loginUser(AuthRequest authRequest,
                                   HttpServletResponse response)
            throws AuthenticationException, InactiveUserException {
        // Authentifizierung des Benutzers
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authRequest.getEmail(),
                        authRequest.getPassword()
                )
        );

        // Überprüfen, ob der Benutzer aktiv ist
        User user = userRepository.findUserByEmail(authRequest.getEmail()).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        if (!user.getActive()) {
            throw new InactiveUserException("Please activate your account!");
        }

        // LoginResponse erstellen
        if (!user.isMfa()) {
            return new LoginResponse(
                    jwtTokenProvider.generateToken(authentication, authRequest.rememberMe),
                    authRequest.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getAffiliateLink(),
                    Base64.getEncoder().encodeToString(user.getProfilepic() != null ? user.getProfilepic() : new byte[]{}),
                    user.getBusiness(),
                    user.getTel(),
                    user.getRoles().stream()
                            .map(Role::getName)
                            .map(Enum::name)
                            .toArray(String[]::new),
                    user.isMfa()
            );
        } else {
            return new LoginResponse(
                    "",
                    user.getEmail(),
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    new String[]{},
                    user.isMfa()
            );
        }
    }

    public void uploadKYCImage(KycImageType type, String email, MultipartFile file) throws UserNotFoundException, IOException {
        User user = userRepository.findUserByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        String filename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        byte[] data = file.getBytes();
        KycImage kycImage = new KycImage(null, type, false, data, user);
        kycImageRepository.save(kycImage);
    }

    public String uploadProfilePicture(MultipartFile file,
                                       String email)
            throws IOException,
            UserNotFoundException {
        // Benutzer suchen
        User user = userRepository.findUserByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found"));
        user.setProfilepic(file.getBytes());
        userRepository.save(user);
        return Base64.getEncoder().encodeToString(file.getBytes());
    }

    public UpdateResponse updateUser(UpdatetUserRequest updatetUserRequest,
                                     String email)
            throws UserNotFoundException {
        // Benutzer suchen
        User user = userRepository.findUserByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found"));

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
        return new UpdateResponse(
                savedUser.getEmail(),
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getProfilePicturepath(),
                savedUser.getBusiness(),
                savedUser.getTel(),
                ""
        );
    }

    public void forgotPassword(String email)
            throws UserNotFoundException,
            EmailSendException {
        // Benutzer suchen
        User user = userRepository.findUserByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found"));

        // Reset-Token generieren und in der Datenbank speichern
        String resetToken = passwordEncoder.encode(user.getEmail() + LocalDateTime.now().toString());
        user.setResetToken(resetToken);
        userRepository.save(user);

        // Reset-Passwort-Email senden
        try {
            sendResetPasswordEmail(user.getEmail(), resetToken);
        } catch (Exception e) {
            throw new EmailSendException("Failed to send reset password email");
        }
    }

    public void resetPassword(String token,
                              String newPassword)
            throws InvalidTokenException {
        // Benutzer suchen
        User user = userRepository.findByResetToken(token).orElseThrow(() -> new InvalidTokenException("Invalid reset token"));

        // Passwort aktualisieren und Reset-Token entfernen
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        userRepository.save(user);
    }

    public void resetEmail(String email,
                           String oldPassword,
                           String newPassword)
            throws UserNotFoundException,
            IncorrectPasswordException,
            EmailSendException {
        // Benutzer suchen
        User user = userRepository.findUserByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found"));

        // Überprüfen, ob das alte Passwort korrekt ist
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IncorrectPasswordException("Old password is not correct");
        }

        // Passwort-Reset-Token erstellen und in der Datenbank speichern
        String resetToken = passwordEncoder.encode(user.getEmail() + LocalDateTime.now().toString());
        PasswordResetToken createdToken = resetTokenService.createPasswordResetTokenForUser(user, passwordEncoder.encode(newPassword));

        // Reset-Passwort-Email senden
        try {
            sendResetEmail(user.getEmail(), createdToken.getToken());
        } catch (Exception e) {
            throw new EmailSendException("Failed to send reset password email");
        }
    }

    public void setNewPassword(String email,
                               String token)
            throws UserNotFoundException,
            InvalidTokenException {
        // Benutzer suchen
        User user = userRepository.findUserByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found"));

        // Überprüfen, ob das Token vorhanden und noch gültig ist
        Optional<PasswordResetToken> tokenOptional = resetTokenRepository.findByToken(token);
        if (tokenOptional.isEmpty() || tokenOptional.get().getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Invalid reset token");
        }

        // Passwort aktualisieren und Reset-Token entfernen
        PasswordResetToken resetToken = tokenOptional.get();
        user.setPassword(resetToken.getNewPassword());
        userRepository.save(user);
        resetTokenRepository.delete(resetToken);
    }

    public String setMfa(String email,
                         boolean isActive)
            throws UserNotFoundException {
        // Benutzer suchen
        User user = userRepository.findUserByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found"));
        // 2FA-Status und geheimes Schlüssel aktualisieren, falls aktiviert
        user.setMfa(isActive);
        if (isActive) {
            user.setSecret(totpManager.generateSecret());
        }
        userRepository.save(user);

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
                .orElseThrow(() -> new UserNotFoundException(String.format("email %s", email)));
        if (!totpManager.verifyCode(code, user.getSecret())) {
            throw new BadRequestException("Code is incorrect");
        }
        var optUser = Optional.of(user)
                .map(CustomUserDetails::build)
                .map(userDetailsToken -> new UsernamePasswordAuthenticationToken(
                        userDetailsToken, null, userDetailsToken.getAuthorities()))
                .map(userDetailsToken -> jwtTokenProvider.generateToken(userDetailsToken, longToken))
                .orElseThrow(() ->
                        new InternalServerException("unable to generate access token"));

        if (optUser.isEmpty()) {
            throw new UserNotFoundException("User not found");
        }

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
                user.isMfa()
        );

    }

    private void sendResetEmail(String email,
                                String token)
            throws Exception {
        var html = HtmlProvider.loadResetTokenMail(token);
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom("noreply@nefentus.com");
        helper.setTo(email);
        helper.setSubject("Please activate your account");
        helper.setText(html, true);
        mailSender.send(message);
    }

    private void sendResetPasswordEmail(String email,
                                        String token)
            throws Exception {
        var html = HtmlProvider.loadHtmlFileReset(token);
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom("noreply@nefentus.com");
        helper.setTo(email);
        helper.setSubject("Reset your password!");
        helper.setText(html, true);
        mailSender.send(message);
    }

    private void sendConfirmationEmail(String email,
                                       String token)
            throws Exception {

        var html = HtmlProvider.loadHtmlFile(token);
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom("noreply@nefentus.com");
        helper.setTo(email);
        helper.setSubject("Please activate your account");
        helper.setText(html, true);
        mailSender.send(message);
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
                    case "Gold-Partner", "Gold Partner" -> {
                        Role goldP = roleRepository.findByName(ERole.ROLE_GOLD_PARTNER)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(goldP);
                    }
                    case "Diamond-Partner", "Diamond Partner" -> {
                        Role diaP = roleRepository.findByName(ERole.ROLE_DIAMOND_PARTNER)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(diaP);
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
