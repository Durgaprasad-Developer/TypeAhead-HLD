/**
 * Home page — Search page (/).
 * Placeholder for Milestone 1; real search UI implemented in Milestone 6.
 */
export default function Home() {
  return (
    <div className="flex flex-col items-center justify-center min-h-[70vh] gap-6 px-4">
      <h1 className="text-4xl font-bold text-white tracking-tight">
        Search <span style={{ color: 'var(--color-accent)' }}>Typeahead</span>
      </h1>
      <p style={{ color: 'var(--color-muted)' }} className="text-lg text-center max-w-md">
        Backend connected. Search UI coming in Milestone 6.
      </p>
      <div
        className="w-full max-w-lg h-12 rounded-xl"
        style={{ background: 'var(--color-surface)', border: '1px solid var(--color-border)' }}
      />
    </div>
  )
}
