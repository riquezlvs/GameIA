package mackenzie.gameIA.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import java.util.List;

/**
 * Classe que modela o Agente autônomo baseado no modelo de Craig Reynolds (Steering Behaviors).
 * 
 * Atributos principais exigidos:
 * - posicao (Vector2)
 * - velocidade (Vector2)
 * - maxVelocidade (float / Vector2)
 * - maxForca (float / Vector2)
 */
public class Agente {

    // 1. Modelagem do Agente: Vetores de física e limites
    public Vector2 posicao;
    public Vector2 velocidade;
    public Vector2 aceleracao; // Força acumulada por quadro

    public float maxVelocidade;
    public float maxForca;
    public float massa = 1.0f;
    public float raio = 10.0f; // Raio físico para desenho e colisão visual

    // Estado da Máquina de Estados Finitos (FSM)
    public EstadoAgente estado = EstadoAgente.WANDER;
    public float raioVisao = 170.0f; // Raio de percepção para detectar jogador

    // Variáveis auxiliares para o comportamento Wander (Craig Reynolds)
    public float wanderAngle = MathUtils.random(0f, 360f);
    public Vector2 wanderCircleCenter = new Vector2();
    public Vector2 wanderTarget = new Vector2();

    // Posição prevista calculada para o Pursuit (usado para depuração)
    public Vector2 predictedTarget = new Vector2();

    // Guarda a última força resultante aplicada para depuração visual
    public Vector2 ultimaForca = new Vector2();

    // Cor do agente para renderização
    public Color cor = new Color(0.2f, 0.7f, 1f, 1f);

    public Agente(float x, float y, float maxVelocidade, float maxForca) {
        this.posicao = new Vector2(x, y);
        this.velocidade = new Vector2(0, 0);
        this.aceleracao = new Vector2(0, 0);
        this.maxVelocidade = maxVelocidade;
        this.maxForca = maxForca;
    }

    public Agente(Vector2 posicao, float maxVelocidade, float maxForca) {
        this(posicao.x, posicao.y, maxVelocidade, maxForca);
    }

    public Agente(Vector2 posicao, Vector2 velocidade, float maxVelocidade, float maxForca) {
        this.posicao = new Vector2(posicao);
        this.velocidade = new Vector2(velocidade);
        this.aceleracao = new Vector2(0, 0);
        this.maxVelocidade = maxVelocidade;
        this.maxForca = maxForca;
    }

    public Agente(Vector2 posicao, Vector2 velocidade, Vector2 maxVelocidade, Vector2 maxForca) {
        this(posicao, velocidade, maxVelocidade.len(), maxForca.len());
    }

    /**
     * Aplica uma força direcionadora (steering force) à aceleração do agente.
     * Baseado na segunda lei de Newton: a = F / m.
     */
    public void aplicarForca(Vector2 forca) {
        if (forca == null || forca.isZero()) return;
        Vector2 f = forca.cpy().scl(1.0f / massa);
        aceleracao.add(f);
        ultimaForca.set(forca); // Registra para desenho de depuração
    }

    /**
     * Atualização da física com integração de Euler.
     */
    public void atualizar(float delta) {
        // Atualiza a velocidade baseada na aceleração
        velocidade.add(aceleracao.x * delta, aceleracao.y * delta);
        velocidade.limit(maxVelocidade);

        // Atualiza a posição baseada na velocidade
        posicao.add(velocidade.x * delta, velocidade.y * delta);

        // Zera a aceleração para o próximo quadro
        aceleracao.setZero();
    }

    // =========================================================================
    // 2. SEEK (Busca)
    // =========================================================================
    /**
     * Comportamento Seek: calcula força para mover em direção ao alvo na velocidade máxima.
     * desired_velocity = normalize(alvo - posicao) * maxVelocidade
     * steering = desired_velocity - velocidade
     * steering = clamp(steering, maxForca)
     */
    public Vector2 seek(Vector2 alvo) {
        Vector2 desejada = alvo.cpy().sub(posicao);
        if (desejada.isZero()) return new Vector2();

        desejada.nor().scl(maxVelocidade);
        Vector2 direcaoForca = desejada.sub(velocidade);
        direcaoForca.limit(maxForca);
        return direcaoForca;
    }

