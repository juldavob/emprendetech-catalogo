package com.emprendetech.catalogo.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emprendetech.catalogo.CatalogoFacade;
import com.emprendetech.catalogo.domain.excepcion.ConflictoException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Prueba de extremo a extremo contra PostgreSQL real (Testcontainers): Flyway + datos semilla +
 * seguridad por token + búsqueda de texto completo + fachada. Requiere Docker; sin Docker se omite.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class CatalogoApiIntegrationTest {

    private static final String SECRETO = "secreto-solo-para-pruebas-0123456789-abcdef";
    private static final UUID EMPRENDEDOR_1 = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID COMPRADOR = UUID.fromString("33333333-3333-4333-8333-333333333333");
    private static final UUID ADMIN = UUID.fromString("99999999-9999-4999-8999-999999999999");
    private static final String PRODUCTO_DE_TIENDA_2 = "b0000000-0000-4000-8000-000000000005";
    private static final String CAFE_HUILA = "b0000000-0000-4000-8000-000000000001";
    private static final String BASE = "/api/v1/catalogo";

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", POSTGRES::getUsername);
        registro.add("spring.datasource.password", POSTGRES::getPassword);
        registro.add("emprendetech.seguridad.jwt-secret", () -> SECRETO);
    }

    @Value("${local.server.port}")
    int puerto;

    @Autowired
    CatalogoFacade fachada;

    // ------------------------------------------------------------------ RF3

    @Test
    void busquedaPublicaPorTextoEncuentraProductosRelevantes() throws Exception {
        HttpResponse<String> r = llamar("GET", BASE + "/productos?q=miel", null, null);
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("Miel de abejas multifloral 500 g"));
        assertFalse(r.body().contains("Mochila wayuu"));
    }

    @Test
    void filtroPorLocalidadDevuelveSoloProductosDeEsaLocalidad() throws Exception {
        HttpResponse<String> r = llamar("GET", BASE + "/productos?localidad=LA_CANDELARIA", null, null);
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("Mochila wayuu tejida a mano"));
        assertFalse(r.body().contains("Miel de abejas multifloral"));
    }

    @Test
    void filtroPorRangoDePrecioYOrdenAscendente() throws Exception {
        HttpResponse<String> r = llamar("GET",
                BASE + "/productos?precioMin=9000&precioMax=15000&orden=PRECIO_ASC", null, null);
        assertEquals(200, r.statusCode());
        assertTrue(r.body().indexOf("Galletas de avena") < r.body().indexOf("Chocolate de mesa"),
                "la galleta (9.500) debe aparecer antes que el chocolate (15.000)");
        assertFalse(r.body().contains("Mochila wayuu"));
    }

    @Test
    void productosOcultosPorModeracionNoAparecenEnLaBusqueda() throws Exception {
        HttpResponse<String> r = llamar("GET", BASE + "/productos?q=perfume", null, null);
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("\"total\":0"));
    }

    // ------------------------------------------------------------------ RF2 + seguridad

    @Test
    void crearProductoSinTokenRetorna401() throws Exception {
        assertEquals(401, llamar("POST", BASE + "/productos", null, cuerpoProducto("Sin token")).statusCode());
    }

    @Test
    void compradorNoPuedeCrearProductos() throws Exception {
        String token = token(COMPRADOR, "COMPRADOR");
        assertEquals(403, llamar("POST", BASE + "/productos", token, cuerpoProducto("Comprador")).statusCode());
    }

    @Test
    void emprendedorCreaYLuegoConsultaSuProducto() throws Exception {
        String token = token(EMPRENDEDOR_1, "EMPRENDEDOR");
        HttpResponse<String> creado = llamar("POST", BASE + "/productos", token, cuerpoProducto("Panela orgánica"));
        assertEquals(201, creado.statusCode());

        String id = extraerId(creado.body());
        HttpResponse<String> consulta = llamar("GET", BASE + "/productos/" + id, null, null);
        assertEquals(200, consulta.statusCode());
        assertTrue(consulta.body().contains("Panela orgánica"));
    }

    @Test
    void datosInvalidosRetornan400() throws Exception {
        String token = token(EMPRENDEDOR_1, "EMPRENDEDOR");
        String invalido = "{\"nombre\":\"\",\"categoria\":\"OTROS\",\"precio\":-5,\"stock\":1}";
        assertEquals(400, llamar("POST", BASE + "/productos", token, invalido).statusCode());
    }

    @Test
    void emprendedorNoPuedeEditarProductosDeOtraTienda() throws Exception {
        String token = token(EMPRENDEDOR_1, "EMPRENDEDOR");
        HttpResponse<String> r = llamar("PUT", BASE + "/productos/" + PRODUCTO_DE_TIENDA_2, token,
                cuerpoProducto("Intento de edición ajena"));
        assertEquals(403, r.statusCode());
    }

    @Test
    void miTiendaDevuelveLaTiendaDelEmprendedorAutenticado() throws Exception {
        HttpResponse<String> r = llamar("GET", BASE + "/tiendas/mia", token(EMPRENDEDOR_1, "EMPRENDEDOR"), null);
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("Café de la Montaña"));
        assertEquals(403, llamar("GET", BASE + "/tiendas/mia", token(COMPRADOR, "COMPRADOR"), null).statusCode());
        assertEquals(401, llamar("GET", BASE + "/tiendas/mia", null, null).statusCode());
    }

    // ------------------------------------------------------------------ moderación

    @Test
    void adminOcultaUnProductoYDejaDeSerVisibleParaElPublico() throws Exception {
        String emprendedor = token(EMPRENDEDOR_1, "EMPRENDEDOR");
        String id = extraerId(llamar("POST", BASE + "/productos", emprendedor, cuerpoProducto("Producto a moderar")).body());

        assertEquals(403, llamar("PATCH", BASE + "/productos/" + id + "/moderacion", emprendedor,
                "{\"accion\":\"OCULTAR\",\"motivo\":\"x\"}").statusCode(), "solo ADMIN puede moderar");

        HttpResponse<String> r = llamar("PATCH", BASE + "/productos/" + id + "/moderacion", token(ADMIN, "ADMIN"),
                "{\"accion\":\"OCULTAR\",\"motivo\":\"Contenido inapropiado\"}");
        assertEquals(200, r.statusCode());
        assertEquals(404, llamar("GET", BASE + "/productos/" + id, null, null).statusCode());
    }

    // ------------------------------------------------------------------ fachada (contrato con Pedidos)

    @Test
    void laFachadaReservaStockDeFormaAtomicaYRechazaLaSobreventa() {
        UUID id = UUID.fromString(CAFE_HUILA);
        int antes = fachada.consultarProducto(id).orElseThrow().stock();

        fachada.reservarStock(id, 2);
        assertEquals(antes - 2, fachada.consultarProducto(id).orElseThrow().stock());

        assertThrows(ConflictoException.class, () -> fachada.reservarStock(id, 1_000_000));
        assertEquals(antes - 2, fachada.consultarProducto(id).orElseThrow().stock());

        fachada.liberarStock(id, 2);
        assertEquals(antes, fachada.consultarProducto(id).orElseThrow().stock());
    }

    // ------------------------------------------------------------------ utilidades

    private HttpResponse<String> llamar(String metodo, String ruta, String token, String cuerpo) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
                .header("Content-Type", "application/json")
                .method(metodo, cuerpo == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(cuerpo, StandardCharsets.UTF_8));
        if (token != null) {
            b.header("Authorization", "Bearer " + token);
        }
        return HttpClient.newHttpClient().send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static String cuerpoProducto(String nombre) {
        return "{\"nombre\":\"" + nombre + "\",\"descripcion\":\"Descripción de prueba\","
                + "\"categoria\":\"OTROS\",\"precio\":12345.50,\"stock\":7}";
    }

    private static String extraerId(String json) {
        Matcher m = Pattern.compile("\"id\":\"([0-9a-f\\-]{36})\"").matcher(json);
        assertTrue(m.find(), "la respuesta debe traer un id: " + json);
        return m.group(1);
    }

    private static String token(UUID usuario, String rol) throws Exception {
        Instant ahora = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(usuario.toString()).claim("rol", rol)
                .issueTime(Date.from(ahora)).expirationTime(Date.from(ahora.plus(Duration.ofHours(1))))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(SECRETO.getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
    }
}
