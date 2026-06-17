param(
  [string]$ContainerName = "shipinsight-clickhouse",
  [string]$Image = "clickhouse/clickhouse-server:25.8-alpine",
  [string]$Password = "123456",
  [string]$VolumeName = "shipinsight-clickhouse-data"
)

$ErrorActionPreference = "Stop"

function Wait-For-Docker {
  for ($i = 0; $i -lt 60; $i++) {
    docker info *> $null
    if ($LASTEXITCODE -eq 0) {
      return
    }
    Start-Sleep -Seconds 2
  }
  throw "Docker daemon is not ready."
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
  throw "Docker is required to run local ClickHouse."
}

docker info *> $null
if ($LASTEXITCODE -ne 0) {
  $dockerDesktop = "C:\Program Files\Docker\Docker\Docker Desktop.exe"
  if (Test-Path $dockerDesktop) {
    Start-Process -FilePath $dockerDesktop -WindowStyle Hidden
  }
  Wait-For-Docker
}

$existing = docker ps -a --filter "name=^/$ContainerName$" --format "{{.Names}}"
if ($existing -eq $ContainerName) {
  $containerInfo = docker inspect $ContainerName | ConvertFrom-Json
  $mount = $containerInfo[0].Mounts | Where-Object { $_.Destination -eq "/var/lib/clickhouse" } | Select-Object -First 1
  $currentMount = if ($mount) { "$($mount.Type)|$($mount.Name)" } else { "" }
  $expectedMount = "volume|$VolumeName"
  if ($currentMount -eq $expectedMount) {
    if (-not $containerInfo[0].State.Running) {
      docker start $ContainerName | Out-Host
    }
  } else {
    Write-Host "Recreating $ContainerName to use Docker volume: $VolumeName"
    docker stop $ContainerName *> $null
    docker rm $ContainerName *> $null
    docker run -d `
      --name $ContainerName `
      -e CLICKHOUSE_USER=default `
      -e CLICKHOUSE_PASSWORD=$Password `
      -e CLICKHOUSE_DEFAULT_ACCESS_MANAGEMENT=1 `
      -p 8123:8123 `
      -p 9000:9000 `
      -v "${VolumeName}:/var/lib/clickhouse" `
      $Image | Out-Host
  }
} else {
  docker run -d `
    --name $ContainerName `
    -e CLICKHOUSE_USER=default `
    -e CLICKHOUSE_PASSWORD=$Password `
    -e CLICKHOUSE_DEFAULT_ACCESS_MANAGEMENT=1 `
    -p 8123:8123 `
    -p 9000:9000 `
    -v "${VolumeName}:/var/lib/clickhouse" `
    $Image | Out-Host
}

for ($i = 0; $i -lt 90; $i++) {
  Start-Sleep -Seconds 1
  try {
    $response = Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:8123/ping" -TimeoutSec 2
    if ($response.Content.Trim() -eq "Ok.") {
      Write-Host "ClickHouse ready: http://localhost:8123"
      exit 0
    }
  } catch {
  }
}

docker logs --tail 120 $ContainerName
throw "ClickHouse did not become ready in time."
