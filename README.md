## AI Article Summarizer

A full‑stack, production‑ready AI Article Summarizer built with **Java + Spring Boot + Spring AI** on the backend and **React + Vite** on the frontend.

### ✨ Features

- **📝 Text & URL Input**: Paste article text directly or provide a URL to automatically fetch and extract content
- **🤖 AI-Powered Summarization**: Uses Ollama (local, free) with support for multiple summary lengths (short, medium, long)
- **📚 Summary History**: Automatically saves all summaries with local persistence (H2 database)
- **🔍 Search & Browse**: View, search, and manage your summary history
- **🎨 Modern UI**: Clean, responsive dark theme with intuitive controls
- **🔒 Security**: User authentication (JWT), admin role, input validation, CORS protection, and prompt injection prevention

### Backend (Java + Spring Boot + Spring AI)

- **Stack**: Spring Boot 3, Spring Web, Validation, Actuator, Spring AI (Ollama starter - **100% free, runs locally**).
- **AI Provider**: **Ollama** - runs models locally on your machine, completely free, no API keys needed!
- **Core endpoints**:
  - `POST /api/v1/summarize` - Generate summary from text or URL
    - Request JSON:
      - `content` (string, optional): raw article text (required if `url` not provided)
      - `url` (string, optional): article URL to fetch (required if `content` not provided)
      - `targetLength` (string, optional): `"short" | "medium" | "long"` (default: "medium")
    - Response JSON:
      - `id` (number): summary ID for history lookup
      - `summary` (string): generated summary
      - `model` (string): LLM model identifier
      - `latencyMs` (number): end‑to‑end latency in milliseconds
      - `sourceUrl` (string, optional): original URL if fetched from web
      - `articleTitle` (string, optional): extracted article title
      - `createdAt` (string): timestamp of creation
  - `GET /api/v1/history` - Get summary history
    - Query params: `limit` (default: 10), `search` (optional search term)
  - `GET /api/v1/history/{id}` - Get specific summary by ID
  - `DELETE /api/v1/history/{id}` - Delete a summary
  - `DELETE /api/v1/history` - Delete all history
  - **Auth** (no token required):
    - `POST /api/auth/register` - Register (body: `username`, `password`). First user becomes **ADMIN**.
    - `POST /api/auth/login` - Login (body: `username`, `password`). Returns JWT and user info.
  - **Admin** (requires `Authorization: Bearer <token>` and role **ADMIN**):
    - `GET /api/admin/users` - List all users
    - `GET /api/admin/stats` - Total users and summaries
- **Database**: H2 (file-based, no setup required)
  - Database file: `./data/summarizer.mv.db`
  - H2 Console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:file:./data/summarizer`)
- **Config**: `backend/src/main/resources/application.yml`
  - Defaults to `http://localhost:11434` (Ollama default port)
  - Default model: `llama3` (can be changed via `OLLAMA_MODEL` env var)
  - **JWT** (required for auth): set `JWT_SECRET` in production (min 32 characters). Optional: `JWT_EXPIRATION_MS` (default 24h).

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
  - **Login / Register**: Required before using the app. First registered user is an admin.
  - **Admin page** (`/admin`): List users and view stats (admin role only).
  - **Dual input modes**: Paste article text OR provide URL to fetch automatically
  - **Summary history**: View, search, and manage all past summaries
  - **Tabbed interface**: Switch between "Summarize" and "History" views
  - Choose summary length via pill controls (short, medium, long)
  - One‑click "Generate summary" button with loading and error states
  - Clean dark UI with responsive layout
  - Click any history item to view its full summary
- **Dev server**: Vite dev server proxies `/api` → `http://localhost:8080`

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

### ✅ Implemented Features

- ✅ **User authentication** - JWT-based login/register; first user is admin
- ✅ **Admin page** - List users, view stats (admin only)
- ✅ **URL fetching** - Automatically extracts article content from URLs using Jsoup
- ✅ **Summary history** - All summaries saved to H2 database with search and management
- ✅ **Dual input modes** - Support for both text paste and URL input
- ✅ **History UI** - Browse, search, and delete past summaries

### 🚀 Next Steps / Ideas

- Add export functionality (PDF, Markdown, Word)
- Add batch processing (multiple URLs at once)
- Add real-time streaming responses
- Support multiple AI providers (OpenAI, Anthropic, etc.) via Spring AI profiles
- User-scoped history (per-user summaries)
- Add summary sharing via unique links
- Add authentication (API keys / OAuth) if you expose it publicly.

