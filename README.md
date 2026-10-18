# ⚓ Battleship 2.0

> 🎥 **Vídeo de Demonstração (YouTube):** [https://www.youtube.com/watch?v=sxTSu2tDtMA](https://www.youtube.com/watch?v=sxTSu2tDtMA)

![Build Status](https://img.shields.io/badge/build-passing-brightgreen)
![Java Version](https://img.shields.io/badge/Java-21-blue)
![Build Tool](https://img.shields.io/badge/Maven-3.x-red)
![License](https://img.shields.io/badge/license-MIT-green)

A modern take on the classic naval warfare game, designed for the XVII century Portuguese Discoveries setting with automated build workflows and AI interoperability.

### Turma / Grupo: GrupoTP02_LETI-5

---

## 👥 Membros da Equipa

| Número | Nome | Curso | GitHub Repo / Profile |
| :---: | :--- | :---: | :--- |
| **122615** | Rodrigo Chaves | LETI | https://github.com/122615 |
| **122650** | Miguel Pancada | LETI | https://github.com/miguelpancada05 |
| **129787** | António Contente | LETI | https://github.com/antoniocontente |
| **122652** | Gonçalo Gonçalves | LETI | https://github.com/ggpsg |

> **Nota:** Os nomes e identificadores das contas GitHub foram mapeados e associados na tabela em conformidade com o conhecimento do docente.

---

## 📋 User Stories Implementadas (Sprint - Parte 1)

Cada membro do grupo desenvolveu um requisito individual num ramo dedicado, integrando uma dependência externa através do Maven Central:

1. **Geração de Relatório de Partida em PDF:** Exportação das estatísticas e do resultado do jogo para um ficheiro PDF através da biblioteca **Apache PDFBox** (`org.apache.pdfbox:pdfbox:2.0.31`).
2. **Exportação do Histórico de Jogadas para Excel:** Registo detalhado das rajadas, tiros e estado da partida numa folha de cálculo através da biblioteca **Apache POI** (`org.apache.poi:poi-ooxml`).
3. **Persistência de Jogo (Guardar / Carregar):** Capacidade de guardar o estado da partida e retomar jogos anteriores a partir de ficheiros estruturados em formato JSON utilizando a biblioteca **Jackson** (`com.fasterxml.jackson.core:jackson-databind`).
---

## 🤖 Treino e Estratégia com IA (Sprint - Parte 2)

Integração de um LLM como oponente inteligente através do intercâmbio de mensagens estruturadas em formato JSON e aplicação de técnicas de *few-shot prompting*.

### Prompt Final da Estratégia (Few-Shot & Tática de Jogo)

```text
Considere agora a seguinte tática avançada de geração de rajadas de tiros para derrotar o oponente com o menor número de jogadas possível.

Crie e mantenha atualizado um Diário de Bordo antes de cada resposta, numerando as rajadas sequencialmente (Rajada 1, 2, 3...). Registe em cada entrada as coordenadas exatas disparadas e o resultado global da rajada (Água, Nau atingida, Barca afundada, etc.), a lista atualizada de navios inimigos restantes (começando com 4 Barcas de 1 posição, 3 Caravelas de 2 posições, 2 Naus de 3 posições, 1 Fragata de 4 posições e 1 Galeão de 5 posições em forma de T), e o mapa mental de exclusão com coordenadas já disparadas ou descartadas por halo de segurança. A memória é a principal arma de um bom estratega.

Não dispare fora dos limites do mapa (linhas A a J e colunas 1 a 10, evitando erros como Z99) nem repita tiros em coordenadas já testadas ou descartadas. A única exceção para este desperdício de pólvora é a última rajada do jogo, apenas para perfazer os 3 tiros obrigatórios quando a frota inimiga já estiver irremediavelmente no fundo do mar.

Quando não houver navios feridos por afundar (Modo Caça), utilize uma grelha de paridade em padrão de xadrez, disparando em casas alternadas (por exemplo A2, A4, A6, B1, B3) para maximizar a cobertura espacial e intercetar rapidamente os navios maiores. Distribua os 3 tiros da rajada por quadrantes distintos do tabuleiro que ainda tenham alta densidade de células livres. Se restarem apenas Barcas de 1 posição por encontrar, procure prioritariamente nos espaços vazios isolados onde já não caberiam navios maiores.

Se atingir um navio numa rajada (Modo Alvo), dispare nas posições contíguas ortogonais (Norte, Sul, Este, Oeste) na jogada seguinte para descobrir a orientação da embarcação e acabar de a afundar. No entanto, se a rajada anterior confirmar que o navio já foi afundado (por exemplo, em sunkBoats aparecer uma Barca), não dispare para as posições contíguas, pois os navios nunca estão encostados.

Como as Caravelas, Naus e Fragatas são linhas retas, um tiro certeiro significa que o resto do navio está na horizontal ou na vertical. Assim que confirmar dois acertos contíguos (por exemplo C4 e C5), o eixo do navio fica determinado; concentre os tiros seguintes nas extremidades dessa linha (C3 e C6) até afundar o navio. Como os navios não se podem tocar nem sequer nos cantos, as posições diagonais a um tiro certeiro são garantidamente água (a única exceção é o corpo do Galeão, devido à sua forma em T com um corpo de 3 posições e duas asas da ponte numa extremidade). Evitar estas diagonais poupa imensos tiros. Se o relatório indicar que atingiu um Galeão, teste tanto o prolongamento do eixo como as posições perpendiculares na extremidade para destruir as asas.

Quando o relatório de uma rajada confirmar que um navio foi afundado (por exemplo uma Fragata de 4 posições), analise os dados do seu Diário de Bordo para identificar exatamente onde caíram esses 4 tiros. Confirmada a posição exata da carcaça, marque todas as quadrículas adjacentes (o halo de 1 posição em redor de todo o navio, incluindo diagonais) como água intransitável, pois é impossível haver outra embarcação nesse perímetro.

Aplique os seguintes exemplos práticos de raciocínio tático:

Exemplo 1 (Barca afundada e marcação de halo):
- Rajada Disparada: [{"row": "B", "column": 2}, {"row": "E", "column": 5}, {"row": "H", "column": 8}]
- Resposta Recebida: {"validShots": 3, "sunkBoats": [{"count": 1, "type": "Barca"}], "repeatedShots": 0, "outsideShots": 0, "hitsOnBoats": [], "missedShots": 2}
- Ação Tática: Uma Barca de 1 posição foi afundada ao primeiro tiro e não há navios feridos em hitsOnBoats. Não desperdice tiros nas adjacências de B2, E5 ou H8 à procura de continuação; continue em Modo Caça noutras coordenadas livres.

Exemplo 2 (Nau atingida e foco ortogonal):
- Rajada Disparada: [{"row": "D", "column": 4}, {"row": "A", "column": 1}, {"row": "J", "column": 10}]
- Resposta Recebida: {"validShots": 3, "sunkBoats": [], "repeatedShots": 0, "outsideShots": 0, "hitsOnBoats": [{"hits": 1, "type": "Nau"}], "missedShots": 2}
- Ação Tática: Assumindo que o tiro certeiro foi em D4, marque imediatamente as diagonais C3, C5, E3 e E5 como água garantida. Na rajada seguinte, dispare nas posições ortogonais contíguas: [{"row": "C", "column": 4}, {"row": "E", "column": 4}, {"row": "D", "column": 5}].

Em cada turno, apresente o resumo do seu Diário de Bordo seguido da próxima rajada de 3 tiros no formato JSON acordado:
[
  {"row": "X", "column": Y},
  {"row": "X", "column": Y},
  {"row": "X", "column": Y}
]
Se a sua frota for toda afundada, declare a derrota com honra. Em contrapartida, seja um vencedor magnânimo se for o inimigo a render-se com os navios todos no fundo do oceano! Comece agora por enviar a Rajada 1.
