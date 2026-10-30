import { apiRequest } from '../../../shared/api/client';

export function getBackendStatus(signal) {
  return apiRequest('/status', { signal });
}
