package com.emprendetech.catalogo.presentacion.seguridad;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autenticación stateless (RNF6, SAD §2.3 regla 5): el usuario y su rol salen del token firmado
 * (HS256); ningún módulo consulta a Identidad en cada petición. Un token ausente, inválido o
 * vencido deja la petición sin autenticar (401 en rutas protegidas).
 */
public class JwtFiltro extends OncePerRequestFilter {

    static final Set<String> ROLES = Set.of("EMPRENDEDOR", "COMPRADOR", "ADMIN");

    private final byte[] secreto;

    public JwtFiltro(byte[] secreto) {
        this.secreto = secreto;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecera != null && cabecera.startsWith("Bearer ")) {
            autenticar(cabecera.substring(7).trim())
                    .ifPresent(a -> SecurityContextHolder.getContext().setAuthentication(a));
        }
        chain.doFilter(request, response);
    }

    private Optional<Authentication> autenticar(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm())
                    || !jwt.verify(new MACVerifier(secreto))) {
                return Optional.empty();
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Date expira = claims.getExpirationTime();
            String sujeto = claims.getSubject();
            String rol = claims.getStringClaim("rol");
            if (expira == null || expira.before(new Date()) || sujeto == null
                    || rol == null || !ROLES.contains(rol)) {
                return Optional.empty();
            }
            UUID.fromString(sujeto); // el sujeto debe ser el UUID del usuario
            return Optional.of(UsernamePasswordAuthenticationToken.authenticated(
                    sujeto, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol))));
        } catch (ParseException | JOSEException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
