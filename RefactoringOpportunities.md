# Oportunidades de Refabricação

Ficheiro criado por: LETI-122652 . Cada membro do grupo aplica entre 5 e 10 refabricações de tipos diferentes no seu ramo `Refactoring_XXXXX` O mesmo tipo de refabricação pode ser aplicado por membros diferentes, desde que em locais distintos do código.

| Local | Cheiro no código | Refabricação | Nº aluno |
|---|---|---|---|
| `PDFReportGenerator::generateReport` | Long Parameter List | Introduce Parameter Object | 122652 |
| `PDFReportGenerator::generateReport` | Números mágicos (Magic Number) | Introduce Constant | 122652 |
| `Move::processEnemyFire` | Long Method | Extract Method | 122652 |
| `Move::processEnemyFire` | Objeto criado em cada chamada (`new ObjectMapper()`) | Introduce Field | 122652 |
| `Move::processEnemyFire` | Variável temporária desnecessária (`boatName`, no ciclo dos navios afundados) | Inline Variable | 122652 |
| `Game` (parâmetros `show_shots`, `show_legend`) | Nomes fora da convenção (Checkstyle `ParameterName`) | Rename | 122652 |
| `SaveGameManager::load` | Long Method | Extract Method | 122615 |
| `SaveGameManager::save` | Chamada repetida (`ship.getPosition()` duas vezes) | Introduce Variable | 122615 |
| `GameSession::markAiWins` | Texto mágico (Magic String `"AI_WINS"`) | Introduce Constant | 122615 |
| `SaveGameManager::save` | Variável temporária desnecessária (`state`) | Inline Variable | 122615 |
| `SaveGameManager::buildFleet` | Nome pouco expressivo (`data`) | Rename | 122615 |