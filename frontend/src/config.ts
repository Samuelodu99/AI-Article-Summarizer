/**
 * API base URL for backend requests.
 * - In development: empty string (Vite proxies /api to backend)
 * - In production (Vercel/static): set VITE_API_URL to your backend URL (e.g. https://your-backend.onrender.com)
 */
export const API_BASE = (import.meta.env.VITE_API_URL as string) || '';