    // =========================================================================
    // 2. ARRIVE (Chegada Suave)
    // =========================================================================
    /**
     * Comportamento Arrive: segue o alvo na velocidade máxima até entrar no raio de desaceleração,
     * reduzindo suavemente até parar no ponto.
     * 
     * @param alvo Ponto de destino
     * @param raioDesaceleracao Distância na qual a frenagem começa a agir
     */
    public Vector2 arrive(Vector2 alvo, float raioDesaceleracao) {
        Vector2 paraAlvo = alvo.cpy().sub(posicao);
        float distancia = paraAlvo.len();

        // Se já está praticamente no ponto (menos de 2.5 pixels), zera velocidade e para perfeitamente
        if (distancia < 2.5f) {
            if (velocidade.len() < 15.0f) {
                velocidade.setZero();
                return new Vector2();
            }
            Vector2 parar = velocidade.cpy().scl(-1f);
            parar.limit(maxForca);
            return parar;
        }

        // Velocidade desejada diminui linearmente dentro do raio de desaceleração
        float velocidadeDesejada;
        if (distancia < raioDesaceleracao) {
            velocidadeDesejada = maxVelocidade * (distancia / raioDesaceleracao);
        } else {
            velocidadeDesejada = maxVelocidade;
        }

        Vector2 desejada = paraAlvo.nor().scl(velocidadeDesejada);
        Vector2 direcaoForca = desejada.sub(velocidade);
        direcaoForca.limit(maxForca);
        return direcaoForca;
    }

    // =========================================================================
    // 3. WANDER (Vagando suavemente)
    // =========================================================================
    /**
     * Comportamento Wander de Craig Reynolds:
     * Projeta um círculo a uma distância à frente do agente e desloca um alvo
     * ao longo da sua circunferência adicionando ruído suave ao ângulo a cada frame.
     */
    public Vector2 wander(float delta, float wanderDist, float wanderRadius, float wanderJitter) {
        // Direção de avanço atual (ou (1,0) se parado)
        Vector2 direcao = velocidade.isZero(0.1f) ? new Vector2(1, 0) : velocidade.cpy().nor();

        // Centro do círculo de projeção
        wanderCircleCenter.set(posicao).add(direcao.scl(wanderDist));

        // Pequena variação aleatória no ângulo para movimento orgânico
        wanderAngle += MathUtils.random(-wanderJitter, wanderJitter) * delta;

        // Ponto alvo na circunferência do círculo
        Vector2 deslocamento = new Vector2(
                MathUtils.cosDeg(wanderAngle) * wanderRadius,
                MathUtils.sinDeg(wanderAngle) * wanderRadius
        );

        wanderTarget.set(wanderCircleCenter).add(deslocamento);

        // Busca o ponto calculado
        return seek(wanderTarget);
    }

    // =========================================================================
    // 3. PURSUIT (Perseguição Preditiva com FSM)
    // =========================================================================
    /**
     * Comportamento Pursuit de Craig Reynolds:
     * Prevê a posição futura do alvo com base na sua velocidade atual e na distância,
     * perseguindo essa posição futura em vez da posição atual.
     */
    public Vector2 pursuit(Agente alvo) {
        Vector2 paraAlvo = alvo.posicao.cpy().sub(this.posicao);
        float distancia = paraAlvo.len();

        // Tempo de predição baseado na velocidade relativa
        float velocidadeRelativa = this.maxVelocidade + alvo.velocidade.len();
        float tempoPredicao = distancia / (velocidadeRelativa + 0.001f);

        // Limita o tempo de predição para evitar predições astronômicas
        if (tempoPredicao > 1.2f) tempoPredicao = 1.2f;

        // Posição futura prevista
        predictedTarget.set(alvo.posicao).add(alvo.velocidade.cpy().scl(tempoPredicao));

        // Se estiver muito próximo, aplica arrive para não passar direto oscilando
        if (distancia < 60f) {
            return arrive(predictedTarget, 60f);
        }
        return seek(predictedTarget);
    }

