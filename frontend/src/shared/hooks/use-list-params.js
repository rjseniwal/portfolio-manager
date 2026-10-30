import { useCallback, useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { normalizeListParams } from '../lib/filters.js';

export function useListParams(sort = 'date_desc') {
  const [search, setSearch] = useSearchParams();
  const params = normalizeListParams(search, { sort });
  const update = useCallback(
    (patch, replace = false) => {
      setSearch(
        (previous) => {
          const next = new URLSearchParams(previous);
          for (const [key, value] of Object.entries(patch)) {
            if (value === '' || value == null) next.delete(key);
            else next.set(key, String(value));
          }
          if (!Object.hasOwn(patch, 'page')) next.delete('page');
          return next;
        },
        { replace },
      );
    },
    [setSearch],
  );
  return [params, update];
}
export function useSearchDraft(value, onChange) {
  const [draft, setDraft] = useState(value || '');
  useEffect(() => {
    setDraft(value || '');
  }, [value]);
  useEffect(() => {
    if (draft === (value || '')) return;
    const timer = setTimeout(() => onChange(draft), 300);
    return () => clearTimeout(timer);
  }, [draft, value, onChange]);
  return [draft, setDraft];
}
