import { useEffect, useState } from "react";

// ---------------------------------------------------------------- телефон и email

/**
 * Приводит телефон к виду +7XXXXXXXXXX (как хранит бэкенд).
 *   "8 900 123-45-67"      -> "+79001234567"
 *   "+7 (900) 123-45-67"   -> "+79001234567"
 *   "9001234567"           -> "+79001234567"
 * Всё нераспознаваемое возвращается очищенным от пробелов и скобок.
 */
export function normalizePhone(value) {
  const hasPlus = String(value).trim().startsWith("+");
  const digits = String(value).replace(/\D/g, "");

  if (!hasPlus && digits.length === 11 && (digits[0] === "8" || digits[0] === "7")) {
    return "+7" + digits.slice(1);
  }
  if (!hasPlus && digits.length === 10) return "+7" + digits;
  return (hasPlus ? "+" : "") + digits;
}

/** Телефон валиден, если после нормализации это + и 10-15 цифр */
export function isValidPhone(value) {
  return /^\+\d{10,15}$/.test(normalizePhone(value));
}

export function isValidEmail(value) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(String(value).trim());
}

// ---------------------------------------------------------------- деньги и числа

const priceFormatter = new Intl.NumberFormat("ru-RU", {
  style: "currency",
  currency: "RUB",
  maximumFractionDigits: 0,
});

/** 125000 -> "125 000 ₽" */
export function formatPrice(value) {
  const n = Number(value);
  if (!Number.isFinite(n)) return "—";
  return priceFormatter.format(n);
}

/** plural(3, ["товар", "товара", "товаров"]) -> "товара" */
export function plural(n, forms) {
  const abs = Math.abs(n) % 100;
  const last = abs % 10;
  if (abs > 10 && abs < 20) return forms[2];
  if (last > 1 && last < 5) return forms[1];
  if (last === 1) return forms[0];
  return forms[2];
}

// ---------------------------------------------------------------- даты

const p2 = (n) => String(n).padStart(2, "0");

/** Date -> "yyyy-MM-dd" (локальное время, как ждёт API брони) */
export function toIsoDate(d) {
  return `${d.getFullYear()}-${p2(d.getMonth() + 1)}-${p2(d.getDate())}`;
}

export function todayIso() {
  return toIsoDate(new Date());
}

/** addDaysIso("2026-10-25", 3) -> "2026-10-28" */
export function addDaysIso(iso, days) {
  const [y, m, d] = iso.split("-").map(Number);
  return toIsoDate(new Date(y, m - 1, d + days));
}

/** "2026-10-25" или "2026-10-25T12:30:00" -> "25.10.2026" */
export function formatDate(value) {
  if (!value) return "—";
  const [y, m, d] = String(value).slice(0, 10).split("-");
  return y && m && d ? `${d}.${m}.${y}` : String(value);
}

// ---------------------------------------------------------------- хуки

/** Значение, которое обновляется с задержкой - для поиска без запроса на каждую букву */
export function useDebounce(value, delay = 300) {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(t);
  }, [value, delay]);
  return debounced;
}
