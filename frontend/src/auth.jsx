import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { getMe, login, logout, setUnauthorizedHandler } from "./api";

// Контекст сессии сотрудника.
//   user    - { id, fullName, role } или null (покупатель / не вошёл)
//   loading - идёт первичная проверка сессии
//   isStaff - сотрудник вошёл
//   isAdmin - роль ADMIN. Пока бэкенд не отдаёт /api/me, роль неизвестна (null):
//             в этом случае считаем, что админ-разделы показывать можно,
//             а если прав на деле нет - бэкенд вернёт 403 и мы покажем ошибку.
const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // Первичная проверка: есть ли уже живая сессия (cookie JSESSIONID)
  useEffect(() => {
    let cancelled = false;
    getMe()
      .then((me) => !cancelled && setUser(me))
      .catch(() => !cancelled && setUser(null))
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true;
    };
  }, []);

  // Любой ответ 401 из api.js = сессия кончилась
  useEffect(() => {
    setUnauthorizedHandler(() => setUser(null));
    return () => setUnauthorizedHandler(null);
  }, []);

  const signIn = useCallback(async (phone, password) => {
    await login(phone, password); // бросит ApiError, если данные неверные
    const me = await getMe();
    if (!me) throw new Error("Не удалось войти. Попробуйте ещё раз.");
    setUser(me);
    return me;
  }, []);

  const signOut = useCallback(async () => {
    try {
      await logout();
    } finally {
      setUser(null);
    }
  }, []);

  const value = useMemo(
    () => ({
      user,
      loading,
      isStaff: !!user,
      roleKnown: !!user && !!user.role,
      isAdmin: !!user && (user.role === "ADMIN" || user.role == null),
      signIn,
      signOut,
    }),
    [user, loading, signIn, signOut]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth нужно использовать внутри <AuthProvider>");
  return ctx;
}

/** Страница только для вошедших сотрудников. Иначе - на /signin. */
export function RequireStaff({ children }) {
  const { isStaff, loading } = useAuth();
  const location = useLocation();

  if (loading) return <div className="page-loading">Загрузка…</div>;
  if (!isStaff) return <Navigate to="/signin" replace state={{ from: location.pathname }} />;
  return children;
}

/** Страница только для ADMIN. Продавца отправляем в каталог. */
export function RequireAdmin({ children }) {
  const { isStaff, isAdmin, loading } = useAuth();
  const location = useLocation();

  if (loading) return <div className="page-loading">Загрузка…</div>;
  if (!isStaff) return <Navigate to="/signin" replace state={{ from: location.pathname }} />;
  if (!isAdmin) return <Navigate to="/products/catalog" replace />;
  return children;
}
