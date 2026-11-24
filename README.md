# EmprendeTech – MVP del módulo Catálogo

## 1. Propósito
Producto Mínimo Viable del componente **Catálogo** de EmprendeTech (Reto 4), alineado con el documento
**SAD-EMPTECH-001 v1.0**. Evidencia el estilo del SAD (§3.3): **monolito modular + Clean Architecture**, con las
cuatro capas de la Tabla 4 y la fachada pública `CatalogoFacade`. Cubre **RF2** (CRUD de productos por emprendedor),
**RF3** (búsqueda y filtro por texto, localidad, categoría y precio), la tienda virtual (CU-02), la moderación
(CU-10) y el contrato de inventario que consume Pedidos (reserva, confirmación y liberación de stock).

## 2. Cómo ejecutar
Requisitos: JDK 21+, Maven 3.9+ y Docker.

```bash
docker compose up -d        # PostgreSQL 17 y Redis 7
mvn spring-boot:run         # perfil "dev" por defecto: Flyway + datos semilla + tokens de demostración
```
Abra **http://localhost:8080/** (interfaz web básica). Los tokens de demostración se imprimen al arrancar y la
interfaz los toma de `GET /dev/tokens` (solo perfil `dev`).
Pruebas: `mvn test` (las de integración usan Testcontainers y se omiten si no hay Docker).

```bash
curl "http://localhost:8080/api/v1/catalogo/productos?q=miel"
curl "http://localhost:8080/api/v1/catalogo/productos?localidad=LA_CANDELARIA&categoria=hogar"
curl "http://localhost:8080/api/v1/catalogo/productos?categoria=alimentos&precioMin=10000&precioMax=30000"
```

| Método y ruta | Rol | Caso de uso del SAD |
|---|---|---|
| `GET /api/v1/catalogo/productos` (q, localidad, categoria, precioMin, precioMax, pagina, tamano) · `GET .../productos/{id}` | público | CU-03 (RF3) |
| `POST/PUT/DELETE .../productos[/{id}]` | EMPRENDEDOR (dueño) | CU-02 (RF2) |
| `POST .../tiendas` · `GET .../tiendas/mia` · `GET .../tiendas/{id}` | EMPRENDEDOR · EMPRENDEDOR · público | CU-02 |
| `PATCH .../productos/{id}/moderacion` | ADMIN | CU-10 |

**Fachada** (`CatalogoFacade`, API para Pedidos): `consultarProducto(id)`, `reservarStock(pedidoId, items)`,
`confirmarStock(pedidoId)` y `liberarStock(pedidoId)`.

## 3. Correspondencia con el SAD
**Capas (Tabla 4 y §8)** – paquetes en `com.emprendetech.catalogo`:

| Capa | Regla del SAD | Contenido en el MVP |
|---|---|---|
| `presentacion` | Depende solo de Aplicación y DTOs | `CatalogoController`, DTOs de entrada, `ManejadorErrores`, filtros de seguridad (`seguridad/`) |
| `aplicacion` | Orquesta transacciones y casos de uso; depende solo de Dominio; expone puertos y fachadas | `servicio/` (casos de uso, Java puro), `puerto/` (`*Repositorio`, `Transaccion`, caché y búsqueda), `dto/`, `fachada/CatalogoFacade` |
| `dominio` | Java puro, sin frameworks ni otras capas | `Producto`, `Tienda`, `ReservaStock` y reglas |
| `infraestructura` | Implementa los puertos por inversión de dependencias | JPA/PostgreSQL + Flyway, cliente Redis, `TransaccionSpring`, `CatalogoConfig` |
| `shared_kernel` | Transversal | `Dinero`, `Localidad` y excepciones de negocio |

Las reglas se verifican en `ArquitecturaTest` (ArchUnit para las capas; Spring Modulith para las fronteras entre
módulos). La fachada es la única interfaz expuesta (`@NamedInterface` en `aplicacion/fachada`).

**Realizaciones de casos de uso (Tabla 3 y §6)**
- *Búsqueda filtrada*: consulta primero en **Redis**; ante *cache miss* ejecuta la **búsqueda de texto completo
  indexada** en el esquema `catalogo` (GIN sobre `tsvector`). Las escrituras invalidan la caché tras confirmar la
  transacción; si Redis falla, la búsqueda sigue contra PostgreSQL.
- *Checkout / pago*: Pedidos llama a la fachada para **reservar** el stock (todo o nada, con `UPDATE` atómico, sin
  sobreventa); al `PagoAprobado` se **confirma** y si el pago se rechaza se **libera**. Ambas operaciones son idempotentes
  (la pasarela puede reintentar el webhook).

**Trazabilidad con RNF (Tabla 1)**: RNF1/RNF2/RNF7 sin estado, caché distribuida y reserva atómica · RNF3 índices +
texto completo + Redis · RNF4 un solo artefacto, sin motores externos · RNF5 puertos/adaptadores, capas verificadas
automáticamente y complejidad baja · RNF6 JWT firmado, RBAC y consultas parametrizadas.

## 4. Patrones de diseño (importante)
El SAD (§3.2, §5.2 y Tabla 6) establece que **Catálogo no incorpora patrones GoF** y que Strategy, Adapter y State se
aplican solo en Pagos y Pedidos. Este MVP respeta esa decisión: los "adaptadores" de `infraestructura` son la
inversión de dependencias de Clean Architecture descrita en la Tabla 4, no un patrón GoF añadido.
> **Atención:** el Reto (§7) pide que el PMV implemente al menos 2 patrones justificados en el SAD. Con el alcance
> limitado al Catálogo, este requisito queda sin evidenciar. Conviene acordar con el docente cómo se acredita
> (p. ej., con el módulo Pagos de otro integrante del equipo).

## 5. Supuestos técnicos
- `reservarStock` recibe el `pedidoId` además de los ítems: el SAD usa `confirmarStock(pedidoId)`, así que la reserva
  debe quedar identificada por pedido.
- Las excepciones de negocio están en `shared_kernel` para que la presentación las traduzca a HTTP sin conocer el dominio.
- Los filtros de seguridad (Spring Security) viven en `presentacion/seguridad`, según la Tabla 4. Identidad (RF1) no está
  en el alcance: el token trae `sub` (UUID) y `rol`. En `dev` se emiten tokens de demostración; en producción defina
  `SPRING_PROFILES_ACTIVE=prod` y `JWT_SECRET` (≥ 32 caracteres).
- Un emprendedor tiene una sola tienda; la localidad de un producto es la de su tienda. La lectura del catálogo es pública
  y un producto oculto por moderación responde 404.
- La capa de Aplicación no usa anotaciones de Spring; la transacción se orquesta mediante el puerto `Transaccion`.
- Fuera del alcance de este MVP: imágenes en CDN, eventos entre módulos (`ReseñaCreada`, promedio de calificaciones),
  y los demás módulos (Identidad, Pedidos, Pagos, Reseñas, Reportes).
- `PUT` fija el stock de forma absoluta (gana la última escritura); reservar y liberar sí son atómicos.
- Versiones verificadas en noviembre de 2026: Spring Boot 4.1.2, Spring Modulith 2.1.0, Java 21 (compatible con 25).
