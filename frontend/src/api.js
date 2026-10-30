// Весь обмен с бэкендом MehMagnat. Остальной код фронтенда в fetch напрямую не ходит.
// Запросы идут на тот же домен (nginx проксирует /api, /csrf, /login, /logout).
//
// Адреса, помеченные [НУЖЕН БЭК], в текущем API ещё не существуют -
// их должен дописать бэкендщик (см. список в чате). Пока их нет, фронт
// аккуратно подставляет запасное поведение.

// ---------------------------------------------------------------- ошибки

export class ApiError extends Error {
  constructor(status, message, body = null) {
    super(message);
    this.name = "ApiError";
    this.status = status; // 0 = нет связи с сервером
    this.body = body;
  }
}

const DEFAULT_MESSAGES = {
  0: "Нет связи с сервером. Проверьте подключение.",
  400: "Некорректный запрос.",
  401: "Нужно войти в систему.",
  403: "Недостаточно прав для этого действия.",
  404: "Не найдено.",
  500: "Не удалось выполнить операцию.",
};

function messageFor(status, serverMessage, overrides) {
  if (overrides && overrides[status]) return overrides[status];
  // Бэкенд пока не отдаёт текст ошибок; когда начнёт ({"message": "..."}), покажем его.
  if (serverMessage) return serverMessage;
  return DEFAULT_MESSAGES[status] || `Ошибка ${status}.`;
}

// Страница подписывается сюда, чтобы по 401 показать экран входа.
let unauthorizedHandler = null;
export function setUnauthorizedHandler(fn) {
  unauthorizedHandler = fn;
}

// ---------------------------------------------------------------- CSRF

let csrfToken = null;

async function loadCsrf() {
  const res = await fetch("/csrf", { credentials: "include" });
  if (!res.ok) throw new ApiError(res.status, "Не удалось получить CSRF-токен.");
  const data = await res.json();
  csrfToken = data.token;
  return csrfToken;
}

async function getCsrf() {
  return csrfToken || loadCsrf();
}

// После входа/выхода токен меняется - сбрасываем кэш.
export function resetCsrf() {
  csrfToken = null;
}

// ---------------------------------------------------------------- базовый запрос

/**
 * @param {string} path
 * @param {object} opts
 *  method   - GET (по умолчанию) или POST
 *  query    - объект с query-параметрами
 *  json     - тело запроса (отправится как JSON)
 *  form     - объект для application/x-www-form-urlencoded
 *  as       - как разбирать ответ: "json" (по умолчанию) | "text" | "none"
 *  errors   - свои тексты ошибок по коду: { 400: "..." }
 *  silent401 - не вызывать глобальный обработчик 401
 */
async function request(path, opts = {}) {
  const {
    method = "GET",
    query,
    json,
    form,
    as = "json",
    errors,
    silent401 = false,
  } = opts;

  let url = path;
  if (query) {
    const qs = new URLSearchParams();
    Object.entries(query).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== "") qs.append(k, v);
    });
    const s = qs.toString();
    if (s) url += (url.includes("?") ? "&" : "?") + s;
  }

  const isWrite = method !== "GET";

  async function send() {
    const headers = {};
    let body;
    if (json !== undefined) {
      headers["Content-Type"] = "application/json";
      body = JSON.stringify(json);
    } else if (form) {
      body = new URLSearchParams(form); // Content-Type выставится сам
    }
    if (isWrite) headers["X-XSRF-TOKEN"] = await getCsrf();
    return fetch(url, { method, headers, body, credentials: "include" });
  }

  let res;
  try {
    res = await send();
    // 403 может означать просроченный CSRF-токен: берём новый и пробуем один раз ещё.
    if (isWrite && res.status === 403) {
      resetCsrf();
      res = await send();
    }
  } catch (e) {
    if (e instanceof ApiError) throw e;
    throw new ApiError(0, messageFor(0, null, errors));
  }

  const raw = await res.text();

  if (!res.ok) {
    let serverMessage = null;
    let parsed = null;
    try {
      parsed = raw ? JSON.parse(raw) : null;
      serverMessage = parsed && (parsed.message || parsed.error);
    } catch {
      /* тело не JSON - игнорируем */
    }
    if (res.status === 401 && !silent401 && unauthorizedHandler) unauthorizedHandler();
    throw new ApiError(res.status, messageFor(res.status, serverMessage, errors), parsed);
  }

  if (as === "none") return null;
  if (as === "text") return raw;
  return raw ? JSON.parse(raw) : null;
}

// ---------------------------------------------------------------- утилиты

