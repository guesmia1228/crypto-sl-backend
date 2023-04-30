package com.nefentus.api.payload.response;

import com.nefentus.api.entities.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserDisplayAdminResponse {
    private String fullname;
    private Set<String> roles;
    private String email;
    private boolean status;
    private double Income;
    private LocalDate joinedOn;

    public static UserDisplayAdminResponse fromUser(User user) {
        UserDisplayAdminResponse response = new UserDisplayAdminResponse();
        response.setFullname(user.getFirstName() + " " + user.getLastName());
        response.setRoles(user.getRoles().stream()
                .map(role -> role.getName().toString().replace("ROLE_", "").toLowerCase())
                .collect(Collectors.toSet()));
        response.setEmail(user.getEmail());
        response.setStatus(user.getActive());
        // Set the income field here based on your business logic
        response.setIncome(0);
        response.setJoinedOn(user.getCreatedAt().toLocalDateTime().toLocalDate());
        return response;
    }

}
