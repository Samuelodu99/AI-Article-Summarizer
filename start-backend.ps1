# Backend Startup Script
# Run this from the project root

$backendPath = Join-Path $PSScriptRoot "backend"

if (-not (Test-Path $backendPath)) {
    Write-Host "Error: backend directory not found at $backendPath" -ForegroundColor Red
    exit 1
}

Set-Location $backendPath

# Set Java environment
$env:JAVA_HOME = "C:\Program Files\Java\jdk-22"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

Write-Host "Starting backend..." -ForegroundColor Green
Write-Host "Java Home: $env:JAVA_HOME" -ForegroundColor Cyan

# Run Maven
.\mvnw.cmd spring-boot:run
