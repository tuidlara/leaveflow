package com.arthur.leaveflow.security;

import com.arthur.leaveflow.entity.User;
import com.arthur.leaveflow.enums.Role;
import com.arthur.leaveflow.repository.UserRepository;
import com.arthur.leaveflow.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.springframework.http.MediaType;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private void criarUsuarioDeTeste() {
        User user = new User(
                "teste@leaveflow.com",
                passwordEncoder.encode("senha-correta"),
                Role.ADMIN
        );

        userRepository.save(user);
    }

    @Test
    void deveRetornar401QuandoAcessarRotaProtegidaSemToken() throws Exception {
        mockMvc.perform(get("/employees"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRetornar401QuandoTokenForInvalido() throws Exception {
        mockMvc.perform(get("/employees")
                        .header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void devePermitirAcessoQuandoTokenForValido() throws Exception {
        User user = new User(
                "admin@leaveflow.com",
                "senha-criptografada",
                Role.ADMIN
        );

        String token = jwtService.gerarToken(user);

        mockMvc.perform(get("/employees")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void deveRetornar401QuandoSenhaEstiverIncorreta() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "admin@leaveflow.com",
                                    "senha": "senha-incorreta"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRetornarTokenQuandoCredenciaisForemValidas() throws Exception {
        criarUsuarioDeTeste();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "teste@leaveflow.com",
                                "senha": "senha-correta"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(result ->
                        org.junit.jupiter.api.Assertions.assertFalse(
                                result.getResponse().getContentAsString().isBlank()
                        ));
    }

    @Test
    void deveRetornar401QuandoUsuarioNaoExistir() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "inexistente@leaveflow.com",
                                "senha": "qualquer-senha"
                            }
                            """))
                .andExpect(status().isUnauthorized());
    }
}
