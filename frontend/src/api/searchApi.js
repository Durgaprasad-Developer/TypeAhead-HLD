import axios from 'axios'

/**
 * POST /search
 * Submits a search query, increments its count in memory + batch queue.
 * Returns { message: "Searched" }.
 *
 * @param {string} query
 * @returns {Promise<{ message: string }>}
 */
export const submitSearch = async (query) => {
  const { data } = await axios.post('/search', { query })
  return data
}
