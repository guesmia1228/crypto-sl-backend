package com.nefentus.api.payload.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyCodeRequest {
    private String token;
    private String email;
    private boolean rememberMe;
}
