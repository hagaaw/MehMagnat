import { useCallback, useEffect, useRef, useState } from "react";
import { useSearchParams } from "react-router-dom";
import {
  addSale,
  getReservation,
  getReservationItems,
  searchReservationsByPhone,
  setReservationStatus,
} from "../api";
import { useAuth } from "../auth";
import { formatDate, formatPrice, isValidPhone, normalizePhone } from "../utils";

const STATUS_LABEL = { ACTUAL: "Активна", INVALID: "Недействительна" };

// Добавляет к брони список товаров. Если не получилось - показываем бронь без товаров.
async function withItems(reservation) {
  try {
    return { ...reservation, items: await getReservationItems(reservation.reservationNum) };
  } catch {
    return reservation;
  }
}

// ------------------------------------------------------------------ карточка брони

// staff = true  -> видны кнопки «Оформить продажу» и «Отменить бронь»
// staff = false -> покупатель только смотрит бронь
function ReservationCard({ reservation: r, staff, onUpdate }) {
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
      onUpdate({ ...r, ...updated });
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

  const badgeClass = r.status === "ACTUAL" ? "badge-free" : "badge-off";

  return (
    <article className="card stack">
      <div className="panel-head" style={{ marginBottom: 0 }}>
        <div>
          <div className="muted">Номер брони</div>
          <h2 style={{ letterSpacing: 1, userSelect: "all" }}>{r.reservationNum}</h2>
          {r.status && <span className={"badge " + badgeClass}>{STATUS_LABEL[r.status] || r.status}</span>}
        </div>
      </div>

      <dl className="props">
        <dt>Период брони</dt><dd>{formatDate(r.startAt)} — {formatDate(r.endAt)}</dd>
        <dt>Оформлена</dt><dd>{formatDate(r.createdAt)}</dd>
        <dt>Покупатель</dt><dd>{r.customer?.fullName || "—"}</dd>
        <dt>Телефон</dt><dd>{r.customer?.phone || "—"}</dd>
        <dt>Email</dt><dd>{r.customer?.email || "—"}</dd>
        {r.comment && (<><dt>Комментарий</dt><dd>{r.comment}</dd></>)}
      </dl>

      {Array.isArray(r.items) && r.items.length > 0 && (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Артикул</th>
                <th>Товар</th>
                <th>Кол-во</th>
                <th>Цена</th>
                <th>Сумма</th>
              </tr>
            </thead>
            <tbody>
              {r.items.map((it) => (
                <tr key={it.id ?? it.sku}>
                  <td>{it.sku}</td>
                  <td>{it.title || "—"}</td>
                  <td>{it.count}</td>
                  <td>{it.price != null ? formatPrice(it.price) : "—"}</td>
                  <td>{it.price != null ? formatPrice(it.price * it.count) : "—"}</td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr>
                <td colSpan={4}><strong>Итого</strong></td>
                <td>
                  <strong>
                    {formatPrice(r.items.reduce((sum, it) => sum + (Number(it.price) || 0) * (Number(it.count) || 0), 0))}
                  </strong>
                </td>
              </tr>
            </tfoot>
          </table>
        </div>
      )}

      {r.status === "INVALID" && !staff && (
        <div className="alert alert-warn" style={{ margin: 0 }}>
          Эта бронь больше не действует. Если нужна помощь, свяжитесь с магазином.
        </div>
      )}

      {message && <div className="alert alert-success" style={{ margin: 0 }}>{message}</div>}
      {error && <div className="alert alert-error" role="alert" style={{ margin: 0 }}>{error}</div>}

      {staff && (
        <div className="actions">
          {!sold && r.status !== "INVALID" && (
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
      )}
    </article>
  );
}

// ------------------------------------------------------------------ страница

export default function ReservationSearch() {
  const { isStaff } = useAuth();
  const [params, setParams] = useSearchParams();
  const initialNum = params.get("num") || "";

  const [mode, setMode] = useState("num"); // "num" | "phone" (телефон - только сотрудникам)
  const [value, setValue] = useState(initialNum);
  const [results, setResults] = useState(null); // null = ещё не искали
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const runSearch = useCallback(async (searchMode, raw) => {
    setError("");
    setResults(null);

    const v = raw.trim();
    if (!v) {
      setError(searchMode === "num" ? "Введите номер брони." : "Введите телефон покупателя.");
      return;
    }
    if (searchMode === "phone" && !isValidPhone(v)) {
      setError("Введите корректный телефон, например +7 900 123-45-67.");
      return;
    }

    setLoading(true);
    try {
      if (searchMode === "num") {
        const num = v.replace(/\s+/g, "").toUpperCase();
        setResults([await withItems(await getReservation(num))]);
        setParams({ num }, { replace: true }); // ссылку на найденную бронь можно сохранить
      } else {
        const list = await searchReservationsByPhone(normalizePhone(v));
        setResults(Array.isArray(list) ? await Promise.all(list.map(withItems)) : []);
      }
    } catch (err) {
      // Бэкенд на «не найдено» отвечает 500 без текста, поэтому трактуем 500 и 404 как «нет такой брони»
      if (err.status === 500 || err.status === 404) {
        setError(
          searchMode === "num"
            ? "Бронь с таким номером не найдена. Проверьте номер и попробуйте ещё раз."
            : "Для этого телефона броней не найдено."
        );
      } else {
        setError(err.message || "Не удалось выполнить поиск.");
      }
    } finally {
      setLoading(false);
    }
  }, [setParams]);

  // Открыли по ссылке /reservations/search?num=... - сразу показываем бронь
  const autoRan = useRef(false);
  useEffect(() => {
    if (initialNum && !autoRan.current) {
      autoRan.current = true;
      runSearch("num", initialNum);
    }
  }, [initialNum, runSearch]);

  function switchMode(next) {
    setMode(next);
    setValue("");
    setError("");
    setResults(null);
  }

  const updateOne = (updated) =>
    setResults((list) => list.map((r) => (r.reservationNum === updated.reservationNum ? updated : r)));

  const byNumber = mode === "num";

  return (
    <div className="container page page-wide">
      <nav className="breadcrumbs">Главная / Брони / Поиск</nav>
      <h1>Поиск брони</h1>
      {!isStaff && (
        <p className="muted">Введите номер брони, который вы получили при оформлении, и мы покажем её данные.</p>
      )}

      <div className="card">
        {isStaff && (
          <div className="actions" style={{ marginBottom: 14 }}>
            <button
              type="button" className={"btn " + (byNumber ? "btn-primary" : "btn-secondary")}
              onClick={() => switchMode("num")}
            >
              По номеру
            </button>
            <button
              type="button" className={"btn " + (!byNumber ? "btn-primary" : "btn-secondary")}
              onClick={() => switchMode("phone")}
            >
              По телефону
            </button>
          </div>
        )}

        <form
          onSubmit={(e) => {
            e.preventDefault();
            runSearch(mode, value);
          }}
          noValidate
        >
          <div className="form-field">
            <label htmlFor="rs-value">{byNumber ? "Номер брони" : "Телефон покупателя"}</label>
            <input
              id="rs-value"
              type={byNumber ? "text" : "tel"}
              placeholder={byNumber ? "K3J9X0Q2LZ81MWA" : "+7 900 123-45-67"}
              value={value}
              onChange={(e) => setValue(e.target.value)}
              autoComplete="off"
              autoFocus={!initialNum}
            />
          </div>
          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? "Ищем…" : "Найти"}
          </button>
        </form>
      </div>

      <div className="stack" style={{ marginTop: 16 }}>
        {error && <div className="alert alert-error" role="alert">{error}</div>}

        {results && results.length === 0 && <div className="empty-state">Броней не найдено.</div>}

        {results &&
          results.map((r) => (
            <ReservationCard key={r.reservationNum} reservation={r} staff={isStaff} onUpdate={updateOne} />
          ))}
      </div>
    </div>
  );
}
