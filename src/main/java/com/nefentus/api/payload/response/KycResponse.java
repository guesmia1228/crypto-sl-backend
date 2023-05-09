package com.nefentus.api.payload.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class KycResponse {
    String url;
    boolean isVerify;
}
