import { useCallback, useEffect, useState } from 'react';
import { LoaderCircle } from 'lucide-react';
import { Button } from '../../../shared/ui/button';
import { getBackendStatus } from '../api/get-status';

export function BackendStatus() {
  const [state, setState] = useState({ phase: 'loading' });
  const [attempt, setAttempt] = useState(0);
  const retry = useCallback(() => setAttempt((value) => value + 1), []);

  useEffect(() => {
    const controller = new AbortController();
    setState({ phase: 'loading' });
    getBackendStatus(controller.signal)
      .then((data) => setState(data?.status === 'ok' ? { phase: 'connected' } : { phase: 'error' }))
      .catch((error) => {
        if (error.name !== 'AbortError') setState({ phase: 'error' });
      });
    return () => controller.abort();
  }, [attempt]);

  return (
    <section aria-labelledby="connection-heading">
      <div className="mb-4 flex items-center justify-between gap-4">
        <h2 id="connection-heading" className="text-lg font-semibold">Backend connection</h2>
        {state.phase === 'connected' && <span className="rounded-full bg-success-background px-2.5 py-0.5 text-xs font-semibold text-success">Connected</span>}
      </div>
      {state.phase === 'loading' && <div className="flex items-center gap-3 text-sm text-muted-foreground" role="status"><LoaderCircle className="size-4 animate-spin" />Checking connection…</div>}
      {state.phase === 'connected' && <p className="text-sm text-muted-foreground">The backend is responding and ready to use.</p>}
      {state.phase === 'error' && <div className="space-y-4"><div role="alert" className="rounded-md border border-warning-border bg-warning-background p-4 text-sm text-warning">Could not connect to the backend. Check that it is running and the backend address is configured correctly.</div><Button variant="outline" onClick={retry}>Try again</Button></div>}
    </section>
  );
}
