# EmprendeTech – MVP del módulo Catálogo

## 1. Propósito
Producto Mínimo Viable del componente **Catálogo** de EmprendeTech (Reto 4). Evidencia el estilo
arquitectónico del SAD: **monolito modular + Clean Architecture**. Cubre **RF2** (CRUD de productos por
emprendedor) y **RF3** (búsqueda y filtro por texto, localidad, categoría y precio), más tienda virtual,
inventario y moderación, que el SAD §2.2 asigna a este componente.

## 2. Cómo ejecutar
Requisitos: JDK 21+ (probado en diseño para 21 y 25), Maven 3.9+, Docker.

```bash
docker compose up -d                 # PostgreSQL 17
mvn spring-boot:run                  # perfil "dev" por defecto: Flyway + datos semilla
```
Abra **http://localhost:8080/** para usar la interfaz web (búsqueda con filtros, panel del emprendedor y
moderación). Al arrancar también se imprimen **tokens JWT de demostración**; la interfaz los toma de
`GET /dev/tokens` (solo perfil `dev`) y los usa en el selector «Sesión de demostración».
Pruebas: `mvn test` (las de integración usan Testcontainers y se omiten si no hay Docker).

```bash
# Búsqueda pública (texto, localidad, categoría, precio, orden, paginación)
curl "http://localhost:8080/api/v1/catalogo/productos?q=miel"
curl "http://localhost:8080/api/v1/catalogo/productos?localidad=LA_CANDELARIA&orden=PRECIO_DESC"
curl "http://localhost:8080/api/v1/catalogo/productos?categoria=ALIMENTOS&precioMin=10000&precioMax=30000"

# Crear producto (token EMPRENDEDOR)
curl -X POST http://localhost:8080/api/v1/catalogo/productos \
  -H "Authorization: Bearer $TOKEN_EMPRENDEDOR_1" -H "Content-Type: application/json" \
  -d '{"nombre":"Panela orgánica","descripcion":"Bloque 500 g","categoria":"ALIMENTOS","precio":7500,"stock":40}'

# Moderar (token ADMIN)
curl -X PATCH http://localhost:8080/api/v1/catalogo/productos/<id>/moderacion \
  -H "Authorization: Bearer $TOKEN_ADMIN" -H "Content-Type: application/json" \
  -d '{"accion":"OCULTAR","motivo":"Contenido inapropiado"}'
```

| Método y ruta | Rol | RF |
|---|---|---|
| `POST /api/v1/catalogo/tiendas` · `GET .../tiendas/mia` · `GET .../tiendas/{id}` | EMPRENDEDOR · EMPRENDEDOR · público | tienda |
| `GET .../productos` (filtros) · `GET .../productos/{id}` | público | RF3 |
| `POST/PUT/DELETE .../productos[/{id}]` | EMPRENDEDOR (dueño) | RF2 |
| `PATCH .../productos/{id}/moderacion` | ADMIN | moderación |

Fachada para otros módulos: `CatalogoFacade` (`consultarProducto`, `reservarStock`, `liberarStock`).

## 3. Estructura (SAD §1.1 y §2)
```
com.emprendetech
├── shared/                 kernel compartido (Dinero, Localidad)
├── api/                    capa REST transversal: JWT stateless, roles, tokens de desarrollo
resources/static/index.html  interfaz web básica (HTML + JS, sin build) que consume la API
└── catalogo/
    ├── CatalogoFacade, ProductoVista   ← única API pública del módulo
    ├── domain/             entidades y reglas, sin frameworks
    ├── application/        casos de uso + puertos (interfaces)
    ├── infrastructure/     adaptadores: JPA/PostgreSQL, caché Caffeine, estrategias de orden
    └── presentation/       controlador REST, DTOs, manejo de errores
```
La regla de dependencia y las fronteras entre módulos se verifican en `ArquitecturaTest`
(ArchUnit + Spring Modulith).

## 4. Patrones de diseño y relación con el SAD
| Patrón | Dónde | Relación con el SAD |
|---|---|---|
| **Adapter** (estructural) | `infrastructure/`: `*RepositorioAdapter`, `BusquedaProductosAdapter`, `CaffeineBusquedaCacheAdapter` implementan los puertos de `application/puerto` | SAD §1.1 (infraestructura = adaptadores que implementan puertos) y §3.2 (Adapter). Cambiar JPA o Caffeine por Redis no toca dominio ni casos de uso. |
| **Strategy** (comportamiento) | `EstrategiaOrdenamiento` con `OrdenRelevancia`, `OrdenRecientes`, `OrdenPrecioAscendente`, `OrdenPrecioDescendente` | SAD §3.2 (Strategy: Abierto/Cerrado, sin condicionales, RNF5). Un nuevo orden = una clase nueva. |

> **Supuesto técnico (a resolver en el SAD):** el SAD §3.3 indica que Catálogo *no* usa patrones GoF y
> ubica Strategy/Adapter/State en Pagos y Pedidos, pero el Reto §7 exige ≥ 2 patrones del SAD en el PMV y
> esos módulos están fuera del alcance. Se aplican Adapter (respaldado por §1.1) y Strategy (ordenamiento).
> Antes de entregar, añada a §3.2 una fila «Catálogo: Adapter + Strategy» (y quite la fila de §3.3 o
> ajuste su texto) para que SAD y código coincidan.

## 5. Trazabilidad con RNF
RNF3: índice GIN + `tsvector` en PostgreSQL y caché de consultas · RNF2: aplicación sin estado, caché,
reserva de stock atómica (sin sobreventa) · RNF5: puertos/adaptadores + Strategy · RNF6: JWT firmado, roles,
parámetros SQL enlazados · RNF4: un solo artefacto + PostgreSQL · RNF1/RNF7: sin estado, escalable horizontalmente.

## 6. Supuestos y límites del MVP
- La interfaz es deliberadamente simple (un solo `index.html`, sin framework): su función es demostrar el flujo
  completo del Catálogo sobre la API; no es parte del SAD y puede reemplazarse sin tocar el backend.
- Identidad (RF1) no está implementada: el token trae `sub` (UUID) y `rol`; en `dev` se generan tokens de demostración.
  En producción defina `SPRING_PROFILES_ACTIVE=prod` y `JWT_SECRET` (≥ 32 caracteres); sin semilla ni tokens.
- Un emprendedor tiene una sola tienda; la localidad de un producto es la de su tienda.
- Lectura del catálogo pública; un producto oculto responde 404 al público.
- Caché en memoria por instancia (TTL 60 s, se vacía en cada escritura); en producción, adaptador Redis.
- `PUT` fija el stock de forma absoluta (último en escribir gana); reservar/liberar sí son atómicos.
- Eventos de dominio (`Reseñas → Catálogo`, promedio de calificaciones) y Outbox quedan fuera del alcance.
- Versiones: Spring Boot 4.1.2, Spring Modulith 2.1.0, Java 21 (verificadas en noviembre de 2026; revise parches).
