# MediaBuyerBot AI backend

This service generates six Egyptian-Arabic marketing posts for Media Buying / Meta Ads.

## Run locally

1. Install Node.js 20 or newer.
2. Copy `.env.example` to `.env`.
3. Set `OPENAI_API_KEY` and a long random `APP_API_TOKEN` in the server environment. Never put the OpenAI key in the Android app, GitHub, or a chat.
4. Run `npm install` then `npm start`.

The API listens on `PORT` (default 3000).
- `GET /health`
- `POST /api/generate-six`

Example request:
```json
{
  "audience": "Business owners, ecommerce stores, brands and companies",
  "context": "Meta Ads media buying and campaign optimization"
}
```

The response contains exactly six `title` / `content` objects.

## Deployment

Deploy this folder to a Node.js host that supports environment variables. Configure `OPENAI_API_KEY` and optionally `OPENAI_MODEL` in the host's secret/environment settings. Do not commit a real `.env` file. The Android app must be configured with the deployed HTTPS API base URL and the matching `APP_API_TOKEN` before live generation can work.\n\n## Render deployment\n\nThe repository includes `render.yaml` and `backend/Dockerfile`. In Render, create a new Blueprint from this GitHub repository. Render generates `APP_API_TOKEN`; set `OPENAI_API_KEY` as a secret in the service environment. After deployment, copy the service HTTPS URL and the generated app token into the two corresponding fields in the Android app.

OpenAI API usage is billed separately from ChatGPT subscriptions. No API calls are made until a valid key is configured and the endpoint is invoked.
