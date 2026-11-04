import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { createReservation, getProducts } from "../api";
import { useCart } from "../cart";
import {
  addDaysIso,
  formatDate,
  formatPrice,
  isValidEmail,
  isValidPhone,
  normalizePhone,
  plural,
  todayIso,
} from "../utils";

export default function Cart() {
  const cart = useCart();
  const today = todayIso();

  const [form, setForm] = useState({
    startAt: today,
    endAt: addDaysIso(today, 3),
    fullName: "",
    phone: "",
    email: "",
  });
  const [errors, setErrors] = useState({});
  const [serverError, setServerError] = useState("");
  const [notice, setNotice] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [done, setDone] = useState(null); // данные оформленной брони
  const [copied, setCopied] = useState(false);

  // При открытии сверяем корзину с каталогом: цены и остатки могли измениться
  useEffect(() => {
    let cancelled = false;
    getProducts()
      .then((products) => {
        if (cancelled) return;
        const removed = cart.syncStock(products);
        if (removed.length) {
          setNotice(`Убрали из корзины, так как закончилось: ${removed.join(", ")}.`);
        }
      })
      .catch(() => {
        /* нет связи - работаем с тем, что в корзине */
      });
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

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
    if (Object.keys(er).length || cart.items.length === 0) return;

    setSubmitting(true);
    try {
      const num = await createReservation({
        startAt: form.startAt,
        endAt: form.endAt,
        skuAndCount: cart.items.map((it) => ({ sku: it.sku, count: it.count })),
        customer: {
          fullName: form.fullName.trim(),
          phone: normalizePhone(form.phone),
          email: form.email.trim(),
        },
      });
      setDone({
        num: String(num).trim().replace(/^"|"$/g, ""),
        items: cart.items,
        total: cart.totalPrice,
        startAt: form.startAt,
        endAt: form.endAt,
        fullName: form.fullName.trim(),
      });
      cart.clear();
    } catch (err) {
      setServerError(err.message || "Не удалось создать бронь.");
      // остатки могли поменяться - обновим корзину
      getProducts().then(cart.syncStock).catch(() => {});
    } finally {
      setSubmitting(false);
    }
  }

  async function copyNumber() {
    try {
      await navigator.clipboard.writeText(done.num);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      /* номер можно выделить вручную */
    }
  }

  const fieldError = (name) => errors[name] && <div className="field-error">{errors[name]}</div>;
  const cls = (name) => (errors[name] ? "invalid" : "");

  // ---------------- бронь оформлена ----------------
  if (done) {
    return (
      <div className="container page page-narrow">
        <div className="card stack" style={{ marginTop: 24 }}>
          <h1>Бронь оформлена</h1>
          <div className="alert alert-success" style={{ margin: 0 }}>
            Сохраните номер брони: он нужен, чтобы посмотреть бронь и забрать шубы.
          </div>
          <div style={{ textAlign: "center" }}>
            <div className="muted">Номер брони</div>
            <div style={{ fontSize: "1.6rem", fontWeight: 700, letterSpacing: 2, userSelect: "all" }}>{done.num}</div>
            <button type="button" className="btn btn-secondary btn-sm" onClick={copyNumber} style={{ marginTop: 8 }}>
              {copied ? "Скопировано ✓" : "Скопировать"}
            </button>
          </div>
          <dl className="props">
            <dt>Период</dt><dd>{formatDate(done.startAt)} — {formatDate(done.endAt)}</dd>
            <dt>На имя</dt><dd>{done.fullName}</dd>
            <dt>Товары</dt>
            <dd>{done.items.map((it) => `${it.title} × ${it.count}`).join(", ")}</dd>
            <dt>Сумма</dt><dd>{formatPrice(done.total)}</dd>
          </dl>
          <div className="actions">
            <Link to={`/reservations/search?num=${encodeURIComponent(done.num)}`} className="btn btn-primary">
              Посмотреть бронь
            </Link>
            <Link to="/products/catalog" className="btn btn-ghost">В каталог</Link>
          </div>
        </div>
      </div>
    );
  }

  // ---------------- пустая корзина ----------------
  if (cart.items.length === 0) {
    return (
      <div className="container page">
        <h1>Корзина</h1>
        {notice && <div className="alert alert-warn">{notice}</div>}
        <div className="empty-state">
          <h2>Корзина пуста</h2>
          <p>Добавьте шубы из каталога, чтобы забронировать их одной бронью.</p>
          <Link to="/products/catalog" className="btn btn-primary">Перейти в каталог</Link>
        </div>
      </div>
    );
  }

  // ---------------- корзина и форма ----------------
  return (
    <div className="container page page-wide">
      <nav className="breadcrumbs">Главная / Корзина</nav>
      <h1>Корзина</h1>

      {notice && <div className="alert alert-warn" role="status">{notice}</div>}

      <section className="card" aria-label="Выбранные шубы">
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Товар</th>
                <th>Цена</th>
                <th>Количество</th>
                <th>Сумма</th>
                <th aria-label="Удалить" />
              </tr>
            </thead>
            <tbody>
              {cart.items.map((it) => (
                <tr key={it.sku}>
                  <td>
                    <div style={{ display: "flex", gap: 10, alignItems: "center" }}>
                      {it.imageUrl ? (
                        <img src={it.imageUrl} alt="" style={{ width: 48, height: 48, objectFit: "cover", borderRadius: 8 }} />
                      ) : (
                        <span aria-hidden="true" style={{ fontSize: "1.8rem" }}>🧥</span>
                      )}
                      <div>
                        <div style={{ fontWeight: 600 }}>{it.title}</div>
                        <div className="muted" style={{ fontSize: "0.82rem" }}>{it.details}</div>
                      </div>
                    </div>
                  </td>
                  <td>{formatPrice(it.price)}</td>
                  <td>
                    <div style={{ display: "flex", gap: 4, alignItems: "center" }}>
                      <button
                        type="button" className="btn btn-secondary btn-sm" aria-label="Меньше"
                        onClick={() => cart.setCount(it.sku, it.count - 1)} disabled={it.count <= 1}
                      >
                        −
                      </button>
                      <input
                        type="number" min="1" max={it.max} value={it.count} aria-label={`Количество: ${it.title}`}
                        onChange={(e) => cart.setCount(it.sku, e.target.value)} style={{ width: 64, textAlign: "center" }}
                      />
                      <button
                        type="button" className="btn btn-secondary btn-sm" aria-label="Больше"
                        onClick={() => cart.setCount(it.sku, it.count + 1)} disabled={it.count >= it.max}
                      >
                        +
                      </button>
                    </div>
                    <div className="hint">свободно {it.max} шт.</div>
                  </td>
                  <td>{formatPrice(it.price * it.count)}</td>
                  <td>
                    <button type="button" className="btn btn-ghost btn-sm" onClick={() => cart.remove(it.sku)}>
                      Убрать
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr>
                <td colSpan={3}>
                  <strong>
                    Итого: {cart.totalCount} {plural(cart.totalCount, ["шуба", "шубы", "шуб"])}
                  </strong>
                </td>
                <td colSpan={2}><strong>{formatPrice(cart.totalPrice)}</strong></td>
              </tr>
            </tfoot>
          </table>
        </div>
        <div className="actions" style={{ marginTop: 12 }}>
          <Link to="/products/catalog" className="btn btn-secondary">Добавить ещё</Link>
          <button type="button" className="btn btn-ghost" onClick={cart.clear}>Очистить корзину</button>
        </div>
      </section>

      <form className="card" onSubmit={handleSubmit} noValidate style={{ marginTop: 16 }}>
        <h2>Данные для брони</h2>

        <div className="row">
          <div className="form-field">
            <label htmlFor="c-start">Бронь с</label>
            <input id="c-start" type="date" min={today} value={form.startAt} onChange={set("startAt")} className={cls("startAt")} />
            {fieldError("startAt")}
          </div>
          <div className="form-field">
            <label htmlFor="c-end">Бронь до</label>
            <input id="c-end" type="date" min={form.startAt || today} value={form.endAt} onChange={set("endAt")} className={cls("endAt")} />
            {fieldError("endAt")}
          </div>
        </div>

        <div className="form-field">
          <label htmlFor="c-name">ФИО</label>
          <input id="c-name" type="text" autoComplete="name" placeholder="Иванов Иван Иванович" value={form.fullName} onChange={set("fullName")} className={cls("fullName")} />
          {fieldError("fullName")}
        </div>

        <div className="row">
          <div className="form-field">
            <label htmlFor="c-phone">Телефон</label>
            <input id="c-phone" type="tel" autoComplete="tel" placeholder="+7 900 123-45-67" value={form.phone} onChange={set("phone")} className={cls("phone")} />
            {fieldError("phone")}
          </div>
          <div className="form-field">
            <label htmlFor="c-email">Email</label>
            <input id="c-email" type="email" autoComplete="email" placeholder="ivan@example.com" value={form.email} onChange={set("email")} className={cls("email")} />
            {fieldError("email")}
          </div>
        </div>

        {serverError && <div className="alert alert-error" role="alert">{serverError}</div>}

        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? "Бронируем…" : `Забронировать на ${formatPrice(cart.totalPrice)}`}
        </button>
      </form>
    </div>
  );
}
