# Payment Receipt PDF Update

- A formatted one-page PDF receipt is generated immediately after each fee payment is recorded.
- Receipts are retained in the app's private `files/receipts/` directory and named using the receipt number and payment timestamp.
- The PDF includes receipt number, payment date, student/admission/class/roll details, academic year, paid months, base fee, late fee, total paid, and paid status.
- The fee-collection confirmation offers View PDF, Print PDF (Android print service), Thermal Printer (existing Bluetooth thermal printer), and Share PDF.
- Each student card in the Paid List now has a Receipt action. It lists that student's payment receipts and offers the same four actions.
- Older payment entries without a stored PDF can have their PDF generated when opened from the Paid List.
- PDF sharing/viewing uses the existing FileProvider with a narrowly scoped `receipts/` files path. No database schema change or Room migration is required.

Receipts remain available while the app's local data is retained. Uninstalling the app or clearing its storage removes locally stored receipts unless they have been backed up/shared separately.
