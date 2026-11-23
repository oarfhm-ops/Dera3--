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
const ADMIN_UID = "admin_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validUserPayload(uid, email, pastMs = 10000) {
  const ts = new Date(Date.now() - pastMs);
  return {
    userId: uid,
    fullName: "Omar Ahmed",
    email: email,
    phoneNumber: "01012345678",
    phoneVerified: false,
    securityScore: 0,
    completedChecks: [],
    emergencyContact: "",
    honeypotLogs: [],
    passwordMasked: "••••••••",
    createdAt: ts,
    updatedAt: ts,
  };
}

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

test("Unauthenticated user cannot read or write users or app_config", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("app_config").doc("shields").get());
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .set(validUserPayload(ALICE_UID, "alice@example.com"))
  );
});

test("Owner can create profile with score 0 and perform valid partial updates", async () => {
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true })
    .firestore();

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .set(validUserPayload(ALICE_UID, "alice@example.com"))
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .update({
        phoneVerified: true,
        securityScore: 15,
        completedChecks: ["wifi_1", "whatsapp_btn"],
        emergencyContact: "01099998888",
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("Reject score spoofing on create, ghost fields, future timestamps, and immutable createdAt change", async () => {
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true })
    .firestore();

  // Score must start at 0 on create
  const spoofScore = validUserPayload(ALICE_UID, "alice@example.com");
  spoofScore.securityScore = 100;
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).set(spoofScore));

  // Ghost field 'id' or 'isAdmin' rejected
  const withGhost = validUserPayload(ALICE_UID, "alice@example.com");
  withGhost.id = ALICE_UID;
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).set(withGhost));

  // Future timestamp rejected
  const futureTs = validUserPayload(ALICE_UID, "alice@example.com");
  futureTs.createdAt = new Date(Date.now() + 60000);
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).set(futureTs));

  // Valid create, then reject immutable createdAt mutation
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .set(validUserPayload(ALICE_UID, "alice@example.com"))
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .update({
        createdAt: new Date(Date.now() - 500),
        updatedAt: new Date(Date.now() - 500),
      })
  );
});

test("Cross-user isolation: Alice cannot read, list, or update Bob's profile", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("users")
      .doc(BOB_UID)
      .set(validUserPayload(BOB_UID, "bob@example.com"));
  });

  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true })
    .firestore();

  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").get());
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .update({ fullName: "Hacked", updatedAt: new Date(Date.now() - 1000) })
  );
});

test("Admin access & email spoofing protection", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .set(validUserPayload(ALICE_UID, "alice@example.com"));
  });

  // Spoofed unverified admin email MUST fail
  const spoofAdminDb = testEnv
    .authenticatedContext("spoof_uid", {
      email: "oarfhm@gmail.com",
      email_verified: false,
    })
    .firestore();
  await assertFails(spoofAdminDb.collection("users").get());
  await assertFails(
    spoofAdminDb
      .collection("app_config")
      .doc("shields")
      .set({
        disabledShieldIds: ["shield_7"],
        updatedBy: "spoof_uid",
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Verified admin email succeeds
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: "oarfhm@gmail.com",
      email_verified: true,
    })
    .firestore();
  await assertSucceeds(adminDb.collection("users").get());
  await assertSucceeds(
    adminDb
      .collection("app_config")
      .doc("shields")
      .set({
        disabledShieldIds: ["shield_7"],
        updatedBy: ADMIN_UID,
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Signed-in regular user can get app_config/shields, cannot write it
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true })
    .firestore();
  await assertSucceeds(aliceDb.collection("app_config").doc("shields").get());
  await assertFails(
    aliceDb
      .collection("app_config")
      .doc("shields")
      .set({
        disabledShieldIds: [],
        updatedBy: ALICE_UID,
        updatedAt: new Date(Date.now() - 1000),
      })
  );
});

test("User badges: cannot forge before threshold, unlocks at 50% and 100%, isolated from other users", async () => {
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true })
    .firestore();
  const bobDb = testEnv
    .authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true })
    .firestore();

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .set(validUserPayload(ALICE_UID, "alice@example.com"))
  );

  // Score is 0 -> forging 50% badge MUST fail
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("badges")
      .doc("cyber_defender")
      .set({
        badgeId: "cyber_defender",
        userId: ALICE_UID,
        threshold: 50,
        titleEn: "Cyber Defender",
        titleAr: "حارس الأمان",
        unlockedAt: new Date(Date.now() - 1000),
      })
  );

  // Reach score 50 -> cyber_defender unlocks, security_expert still rejected
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      securityScore: 50,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("badges")
      .doc("cyber_defender")
      .set({
        badgeId: "cyber_defender",
        userId: ALICE_UID,
        threshold: 50,
        titleEn: "Cyber Defender",
        titleAr: "حارس الأمان",
        unlockedAt: new Date(Date.now() - 1000),
      })
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("badges")
      .doc("security_expert")
      .set({
        badgeId: "security_expert",
        userId: ALICE_UID,
        threshold: 100,
        titleEn: "Security Expert",
        titleAr: "خبير الأمان",
        unlockedAt: new Date(Date.now() - 1000),
      })
  );

  // Reach score 100 -> security_expert unlocks
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      securityScore: 100,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("badges")
      .doc("security_expert")
      .set({
        badgeId: "security_expert",
        userId: ALICE_UID,
        threshold: 100,
        titleEn: "Security Expert",
        titleAr: "خبير الأمان",
        unlockedAt: new Date(Date.now() - 1000),
      })
  );

  // Owner can list badges, Bob cannot read Alice's badges, Admin can read
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("badges").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("badges").get());

  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, { email: "oarfhm@gmail.com", email_verified: true })
    .firestore();
  await assertSucceeds(adminDb.collection("users").doc(ALICE_UID).collection("badges").get());
});

