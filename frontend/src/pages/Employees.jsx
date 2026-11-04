import { useCallback, useEffect, useState } from "react";
import {
  changeEmployeeRole,
  deleteEmployee,
  getEmployees,
  registerEmployee,
  setEmployeeActive,
} from "../api";
import { useAuth } from "../auth";
import { formatDate, isValidEmail, isValidPhone, normalizePhone } from "../utils";

const ROLE_LABEL = { ADMIN: "Администратор", SALER: "Продавец" };

const EMPTY = {
  fullName: "",
  phone: "",
  email: "",
  role: "SALER",
  isActive: true,
  password: "",
  password2: "",
};

// ------------------------------------------------------------------ форма добавления

function NewEmployeeForm({ onCreated, onCancel }) {
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [serverError, setServerError] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [saving, setSaving] = useState(false);

  const set = (field) => (e) => {
    const value = e.target.type === "checkbox" ? e.target.checked : e.target.value;
    setForm((f) => ({ ...f, [field]: value }));
    setErrors((er) => ({ ...er, [field]: undefined }));
  };

  function validate() {
    const er = {};
    if (form.fullName.trim().length < 3) er.fullName = "Введите ФИО.";
    if (!isValidPhone(form.phone)) er.phone = "Введите телефон, например +7 900 123-45-67.";
    if (!isValidEmail(form.email)) er.email = "Введите корректный email.";
    if (form.password.length < 6) er.password = "Минимум 6 символов.";
    if (form.password2 !== form.password) er.password2 = "Пароли не совпадают.";
    return er;
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setServerError("");
    const er = validate();
    setErrors(er);
    if (Object.keys(er).length) return;

    setSaving(true);
    try {
      await registerEmployee({
        fullName: form.fullName.trim(),
        phone: normalizePhone(form.phone),
        email: form.email.trim(),
        role: form.role,
        isActive: form.isActive,
        password: form.password,
      });
      onCreated(form.fullName.trim());
    } catch (err) {
      setServerError(err.message || "Не удалось зарегистрировать сотрудника.");
    } finally {
      setSaving(false);
    }
  }

  const fieldError = (name) => errors[name] && <div className="field-error">{errors[name]}</div>;
  const cls = (name) => (errors[name] ? "invalid" : "");

  return (
    <form className="card" onSubmit={handleSubmit} noValidate style={{ marginBottom: 16 }}>
      <h2>Новый сотрудник</h2>

      <div className="form-field">
        <label htmlFor="em-name">ФИО</label>
        <input id="em-name" type="text" autoComplete="off" value={form.fullName} onChange={set("fullName")} className={cls("fullName")} />
        {fieldError("fullName")}
      </div>

      <div className="row">
        <div className="form-field">
          <label htmlFor="em-phone">Телефон (он же логин)</label>
          <input id="em-phone" type="tel" autoComplete="off" placeholder="+7 900 123-45-67" value={form.phone} onChange={set("phone")} className={cls("phone")} />
          {fieldError("phone")}
        </div>
        <div className="form-field">
          <label htmlFor="em-email">Email</label>
          <input id="em-email" type="email" autoComplete="off" value={form.email} onChange={set("email")} className={cls("email")} />
          {fieldError("email")}
        </div>
      </div>

      <div className="form-field" style={{ maxWidth: 260 }}>
        <label htmlFor="em-role">Роль</label>
        <select id="em-role" value={form.role} onChange={set("role")}>
          <option value="SALER">Продавец</option>
          <option value="ADMIN">Администратор</option>
        </select>
      </div>

      <div className="row">
        <div className="form-field">
          <label htmlFor="em-pass">Пароль</label>
          <input id="em-pass" type={showPassword ? "text" : "password"} autoComplete="new-password" value={form.password} onChange={set("password")} className={cls("password")} />
          {fieldError("password")}
        </div>
        <div className="form-field">
          <label htmlFor="em-pass2">Повторите пароль</label>
          <input id="em-pass2" type={showPassword ? "text" : "password"} autoComplete="new-password" value={form.password2} onChange={set("password2")} className={cls("password2")} />
          {fieldError("password2")}
        </div>
      </div>

      <label className="checkbox-row">
        <input type="checkbox" checked={showPassword} onChange={(e) => setShowPassword(e.target.checked)} />
        Показать пароль
      </label>
      <label className="checkbox-row">
        <input type="checkbox" checked={form.isActive} onChange={set("isActive")} />
        Учётная запись активна
      </label>

      {serverError && <div className="alert alert-error" role="alert" style={{ marginTop: 12 }}>{serverError}</div>}

      <div className="actions" style={{ marginTop: 8 }}>
        <button type="submit" className="btn btn-primary" disabled={saving}>
          {saving ? "Сохраняем…" : "Зарегистрировать"}
        </button>
        <button type="button" className="btn btn-ghost" onClick={onCancel} disabled={saving}>
          Отмена
        </button>
      </div>
    </form>
  );
}

