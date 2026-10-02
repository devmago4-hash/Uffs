import java.util.Random;

/**
 * Simula um tornado usando o modelo de vortice combinado de Rankine:
 * - Dentro do raio do nucleo (coreRadius): velocidade tangencial cresce linearmente com r (rotacao de corpo rigido)
 * - Fora do raio do nucleo: velocidade tangencial cai com 1/r (vortice potencial)
 *
 * O "controller" e a maquina de estados (TornadoPhase) que avanca ao longo do tempo,
 * alterando intensidade, raio, deslocamento (drift) e trajetoria.
 */
public class Tornado {

    public enum EFScale {
        EF0(29, 38, 40, 15),
        EF1(39, 49, 55, 25),
        EF2(50, 60, 70, 40),
        EF3(61, 74, 90, 55),
        EF4(75, 89, 110, 70),
        EF5(90, 130, 140, 90);

        public final double vMinMs, vMaxMs; // faixa de velocidade de vento em m/s (aprox convertido de mph)
        public final double maxRadius;       // raio maximo do nucleo em metros (escala do simulador)
        public final double damagePower;     // fator de dano estrutural

        EFScale(double vMin, double vMax, double maxRadius, double damagePower) {
            this.vMinMs = vMin;
            this.vMaxMs = vMax;
            this.maxRadius = maxRadius;
            this.damagePower = damagePower;
        }
    }

    private final Random rng = new Random();

    public TornadoPhase phase = TornadoPhase.CALMARIA;
    public EFScale scale;

    public Vector2 center;
    public Vector2 velocity; // deslocamento do tornado no mapa (drift)

    public double coreRadius = 0;      // raio atual do nucleo do vortice (cresce/diminui conforme a fase)
    public double maxTangentialSpeed = 0; // velocidade tangencial de pico atual
    public double funnelWidthFactor = 0;  // 0 a 1, controla o desenho visual do funil (afunilamento)
    public double rotationAngle = 0;      // angulo acumulado de rotacao visual

    private double phaseTimer = 0;
    private double totalTime = 0;

    // Duracoes de cada fase em segundos (ajustavel para acelerar a simulacao)
    private static final double T_INSTABILIDADE = 6.0;
    private static final double T_WALL_CLOUD = 5.0;
    private static final double T_FUNIL = 4.0;
    private static final double T_TOQUE = 3.0;
    private static final double T_MADURO = 14.0;
    private static final double T_ENFRAQUECENDO = 6.0;
    private static final double T_ROPE_OUT = 4.0;

    public Tornado(EFScale scale, Vector2 startCenter) {
        this.scale = scale;
        this.center = startCenter;
        this.velocity = new Vector2(6 + rng.nextDouble() * 4, -2 + rng.nextDouble() * 4);
    }

    public void setPhase(TornadoPhase p) {
        this.phase = p;
        this.phaseTimer = 0;
    }

    public boolean isDissipated() {
        return phase == TornadoPhase.DISSIPADO;
    }

    public boolean isOnGround() {
        return phase == TornadoPhase.TOQUE_NO_SOLO || phase == TornadoPhase.MADURO
                || phase == TornadoPhase.ENFRAQUECENDO || phase == TornadoPhase.ROPE_OUT;
    }

