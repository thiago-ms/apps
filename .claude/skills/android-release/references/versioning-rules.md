# Regras de versionamento

## Versionamento é manual — por decisão, não por falta de automação

Registrado em `.claude/memory/bump-versao-antes-do-dist.md`: o bump de
`versionName` **e** `versionCode` é feito à mão antes de gerar qualquer APK
entregável, e nunca se sobrescreve uma versão já usada. O motivo histórico: o app
`notes` teve a 1.0 sobrescrita várias vezes, e ficou impossível saber qual APK
correspondia a qual código.

Esta skill **propõe** a versão e **exige confirmação**. Nunca calcula e aplica.

Rebuilds de validação durante o desenvolvimento podem reusar a versão — é só ao
gerar entregável (`dist/` + hubs) que o bump é obrigatório.

---

## O esquema de duas casas

`versionName` tem duas casas: `1.16`, `2.3`, `1.9`. **Não** é semver de três casas.
A segunda casa passa de 9 sem virar (1.9 → 1.10 → 1.11 … → 1.16), ou seja é um
contador, não um decimal. Comparar como **par de inteiros**, nunca como float —
`1.10 > 1.9` é verdade aqui e falso em aritmética decimal.

O app `gastos` usa o mesmo esquema de 2 casas (registrado na memória global do
usuário).

## A relação `versionCode` ↔ `versionName`

Estado verificado dos 7:

| app | versionName | versionCode | bate com minor+1? |
|---|---|---|---|
| watch-up | 1.16 | 17 | ✅ |
| utilities | 1.9 | 10 | ✅ |
| notes | 1.6 | 7 | ✅ |
| gastos | 1.1 | 2 | ✅ |
| apps-hub | 1.1 | 2 | ✅ |
| `.sample` | 0.1 | 1 | ✅ |
| site-blocker | 2.3 | 6 | ❌ (fora do padrão) |

A regra `versionCode = minor + 1` vale em 6 de 7. Ela é **default sugerido, nunca
asserção**. `site-blocker` está fora dela e o histórico não diz por quê — tem uma
única tag (`2.3`) e 6 commits, e o code 6 sugere que ali ele contou builds em vez de
acompanhar o minor. Não tentar reconciliar: para esse app, oferecer `código atual + 1`.

A única regra dura é: **`versionCode` estritamente crescente**. O Android recusa
instalar um APK com code menor ou igual ao instalado.

Na dúvida, para qualquer app fora do padrão minor+1, oferecer `código atual + 1`.

---

## Propor a próxima versão

Duas opções, sempre nesta ordem:

- **próximo minor** — `1.16` → `1.17`. O default. Cobre praticamente toda entrega
  deste repo: ajuste, bug, melhoria.
- **próximo major** — `1.16` → `2.0`. Só quando o usuário sinaliza mudança grande.
  `site-blocker` fez isso uma vez (1.x → 2.0).

Não oferecer "patch": o esquema de 2 casas não tem terceira posição.

---

## Quando a versão anterior nunca foi tagueada

Casos reais: `watch-up` 1.15 (buildada, em `dist/`, sem tag, nunca publicada),
`notes` 1.0 e 1.1 (sem tag).

Isso **não bloqueia** o release atual. Fazer:

1. Seguir normalmente com a versão nova.
2. **Mencionar** o buraco no relatório final, com o comando pra fechar retroativamente
   se o usuário quiser:
   ```bash
   git -C <app> tag <versão-antiga> <sha-do-commit-correspondente>
   ```
3. **Não** taguear retroativamente por conta própria. Escolher o commit certo de um
   build de meses atrás é julgamento — e no caso da 1.15 do watch-up a versão nunca
   virou release de verdade, então taguear seria *errado*, não corretivo.

O `/android-audit` reporta esses buracos com o sha candidato; a decisão fica com o
usuário.

---

## Tags

- **bare, sem prefixo**: `1.16`, não `v1.16`. Verificado nos dois apps que taguearam.
- mensagem do commit no submódulo: `Release <ver>`.
- mensagem do commit no superprojeto: `App <rootProject.name> <ver>` — `App WatchUp
  1.16`, `App Notes 1.6`. Note que é o `rootProject.name`, não o diretório.
- as tags são gappy e isso é normal: `watch-up` tem 1.8–1.14 e 1.16, sem 1.15.

Confirmar o estilo lendo `git log -1 --format=%s` nos dois repos no Preflight, em vez
de assumir — se o usuário mudou de convenção, seguir a nova.
