import React, { useState } from 'react';
import axios from 'axios';

type TargetLength = 'short' | 'medium' | 'long';

interface SummarizeResponse {
  summary: string;
  model: string;
  latencyMs: number;
}

export const App: React.FC = () => {
  const [content, setContent] = useState('');
  const [targetLength, setTargetLength] = useState<TargetLength>('medium');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [response, setResponse] = useState<SummarizeResponse | null>(null);

  const canSubmit = content.trim().length > 0 && !isLoading;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!canSubmit) return;

    setIsLoading(true);
    setError(null);
    setResponse(null);

    try {
      const res = await axios.post<SummarizeResponse>('/api/v1/summarize', {
        content,
        targetLength
      });
      setResponse(res.data);
    } catch (err: any) {
      const message =
        err?.response?.data?.message ||
        err?.message ||
        'Unexpected error while summarizing.';
      setError(message);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="app-root">
      <header className="app-header">
        <div className="logo">AI Article Summarizer</div>
        <div className="header-tagline">Paste any article, get a clean summary in seconds.</div>
      </header>

      <main className="app-main">
        <section className="card">
          <form onSubmit={handleSubmit} className="form">
            <label className="field">
              <span className="field-label">Article content</span>
              <textarea
                className="textarea"
                placeholder="Paste article text here..."
                value={content}
                onChange={(e) => setContent(e.target.value)}
                rows={12}
              />
            </label>

            <div className="field field-inline">
              <span className="field-label">Summary length</span>
              <div className="pill-group">
                {(['short', 'medium', 'long'] as TargetLength[]).map((option) => (
                  <button
                    key={option}
                    type="button"
                    className={`pill ${targetLength === option ? 'pill-active' : ''}`}
                    onClick={() => setTargetLength(option)}
                  >
                    {option.charAt(0).toUpperCase() + option.slice(1)}
                  </button>
                ))}
              </div>
            </div>

            <button
              type="submit"
              className="primary-button"
              disabled={!canSubmit}
            >
              {isLoading ? 'Summarizing…' : 'Generate summary'}
            </button>

            {error && <div className="alert alert-error">{error}</div>}
          </form>
        </section>

        {response && (
          <section className="card card-secondary">
            <div className="summary-header">
              <h2 className="summary-title">Summary</h2>
              <div className="summary-meta">
                <span className="meta-pill">{response.model}</span>
                <span className="meta-pill">{response.latencyMs} ms</span>
              </div>
            </div>
            <p className="summary-body">{response.summary}</p>
          </section>
        )}
      </main>

      <footer className="app-footer">
        <span>Built with Spring Boot, Spring AI & React.</span>
      </footer>
    </div>
  );
};

