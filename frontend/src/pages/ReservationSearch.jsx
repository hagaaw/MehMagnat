import { useState } from "react";
import { addSale, getReservation, searchReservationsByPhone, setReservationStatus } from "../api";
import { formatDate, isValidPhone, normalizePhone } from "../utils";

const STATUS_LABEL = { ACTUAL: "Активна", INVALID: "Недействительна" };

// ------------------------------------------------------------------ карточка брони

function ReservationCard({ reservation, onUpdate }) {
  const r = reservation;
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [sold, setSold] = useState(false);

  async function run(action) {
    setBusy(true);
    setError("");
    setMessage("");
    try {
      await action();
    } catch (e) {
      setError(e.message || "Не удалось выполнить действие.");
    } finally {
      setBusy(false);
    }
  }

  const changeStatus = (status) => {
    const text =
      status === "INVALID"
        ? `Отменить бронь ${r.reservationNum}? Товары вернутся в доступные.`
        : `Вернуть бронь ${r.reservationNum} в статус «Активна»?`;
    if (!window.confirm(text)) return;
    run(async () => {
      const updated = await setReservationStatus(r.reservationNum, status);
      // Ответ не содержит позиций брони - сохраняем их из текущей карточки
      onUpdate({ ...r, ...updated, unknown: false });
      setMessage(status === "INVALID" ? "Бронь отменена." : "Бронь снова активна.");
    });
  };

  const makeSale = () => {
    if (!window.confirm(`Оформить продажу по брони ${r.reservationNum}?`)) return;
    run(async () => {
      const sale = await addSale(r.reservationNum);
      setSold(true);
      setMessage(`Продажа оформлена${sale?.id ? ` (№ ${sale.id})` : ""}.`);
    });
  };

  const canSell = !sold && r.status !== "INVALID";

  return (
    <article className="card stack">
      <div className="panel-head" style={{ marginBottom: 0 }}>
        <div>
          <h2 style={{ letterSpacing: 1 }}>{r.reservationNum}</h2>
          {r.status && (
            <span className={"badge " + (r.status === "ACTUAL" ? "badge-free" : "badge-off")}>
              {STATUS_LABEL[r.status] || r.status}
            </span>
          )}
        </div>
      </div>

      {r.unknown ? (
        <div className="alert alert-warn">
          Данные брони получить не удалось (поиск брони на сервере пока недоступен). Действия ниже работают по номеру.
        </div>
      ) : (
        <dl className="props">
          <dt>Покупатель</dt><dd>{r.customer?.fullName || "—"}</dd>
          <dt>Телефон</dt><dd>{r.customer?.phone || "—"}</dd>
          <dt>Email</dt><dd>{r.customer?.email || "—"}</dd>
          <dt>Период</dt><dd>{formatDate(r.startAt)} — {formatDate(r.endAt)}</dd>
          <dt>Создана</dt><dd>{formatDate(r.createdAt)}</dd>
          {r.comment && (<><dt>Комментарий</dt><dd>{r.comment}</dd></>)}
        </dl>
      )}

      {Array.isArray(r.items) && r.items.length > 0 && (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr><th>Артикул</th><th>Товар</th><th>Кол-во</th></tr>
            </thead>
            <tbody>
              {r.items.map((it) => (
                <tr key={it.sku}>
                  <td>{it.sku}</td>
                  <td>{it.title || "—"}</td>
                  <td>{it.count}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {message && <div className="alert alert-success" style={{ margin: 0 }}>{message}</div>}
      {error && <div className="alert alert-error" role="alert" style={{ margin: 0 }}>{error}</div>}

      <div className="actions">
        {canSell && (
          <button type="button" className="btn btn-primary" onClick={makeSale} disabled={busy}>
            Оформить продажу
          </button>
        )}
        {r.status !== "INVALID" && (
          <button type="button" className="btn btn-danger" onClick={() => changeStatus("INVALID")} disabled={busy}>
            Отменить бронь
          </button>
        )}
        {r.status === "INVALID" && (
          <button type="button" className="btn btn-secondary" onClick={() => changeStatus("ACTUAL")} disabled={busy}>
            Вернуть в активные
          </button>
        )}
      </div>
    </article>
  );
}

// ------------------------------------------------------------------ страница

export default function ReservationSearch() {
  const [mode, setMode] = useState("num"); // "num" | "phone"
  const [value, setValue] = useState("");
  const [results, setResults] = useState(null); // null = ещё не искали
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [fallbackNum, setFallbackNum] = useState("");

  function switchMode(next) {
    setMode(next);
    setValue("");
    setError("");
    setResults(null);
    setFallbackNum("");
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setFallbackNum("");
    setResults(null);

    const v = value.trim();
    if (!v) {
      setError(mode === "num" ? "Введите номер брони." : "Введите телефон покупателя.");
      return;
    }
    if (mode === "phone" && !isValidPhone(v)) {
      setError("Введите корректный телефон, например +7 900 123-45-67.");
      return;
    }

    setLoading(true);
    try {
      if (mode === "num") {
        const num = v.toUpperCase();
        try {
          setResults([await getReservation(num)]);
        } catch (err) {
          setError(err.message);
          setFallbackNum(num); // позволим выполнить действия по номеру вслепую
        }
      } else {
        const list = await searchReservationsByPhone(normalizePhone(v));
        setResults(Array.isArray(list) ? list : []);
      }
    } catch (err) {
      setError(err.message || "Не удалось выполнить поиск.");
    } finally {
      setLoading(false);
    }
  }

  const updateOne = (updated) =>
    setResults((list) => list.map((r) => (r.reservationNum === updated.reservationNum ? updated : r)));

  return (
    <div className="container page page-wide">
      <nav className="breadcrumbs">Главная / Брони / Поиск</nav>
      <h1>Поиск брони</h1>

      <div className="card">
        <div className="actions" style={{ marginBottom: 14 }}>
          <button
            type="button" className={"btn " + (mode === "num" ? "btn-primary" : "btn-secondary")}
            onClick={() => switchMode("num")}
          >
            По номеру
          </button>
          <button
            type="button" className={"btn " + (mode === "phone" ? "btn-primary" : "btn-secondary")}
            onClick={() => switchMode("phone")}
          >
            По телефону
          </button>
        </div>

        <form onSubmit={handleSubmit} noValidate>
          <div className="form-field">
            <label htmlFor="rs-value">{mode === "num" ? "Номер брони" : "Телефон покупателя"}</label>
            <input
              id="rs-value"
              type={mode === "num" ? "text" : "tel"}
              placeholder={mode === "num" ? "K3J9X0Q2LZ81MWA" : "+7 900 123-45-67"}
              value={value}
              onChange={(e) => setValue(e.target.value)}
              autoFocus
            />
          </div>
          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? "Ищем…" : "Найти"}
          </button>
        </form>
      </div>

      <div className="stack" style={{ marginTop: 16 }}>
        {error && (
          <div className="alert alert-error" role="alert">
            {error}
            {fallbackNum && (
              <div style={{ marginTop: 8 }}>
                <button
                  type="button" className="btn btn-sm btn-secondary"
                  onClick={() => {
                    setError("");
                    setResults([{ reservationNum: fallbackNum, status: null, unknown: true }]);
                    setFallbackNum("");
                  }}
                >
                  Всё равно работать с номером {fallbackNum}
                </button>
              </div>
            )}
          </div>
        )}

        {results && results.length === 0 && <div className="empty-state">Броней не найдено.</div>}

        {results &&
          results.map((r) => <ReservationCard key={r.reservationNum} reservation={r} onUpdate={updateOne} />)}
      </div>
    </div>
  );
}
