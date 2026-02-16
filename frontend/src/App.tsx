import React, { useState, useEffect, useRef } from 'react';
import { Routes, Route, Link } from 'react-router-dom';
import axios from 'axios';
import { useAuth } from './context/AuthContext';
import { API_BASE } from './config';
import { ProtectedRoute } from './components/ProtectedRoute';
import { AdminRoute } from './components/AdminRoute';
import { Login } from './pages/Login';
import { Register } from './pages/Register';
import { Admin } from './pages/Admin';

type TargetLength = 'short' | 'medium' | 'long';
type InputMode = 'text' | 'url';

interface SummarizeResponse {
  id?: number;
  summary: string;
  model: string;
  latencyMs: number;
  sourceUrl?: string;
  articleTitle?: string;
  createdAt?: string;
}

interface SummaryHistoryItem {
  id: number;
  summary: string;
  sourceUrl?: string;
  articleTitle?: string;
  targetLength?: string;
  model: string;
  latencyMs: number;
  createdAt: string;
  preview?: string;
}

function SummarizerApp() {
  const { user, logout, token } = useAuth();
  const [inputMode, setInputMode] = useState<InputMode>('text');
  const [content, setContent] = useState('');
  const [url, setUrl] = useState('');
  const [targetLength, setTargetLength] = useState<TargetLength>('medium');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [response, setResponse] = useState<SummarizeResponse | null>(null);
  const [history, setHistory] = useState<SummaryHistoryItem[]>([]);
  const [showHistory, setShowHistory] = useState(false);
  const [selectedHistoryItem, setSelectedHistoryItem] = useState<SummaryHistoryItem | null>(null);
  const [useStreaming, setUseStreaming] = useState(false);
  const [streamingText, setStreamingText] = useState('');
  const [isStreaming, setIsStreaming] = useState(false);
  const [copySuccess, setCopySuccess] = useState(false);
  const summaryBodyRef = useRef<HTMLParagraphElement>(null);
  const summaryContainerRef = useRef<HTMLDivElement>(null);

  const canSubmit = ((inputMode === 'text' && content.trim().length > 0) || 
                     (inputMode === 'url' && url.trim().length > 0)) && !isLoading;

  // Load history on mount
  useEffect(() => {
    loadHistory();
  }, []);

  const loadHistory = async () => {
    try {
      const res = await axios.get<SummaryHistoryItem[]>('/api/v1/history?limit=20');
      setHistory(res.data);
    } catch (err) {
      console.error('Failed to load history:', err);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!canSubmit) return;

    if (useStreaming) {
      handleStreamingSubmit();
      return;
    }

    setIsLoading(true);
    setError(null);
    setResponse(null);
    setSelectedHistoryItem(null);
    setStreamingText('');

    try {
      const requestData: any = {
        targetLength
      };

      if (inputMode === 'url') {
        requestData.url = url.trim();
      } else {
        requestData.content = content.trim();
      }

      const res = await axios.post<SummarizeResponse>('/api/v1/summarize', requestData);
      setResponse(res.data);
      // Reload history after successful summary
      await loadHistory();
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

  const handleStreamingSubmit = async () => {
    setIsStreaming(true);
    setError(null);
    setResponse(null);
    setSelectedHistoryItem(null);
    setStreamingText('');

    try {
      const requestData: any = {
        targetLength
      };

      if (inputMode === 'url') {
        requestData.url = url.trim();
      } else {
        requestData.content = content.trim();
      }

      // Use fetch with ReadableStream for POST requests with SSE
      const headers: Record<string, string> = { 'Content-Type': 'application/json' };
      if (token) headers['Authorization'] = `Bearer ${token}`;
      const response = await fetch(`${API_BASE}/api/v1/summarize/stream`, {
        method: 'POST',
        headers,
        body: JSON.stringify(requestData),
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: 'Streaming request failed' }));
        throw new Error(errorData.message || 'Streaming request failed');
      }

      const reader = response.body?.getReader();
      const decoder = new TextDecoder();
      let buffer = '';
      let receivedDone = false;

      if (!reader) {
        throw new Error('No response body');
      }

      while (true) {
        const { done, value } = await reader.read();
        if (value) buffer += decoder.decode(value, { stream: true });
        if (done) break;
        // Normalize \r\n to \n so SSE parsing works when server uses CRLF
        buffer = buffer.replace(/\r\n/g, '\n');

        // SSE format: messages are separated by double newlines
        // Format: "event: <name>\ndata: <data>\n\n"
        const messages = buffer.split('\n\n');
        buffer = messages.pop() || ''; // Keep incomplete message in buffer

        for (const message of messages) {
          if (!message.trim()) continue;

          const lines = message.split('\n');
          let eventName = '';
          let data = '';

          for (const line of lines) {
            // Be lenient with spaces after the colon: handle both "event:error" and "event: error"
            if (line.startsWith('event:')) {
              eventName = line.substring('event:'.length).trim();
            } else if (line.startsWith('data:')) {
              const value = line.substring('data:'.length);
              data = data ? `${data}\n${value}` : value;
            }
          }

          // Backend sends "error" event for URL fetch failures (e.g. 403) and other stream errors
          if (eventName === 'error' && data) {
            setIsStreaming(false);
            setIsLoading(false);
            setError(data.trim() || 'An error occurred while streaming.');
            setStreamingText('');
            return;
          }

          // Also treat bare error message (no event line) as error when stream failed
          if (!eventName && data && data.trim().startsWith('Failed to fetch content from URL')) {
            setIsStreaming(false);
            setIsLoading(false);
            setError(data.trim());
            setStreamingText('');
            return;
          }

          if (data) {
            if (data.trim() === '[DONE]') {
              receivedDone = true;
              // First, stop the streaming indicators
              setIsStreaming(false);
              setIsLoading(false);

              // Load history to get the saved summary with ID
              await loadHistory();

              // Find the most recent summary and set it as selected
              // This ensures the summary section stays visible with the final content
              try {
                const historyResponse = await axios.get('/api/v1/history?limit=1');
                if (historyResponse.data && historyResponse.data.length > 0) {
                  setSelectedHistoryItem(historyResponse.data[0]);
                }
              } catch (err) {
                console.error('Failed to load summary from history:', err);
              }

              // Clear streaming text since we now have the final summary in selectedHistoryItem
              setStreamingText('');
              return;
            }
            
            // Append data with smart spacing
            // Ollama sends token-level chunks that may not include spaces between words
            setStreamingText((prev) => {
              let newText = prev;
              
              // Add space between chunks if needed to prevent word concatenation
              if (prev.length > 0 && data.length > 0) {
                const lastChar = prev[prev.length - 1];
                const firstChar = data[0];
                
                const isLetter = (char: string) => /[a-zA-Z]/.test(char);
                const isDigit = (char: string) => /[0-9]/.test(char);
                const isWhitespace = (char: string) => /\s/.test(char);
                const isPunctuation = (char: string) => /[.,!?;:()\[\]{}"'`\-]/.test(char);
                
                // Don't add space if either character is already whitespace
                if (!isWhitespace(lastChar) && !isWhitespace(firstChar)) {
                  // Add space between letters (word boundary)
                  if (isLetter(lastChar) && isLetter(firstChar)) {
                    newText += ' ';
                  }
                  // Add space between letter and digit or vice versa
                  else if ((isLetter(lastChar) && isDigit(firstChar)) || 
                           (isDigit(lastChar) && isLetter(firstChar))) {
                    newText += ' ';
                  }
                  // Add space after punctuation if followed by a letter/digit
                  else if (isPunctuation(lastChar) && (isLetter(firstChar) || isDigit(firstChar))) {
                    // Only add space if punctuation is sentence-ending (not hyphen, apostrophe, etc.)
                    if (/[.,!?;:]/.test(lastChar)) {
                      newText += ' ';
                    }
                  }
                }
              }
              
              newText += data;
              
              // Auto-scroll to bottom when new text arrives
              requestAnimationFrame(() => {
                if (summaryContainerRef.current) {
                  summaryContainerRef.current.scrollTop = summaryContainerRef.current.scrollHeight;
                }
              });
              return newText;
            });
          }
        }
      }

      // Process any remaining buffer (last chunk may contain done/error event without trailing \n\n)
      if (buffer.trim()) {
        buffer = buffer.replace(/\r\n/g, '\n');
        const lines = buffer.split('\n');
        let eventName = '';
        let data = '';
        for (const line of lines) {
          if (line.startsWith('event:')) {
            eventName = line.substring('event:'.length).trim();
          } else if (line.startsWith('data:')) {
            const value = line.substring('data:'.length);
            data = data ? `${data}\n${value}` : value;
          }
        }
        // Success: done event or bare [DONE] in last chunk (event line may have been in previous chunk)
        if ((eventName === 'done' || eventName === '') && data?.trim() === '[DONE]') {
          receivedDone = true;
        }
        if (eventName === 'error' && data) {
          setIsStreaming(false);
          setIsLoading(false);
          setError(data.trim() || 'An error occurred while streaming.');
          setStreamingText('');
          return;
        }
        if (!eventName && data && data.trim().startsWith('Failed to fetch content from URL')) {
          setIsStreaming(false);
          setIsLoading(false);
          setError(data.trim());
          setStreamingText('');
          return;
        }
      }

      // Stream ended (reader.read() returned done). Only show the latest history
      // item if we actually received [DONE] (success). Otherwise the request failed
      // (e.g. 403) and we must not show a previous summary as if it were this request's result.
      setIsStreaming(false);
      setIsLoading(false);
      setStreamingText('');
      if (!receivedDone) {
        setSelectedHistoryItem(null);
        setResponse(null);
        setError((prev) => prev || 'Stream ended without completing. The request may have failed.');
      } else {
        await loadHistory();
        try {
          const historyResponse = await axios.get('/api/v1/history?limit=1');
          if (historyResponse.data && historyResponse.data.length > 0) {
            setSelectedHistoryItem(historyResponse.data[0]);
          }
        } catch (err) {
          console.error('Failed to load summary from history:', err);
        }
      }
    } catch (err: any) {
      setIsStreaming(false);
      setIsLoading(false);
      const message =
        err?.message ||
        'Unexpected error while streaming summary.';
      setError(message);
    }
  };

  const handleExportPdf = async (id: number) => {
    try {
      const headers: Record<string, string> = {};
      if (token) headers['Authorization'] = `Bearer ${token}`;
      const response = await fetch(`${API_BASE}/api/v1/history/${id}/export/pdf`, { headers });
      if (!response.ok) throw new Error('Export failed');
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `summary_${id}.pdf`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (err) {
      console.error('Failed to export PDF:', err);
      setError('Failed to export PDF');
    }
  };

  const handleExportMarkdown = async (id: number) => {
    try {
      const headers: Record<string, string> = {};
      if (token) headers['Authorization'] = `Bearer ${token}`;
      const response = await fetch(`${API_BASE}/api/v1/history/${id}/export/markdown`, { headers });
      if (!response.ok) throw new Error('Export failed');
      const text = await response.text();
      const blob = new Blob([text], { type: 'text/markdown' });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `summary_${id}.md`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (err) {
      console.error('Failed to export Markdown:', err);
      setError('Failed to export Markdown');
    }
  };

  const handleDeleteHistory = async (id: number) => {
    try {
      await axios.delete(`/api/v1/history/${id}`);
      setHistory(history.filter(item => item.id !== id));
      if (selectedHistoryItem?.id === id) {
        setSelectedHistoryItem(null);
      }
    } catch (err) {
      console.error('Failed to delete history item:', err);
    }
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  const handleCopySummary = async () => {
    const textToCopy = isStreaming 
      ? streamingText 
      : (response || selectedHistoryItem)?.summary || '';
    
    if (!textToCopy) return;

    try {
      await navigator.clipboard.writeText(textToCopy);
      setCopySuccess(true);
      setTimeout(() => setCopySuccess(false), 2000);
    } catch (err) {
      console.error('Failed to copy:', err);
      // Fallback for older browsers
      const textArea = document.createElement('textarea');
      textArea.value = textToCopy;
      textArea.style.position = 'fixed';
      textArea.style.opacity = '0';
      document.body.appendChild(textArea);
      textArea.select();
      try {
        document.execCommand('copy');
        setCopySuccess(true);
        setTimeout(() => setCopySuccess(false), 2000);
      } catch (fallbackErr) {
        console.error('Fallback copy failed:', fallbackErr);
      }
      document.body.removeChild(textArea);
    }
  };

  return (
    <div className="app-root">
      <div className="organic-blob organic-blob-1"></div>
      <div className="organic-blob organic-blob-2"></div>
      <div className="organic-blob organic-blob-3"></div>
      
      <header className="app-header">
        <div className="header-left">
          <div className="logo">AI Article Summarizer</div>
          <div className="header-tagline">Transform long articles into concise summaries with AI-powered intelligence</div>
        </div>
        <div className="header-actions">
          <span className="header-user">{user?.username}</span>
          {user?.role === 'ADMIN' && (
            <Link to="/admin" className="btn btn-secondary header-btn">Admin</Link>
          )}
          <button type="button" className="btn btn-secondary header-btn" onClick={logout}>
            Log out
          </button>
        </div>
      </header>

      <main className="app-main">
        <section className="card">
          <div className="tab-group">
            <button
              type="button"
              className={`tab ${!showHistory ? 'tab-active' : ''}`}
              onClick={() => setShowHistory(false)}
            >
              Summarize
            </button>
            <button
              type="button"
              className={`tab ${showHistory ? 'tab-active' : ''}`}
              onClick={() => {
                setShowHistory(true);
                loadHistory();
              }}
            >
              History ({history.length})
            </button>
          </div>

          {!showHistory ? (
            <form onSubmit={handleSubmit} className="form">
              {/* Hero Input Section */}
              <div className="hero-input-section">
                <div className="field">
                  <span className="field-label">Input method</span>
                  <div className="pill-group">
                    <button
                      type="button"
                      className={`pill ${inputMode === 'text' ? 'pill-active' : ''}`}
                      onClick={() => {
                        setInputMode('text');
                        setUrl('');
                      }}
                    >
                      Text
                    </button>
                    <button
                      type="button"
                      className={`pill ${inputMode === 'url' ? 'pill-active' : ''}`}
                      onClick={() => {
                        setInputMode('url');
                        setContent('');
                      }}
                    >
                      URL
                    </button>
                  </div>
                </div>

                {inputMode === 'url' ? (
                  <label className="field hero-input-field">
                    <span className="field-label">Article URL</span>
                    <input
                      type="url"
                      className="input hero-input"
                      placeholder="https://example.com/article"
                      value={url}
                      onChange={(e) => setUrl(e.target.value)}
                      autoFocus
                    />
                  </label>
                ) : (
                  <label className="field hero-input-field">
                    <span className="field-label">Article content</span>
                    <textarea
                      className="textarea hero-textarea"
                      placeholder="Paste article text here..."
                      value={content}
                      onChange={(e) => setContent(e.target.value)}
                      rows={14}
                      autoFocus
                    />
                  </label>
                )}
              </div>

              {/* Bento Grid for Controls */}
              <div className="bento-grid">
                <div className="bento-item-group">
                  <div className="field-label" style={{ marginBottom: '0.5rem', fontSize: '0.85rem' }}>Summary length</div>
                  <div className="bento-item-group-inner">
                    {(['short', 'medium', 'long'] as TargetLength[]).map((option) => (
                      <button
                        key={option}
                        type="button"
                        className={`bento-item ${targetLength === option ? 'bento-item-active' : ''}`}
                        onClick={() => setTargetLength(option)}
                      >
                        {option.charAt(0).toUpperCase() + option.slice(1)}
                      </button>
                    ))}
                  </div>
                </div>
                <div className="bento-item-group">
                  <div className="field-label" style={{ marginBottom: '0.5rem', fontSize: '0.85rem' }}>Streaming</div>
                  <div className="bento-item-group-inner">
                    <button
                      type="button"
                      className={`bento-item ${!useStreaming ? 'bento-item-active' : ''}`}
                      onClick={() => setUseStreaming(false)}
                    >
                      Off
                    </button>
                    <button
                      type="button"
                      className={`bento-item ${useStreaming ? 'bento-item-active' : ''}`}
                      onClick={() => setUseStreaming(true)}
                    >
                      On
                    </button>
                  </div>
                </div>
              </div>

              <button
                type="submit"
                className="primary-button"
                disabled={!canSubmit || isStreaming}
              >
                {isLoading || isStreaming ? 'Summarizing…' : 'Generate summary'}
              </button>

              {error && <div className="alert alert-error">{error}</div>}
            </form>
          ) : (
            <div className="history-container">
              {history.length === 0 ? (
                <div className="empty-state">No summaries yet. Create your first summary!</div>
              ) : (
                <div className="history-list">
                  {history.map((item) => (
                    <div
                      key={item.id}
                      className={`history-item ${selectedHistoryItem?.id === item.id ? 'history-item-selected' : ''}`}
                      onClick={() => setSelectedHistoryItem(item)}
                    >
                      <div className="history-item-header">
                        <div className="history-item-title">
                          {item.articleTitle || item.sourceUrl || 'Untitled Article'}
                        </div>
                        <button
                          className="delete-button"
                          onClick={(e) => {
                            e.stopPropagation();
                            handleDeleteHistory(item.id);
                          }}
                          title="Delete"
                        >
                          ×
                        </button>
                      </div>
                      {item.preview && (
                        <div className="history-item-preview">{item.preview}</div>
                      )}
                      <div className="history-item-meta">
                        <span className="meta-pill">{item.targetLength || 'medium'}</span>
                        <span className="meta-pill">{item.latencyMs}ms</span>
                        <span className="meta-pill">{formatDate(item.createdAt)}</span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </section>

        {(response || selectedHistoryItem || (isStreaming && streamingText)) && (
          <section className="card card-secondary">
            <div className="summary-header">
              <h2 className="summary-title">Summary</h2>
              <div className="summary-meta">
                {(response || selectedHistoryItem) && !isStreaming && (
                  <>
                    <span className="meta-pill">{(response || selectedHistoryItem)?.model}</span>
                    <span className="meta-pill">{(response || selectedHistoryItem)?.latencyMs} ms</span>
                  </>
                )}
                {isStreaming && (
                  <span className="meta-pill streaming-indicator">
                    <span className="streaming-dot"></span>
                    Generating...
                  </span>
                )}
                {(response || selectedHistoryItem || (isStreaming && streamingText)) && (
                  <div className="export-buttons">
                    <button
                      className={`copy-button ${copySuccess ? 'copy-button-success' : ''}`}
                      onClick={handleCopySummary}
                      title={copySuccess ? 'Copied!' : 'Copy summary'}
                    >
                      {copySuccess ? '✓ Copied' : '📋 Copy'}
                    </button>
                    {(response || selectedHistoryItem) && !isStreaming && (response?.id || selectedHistoryItem?.id) && (
                      <>
                        <button
                          className="export-button"
                          onClick={() => handleExportPdf((response?.id || selectedHistoryItem?.id)!)}
                          title="Export as PDF"
                        >
                          📄 PDF
                        </button>
                        <button
                          className="export-button"
                          onClick={() => handleExportMarkdown((response?.id || selectedHistoryItem?.id)!)}
                          title="Export as Markdown"
                        >
                          📝 MD
                        </button>
                      </>
                    )}
                  </div>
                )}
              </div>
            </div>
            {((response || selectedHistoryItem)?.articleTitle || (isStreaming && inputMode === 'url' && url)) && (
              <div className="summary-source">
                <strong>Source:</strong>{' '}
                {isStreaming && inputMode === 'url' ? (
                  <a
                    href={url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="source-link"
                  >
                    {url}
                  </a>
                ) : (response || selectedHistoryItem)?.sourceUrl ? (
                  <a
                    href={(response || selectedHistoryItem)?.sourceUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="source-link"
                  >
                    {(response || selectedHistoryItem)?.articleTitle}
                  </a>
                ) : (
                  (response || selectedHistoryItem)?.articleTitle
                )}
              </div>
            )}
            <div className="summary-body-container" ref={summaryContainerRef}>
              <p
                key={isStreaming ? streamingText.length : (response || selectedHistoryItem)?.id ?? 'summary'}
                className={`summary-body ${isStreaming ? 'summary-body-streaming' : ''}`}
                ref={summaryBodyRef}
              >
                {isStreaming ? streamingText : (response || selectedHistoryItem)?.summary}
                {isStreaming && <span className="streaming-cursor">▋</span>}
              </p>
            </div>
            {(response || selectedHistoryItem)?.createdAt && !isStreaming && (
              <div className="summary-footer">
                Created: {formatDate((response || selectedHistoryItem)!.createdAt!)}
              </div>
            )}
          </section>
        )}
      </main>

      <footer className="app-footer">
        <span>Built with Spring Boot, Spring AI & React.</span>
      </footer>
    </div>
  );
}

export const App: React.FC = () => (
  <Routes>
    <Route path="/login" element={<Login />} />
    <Route path="/register" element={<Register />} />
    <Route path="/admin" element={<AdminRoute><Admin /></AdminRoute>} />
    <Route path="/*" element={<ProtectedRoute><SummarizerApp /></ProtectedRoute>} />
  </Routes>
);
