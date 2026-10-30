import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { getProducts } from "../api";
import { useAuth } from "../auth";
import { formatDate, formatPrice, plural, useDebounce } from "../utils";
import ReservationModal from "../components/ReservationModal";

const PAGE_SIZE = 12;
const SEP = "|"; // разделитель значений в адресной строке (в названиях запятые бывают)

// Сколько штук можно забронировать. По описанию API это поле quantityReserved.
export const getAvailable = (p) => Number(p.quantityReserved) || 0;

const SORTS = {
  new: "Сначала новые",
  price_asc: "Сначала дешёвые",
  price_desc: "Сначала дорогие",
  title: "По названию",
};

const STATUS_LABEL = { FREE: "Свободен", BOOKED: "Забронирован" };

function uniqueNames(products, pick) {
  const set = new Set();
  products.forEach((p) => {
    const name = pick(p);
    if (name) set.add(name);
  });
  return [...set].sort((a, b) => a.localeCompare(b, "ru", { numeric: true }));
}

function pageList(current, total) {
  const nums = [...new Set([1, total, current - 1, current, current + 1])]
    .filter((n) => n >= 1 && n <= total)
    .sort((a, b) => a - b);
  const out = [];
  nums.forEach((n, i) => {
    if (i && n - nums[i - 1] > 1) out.push("…" + n);
    out.push(n);
  });
  return out;
}

// ------------------------------------------------------------------ мелкие компоненты

function StatusBadge({ product }) {
  if (getAvailable(product) <= 0) return <span className="badge badge-off">Нет в наличии</span>;
  const free = product.status === "FREE";
  return (
    <span className={"badge " + (free ? "badge-free" : "badge-booked")}>
      {STATUS_LABEL[product.status] || product.status}
    </span>
  );
}

function CheckGroup({ title, options, selected, onToggle }) {
  if (!options.length) return null;
  return (
    <div className="filters-group">
      <div className="filters-title">{title}</div>
      {options.map((name) => (
        <label key={name} className="checkbox-row">
          <input type="checkbox" checked={selected.includes(name)} onChange={() => onToggle(name)} />
          {name}
        </label>
      ))}
    </div>
  );
}

function ProductCard({ product, onOpen }) {
  return (
    <button type="button" className="product-card" onClick={() => onOpen(product)}>
      {product.imageUrl ? (
        <img className="product-img" src={product.imageUrl} alt={product.title} loading="lazy" />
      ) : (
        <div className="product-img" aria-hidden="true">🧥</div>
      )}
      <div className="product-body">
        <StatusBadge product={product} />
        <div className="product-title">{product.title}</div>
        <div className="product-meta">
          {[product.furType?.name, product.color?.name, product.size?.name].filter(Boolean).join(" · ")}
        </div>
        <div className="product-price">{formatPrice(product.listPrice)}</div>
      </div>
    </button>
  );
}

function ProductDrawer({ product, isStaff, onClose, onReserve }) {
  useEffect(() => {
    const onKey = (e) => e.key === "Escape" && onClose();
    document.addEventListener("keydown", onKey);
    const prev = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", onKey);
      document.body.style.overflow = prev;
    };
  }, [onClose]);

  const available = getAvailable(product);

  return (
    <div className="overlay overlay-drawer" onClick={onClose}>
      <aside
        className="drawer"
        role="dialog"
        aria-modal="true"
        aria-label={product.title}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="panel-head">
          <div>
            <h2>{product.title}</h2>
            <StatusBadge product={product} />
          </div>
          <button type="button" className="close-btn" onClick={onClose} aria-label="Закрыть">×</button>
        </div>

        <div className="product-price" style={{ fontSize: "1.4rem" }}>{formatPrice(product.listPrice)}</div>

        <dl className="props">
          <dt>Артикул</dt><dd>{product.sku}</dd>
          <dt>Вид меха</dt><dd>{product.furType?.name || "—"}</dd>
          <dt>Цвет</dt><dd>{product.color?.name || "—"}</dd>
          <dt>Размер</dt>
          <dd>
            {product.size?.name || "—"}
            {product.size?.description && <span className="muted"> ({product.size.description})</span>}
          </dd>
          <dt>На складе</dt><dd>{product.quantityOnHand} шт.</dd>
          <dt>Доступно для брони</dt><dd>{available} шт.</dd>
          <dt>Добавлен</dt><dd>{formatDate(product.createdAt)}</dd>
        </dl>

        <div className="actions">
          <button type="button" className="btn btn-primary" disabled={available <= 0} onClick={() => onReserve(product)}>
            Забронировать
          </button>
          {isStaff && (
            <Link to={`/products/${product.id}`} className="btn btn-secondary">Редактировать</Link>
          )}
        </div>
        {available <= 0 && <p className="hint">Сейчас этого товара нет в наличии для брони.</p>}
      </aside>
    </div>
  );
}

// ------------------------------------------------------------------ страница

