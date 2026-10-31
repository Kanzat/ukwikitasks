# Записані відповіді uk.wikipedia для тестів

Відповіді API uk.wikipedia.org, записані 31.10.2026 тими самими запитами, які робить jwiki. `MockUkWikipedia`
віддає їх тестам замість справжньої Вікіпедії. Файли збережено як є, без змін.

Єдина наявна сторінка: `Вікіпедія:Статті-кандидати на вилучення/15 вересня 2026`, ревізія 48886016:
https://uk.wikipedia.org/w/index.php?oldid=48886016. Усі 10 номінацій з неї перелічено таблицею в
`VpVylParserTest.parsesDayPage`, кожен рядок можна звірити з ревізією.

Відсутня сторінка: `Вікіпедія:Статті-кандидати на вилучення/14 грудня 2025`.

| Файл | Запит (`https://uk.wikipedia.org/w/api.php?format=json&…`) | Коли jwiki його робить |
|---|---|---|
| `siteinfo.json` | `action=query&meta=siteinfo&siprop=namespaces\|namespacealiases` | під час створення `Wiki` |
| `day-page-sections.json` | `action=parse&prop=sections&page=<наявна сторінка>` | `splitPageByHeader`: заголовки |
| `day-page-text.json` | `action=query&prop=revisions&rvprop=content&titles=<наявна сторінка>` | `splitPageByHeader`: текст |
| `missing-page-parse.json` | `action=parse&prop=sections&page=<відсутня сторінка>` | `splitPageByHeader` для відсутньої сторінки |
| `existing-page.json` | `action=query&prop=pageprops&ppprop=missing&titles=<наявна сторінка>` | `exists` |
| `missing-page.json` | `action=query&prop=pageprops&ppprop=missing&titles=<відсутня сторінка>` | `exists` |

jwiki шукає відповідь `exists` за назвою сторінки, тому `MockUkWikipedia` підставляє в `existing-page.json` і
`missing-page.json` назву, яку запитали.

## Як записати заново

```bash
API=https://uk.wikipedia.org/w/api.php
UA="User-Agent: KanzatBot test data"
PAGE="Вікіпедія:Статті-кандидати на вилучення/15 вересня 2026"
MISSING="Вікіпедія:Статті-кандидати на вилучення/14 грудня 2025"
curl -sG -H "$UA" $API -d format=json -d action=query -d meta=siteinfo --data-urlencode 'siprop=namespaces|namespacealiases' > siteinfo.json
curl -sG -H "$UA" $API -d format=json -d action=parse -d prop=sections --data-urlencode "page=$PAGE" > day-page-sections.json
curl -sG -H "$UA" $API -d format=json -d action=query -d prop=revisions -d rvprop=content --data-urlencode "titles=$PAGE" > day-page-text.json
curl -sG -H "$UA" $API -d format=json -d action=parse -d prop=sections --data-urlencode "page=$MISSING" > missing-page-parse.json
curl -sG -H "$UA" $API -d format=json -d action=query -d prop=pageprops -d ppprop=missing --data-urlencode "titles=$PAGE" > existing-page.json
curl -sG -H "$UA" $API -d format=json -d action=query -d prop=pageprops -d ppprop=missing --data-urlencode "titles=$MISSING" > missing-page.json
```

Запити повертають поточну версію сторінки. Якщо сторінку відтоді редагували, онови номер ревізії тут і таблицю в
`VpVylParserTest.parsesDayPage`.
