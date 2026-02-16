# Start the AI Article Summarizer in demo mode (no Ollama required)
# Use this for quick portfolio demos on Windows

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $ProjectRoot

Write-Host "Starting AI Article Summarizer in DEMO mode..."
Write-Host "Backend: http://localhost:8080"
Write-Host "Frontend: http://localhost:5173"
Write-Host ""

$env:DEMO_MODE = "true"

# Start backend in background
$backendJob = Start-Job -ScriptBlock {
    Set-Location $using:ProjectRoot\backend
    & .\mvnw.cmd -q spring-boot:run
}

# Wait for backend to be ready
Write-Host "Waiting for backend to start..."
$maxAttempts = 60
for ($i = 1; $i -le $maxAttempts; $i++) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -UseBasicParsing -TimeoutSec 2 -ErrorAction SilentlyContinue
        if ($response.StatusCode -eq 200) {
            Write-Host "Backend ready!"
            break
        }
    } catch {}
    Start-Sleep -Seconds 2
    if ($i -eq $maxAttempts) {
        Write-Host "Backend failed to start. Check logs."
        Stop-Job $backendJob
        exit 1
    }
}

# Start frontend
Set-Location "$ProjectRoot\frontend"
npm run dev
