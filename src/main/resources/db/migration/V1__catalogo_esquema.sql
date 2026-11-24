-- Módulo Catálogo: esquema propio (SAD §2.1: "cada módulo es dueño de su esquema").
CREATE SCHEMA IF NOT EXISTS catalogo;

CREATE TABLE catalogo.tienda (
    id             uuid         PRIMARY KEY,
    propietario_id uuid         NOT NULL,            -- id del emprendedor (viene del token, sin FK a Identidad)
    nombre         varchar(120) NOT NULL,
    descripcion    varchar(500),
    localidad      varchar(40)  NOT NULL,
    creada_en      timestamptz  NOT NULL,
    CONSTRAINT uq_tienda_propietario UNIQUE (propietario_id)
);
CREATE INDEX ix_tienda_localidad ON catalogo.tienda (localidad);

CREATE TABLE catalogo.producto (
    id                uuid          PRIMARY KEY,
    tienda_id         uuid          NOT NULL REFERENCES catalogo.tienda (id) ON DELETE CASCADE,
    nombre            varchar(120)  NOT NULL,
    descripcion       varchar(2000),
    categoria         varchar(30)   NOT NULL,
    precio            numeric(12,2) NOT NULL CHECK (precio > 0),
    stock             integer       NOT NULL CHECK (stock >= 0),
    estado            varchar(20)   NOT NULL DEFAULT 'ACTIVO',
    motivo_moderacion varchar(300),
    creado_en         timestamptz   NOT NULL,
    actualizado_en    timestamptz   NOT NULL,
    -- RNF3 (< 2 s): búsqueda de texto completo dentro de PostgreSQL, sin motor externo.
    busqueda          tsvector GENERATED ALWAYS AS (
                          to_tsvector('spanish', coalesce(nombre, '') || ' ' || coalesce(descripcion, ''))
                      ) STORED
);
CREATE INDEX ix_producto_busqueda ON catalogo.producto USING GIN (busqueda);
CREATE INDEX ix_producto_filtros  ON catalogo.producto (estado, categoria, precio);
CREATE INDEX ix_producto_tienda   ON catalogo.producto (tienda_id);
CREATE INDEX ix_producto_recientes ON catalogo.producto (creado_en DESC);
