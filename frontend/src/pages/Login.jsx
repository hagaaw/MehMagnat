import { useState } from "react";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../auth";
import { normalizePhone } from "../utils";

export default function Login() {
  const { isStaff, loading, signIn } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = location.state?.from || "/products/catalog";

  const [phone, setPhone] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  // Уже вошли - на логин смотреть незачем
  if (!loading && isStaff) return <Navigate to={from} replace />;

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");

    if (!phone.trim() || !password) {
      setError("Введите телефон и пароль.");
      return;
    }

    setSubmitting(true);
    try {
      await signIn(normalizePhone(phone), password);
      navigate(from, { replace: true });
    } catch (err) {
      setError(err.message || "Не удалось войти.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="container page page-narrow">
      <div className="card auth-card">
        <h1>Вход для сотрудников</h1>
        <p className="muted">Покупателям входить не нужно: каталог и бронирование доступны всем.</p>

        <form onSubmit={handleSubmit} noValidate>
          <div className="form-field">
            <label htmlFor="phone">Телефон</label>
            <input
              id="phone"
              type="tel"
              inputMode="tel"
              autoComplete="username"
              placeholder="+7 900 123-45-67"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              autoFocus
            />
          </div>

          <div className="form-field">
            <label htmlFor="password">Пароль</label>
            <input
              id="password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>

          {error && (
            <div className="alert alert-error" role="alert">
              {error}
            </div>
          )}

          <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
            {submitting ? "Входим…" : "Войти"}
          </button>
        </form>
      </div>
    </div>
  );
}
