// The only write path. Called from the laptop with header x-learning-token.
import { createClient } from "npm:@supabase/supabase-js@2";

const TOKEN = Deno.env.get("LEARNING_TOKEN") ?? "";
const db = createClient(
  Deno.env.get("SUPABASE_URL")!,
  Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
  { auth: { persistSession: false } },
);

const LEVELS = ["seen", "held", "solid", "dissolved"];
const SLUG = /^[a-z0-9]+(-[a-z0-9]+)*$/;
const DATE = /^\d{4}-\d{2}-\d{2}$/;
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

// Dubai is UTC+4 with no DST, so a fixed offset is exact. Server clock is UTC.
function dubaiToday(): string {
  return new Date(Date.now() + 4 * 3600_000).toISOString().slice(0, 10);
}

function sameToken(given: string): boolean {
  if (!TOKEN || given.length !== TOKEN.length) return false;
  let diff = 0;
  for (let i = 0; i < given.length; i++) diff |= given.charCodeAt(i) ^ TOKEN.charCodeAt(i);
  return diff === 0;
}

function reply(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json" },
  });
}

class BadRequest extends Error {}

function need<T>(ok: boolean, value: T, message: string): T {
  if (!ok) throw new BadRequest(message);
  return value;
}

async function start(b: Record<string, unknown>) {
  const subject = need(typeof b.subject === "string" && SLUG.test(b.subject), b.subject as string,
    "subject must be a kebab-case slug");
  const topics = need(
    Array.isArray(b.topics) && b.topics.length > 0 &&
      b.topics.every((t) => typeof t === "string" && SLUG.test(t)),
    b.topics as string[], "topics must be a non-empty array of kebab-case slugs");
  const light = need(b.light === undefined || typeof b.light === "boolean", b.light === true,
    "light must be true or false");

  // Optional backfill date, e.g. to record a session logged late. Never in the future.
  const today = dubaiToday();
  const date = b.date === undefined ? today : need(
    typeof b.date === "string" && DATE.test(b.date) && b.date <= today, b.date as string,
    "date must be YYYY-MM-DD and not after today (Dubai)");

  const { data, error } = await db.from("sessions")
    .insert({ date, subject, topics, light })
    .select("id, date").single();
  if (error) throw error;
  return reply(200, data);
}

async function summary(b: Record<string, unknown>) {
  const id = need(typeof b.id === "string" && UUID.test(b.id), b.id as string, "id must be a session uuid");
  const text = need(typeof b.summary === "string" && b.summary.trim() !== "", (b.summary as string).trim(),
    "summary must be non-empty text");
  const { data, error } = await db.from("sessions").update({ summary: text }).eq("id", id).select("id");
  if (error) throw error;
  if (!data.length) return reply(404, { error: "no session with that id" });
  return reply(200, { id, updated: true });
}

async function mastery(b: Record<string, unknown>) {
  const subject = need(typeof b.subject === "string" && SLUG.test(b.subject), b.subject as string,
    "subject must be a kebab-case slug");
  const topic = need(typeof b.topic === "string" && SLUG.test(b.topic), b.topic as string,
    "topic must be a kebab-case slug");
  const level = need(typeof b.level === "string" && LEVELS.includes(b.level), b.level as string,
    `level must be one of ${LEVELS.join(", ")}`);
  const { error } = await db.from("mastery")
    .upsert({ subject, topic, level, updated_at: new Date().toISOString() }, { onConflict: "subject,topic" });
  if (error) throw error;
  return reply(200, { subject, topic, level });
}

// For removing test rows. Real sessions should never need it.
async function remove(b: Record<string, unknown>) {
  const id = need(typeof b.id === "string" && UUID.test(b.id), b.id as string, "id must be a session uuid");
  const { data, error } = await db.from("sessions").delete().eq("id", id).select("id");
  if (error) throw error;
  return reply(200, { id, deleted: data.length > 0 });
}

Deno.serve(async (req) => {
  if (req.method !== "POST") return reply(405, { error: "POST only" });
  if (!sameToken(req.headers.get("x-learning-token") ?? "")) return reply(401, { error: "bad token" });

  let body: Record<string, unknown>;
  try {
    body = await req.json();
  } catch {
    return reply(400, { error: "body must be JSON" });
  }

  try {
    switch (body.action) {
      case "start": return await start(body);
      case "summary": return await summary(body);
      case "mastery": return await mastery(body);
      case "delete": return await remove(body);
      default: return reply(400, { error: "action must be start, summary, mastery or delete" });
    }
  } catch (e) {
    if (e instanceof BadRequest) return reply(400, { error: e.message });
    console.error(e);
    return reply(500, { error: "database error" });
  }
});
