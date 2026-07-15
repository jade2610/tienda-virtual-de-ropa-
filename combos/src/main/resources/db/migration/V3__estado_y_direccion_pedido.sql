-- V3: agrega el estado del pedido (para poder marcarlo como enviado/entregado/cancelado)
-- y la dirección de entrega, que hasta ahora no se guardaba en ningún lado.
ALTER TABLE pedidos ADD COLUMN estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE';
ALTER TABLE pedidos ADD COLUMN direccion_entrega VARCHAR(255) NULL;
