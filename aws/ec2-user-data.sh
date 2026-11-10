#!/bin/bash
# ---------------------------------------------------------------
# User data de la EC2 (Amazon Linux 2023): se ejecuta UNA vez, como root, al crear la instancia.
# Instala Docker, el plugin docker compose y git, y crea 2 GB de swap (las JVM de los 4 servicios
# más la compilación con Maven no caben cómodas en los 2 GB de una t3.small).
# Registro de ejecución en la instancia: /var/log/cloud-init-output.log
# ---------------------------------------------------------------
set -euxo pipefail

dnf update -y
dnf install -y docker git
systemctl enable --now docker
usermod -aG docker ec2-user

# Plugin docker compose v2 (no viene en el paquete docker de AL2023)
mkdir -p /usr/local/lib/docker/cli-plugins
ARQ=$(uname -m)
curl -fsSL "https://github.com/docker/compose/releases/download/v2.29.7/docker-compose-linux-${ARQ}" \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

# Swap de 2 GB
if [ ! -f /swapfile ]; then
  dd if=/dev/zero of=/swapfile bs=1M count=2048
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  echo '/swapfile swap swap defaults 0 0' >> /etc/fstab
fi

echo "Sonido Vivo: Docker $(docker --version) y $(docker compose version) listos" > /home/ec2-user/LISTO.txt
chown ec2-user:ec2-user /home/ec2-user/LISTO.txt
