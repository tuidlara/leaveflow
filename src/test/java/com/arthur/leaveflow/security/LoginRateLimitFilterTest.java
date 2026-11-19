package com.arthur.leaveflow.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class LoginRateLimitFilterTest {

    private LoginRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new LoginRateLimitFilter();
    }

    @Test
    void devePermitirLoginDentroDoLimite() throws Exception {

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void deveBloquearLoginAoExcederLimiteDeTentativas() throws Exception {

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        StringWriter responseBody = new StringWriter();

        when(response.getWriter()).thenReturn(
                new PrintWriter(responseBody)
        );

        for (int i = 0; i < 6; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }

        verify(filterChain, times(5)).doFilter(request, response);
        verify(response).setStatus(429);
    }

    @Test
    void naoDeveAplicarLimiteEmOutrasRotas() throws Exception {

        HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getMethod()).thenReturn("GET");
        when(request.getServletPath()).thenReturn("/employees");

        boolean resultado = filter.shouldNotFilter(request);

        assertTrue(resultado);
    }
}