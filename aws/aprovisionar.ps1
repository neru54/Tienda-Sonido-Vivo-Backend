# ---------------------------------------------------------------
# Crea en AWS Academy (Learner Lab) la infraestructura del backend con AWS CLI:
#   1. Security Groups: sv-ec2-sg (22 desde tu IP, 8080 público) y sv-rds-sg (3306 SOLO desde sv-ec2-sg)
#   2. RDS MySQL 8 (db.t3.micro, 20 GB, NO público, sin Multi-AZ ni respaldos para ahorrar crédito)
#   3. EC2 Amazon Linux 2023 con Docker (aws/ec2-user-data.sh) y el perfil LabInstanceProfile
#   4. (Opcional) IP elástica para que la IP pública no cambie al detener/iniciar el laboratorio
#
# NO se ha ejecutado en la cuenta: revisa los parámetros antes de correrlo. Uso (PowerShell):
#   powershell -ExecutionPolicy Bypass -File aws/aprovisionar.ps1 -RdsPassword 'ClaveAdminSegura123'
#   powershell -ExecutionPolicy Bypass -File aws/aprovisionar.ps1 -RdsPassword '...' -TipoEc2 t3.medium -IpElastica
#
# Requisitos: AWS CLI v2 y credenciales vigentes del laboratorio en ~/.aws/credentials
# (AWS Details > AWS CLI > Show: aws_access_key_id, aws_secret_access_key y aws_session_token).
# ---------------------------------------------------------------
param(
  [Parameter(Mandatory = $true)][string]$RdsPassword,
  [string]$Region = 'us-east-1',
  [string]$TipoEc2 = 't3.small',
  [string]$ClaseRds = 'db.t3.micro',
  [string]$KeyName = 'vockey',             # par de llaves que trae el Learner Lab (labsuser.pem)
  [string]$PerfilInstancia = 'LabInstanceProfile',
  [switch]$IpElastica
)
$ErrorActionPreference = 'Stop'
$env:AWS_DEFAULT_REGION = $Region

function Aws {
  $salida = & aws @args --output text 2>&1
  if ($LASTEXITCODE -ne 0) { throw "aws $($args -join ' ') -> $salida" }
  return ($salida | Out-String).Trim()
}

if ($RdsPassword.Length -lt 8 -or $RdsPassword -match '[/@" ]') {
  throw 'La contraseña maestra de RDS debe tener 8+ caracteres y no puede contener / @ " ni espacios.'
}
Write-Host "Cuenta: $(Aws sts get-caller-identity --query Arn)"

# --- 1. Red: VPC por defecto y Security Groups ---
$vpc = Aws ec2 describe-vpcs --filters Name=isDefault,Values=true --query 'Vpcs[0].VpcId'
$miIp = (Invoke-RestMethod -Uri 'https://checkip.amazonaws.com').Trim()
Write-Host "VPC por defecto: $vpc   Tu IP pública: $miIp"

function GrupoSeguridad([string]$nombre, [string]$descripcion) {
  $id = Aws ec2 describe-security-groups --filters "Name=group-name,Values=$nombre" "Name=vpc-id,Values=$vpc" --query 'SecurityGroups[0].GroupId'
  if ($id -eq 'None' -or -not $id) {
    $id = Aws ec2 create-security-group --group-name $nombre --description $descripcion --vpc-id $vpc --query GroupId
    Write-Host "Creado $nombre ($id)"
  } else { Write-Host "Ya existe $nombre ($id)" }
  return $id
}
$sgEc2 = GrupoSeguridad 'sv-ec2-sg' 'Sonido Vivo EC2: SSH desde mi IP y Gateway 8080'
$sgRds = GrupoSeguridad 'sv-rds-sg' 'Sonido Vivo RDS: MySQL solo desde la EC2'

function Permitir([string]$sg, [string[]]$regla) {
  & aws ec2 authorize-security-group-ingress --group-id $sg @regla 2>$null | Out-Null  # si ya existe, se ignora
}
Permitir $sgEc2 @('--protocol', 'tcp', '--port', '22', '--cidr', "$miIp/32")
Permitir $sgEc2 @('--protocol', 'tcp', '--port', '8080', '--cidr', '0.0.0.0/0')
# 3306 solo desde las instancias que tengan sv-ec2-sg (no desde Internet)
Permitir $sgRds @('--protocol', 'tcp', '--port', '3306', '--source-group', $sgEc2)

# --- 2. RDS MySQL 8 ---
$idRds = 'sonido-vivo-mysql'
$existe = & aws rds describe-db-instances --db-instance-identifier $idRds --query 'DBInstances[0].DBInstanceStatus' --output text 2>$null
if ($LASTEXITCODE -ne 0) {
  Write-Host "Creando RDS $idRds ($ClaseRds, 20 GB). Tarda 5-10 minutos..."
  Aws rds create-db-instance --db-instance-identifier $idRds --engine mysql --engine-version 8.0 `
    --db-instance-class $ClaseRds --allocated-storage 20 --storage-type gp2 `
    --master-username admin --master-user-password $RdsPassword `
    --vpc-security-group-ids $sgRds --no-publicly-accessible --no-multi-az `
    --backup-retention-period 0 --no-deletion-protection --query 'DBInstance.DBInstanceIdentifier' | Out-Null
} else { Write-Host "RDS $idRds ya existe (estado: $existe)" }

# --- 3. EC2 Amazon Linux 2023 ---
$ami = Aws ssm get-parameters --names /aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64 --query 'Parameters[0].Value'
$userData = Join-Path $PSScriptRoot 'ec2-user-data.sh'
Write-Host "Creando EC2 $TipoEc2 con AMI $ami..."
$idEc2 = Aws ec2 run-instances --image-id $ami --instance-type $TipoEc2 --key-name $KeyName `
  --security-group-ids $sgEc2 --iam-instance-profile "Name=$PerfilInstancia" `
  --user-data "file://$userData" `
  --block-device-mappings 'DeviceName=/dev/xvda,Ebs={VolumeSize=20,VolumeType=gp3}' `
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=sonido-vivo-backend}]' `
  --query 'Instances[0].InstanceId'
Aws ec2 wait instance-running --instance-ids $idEc2 | Out-Null

if ($IpElastica) {
  $alloc = Aws ec2 allocate-address --domain vpc --query AllocationId
  Aws ec2 associate-address --instance-id $idEc2 --allocation-id $alloc | Out-Null
  Write-Host "IP elástica asociada ($alloc)"
}
$ip = Aws ec2 describe-instances --instance-ids $idEc2 --query 'Reservations[0].Instances[0].PublicIpAddress'

Write-Host 'Esperando que RDS quede disponible...'
Aws rds wait db-instance-available --db-instance-identifier $idRds | Out-Null
$endpoint = Aws rds describe-db-instances --db-instance-identifier $idRds --query 'DBInstances[0].Endpoint.Address'

Write-Host ''
Write-Host 'Listo:' -ForegroundColor Green
Write-Host "  EC2:          $idEc2   IP pública: $ip"
Write-Host "  RDS endpoint: $endpoint  (DB_HOST del .env de la EC2)"
Write-Host "  SSH:          ssh -i labsuser.pem ec2-user@$ip"
Write-Host "  API:          http://${ip}:8080/api/v1   (cuando los contenedores estén arriba)"
Write-Host 'Siguiente: aws/README_AWS.md, paso 4 (clonar el repo, .env, SQL y docker compose).'
