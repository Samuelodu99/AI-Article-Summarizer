#!/bin/bash
# Start the AI Article Summarizer in demo mode (no Ollama required)
# Use this in GitHub Codespaces or for quick portfolio demos

set -e
cd "$(dirname "$0")/.."

echo "Starting AI Article Summarizer in DEMO mode..."
echo "Backend: http://localhost:8080"
echo "Frontend: http://localhost:5173"
echo ""

# Start backend in background with DEMO_MODE
export DEMO_MODE=true
cd backend
./mvnw -q spring-boot:run &
BACKEND_PID=$!
cd ..

# Wait for backend to be ready
echo "Waiting for backend to start..."
for i in {1..60}; do
  if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "Backend ready!"
    break
  fi
  sleep 2
  if [ $i -eq 60 ]; then
    echo "Backend failed to start. Check logs."
    kill $BACKEND_PID 2>/dev/null || true
    exit 1
  fi
done

# Start frontend (foreground)
cd frontend
npm run dev
