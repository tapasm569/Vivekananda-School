# Firebase data-storage readiness

This build remains fully offline and continues to use Room as its working database. Firebase is intentionally not activated until the school supplies its own Firebase project configuration (`google-services.json`) and approves the authentication/security rules. No credentials or Firebase project identifiers are embedded in this source.

## Proposed future Firestore collections

- `schools/{schoolId}` — school profile and configuration.
- `schools/{schoolId}/students/{studentId}` — student records; keep a stable cloud document ID separate from the local Room auto-increment ID.
- `schools/{schoolId}/teachers/{teacherId}` — teacher records.
- `schools/{schoolId}/classes/{classId}` — class and section configuration.
- `schools/{schoolId}/feePayments/{paymentId}` — immutable payment/receipt records.
- `schools/{schoolId}/exams/{examId}` and nested `subjects` — exam configuration.
- `schools/{schoolId}/examResults/{resultId}` — marks and approval state.
- `schools/{schoolId}/lateFeeRules/{ruleId}` — fee rules.

## Required implementation before enabling cloud sync

1. Add Firebase BoM, Firebase Authentication, Cloud Firestore and optionally Cloud Storage dependencies; apply Google Services Gradle plugin only after receiving the real project file.
2. Add stable UUID/cloud IDs and sync metadata (`syncStatus`, `updatedAt`, `deletedAt`, `lastSyncedAt`) to records through explicit Room migrations. Never use local auto-generated numeric IDs as cross-device IDs.
3. Introduce repository interfaces between UI and Room, then implement Room-local and Firebase remote data sources.
4. Use an outbox/queue for offline writes; sync idempotently by stable document ID and retain local data until server acknowledgement.
5. Define conflict policy (server timestamps plus explicit field-level rules), soft deletes, retry/backoff and sync status UI.
6. Lock Firestore rules to authenticated users belonging to the matching school. Enforce admin/teacher roles server-side; never trust a role stored only in SharedPreferences.
7. Store student photos in Firebase Storage and persist storage paths, not device `content://` URIs.
8. Test migration, offline mode, duplicate receipt prevention, multi-device conflicts, account removal and backup/restore before enabling production sync.

## Local database safety

The destructive Room migration fallback has been removed. Any future entity/schema change must include a tested `Migration` and a database backup/restore test before increasing the database version.
