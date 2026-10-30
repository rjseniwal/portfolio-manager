export function toSearchParams(values) {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(values)) {
    if (value !== null && value !== undefined && value !== '')
      params.set(key, String(value));
  }
  return params;
}

function positiveInteger(value, fallback) {
  if (value === null || value.length === 0 || /\D/.test(value)) return fallback;
  const number = Number(value);
  return Number.isSafeInteger(number) && number >= 1 ? number : fallback;
}

/** Normalize shared collection query parameters; each collection validates sort. */
export function normalizeListParams(search, { sort = 'date_desc' } = {}) {
  const values = {};
  for (const key of ['q', 'type', 'accountId', 'categoryId', 'from', 'to']) {
    const value = search.get(key);
    if (value !== null && value !== '') values[key] = value;
  }
  values.page = positiveInteger(search.get('page'), 1);
  const size = positiveInteger(search.get('size'), 20);
  values.size = [20, 50, 100].includes(size) ? size : 20;
  values.sort = search.get('sort') || sort;
  return values;
}
