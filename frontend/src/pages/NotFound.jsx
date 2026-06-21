/**
 * 404 Not Found page.
 */
export default function NotFound() {
  return (
    <div className="flex flex-col items-center justify-center min-h-[70vh] gap-4 px-4">
      <h1 className="text-6xl font-bold" style={{ color: 'var(--color-accent)' }}>404</h1>
      <p className="text-xl text-white">Page not found.</p>
      <a
        href="/"
        className="mt-4 px-6 py-2 rounded-lg font-semibold text-white transition"
        style={{ background: 'var(--color-accent)' }}
      >
        Go Home
      </a>
    </div>
  )
}
