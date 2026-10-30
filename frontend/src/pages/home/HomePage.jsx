import { SpaceSelector } from '../../features/select-space/ui/SpaceSelector';

export function HomePage({ onSelectSpace }) {
  return (
    <main
      id="main"
      tabIndex={-1}
      className="mx-auto max-w-4xl px-6 py-12 sm:py-20"
    >
      <div className="space-y-8">
        <div>
          <p className="text-xs font-bold tracking-[0.12em] text-primary">
            YOUR WORKSPACE
          </p>
          <h1 className="mt-2 text-4xl font-bold tracking-tight sm:text-5xl">
            Welcome to Portfolio Manager
          </h1>
          <p className="mt-4 max-w-xl leading-relaxed text-muted-foreground">
            Choose a space to view your portfolio.
          </p>
        </div>
        <section
          aria-labelledby="spaces-heading"
          className="rounded-xl border bg-card p-6 shadow-[0_8px_30px_rgba(24,44,39,0.04)] sm:p-8"
        >
          <h2 id="spaces-heading" className="text-lg font-semibold">
            Your spaces
          </h2>
          <p className="mb-6 mt-1 text-sm text-muted-foreground">
            Select a space to continue.
          </p>
          <SpaceSelector onSelect={onSelectSpace} />
        </section>
      </div>
    </main>
  );
}
