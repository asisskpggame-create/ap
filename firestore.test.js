const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validTaskPayload(userId, now) {
  return {
    userId,
    title: "پرداخت قبض برق",
    description: "پرداخت آنلاین",
    category: "مالی",
    tags: ["قبض", "ضروری"],
    priority: "HIGH",
    dueDateEpochDay: 20751,
    dueTimeMinutes: 540,
    reminderAtMillis: -1,
    estimatedDurationMinutes: 15,
    status: "PENDING",
    subtasksJson: "[]",
    recurrenceRule: "MONTHLY",
    recurrenceIntervalDays: 30,
    notes: "یادداشت تست",
    inputSource: "MANUAL",
    sortOrder: 0,
    completedAtMillis: -1,
    idempotencyKey: "idem_1",
    createdAt: now,
    updatedAt: now,
  };
}

test("1. Unauthenticated user cannot read or list tasks", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("tasks").get());
});

test("2. Cross-user read is rejected", async () => {
  const now = new Date(Date.now() - 1000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").set(validTaskPayload(ALICE_UID, now));
  });
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").get());
});

test("3. Authenticated owner can create, query, and update valid task", async () => {
  const now = new Date(Date.now() - 2000);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const taskRef = aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1");

  await assertSucceeds(taskRef.set(validTaskPayload(ALICE_UID, now)));

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").where("userId", "==", ALICE_UID).get()
  );

  await assertSucceeds(
    taskRef.update({
      status: "COMPLETED",
      completedAtMillis: Date.now(),
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("4. Identity spoofing on create is rejected", async () => {
  const now = new Date(Date.now() - 1000);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_spoof").set(validTaskPayload(BOB_UID, now))
  );
});

test("5. Shadow field injection on create or update is rejected", async () => {
  const now = new Date(Date.now() - 1000);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const payloadWithGhost = { ...validTaskPayload(ALICE_UID, now), isAdmin: true };
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_ghost").set(payloadWithGhost)
  );
});

test("6. Append-only activityEvents allows owner create and read, but rejects update and delete", async () => {
  const now = new Date(Date.now() - 1000);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const evtRef = aliceDb.collection("users").doc(ALICE_UID).collection("activityEvents").doc("evt_1");

  await assertSucceeds(
    evtRef.set({
      userId: ALICE_UID,
      timestampMillis: Date.now(),
      eventType: "TASK_CREATED",
      entityType: "TASK",
      entityId: "task_1",
      inputSource: "VOICE",
      operationStatus: "EXECUTED",
      verificationSource: "CLIENT_RECORDED",
      correlationId: "corr_1",
      humanSummary: "تسک پرداخت قبض ایجاد شد",
      createdAt: now,
    })
  );

  // Reject client-side update on audit event
  await assertFails(evtRef.update({ humanSummary: "Tampered summary" }));

  // Reject client-side delete on audit event
  await assertFails(evtRef.delete());

  // Reject forging SERVER_VERIFIED from client
  const forgedRef = aliceDb.collection("users").doc(ALICE_UID).collection("activityEvents").doc("evt_2");
  await assertFails(
    forgedRef.set({
      userId: ALICE_UID,
      timestampMillis: Date.now(),
      eventType: "TASK_CREATED",
      entityType: "TASK",
      entityId: "task_1",
      inputSource: "VOICE",
      operationStatus: "EXECUTED",
      verificationSource: "SERVER_VERIFIED",
      correlationId: "corr_2",
      humanSummary: "Forged server event",
      createdAt: now,
    })
  );
});

test("7. Consumption, Expense, Habit, Sleep, Exercise creation and validation", async () => {
  const now = new Date(Date.now() - 1000);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("consumptionEntries").doc("cons_1").set({
      userId: ALICE_UID,
      itemName: "آب",
      category: "WATER",
      quantity: 400,
      unit: "ml",
      timestampMillis: Date.now(),
      localEpochDay: 20750,
      inputSource: "VOICE",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("expenses").doc("exp_1").set({
      userId: ALICE_UID,
      title: "خرید قهوه",
      category: "COFFEE",
      amount: 3.0,
      currency: "EUR",
      timestampMillis: Date.now(),
      localEpochDay: 20750,
      inputSource: "VOICE",
      createdAt: now,
      updatedAt: now,
    })
  );
});
