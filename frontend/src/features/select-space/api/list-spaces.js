import { apiRequest } from '../../../shared/api/client';

const PAGE_SIZE = 20;

export function listSpaces(page, signal) {
  const query = new URLSearchParams({ page: String(page), size: String(PAGE_SIZE) });
  return apiRequest(`/api/spaces?${query}`, { signal });
}
