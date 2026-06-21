import axios from 'axios'

/**
 * GET /suggest?q=<prefix>
 * Returns up to 10 prefix-matching suggestions sorted by trending score.
 * Implemented in Milestone 6; this file is the correct import target for all components.
 *
 * @param {string} prefix
 * @returns {Promise<string[]>}
 */
export const fetchSuggestions = async (prefix) => {
  if (!prefix || prefix.trim() === '') return []
  const { data } = await axios.get('/suggest', { params: { q: prefix.trim() } })
  return data
}
