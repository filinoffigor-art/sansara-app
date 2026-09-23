# SANSARA Android App

SANSARA 0.2-alpha — Android/Kotlin/Jetpack Compose.

## Roles
- Client
- Administrator
- Production

## Fixed business rules
- Registration type: Agent / Trading organisation.
- Admin assigns status and discount.
- Statuses: Active, Wholesaler, VIP, Suspended.
- Suspended client cannot place orders and sees admin phone: 8 961 126-66-75.
- Production role only: add model, add SKU/article, enter daily output, post to warehouse.
- Client availability = physical stock - reserve.
- If available stock is zero: `Срок производства: от 3 дней`.
- Orders reserve stock.
- Admin can approve registrations, change status/discount/order access, and move order stages.

## Alpha login
Until Firebase/Apps Script authentication is connected:
- Admin: 8 961 126-66-75
- Production: 8 999 000-00-01
- Any other phone: client

The confirmation-code field is present but not validated in this alpha. This is intentional and will be replaced by server-side/Firebase OTP.

## Build
GitHub Actions builds `app-debug.apk` on every push to `main` and uploads it as artifact `SANSARA-alpha-apk`.

## Backend
`backend/Code.gs` is the Apps Script API. Store `SHEET_ID` and `API_KEY` in Apps Script Script Properties, not in GitHub.
