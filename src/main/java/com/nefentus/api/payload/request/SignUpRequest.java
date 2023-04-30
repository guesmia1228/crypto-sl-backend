package com.nefentus.api.payload.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class SignUpRequest {

    @NotBlank
    @Size(max = 70)
    @Email
    private String email;

    private Set<String> roles;

    @NotBlank
    @Size(min = 6, max = 70)
    private String password;

    @NotBlank
    @Size(min = 2, max = 70)
    private String firstName;

    @NotBlank
    @Size(min = 2, max = 70)
    private String lastName;

    @NotBlank
    @Size(min = 6, max = 70)
    private String telNr;

    private String affiliate;

}
