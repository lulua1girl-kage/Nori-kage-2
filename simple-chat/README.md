# Nori Simple Chat

A deliberately small Nori chat: one web page, one backend endpoint, and one system-rules block.

## Run

1. Enter `simple-chat`.
2. Install dependencies: `npm install`.
3. Set `OPENAI_API_KEY` in the server environment. Never put the key in the browser or commit it to GitHub.
4. Optional: set `OPENAI_MODEL` to another model available to your API account.
5. Start with `npm start`.
6. Open `http://localhost:3000`.

The backend uses OpenAI's Responses API and keeps the Nori rules in one small constant in `server.mjs`.
