# 🚀 Quick Start Guide

## Prerequisites

Before running the project, ensure you have:

1. **Java JDK 21 or 22** installed
   - Check: `java -version`
   - Your JDK is at: `C:\Program Files\Java\jdk-22`

2. **Node.js 20+** installed
   - Check: `node --version`
   - Download: https://nodejs.org/

3. **Ollama** installed and running
   - Download: https://ollama.com/download
   - Or install via: `winget install Ollama.Ollama`
   - Start Ollama: `ollama serve` (or use the desktop app)

4. **Ollama Model** downloaded
   - Run: `ollama pull llama3`
   - This downloads the AI model (first time only, ~4GB)

---

## Running the Project

### Option 1: Quick Start (Recommended)

#### Terminal 1 - Backend:
```powershell
# From project root
.\start-backend.ps1
```

Or manually:
```powershell
cd backend
$env:JAVA_HOME="C:\Program Files\Java\jdk-22"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd spring-boot:run
```

#### Terminal 2 - Frontend:
```powershell
cd frontend
npm install  # First time only
npm run dev
```

### Option 2: Using Docker

```powershell
docker compose up --build
```

---

## Access the Application

- **Frontend**: http://localhost:5173
- **Backend API**: http://localhost:8080
- **H2 Database Console**: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:file:./data/summarizer`
  - Username: `sa`
  - Password: (leave empty)

---

## Verify Everything Works

1. **Check Ollama is running:**
   ```powershell
   ollama list
   ```
   Should show `llama3` (or your chosen model)

2. **Check Backend is running:**
   - Look for: `Started AIArticleSummarizerApplication` in backend logs
   - Visit: http://localhost:8080/actuator/health

3. **Check Frontend is running:**
   - Visit: http://localhost:5173
   - You should see the AI Article Summarizer UI

4. **Test Summarization:**
   - Paste some text or enter a URL
   - Click "Generate summary"
   - You should see a summary appear!

---

## Troubleshooting

### Backend won't start
- **Check Java**: `java -version` should show Java 21 or 22
- **Check Ollama**: Make sure `ollama serve` is running
- **Check model**: Run `ollama pull llama3` if you see "model not found"

### Frontend won't start
- **Check Node.js**: `node --version` should be 20+
- **Reinstall dependencies**: `cd frontend && rm -rf node_modules && npm install`

### "Cannot connect to Ollama"
- Make sure Ollama is running: `ollama serve`
- Check it's on port 11434: Visit http://localhost:11434

### "Model not found"
- Download the model: `ollama pull llama3`
- Check available models: `ollama list`

---

## Development Tips

- **Backend logs**: Check the terminal where you ran `.\start-backend.ps1`
- **Frontend hot reload**: Changes to frontend files auto-reload
- **Database**: All summaries are saved in `backend/data/summarizer.mv.db`
- **Export features**: Click the PDF/MD buttons on any summary to export

---

## Stopping the Application

- **Backend**: Press `Ctrl+C` in the backend terminal
- **Frontend**: Press `Ctrl+C` in the frontend terminal
- **Docker**: `docker compose down`

---

## Next Steps

- Try summarizing a URL: Paste any article URL
- View history: Click the "History" tab
- Export summaries: Use the PDF/Markdown export buttons
- Try streaming: Enable "Streaming: On" for real-time responses

Enjoy! 🎉
