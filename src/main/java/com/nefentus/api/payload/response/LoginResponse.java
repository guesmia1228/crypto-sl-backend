package com.nefentus.api.payload.response;

import com.nefentus.api.entities.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@AllArgsConstructor
@Getter
@Setter
public class LoginResponse {
    public String jwtToken;
    public String email;
    public String firstName;
    public String lastName;
    public String affiliateLink;
    public String imgData;
    public String business;
    public String phoneNumber;
    public String[] roles;
    public boolean isMfa;
    //pic data
}
