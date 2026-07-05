---
name: nunca-apagar-artefatos-sem-perguntar
description: Nunca apagar artefatos (dist/, build/, APKs) sem perguntar antes
metadata:
  type: feedback
---

Nunca apagar artefatos de build (`dist/`, `build/`, `app/build/`, APKs, etc.) sem
perguntar ao usuário antes — mesmo que sejam gitignored e regeneráveis.

**Why:** Em 2026-07-05, ao limpar artefatos de teste após validar builds, apaguei o
`dist/` de watch-up, utilities e site-blocker, removendo APKs que o usuário já tinha
buildado. Ele deixou claro que não quer isso.
**How to apply:** Após gerar artefatos de teste, ou perguntar antes de remover, ou
deixar por conta do usuário. Limpar só o que eu mesmo criei naquele passo e, ainda
assim, confirmar quando houver dúvida se o alvo já existia. Ver [[sample-android-skeleton]].
