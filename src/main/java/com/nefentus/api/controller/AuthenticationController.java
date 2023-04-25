package com.nefentus.api.controller;


import com.google.zxing.NotFoundException;
import com.nefentus.api.Errors.*;
import com.nefentus.api.Services.*;
import com.nefentus.api.entities.*;
import com.nefentus.api.payload.request.*;
import com.nefentus.api.payload.response.LoginResponse;
import com.nefentus.api.payload.response.MessageResponse;
import com.nefentus.api.payload.response.UpdateResponse;
import com.nefentus.api.payload.response.twoFAResponse;
import com.nefentus.api.repositories.*;
import com.nefentus.api.security.JwtTokenProvider;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;


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
public class AuthenticationController {

    UserService userService;
    KycImageRepository imageRepository;
    @GetMapping(value = "/checkJWTCookie")
    @PreAuthorize("isAuthenticated()")
    public void checkJwt() {
    }

    @GetMapping("/profilePic")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getProfilePicture(Principal principal){
        var profilePicture = userService.getProfilePicture(principal.getName());
        return ResponseEntity.ok(profilePicture);
    }


    @PostMapping(value = "/register")
    public ResponseEntity<?> register(@RequestBody SignUpRequest authRequest) {
        try {
            User created = userService.registerNewUser(authRequest);
            return ResponseEntity.ok(created);
        } catch (UserAlreadyExistsException e){
            return ResponseEntity.badRequest().body(new MessageResponse("User with this email already exists."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Failed to register user. Please try again."));
        }
    }

    @PostMapping(value = "/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest,
                                   HttpServletResponse response) {
        try {
            LoginResponse loginResponse = userService.loginUser(authRequest, response);
            return ResponseEntity.ok(loginResponse);
        } catch (AuthenticationException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Invalid email or password."));
        } catch (InactiveUserException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Failed to login. Please try again."));
        }
    }

    @PostMapping("/upload_kyc")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> saveKycImage(@PathVariable Long userId,
                                          @RequestParam("type") KycImageType type,
                                          @RequestParam("file") MultipartFile file,
                                          Principal principal) {
       try {
           userService.uploadKYCImage(type, principal.getName(), file);
           return  ResponseEntity.ok(new MessageResponse("successfull!"));
       }catch (UserNotFoundException e){
          return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
       }catch (IOException e){
           return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));

       }
    }


    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> handleFileUpload(@RequestParam("file") MultipartFile file,
                                                            @RequestHeader("Authorization") String authorization,
                                                            Principal principal) {
        try {
            String email = principal.getName();
            String profilePictureUrl = userService.uploadProfilePicture(file, email);
            return ResponseEntity.ok(profilePictureUrl);
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse(e.getMessage()));
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse("Error on uploading the file"));
        }
    }

    @GetMapping("/getBlob")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getBlob(){
        var image = imageRepository.findById(1L).get();
        byte[] blobData = image.getData();
        String base64Data = "data:image/jpeg;base64,"+Base64.getEncoder().encodeToString(blobData);
        return ResponseEntity.ok(base64Data);
    }

    @PatchMapping("/update")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateUser(@RequestBody UpdatetUserRequest updatetUserRequest,
                                        Principal principal){
        try {
            String email = principal.getName();
            UpdateResponse updateResponse = userService.updateUser(updatetUserRequest, email);
            return ResponseEntity.ok(updateResponse);
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Failed to update user. Please try again."));
        }
    }

    @PostMapping("/signout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> logoutUser() {
        return ResponseEntity.ok()
                .body(new MessageResponse("You've been signed out!"));
    }

    @PatchMapping("/activate")
    public ResponseEntity<?> activate(@RequestBody String token) {
        try {
            User activatedUser = userService.activateUser(token);
            return ResponseEntity.ok().body(new MessageResponse("Your account has been activated!"));
        } catch (TokenNotFoundException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Failed to activate account. Please try again."));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody String email) {
        try {
            userService.forgotPassword(email);
            return ResponseEntity.ok(new MessageResponse("Password reset email sent!"));
        } catch (UserNotFoundException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        } catch (EmailSendException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            userService.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok(new MessageResponse("Password reset successful!"));
        } catch (InvalidTokenException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @PostMapping("/reset-password-email")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> setResetEmail(@RequestBody DashboardPasswordReset request, Principal principal) {
        try {
            userService.resetEmail(principal.getName(), request.getOldPassword(), request.getNewPassword());
            return ResponseEntity.ok(new MessageResponse("Password reset email sent!"));
        } catch (UserNotFoundException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        } catch (IncorrectPasswordException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        } catch (EmailSendException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @PostMapping("/reset-password-auth")
    public ResponseEntity<?> setNewPassword(@RequestBody DashboardPasswordRequestAuth requestAuth, Principal principal ) {
        try {
            userService.setNewPassword(principal.getName(), requestAuth.getToken());
            return ResponseEntity.ok(new MessageResponse("Password reset successful!"));
        } catch (UserNotFoundException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        } catch (InvalidTokenException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @PostMapping(value = "/2fa", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> add2fa(@RequestBody twoFARequest payload, Principal principal) {
        try {
            var Uri =  userService.setMfa(principal.getName(), payload.isActive());
            return ResponseEntity.ok().body(new twoFAResponse(Uri));
        } catch (UserNotFoundException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyCode(@RequestBody VerifyCodeRequest verifyCodeRequest) {
        try{
        var loginResponse = userService.verify(verifyCodeRequest.getEmail(), verifyCodeRequest.getToken(), verifyCodeRequest.isRememberMe());
        return ResponseEntity.ok(loginResponse);
        }
        catch (UserNotFoundException e){
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
        catch (BadRequestException e){
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
        catch (InternalServerException e){
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }


}
