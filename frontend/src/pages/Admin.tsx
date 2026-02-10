import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import axios from 'axios';
import { useAuth } from '../context/AuthContext';

interface UserDto {
  id: number;
  username: string;
  role: string;
  createdAt: string;
}

interface AdminStats {
  totalUsers: number;
  totalSummaries: number;
}

export function Admin() {
  const { user, logout } = useAuth();
  const [users, setUsers] = useState<UserDto[]>([]);
  const [stats, setStats] = useState<AdminStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const load = async () => {
      try {
        const [usersRes, statsRes] = await Promise.all([
          axios.get<UserDto[]>('/api/admin/users'),
          axios.get<AdminStats>('/api/admin/stats'),
        ]);
        setUsers(usersRes.data);
        setStats(statsRes.data);
      } catch (err) {
        setError('Failed to load admin data');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString();
  };

  return (
    <div className="app-root">
      <div className="organic-blob organic-blob-1"></div>
      <div className="organic-blob organic-blob-2"></div>
      <header className="app-header">
        <div className="header-left">
          <div className="logo">AI Article Summarizer</div>
          <div className="header-tagline">Admin dashboard</div>
        </div>
        <div className="header-actions">
          <span className="header-user">{user?.username}</span>
          <Link to="/" className="btn btn-secondary header-btn">App</Link>
          <button type="button" className="btn btn-secondary header-btn" onClick={logout}>
            Log out
          </button>
        </div>
      </header>
      <main className="app-main admin-main">
        <section className="card admin-card">
          <h1 className="admin-title">Admin</h1>
          {error && (
            <div className="error-banner" role="alert">
              {error}
            </div>
          )}
          {loading ? (
            <p>Loading...</p>
          ) : (
            <>
              {stats && (
                <div className="admin-stats">
                  <div className="admin-stat">
                    <span className="admin-stat-value">{stats.totalUsers}</span>
                    <span className="admin-stat-label">Users</span>
                  </div>
                  <div className="admin-stat">
                    <span className="admin-stat-value">{stats.totalSummaries}</span>
                    <span className="admin-stat-label">Summaries</span>
                  </div>
                </div>
              )}
              <h2 className="admin-subtitle">Users</h2>
              <div className="admin-table-wrap">
                <table className="admin-table">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Username</th>
                      <th>Role</th>
                      <th>Created</th>
                    </tr>
                  </thead>
                  <tbody>
                    {users.map((u) => (
                      <tr key={u.id}>
                        <td>{u.id}</td>
                        <td>{u.username}</td>
                        <td><span className={`role-badge role-${u.role.toLowerCase()}`}>{u.role}</span></td>
                        <td>{formatDate(u.createdAt)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          )}
        </section>
      </main>
    </div>
  );
}
