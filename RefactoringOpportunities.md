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
| `ExcelExporter::exportarJogo` | String literal repetida / hardcoded (`"Histórico de Jogadas"`) | Introduce Constant | 129787 |
| `ExcelExporter::exportarJogo` | Long Method (formatação detalhada dos resultados misturada com escrita Excel) | Extract Method | 129787 |
| `ExcelExporter::exportarJogo` | Chamada duplicada (`file.getParentFile()`) | Introduce Variable | 129787 |
| `ExcelExporter::exportarJogo` | Variável temporária desnecessária (`numShots`) | Inline Variable | 129787 |
| `AiShotStrategy::buildHuntPool` | Nome de parâmetro inconsistente (`alreadyChosen` vs `chosen`) | Rename | 129787 |
| `AiShotStrategy::computeShots` | Instanciação repetida (`new Random(...)`) em cada chamada | Introduce Field | 129787 |
