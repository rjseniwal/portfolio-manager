import { useEffect, useState } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';
import { useReport } from '../../entities/ledger/api.js';
import { dateRange, monthToDate, validDate } from '../../shared/lib/dates.js';
import { Button } from '../../shared/ui/button';
import {
  Alert,
  Field,
  Input,
  PageTitle,
  Panel,
  QueryState,
  Select,
} from '../../shared/ui/controls';
import {
  CategoryBreakdown,
  MonthlyChart,
  SummaryCards,
} from '../../features/reporting/ui/ReportViews';

export function ReportsPage() {
  const { spaceId } = useParams();
  const [search, setSearch] = useSearchParams();
  const defaults = monthToDate();
  const from = search.get('from') || defaults.from;
  const to = search.get('to') || defaults.to;
  const valid = validDate(from) && validDate(to) && from <= to;
  const range = { from, to };
  const [draft, setDraft] = useState(range);
  const [error, setError] = useState('');
  useEffect(() => {
    setDraft({ from, to });
  }, [from, to]);
  const summary = useReport(spaceId, 'summary', range, valid);
  const monthly = useReport(spaceId, 'monthly', range, valid);
  function apply(event) {
    event.preventDefault();
    if (
      !validDate(draft.from) ||
      !validDate(draft.to) ||
      draft.from > draft.to
    ) {
      setError('Choose valid dates with the start on or before the end.');
      return;
    }
    setError('');
    setSearch(draft);
  }
  return (
    <>
      <PageTitle
        title="Reports"
        description="Income, expenses, and net cash flow for your selected dates. Transfers and starting balances are excluded."
      />
      <Panel className="mb-6">
        <form onSubmit={apply} className="flex flex-wrap items-end gap-4">
          <div className="min-w-40">
            <Field label="Date range">
              {(p) => (
                <Select
                  {...p}
                  value={
                    search.get('preset') ||
                    (search.has('from') || search.has('to')
                      ? 'custom'
                      : 'month')
                  }
                  onChange={(e) => {
                    if (e.target.value === 'custom') {
                      setSearch({ from, to, preset: 'custom' });
                      return;
                    }
                    const next = dateRange(e.target.value);
                    setError('');
                    setDraft(next);
                    setSearch({ ...next, preset: e.target.value });
                  }}
                >
                  <option value="month">This month</option>
                  <option value="30days">Last 30 days</option>
                  <option value="year">This year</option>
                  <option value="custom">Custom range</option>
                </Select>
              )}
            </Field>
          </div>
          <Field label="Report from">
            {(p) => (
              <Input
                {...p}
                type="date"
                required
                value={draft.from}
                onChange={(e) => setDraft({ ...draft, from: e.target.value })}
              />
            )}
          </Field>
          <Field label="Report to">
            {(p) => (
              <Input
                {...p}
                type="date"
                required
                value={draft.to}
                onChange={(e) => setDraft({ ...draft, to: e.target.value })}
              />
            )}
          </Field>
          <Button type="submit">Apply dates</Button>
        </form>
        {(error || !valid) && (
          <div className="mt-4">
            <Alert>
              {error ||
                'The date range in this URL is invalid. Choose new dates.'}
            </Alert>
          </div>
        )}
      </Panel>
      {valid && (
        <>
          <QueryState query={summary}>
            {summary.data && (
              <>
                <SummaryCards data={summary.data} />
                <p className="my-4 text-sm text-muted-foreground">
                  {summary.data.transactionCount} transactions · {from} to {to}
                </p>
              </>
            )}
          </QueryState>
          <div className="mt-6 grid gap-6 lg:grid-cols-2">
            <Panel>
              <h3 className="mb-5 text-lg font-semibold">Monthly cash flow</h3>
              <QueryState query={monthly}>
                {monthly.data && <MonthlyChart items={monthly.data.items} />}
              </QueryState>
            </Panel>
            <Panel>
              <h3 className="mb-5 text-lg font-semibold">Category breakdown</h3>
              <QueryState query={summary}>
                {summary.data && (
                  <CategoryBreakdown items={summary.data.categoryTotals} />
                )}
              </QueryState>
            </Panel>
          </div>
        </>
      )}
    </>
  );
}