    /** Avanca a maquina de estados e a fisica do vortice em dt segundos. */
    public void update(double dt) {
        phaseTimer += dt;
        totalTime += dt;
        rotationAngle += dt * (2 + maxTangentialSpeed * 0.05);

        switch (phase) {
            case CALMARIA:
                funnelWidthFactor = 0;
                maxTangentialSpeed = 0;
                coreRadius = 0;
                break;

            case INSTABILIDADE:
                // cisalhamento de vento aumentando, ainda sem funil visivel
                funnelWidthFactor = 0;
                maxTangentialSpeed = lerp(0, scale.vMinMs * 0.3, phaseTimer / T_INSTABILIDADE);
                coreRadius = lerp(0, scale.maxRadius * 0.15, phaseTimer / T_INSTABILIDADE);
                if (phaseTimer >= T_INSTABILIDADE) setPhase(TornadoPhase.WALL_CLOUD);
                break;

            case WALL_CLOUD:
                // rotacao mesociclonica ganhando forma, funil comeca a descer
                funnelWidthFactor = lerp(0, 0.35, phaseTimer / T_WALL_CLOUD);
                maxTangentialSpeed = lerp(scale.vMinMs * 0.3, scale.vMinMs * 0.6, phaseTimer / T_WALL_CLOUD);
                coreRadius = lerp(scale.maxRadius * 0.15, scale.maxRadius * 0.35, phaseTimer / T_WALL_CLOUD);
                if (phaseTimer >= T_WALL_CLOUD) setPhase(TornadoPhase.FUNIL);
                break;

            case FUNIL:
                funnelWidthFactor = lerp(0.35, 0.7, phaseTimer / T_FUNIL);
                maxTangentialSpeed = lerp(scale.vMinMs * 0.6, scale.vMinMs * 0.9, phaseTimer / T_FUNIL);
                coreRadius = lerp(scale.maxRadius * 0.35, scale.maxRadius * 0.6, phaseTimer / T_FUNIL);
                if (phaseTimer >= T_FUNIL) setPhase(TornadoPhase.TOQUE_NO_SOLO);
                break;

            case TOQUE_NO_SOLO:
                funnelWidthFactor = lerp(0.7, 1.0, phaseTimer / T_TOQUE);
                maxTangentialSpeed = lerp(scale.vMinMs * 0.9, scale.vMaxMs, phaseTimer / T_TOQUE);
                coreRadius = lerp(scale.maxRadius * 0.6, scale.maxRadius, phaseTimer / T_TOQUE);
                if (phaseTimer >= T_TOQUE) setPhase(TornadoPhase.MADURO);
                break;

            case MADURO:
                // fase de pico: pequenas oscilacoes de intensidade para dar realismo (rajadas)
                funnelWidthFactor = 1.0;
                double gust = Math.sin(totalTime * 1.7) * 0.08 + Math.sin(totalTime * 0.6) * 0.05;
                maxTangentialSpeed = scale.vMaxMs * (1.0 + gust);
                coreRadius = scale.maxRadius * (1.0 + gust * 0.3);
                if (phaseTimer >= T_MADURO) setPhase(TornadoPhase.ENFRAQUECENDO);
                break;

            case ENFRAQUECENDO:
                funnelWidthFactor = lerp(1.0, 0.6, phaseTimer / T_ENFRAQUECENDO);
                maxTangentialSpeed = lerp(scale.vMaxMs, scale.vMinMs * 0.5, phaseTimer / T_ENFRAQUECENDO);
                coreRadius = lerp(scale.maxRadius, scale.maxRadius * 0.4, phaseTimer / T_ENFRAQUECENDO);
                if (phaseTimer >= T_ENFRAQUECENDO) setPhase(TornadoPhase.ROPE_OUT);
                break;

            case ROPE_OUT:
                // funil se estreita e "torce" como uma corda antes de sumir
                funnelWidthFactor = lerp(0.6, 0.1, phaseTimer / T_ROPE_OUT);
                maxTangentialSpeed = lerp(scale.vMinMs * 0.5, 0, phaseTimer / T_ROPE_OUT);
                coreRadius = lerp(scale.maxRadius * 0.4, scale.maxRadius * 0.1, phaseTimer / T_ROPE_OUT);
                velocity = velocity.scale(1.02); // acelera erraticamente antes de sumir
                if (phaseTimer >= T_ROPE_OUT) setPhase(TornadoPhase.DISSIPADO);
                break;

            case DISSIPADO:
                funnelWidthFactor = 0;
                maxTangentialSpeed = 0;
                break;
        }

        // Deslocamento do tornado no mapa (so se move de fato a partir da fase de toque no solo em diante,
        // mas ja "vagueia" um pouco antes disso para parecer natural)
        double driftFactor = isOnGround() ? 1.0 : 0.3;
        center = center.add(velocity.scale(dt * driftFactor));

        // pequena mudanca aleatoria de direcao (trajetoria erratica real de tornados)
        velocity = velocity.add(new Vector2((rng.nextDouble() - 0.5) * 0.6, (rng.nextDouble() - 0.5) * 0.6));
    }

    /**
     * Retorna o vetor de vento (direcao + magnitude em m/s) em um ponto do mundo,
     * usando o modelo de vortice combinado de Rankine.
     */
    public Vector2 windAt(Vector2 point) {
        if (maxTangentialSpeed <= 0.001) return new Vector2(0, 0);

        Vector2 rel = point.sub(center);
        double r = rel.length();
        if (r < 1e-6) return new Vector2(0, 0);

        double tangentialSpeed;
        if (r <= coreRadius) {
            // dentro do nucleo: rotacao de corpo rigido, cresce linearmente ate o raio do nucleo
            tangentialSpeed = maxTangentialSpeed * (r / coreRadius);
        } else {
            // fora do nucleo: vortice potencial, decai com 1/r
            tangentialSpeed = maxTangentialSpeed * (coreRadius / r);
        }

        Vector2 tangentDir = rel.normalized().perpendicular();
        return tangentDir.scale(tangentialSpeed);
    }

    public double windSpeedAt(Vector2 point) {
        return windAt(point).length();
    }

    /**
     * Aproxima a correnteza ascendente (updraft) proxima ao nucleo do tornado.
     * O ar succionado horizontalmente pelo vortice precisa subir por dentro do
     * centro de baixa pressao; aqui isso e modelado como um decaimento gaussiano
     * a partir do centro, com pico proporcional a velocidade tangencial maxima.
     * E o que da sustentacao para objetos leves serem erguidos, alem do arrasto puramente horizontal.
     */
    public double updraftAt(Vector2 point) {
        if (maxTangentialSpeed <= 0.001 || coreRadius <= 0.001) return 0;
        double r = point.sub(center).length();
        double sigma = coreRadius;
        double pico = maxTangentialSpeed * 0.6;
        return pico * Math.exp(-(r * r) / (2 * sigma * sigma));
    }

    /**
     * Perfil de vento tipo "lei de potencia" (power-law), usado em engenharia de vento:
     * a velocidade do vento cresce com a altura acima do solo por causa do atrito da superficie.
     * alturaRef = altura de referencia (~1.5m, onde a velocidade "de base" do vortice e valida).
     */
    public static double perfilVerticalDoVento(double altura) {
        double alturaRef = 1.5;
        double expoente = 0.14; // valor tipico para terreno aberto (categoria de rugosidade baixa)
        double h = Math.max(0.3, altura);
        return Math.pow(h / alturaRef, expoente);
    }

    private static double lerp(double a, double b, double t) {
        t = Math.max(0, Math.min(1, t));
        return a + (b - a) * t;
    }
}
