package com.arthur.leaveflow.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new TestController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void deveRetornar409QuandoOcorrerConflitoDeConcorrencia() throws Exception {
        mockMvc.perform(get("/teste-concorrencia"))
                .andExpect(status().isConflict())
                .andExpect(content().string(
                        "Esta solicitação foi alterada por outro usuário. Atualize os dados e tente novamente."
                ));
    }

    @RestController
    static class TestController {

        @GetMapping("/teste-concorrencia")
        void simularConflito() {
            throw new OptimisticLockingFailureException("Conflito simulado");
        }
    }
}