test("Global phishing alerts: admin manages, signed-in users query active=true only, inactive hidden", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true })
    .firestore();
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, { email: "oarfhm@gmail.com", email_verified: true })
    .firestore();

  const ts = new Date(Date.now() - 1000);
  const validAlert = {
    titleAr: "احتيال انتحال البنك",
    titleEn: "Fake Bank OTP Scam",
    messageAr: "مكالمات تطلب كود OTP بحجة تحديث البطاقة.",
    messageEn: "Callers impersonating banks asking for card OTP codes.",
    category: "BANK_IMPERSONATION",
    severity: "CRITICAL",
    indicator: "OTP / Bank Call",
    active: true,
    createdBy: ADMIN_UID,
    createdAt: ts,
    updatedAt: ts,
  };

  // Unauth and non-admin cannot write
  await assertFails(unauthDb.collection("phishing_alerts").doc("alert_bank_1").set(validAlert));
  await assertFails(
    aliceDb.collection("phishing_alerts").doc("alert_bank_1").set({
      ...validAlert,
      createdBy: ALICE_UID,
    })
  );

  // Admin creates active alert and inactive alert
  await assertSucceeds(adminDb.collection("phishing_alerts").doc("alert_bank_1").set(validAlert));
  await assertSucceeds(
    adminDb.collection("phishing_alerts").doc("alert_old_2").set({
      ...validAlert,
      active: false,
    })
  );

  // Admin can list unfiltered; Alice CANNOT list unfiltered or read inactive alert
  await assertSucceeds(adminDb.collection("phishing_alerts").get());
  await assertFails(aliceDb.collection("phishing_alerts").get());
  await assertFails(aliceDb.collection("phishing_alerts").doc("alert_old_2").get());

  // Alice CAN query active == true and get active alert
  await assertSucceeds(aliceDb.collection("phishing_alerts").where("active", "==", true).get());
  await assertSucceeds(aliceDb.collection("phishing_alerts").doc("alert_bank_1").get());

  // Admin can toggle active status; Alice cannot
  await assertFails(
    aliceDb.collection("phishing_alerts").doc("alert_bank_1").update({
      active: false,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(
    adminDb.collection("phishing_alerts").doc("alert_bank_1").update({
      active: false,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Alert acks and user detected threats enforce ownership, schema, and parent existence", async () => {
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true })
    .firestore();
  const bobDb = testEnv
    .authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true })
    .firestore();
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, { email: "oarfhm@gmail.com", email_verified: true })
    .firestore();

  const ts = new Date(Date.now() - 1000);
  await assertSucceeds(
    adminDb.collection("phishing_alerts").doc("alert_link_1").set({
      titleAr: "روابط جوائز وهمية",
      titleEn: "Fake Prize Phishing Links",
      messageAr: "تجنب روابط bit.ly التي تطلب بيانات محفظتك.",
      messageEn: "Avoid shortened links asking for mobile wallet PINs.",
      category: "LINK_PHISHING",
      severity: "HIGH",
      indicator: "bit.ly/free-prize",
      active: true,
      createdBy: ADMIN_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Ack for nonexistent global alert must fail
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("alert_acks")
      .doc("missing_alert")
      .set({
        alertId: "missing_alert",
        userId: ALICE_UID,
        read: true,
        dismissed: false,
        updatedAt: ts,
      })
  );

  // Ack for real alert succeeds and updates cleanly; Bob isolated
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("alert_acks")
      .doc("alert_link_1")
      .set({
        alertId: "alert_link_1",
        userId: ALICE_UID,
        read: true,
        dismissed: false,
        updatedAt: ts,
      })
  );
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("alert_acks")
      .doc("alert_link_1")
      .update({
        read: true,
        dismissed: true,
        updatedAt: new Date(Date.now() - 500),
      })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("alert_acks").get());

  // User detected threats create, update, isolation, and immutability
  const threatPayload = {
    userId: ALICE_UID,
    shieldKey: "shield_6",
    category: "LINK_PHISHING",
    severity: "CRITICAL",
    indicator: "http://bit.ly/free-gift",
    titleAr: "تم رصد رابط تصيد مشبوه",
    titleEn: "Suspicious Phishing Link Detected",
    messageAr: "تم اكتشاف رابط غير مشفر ومختصر يحمل علامات احتيال.",
    messageEn: "Detected an unencrypted shortened URL with phishing indicators.",
    read: false,
    dismissed: false,
    detectedAt: ts,
    updatedAt: ts,
  };
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("detected_threats")
      .doc("threat_1")
      .set(threatPayload)
  );
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("detected_threats")
      .doc("threat_1")
      .update({
        read: true,
        dismissed: true,
        updatedAt: new Date(Date.now() - 500),
      })
  );
  // Reject tampering with detectedAt or cross-user access
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("detected_threats")
      .doc("threat_1")
      .update({
        detectedAt: new Date(Date.now() - 200),
        updatedAt: new Date(Date.now() - 200),
      })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("detected_threats").get());
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("detected_threats")
      .doc("threat_2")
      .set({ ...threatPayload, userId: BOB_UID })
  );
});
