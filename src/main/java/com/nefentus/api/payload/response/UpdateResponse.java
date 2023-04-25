package com.nefentus.api.payload.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateResponse {
    public String email;
    public String contactEmail;
    public String firstName;
    public String lastName;
    public String imgData;
    public String business;
    public String phoneNumber;
    public String username;
}
