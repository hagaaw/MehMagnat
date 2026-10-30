import { useState } from "react";
import { registerEmployee } from "../api";
import { isValidEmail, isValidPhone, normalizePhone } from "../utils";

const EMPTY = {
  fullName: "",
  phone: "",
  email: "",
  role: "SALER",
  isActive: true,
  password: "",
  password2: "",
};

// Страница только для ADMIN (защита в App.jsx через RequireAdmin).
export default function Employees() {
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [serverError, setServerError] = useState("");
  const [created, setCreated] = useState("");
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
    setCreated("");
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
      setCreated(`${form.fullName.trim()} (${normalizePhone(form.phone)})`);
      setForm(EMPTY);
    } catch (err) {
      setServerError(err.message || "Не удалось зарегистрировать сотрудника.");
    } finally {
      setSaving(false);
    }
  }

  const fieldError = (name) => errors[name] && <div className="field-error">{errors[name]}</div>;
  const cls = (name) => (errors[name] ? "invalid" : "");

  return (
    <div className="container page page-narrow">
      <nav className="breadcrumbs">Главная / Администрирование / Сотрудники</nav>
      <h1>Новый сотрудник</h1>

      <form className="card" onSubmit={handleSubmit} noValidate>
        <div className="form-field">
          <label htmlFor="em-name">ФИО</label>
          <input id="em-name" type="text" autoComplete="off" value={form.fullName} onChange={set("fullName")} className={cls("fullName")} />
          {fieldError("fullName")}
        </div>

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

        <div className="form-field">
          <label htmlFor="em-role">Роль</label>
          <select id="em-role" value={form.role} onChange={set("role")}>
            <option value="SALER">Продавец</option>
            <option value="ADMIN">Администратор</option>
          </select>
          {form.role === "ADMIN" && (
            <div className="hint">Администратор может регистрировать сотрудников и видит все разделы.</div>
          )}
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
        {created && <div className="alert alert-success" role="status" style={{ marginTop: 12 }}>Сотрудник добавлен: {created}.</div>}

        <button type="submit" className="btn btn-primary" disabled={saving} style={{ marginTop: 8 }}>
          {saving ? "Сохраняем…" : "Зарегистрировать"}
        </button>
      </form>
    </div>
  );
}
