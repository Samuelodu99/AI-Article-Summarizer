## AI Article Summarizer

A full‑stack, production‑ready AI Article Summarizer built with **Java + Spring Boot + Spring AI** on the backend and **React + Vite** on the frontend.

### Backend (Java + Spring Boot + Spring AI)

- **Stack**: Spring Boot 3, Spring Web, Validation, Actuator, Spring AI (Ollama starter - **100% free, runs locally**).
- **AI Provider**: **Ollama** - runs models locally on your machine, completely free, no API keys needed!
- **Core endpoint**: `POST /api/v1/summarize`
  - Request JSON:
    - `content` (string, required): raw article text.
    - `targetLength` (string, optional): `"short" | "medium" | "long"`.
  - Response JSON:
    - `summary` (string): generated summary.
    - `model` (string): LLM model identifier (if reported by provider).
    - `latencyMs` (number): end‑to‑end latency in milliseconds.
- **Config**: `backend/src/main/resources/application.yml`
  - Defaults to `http://localhost:11434` (Ollama default port).
  - Default model: `llama3` (can be changed via `OLLAMA_MODEL` env var).

#### Running the backend locally

1. **Install Ollama** (if not already installed):
   - Download from: https://ollama.com/download
   - Or via winget: `winget install Ollama.Ollama`
   - Start Ollama (it runs as a service, or run `ollama serve` manually).

2. **Download a model** (first time only):
   ```bash
   ollama pull llama3
   ```
   Other good options: `mistral`, `llama3.2`, `phi3`, `gemma2`

3. **Install JDK 21** and **Maven** (or use the Maven wrapper).

4. From the `backend` directory:
   ```bash
   .\mvnw.cmd spring-boot:run
   ```
   (No API keys needed! Ollama runs locally.)

### Frontend (React + Vite)

- **Stack**: React 18, TypeScript, Vite, Axios.
- **UX**:
  - Paste article text into a large editor.
  - Choose summary length via pill controls.
  - One‑click “Generate summary” button with loading and error states.
  - Clean dark UI with responsive layout.
- **Dev server**: Vite dev server proxies `/api` → `http://localhost:8080`.

#### Running the frontend locally

1. Install **Node.js 20+** and `npm`.
2. From the `frontend` directory:
   - `npm install`
   - `npm run dev`
3. Open `http://localhost:5173` in your browser.

### Docker & Deployment

- `backend/Dockerfile`: builds a minimal JVM image to run the Spring Boot JAR.
- `frontend/Dockerfile`: builds the Vite app and serves it via Nginx.
- `docker-compose.yml`:
  - Builds and runs both `backend` and `frontend`.
  - Exposes:
    - Backend at `http://localhost:8080`
    - Frontend at `http://localhost:5173`
  - Requires Ollama running locally (or set `OLLAMA_BASE_URL` to point to remote Ollama instance).

Run everything with:

```bash
docker compose up --build
```

### Next steps / ideas

- Add URL fetching (scrape article content from a URL before summarizing).
- Add history of recent summaries with local persistence.
- Currently using Ollama (free, local). Can easily switch to OpenAI, Anthropic, etc. via Spring AI profiles.
- Add authentication (API keys / OAuth) if you expose it publicly.

