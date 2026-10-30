import { useEffect, useState } from "react";
import { createReservation } from "../api";
import { addDaysIso, formatDate, formatPrice, isValidEmail, isValidPhone, normalizePhone, todayIso } from "../utils";

/**
 * Окно бронирования одного товара (доступно покупателям без входа).
 * props:
 *   product    - товар из каталога
 *   onClose    - закрыть окно
 *   onReserved - вызывается после успешной брони (каталог обновит остатки)
 */
export default function ReservationModal({ product, onClose, onReserved }) {
  const maxCount = Math.max(1, Number(product.quantityReserved) || 1);
  const today = todayIso();

  const [form, setForm] = useState({
    startAt: today,
    endAt: addDaysIso(today, 3),
    count: 1,
    fullName: "",
    phone: "",
    email: "",
  });
  const [errors, setErrors] = useState({});
  const [serverError, setServerError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [reservationNum, setReservationNum] = useState(null);
  const [copied, setCopied] = useState(false);

  // Esc закрывает, пока идёт отправка - не закрываем; прокрутку страницы блокируем
  useEffect(() => {
    const onKey = (e) => e.key === "Escape" && !submitting && onClose();
    document.addEventListener("keydown", onKey);
    const prev = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", onKey);
      document.body.style.overflow = prev;
    };
  }, [onClose, submitting]);

  const set = (field) => (e) => {
    setForm((f) => ({ ...f, [field]: e.target.value }));
    setErrors((er) => ({ ...er, [field]: undefined }));
  };

  function validate() {
    const er = {};
    if (!form.startAt) er.startAt = "Укажите дату начала.";
    else if (form.startAt < today) er.startAt = "Дата начала не может быть в прошлом.";

    if (!form.endAt) er.endAt = "Укажите дату окончания.";
    else if (form.startAt && form.endAt < form.startAt) er.endAt = "Окончание раньше начала.";

    const count = Number(form.count);
    if (!Number.isInteger(count) || count < 1) er.count = "Минимум 1 шт.";
    else if (count > maxCount) er.count = `Доступно не больше ${maxCount} шт.`;

    if (form.fullName.trim().length < 3) er.fullName = "Введите ФИО.";
    if (!isValidPhone(form.phone)) er.phone = "Введите телефон, например +7 900 123-45-67.";
    if (!isValidEmail(form.email)) er.email = "Введите корректный email.";
    return er;
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setServerError("");
    const er = validate();
    setErrors(er);
    if (Object.keys(er).length) return;

    setSubmitting(true);
    try {
      const num = await createReservation({
        startAt: form.startAt,
        endAt: form.endAt,
        skuAndCount: [{ sku: product.sku, count: Number(form.count) }],
        customer: {
          fullName: form.fullName.trim(),
          phone: normalizePhone(form.phone),
          email: form.email.trim(),
        },
      });
      setReservationNum(String(num).trim().replace(/^"|"$/g, ""));
      onReserved?.();
    } catch (err) {
      setServerError(err.message || "Не удалось создать бронь.");
    } finally {
      setSubmitting(false);
    }
  }

  async function copyNumber() {
    try {
      await navigator.clipboard.writeText(reservationNum);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      /* буфер недоступен - номер можно выделить вручную */
    }
  }

  const fieldError = (name) => errors[name] && <div className="field-error">{errors[name]}</div>;

  return (
    <div className="overlay overlay-modal" onClick={() => !submitting && onClose()}>
      <div
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-label="Бронирование"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="panel-head">
          <h2>{reservationNum ? "Бронь оформлена" : "Бронирование"}</h2>
          <button type="button" className="close-btn" onClick={onClose} disabled={submitting} aria-label="Закрыть">
            ×
          </button>
        </div>

        {reservationNum ? (
          // ---------------- успех ----------------
          <div className="stack">
            <div className="alert alert-success">Бронь создана. Сохраните её номер: он нужен для покупки.</div>
            <div className="card" style={{ textAlign: "center" }}>
              <div className="muted">Номер брони</div>
              <div style={{ fontSize: "1.6rem", fontWeight: 700, letterSpacing: 2, userSelect: "all" }}>
                {reservationNum}
              </div>
              <button type="button" className="btn btn-secondary btn-sm" onClick={copyNumber} style={{ marginTop: 8 }}>
                {copied ? "Скопировано ✓" : "Скопировать"}
              </button>
            </div>
            <dl className="props">
              <dt>Товар</dt><dd>{product.title} × {form.count}</dd>
              <dt>Период</dt><dd>{formatDate(form.startAt)} — {formatDate(form.endAt)}</dd>
              <dt>На имя</dt><dd>{form.fullName.trim()}</dd>
            </dl>
            <button type="button" className="btn btn-primary btn-block" onClick={onClose}>
              Готово
            </button>
          </div>
        ) : (
          // ---------------- форма ----------------
          <form onSubmit={handleSubmit} noValidate>
            <p className="muted">
              {product.title} · {formatPrice(product.listPrice)} · доступно {maxCount} шт.
            </p>

            <div className="row">
              <div className="form-field">
                <label htmlFor="r-start">Бронь с</label>
                <input
                  id="r-start" type="date" min={today} value={form.startAt}
                  onChange={set("startAt")} className={errors.startAt ? "invalid" : ""}
                />
                {fieldError("startAt")}
              </div>
              <div className="form-field">
                <label htmlFor="r-end">Бронь до</label>
                <input
                  id="r-end" type="date" min={form.startAt || today} value={form.endAt}
                  onChange={set("endAt")} className={errors.endAt ? "invalid" : ""}
                />
                {fieldError("endAt")}
              </div>
              <div className="form-field">
                <label htmlFor="r-count">Количество</label>
                <input
                  id="r-count" type="number" min="1" max={maxCount} value={form.count}
                  onChange={set("count")} className={errors.count ? "invalid" : ""}
                />
                {fieldError("count")}
              </div>
            </div>

            <div className="form-field">
              <label htmlFor="r-name">ФИО</label>
              <input
                id="r-name" type="text" autoComplete="name" placeholder="Иванов Иван Иванович"
                value={form.fullName} onChange={set("fullName")} className={errors.fullName ? "invalid" : ""}
              />
              {fieldError("fullName")}
            </div>

            <div className="row">
              <div className="form-field">
                <label htmlFor="r-phone">Телефон</label>
                <input
                  id="r-phone" type="tel" autoComplete="tel" placeholder="+7 900 123-45-67"
                  value={form.phone} onChange={set("phone")} className={errors.phone ? "invalid" : ""}
                />
                {fieldError("phone")}
              </div>
              <div className="form-field">
                <label htmlFor="r-email">Email</label>
                <input
                  id="r-email" type="email" autoComplete="email" placeholder="ivan@example.com"
                  value={form.email} onChange={set("email")} className={errors.email ? "invalid" : ""}
                />
                {fieldError("email")}
              </div>
            </div>

            {serverError && (
              <div className="alert alert-error" role="alert">
                {serverError}
              </div>
            )}

            <div className="actions">
              <button type="submit" className="btn btn-primary" disabled={submitting}>
                {submitting ? "Бронируем…" : "Забронировать"}
              </button>
              <button type="button" className="btn btn-ghost" onClick={onClose} disabled={submitting}>
                Отмена
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
