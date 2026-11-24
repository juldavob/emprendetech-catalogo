package com.emprendetech.catalogo.presentacion.seguridad;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SOLO DESARROLLO (perfil "dev"). El módulo Identidad (RF1) no forma parte del MVP del Catálogo,
 * así que se emiten tokens de demostración firmados con la misma clave que valida la API: se
 * imprimen al arrancar y los consume la interfaz web en GET /dev/tokens. En "prod" este bean
 * no existe. Los UUID coinciden con los propietarios de las tiendas semilla.
 */
@RestController
@Profile("dev")
public class TokensDesarrollo implements ApplicationRunner {

    public static final UUID EMPRENDEDOR_1 = UUID.fromString("11111111-1111-4111-8111-111111111111");
    public static final UUID EMPRENDEDOR_2 = UUID.fromString("22222222-2222-4222-8222-222222222222");
    public static final UUID EMPRENDEDOR_3 = UUID.fromString("44444444-4444-4444-8444-444444444444");
    public static final UUID COMPRADOR = UUID.fromString("33333333-3333-4333-8333-333333333333");
    public static final UUID ADMIN = UUID.fromString("99999999-9999-4999-8999-999999999999");

    private static final Logger LOG = LoggerFactory.getLogger(TokensDesarrollo.class);

    public record TokenDemo(String etiqueta, String rol, UUID usuarioId, String token) {
    }

    private final byte[] secreto;

    public TokensDesarrollo(@Value("${emprendetech.seguridad.jwt-secret}") String secreto) {
        this.secreto = secreto.getBytes(StandardCharsets.UTF_8);
    }

    @GetMapping("/dev/tokens")
    public List<TokenDemo> listar() throws JOSEException {
        return List.of(
                demo("Emprendedor 1 · Café de la Montaña", "EMPRENDEDOR", EMPRENDEDOR_1),
                demo("Emprendedor 2 · Manos Muiscas Artesanías", "EMPRENDEDOR", EMPRENDEDOR_2),
                demo("Emprendedor 3 · Huerta Urbana Suba", "EMPRENDEDOR", EMPRENDEDOR_3),
                demo("Comprador", "COMPRADOR", COMPRADOR),
                demo("Administrador", "ADMIN", ADMIN));
    }

    @Override
    public void run(ApplicationArguments args) throws JOSEException {
        StringBuilder sb = new StringBuilder("\n===== TOKENS DE DESARROLLO (válidos 12 h) =====\n");
        for (TokenDemo t : listar()) {
            sb.append(t.etiqueta()).append(": ").append(t.token()).append('\n');
        }
        sb.append("Interfaz web: http://localhost:8080/   |   Tokens por HTTP: GET /dev/tokens\n")
                .append("===============================================");
        LOG.info(sb.toString());
    }

    private TokenDemo demo(String etiqueta, String rol, UUID usuario) throws JOSEException {
        return new TokenDemo(etiqueta, rol, usuario, token(usuario, rol));
    }

    private String token(UUID usuario, String rol) throws JOSEException {
        Instant ahora = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(usuario.toString())
                .claim("rol", rol)
                .issueTime(Date.from(ahora))
                .expirationTime(Date.from(ahora.plus(Duration.ofHours(12))))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(secreto));
        return jwt.serialize();
    }
}
