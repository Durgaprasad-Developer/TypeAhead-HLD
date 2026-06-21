import axios from 'axios'

/**
 * GET /cache/debug?prefix=<prefix>
 * Returns per-prefix routing info: assigned node, hash value, cache hit/miss, cached suggestions.
 *
 * GET /cache/debug (no param)
 * Returns aggregate cache stats: hit rate, miss rate, per-node key counts, writes avoided.
 *
 * @param {string} [prefix]
 * @returns {Promise<object>}
 */
export const fetchCacheDebug = async (prefix) => {
  const params = prefix ? { prefix } : {}
  const { data } = await axios.get('/cache/debug', { params })
  return data
}
