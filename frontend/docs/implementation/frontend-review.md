# Independent frontend review

Reviewed the ledger API contract/progress notes and routed frontend source, including CRUD dialogs, paginated lists/pickers, transaction payloads, query invalidation, reports, export, and shared controls. Static review only; existing tests were not rerun. No application source was changed.

## Findings

### P2 — Nested category creation can select an incompatible category

Location: `src/features/manage-transaction/ui/TransactionDialog.jsx:40`; related `src/features/manage-category/ui/CategoryDialog.jsx:22` and `src/shared/ui/entity-picker.jsx:12`.

Reproduction: Open an Expense transaction, choose Add a category, change the new category's type to Income, and save it. The callback unconditionally assigns that category to the Expense transaction. The picker then prepends the selected record even though it fails the picker's Expense filter, so it appears to be a legitimate selection. Saving the transaction submits an incompatible category and is rejected by the backend. Category type is immutable, so the freshly created category cannot be corrected through rename.

Constrain category type to the parent transaction in this nested workflow, or refuse to auto-select a mismatching category and explain the mismatch. The selected-record injection should respect the requested type as well.

### P2 — Closing shared form dialogs loses the invoking control's keyboard focus

Location: `src/shared/ui/form-dialog.jsx:6`.

Reproduction: Focus Add account using the keyboard, open the dialog, and dismiss it with Escape or Cancel. The caller is not restored as the focused element. The shared dialog is mounted with `open`, without a Radix `Dialog.Trigger` or a custom `onCloseAutoFocus` callback. The installed Radix modal implementation (`node_modules/@radix-ui/react-dialog/dist/index.mjs:174`) prevents default focus restoration and focuses only `triggerRef`, which is unset in this usage. This affects all account/category/transaction/delete forms and interrupts keyboard navigation after each workflow.

Capture the invoking focused element before opening and restore it on close, or integrate the opening buttons as Radix triggers. Preserve an appropriate fallback when a successful delete removes the invoking row.

### P2 — Changing transaction type preserves an out-of-range category-picker page

Location: `src/shared/ui/entity-picker.jsx:8-9`; triggering type change at `src/features/manage-transaction/ui/TransactionDialog.jsx:31`.

Reproduction: With at least 21 Expense categories and one Income category, open an Expense transaction and navigate the category picker to page 2. Change the transaction type to Income. The category picker stays mounted with `page=2` and requests Income page 2, so it presents no category choices even though a valid Income category exists. The user must discover and use Previous choices to recover; more previously visited pages require more clicks.

Reset lookup pagination when the resource/type/space context changes, or remount the picker with a context key. Retaining a search term need not retain a page number that belongs to the old result set.

## Assessment

No critical security or monetary precision defect identified in the inspected frontend. Address the above functional and keyboard issues before calling the workflows complete. Verification of the findings is based on source/data flow and the installed dialog implementation; browser reproduction remains for the ongoing workflow test pass.
