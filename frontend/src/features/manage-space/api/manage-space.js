import { apiRequest } from '../../../shared/api/client';

export function createSpace(name) {
  return apiRequest('/api/spaces', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name }),
  });
}

export function renameSpace(id, name) {
  return apiRequest(`/api/spaces/${encodeURIComponent(id)}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name }),
  });
}
