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
import org.junit.jupiter.api.BeforeEach;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

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

    @BeforeEach
    void limparBanco() {
        userRepository.deleteAll();
    }

    private User criarUsuarioDeTeste(Role role) {
        User user = new User(
                "teste@leaveflow.com",
                passwordEncoder.encode("senha-correta"),
                role
        );

        return userRepository.save(user);
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
        User user = criarUsuarioDeTeste(Role.ADMIN);

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
        criarUsuarioDeTeste(Role.ADMIN);

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

    @Test
    void deveRetornar403QuandoFuncionarioTentarAprovarSolicitacao() throws Exception {
        User user = criarUsuarioDeTeste(Role.EMPLOYEE);
        String token = jwtService.gerarToken(user);

        mockMvc.perform(post("/leave-requests/1/approve")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void devePermitirAcessoDeSupervisorNaRotaDeAprovacao() throws Exception {
        User user = criarUsuarioDeTeste(Role.SUPERVISOR);
        String token = jwtService.gerarToken(user);

        mockMvc.perform(patch("/leave-requests/1/approve")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void devePermitirAcessoDeAdminNaRotaDeAprovacao() throws Exception {
        User user = criarUsuarioDeTeste(Role.ADMIN);
        String token = jwtService.gerarToken(user);

        mockMvc.perform(patch("/leave-requests/1/approve")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
