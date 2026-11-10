-- ---------------------------------------------------------------
-- Sonido Vivo - UNA instancia MySQL 8 con TRES esquemas (en MySQL esquema = base de datos).
-- Cada microservicio tiene su propio usuario con permisos SOLO sobre su esquema:
--
--   sonido_usuarios  <- sv_usuarios  (microservicio usuarios)
--   sonido_catalogo  <- sv_catalogo  (microservicio catalogo)
--   sonido_pedidos   <- sv_pedidos   (microservicio pedidos)
--
-- No hay claves foráneas entre esquemas: los servicios se referencian por ID (RUN, código de
-- producto) y se comunican por HTTP. Las TABLAS las crea Hibernate (ddl-auto=update) en el primer
-- arranque de cada servicio; este script solo crea esquemas y usuarios.
--
-- Uso manual (MySQL Workbench, cliente mysql en la EC2 contra RDS, etc.) con una cuenta administradora:
--   1. Reemplaza las tres contraseñas CAMBIAR_... por valores propios.
--   2. Copia esas mismas contraseñas a DB_USUARIOS_PASSWORD, DB_CATALOGO_PASSWORD y DB_PEDIDOS_PASSWORD del .env.
--   3. Ejecuta:  mysql -h <host> -u admin -p < sql/01_crear_esquemas_mysql.sql
-- En Docker no hace falta: sql/docker/01_init.sh hace lo mismo leyendo las contraseñas del .env.
--
-- '%' permite conectarse desde otra máquina (los contenedores o la EC2). En RDS el acceso real lo
-- restringe el Security Group (solo el puerto 3306 desde la EC2).
-- ---------------------------------------------------------------

CREATE DATABASE IF NOT EXISTS sonido_usuarios CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS sonido_catalogo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS sonido_pedidos  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'sv_usuarios'@'%' IDENTIFIED BY 'CAMBIAR_usuarios';
CREATE USER IF NOT EXISTS 'sv_catalogo'@'%' IDENTIFIED BY 'CAMBIAR_catalogo';
CREATE USER IF NOT EXISTS 'sv_pedidos'@'%'  IDENTIFIED BY 'CAMBIAR_pedidos';

-- ALL sobre SU esquema (Hibernate necesita CREATE/ALTER/INDEX para crear las tablas). Nada sobre los otros.
GRANT ALL PRIVILEGES ON sonido_usuarios.* TO 'sv_usuarios'@'%';
GRANT ALL PRIVILEGES ON sonido_catalogo.* TO 'sv_catalogo'@'%';
GRANT ALL PRIVILEGES ON sonido_pedidos.*  TO 'sv_pedidos'@'%';

FLUSH PRIVILEGES;

-- Nota: IF NOT EXISTS no cambia la contraseña de un usuario que ya existe. Para cambiarla:
--   ALTER USER 'sv_usuarios'@'%' IDENTIFIED BY 'nueva';
-- Comprobación: SHOW GRANTS FOR 'sv_catalogo'@'%';  (solo debe aparecer sonido_catalogo.*)
