# Backend Startup Script
# Run this from the backend directory or project root

$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendPath = if (Test-Path "$scriptPath\backend") { "$scriptPath\backend" } else { $scriptPath }

Set-Location $backendPath

# Set Java environment
$env:JAVA_HOME = "C:\Program Files\Java\jdk-22"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

Write-Host "Starting backend from: $backendPath" -ForegroundColor Green
Write-Host "Java Home: $env:JAVA_HOME" -ForegroundColor Cyan

# Run Maven
.\mvnw.cmd spring-boot:run
