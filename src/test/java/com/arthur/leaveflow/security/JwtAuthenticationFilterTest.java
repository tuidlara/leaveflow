package com.arthur.leaveflow.security;

import com.arthur.leaveflow.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void configurar() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
    }

    @Test
    void deveContinuarFiltroQuandoNaoExistirToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    void deveContinuarFiltroQuandoTokenNaoPossuirPrefixoBearer() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("token-invalido");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    void deveRetornar401QuandoTokenForInvalido() throws Exception {
        when(request.getHeader("Authorization"))
                .thenReturn("Bearer token-invalido");

        when(jwtService.validarToken("token-invalido"))
                .thenThrow(new io.jsonwebtoken.JwtException("Token inválido"));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verifyNoInteractions(userDetailsService);
        verifyNoInteractions(filterChain);
    }

    @Test
    void deveAutenticarUsuarioQuandoTokenForValido() throws Exception {
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("admin@leaveflow.com")
                .password("senha")
                .authorities("ROLE_ADMIN")
                .build();

        when(request.getHeader("Authorization"))
                .thenReturn("Bearer token-valido");

        when(jwtService.validarToken("token-valido"))
                .thenReturn("admin@leaveflow.com");

        when(userDetailsService.loadUserByUsername("admin@leaveflow.com"))
                .thenReturn(userDetails);

        filter.doFilterInternal(request, response, filterChain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();

        org.junit.jupiter.api.Assertions.assertNotNull(authentication);
        org.junit.jupiter.api.Assertions.assertEquals(
                "admin@leaveflow.com",
                authentication.getName()
        );

        verify(userDetailsService).loadUserByUsername("admin@leaveflow.com");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void deveRetornar401QuandoUsuarioNaoForEncontrado() throws Exception {
        when(request.getHeader("Authorization"))
                .thenReturn("Bearer token-valido");

        when(jwtService.validarToken("token-valido"))
                .thenReturn("inexistente@leaveflow.com");

        when(userDetailsService.loadUserByUsername("inexistente@leaveflow.com"))
                .thenThrow(new UsernameNotFoundException("Usuário não encontrado."));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verifyNoInteractions(filterChain);
    }
}
