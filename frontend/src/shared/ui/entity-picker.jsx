import { useState } from 'react';
import { useLedgerItem, useLedgerList } from '../../entities/ledger/api.js';
import { Button } from './button';
import { Alert, Field, Input, Select } from './controls';

function PagedEntityPicker({
  spaceId,
  resource,
  label,
  value,
  onChange,
  type,
  required = false,
  allLabel,
  id,
}) {
  const [q, setQ] = useState('');
  const [page, setPage] = useState(1);
  const list = useLedgerList(spaceId, resource, {
    q,
    type,
    page,
    size: 20,
    sort: 'name_asc',
  });
  const selected = useLedgerItem(spaceId, resource, value);
  const items = list.data?.items || [];
  const options =
    selected.data &&
    (!type || selected.data.type === type) &&
    !items.some((item) => item.id === value)
      ? [selected.data, ...items]
      : items;
  return (
    <div className="space-y-2">
      <Field label={`Search ${label.toLowerCase()}`}>
        {(props) => (
          <Input
            {...props}
            value={q}
            placeholder="Search by name…"
            onChange={(e) => {
              setQ(e.target.value);
              setPage(1);
            }}
          />
        )}
      </Field>
      <label htmlFor={id} className="sr-only">
        {label}
      </label>
      <Select
        id={id}
        aria-label={label}
        required={required}
        value={value || ''}
        onChange={(e) => onChange(e.target.value)}
      >
        <option value="">
          {allLabel ||
            (list.isPending ? 'Loading…' : `Select ${label.toLowerCase()}`)}
        </option>
        {value && !options.some((item) => item.id === value) && (
          <option value={value}>
            {selected.isError ? 'Unavailable selection' : 'Loading selection…'}
          </option>
        )}
        {options.map((item) => (
          <option key={item.id} value={item.id}>
            {item.name}
          </option>
        ))}
      </Select>
      {list.isError && (
        <Alert>
          Could not load {resource}.{' '}
          <button
            type="button"
            className="underline"
            onClick={() => list.refetch()}
          >
            Try again
          </button>
        </Alert>
      )}
      {(page > 1 || (list.data && page * 20 < list.data.total)) && (
        <div className="flex items-center gap-2">
          <Button
            type="button"
            variant="ghost"
            disabled={page <= 1}
            onClick={() => setPage(page - 1)}
          >
            Previous choices
          </Button>
          <span className="text-xs text-muted-foreground">Page {page}</span>
          <Button
            type="button"
            variant="ghost"
            disabled={!list.data || page * 20 >= list.data.total}
            onClick={() => setPage(page + 1)}
          >
            More choices
          </Button>
        </div>
      )}
    </div>
  );
}

export function EntityPicker(props) {
  return (
    <PagedEntityPicker
      key={`${props.spaceId}/${props.resource}/${props.type || ''}`}
      {...props}
    />
  );
}