    // =========================================================================
    // 4. BÔNUS: BANDO (FLOCKING)
    // =========================================================================

    /**
     * 1. Separação: Evita aglomeração com agentes muito próximos.
     * Cria uma força repulsiva inversamente proporcional à distância dos vizinhos.
     */
    public Vector2 separacao(List<Agente> bando, float raioSeparacao) {
        Vector2 steer = new Vector2();
        int totalVizinhos = 0;

        for (Agente vizinho : bando) {
            if (vizinho == this) continue;
            float d = posicao.dst(vizinho.posicao);
            if (d > 0 && d < raioSeparacao) {
                // Vetor apontando para longe do vizinho
                Vector2 repulsao = posicao.cpy().sub(vizinho.posicao);
                repulsao.nor();
                repulsao.scl(1.0f / d); // Quanto mais perto, maior a repulsão
                steer.add(repulsao);
                totalVizinhos++;
            }
        }

        if (totalVizinhos > 0) {
            steer.scl(1.0f / totalVizinhos);
            if (steer.len2() > 0) {
                steer.nor().scl(maxVelocidade);
                steer.sub(velocidade);
                steer.limit(maxForca);
            }
        }
        return steer;
    }

    /**
     * 2. Alinhamento: Faz o agente navegar na mesma direção média dos vizinhos.
     */
    public Vector2 alinhamento(List<Agente> bando, float raioVizinhanca) {
        Vector2 velMedia = new Vector2();
        int totalVizinhos = 0;

        for (Agente vizinho : bando) {
            if (vizinho == this) continue;
            float d = posicao.dst(vizinho.posicao);
            if (d > 0 && d < raioVizinhanca) {
                velMedia.add(vizinho.velocidade);
                totalVizinhos++;
            }
        }

        if (totalVizinhos > 0) {
            velMedia.scl(1.0f / totalVizinhos);
            velMedia.nor().scl(maxVelocidade);
            Vector2 steer = velMedia.sub(velocidade);
            steer.limit(maxForca);
            return steer;
        }
        return new Vector2();
    }

    /**
     * 3. Coesão: Atrai o agente em direção ao centro de massa dos vizinhos.
     */
    public Vector2 coesao(List<Agente> bando, float raioVizinhanca) {
        Vector2 centroMassa = new Vector2();
        int totalVizinhos = 0;

        for (Agente vizinho : bando) {
            if (vizinho == this) continue;
            float d = posicao.dst(vizinho.posicao);
            if (d > 0 && d < raioVizinhanca) {
                centroMassa.add(vizinho.posicao);
                totalVizinhos++;
            }
        }

        if (totalVizinhos > 0) {
            centroMassa.scl(1.0f / totalVizinhos);
            return seek(centroMassa);
        }
        return new Vector2();
    }

    /**
     * Combina os 3 comportamentos do bando com pesos configuráveis.
     */
    public void aplicarFlocking(List<Agente> bando, float pesoSep, float pesoAli, float pesoCoe,
                                float raioSep, float raioViz) {
        Vector2 fSep = separacao(bando, raioSep).scl(pesoSep);
        Vector2 fAli = alinhamento(bando, raioViz).scl(pesoAli);
        Vector2 fCoe = coesao(bando, raioViz).scl(pesoCoe);

        aplicarForca(fSep);
        aplicarForca(fAli);
        aplicarForca(fCoe);
    }

    // =========================================================================
    // LIMITES DE TELA
    // =========================================================================
    /**
     * Mantém os agentes dentro da tela com teletransporte nas bordas (wrap around)
     * ou reflexão suave.
     */
    public void limitarBordas(float largura, float altura, float margem, boolean wrap) {
        if (wrap) {
            if (posicao.x < -margem) posicao.x = largura + margem;
            if (posicao.x > largura + margem) posicao.x = -margem;
            if (posicao.y < -margem) posicao.y = altura + margem;
            if (posicao.y > altura + margem) posicao.y = -margem;
        } else {
            // Força suave para repelir das bordas
            Vector2 forcaBorda = new Vector2();
            if (posicao.x < margem) forcaBorda.x = maxForca;
            if (posicao.x > largura - margem) forcaBorda.x = -maxForca;
            if (posicao.y < margem) forcaBorda.y = maxForca;
            if (posicao.y > altura - margem) forcaBorda.y = -maxForca;
            aplicarForca(forcaBorda);
        }
    }

