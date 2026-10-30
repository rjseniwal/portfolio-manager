import { useState } from 'react';
import { Button } from '../../../shared/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '../../../shared/ui/dialog';
import { createSpace, renameSpace } from '../api/manage-space';
import { ApiError } from '../../../shared/api/client';

export function SpaceNameDialog({ space, onClose, onSaved }) {
  const [name, setName] = useState(space?.name ?? '');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const isRename = Boolean(space);

  async function handleSubmit(event) {
    event.preventDefault();
    if (saving) return;
    const trimmedName = name.trim();
    if (!trimmedName) {
      setError('Enter a space name.');
      return;
    }
    if (Array.from(trimmedName).length > 255) {
      setError('Space names must be at most 255 characters.');
      return;
    }
    setError('');
    setSaving(true);
    try {
      const saved = isRename
        ? await renameSpace(space.id, trimmedName)
        : await createSpace(trimmedName);
      onSaved(saved);
    } catch (requestError) {
      setError(
        requestError instanceof ApiError
          ? requestError.message
          : 'Could not reach the backend. Please check your connection and try again.',
      );
      setSaving(false);
    }
  }

  return (
    <Dialog
      open
      onOpenChange={(open) => {
        if (!open && !saving) onClose();
      }}
    >
      <DialogContent
        closeDisabled={saving}
        onEscapeKeyDown={(event) => {
          if (saving) event.preventDefault();
        }}
        onPointerDownOutside={(event) => {
          if (saving) event.preventDefault();
        }}
      >
        <form onSubmit={handleSubmit} className="space-y-5">
          <div className="space-y-1.5">
            <DialogTitle className="text-lg font-semibold leading-none tracking-tight">
              {isRename ? 'Rename space' : 'Add a space'}
            </DialogTitle>
            <DialogDescription className="text-sm text-muted-foreground">
              Enter a name for your space.
            </DialogDescription>
          </div>
          {error && (
            <div
              role="alert"
              className="rounded-md border border-destructive/30 bg-destructive-background p-3 text-sm text-destructive"
            >
              {error}
            </div>
          )}
          <div className="space-y-2">
            <label htmlFor="space-name" className="text-sm font-medium">
              Space name
            </label>
            <input
              id="space-name"
              autoFocus
              required
              value={name}
              onChange={(event) => {
                setName(event.target.value);
                setError('');
              }}
              disabled={saving}
              aria-describedby="space-name-help"
              className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-sm outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
            />
            <p id="space-name-help" className="text-sm text-muted-foreground">
              Up to 255 characters
            </p>
          </div>
          <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
            <Button
              type="button"
              variant="outline"
              onClick={onClose}
              disabled={saving}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={saving}>
              {saving ? 'Saving…' : isRename ? 'Save' : 'Add space'}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