// ------------------------------------------------------------------ страница

// Только для ADMIN (защита в App.jsx через RequireAdmin).
export default function Employees() {
  const { user } = useAuth();
  const [employees, setEmployees] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [busyPhone, setBusyPhone] = useState("");
  const [showForm, setShowForm] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setEmployees(await getEmployees());
    } catch (e) {
      setError(e.message || "Не удалось загрузить сотрудников.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  // Выполняет действие над сотрудником и обновляет список
  async function act(phone, action, okMessage) {
    setBusyPhone(phone);
    setError("");
    setNotice("");
    try {
      await action();
      setNotice(okMessage);
      setEmployees(await getEmployees());
    } catch (e) {
      setError(e.message || "Не удалось выполнить действие.");
    } finally {
      setBusyPhone("");
    }
  }

  const changeRole = (emp, role) => {
    if (role === emp.role) return;
    if (!window.confirm(`Сменить роль сотрудника ${emp.fullName} на «${ROLE_LABEL[role]}»?`)) return;
    act(emp.phone, () => changeEmployeeRole(emp.phone, role), `Роль изменена: ${emp.fullName} - ${ROLE_LABEL[role]}.`);
  };

  const toggleActive = (emp) => {
    const next = !emp.isActive;
    if (!next && !window.confirm(`Отключить учётную запись ${emp.fullName}? Войти он больше не сможет.`)) return;
    act(
      emp.phone,
      () => setEmployeeActive(emp.phone, next),
      next ? `${emp.fullName}: учётная запись включена.` : `${emp.fullName}: учётная запись отключена.`
    );
  };

  const remove = (emp) => {
    if (!window.confirm(`Удалить сотрудника ${emp.fullName}? Это действие нельзя отменить.`)) return;
    act(emp.phone, () => deleteEmployee(emp.phone), `Сотрудник ${emp.fullName} удалён.`);
  };

  async function handleCreated(name) {
    setShowForm(false);
    setNotice(`Сотрудник добавлен: ${name}.`);
    try {
      setEmployees(await getEmployees());
    } catch {
      /* список обновится при следующей загрузке */
    }
  }

  return (
    <div className="container page">
      <nav className="breadcrumbs">Главная / Администрирование / Сотрудники</nav>
      <div className="panel-head">
        <h1>Сотрудники</h1>
        {!showForm && (
          <button type="button" className="btn btn-primary" onClick={() => setShowForm(true)}>
            Добавить сотрудника
          </button>
        )}
      </div>

      {showForm && <NewEmployeeForm onCreated={handleCreated} onCancel={() => setShowForm(false)} />}

      {notice && <div className="alert alert-success" role="status">{notice}</div>}
      {error && (
        <div className="alert alert-error" role="alert">
          {error}{" "}
          <button type="button" className="btn btn-sm btn-secondary" onClick={load}>Обновить</button>
        </div>
      )}

      <div className="card">
        {loading ? (
          <div className="page-loading">Загрузка…</div>
        ) : employees.length === 0 ? (
          <div className="empty-state">Сотрудников пока нет.</div>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>ФИО</th>
                  <th>Телефон</th>
                  <th>Email</th>
                  <th>Роль</th>
                  <th>Статус</th>
                  <th>Добавлен</th>
                  <th>Действия</th>
                </tr>
              </thead>
              <tbody>
                {employees.map((emp) => {
                  const isSelf = user?.id != null && user.id === emp.id;
                  const busy = busyPhone === emp.phone;
                  return (
                    <tr key={emp.id}>
                      <td>
                        {emp.fullName}
                        {isSelf && <span className="role-badge" style={{ marginLeft: 6 }}>это вы</span>}
                      </td>
                      <td>{emp.phone}</td>
                      <td>{emp.email}</td>
                      <td>
                        <select
                          value={emp.role}
                          onChange={(e) => changeRole(emp, e.target.value)}
                          disabled={isSelf || busy}
                          aria-label={`Роль: ${emp.fullName}`}
                          style={{ width: "auto" }}
                        >
                          <option value="SALER">{ROLE_LABEL.SALER}</option>
                          <option value="ADMIN">{ROLE_LABEL.ADMIN}</option>
                        </select>
                      </td>
                      <td>
                        <span className={"badge " + (emp.isActive ? "badge-free" : "badge-off")}>
                          {emp.isActive ? "Активен" : "Отключён"}
                        </span>
                      </td>
                      <td>{formatDate(emp.createdAt)}</td>
                      <td>
                        <div className="actions">
                          <button
                            type="button" className="btn btn-secondary btn-sm"
                            onClick={() => toggleActive(emp)} disabled={isSelf || busy}
                          >
                            {emp.isActive ? "Отключить" : "Включить"}
                          </button>
                          <button
                            type="button" className="btn btn-danger btn-sm"
                            onClick={() => remove(emp)} disabled={isSelf || busy}
                          >
                            Удалить
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
