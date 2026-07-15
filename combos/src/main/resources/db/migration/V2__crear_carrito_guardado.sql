-- V2: tabla para el carrito persistente de clientes con cuenta.
-- Usamos IF NOT EXISTS porque quizás ya la creaste a mano antes de tener Flyway;
-- así esta migración no falla si la tabla ya existe.
CREATE TABLE IF NOT EXISTS carrito_guardado (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    cantidad INT NOT NULL
);
