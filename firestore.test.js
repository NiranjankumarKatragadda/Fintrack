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

// --- SECURITY BOUNDS & NEGATIVE TESTS ---

test("Unauthenticated user: cannot read expenses", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("expenses").get());
});

test("Unauthenticated user: cannot create expense", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("expenses").doc("exp1").set({
      userId: ALICE_UID,
      title: "Groceries",
      amount: 50.0,
      type: "EXPENSE",
      category: "Food & Dining",
      createdAt: new Date(),
    })
  );
});

test("Cross-user access: Alice cannot read Bob's expenses", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).collection("expenses").doc("bob_exp").set({
      userId: BOB_UID,
      title: "Coffee",
      amount: 4.5,
      type: "EXPENSE",
      category: "Food & Dining",
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("expenses").doc("bob_exp").get());
});

test("Cross-user access: Alice cannot write into Bob's expenses", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("expenses").doc("alice_hacked").set({
      userId: BOB_UID,
      title: "Hack",
      amount: 100.0,
      type: "EXPENSE",
      category: "Food & Dining",
      createdAt: new Date(),
    })
  );
});

// --- POSITIVE OWNER TESTS ---

test("Owner: Alice can create and read her own expense", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const expRef = aliceDb.collection("users").doc(ALICE_UID).collection("expenses").doc("exp_1");

  await assertSucceeds(
    expRef.set({
      userId: ALICE_UID,
      title: "Grocery",
      amount: 65.50,
      type: "EXPENSE",
      category: "Food & Dining",
      date: "2026-10-29",
      note: "Weekly shopping",
      createdAt: new Date(),
    })
  );

  await assertSucceeds(expRef.get());
});

test("Owner: Alice can create budget and read her budgets", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const budgetRef = aliceDb.collection("users").doc(ALICE_UID).collection("budgets").doc("b_food");

  await assertSucceeds(
    budgetRef.set({
      userId: ALICE_UID,
      category: "Food & Dining",
      monthlyLimit: 500.0,
      alertThresholdPercent: 80,
      createdAt: new Date(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("budgets").get()
  );
});

test("Owner: Alice can create and update savings goal", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const goalRef = aliceDb.collection("users").doc(ALICE_UID).collection("goals").doc("g_emergency");
  const now = new Date();

  await assertSucceeds(
    goalRef.set({
      userId: ALICE_UID,
      title: "Emergency Fund",
      targetAmount: 5000.0,
      currentAmount: 1200.0,
      createdAt: now,
    })
  );

  await assertSucceeds(
    goalRef.update({
      currentAmount: 1500.0,
      updatedAt: new Date(),
    })
  );
});

test("Validation: Reject invalid expense amount <= 0 or invalid type", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const expRef = aliceDb.collection("users").doc(ALICE_UID).collection("expenses").doc("invalid_exp");

  await assertFails(
    expRef.set({
      userId: ALICE_UID,
      title: "Bad Amount",
      amount: -10.0,
      type: "EXPENSE",
      category: "Food",
      createdAt: new Date(),
    })
  );

  await assertFails(
    expRef.set({
      userId: ALICE_UID,
      title: "Bad Type",
      amount: 10.0,
      type: "TRANSFER_INVALID",
      category: "Food",
      createdAt: new Date(),
    })
  );
});
