import express from "express";
import OpenAI from "openai";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const app = express();
const port = process.env.PORT || 3000;

const client = new OpenAI({ apiKey: process.env.OPENAI_API_KEY });

// Keep the system small and explicit. The model explains and chats;
// the app itself does not implement a complex agent/rule engine.
const NORI_RULES = `
You are Nori, a simple AI assistant for the Nori/KAGE system.

Rules:
- Be direct, calm, honest, and practical.
- Understand that the user's goal is academic development, discipline, and long-term improvement.
- Help with studying, planning, decisions, accountability, and ordinary conversation.
- Do not invent actions, progress, files, tests, or capabilities that did not happen.
- If you are uncertain, say so clearly.
- Discipline is a tool, not humiliation or destructive punishment.
- Human judgment overrides rigid rules when a rule conflicts with safety, health, or the actual situation.
- Do not make the system more complicated than necessary.
- Ask for clarification only when it is genuinely needed.
`;

app.use(express.json({ limit: "1mb" }));
app.use(express.static(path.join(__dirname, "public")));

app.post("/api/chat", async (req, res) => {
  try {
    const messages = Array.isArray(req.body?.messages) ? req.body.messages : [];

    const input = messages
      .filter((m) => m && (m.role === "user" || m.role === "assistant") && typeof m.content === "string")
      .slice(-20)
      .map((m) => ({ role: m.role, content: m.content }));

    if (!input.length) {
      return res.status(400).json({ error: "No message provided." });
    }

    const response = await client.responses.create({
      model: process.env.OPENAI_MODEL || "gpt-5.6-luna",
      instructions: NORI_RULES,
      input
    });

    res.json({ reply: response.output_text || "I couldn't produce a response." });
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: "AI request failed." });
  }
});

app.listen(port, () => {
  console.log(`Nori simple chat running on port ${port}`);
});
