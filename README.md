# Atividade Prática - IA para Jogos (libGDX)
**Curso de Jogos Digitais - 4º Semestre (Mackenzie)**  
**Tema:** Steering Behaviors (Comportamentos Direcionadores de Craig Reynolds) & Máquina de Estados Finitos (FSM)

---

## 📌 Conteúdo Implementado

### 1. Modelagem do Agente (`Agente.java`)
- **Atributos de Física:**
  - `posicao` (`Vector2`): Posição atual do agente no espaço 2D.
  - `velocidade` (`Vector2`): Vetor velocidade atual.
  - `aceleracao` (`Vector2`): Força resultante acumulada dividida pela massa ($a = \frac{F}{m}$).
  - `maxVelocidade` (`float` / `Vector2`): Limite máximo de velocidade escalar.
  - `maxForca` (`float` / `Vector2`): Limite de força de direcionamento (*steering force*).
- **Integração de Euler:** Método `atualizar(float delta)` para integrar aceleração e velocidade a cada frame.

### 2. Seek e Arrive (`Agente.java` e `GameIA.java`)
- **Seek (Busca):**
  - $\vec{v}_{desejada} = \text{normalizar}(\vec{p}_{alvo} - \vec{p}_{agente}) \times v_{max}$
  - $\vec{F}_{steer} = \text{limitar}(\vec{v}_{desejada} - \vec{v}, F_{max})$
- **Arrive (Chegada Suave):**
  - Calcula a distância $d$ até o ponto clicado pelo mouse.
  - Se $d < \text{raioDesaceleracao}$, a velocidade desejada diminui linearmente:
    $v_{desejada} = v_{max} \times \left(\frac{d}{\text{raioDesaceleracao}}\right)$
  - Se $d < 2.5\text{px}$, zera a velocidade para estacionar de forma suave, estável e sem trepidações.
- **Interação:** Clique com o mouse na tela para definir o alvo. Pressione `[T]` para alternar entre *Arrive* suave e *Seek* direto para comparar o comportamento!

### 3. Wander + FSM (Pursuit)
- **Wander (Vagando Orgânico):**
  - Projeta um círculo de raio fixo à frente do agente na direção de sua velocidade.
  - Desloca um ponto alvo na circunferência variando suavemente seu ângulo a cada quadro com ruído angular.
  - O agente busca (*seek*) esse ponto projetado.
- **Máquina de Estados Finitos (FSM - `EstadoAgente.java`):**
  - **Estado WANDER:** O NPC não possui alvo e vaga pela tela.
  - **Transição de Percepção:** Quando a distância entre o NPC e o jogador for menor que o raio de visão (`raioVisao = 180px`), o estado muda para `PURSUIT`.
  - **Estado PURSUIT:** O NPC calcula a posição futura prevista do jogador baseada na velocidade atual e na distância relativa, perseguindo o ponto previsto.
  - **Transição de Perda:** Se o jogador fugir para além do raio de visão (com margem de histerese), o NPC retorna ao estado `WANDER`.
- **Interação:** Controle o Jogador usando `[W, A, S, D]` ou as Setas do teclado.

### 4. Bônus: Bando (Flocking - 30 Agentes)
- **30 Agentes Autônomos simultâneos:**
  - **Separação:** Força repulsiva inversamente proporcional à distância dos vizinhos para evitar colisões.
  - **Alinhamento:** Força para navegar na direção média da velocidade dos vizinhos no raio de percepção.
  - **Coesão:** Força atrativa para o centro de massa do grupo local.
- **Ajuste de Pesos em Tempo Real:**
  - `[U]` / `[J]`: Aumentar / Diminuir peso da **Separação**
  - `[I]` / `[K]`: Aumentar / Diminuir peso do **Alinhamento**
  - `[O]` / `[L]`: Aumentar / Diminuir peso da **Coesão**
- **Bordas:** *Wrap-around* suave para manter o bando dinâmico na tela.

### 5. Depuração Visual dos Vetores com `ShapeRenderer` (Dica)
- **Vetor Velocidade:** Linha **VERDE** apontando a direção e magnitude do movimento.
- **Vetor Força (*Steering*):** Linha **VERMELHA** indicando a aceleração direcionadora aplicada no frame.
- **Geometria de Depuração:**
  - Círculo azul do raio de desaceleração do *Arrive*.
  - Círculo de campo de visão do NPC (Amarelo em *Wander*, Vermelho em *Pursuit*).
  - Círculo projetado do *Wander* e ponto previsto do *Pursuit*.
- Tecla `[D]`: Liga/desliga todos os vetores de depuração visual.

---

## 🎮 Controles e Atalhos

| Tecla | Ação |
| :--- | :--- |
| `[1]` | Alternar para o **Modo 1: Seek & Arrive** |
| `[2]` | Alternar para o **Modo 2: Wander + FSM (Pursuit)** |
| `[3]` | Alternar para o **Modo 3: Bando (30 Agentes Flocking)** |
| `[4]` | Alternar para o **Modo 4: Sandbox Geral (Todos os comportamentos juntos)** |
| `[Clique Mouse]` | Definir o alvo do *Seek/Arrive* ou reposicionar alvo |
| `[T]` | Alternar entre *Arrive* suave e *Seek* direto (no Modo 1) |
| `[W, A, S, D]` / `Setas` | Controlar a movimentação do Jogador (no Modo 2 e 4) |
| `[U] / [J]` | Aumentar / Diminuir peso da **Separação** |
| `[I] / [K]` | Aumentar / Diminuir peso do **Alinhamento** |
| `[O] / [L]` | Aumentar / Diminuir peso da **Coesão** |
| `[D]` | Ativar / Desativar vetores e miras de depuração visual |
| `[R]` | Resetar posições e valores padrão dos agentes |

---

## 🚀 Como Executar o Projeto

### Pelo Terminal / Prompt de Comando:
```bash
./gradlew lwjgl3:run
```
*(No Windows, você pode usar `gradlew.bat lwjgl3:run`)*

### Gerar e Executar o Executável JAR:
```bash
./gradlew lwjgl3:jar
java -jar lwjgl3/build/libs/Game\ IA-1.0.0.jar
```
*(Ou execute pelo IntelliJ IDEA / Eclipse selecionando a classe `Lwjgl3Launcher`).*
