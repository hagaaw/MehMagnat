import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { getAvailable } from "./utils";

// Корзина для брони нескольких шуб сразу.
// Элемент корзины: { sku, title, price, count, max, imageUrl, details }
//   max - сколько свободно на момент добавления (count не может быть больше)
// Содержимое хранится в localStorage, чтобы пережить обновление страницы.

const STORAGE_KEY = "mehmagnat.cart.v1";
const CartContext = createContext(null);

function loadCart() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    const parsed = raw ? JSON.parse(raw) : [];
    return Array.isArray(parsed)
      ? parsed.filter((it) => it && it.sku && Number(it.count) > 0 && Number(it.max) > 0)
      : [];
  } catch {
    return [];
  }
}

const clamp = (n, min, max) => Math.min(Math.max(n, min), max);

export function CartProvider({ children }) {
  const [items, setItems] = useState(loadCart);

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
    } catch {
      /* хранилище недоступно (приватный режим) - корзина живёт до закрытия вкладки */
    }
  }, [items]);

  /** Добавить товар. Больше свободного количества положить нельзя. */
  const add = useCallback((product, count = 1) => {
    const max = getAvailable(product);
    if (max <= 0) return;

    setItems((list) => {
      const existing = list.find((it) => it.sku === product.sku);
      if (existing) {
        return list.map((it) =>
          it.sku === product.sku ? { ...it, max, count: clamp(it.count + count, 1, max) } : it
        );
      }
      return [
        ...list,
        {
          sku: product.sku,
          title: product.title,
          price: Number(product.listPrice) || 0,
          count: clamp(count, 1, max),
          max,
          imageUrl: product.imageUrl || null,
          details: [product.furType?.name, product.color?.name, product.size?.name].filter(Boolean).join(" · "),
        },
      ];
    });
  }, []);

  const setCount = useCallback((sku, count) => {
    setItems((list) =>
      list.map((it) => (it.sku === sku ? { ...it, count: clamp(Math.floor(Number(count)) || 1, 1, it.max) } : it))
    );
  }, []);

  const remove = useCallback((sku) => setItems((list) => list.filter((it) => it.sku !== sku)), []);
  const clear = useCallback(() => setItems([]), []);

  /**
   * Сверить корзину с актуальным каталогом: обновить цену, название и остаток,
   * убрать то, что закончилось или исчезло. Возвращает список названий убранных товаров.
   */
  const syncStock = useCallback((products) => {
    const removed = [];
    setItems((list) => {
      const bySku = new Map(products.map((p) => [p.sku, p]));
      const next = [];
      list.forEach((it) => {
        const p = bySku.get(it.sku);
        const max = p ? getAvailable(p) : 0;
        if (!p || max <= 0) {
          removed.push(it.title);
          return;
        }
        next.push({
          ...it,
          title: p.title,
          price: Number(p.listPrice) || 0,
          max,
          count: clamp(it.count, 1, max),
          imageUrl: p.imageUrl || it.imageUrl || null,
        });
      });
      return next;
    });
    return removed;
  }, []);

  const value = useMemo(
    () => ({
      items,
      add,
      setCount,
      remove,
      clear,
      syncStock,
      countOf: (sku) => items.find((it) => it.sku === sku)?.count || 0,
      totalCount: items.reduce((sum, it) => sum + it.count, 0),
      totalPrice: items.reduce((sum, it) => sum + it.count * it.price, 0),
    }),
    [items, add, setCount, remove, clear, syncStock]
  );

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

export function useCart() {
  const ctx = useContext(CartContext);
  if (!ctx) throw new Error("useCart нужно использовать внутри <CartProvider>");
  return ctx;
}
