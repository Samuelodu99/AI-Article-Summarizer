# Troubleshooting Guide

## 500 Error When Summarizing

If you're getting a "Request failed with status code 500" error, follow these steps:

### 1. Check if Ollama is Running

```powershell
# Check if Ollama is running
ollama list
```

If this command fails or shows an error, Ollama is not running. Start it:
```powershell
ollama serve
```

Or start the Ollama desktop app if you have it installed.

### 2. Check if the Model is Downloaded

```powershell
# List available models
ollama list

# If llama3 is not in the list, download it:
ollama pull llama3
```

### 3. Check Backend Logs

Look at the terminal where you ran `.\mvnw.cmd spring-boot:run`. You should see error messages that indicate what went wrong.

Common error messages:
- **"Connection refused"** → Ollama is not running
- **"Model not found"** → Model not downloaded (run `ollama pull llama3`)
- **"Database error"** → H2 database issue (check if `backend/data` directory exists)

### 4. Test Ollama Directly

```powershell
# Test if Ollama is responding
curl http://localhost:11434/api/tags
```

This should return a JSON response with available models.

### 5. Test the Backend Health

```powershell
# Check if backend is running
curl http://localhost:8080/actuator/health
```

### 6. Check Browser Console

Open your browser's developer console (F12) and check the Network tab when you try to summarize. Look for:
- The exact error message in the response
- The request payload
- The response status code

### 7. Common Solutions

**Issue: "Cannot connect to Ollama"**
- Solution: Make sure Ollama is running (`ollama serve`)

**Issue: "Model not found"**
- Solution: Download the model (`ollama pull llama3`)

**Issue: "Database error"**
- Solution: Delete the `backend/data` directory and restart the backend

**Issue: Java version mismatch (IDE warnings)**
- Solution: These are just IDE warnings. The code should still compile and run. The pom.xml has been updated to fix this.

## IDE Warnings (Non-Critical)

The following warnings in your IDE are **not critical** and won't prevent the app from running:
- "Build path specifies execution environment JavaSE-1.8" → Just an IDE configuration issue
- "Null type safety" warnings → Just IDE warnings, not runtime errors

These warnings can be ignored. The code will still compile and run correctly.
