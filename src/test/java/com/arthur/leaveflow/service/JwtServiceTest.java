package com.arthur.leaveflow.service;

import com.arthur.leaveflow.entity.User;
import com.arthur.leaveflow.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        ReflectionTestUtils.setField(
                jwtService,
                "secret",
                "bXktdGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdGVzdHMtMzI="
        );
    }

    @Test
    void deveGerarTokenComEmailERoleDoUsuario() {
        User user = new User(
                "admin@leaveflow.com",
                "senha-criptografada",
                Role.ADMIN
        );

        String token = jwtService.gerarToken(user);

        assertNotNull(token);
        assertFalse(token.isBlank());

        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(
                        Decoders.BASE64.decode(
                                "bXktdGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdGVzdHMtMzI="
                        )
                ))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals("admin@leaveflow.com", claims.getSubject());
        assertEquals("ADMIN", claims.get("role", String.class));
    }

    @Test
    void deveRejeitarTokenInvalido() {
        String tokenInvalido = "token-invalido";

        assertThrows(
                JwtException.class,
                () -> jwtService.validarToken(tokenInvalido)
        );
    }

    @Test
    void deveRejeitarTokenExpirado() {
        String tokenExpirado = Jwts.builder()
                .subject("admin@leaveflow.com")
                .issuedAt(new Date(System.currentTimeMillis() - 120_000))
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(Keys.hmacShaKeyFor(
                        Decoders.BASE64.decode(
                                "bXktdGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdGVzdHMtMzI="
                        )
                ))
                .compact();

        assertThrows(
                JwtException.class,
                () -> jwtService.validarToken(tokenExpirado)
        );
    }
}