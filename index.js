// notifications/{uid}/{nid} এ নতুন কিছু লেখা হলেই সেই ইউজারের ফোনে OneSignal পুশ যাবে।
const { onValueCreated } = require("firebase-functions/v2/database");
const { defineSecret } = require("firebase-functions/params");
const ONESIGNAL_KEY = defineSecret("ONESIGNAL_REST_API_KEY");
const APP_ID = "cc3ee16e-2486-46b4-86de-f5118a66f03a";

exports.pushOnNotification = onValueCreated(
  { ref: "/notifications/{uid}/{nid}", secrets: [ONESIGNAL_KEY], region: "us-central1" },
  async (event) => {
    const n = event.data.val();
    if (!n || typeof n !== "object" || !n.title) return;
    const res = await fetch("https://api.onesignal.com/notifications", {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: "Key " + ONESIGNAL_KEY.value() },
      body: JSON.stringify({
        app_id: APP_ID,
        target_channel: "push",
        include_aliases: { external_id: [event.params.uid] },
        headings: { en: String(n.title) },
        contents: { en: String(n.body || "") },
        data: { type: n.type || "", tournamentId: n.tournamentId || "" },
      }),
    });
    if (!res.ok) console.error("OneSignal error", res.status, await res.text());
  }
);
