# SANSARA server table

The live source of truth stays normalized. Monthly sheets are reports/views, not independent stock ledgers.

- Products — model/SKU catalog and lead time (`ProductionLeadDays`, default 3)
- Clients — type, status, discount, ordering permission
- ClientUsers — people within an organisation
- StockMovements — all production receipts, reservations, releases and shipments
- Orders / OrderItems — orders and their lines
- AuditLog — admin changes

Live availability: `Available = Physical - Reserved`.
If Available = 0, the client app shows `Срок производства: от 3 дней` (or the product-specific lead time).