/** Текущее локальное время в формате LocalDateTime: "2026-10-25T12:30:00" */
export function nowLocalIso() {
  const d = new Date();
  const p = (n) => String(n).padStart(2, "0");
  return (
    `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}` +
    `T${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
  );
}

// ---------------------------------------------------------------- авторизация

export async function login(phone, password) {
  await request("/login", {
    method: "POST",
    form: { phone, password },
    as: "none",
    silent401: true,
    errors: { 401: "Неверный телефон или пароль." },
  });
  resetCsrf(); // после входа токен новый
}

export async function logout() {
  try {
    await request("/logout", { method: "POST", as: "none", silent401: true });
  } finally {
    resetCsrf();
  }
}

/**
 * Кто сейчас вошёл. Возвращает { id, fullName, role } или null, если не вошёл.
 * GET /api/me (есть в ProfileController). Если вдруг не ответит, проверяем сессию через /api/products
 * (он доступен только сотрудникам) - тогда роль неизвестна (role: null).
 */
export async function getMe() {
  try {
    return await request("/api/me", { silent401: true });
  } catch (e) {
    if (e.status === 401) return null;
    if (e.status === 0) throw e;
    // /api/me ещё не реализован - запасной вариант
    try {
      await request("/api/products", { silent401: true });
      return { id: null, fullName: "Сотрудник", role: null };
    } catch {
      return null;
    }
  }
}

// ---------------------------------------------------------------- товары и справочники

/** Каталог для всех (без входа) */
export function getProducts() {
  return request("/api/products/get_all");
}

/**
 * Добавить товар. createdAt проставляется здесь, если не передан.
 * sku НЕ передаём - его генерирует сервер.
 */
export function addProduct(product) {
  return request("/api/products/add", {
    method: "POST",
    json: { ...product, createdAt: product.createdAt || nowLocalIso() },
    errors: { 500: "Не удалось добавить товар. Проверьте поля и попробуйте ещё раз." },
  });
}

export function getFurTypes() {
  return request("/api/fur-type/get");
}

/**
 * Справочник, которого пока нет на бэкенде.
 * Возвращает массив или null, если адрес ещё не реализован / нет доступа -
 * тогда вызывающий код соберёт значения из списка товаров.
 */
async function optionalDictionary(path) {
  try {
    return await request(path, { silent401: true });
  } catch (e) {
    if (e.status === 0) throw e;
    return null;
  }
}

/** [НУЖЕН БЭК] GET /api/color/get */
export const getColors = () => optionalDictionary("/api/color/get");
/** [НУЖЕН БЭК] GET /api/size/get */
export const getSizes = () => optionalDictionary("/api/size/get");

/** [НУЖЕН БЭК] GET /api/products/{id} */
export function getProduct(id) {
  return request(`/api/products/${encodeURIComponent(id)}`);
}

/** [НУЖЕН БЭК] PUT /api/products/{id} */
export function updateProduct(id, product) {
  return request(`/api/products/${encodeURIComponent(id)}`, {
    method: "PUT",
    json: product,
  });
}

// ---------------------------------------------------------------- брони

/**
 * Создать бронь. Возвращает номер брони (строка, НЕ JSON).
 * payload: { startAt: "yyyy-MM-dd", endAt: "yyyy-MM-dd",
 *            skuAndCount: [{ sku, count }],
 *            customer: { fullName, phone, email } }
 */
export function createReservation(payload) {
  return request("/api/reservation/made_reservation", {
    method: "POST",
    json: payload,
    as: "text",
    errors: {
      500:
        "Не удалось создать бронь: товара нет в наличии на эти даты, " +
        "либо телефон или email уже привязаны к другому покупателю.",
    },
  });
}

/** Сменить статус брони: ACTUAL | INVALID. Возвращает объект брони. */
export function setReservationStatus(reservationNum, status) {
  return request("/api/reservation/set_status_reservation", {
    method: "POST",
    query: { reservationNum, status },
    errors: { 500: "Бронь с таким номером не найдена." },
  });
}

/** Бронь по номеру: GET /api/reservation/get?reservationNum=... */
export function getReservation(reservationNum) {
  return request("/api/reservation/get", {
    query: { reservationNum },
    errors: { 404: "Бронь с таким номером не найдена." },
  });
}

/** Брони покупателя по телефону: GET /api/reservation/search?phone=... */
export function searchReservationsByPhone(phone) {
  return request("/api/reservation/search", { query: { phone } });
}

/**
 * Товары брони: GET /api/reservation_item?reservationNum=...
 * Бэкенд отдаёт позиции целиком (ReservationItemEntity), приводим их к простому виду:
 * { id, sku, title, count, price }.
 */
export async function getReservationItems(reservationNum) {
  const list = await request("/api/reservation_item", { query: { reservationNum } });
  return (Array.isArray(list) ? list : []).map((it) => ({
    id: it.id,
    sku: it.product?.sku ?? it.sku,
    title: it.product?.title ?? it.title,
    count: it.quantity ?? it.count,
    price: it.unitPrice ?? it.price,
  }));
}

// ---------------------------------------------------------------- продажи и сотрудники

/** Оформить продажу по брони (ADMIN, SALER). Возвращает объект продажи. */
export function addSale(reservationNum) {
  return request("/api/sale/add", {
    method: "POST",
    query: { reservationNum },
    errors: {
      400: "Бронь уже недействительна, продажу оформить нельзя.",
      500: "Бронь не найдена или в ней нет товаров.",
    },
  });
}

/**
 * Зарегистрировать сотрудника (только ADMIN).
 * payload: { fullName, phone, email, role: "SALER"|"ADMIN", isActive, password }
 * Бэкенд отвечает true/false.
 */
export async function registerEmployee(payload) {
  const ok = await request("/api/registration", {
    method: "POST",
    json: payload,
    errors: {
      400: "Не удалось сохранить сотрудника.",
      500: "Телефон или email уже заняты.",
    },
  });
  if (ok === false) throw new ApiError(400, "Не удалось сохранить сотрудника.");
  return ok;
}
