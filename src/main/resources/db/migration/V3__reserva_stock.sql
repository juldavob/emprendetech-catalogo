-- Reserva de stock en dos fases (SAD §4.1 y §6): Pedidos reserva, Pagos confirma o libera.
-- Se identifica por pedido_id (identificador lógico, sin llave foránea a otro módulo).
CREATE TABLE catalogo.reserva_stock (
    id             uuid        PRIMARY KEY,
    pedido_id      uuid        NOT NULL,
    producto_id    uuid        NOT NULL REFERENCES catalogo.producto (id),
    cantidad       integer     NOT NULL CHECK (cantidad > 0),
    estado         varchar(20) NOT NULL,
    creada_en      timestamptz NOT NULL,
    actualizada_en timestamptz NOT NULL,
    CONSTRAINT uq_reserva_pedido_producto UNIQUE (pedido_id, producto_id)
);
CREATE INDEX ix_reserva_pedido ON catalogo.reserva_stock (pedido_id);
