# Thermal fee receipt printing update

- Fee collection now generates a detailed receipt containing school name/location, receipt number, payment date, student/admission/class/roll details, academic year, months covered, base fee, late fee, total paid, and payment status.
- After a payment is saved, the receipt dialog offers WhatsApp sharing, sharing through other apps, and direct Bluetooth thermal printing.
- Direct printing uses ESC/POS over the standard Bluetooth SPP profile. Pair the 58mm/80mm ESC/POS printer in Android Bluetooth settings before printing. The printer must support SPP; BLE-only printers are not supported by this implementation.
- Android 12+ asks for the Nearby devices/Bluetooth Connect permission. After granting it, tap Print receipt again.
- The dashboard Track Payment, Approved Marksheet, Set Exams, and Publish Result cards have a larger minimum height and increased internal padding to reduce text clipping on compact displays. Cards remain wrap-content and can grow beyond the minimum height.
- The hamburger menu has moved to the right side of the toolbar.
