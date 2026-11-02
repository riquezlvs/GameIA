package mackenzie.gameIA.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Atividade Prática - Inteligência Artificial para Jogos (libGDX)
 * 
 * 1. Modelagem do Agente: Posição, velocidade, maxVelocidade, maxForca (Vector2).
 * 2. Seek e Arrive: NPC segue o clique do mouse e para suavemente no ponto.
 * 3. Wander + FSM: Sem alvo, o NPC vaga; ao ver o jogador, troca para pursuit.
 * 4. Bônus (Bando): 30 agentes com separação, alinhamento e coesão (pesos ajustáveis).
 * Dica: Desenho dos vetores de velocidade e força com ShapeRenderer para depuração.
 */
public class GameIA extends ApplicationAdapter {

    // Modos de Demonstração
    public static final int MODO_SEEK_ARRIVE = 1;
    public static final int MODO_WANDER_FSM  = 2;
    public static final int MODO_BANDO       = 3;
    public static final int MODO_TODOS       = 4;

    private int modoAtual = MODO_SEEK_ARRIVE;

    // Renderização e Câmera
    private OrthographicCamera camera;
    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;
    private BitmapFont font;

    // Configurações Globais
    private boolean desenharDebug = true;
    private final Vector3 mouseUnproject = new Vector3();

    // =========================================================================
    // VARIÁVEIS DO MODO 1: SEEK & ARRIVE
    // =========================================================================
    private Agente npcSeekArrive;
    private Vector2 alvoMouse;
    private boolean usarArrive = true; // Permite alternar entre Seek puro e Arrive
    private final float RAIO_DESACELERACAO = 120.0f;

    // =========================================================================
    // VARIÁVEIS DO MODO 2: WANDER + FSM (PURSUIT)
    // =========================================================================
    private Agente npcWanderFSM;
    private Agente jogador;

    // =========================================================================
    // VARIÁVEIS DO MODO 3: BANDO (FLOCKING - 30 AGENTES)
    // =========================================================================
    private final List<Agente> bando = new ArrayList<>(30);
    private float pesoSeparacao = 1.5f;
    private float pesoAlinhamento = 1.0f;
    private float pesoCoesao = 1.0f;
    private final float raioSeparacao = 35.0f;
    private final float raioVizinhanca = 75.0f;

    @Override
    public void create() {
        // Inicializa câmera para viewport de 800x600
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);

        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.setColor(Color.WHITE);

