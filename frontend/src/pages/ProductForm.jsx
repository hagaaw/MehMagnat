import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
  addProduct,
  deleteProductImage,
  getFurTypes,
  getProduct,
  getProducts,
  updateProduct,
  uploadProductImage,
} from "../api";
import { getAvailable } from "../utils";

const IMAGE_TYPES = ["image/jpeg", "image/png", "image/webp"];
const MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5 МБ, как на сервере

const EMPTY = {
  title: "",
  listPrice: "",
  quantityOnHand: "",
  status: "FREE",
  furType: "",
  color: "",
  sizeName: "",
  sizeDesc: "",
};

const toForm = (p) => ({
  title: p.title || "",
  listPrice: p.listPrice ?? "",
  quantityOnHand: p.quantityOnHand ?? "",
  status: p.status || "FREE",
  furType: p.furType?.name || "",
  color: p.color?.name || "",
  sizeName: p.size?.name || "",
  sizeDesc: p.size?.description || "",
});

const uniq = (arr) => [...new Set(arr.filter(Boolean))].sort((a, b) => a.localeCompare(b, "ru", { numeric: true }));

/**
 * Один компонент на два маршрута:
 *   /products/new  - создание
 *   /products/:id  - редактирование (нужны GET и PUT /api/products/{id} на бэкенде)
 */
