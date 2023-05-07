package com.nefentus.api.Services.Init;

import com.nefentus.api.entities.*;
import com.nefentus.api.repositories.HierarchyRepository;
import com.nefentus.api.repositories.RoleRepository;
import com.nefentus.api.repositories.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Set;

@Component
@AllArgsConstructor
public class InitDataGenerator implements CommandLineRunner {
    public RoleRepository roleRepository;
    public UserRepository userRepository;
    public HierarchyRepository hierarchyRepository;
    public PasswordEncoder encoder;

    @Override
    public void run(String... args) throws Exception {
        if (roleRepository.findAll().size() == 0) {
            var roleU = new Role(null, ERole.ROLE_USER);
            var roleA = new Role(null, ERole.ROLE_ADMIN);
            var roleAff = new Role(null, ERole.ROLE_AFFILIATE);
            var roleV = new Role(null, ERole.ROLE_VENDOR);
            var roleGP = new Role(null, ERole.ROLE_GOLD_PARTNER);
            var roleDP = new Role(null, ERole.ROLE_DIAMOND_PARTNER);

            roleRepository.save(roleU);
            roleRepository.save(roleA);
            roleRepository.save(roleAff);
            roleRepository.save(roleV);
            roleRepository.save(roleGP);
            roleRepository.save(roleDP);
        }
        if (userRepository.findByRoleName(ERole.ROLE_ADMIN).isEmpty()) {
            var adminRole = roleRepository.findByName(ERole.ROLE_ADMIN);
            User admin = new User(null,
                    "steven@nefentus.com",
                    encoder.encode("8W%#P5oNkR+9XzJt&f$3"),
                    "Steven",
                    "Maindl",
                    "+4368110603393",
                    Set.of(adminRole.get()),
                    true,
                    "",
                    Timestamp.valueOf(LocalDateTime.now()),
                    Timestamp.valueOf(LocalDateTime.now()),
                    "1",
                    "http://localhost:80/images/test.png",
                    "steven@nefentus.com",
                    "Nefentus",
                    false,
                    "AAABSDACBASDDD",
                    Set.of(),
                    null,
                    null
            );

            User admin2 = new User(null,
                    "dev@nefentus.com",
                    encoder.encode("test123"),
                    "Dev",
                    "Nefentus",
                    "+4368110603393",
                    Set.of(adminRole.get()),
                    true,
                    "",
                    Timestamp.valueOf(LocalDateTime.now()),
                    Timestamp.valueOf(LocalDateTime.now()),
                    "2",
                    "http://localhost:80/images/test.png",
                    "dev@nefentus.com",
                    "nefentus",
                    false,
                    "ASDASDASDFFJIQWLEKDMDFA",
                    Set.of(),
                    null,
                    null
            );


            userRepository.save(admin);
            userRepository.save(admin2);
        }

        if (userRepository.findByRoleName(ERole.ROLE_DIAMOND_PARTNER).isEmpty()) {
            var diamondRole = roleRepository.findByName(ERole.ROLE_DIAMOND_PARTNER);
            User diamondUser = new User(null,
                    "diamond@example.com",
                    encoder.encode("password"),
                    "Diamond",
                    "User",
                    "+123456789",
                    Set.of(diamondRole.get()),
                    true,
                    "",
                    Timestamp.valueOf(LocalDateTime.now()),
                    Timestamp.valueOf(LocalDateTime.now()),
                    "8888",
                    "http://localhost:80/images/test.png",
                    "diamond@example.com",
                    "Nefentus",
                    false,
                    "AAABSDACBASDDD",
                    Set.of(), null, null
            );

            var goldRole = roleRepository.findByName(ERole.ROLE_GOLD_PARTNER);
            User goldUser = new User(null,
                    "gold@example.com",
                    encoder.encode("password"),
                    "Gold",
                    "User",
                    "+123456789",
                    Set.of(goldRole.get()),
                    true,
                    "",
                    Timestamp.valueOf(LocalDateTime.now()),
                    Timestamp.valueOf(LocalDateTime.now()),
                    "9999",
                    "http://localhost:80/images/test.png",
                    "gold@example.com",
                    "Nefentus",
                    false,
                    "AAABSDACBASDDD",
                    Set.of(), null, null
            );
            userRepository.save(diamondUser);
            userRepository.save(goldUser);

        }
        if (hierarchyRepository.count() == 0) {
            var user1 = userRepository.findUserByEmail("diamond@example.com").get();
            var user2 = userRepository.findUserByEmail("gold@example.com").get();
            Hierarchy hierarchy = new Hierarchy();
            hierarchy.setRelationshipType(RelationshipType.DIAMOND);
            hierarchy.setChild(user2);
            hierarchy.setParent(user1);
            hierarchy.setCommissionRate(1.25f);
            hierarchy.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
            hierarchyRepository.save(hierarchy);
        }


    }
}