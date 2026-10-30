import { useId } from 'react';
import { LoaderCircle } from 'lucide-react';
import { Button } from './button';
import { cn } from './utils';

const inputClass =
  'flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50';
export function Input({ className, ...props }) {
  return <input className={cn(inputClass, className)} {...props} />;
}
export function Select({ className, children, ...props }) {
  return (
    <select className={cn(inputClass, 'pr-8', className)} {...props}>
      {children}
    </select>
  );
}
export function Field({ label, help, children }) {
  const id = useId();
  return (
    <div className="space-y-2">
      <label className="block text-sm font-medium" htmlFor={id}>
        {label}
      </label>
      {typeof children === 'function'
        ? children({ id, 'aria-describedby': help ? `${id}-help` : undefined })
        : children}
      {help && (
        <p
          id={`${id}-help`}
          className="text-xs leading-relaxed text-muted-foreground"
        >
          {help}
        </p>
      )}
    </div>
  );
}
export function Alert({ children }) {
  return (
    <div
      role="alert"
      className="rounded-lg border border-destructive/30 bg-destructive-background p-4 text-sm text-destructive"
    >
      {children}
    </div>
  );
}
export function Panel({ children, className }) {
  return (
    <section
      className={cn(
        'rounded-xl border bg-card p-5 shadow-sm sm:p-6',
        className,
      )}
    >
      {children}
    </section>
  );
}
export function PageTitle({ title, description, action }) {
  return (
    <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
      <div>
        <h2 className="text-2xl font-semibold tracking-tight">{title}</h2>
        {description && (
          <p className="mt-1 max-w-2xl text-sm leading-relaxed text-muted-foreground">
            {description}
          </p>
        )}
      </div>
      {action}
    </div>
  );
}
export function EmptyState({ title = 'Nothing here yet', children, action }) {
  return (
    <div className="rounded-lg border border-dashed p-8 text-center">
      <h3 className="font-semibold">{title}</h3>
      <div className="mt-2 text-sm text-muted-foreground">{children}</div>
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}
export function QueryState({ query, children }) {
  if (query.isPending)
    return (
      <div
        role="status"
        className="flex items-center gap-2 py-6 text-sm text-muted-foreground"
      >
        <LoaderCircle className="size-4 animate-spin" />
        Loading…
      </div>
    );
  if (query.isError && !query.data)
    return (
      <div className="space-y-3">
        <Alert>
          {query.error.status === 404
            ? 'This record could not be found in this space.'
            : query.error.message || 'Could not reach the backend.'}
        </Alert>
        <Button variant="outline" onClick={() => query.refetch()}>
          Try again
        </Button>
      </div>
    );
  return (
    <>
      {query.isError && (
        <Alert>
          Could not refresh this view. The information below may be out of date.{' '}
          <button className="underline" onClick={() => query.refetch()}>
            Try again
          </button>
        </Alert>
      )}
      {children}
    </>
  );
}
export function Pagination({ data, onChange, label = 'Results' }) {
  if (!data) return null;
  const last = Math.max(1, Math.ceil(data.total / data.size));
  return (
    <div className="mt-5 flex flex-wrap items-center justify-between gap-3 border-t pt-4 text-sm">
      <span className="text-muted-foreground">
        {data.total} {label.toLowerCase()}
      </span>
      <nav aria-label={`${label} pages`} className="flex items-center gap-2">
        <Button
          variant="outline"
          disabled={data.page <= 1}
          onClick={() => onChange(data.page - 1)}
        >
          Previous
        </Button>
        <span className="px-1">
          Page {data.page} of {last}
        </span>
        <Button
          variant="outline"
          disabled={data.page >= last}
          onClick={() => onChange(data.page + 1)}
        >
          Next
        </Button>
        {data.page > last && (
          <Button variant="ghost" onClick={() => onChange(last)}>
            Go to last page
          </Button>
        )}
      </nav>
    </div>
  );
}
export function Table({ headings, children, caption }) {
  return (
    <div className="overflow-x-auto rounded-lg border">
      <table className="w-full text-left text-sm">
        {caption && <caption className="sr-only">{caption}</caption>}
        <thead className="border-b bg-muted/60">
          <tr>
            {headings.map((heading) => (
              <th
                key={heading}
                scope="col"
                className="whitespace-nowrap px-4 py-3 font-medium text-muted-foreground"
              >
                {heading}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y">{children}</tbody>
      </table>
    </div>
  );
}
export function Cell({ children, className }) {
  return <td className={cn('px-4 py-3 align-top', className)}>{children}</td>;
}
export function Badge({ children }) {
  return (
    <span className="inline-flex rounded-full border bg-muted px-2 py-0.5 text-xs font-medium capitalize">
      {children?.toLowerCase()}
    </span>
  );
}
