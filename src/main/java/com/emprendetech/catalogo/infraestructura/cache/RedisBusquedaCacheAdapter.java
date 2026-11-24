package com.emprendetech.catalogo.infraestructura.cache;

import com.emprendetech.catalogo.aplicacion.dto.CriteriosBusqueda;
import com.emprendetech.catalogo.aplicacion.dto.PaginaDto;
import com.emprendetech.catalogo.aplicacion.dto.ProductoDto;
import com.emprendetech.catalogo.aplicacion.puerto.CacheBusquedaPort;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Cliente de caché Redis (SAD Tabla 4, RNF2/RNF3): guarda páginas de resultados con TTL.
 * La invalidación es O(1): un contador de versión forma parte de la llave, así que al incrementarlo
 * las entradas anteriores quedan huérfanas y expiran solas. Si Redis falla, la búsqueda sigue
 * funcionando contra PostgreSQL (la caché es una optimización, no un punto único de falla, RNF1).
 */
@Component
public class RedisBusquedaCacheAdapter implements CacheBusquedaPort {

    private static final Logger LOG = LoggerFactory.getLogger(RedisBusquedaCacheAdapter.class);
    private static final String CLAVE_VERSION = "catalogo:busqueda:version";
    /** Solo se deserializan los tipos esperados (defensa ante contenido manipulado en Redis). */
    private static final ObjectInputFilter FILTRO = ObjectInputFilter.Config.createFilter(
            "java.lang.**;java.math.**;java.util.**;com.emprendetech.catalogo.aplicacion.dto.*;!*");

    private final StringRedisTemplate redis;
    private final Duration ttl;

    public RedisBusquedaCacheAdapter(StringRedisTemplate redis,
            @Value("${emprendetech.catalogo.cache.ttl-segundos:60}") long ttlSegundos) {
        this.redis = redis;
        this.ttl = Duration.ofSeconds(ttlSegundos);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<PaginaDto<ProductoDto>> obtener(CriteriosBusqueda criterios) {
        try {
            String guardado = redis.opsForValue().get(clave(criterios));
            if (guardado == null) {
                return Optional.empty();
            }
            Object objeto = deserializar(guardado);
            return objeto instanceof PaginaDto<?> pagina
                    ? Optional.of((PaginaDto<ProductoDto>) pagina)
                    : Optional.empty();
        } catch (RuntimeException | IOException | ClassNotFoundException e) {
            LOG.warn("Caché Redis no disponible al leer; se consulta PostgreSQL: {}", e.toString());
            return Optional.empty();
        }
    }

    @Override
    public void guardar(CriteriosBusqueda criterios, PaginaDto<ProductoDto> resultado) {
        try {
            redis.opsForValue().set(clave(criterios), serializar(resultado), ttl);
        } catch (RuntimeException | IOException e) {
            LOG.warn("Caché Redis no disponible al escribir: {}", e.toString());
        }
    }

    @Override
    public void invalidarTodo() {
        try {
            redis.opsForValue().increment(CLAVE_VERSION);
        } catch (RuntimeException e) {
            LOG.warn("No se pudo invalidar la caché Redis (las entradas expiran por TTL): {}", e.toString());
        }
    }

    private String clave(CriteriosBusqueda criterios) {
        String version = redis.opsForValue().get(CLAVE_VERSION);
        return "catalogo:busqueda:" + (version == null ? "0" : version) + ":" + huella(criterios);
    }

    /** SHA-256 del texto canónico de los criterios (el record normalizado tiene un toString estable). */
    private static String huella(CriteriosBusqueda criterios) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(criterios.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String serializar(PaginaDto<ProductoDto> pagina) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream salida = new ObjectOutputStream(bytes)) {
            salida.writeObject(pagina);
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray());
    }

    private static Object deserializar(String texto) throws IOException, ClassNotFoundException {
        try (ObjectInputStream entrada = new ObjectInputStream(
                new ByteArrayInputStream(Base64.getDecoder().decode(texto)))) {
            entrada.setObjectInputFilter(FILTRO);
            return entrada.readObject();
        }
    }
}
