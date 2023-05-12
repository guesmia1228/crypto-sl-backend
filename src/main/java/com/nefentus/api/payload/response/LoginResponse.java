package com.nefentus.api.payload.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

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
    public String profileImage;
    public String country;
    public boolean isRequireKyc;
    public Long userId;
    //pic data
}
