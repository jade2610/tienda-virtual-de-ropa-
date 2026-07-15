-- V4: tabla de favoritos/lista de deseos de clientes.
-- producto_id guarda el id de CUALQUIER fila/talla del producto (igual que
-- "idReferencia" en ProductoVista), suficiente para reconstruir el grupo
-- completo con ProductoService.obtenerGrupoPorId().
CREATE TABLE IF NOT EXISTS favoritos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    fecha_agregado DATETIME NOT NULL,
    CONSTRAINT uq_favorito_cliente_producto UNIQUE (cliente_id, producto_id)
);
