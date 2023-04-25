package com.nefentus.api.payload.request;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {
    public String token;
    public String newPassword;
}
