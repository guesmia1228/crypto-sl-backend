package com.nefentus.api.payload.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardPasswordReset {
    public String newPassword;
    public String oldPassword;
}
