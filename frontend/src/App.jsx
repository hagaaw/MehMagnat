import { Link, NavLink, Navigate, Route, Routes, useNavigate } from "react-router-dom";
import { RequireAdmin, RequireStaff, useAuth } from "./auth";
import Catalog from "./pages/Catalog";
import Login from "./pages/Login";
import ReservationSearch from "./pages/ReservationSearch";
import ProductForm from "./pages/ProductForm";
import Employees from "./pages/Employees";

const navClass = ({ isActive }) => "nav-link" + (isActive ? " active" : "");

function Header() {
  const { user, isStaff, isAdmin, signOut } = useAuth();
  const navigate = useNavigate();

  async function handleSignOut() {
    await signOut();
    navigate("/products/catalog");
  }

  return (
    <header className="site-header">
      <div className="container header-inner">
        <Link to="/products/catalog" className="brand">
          <span className="brand-icon" aria-hidden="true">🧥</span>
          MehMagnat
        </Link>

        <nav className="main-nav" aria-label="Основное меню">
          <NavLink to="/products/catalog" className={navClass}>Каталог</NavLink>
          {isStaff && (
            <>
              <NavLink to="/reservations/search" className={navClass}>Поиск брони</NavLink>
              <NavLink to="/products/new" className={navClass}>Добавить товар</NavLink>
            </>
          )}
          {isStaff && isAdmin && (
            <NavLink to="/employees" className={navClass}>Сотрудники</NavLink>
          )}
        </nav>

        <div className="header-user">
          {isStaff ? (
            <>
              <span className="user-name">
                {user.fullName}
                {user.role && <span className="role-badge">{user.role === "ADMIN" ? "админ" : "продавец"}</span>}
              </span>
              <button type="button" className="btn btn-ghost" onClick={handleSignOut}>
                Выйти
              </button>
            </>
          ) : (
            <Link to="/signin" className="btn btn-ghost">Вход для сотрудников</Link>
          )}
        </div>
      </div>
    </header>
  );
}

function NotFound() {
  return (
    <div className="container page">
      <h1>Страница не найдена</h1>
      <p>
        <Link to="/products/catalog">Перейти в каталог</Link>
      </p>
    </div>
  );
}

export default function App() {
  return (
    <>
      <Header />
      <main>
        <Routes>
          <Route path="/" element={<Navigate to="/products/catalog" replace />} />

          {/* Открыто для всех */}
          <Route path="/products/catalog" element={<Catalog />} />
          <Route path="/signin" element={<Login />} />

          {/* Только сотрудники */}
          <Route
            path="/reservations/search"
            element={<RequireStaff><ReservationSearch /></RequireStaff>}
          />
          <Route
            path="/products/new"
            element={<RequireStaff><ProductForm /></RequireStaff>}
          />
          <Route
            path="/products/:id"
            element={<RequireStaff><ProductForm /></RequireStaff>}
          />

          {/* Только администратор */}
          <Route
            path="/employees"
            element={<RequireAdmin><Employees /></RequireAdmin>}
          />

          <Route path="*" element={<NotFound />} />
        </Routes>
      </main>
    </>
  );
}
