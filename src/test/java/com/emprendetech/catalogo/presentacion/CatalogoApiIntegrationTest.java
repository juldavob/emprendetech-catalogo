package com.emprendetech.catalogo.presentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emprendetech.catalogo.aplicacion.fachada.CatalogoFacade;
import com.emprendetech.catalogo.aplicacion.fachada.ItemReserva;
import com.emprendetech.shared_kernel.excepcion.ConflictoException;
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
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Prueba de extremo a extremo contra PostgreSQL y Redis reales (Testcontainers): Flyway + datos semilla +
 * seguridad por token + búsqueda de texto completo + caché Redis + fachada. Requiere Docker; sin Docker se omite.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@SuppressWarnings({"rawtypes", "unchecked"})
class CatalogoApiIntegrationTest {

    private static final String SECRETO = "secreto-solo-para-pruebas-0123456789-abcdef";
    private static final UUID EMPRENDEDOR_1 = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID COMPRADOR = UUID.fromString("33333333-3333-4333-8333-333333333333");
    private static final UUID ADMIN = UUID.fromString("99999999-9999-4999-8999-999999999999");
    private static final String PRODUCTO_DE_TIENDA_2 = "b0000000-0000-4000-8000-000000000005";
    private static final UUID CAFE_HUILA = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final UUID MIEL = UUID.fromString("b0000000-0000-4000-8000-000000000009");
    private static final UUID CAMA_SIN_STOCK = UUID.fromString("b0000000-0000-4000-8000-000000000013");
    private static final String BASE = "/api/v1/catalogo";

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Container
    static final GenericContainer REDIS = new GenericContainer(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", POSTGRES::getUsername);
        registro.add("spring.datasource.password", POSTGRES::getPassword);
        registro.add("spring.data.redis.host", () -> REDIS.getHost());
        registro.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
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
    void filtroPorCategoriaYRangoDePrecio() throws Exception {
        HttpResponse<String> r = llamar("GET",
                BASE + "/productos?categoria=alimentos&precioMin=9000&precioMax=16000", null, null);
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("Galletas de avena"));
        assertTrue(r.body().contains("Chocolate de mesa"));
        assertFalse(r.body().contains("Miel de abejas"), "28.000 está fuera del rango");
        assertFalse(r.body().contains("Mochila wayuu"), "es otra categoría");
    }

    @Test
    void categoriaInvalidaRetorna422() throws Exception {
        assertEquals(422, llamar("GET", BASE + "/productos?categoria=COHETES", null, null).statusCode());
    }

    @Test
    void productosOcultosPorModeracionNoAparecenEnLaBusqueda() throws Exception {
        HttpResponse<String> r = llamar("GET", BASE + "/productos?q=perfume", null, null);
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("\"total\":0"));
    }

    @Test
    void laCacheRedisSeInvalidaAlPublicarUnProducto() throws Exception {
        String consulta = BASE + "/productos?q=zanahoria";
        assertTrue(llamar("GET", consulta, null, null).body().contains("\"total\":0"));
        assertTrue(llamar("GET", consulta, null, null).body().contains("\"total\":0"), "segunda lectura, desde caché");

        String token = token(EMPRENDEDOR_1, "EMPRENDEDOR");
        assertEquals(201, llamar("POST", BASE + "/productos", token, cuerpoProducto("Zanahoria orgánica")).statusCode());

        HttpResponse<String> despues = llamar("GET", consulta, null, null);
        assertTrue(despues.body().contains("\"total\":1"), "la caché debió invalidarse: " + despues.body());
        assertTrue(despues.body().contains("Zanahoria orgánica"));
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
    void reservaDeStockEnDosFasesConfirmarYLiberar() {
        int antes = stockDe(CAFE_HUILA);

        UUID pedidoAprobado = UUID.randomUUID();
        fachada.reservarStock(pedidoAprobado, List.of(new ItemReserva(CAFE_HUILA, 2)));
        assertEquals(antes - 2, stockDe(CAFE_HUILA));
        fachada.confirmarStock(pedidoAprobado);                 // PagoAprobado
        fachada.confirmarStock(pedidoAprobado);                 // reintento del webhook: idempotente
        assertEquals(antes - 2, stockDe(CAFE_HUILA));
        assertThrows(ConflictoException.class, () -> fachada.liberarStock(pedidoAprobado));

        UUID pedidoRechazado = UUID.randomUUID();
        fachada.reservarStock(pedidoRechazado, List.of(new ItemReserva(CAFE_HUILA, 3)));
        assertEquals(antes - 5, stockDe(CAFE_HUILA));
        fachada.liberarStock(pedidoRechazado);                  // pago rechazado
        fachada.liberarStock(pedidoRechazado);                  // idempotente: no devuelve dos veces
        assertEquals(antes - 2, stockDe(CAFE_HUILA));
    }

    @Test
    void laReservaMultiProductoEsAtomicaSiUnProductoNoTieneStock() {
        int cafeAntes = stockDe(CAFE_HUILA);
        int mielAntes = stockDe(MIEL);

        assertThrows(ConflictoException.class, () -> fachada.reservarStock(UUID.randomUUID(),
                List.of(new ItemReserva(CAFE_HUILA, 1), new ItemReserva(MIEL, 1),
                        new ItemReserva(CAMA_SIN_STOCK, 1))));

        assertEquals(cafeAntes, stockDe(CAFE_HUILA), "el rollback debe devolver el stock del café");
        assertEquals(mielAntes, stockDe(MIEL), "el rollback debe devolver el stock de la miel");
    }

    // ------------------------------------------------------------------ utilidades

    private int stockDe(UUID productoId) {
        return fachada.consultarProducto(productoId).orElseThrow().stock();
    }

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
