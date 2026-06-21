import { useState, useEffect, useCallback } from 'react'
import { fetchCacheDebug } from '../api/cacheApi'
import LoadingSpinner from '../components/LoadingSpinner'
import ErrorMessage from '../components/ErrorMessage'

/**
 * Cache Debug Dashboard page.
 *
 * Provides real-time introspection into the distributed consistent hash ring:
 * - Stats overview (physical nodes, virtual nodes, global cache size).
 * - Prefix routing simulator (hashes and routes query prefixes using MurmurHash3).
 * - Shard visualizer (shows all keys currently cached on each physical CacheNode).
 */
export default function CacheDebug() {
  const [cacheDump, setCacheDump] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  // Simulation states
  const [simPrefix, setSimPrefix] = useState('')
  const [simResult, setSimResult] = useState(null)
  const [simLoading, setSimLoading] = useState(false)

  // Filtering cached keys
  const [keyFilter, setKeyFilter] = useState('')

  // Load cache stats dump
  const loadCacheDump = useCallback(async () => {
    try {
      setLoading(true)
      const data = await fetchCacheDebug()
      setCacheDump(data)
      setError(null)
    } catch (err) {
      console.error('Failed to fetch cache debug stats:', err)
      setError('Could not fetch cache debug info from backend.')
    } finally {
      setLoading(false)
    }
  }, [])

  // Run routing simulation
  const runSimulation = useCallback(async (prefix) => {
    if (!prefix || prefix.trim() === '') {
      setSimResult(null)
      return
    }
    try {
      setSimLoading(true)
      const result = await fetchCacheDebug(prefix.trim())
      setSimResult(result)
    } catch (err) {
      console.error('Failed to run route simulation:', err)
    } finally {
      setSimLoading(false)
    }
  }, [])

  useEffect(() => {
    loadCacheDump()
  }, [loadCacheDump])

  // Trigger routing simulation when user types (debounced or on change)
  useEffect(() => {
    const timer = setTimeout(() => {
      runSimulation(simPrefix)
    }, 250)
    return () => clearTimeout(timer)
  }, [simPrefix, runSimulation])

  if (loading && !cacheDump) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh] gap-4">
        <LoadingSpinner />
        <p className="text-sm" style={{ color: 'var(--color-muted)' }}>
          Loading consistent hash ring state...
        </p>
      </div>
    )
  }

  // Calculate aggregate metrics
  const nodes = cacheDump?.nodes || []
  const hashRingSize = cacheDump?.hashRingSize || 0
  const totalCachedKeys = nodes.reduce((sum, node) => sum + (node.cacheSize || 0), 0)

  return (
    <div className="max-w-7xl mx-auto px-4 py-8 flex flex-col gap-8 animate-fade-in">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b pb-6" style={{ borderColor: 'var(--color-border)' }}>
        <div>
          <h1 className="text-3xl font-extrabold text-white tracking-tight">Consistent Hash Ring Dashboard</h1>
          <p className="text-sm mt-1" style={{ color: 'var(--color-muted)' }}>
            Real-time inspection of sharded CacheNodes, virtual nodes, and prefix routing.
          </p>
        </div>
        <button
          onClick={loadCacheDump}
          className="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 active:bg-indigo-700 text-white rounded-lg font-medium transition-colors text-sm shadow-md flex items-center gap-2 cursor-pointer self-start md:self-auto"
        >
          🔄 Refresh Shards
        </button>
      </div>

      {error && <ErrorMessage message={error} />}

      {/* Grid: Metrics Overview */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="p-5 rounded-xl border flex flex-col gap-1 shadow-sm" style={{ background: 'var(--color-surface)', borderColor: 'var(--color-border)' }}>
          <span className="text-xs font-semibold uppercase tracking-wider" style={{ color: 'var(--color-muted)' }}>Physical Shards</span>
          <span className="text-2xl font-black text-white">{nodes.length}</span>
          <span className="text-xs" style={{ color: 'var(--color-success)' }}>● Cluster Status: Online</span>
        </div>
        <div className="p-5 rounded-xl border flex flex-col gap-1 shadow-sm" style={{ background: 'var(--color-surface)', borderColor: 'var(--color-border)' }}>
          <span className="text-xs font-semibold uppercase tracking-wider" style={{ color: 'var(--color-muted)' }}>Virtual Nodes</span>
          <span className="text-2xl font-black text-white">{hashRingSize}</span>
          <span className="text-xs font-medium" style={{ color: 'var(--color-accent)' }}>150 vnodes per physical node</span>
        </div>
        <div className="p-5 rounded-xl border flex flex-col gap-1 shadow-sm" style={{ background: 'var(--color-surface)', borderColor: 'var(--color-border)' }}>
          <span className="text-xs font-semibold uppercase tracking-wider" style={{ color: 'var(--color-muted)' }}>Global Cache Size</span>
          <span className="text-2xl font-black text-white">{totalCachedKeys}</span>
          <span className="text-xs" style={{ color: 'var(--color-muted)' }}>Active cached prefixes</span>
        </div>
        <div className="p-5 rounded-xl border flex flex-col gap-1 shadow-sm" style={{ background: 'var(--color-surface)', borderColor: 'var(--color-border)' }}>
          <span className="text-xs font-semibold uppercase tracking-wider" style={{ color: 'var(--color-muted)' }}>Hashing Algorithm</span>
          <span className="text-2xl font-black text-white">MurmurHash3</span>
          <span className="text-xs" style={{ color: 'var(--color-muted)' }}>32-bit unsigned ring mapping</span>
        </div>
      </div>

      {/* Prefix Routing Simulator Section */}
      <div className="p-6 rounded-xl border shadow-md flex flex-col gap-6" style={{ background: 'var(--color-surface)', borderColor: 'var(--color-border)' }}>
        <div>
          <h2 className="text-xl font-bold text-white">Prefix Routing Simulator</h2>
          <p className="text-xs mt-1" style={{ color: 'var(--color-muted)' }}>
            Type any prefix query to visually trace its route and see which physical shard owns the cache keys.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-12 gap-6 items-start">
          <div className="md:col-span-5 flex flex-col gap-2">
            <label className="text-xs font-semibold uppercase tracking-wider" style={{ color: 'var(--color-muted)' }}>Prefix String</label>
            <input
              type="text"
              placeholder="e.g. apple, test, ca"
              value={simPrefix}
              onChange={(e) => setSimPrefix(e.target.value)}
              className="px-4 py-3 rounded-lg border text-white font-medium focus:outline-none transition-all placeholder:font-normal text-sm"
              style={{
                background: 'var(--color-bg)',
                borderColor: simPrefix ? 'var(--color-accent)' : 'var(--color-border)',
              }}
            />
          </div>

          <div className="md:col-span-7 flex flex-col gap-3">
            <span className="text-xs font-semibold uppercase tracking-wider" style={{ color: 'var(--color-muted)' }}>Routing Output</span>
            {simLoading ? (
              <div className="py-4 text-xs italic" style={{ color: 'var(--color-muted)' }}>Calculating Murmur3 ring slot...</div>
            ) : simResult ? (
              <div className="p-4 rounded-lg flex flex-col gap-3 text-sm border" style={{ background: 'var(--color-bg)', borderColor: 'var(--color-border)' }}>
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <span className="font-semibold text-white">Routed Shard:</span>
                  <span className="px-3 py-1 bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 rounded-md font-bold text-xs uppercase">
                    ⚡ {simResult.assignedNodeName || 'None'} ({simResult.assignedNode || 'None'})
                  </span>
                </div>
                <div className="flex justify-between items-center text-xs">
                  <span style={{ color: 'var(--color-muted)' }}>Ring Unsigned Hash:</span>
                  <span className="font-mono text-white bg-slate-800 px-2 py-0.5 rounded">{simResult.hash}</span>
                </div>
                <div className="flex justify-between items-center text-xs border-t pt-2" style={{ borderColor: 'var(--color-border)' }}>
                  <span style={{ color: 'var(--color-muted)' }}>Cache Status:</span>
                  {simResult.isCached ? (
                    <span className="text-emerald-400 font-bold">HIT (Cached suggestions exist)</span>
                  ) : (
                    <span className="text-amber-400 font-bold">MISS (Not cached yet)</span>
                  )}
                </div>
                {simResult.isCached && simResult.suggestions && simResult.suggestions.length > 0 && (
                  <div className="flex flex-col gap-1 border-t pt-2" style={{ borderColor: 'var(--color-border)' }}>
                    <span className="text-xs font-semibold" style={{ color: 'var(--color-muted)' }}>Cached Suggestions list:</span>
                    <div className="flex flex-wrap gap-1 mt-1">
                      {simResult.suggestions.map((s, idx) => (
                        <span key={idx} className="px-2 py-0.5 bg-slate-800 text-slate-200 text-xs rounded font-mono">
                          {s}
                        </span>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            ) : (
              <div className="p-4 rounded-lg text-xs italic text-center border" style={{ background: 'var(--color-bg)', borderColor: 'var(--color-border)', color: 'var(--color-muted)' }}>
                Enter a prefix to run consistent hashing calculations.
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Shards View Section */}
      <div className="flex flex-col gap-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h2 className="text-xl font-bold text-white">Physical Shards Introspection</h2>
            <p className="text-xs mt-1" style={{ color: 'var(--color-muted)' }}>
              Explore the exact content stored in the local cache store of each server node.
            </p>
          </div>
          {/* Key Filtering Input */}
          <div className="flex items-center gap-2">
            <span className="text-xs font-medium shrink-0" style={{ color: 'var(--color-muted)' }}>Filter Keys:</span>
            <input
              type="text"
              placeholder="Search keys across shards..."
              value={keyFilter}
              onChange={(e) => setKeyFilter(e.target.value)}
              className="px-3 py-1.5 rounded-lg border text-white text-xs font-medium focus:outline-none transition-all placeholder:font-normal"
              style={{
                background: 'var(--color-surface)',
                borderColor: keyFilter ? 'var(--color-accent)' : 'var(--color-border)',
              }}
            />
          </div>
        </div>

        {/* Shard Cards Grid */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {nodes.map((node) => {
            const isRoutedTarget = simResult && simResult.assignedNode === node.nodeId
            const entries = node.entries || {}
            
            // Filter keys
            const filteredKeys = Object.keys(entries).filter(k => 
              k.toLowerCase().includes(keyFilter.toLowerCase())
            )

            return (
              <div
                key={node.nodeId}
                className={`p-6 rounded-xl border flex flex-col gap-4 shadow-sm transition-all duration-300 ${
                  isRoutedTarget ? 'ring-2 ring-indigo-500 scale-[1.02] shadow-indigo-500/10' : ''
                }`}
                style={{
                  background: 'var(--color-surface)',
                  borderColor: isRoutedTarget ? 'var(--color-accent)' : 'var(--color-border)',
                }}
              >
                {/* Node Identity */}
                <div className="flex items-center justify-between border-b pb-3" style={{ borderColor: 'var(--color-border)' }}>
                  <div>
                    <h3 className="font-extrabold text-white">{node.nodeName}</h3>
                    <span className="text-[10px] font-mono" style={{ color: 'var(--color-muted)' }}>ID: {node.nodeId}</span>
                  </div>
                  {isRoutedTarget ? (
                    <span className="px-2 py-0.5 bg-indigo-500/20 text-indigo-400 text-[10px] rounded font-extrabold animate-pulse uppercase">
                      Routed Target
                    </span>
                  ) : (
                    <span className="text-[11px] font-semibold" style={{ color: 'var(--color-muted)' }}>
                      {filteredKeys.length} key(s)
                    </span>
                  )}
                </div>

                {/* Node Entries List */}
                <div className="flex-1 overflow-y-auto max-h-[300px] pr-1 flex flex-col gap-2 min-h-[120px]">
                  {filteredKeys.length > 0 ? (
                    filteredKeys.map((key) => (
                      <div
                        key={key}
                        className="p-3 rounded-lg border flex flex-col gap-1.5"
                        style={{ background: 'var(--color-bg)', borderColor: 'var(--color-border)' }}
                      >
                        <div className="flex justify-between items-center">
                          <span className="font-bold text-white text-xs font-mono bg-indigo-950 px-2 py-0.5 rounded text-indigo-300">
                            "{key}"
                          </span>
                          <span className="text-[10px]" style={{ color: 'var(--color-muted)' }}>
                            {entries[key]?.length || 0} suggestions
                          </span>
                        </div>
                        <div className="flex flex-wrap gap-1">
                          {entries[key]?.map((val, idx) => (
                            <span key={idx} className="px-1.5 py-0.5 bg-slate-800 text-slate-300 text-[10px] rounded">
                              {val}
                            </span>
                          ))}
                        </div>
                      </div>
                    ))
                  ) : (
                    <div className="flex-1 flex flex-col items-center justify-center text-center p-6">
                      <span className="text-xl">📭</span>
                      <p className="text-[11px] mt-1" style={{ color: 'var(--color-muted)' }}>
                        {keyFilter ? 'No matching keys in this shard.' : 'No entries cached in this shard.'}
                      </p>
                    </div>
                  )}
                </div>
              </div>
            )
          })}
        </div>
      </div>
    </div>
  )
}
