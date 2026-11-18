import express from "express";
import OpenAI from "openai";

const app = express();
app.use(express.json({ limit: "32kb" }));

const port = Number(process.env.PORT || 3000);
const model = process.env.OPENAI_MODEL || "gpt-5.5";

app.get("/health", (_req, res) => {
  res.json({ ok: true, service: "media-buyer-bot-api" });
});

app.post("/api/generate-six", async (req, res) => {
  if (!process.env.OPENAI_API_KEY) {
    return res.status(503).json({ error: "AI service is not configured yet." });
  }

  const audience = typeof req.body?.audience === "string"
    ? req.body.audience.slice(0, 500)
    : "أصحاب البيزنس والمتاجر الإلكترونية والبراندات والشركات وصناع القرار";
  const context = typeof req.body?.context === "string"
    ? req.body.context.slice(0, 1000)
    : "Media buying and Meta Ads services";

  try {
    const client = new OpenAI({ apiKey: process.env.OPENAI_API_KEY });
    const response = await client.responses.create({
      model,
      store: false,
      instructions: [
        "أنت كاتب محتوى تسويقي محترف متخصص في التسويق الإلكتروني وMedia Buying وإعلانات Meta.",
        "اكتب باللهجة المصرية الطبيعية، بدون مبالغة أو وعود بنتائج مضمونة.",
        "الجمهور المستهدف أصحاب البيزنس والمتاجر الإلكترونية والبراندات والشركات وصناع القرار. تجنب المدرسين.",
        "أنشئ ستة منشورات مختلفة فعلًا: مشكلة شائعة، نصيحة عملية، خطأ إعلاني، حالة/سيناريو توضيحي غير منسوب لعميل حقيقي، سؤال تفاعلي، ودعوة تواصل.",
        "كل منشور مستقل وجاهز للنشر، بين 50 و100 كلمة تقريبًا، مع افتتاحية جذابة وCTA طبيعي.",
        "لا تخترع أرقام أداء أو نتائج أو شهادات عملاء. لا تكرر نفس الفكرة."
      ].join("\n"),
      input: `الجمهور: ${audience}\nالسياق والخدمات: ${context}\nأخرج JSON فقط بالشكل: {"posts":[{"title":"...","content":"..."}]} ويجب أن يحتوي posts على 6 عناصر بالضبط.`,
      text: {
        format: {
          type: "json_schema",
          name: "daily_marketing_posts",
          strict: true,
          schema: {
            type: "object",
            additionalProperties: false,
            properties: {
              posts: {
                type: "array",
                minItems: 6,
                maxItems: 6,
                items: {
                  type: "object",
                  additionalProperties: false,
                  properties: {
                    title: { type: "string" },
                    content: { type: "string" }
                  },
                  required: ["title", "content"]
                }
              }
            },
            required: ["posts"]
          }
        }
      }
    });

    const data = JSON.parse(response.output_text);
    if (!Array.isArray(data.posts) || data.posts.length !== 6 ||
        data.posts.some(p => !p.title?.trim() || !p.content?.trim())) {
      return res.status(502).json({ error: "The AI returned an invalid post set. Please retry." });
    }
    return res.json({ posts: data.posts });
  } catch (error) {
    console.error("generate-six failed:", error?.message || error);
    return res.status(502).json({ error: "Could not generate posts right now. Please retry." });
  }
});

app.listen(port, () => console.log(`MediaBuyerBot API listening on ${port}`));
