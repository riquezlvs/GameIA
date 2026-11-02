package mackenzie.gameIA.game;

/**
 * Estados da Máquina de Estados Finitos (FSM) do agente.
 */
public enum EstadoAgente {
    SEEK("Seek (Perseguir Alvo)"),
    ARRIVE("Arrive (Chegada Suave)"),
    WANDER("Wander (Vagando)"),
    PURSUIT("Pursuit (Perseguição Preditiva)"),
    FLOCKING("Bando (Flocking)");

    private final String descricao;

    EstadoAgente(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