        // Inicializa os agentes de cada modo
        alvoMouse = new Vector2(400, 300);
        inicializarModoSeekArrive();
        inicializarModoWanderFSM();
        inicializarModoBando();
    }

    private void inicializarModoSeekArrive() {
        npcSeekArrive = new Agente(150, 150, 220f, 350f);
        npcSeekArrive.cor.set(0.2f, 0.8f, 1f, 1f); // Azul ciano
    }

    private void inicializarModoWanderFSM() {
        // NPC que vaga e persegue
        npcWanderFSM = new Agente(200, 300, 160f, 250f);
        npcWanderFSM.raioVisao = 180.0f;
        npcWanderFSM.estado = EstadoAgente.WANDER;
        npcWanderFSM.cor.set(0.3f, 0.9f, 0.4f, 1f);

        // Jogador controlado pelo usuário
        jogador = new Agente(600, 300, 190f, 400f);
        jogador.cor.set(1f, 0.85f, 0.2f, 1f); // Dourado
        jogador.raio = 12f;
    }

    private void inicializarModoBando() {
        bando.clear();
        for (int i = 0; i < 30; i++) {
            float x = MathUtils.random(100f, 700f);
            float y = MathUtils.random(100f, 500f);
            Agente boid = new Agente(x, y, 170f, 220f);
            // Velocidade inicial aleatória
            float angulo = MathUtils.random(0f, 360f);
            boid.velocidade.set(MathUtils.cosDeg(angulo), MathUtils.sinDeg(angulo)).scl(100f);
            boid.cor.set(0.6f + MathUtils.random(0.4f), 0.3f + MathUtils.random(0.5f), 1f, 1f);
            boid.raio = 8f;
            bando.add(boid);
        }
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        if (delta > 0.05f) delta = 0.05f; // Protege contra picos de lag

        // Processa entradas do teclado e mouse
        processarEntradas();

        // Atualiza a lógica do modo ativo
        atualizar(delta);

        // Limpa a tela com fundo escuro elegante
        ScreenUtils.clear(0.08f, 0.09f, 0.12f, 1f);

        camera.update();
        shapeRenderer.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        // Renderiza elementos visuais (geometria e vetores)
        desenharGeometria();

        // Renderiza textos e interface na tela (HUD)
        desenharHUD();
    }

    // =========================================================================
    // ENTRADAS DO USUÁRIO
    // =========================================================================
    private void processarEntradas() {
        // Seleção de modos [1] a [4]
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) modoAtual = MODO_SEEK_ARRIVE;
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) modoAtual = MODO_WANDER_FSM;
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) modoAtual = MODO_BANDO;
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) modoAtual = MODO_TODOS;

        // Alternar depuração de vetores [D]
        if (Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            desenharDebug = !desenharDebug;
        }

        // Resetar posições [R]
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            inicializarModoSeekArrive();
            inicializarModoWanderFSM();
            inicializarModoBando();
        }

        // Alternar Seek / Arrive no Modo 1 [T]
        if (Gdx.input.isKeyJustPressed(Input.Keys.T)) {
            usarArrive = !usarArrive;
        }

        // Captura do clique do mouse para definir o alvo
        if (Gdx.input.isTouched()) {
            mouseUnproject.set(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(mouseUnproject);
            alvoMouse.set(mouseUnproject.x, mouseUnproject.y);
        }

        // Controle do Jogador no Modo 2 e 4 (Teclas WASD ou Setas)
        Vector2 forcaJogador = new Vector2();
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) forcaJogador.y += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) forcaJogador.y -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) forcaJogador.x -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) forcaJogador.x += 1;

        if (!forcaJogador.isZero()) {
            forcaJogador.nor().scl(jogador.maxVelocidade);
            Vector2 steer = forcaJogador.sub(jogador.velocidade);
            steer.limit(jogador.maxForca);
            jogador.aplicarForca(steer);
        } else {
            // Atrito para desacelerar quando soltar as teclas
            Vector2 atrito = jogador.velocidade.cpy().scl(-2.5f);
            atrito.limit(jogador.maxForca);
            jogador.aplicarForca(atrito);
        }

        // Ajuste dos pesos do Bando (Modo 3 e 4)
        // [U] / [J] -> Separação
        if (Gdx.input.isKeyJustPressed(Input.Keys.U)) pesoSeparacao = Math.min(5.0f, pesoSeparacao + 0.25f);
        if (Gdx.input.isKeyJustPressed(Input.Keys.J)) pesoSeparacao = Math.max(0.0f, pesoSeparacao - 0.25f);

        // [I] / [K] -> Alinhamento
        if (Gdx.input.isKeyJustPressed(Input.Keys.I)) pesoAlinhamento = Math.min(5.0f, pesoAlinhamento + 0.25f);
        if (Gdx.input.isKeyJustPressed(Input.Keys.K)) pesoAlinhamento = Math.max(0.0f, pesoAlinhamento - 0.25f);

        // [O] / [L] -> Coesão
        if (Gdx.input.isKeyJustPressed(Input.Keys.O)) pesoCoesao = Math.min(5.0f, pesoCoesao + 0.25f);
        if (Gdx.input.isKeyJustPressed(Input.Keys.L)) pesoCoesao = Math.max(0.0f, pesoCoesao - 0.25f);
    }

    // =========================================================================
    // ATUALIZAÇÃO DA LÓGICA DE IA
    // =========================================================================
    private void atualizar(float delta) {
        float w = 800f;
        float h = 600f;

        // Atualização Modo 1: Seek & Arrive
        if (modoAtual == MODO_SEEK_ARRIVE || modoAtual == MODO_TODOS) {
            Vector2 forcaSteering;
            if (usarArrive) {
                forcaSteering = npcSeekArrive.arrive(alvoMouse, RAIO_DESACELERACAO);
                npcSeekArrive.estado = EstadoAgente.ARRIVE;
            } else {
                forcaSteering = npcSeekArrive.seek(alvoMouse);
                npcSeekArrive.estado = EstadoAgente.SEEK;
            }
            npcSeekArrive.aplicarForca(forcaSteering);
            npcSeekArrive.atualizar(delta);
            npcSeekArrive.limitarBordas(w, h, 20f, false);
        }

        // Atualização Modo 2: Wander + FSM (Pursuit)
        if (modoAtual == MODO_WANDER_FSM || modoAtual == MODO_TODOS) {
            jogador.atualizar(delta);
            jogador.limitarBordas(w, h, 15f, true);

            float distanciaAoJogador = npcWanderFSM.posicao.dst(jogador.posicao);

            // Transições da FSM:
            // "Sem alvo, o NPC vaga; ao ver o jogador, troca para pursuit."
            if (npcWanderFSM.estado == EstadoAgente.WANDER) {
                // Comportamento Wander
                Vector2 forcaWander = npcWanderFSM.wander(delta, 75f, 40f, 100f);
                npcWanderFSM.aplicarForca(forcaWander);
                npcWanderFSM.cor.set(0.3f, 0.9f, 0.4f, 1f); // Verde quando vagando calmo

                // Transição: detectou o jogador dentro do campo de visão
                if (distanciaAoJogador < npcWanderFSM.raioVisao) {
                    npcWanderFSM.estado = EstadoAgente.PURSUIT;
                }
            } else if (npcWanderFSM.estado == EstadoAgente.PURSUIT) {
                // Comportamento Pursuit (Perseguição preditiva)
                Vector2 forcaPursuit = npcWanderFSM.pursuit(jogador);
                npcWanderFSM.aplicarForca(forcaPursuit);
                npcWanderFSM.cor.set(1f, 0.25f, 0.25f, 1f); // Vermelho em alerta/perseguição

                // Transição: perdeu o jogador de vista (com histerese de 1.4x para evitar oscilações)
                if (distanciaAoJogador > npcWanderFSM.raioVisao * 1.45f) {
                    npcWanderFSM.estado = EstadoAgente.WANDER;
                }
            }

            npcWanderFSM.atualizar(delta);
            npcWanderFSM.limitarBordas(w, h, 20f, true);
        }

        // Atualização Modo 3: Bando (Flocking - 30 Agentes)
        if (modoAtual == MODO_BANDO || modoAtual == MODO_TODOS) {
            for (Agente boid : bando) {
                boid.aplicarFlocking(bando, pesoSeparacao, pesoAlinhamento, pesoCoesao, raioSeparacao, raioVizinhanca);

                // Pequeno impulso para frente se estiver muito lento
                if (boid.velocidade.len2() < 400f) {
                    Vector2 impulso = boid.velocidade.isZero() ? new Vector2(1, 0) : boid.velocidade.cpy().nor();
                    boid.aplicarForca(impulso.scl(boid.maxForca * 0.5f));
                }

                boid.atualizar(delta);
                boid.limitarBordas(w, h, 15f, true);
            }
        }
    }

    // =========================================================================
    // RENDERIZAÇÃO GEOMÉTRICA (SHAPERENDERER)
    // =========================================================================
    private void desenharGeometria() {
        // Habilita blending para círculos translúcidos
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // 1. ÁREAS TRANSLÚCIDAS E CÍRCULOS DE DEPURAÇÃO
        shapeRenderer.begin(ShapeType.Filled);

        if (modoAtual == MODO_SEEK_ARRIVE || modoAtual == MODO_TODOS) {
            // Desenha o raio de desaceleração do Arrive
            if (usarArrive) {
                shapeRenderer.setColor(0.2f, 0.7f, 1f, 0.12f);
                shapeRenderer.circle(alvoMouse.x, alvoMouse.y, RAIO_DESACELERACAO, 36);
            }
        }

        if (modoAtual == MODO_WANDER_FSM || modoAtual == MODO_TODOS) {
            // Desenha o campo de visão do NPC
            boolean emPerseguicao = (npcWanderFSM.estado == EstadoAgente.PURSUIT);
            npcWanderFSM.desenharVisao(shapeRenderer, emPerseguicao);
        }

        shapeRenderer.end();

        // 2. LINHAS DE CONTORNO, VETORES E ALVOS
        shapeRenderer.begin(ShapeType.Line);

        if (modoAtual == MODO_SEEK_ARRIVE || modoAtual == MODO_TODOS) {
            // Contorno do raio de desaceleração
            if (usarArrive) {
                shapeRenderer.setColor(0.2f, 0.7f, 1f, 0.45f);
                shapeRenderer.circle(alvoMouse.x, alvoMouse.y, RAIO_DESACELERACAO, 36);
            }

            // Alvo do clique do mouse (Miras concêntricas)
            shapeRenderer.setColor(1f, 0.9f, 0.2f, 0.9f);
            shapeRenderer.circle(alvoMouse.x, alvoMouse.y, 8f);
            shapeRenderer.circle(alvoMouse.x, alvoMouse.y, 3f);
            shapeRenderer.line(alvoMouse.x - 12, alvoMouse.y, alvoMouse.x + 12, alvoMouse.y);
            shapeRenderer.line(alvoMouse.x, alvoMouse.y - 12, alvoMouse.x, alvoMouse.y + 12);

            // Linha tracejada indicando o caminho até o alvo
            shapeRenderer.setColor(1f, 1f, 1f, 0.2f);
            shapeRenderer.line(npcSeekArrive.posicao.x, npcSeekArrive.posicao.y, alvoMouse.x, alvoMouse.y);
        }

        if (modoAtual == MODO_WANDER_FSM || modoAtual == MODO_TODOS) {
            // Depuração visual do Wander (Círculo projetado à frente)
            if (desenharDebug && npcWanderFSM.estado == EstadoAgente.WANDER) {
                npcWanderFSM.desenharWanderDebug(shapeRenderer, 40f);
            }

            // Depuração visual do Pursuit (Posição futura prevista)
            if (desenharDebug && npcWanderFSM.estado == EstadoAgente.PURSUIT) {
                shapeRenderer.setColor(1f, 0.3f, 0.8f, 0.8f);
                shapeRenderer.circle(npcWanderFSM.predictedTarget.x, npcWanderFSM.predictedTarget.y, 6f);
                shapeRenderer.line(jogador.posicao.x, jogador.posicao.y,
                                   npcWanderFSM.predictedTarget.x, npcWanderFSM.predictedTarget.y);
            }
        }

        // Desenha os vetores de depuração (Velocidade = Verde, Força = Vermelho)
        if (desenharDebug) {
            float escalaVel = 0.35f;
            float escalaForca = 0.25f;

            if (modoAtual == MODO_SEEK_ARRIVE || modoAtual == MODO_TODOS) {
                npcSeekArrive.desenharVetores(shapeRenderer, escalaVel, escalaForca);
            }

            if (modoAtual == MODO_WANDER_FSM || modoAtual == MODO_TODOS) {
                npcWanderFSM.desenharVetores(shapeRenderer, escalaVel, escalaForca);
                jogador.desenharVetores(shapeRenderer, escalaVel, escalaForca);
            }

            if (modoAtual == MODO_BANDO || modoAtual == MODO_TODOS) {
                // Desenha os vetores para uma amostra do bando para manter a clareza
                for (int i = 0; i < Math.min(10, bando.size()); i++) {
                    bando.get(i).desenharVetores(shapeRenderer, 0.25f, 0.2f);
                }
            }
        }

        shapeRenderer.end();

        // 3. DESENHO DOS CORPOS DOS AGENTES (TRIÂNGULOS APONTANDO NA DIREÇÃO)
        shapeRenderer.begin(ShapeType.Filled);

        if (modoAtual == MODO_SEEK_ARRIVE || modoAtual == MODO_TODOS) {
            npcSeekArrive.desenharCorpo(shapeRenderer);
        }

        if (modoAtual == MODO_WANDER_FSM || modoAtual == MODO_TODOS) {
            npcWanderFSM.desenharCorpo(shapeRenderer);
            jogador.desenharCorpo(shapeRenderer);
        }

        if (modoAtual == MODO_BANDO || modoAtual == MODO_TODOS) {
            for (Agente boid : bando) {
                boid.desenharCorpo(shapeRenderer);
            }
        }

        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    // =========================================================================
    // HUD / INTERFACE COM INSTRUÇÕES E DIAGNÓSTICO
    // =========================================================================
    private void desenharHUD() {
        // Fundo semitransparente superior e inferior para legibilidade impecável
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.65f);
        // Barra Superior
        shapeRenderer.rect(0, 520, 800, 80);
        // Barra Inferior de Legenda
        shapeRenderer.rect(0, 0, 800, 32);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();

        // Linha 1: Título e Seleção de Modos
        font.setColor(Color.WHITE);
        font.draw(batch, "IA PARA JOGOS (libGDX) - STEERING BEHAVIORS", 15, 590);

        font.setColor(0.7f, 0.85f, 1f, 1f);
        font.draw(batch, "Modos: [1] Seek & Arrive  |  [2] Wander + FSM  |  [3] Bando (30 Agentes)  |  [4] Sandbox Geral", 15, 568);

        // Linha 2: Informações contextuais do modo ativo
        switch (modoAtual) {
            case MODO_SEEK_ARRIVE:
                font.setColor(Color.YELLOW);
                String tipo = usarArrive ? "ARRIVE (Parada Suave)" : "SEEK (Busca Direta)";
                float dist = npcSeekArrive.posicao.dst(alvoMouse);
                float vel = npcSeekArrive.velocidade.len();
                font.draw(batch, String.format("Modo Atual: %s | [T] Alternar Modo | Distancia: %.1f px | Vel: %.1f px/s", tipo, dist, vel), 15, 542);
                break;

            case MODO_WANDER_FSM:
                float distNpcJog = npcWanderFSM.posicao.dst(jogador.posicao);
                boolean vendo = (npcWanderFSM.estado == EstadoAgente.PURSUIT);
                font.setColor(vendo ? Color.RED : Color.GREEN);
                font.draw(batch, String.format("NPC Estado: %s | Dist. Jogador: %.1f px (Visao: %.0f px)",
                        npcWanderFSM.estado.getDescricao(), distNpcJog, npcWanderFSM.raioVisao), 15, 542);
                font.setColor(0.9f, 0.9f, 0.9f, 1f);
                font.draw(batch, "Controle o Jogador com [W, A, S, D] ou Setas para entrar/sair do campo de visao!", 15, 526);
                break;

            case MODO_BANDO:
                font.setColor(0.4f, 0.9f, 1f, 1f);
                font.draw(batch, String.format("Bando: %d agentes | [U/J] Separacao: %.2f | [I/K] Alinhamento: %.2f | [O/L] Coesao: %.2f",
                        bando.size(), pesoSeparacao, pesoAlinhamento, pesoCoesao), 15, 542);
                break;

            case MODO_TODOS:
                font.setColor(Color.ORANGE);
                font.draw(batch, "Modo Sandbox Geral: Seek/Arrive, Wander+FSM e Bando atuando simultaneamente!", 15, 542);
                break;
        }

        // Rodapé: Teclas de Atalho e Legenda de Vetores
        font.setColor(0.2f, 1f, 0.3f, 1f);
        font.draw(batch, "-> Verde: Velocidade", 15, 20);

        font.setColor(1f, 0.3f, 0.3f, 1f);
        font.draw(batch, "-> Vermelho: Forca (Steering)", 165, 20);

        font.setColor(0.9f, 0.9f, 0.9f, 1f);
        font.draw(batch, String.format("[D] Vetores Debug (%s)  |  [R] Resetar  |  Clique para Alvo  |  FPS: %d",
                desenharDebug ? "LIGADO" : "DESLIGADO", Gdx.graphics.getFramesPerSecond()), 360, 20);

        batch.end();
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        batch.dispose();
        font.dispose();
    }
}
