import java.util.ArrayList;
import java.util.List;
import java.awt.Color;

/**
 * Poste (rua/energia): nao voa como um objeto livre - ele tomba em torno da base,
 * como uma haste engastada. O torque do vento e comparado a resistencia estrutural
 * da base (TORQUE_FALHA); uma vez excedida, o poste comeca a ceder e o proprio
 * amolecimento faz a queda acelerar (a resistencia residual cai para 25% do limiar).
 */
public class Pole3D {
    private static final double AR_DENSIDADE = 1.225;
    private static final double TORQUE_FALHA = 2200; // N*m - resistencia estrutural da base

    public Vector3 base;
    public double altura;
    public double tiltAngle = 0;
    public double angularVel = 0;
    private Vector3 eixoTombamento = null;

    private final double massa = 120;
    private final double diametro = 0.18;
    private final double coefArrasto = 1.2;
    private final double momentoInercia; // I = m*L^2/3 (haste girando pela base)

    public Pole3D(Vector3 base, double altura) {
        this.base = base;
        this.altura = altura;
        this.momentoInercia = massa * altura * altura / 3.0;
    }

    public void update(Tornado tornado, double dt) {
        if (tiltAngle >= Math.PI / 2 - 0.001) return;

        Vector2 wind2 = tornado.windAt(new Vector2(base.x, base.z));
        double windSpeedGround = wind2.length();
        if (windSpeedGround < 0.01 && tiltAngle <= 0.001) return;

        double alturaEfetiva = Math.max(0.5, (altura / 2.0) * Math.cos(tiltAngle));
        double perfil = Tornado.perfilVerticalDoVento(alturaEfetiva);
        double velEfetiva = windSpeedGround * perfil;

        double areaFrontal = diametro * altura * Math.cos(tiltAngle);
        double forca = 0.5 * AR_DENSIDADE * coefArrasto * areaFrontal * velEfetiva * velEfetiva;
        double bracoAlavanca = (altura / 2.0) * Math.cos(tiltAngle);
        double torque = forca * bracoAlavanca;

        if (eixoTombamento == null && torque > TORQUE_FALHA) {
            Vector3 dirVento = new Vector3(wind2.x, 0, wind2.y).normalized();
            eixoTombamento = new Vector3(-dirVento.z, 0, dirVento.x);
        }

        if (eixoTombamento != null) {
            double torqueLiquido = Math.max(0, torque - TORQUE_FALHA * 0.25);
            double amortecimento = angularVel * 8;
            double angularAccel = Math.max(0, (torqueLiquido - amortecimento) / momentoInercia);
            angularVel += angularAccel * dt;
            tiltAngle += angularVel * dt;
            if (tiltAngle > Math.PI / 2) {
                tiltAngle = Math.PI / 2;
                angularVel = 0;
            }
        }
    }

    public List<Face3D> render() {
        List<Face3D> out = new ArrayList<>();
        double hd = diametro / 2;

        Vector3[] localHaste = new Vector3[]{
                new Vector3(-hd, 0, -hd), new Vector3(hd, 0, -hd), new Vector3(hd, altura, -hd), new Vector3(-hd, altura, -hd),
                new Vector3(hd, 0, -hd), new Vector3(hd, 0, hd), new Vector3(hd, altura, hd), new Vector3(hd, altura, -hd),
                new Vector3(hd, 0, hd), new Vector3(-hd, 0, hd), new Vector3(-hd, altura, hd), new Vector3(hd, altura, hd),
                new Vector3(-hd, 0, hd), new Vector3(-hd, 0, -hd), new Vector3(-hd, altura, -hd), new Vector3(-hd, altura, hd)
        };

        double lampSize = 0.35;
        Vector3[] localLampada = new Vector3[]{
                new Vector3(-lampSize / 2, altura, -lampSize / 2), new Vector3(lampSize / 2, altura, -lampSize / 2),
                new Vector3(lampSize / 2, altura + lampSize, -lampSize / 2), new Vector3(-lampSize / 2, altura + lampSize, -lampSize / 2)
        };

        Vector3 eixo = eixoTombamento != null ? eixoTombamento : new Vector3(1, 0, 0);
        Color corPoste = new Color(110, 110, 115);
        Color corLampada = new Color(230, 220, 150);

        for (int face = 0; face < 4; face++) {
            Vector3[] mundo = new Vector3[4];
            for (int i = 0; i < 4; i++) {
                Vector3 v = localHaste[face * 4 + i];
                Vector3 rot = tiltAngle > 0.0001 ? v.rotateAroundAxis(eixo, tiltAngle) : v;
                mundo[i] = rot.add(base);
            }
            out.add(new Face3D(mundo, corPoste));
        }

        Vector3[] mundoLampada = new Vector3[4];
        for (int i = 0; i < 4; i++) {
            Vector3 rot = tiltAngle > 0.0001 ? localLampada[i].rotateAroundAxis(eixo, tiltAngle) : localLampada[i];
            mundoLampada[i] = rot.add(base);
        }
        out.add(new Face3D(mundoLampada, corLampada));

        return out;
    }
}