export default function ProductForm() {
  const { id } = useParams();
  const isEdit = Boolean(id);
  const navigate = useNavigate();

  const [form, setForm] = useState(EMPTY);
  const [original, setOriginal] = useState(null); // товар до правок (для sku и createdAt)
  const [errors, setErrors] = useState({});
  const [serverError, setServerError] = useState("");
  const [loading, setLoading] = useState(isEdit);
  const [loadError, setLoadError] = useState("");
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(null); // сохранённый товар
  const [warning, setWarning] = useState(""); // товар сохранён, но с фото возникла проблема

  // Фото: выбранный файл, его предпросмотр и пометка «убрать текущее фото»
  const [imageFile, setImageFile] = useState(null);
  const [imagePreview, setImagePreview] = useState(null);
  const [removeImage, setRemoveImage] = useState(false);
  const [fileKey, setFileKey] = useState(0); // меняем, чтобы сбросить поле выбора файла

  const [furOptions, setFurOptions] = useState([]);
  const [colorOptions, setColorOptions] = useState([]);
  const [sizeMap, setSizeMap] = useState({}); // название размера -> описание

  // Подсказки для полей + (при редактировании) загрузка товара
  useEffect(() => {
    let cancelled = false;
    (async () => {
      setLoadError("");
      const [furs, products] = await Promise.all([
        getFurTypes().catch(() => []),
        getProducts().catch(() => []),
      ]);
      if (cancelled) return;

      setFurOptions(uniq([...furs.map((f) => f.name), ...products.map((p) => p.furType?.name)]));
      setColorOptions(uniq(products.map((p) => p.color?.name)));
      const sizes = {};
      products.forEach((p) => p.size?.name && (sizes[p.size.name] = p.size.description || ""));
      setSizeMap(sizes);

      if (!isEdit) return;
      try {
        let product;
        try {
          product = await getProduct(id);
        } catch (e) {
          if (e.status === 0) throw e;
          // GET /api/products/{id} ещё нет на бэкенде - ищем в общем списке
          product = products.find((p) => String(p.id) === String(id));
        }
        if (!product) throw new Error("Товар не найден.");
        if (!cancelled) {
          setOriginal(product);
          setForm(toForm(product));
        }
      } catch (e) {
        if (!cancelled) setLoadError(e.message || "Не удалось загрузить товар.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [id, isEdit]);

  useEffect(() => {
    if (!imageFile) {
      setImagePreview(null);
      return undefined;
    }
    const url = URL.createObjectURL(imageFile);
    setImagePreview(url);
    return () => URL.revokeObjectURL(url);
  }, [imageFile]);

  function handleFile(e) {
    const file = e.target.files?.[0];
    if (!file) return;
    let problem = null;
    if (!IMAGE_TYPES.includes(file.type)) problem = "Допустимы только JPG, PNG или WebP.";
    else if (file.size > MAX_IMAGE_SIZE) problem = "Файл больше 5 МБ.";

    setErrors((er) => ({ ...er, image: problem || undefined }));
    if (problem) {
      setFileKey((k) => k + 1);
      return;
    }
    setImageFile(file);
    setRemoveImage(false);
  }

  function clearImage() {
    if (imageFile) {
      setImageFile(null);
      setFileKey((k) => k + 1);
    } else if (original?.imageUrl) {
      setRemoveImage(true);
    }
  }

  const set = (field) => (e) => {
    const value = e.target.value;
    setForm((f) => {
      const next = { ...f, [field]: value };
      // выбрали существующий размер - подставим его описание
      if (field === "sizeName" && sizeMap[value] !== undefined && !f.sizeDesc) next.sizeDesc = sizeMap[value];
      return next;
    });
    setErrors((er) => ({ ...er, [field]: undefined }));
  };

  function validate() {
    const er = {};
    const isInt = (v) => /^\d+$/.test(String(v).trim());

    if (form.title.trim().length < 2) er.title = "Введите название.";
    if (!(Number(form.listPrice) > 0)) er.listPrice = "Цена должна быть больше 0.";
    const reserved = Number(original?.quantityReserved) || 0;
    if (!isInt(form.quantityOnHand)) er.quantityOnHand = "Целое число, 0 или больше.";
    else if (Number(form.quantityOnHand) < reserved)
      er.quantityOnHand = `Не может быть меньше забронированного (${reserved} шт.).`;
    if (!form.furType.trim()) er.furType = "Укажите вид меха.";
    if (!form.color.trim()) er.color = "Укажите цвет.";
    if (!form.sizeName.trim()) er.sizeName = "Укажите размер.";
    return er;
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setServerError("");
    setWarning("");
    setSaved(null);
    const er = validate();
    setErrors(er);
    if (Object.keys(er).length) return;

    const payload = {
      title: form.title.trim(),
      listPrice: Number(form.listPrice),
      quantityOnHand: Number(form.quantityOnHand),
      // Забронированное количество ведёт сервер: у новой шубы 0, при правке не трогаем
      quantityReserved: isEdit ? Number(original?.quantityReserved) || 0 : 0,
      status: form.status,
      furType: { name: form.furType.trim() },
      color: { name: form.color.trim() },
      size: { name: form.sizeName.trim(), description: form.sizeDesc.trim() },
    };

    setSaving(true);
    try {
      if (isEdit) {
        const updated = await updateProduct(original.sku, {
          ...payload,
          sku: original.sku,
          createdAt: original?.createdAt,
        });
        let result = updated && typeof updated === "object" ? updated : { ...original, ...payload };

        // Фото обновляем отдельным запросом после сохранения полей
        try {
          if (imageFile) {
            result = (await uploadProductImage(result.sku, imageFile)) || result;
          } else if (removeImage && result.imageUrl) {
            result = (await deleteProductImage(result.sku)) || result;
          }
          setImageFile(null);
          setRemoveImage(false);
          setFileKey((k) => k + 1);
        } catch (imgErr) {
          setWarning(`Изменения сохранены, но фото не обновилось: ${imgErr.message}`);
        }
        setOriginal(result);
        setSaved(result);
      } else {
        let created = await addProduct(payload); // createdAt проставит api.js

        if (imageFile) {
          try {
            created = (await uploadProductImage(created.sku, imageFile)) || created;
          } catch (imgErr) {
            setWarning(
              `Товар добавлен, но фото не загрузилось: ${imgErr.message} ` +
                "Откройте товар на редактирование и загрузите фото ещё раз."
            );
          }
        }
        setSaved(created);
        setForm(EMPTY);
        setImageFile(null);
        setFileKey((k) => k + 1);
      }
    } catch (err) {
      setServerError(err.message || "Не удалось сохранить товар.");
    } finally {
      setSaving(false);
    }
  }

  const sizeOptions = useMemo(() => Object.keys(sizeMap).sort((a, b) => a.localeCompare(b, "ru", { numeric: true })), [sizeMap]);
  const shownImage = imagePreview || (!removeImage ? original?.imageUrl : null) || null;
  const fieldError = (name) => errors[name] && <div className="field-error">{errors[name]}</div>;
  const cls = (name) => (errors[name] ? "invalid" : "");

  if (loading) return <div className="page-loading">Загрузка…</div>;

  if (loadError) {
    return (
      <div className="container page page-narrow">
        <div className="alert alert-error" role="alert">{loadError}</div>
        <Link to="/products/catalog" className="btn btn-secondary">В каталог</Link>
      </div>
    );
  }

  return (
    <div className="container page page-wide">
      <nav className="breadcrumbs">Главная / Товары / {isEdit ? "Редактирование" : "Новый товар"}</nav>
      <h1>{isEdit ? "Редактирование товара" : "Новый товар"}</h1>
      {isEdit && original?.sku && <p className="muted">Артикул: {original.sku}</p>}

      <form className="card" onSubmit={handleSubmit} noValidate>
        <div className="form-field">
          <label htmlFor="pf-title">Название</label>
          <input id="pf-title" type="text" value={form.title} onChange={set("title")} className={cls("title")} placeholder="Шуба норковая" />
          {fieldError("title")}
        </div>

        <div className="row">
          <div className="form-field">
            <label htmlFor="pf-fur">Вид меха</label>
            <input id="pf-fur" type="text" list="pf-fur-list" value={form.furType} onChange={set("furType")} className={cls("furType")} placeholder="Норка" />
            <datalist id="pf-fur-list">{furOptions.map((n) => <option key={n} value={n} />)}</datalist>
            {fieldError("furType")}
          </div>
          <div className="form-field">
            <label htmlFor="pf-color">Цвет</label>
            <input id="pf-color" type="text" list="pf-color-list" value={form.color} onChange={set("color")} className={cls("color")} placeholder="Чёрный" />
            <datalist id="pf-color-list">{colorOptions.map((n) => <option key={n} value={n} />)}</datalist>
            {fieldError("color")}
          </div>
        </div>

        <div className="row">
          <div className="form-field">
            <label htmlFor="pf-size">Размер</label>
            <input id="pf-size" type="text" list="pf-size-list" value={form.sizeName} onChange={set("sizeName")} className={cls("sizeName")} placeholder="M" />
            <datalist id="pf-size-list">{sizeOptions.map((n) => <option key={n} value={n} />)}</datalist>
            {fieldError("sizeName")}
          </div>
          <div className="form-field">
            <label htmlFor="pf-size-desc">Описание размера</label>
            <input id="pf-size-desc" type="text" value={form.sizeDesc} onChange={set("sizeDesc")} placeholder="46-48" />
            <div className="hint">Если такой размер уже есть, описание не изменится.</div>
          </div>
        </div>

        <div className="row">
          <div className="form-field">
            <label htmlFor="pf-price">Цена, ₽</label>
            <input id="pf-price" type="number" min="0" step="0.01" value={form.listPrice} onChange={set("listPrice")} className={cls("listPrice")} />
            {fieldError("listPrice")}
          </div>
          <div className="form-field">
            <label htmlFor="pf-onhand">Всего в наличии, шт.</label>
            <input id="pf-onhand" type="number" min="0" step="1" value={form.quantityOnHand} onChange={set("quantityOnHand")} className={cls("quantityOnHand")} />
            {fieldError("quantityOnHand")}
          </div>
          {isEdit && (
            <div className="form-field">
              <div className="label">Забронировано</div>
              <div style={{ padding: "9px 0" }}>
                {Number(original?.quantityReserved) || 0} шт. · свободно {getAvailable(original)} шт.
              </div>
            </div>
          )}
        </div>

        <div className="form-field">
          <div className="label">Фото</div>
          <div style={{ display: "flex", gap: 16, alignItems: "center", flexWrap: "wrap" }}>
            <div style={{ width: 140, flex: "none", borderRadius: 8, overflow: "hidden" }}>
              {shownImage ? (
                <img className="product-img" src={shownImage} alt="Фото товара" />
              ) : (
                <div className="product-img" aria-hidden="true">🧥</div>
              )}
            </div>
            <div>
              <input
                key={fileKey}
                type="file"
                accept="image/jpeg,image/png,image/webp"
                onChange={handleFile}
                aria-label="Выбрать фото"
              />
              {shownImage && (
                <div style={{ marginTop: 8 }}>
                  <button type="button" className="btn btn-ghost btn-sm" onClick={clearImage}>
                    {imageFile ? "Не загружать выбранное" : "Удалить фото"}
                  </button>
                </div>
              )}
              <div className="hint">JPG, PNG или WebP, до 5 МБ.</div>
              {removeImage && <div className="hint">Фото будет удалено после сохранения.</div>}
              {fieldError("image")}
            </div>
          </div>
        </div>

        <div className="form-field" style={{ maxWidth: 260 }}>
          <label htmlFor="pf-status">Статус</label>
          <select id="pf-status" value={form.status} onChange={set("status")}>
            <option value="FREE">Свободен</option>
            <option value="BOOKED">Забронирован</option>
          </select>
        </div>

        {serverError && <div className="alert alert-error" role="alert">{serverError}</div>}
        {warning && <div className="alert alert-warn" role="alert">{warning}</div>}

        {saved && (
          <div className="alert alert-success" role="status">
            {isEdit ? "Изменения сохранены." : "Товар добавлен."}
            {saved.sku && <> Артикул: <strong>{saved.sku}</strong>.</>}
          </div>
        )}

        <div className="actions">
          <button type="submit" className="btn btn-primary" disabled={saving}>
            {saving ? "Сохраняем…" : isEdit ? "Сохранить" : "Добавить товар"}
          </button>
          <button type="button" className="btn btn-ghost" onClick={() => navigate("/products/catalog")} disabled={saving}>
            {saved ? "В каталог" : "Отмена"}
          </button>
        </div>
      </form>
    </div>
  );
}
