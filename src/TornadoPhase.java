public enum TornadoPhase {
    CALMARIA("Calmaria - condicoes atmosfericas estaveis"),
    INSTABILIDADE("Instabilidade - formacao de cumulonimbus e cisalhamento de vento"),
    WALL_CLOUD("Wall Cloud - rotacao mesociclonica visivel na base da nuvem"),
    FUNIL("Funil - condensacao formando o funil, ainda sem tocar o solo"),
    TOQUE_NO_SOLO("Toque no solo - tornado tocou a superficie, inicio da fase destrutiva"),
    MADURO("Maduro - intensidade e diametro maximos"),
    ENFRAQUECENDO("Enfraquecendo - funil comeca a se estreitar e inclinar"),
    ROPE_OUT("Rope-out - fase de corda, tornado se dissipando"),
    DISSIPADO("Dissipado - fim do evento");

    public final String descricao;

    TornadoPhase(String descricao) {
        this.descricao = descricao;
    }
}
