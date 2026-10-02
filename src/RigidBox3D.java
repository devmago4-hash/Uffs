import java.util.ArrayList;
import java.util.List;
import java.awt.Color;

/**
 * Corpo rigido em formato de caixa, com fisica de arrasto aerodinamico real:
 *   F_arrasto = 0.5 * rho_ar * Cd * A_frontal * v_relativa^2   (na direcao da velocidade relativa)
 * A velocidade critica de arrancamento (a partir de qual o objeto comeca a ser
 * levantado/arrastado) e DERIVADA da massa e da area frontal, igualando arrasto e peso:
 *   0.5 * rho_ar * Cd * A * v_crit^2 = massa * g   =>   v_crit = sqrt(2*massa*g / (rho_ar*Cd*A))
 * Isso e o que faz uma lixeira leve voar bem antes de um carro pesado, sem numeros chutados.
 */
public class RigidBox3D {
    private static final double AR_DENSIDADE = 1.225; // kg/m3, ar ao nivel do mar
    private static final double GRAVIDADE = 9.81;

    public Vector3 pos;
    public Vector3 vel = new Vector3(0, 0, 0);
    public double spin = 0;
    public boolean voando = false;

    public final double largura, altura, comprimento;
    public final double massa;
    public final double coefArrasto;
    public final double velocidadeCritica;

    public Color corCorpo, corTopo;

    public RigidBox3D(Vector3 pos, double largura, double altura, double comprimento,
                       double massa, double coefArrasto, Color corCorpo, Color corTopo) {
        this.pos = pos;
        this.largura = largura;
        this.altura = altura;
        this.comprimento = comprimento;
        this.massa = massa;
        this.coefArrasto = coefArrasto;
        this.corCorpo = corCorpo;
        this.corTopo = corTopo;

        double areaFrontal = largura * altura;
        this.velocidadeCritica = Math.sqrt((2 * massa * GRAVIDADE) / (AR_DENSIDADE * coefArrasto * areaFrontal));
    }

    public void update(Tornado tornado, double dt) {
        Vector2 wind2 = tornado.windAt(new Vector2(pos.x, pos.z));

        if (!voando) {
            if (wind2.length() > velocidadeCritica) {
                voando = true;
            } else {
                return; // parado - sem calculo de forcas em vento fraco, evita tremulacao numerica
            }
        }

        double perfil = Tornado.perfilVerticalDoVento(pos.y + altura / 2);
        double updraft = tornado.updraftAt(new Vector2(pos.x, pos.z));
        Vector3 ventoEfetivo = new Vector3(wind2.x * perfil, updraft, wind2.y * perfil);

        Vector3 velRelativa = ventoEfetivo.sub(vel);
        double velRelSpeed = velRelativa.length();
        double areaFrontal = largura * altura;
        Vector3 forcaArrasto = velRelSpeed < 1e-6
                ? new Vector3(0, 0, 0)
                : velRelativa.normalized().scale(0.5 * AR_DENSIDADE * coefArrasto * areaFrontal * velRelSpeed * velRelSpeed);
        Vector3 forcaGravidade = new Vector3(0, -GRAVIDADE * massa, 0);

        Vector3 aceleracao = forcaArrasto.add(forcaGravidade).scale(1.0 / massa);
        vel = vel.add(aceleracao.scale(dt));
        pos = pos.add(vel.scale(dt));
        spin += dt * (1.5 + velRelSpeed * 0.05);

        if (pos.y < 0) {
            pos = pos.withY(0);
            if (vel.y < 0) vel = vel.withY(0);
            vel = vel.scale(0.9); // atrito ao arrastar/quicar no chao
        }
    }

    public List<Face3D> render() {
        List<Face3D> out = new ArrayList<>();
        double hw = largura / 2, hc = comprimento / 2;

        Vector3[] local = new Vector3[]{
                new Vector3(-hw, 0, -hc), new Vector3(hw, 0, -hc), new Vector3(hw, 0, hc), new Vector3(-hw, 0, hc),
                new Vector3(-hw, altura, -hc), new Vector3(hw, altura, -hc), new Vector3(hw, altura, hc), new Vector3(-hw, altura, hc)
        };
        Vector3[] world = new Vector3[8];
        for (int i = 0; i < 8; i++) {
            Vector3 rotado = voando ? local[i].rotateY(spin) : local[i];
            world[i] = rotado.add(pos);
        }

        out.add(new Face3D(new Vector3[]{world[0], world[1], world[2], world[3]}, corCorpo));
        out.add(new Face3D(new Vector3[]{world[4], world[5], world[6], world[7]}, corTopo));
        out.add(new Face3D(new Vector3[]{world[0], world[1], world[5], world[4]}, corCorpo));
        out.add(new Face3D(new Vector3[]{world[2], world[3], world[7], world[6]}, corCorpo));
        out.add(new Face3D(new Vector3[]{world[3], world[0], world[4], world[7]}, corCorpo));
        out.add(new Face3D(new Vector3[]{world[1], world[2], world[6], world[5]}, corCorpo));
        return out;
    }
}
