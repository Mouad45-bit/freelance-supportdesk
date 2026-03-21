package com.example.supportdesk.config;

import com.example.supportdesk.common.enums.UserRole;
import com.example.supportdesk.user.entity.AppUser;
import com.example.supportdesk.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Profile("dev")
public class DevDataInitializer {
    @Bean
    CommandLineRunner initDevUsers(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            if (!appUserRepository.existsByUsername("admin1")) {
                appUserRepository.save(new AppUser(
                        "System admin",
                        "admin1",
                        passwordEncoder.encode("admin123456"),
                        UserRole.ADMIN
                ));
            }
            //
            if (!appUserRepository.existsByUsername("user1")) {
                appUserRepository.save(new AppUser(
                        "Normal user",
                        "user1",
                        passwordEncoder.encode("user123456"),
                        UserRole.USER
                ));
            }
        };
    }
}
