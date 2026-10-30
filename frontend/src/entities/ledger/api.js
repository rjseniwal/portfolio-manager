import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiRequest } from '../../shared/api/client.js';
import { toSearchParams } from '../../shared/lib/filters.js';

/** @typedef {{items: Array<object>, page: number, size: number, total: number}} ResourcePage */
/** @typedef {'BANK'|'CASH'} AccountType */
/** @typedef {'INCOME'|'EXPENSE'|'TRANSFER'} TransactionType */
/** Monetary properties are decimal strings; dates are YYYY-MM-DD. */
export const spacePath = (spaceId) =>
  `/api/spaces/${encodeURIComponent(spaceId)}`;
export const resourcePath = (spaceId, resource, id) =>
  `${spacePath(spaceId)}/${resource}${id ? `/${encodeURIComponent(id)}` : ''}`;

export function useSpace(spaceId) {
  return useQuery({
    queryKey: ['space', spaceId, 'detail'],
    queryFn: ({ signal }) => apiRequest(spacePath(spaceId), { signal }),
    enabled: Boolean(spaceId),
  });
}
export function useLedgerList(spaceId, resource, params = {}, enabled = true) {
  return useQuery({
    queryKey: ['space', spaceId, resource, 'list', params],
    queryFn: ({ signal }) =>
      apiRequest(
        `${resourcePath(spaceId, resource)}?${toSearchParams(params)}`,
        { signal },
      ),
    enabled: Boolean(spaceId) && enabled,
  });
}
export function useLedgerItem(spaceId, resource, id) {
  return useQuery({
    queryKey: ['space', spaceId, resource, 'detail', id],
    queryFn: ({ signal }) =>
      apiRequest(resourcePath(spaceId, resource, id), { signal }),
    enabled: Boolean(spaceId && id),
  });
}
export function useBalance(spaceId, id) {
  return useQuery({
    queryKey: ['space', spaceId, 'balance', id],
    queryFn: ({ signal }) =>
      apiRequest(`${resourcePath(spaceId, 'accounts', id)}/balance`, {
        signal,
      }),
    enabled: Boolean(id),
  });
}
export function useReport(spaceId, kind, range, enabled = true) {
  return useQuery({
    queryKey: ['space', spaceId, 'reports', kind, range],
    enabled,
    queryFn: ({ signal }) =>
      apiRequest(
        `${spacePath(spaceId)}/reports/${kind}?${toSearchParams(range)}`,
        { signal },
      ),
  });
}
export function useLedgerMutation(spaceId, resource) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data, remove = false }) =>
      apiRequest(resourcePath(spaceId, resource, id), {
        method: remove ? 'DELETE' : id ? 'PATCH' : 'POST',
        ...(remove ? {} : { json: data }),
      }),
    onSuccess: (record, variables) => {
      if (variables.remove)
        client.removeQueries({
          queryKey: ['space', spaceId, resource, 'detail', variables.id],
        });
      else
        client.setQueryData(
          ['space', spaceId, resource, 'detail', record.id],
          record,
        );
      // Refetch errors belong to the views, not to an already successful write.
      void client.invalidateQueries({ queryKey: ['space', spaceId] });
    },
  });
}
