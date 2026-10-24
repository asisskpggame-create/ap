# Security Specification — Personal Life OS

## 1. Data Invariants
1. **Strict Multi-Tenant Isolation**: Every document in `/users/{userId}/{collection}/{docId}` must only be readable or writable when `request.auth != null`, `request.auth.uid == userId`, and `data.userId == request.auth.uid`.
2. **Immutable Ownership & Creation Metadata**: `userId` and `createdAt` are immutable across all updates (`incoming().userId == existing().userId && incoming().createdAt == existing().createdAt`).
3. **Append-Only Activity & Audit Trail**: Documents in `/users/{userId}/activityEvents/{eventId}` can be created by the authenticated owner (with `verificationSource == 'CLIENT_RECORDED'`), read by the owner, and NEVER updated or deleted by any client (`allow update, delete: if false;`).
4. **Server Timestamp Bound**: Every `createdAt` and `updatedAt` field must be a Firestore `timestamp` and satisfy `<= request.time`.
5. **Bounded Strings & Lists**: Every string field enforces a strict character length bound, and every list (`tags`, `subtasks`, `scheduleDays`) enforces a strict element count bound (`<= 50`) to prevent Denial-of-Wallet attacks.
6. **Path ID Hygiene**: All document IDs (`taskId`, `entryId`, `itemId`, `expenseId`, `habitId`, `recordId`, `eventId`, `conversationId`, `prefId`) must match `^[a-zA-Z0-9_\-]+$` with length `<= 128`.

## 2. The "Dirty Dozen" Adversarial Payloads
1. **Unauthenticated Read/List**: Calling `get` or `list` on `/users/alice_123/tasks` with `request.auth == null`.
2. **Cross-User Read (IDOR)**: Authenticated as `bob_456`, reading `/users/alice_123/tasks/task_1`.
3. **Identity Spoofing on Create**: Authenticated as `alice_123`, creating `/users/alice_123/tasks/task_1` with `userId: "bob_456"`.
4. **Shadow Field Injection on Create**: Creating a task with an unauthorized field `isAdmin: true`.
5. **Shadow Field Injection on Update**: Updating a task with an unauthorized field `hacked: true` outside `affectedKeys().hasOnly(...)`.
6. **Immutable `createdAt` Tampering**: Updating a task while mutating `createdAt` to a new timestamp.
7. **Immutable `userId` Transfer**: Updating an expense to transfer `userId` to another user.
8. **Audit Log Tampering (Update)**: Attempting to update an existing `/users/alice_123/activityEvents/evt_1` record.
9. **Audit Log Deletion**: Attempting to delete an existing `/users/alice_123/activityEvents/evt_1` record.
10. **Oversized String Denial-of-Wallet**: Creating a task with a `title` exceeding 300 characters or `notes` exceeding 5000 characters.
11. **Unfiltered Collection List Query**: Attempting to list `/users/alice_123/tasks` without matching `resource.data.userId == request.auth.uid`.
12. **Future Timestamp Spoofing**: Submitting a `createdAt` timestamp set in the future (`> request.time`).