    // =========================================================================
    // RENDERIZAÇÃO E DEPURAÇÃO COM SHAPERENDERER
    // =========================================================================

    /**
     * Desenha o corpo do agente como um triângulo pontiagudo apontando na direção da velocidade.
     */
    public void desenharCorpo(ShapeRenderer sr) {
        float angulo = velocidade.len2() > 0.1f ? velocidade.angleDeg() : 0f;

        // Vértices do triângulo alinhado à rotação
        float r = raio;
        Vector2 ponta = new Vector2(r * 1.5f, 0).rotateDeg(angulo).add(posicao);
        Vector2 asaEsquerda = new Vector2(-r, r * 0.75f).rotateDeg(angulo).add(posicao);
        Vector2 asaDireita = new Vector2(-r, -r * 0.75f).rotateDeg(angulo).add(posicao);

        sr.setColor(cor);
        sr.triangle(ponta.x, ponta.y, asaEsquerda.x, asaEsquerda.y, asaDireita.x, asaDireita.y);
    }

    /**
     * Desenha os vetores de velocidade (verde) e força resultante (vermelho)
     * para depuração visual conforme solicitado na dica do exercício.
     */
    public void desenharVetores(ShapeRenderer sr, float escalaVel, float escalaForca) {
        // 1. Vetor Velocidade (Linha Verde)
        if (velocidade.len2() > 0.5f) {
            float velX = posicao.x + velocidade.x * escalaVel;
            float velY = posicao.y + velocidade.y * escalaVel;
            sr.setColor(0.1f, 1f, 0.2f, 1f); // Verde brilhante
            sr.line(posicao.x, posicao.y, velX, velY);

            // Pequeno ponto ou círculo na ponta do vetor de velocidade
            sr.circle(velX, velY, 2.5f);
        }

        // 2. Vetor Força Resultante (Linha Vermelha)
        if (ultimaForca.len2() > 0.5f) {
            float forcaX = posicao.x + ultimaForca.x * escalaForca;
            float forcaY = posicao.y + ultimaForca.y * escalaForca;
            sr.setColor(1f, 0.2f, 0.2f, 1f); // Vermelho vibrante
            sr.line(posicao.x, posicao.y, forcaX, forcaY);

            // Pequeno ponto na ponta do vetor força
            sr.circle(forcaX, forcaY, 2.5f);
        }
    }

    /**
     * Desenha o círculo de visão do agente (usado na FSM de Wander/Pursuit).
     */
    public void desenharVisao(ShapeRenderer sr, boolean vendoJogador) {
        if (vendoJogador) {
            sr.setColor(1f, 0.2f, 0.2f, 0.35f); // Vermelho translúcido quando alerta
        } else {
            sr.setColor(1f, 0.85f, 0.1f, 0.25f); // Amarelo quando calmo
        }
        sr.circle(posicao.x, posicao.y, raioVisao, 32);
    }

    /**
     * Desenha o círculo e o alvo projetado do comportamento Wander.
     */
    public void desenharWanderDebug(ShapeRenderer sr, float wanderRadius) {
        // Círculo projetado
        sr.setColor(0.3f, 0.8f, 1f, 0.3f);
        sr.circle(wanderCircleCenter.x, wanderCircleCenter.y, wanderRadius, 24);

        // Linha conectando o centro ao alvo na circunferência
        sr.setColor(0.3f, 0.8f, 1f, 0.8f);
        sr.line(wanderCircleCenter.x, wanderCircleCenter.y, wanderTarget.x, wanderTarget.y);
        sr.circle(wanderTarget.x, wanderTarget.y, 4f);
    }
}
