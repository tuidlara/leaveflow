package com.arthur.leaveflow.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.Mockito.*;

class UserRateLimitFilterTest {

    private UserRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new UserRateLimitFilter();
        SecurityContextHolder.clearContext();
    }

    @Test
    void devePermitirRequisicaoDeUsuarioDentroDoLimite() throws Exception {

        var userDetails = new CustomUserDetails(
                1L,
                "admin@leaveflow.com",
                "senha",
                "ADMIN"
        );

        var authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void deveBloquearUsuarioAoExcederLimiteDeRequisicoes() throws Exception {

        var userDetails = new CustomUserDetails(
                1L,
                "admin@leaveflow.com",
                "senha",
                "ADMIN"
        );

        var authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        StringWriter responseBody = new StringWriter();

        when(response.getWriter()).thenReturn(
                new PrintWriter(responseBody)
        );

        for (int i = 0; i < 101; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }

        verify(filterChain, times(100)).doFilter(request, response);
        verify(response).setStatus(429);
    }

    @Test
    void devePermitirRequisicaoSemAutenticacaoSemConsumirTokens() throws Exception {

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }
}