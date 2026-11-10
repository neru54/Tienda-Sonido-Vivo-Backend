#!/bin/bash
# ---------------------------------------------------------------
# Inicialización del contenedor MySQL de compose.yaml (solo la PRIMERA vez, con el volumen vacío).
# Hace lo mismo que sql/01_crear_esquemas_mysql.sql, pero toma las contraseñas de las variables
# de entorno (del .env), para no escribir secretos en archivos versionados.
# La imagen oficial de MySQL ejecuta automáticamente los *.sh de /docker-entrypoint-initdb.d.
# ---------------------------------------------------------------
set -euo pipefail

mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" <<SQL
CREATE DATABASE IF NOT EXISTS sonido_usuarios CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS sonido_catalogo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS sonido_pedidos  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS '${DB_USUARIOS_USER}'@'%' IDENTIFIED BY '${DB_USUARIOS_PASSWORD}';
CREATE USER IF NOT EXISTS '${DB_CATALOGO_USER}'@'%' IDENTIFIED BY '${DB_CATALOGO_PASSWORD}';
CREATE USER IF NOT EXISTS '${DB_PEDIDOS_USER}'@'%'  IDENTIFIED BY '${DB_PEDIDOS_PASSWORD}';

GRANT ALL PRIVILEGES ON sonido_usuarios.* TO '${DB_USUARIOS_USER}'@'%';
GRANT ALL PRIVILEGES ON sonido_catalogo.* TO '${DB_CATALOGO_USER}'@'%';
GRANT ALL PRIVILEGES ON sonido_pedidos.*  TO '${DB_PEDIDOS_USER}'@'%';
FLUSH PRIVILEGES;
SQL

echo "Sonido Vivo: esquemas sonido_usuarios, sonido_catalogo y sonido_pedidos creados con un usuario por servicio."