export default function Catalog() {
  const { isStaff } = useAuth();
  const [params, setParams] = useSearchParams();

  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [filtersOpen, setFiltersOpen] = useState(false);
  const [selected, setSelected] = useState(null); // товар в drawer
  const [reserveFor, setReserveFor] = useState(null); // товар в окне брони

  // --- загрузка
  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setProducts(await getProducts());
    } catch (e) {
      setError(e.message || "Не удалось загрузить каталог.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  // --- фильтры живут в адресной строке (можно делиться ссылкой, работает «Назад»)
  const get = (k) => params.get(k) || "";
  const list = (k) => (params.get(k) ? params.get(k).split(SEP) : []);

  function update(changes) {
    const next = new URLSearchParams(params);
    Object.entries(changes).forEach(([k, v]) => {
      const empty = v === null || v === "" || v === false || (Array.isArray(v) && !v.length);
      if (empty) next.delete(k);
      else next.set(k, Array.isArray(v) ? v.join(SEP) : String(v));
    });
    if (!("page" in changes)) next.delete("page"); // любой фильтр - снова на 1 страницу
    setParams(next, { replace: true });
  }

  function toggle(key, value) {
    const cur = list(key);
    update({ [key]: cur.includes(value) ? cur.filter((v) => v !== value) : [...cur, value] });
  }

  // поиск: поле обновляется сразу, фильтр - с задержкой
  const [qInput, setQInput] = useState(get("q"));
  const qDebounced = useDebounce(qInput, 300);
  useEffect(() => {
    if (qDebounced.trim() !== get("q")) update({ q: qDebounced.trim() });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [qDebounced]);

  const furSel = list("fur");
  const colorSel = list("color");
  const sizeSel = list("size");
  const min = get("min");
  const max = get("max");
  const onlyAvail = get("avail") === "1";
  const status = get("status");
  const sort = SORTS[get("sort")] ? get("sort") : "new";
  const q = get("q");

  function resetAll() {
    setParams(new URLSearchParams(), { replace: true });
    setQInput("");
  }

  // --- варианты фильтров берём из самих товаров (работает и для покупателей без входа)
  const furOptions = useMemo(() => uniqueNames(products, (p) => p.furType?.name), [products]);
  const colorOptions = useMemo(() => uniqueNames(products, (p) => p.color?.name), [products]);
  const sizeOptions = useMemo(() => uniqueNames(products, (p) => p.size?.name), [products]);

  // --- фильтрация и сортировка
  const filtered = useMemo(() => {
    const s = q.toLowerCase();
    const result = products.filter((p) => {
      if (s && !(p.title || "").toLowerCase().includes(s) && !(p.sku || "").toLowerCase().includes(s)) return false;
      if (furSel.length && !furSel.includes(p.furType?.name)) return false;
      if (colorSel.length && !colorSel.includes(p.color?.name)) return false;
      if (sizeSel.length && !sizeSel.includes(p.size?.name)) return false;
      if (min !== "" && Number(p.listPrice) < Number(min)) return false;
      if (max !== "" && Number(p.listPrice) > Number(max)) return false;
      if (onlyAvail && getAvailable(p) <= 0) return false;
      if (status && p.status !== status) return false;
      return true;
    });

    const by = {
      new: (a, b) => (b.createdAt || "").localeCompare(a.createdAt || ""),
      price_asc: (a, b) => a.listPrice - b.listPrice,
      price_desc: (a, b) => b.listPrice - a.listPrice,
      title: (a, b) => (a.title || "").localeCompare(b.title || "", "ru"),
    };
    return result.sort(by[sort]);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [products, q, params]);

  // --- пагинация
  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const page = Math.min(Math.max(1, Number(get("page")) || 1), totalPages);
  const pageItems = filtered.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  function goPage(n) {
    update({ page: n === 1 ? null : n });
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  // --- плашки выбранных фильтров
  const chips = [];
  furSel.forEach((v) => chips.push({ key: "fur" + v, label: v, remove: () => toggle("fur", v) }));
  colorSel.forEach((v) => chips.push({ key: "color" + v, label: v, remove: () => toggle("color", v) }));
  sizeSel.forEach((v) => chips.push({ key: "size" + v, label: "Размер " + v, remove: () => toggle("size", v) }));
  if (min) chips.push({ key: "min", label: "от " + formatPrice(min), remove: () => update({ min: null }) });
  if (max) chips.push({ key: "max", label: "до " + formatPrice(max), remove: () => update({ max: null }) });
  if (onlyAvail) chips.push({ key: "avail", label: "Только доступные", remove: () => update({ avail: null }) });
  if (status) chips.push({ key: "status", label: STATUS_LABEL[status] || status, remove: () => update({ status: null }) });

  const hasFilters = chips.length > 0 || !!q;

  return (
    <div className="container page">
      <nav className="breadcrumbs" aria-label="Хлебные крошки">
        Главная / Товары / Каталог
      </nav>
      <h1>Каталог шуб</h1>

      <div className="catalog-layout">
        {/* ---------- Фильтры ---------- */}
        <div>
          <button type="button" className="btn btn-secondary filters-toggle" onClick={() => setFiltersOpen((v) => !v)}>
            Фильтры{chips.length ? ` (${chips.length})` : ""}
          </button>

          <aside className={"filters card" + (filtersOpen ? "" : " collapsed")} aria-label="Фильтры">
            <CheckGroup title="Вид меха" options={furOptions} selected={furSel} onToggle={(v) => toggle("fur", v)} />
            <CheckGroup title="Цвет" options={colorOptions} selected={colorSel} onToggle={(v) => toggle("color", v)} />
            <CheckGroup title="Размер" options={sizeOptions} selected={sizeSel} onToggle={(v) => toggle("size", v)} />

            <div className="filters-group">
              <div className="filters-title">Цена, ₽</div>
              <div className="row">
                <input
                  type="number" min="0" placeholder="от" aria-label="Цена от"
                  value={min} onChange={(e) => update({ min: e.target.value })}
                />
                <input
                  type="number" min="0" placeholder="до" aria-label="Цена до"
                  value={max} onChange={(e) => update({ max: e.target.value })}
                />
              </div>
            </div>

            <div className="filters-group">
              <div className="filters-title">Статус</div>
              <select value={status} onChange={(e) => update({ status: e.target.value })} aria-label="Статус">
                <option value="">Любой</option>
                <option value="FREE">Свободен</option>
                <option value="BOOKED">Забронирован</option>
              </select>
              <label className="checkbox-row" style={{ marginTop: 10 }}>
                <input type="checkbox" checked={onlyAvail} onChange={(e) => update({ avail: e.target.checked })} />
                Только доступные для брони
              </label>
            </div>

            <button type="button" className="btn btn-ghost btn-block" onClick={resetAll} disabled={!hasFilters}>
              Сбросить фильтры
            </button>
          </aside>
        </div>

        {/* ---------- Список ---------- */}
        <section className="catalog-main">
          <div className="toolbar">
            <input
              type="search" className="search-input" placeholder="Поиск по названию или артикулу"
              aria-label="Поиск" value={qInput} onChange={(e) => setQInput(e.target.value)}
            />
            <select value={sort} onChange={(e) => update({ sort: e.target.value === "new" ? null : e.target.value })} aria-label="Сортировка">
              {Object.entries(SORTS).map(([k, label]) => (
                <option key={k} value={k}>{label}</option>
              ))}
            </select>
          </div>

          {chips.length > 0 && (
            <div className="chips">
              {chips.map((c) => (
                <span key={c.key} className="chip">
                  {c.label}
                  <button type="button" onClick={c.remove} aria-label={`Убрать фильтр ${c.label}`}>×</button>
                </span>
              ))}
            </div>
          )}

          {error && (
            <div className="alert alert-error" role="alert">
              {error}{" "}
              <button type="button" className="btn btn-sm btn-secondary" onClick={load}>Повторить</button>
            </div>
          )}

          {loading ? (
            <div className="product-grid" aria-busy="true">
              {Array.from({ length: 8 }, (_, i) => <div key={i} className="skeleton" />)}
            </div>
          ) : !error && filtered.length === 0 ? (
            <div className="empty-state">
              <h2>Ничего не найдено</h2>
              <p>{products.length ? "Попробуйте изменить или сбросить фильтры." : "В каталоге пока нет товаров."}</p>
              {hasFilters && <button type="button" className="btn btn-primary" onClick={resetAll}>Сбросить фильтры</button>}
            </div>
          ) : (
            !error && (
              <>
                <div className="results-count">
                  Найдено: {filtered.length} {plural(filtered.length, ["товар", "товара", "товаров"])}
                </div>
                <div className="product-grid">
                  {pageItems.map((p) => <ProductCard key={p.id} product={p} onOpen={setSelected} />)}
                </div>

                {totalPages > 1 && (
                  <nav className="pagination" aria-label="Страницы">
                    <button type="button" disabled={page === 1} onClick={() => goPage(page - 1)}>‹</button>
                    {pageList(page, totalPages).map((n) =>
                      typeof n === "string" ? (
                        <span key={n} className="muted" style={{ alignSelf: "center" }}>…</span>
                      ) : (
                        <button
                          key={n} type="button" className={n === page ? "current" : ""}
                          aria-current={n === page ? "page" : undefined} onClick={() => goPage(n)}
                        >
                          {n}
                        </button>
                      )
                    )}
                    <button type="button" disabled={page === totalPages} onClick={() => goPage(page + 1)}>›</button>
                  </nav>
                )}
              </>
            )
          )}
        </section>
      </div>

      {selected && (
        <ProductDrawer
          product={selected}
          isStaff={isStaff}
          onClose={() => setSelected(null)}
          onReserve={(p) => {
            setSelected(null);
            setReserveFor(p);
          }}
        />
      )}

      {reserveFor && (
        <ReservationModal
          product={reserveFor}
          onClose={() => setReserveFor(null)}
          onReserved={load}
        />
      )}
    </div>
  );
}
