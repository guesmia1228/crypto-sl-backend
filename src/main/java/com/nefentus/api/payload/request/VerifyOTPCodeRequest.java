package com.nefentus.api.payload.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyOTPCodeRequest {
    private String email;
    private Integer code;
    private boolean rememberMe;
}
