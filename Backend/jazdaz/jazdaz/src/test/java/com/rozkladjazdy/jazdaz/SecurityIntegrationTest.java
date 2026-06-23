package com.rozkladjazdy.jazdaz;


import com.rozkladjazdy.jazdaz.database.entities.UserEntity;
import com.rozkladjazdy.jazdaz.database.repositories.UserRepository;
import com.rozkladjazdy.jazdaz.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private UserRepository userRepository;
//
//    @Autowired
//    private PasswordEncoder passwordEncoder;
//
//    @BeforeEach
//    void setUp() {
//        userRepository.deleteAll();
//
//        UserEntity user = new UserEntity();
//        user.setEmail("user@test.com");
//        user.setHashedPassword(passwordEncoder.encode("password123"));
//        user.setRole(UserRole.USER);
//
//        userRepository.save(user);
//    }
//
//    @Test
//    void shouldReturnUnauthorizedWhenUserIsNotLoggedIn() throws Exception {
//        mockMvc.perform(get("/"))
//                .andExpect(status().isUnauthorized());
//    }
//
//    @Test
//    void shouldLoginWithValidCredentials() throws Exception {
//        mockMvc.perform(get("/")
//                        .with(httpBasic("user@test.com", "password123")))
//                .andExpect(status().isOk())
//                .andExpect(content().string(equalTo("Rozklad jazdy?")));
//    }
//
//    @Test
//    void shouldRejectInvalidPassword() throws Exception {
//        mockMvc.perform(get("/")
//                        .with(httpBasic("user@test.com", "wrong-password")))
//                .andExpect(status().isUnauthorized());
//    }
//
//    @Test
//    void shouldRejectUnknownUser() throws Exception {
//        mockMvc.perform(get("/")
//                        .with(httpBasic("unknown@test.com", "password123")))
//                .andExpect(status().isUnauthorized());
//    }
}