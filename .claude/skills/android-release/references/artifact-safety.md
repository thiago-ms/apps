# Segurança de artefatos — os três destinos e a lei do não-apagar

## A lei

**Nunca apagar `dist/`, `build/` ou qualquer APK sem perguntar antes** — mesmo
gitignorado, mesmo regenerável.

Isso vem de um incidente real em **2026-07-05**: o `dist/` de `watch-up`,
`utilities` e `site-blocker` foi apagado de uma vez, levando o histórico de APKs
buildados desses três apps. Registrado em
`.claude/memory/nunca-apagar-artefatos-sem-perguntar.md`.

Consequências práticas para esta skill:

- `make clean` está **proibido** no fluxo de release, inclusive "pra garantir um build
  limpo". Se um build falhar de forma que pareça cache sujo, dizer isso ao usuário e
  deixar a decisão com ele.
- `rm -rf dist/`, `git clean`, `git stash` — nunca.
- A **única** remoção que este fluxo faz é o APK superado nos hubs (passo 7), e ela
  exige confirmação explícita nomeando o arquivo, nunca em lote.
- Isso vale também para artefatos que a própria skill criou. Um scaffold de teste em
  `_scratch/` não se apaga sozinho.

---

## Os três destinos

| Caminho | O que é | Versionado? | Quantos APKs |
|---|---|---|---|
| `<app>/dist/` | histórico local de builds — debug **e** release, todas as versões já buildadas | **não** (`.gitignore` linha `dist/`) | acumula (watch-up tem 17 arquivos, 1.8→1.16) |
| `<app>/.dist/` | o release dourado do app | **sim**, no submódulo | exatamente 1 |
| `<ROOT>/.dist/` | hub agregador servido pelo `server.sh` da raiz | **sim**, no superprojeto | exatamente 1 por app + `index.html` |

Detalhe que faz isso funcionar: o `.gitignore` dos apps ignora `dist/` (com barra, sem
prefixo), o que **não** casa com `.dist/`. Por isso o hub dourado é rastreável sem
exceção de ignore.

### Por que dois hubs

- `<app>/.dist/` viaja com o repo do app. Quem clona `thiago-ms/notes` sozinho tem o
  APK instalável ali.
- `<ROOT>/.dist/` é o que o `server.sh` da raiz serve — a lista de todos os apps de uma
  vez, para instalar vários no celular numa sessão.

Os dois recebem o **mesmo arquivo** no passo 7. Nunca deixar um atualizado e o outro não.

---

## O invariante do hub, enunciado direito

> Cada hub guarda exatamente um APK release por app, e a versão dele é igual ao
> `versionName` atual daquele app.

O que **não** é violação:

- versão antiga ausente do hub. É por design — o hub não é arquivo histórico, é "a
  versão de agora". `watchup-1.9-release.apk` não estar lá não é achado.
- `dist/` com buracos (o `watchup-1.9-release.apk` nunca existiu, só o debug da 1.9).
  `dist/` é histórico local best-effort, não tem invariante.

O que **é** violação:

- hub com o APK de uma versão anterior à atual (release não publicado).
- hub com dois APKs do mesmo app (o superado não saiu).
- hub sem APK nenhum de um app que já teve release.
- `index.html` da raiz não listando um APK presente (índice velho).

---

## Regenerar o `index.html` da raiz

O `server.sh` da raiz gera o índice **e depois** bloqueia servindo na porta 8000 — ou
seja, não serve para "só regenerar". Replicar o mesmo loop inline (está no passo 7 de
[release-checklist.md](release-checklist.md)) para que o diff do commit seja
exatamente o que o `server.sh` produziria.

O índice entra no mesmo commit do superprojeto que o APK — foi assim em `App WatchUp
1.16`.

---

## O que fazer quando o gate do device reprova

O bump já foi aplicado (passo 4) e os APKs já existem (passo 5). Ao receber "não
verificado / falhou no aparelho":

1. **Parar.** Não copiar nada para os hubs, não commitar, não taguear.
2. **Não reverter o bump.** A versão está queimada de propósito: existe artefato com
   esse nome em `dist/`, então reusar o número produziria dois APKs diferentes com a
   mesma versão. O próximo release usa o número seguinte.
3. **Não apagar** os APKs recém-gerados. Eles são a evidência do que falhou.
4. Dizer ao usuário: versão bumpada, artefatos em `dist/`, nada publicado, e que a
   correção vira um novo release com o número seguinte.

Esse é exatamente o caso do `watch-up` 1.15: buildado, presente em `dist/`, nunca
tagueado, nunca publicado no hub. O estado é consistente, não um erro a limpar.
