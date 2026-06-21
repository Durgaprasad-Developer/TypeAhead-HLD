import { Link, useLocation } from 'react-router-dom'

/**
 * Global navigation bar.
 * Active route is highlighted with the accent colour.
 */
export default function Navbar() {
  const { pathname } = useLocation()

  const navLink = (to, label) => (
    <Link
      to={to}
      className="px-4 py-2 rounded-lg font-medium transition-colors duration-150"
      style={{
        color: pathname === to ? 'var(--color-accent)' : 'var(--color-muted)',
        background: pathname === to ? 'rgba(108,99,255,0.12)' : 'transparent',
      }}
    >
      {label}
    </Link>
  )

  return (
    <nav
      className="flex items-center justify-between px-6 py-3 border-b"
      style={{ background: 'var(--color-surface)', borderColor: 'var(--color-border)' }}
    >
      <Link to="/" className="text-lg font-bold text-white tracking-tight">
        ⚡ TypeAhead
      </Link>
      <div className="flex gap-2">
        {navLink('/', 'Search')}
        {navLink('/cache', 'Cache Debug')}
      </div>
    </nav>
  )
}
