import java.util.ArrayList;
import java.util.List;
import java.awt.Color;

public class House3D {
    public List<HouseFace> faces = new ArrayList<>();
    public Vector3 origin;
    public double largura, profundidade, alturaParede, alturaTelhado;

    public House3D(Vector3 origin, double largura, double profundidade, double alturaParede, double alturaTelhado) {
        this.origin = origin;
        this.largura = largura;
        this.profundidade = profundidade;
        this.alturaParede = alturaParede;
        this.alturaTelhado = alturaTelhado;
        gerar();
    }

    private void gerar() {
        double x0 = origin.x, x1 = origin.x + largura;
        double z0 = origin.z, z1 = origin.z + profundidade;
        double y0 = origin.y, y1 = origin.y + alturaParede;

        Color paredeCor = new Color(220, 200, 170);
        Color telhadoCor = new Color(160, 60, 50);
        Color janelaCor = new Color(150, 210, 240);
        Color portaCor = new Color(120, 80, 50);

        faces.add(new HouseFace(new Vector3[]{
                new Vector3(x0, y0, z0), new Vector3(x1, y0, z0), new Vector3(x1, y1, z0), new Vector3(x0, y1, z0)
        }, HouseFace.Tipo.PAREDE, 150, paredeCor));

        faces.add(new HouseFace(new Vector3[]{
                new Vector3(x1, y0, z1), new Vector3(x0, y0, z1), new Vector3(x0, y1, z1), new Vector3(x1, y1, z1)
        }, HouseFace.Tipo.PAREDE, 150, paredeCor));

        faces.add(new HouseFace(new Vector3[]{
                new Vector3(x0, y0, z1), new Vector3(x0, y0, z0), new Vector3(x0, y1, z0), new Vector3(x0, y1, z1)
        }, HouseFace.Tipo.PAREDE, 150, paredeCor));

        faces.add(new HouseFace(new Vector3[]{
                new Vector3(x1, y0, z0), new Vector3(x1, y0, z1), new Vector3(x1, y1, z1), new Vector3(x1, y1, z0)
        }, HouseFace.Tipo.PAREDE, 150, paredeCor));

        double xm = (x0 + x1) / 2;
        double ytop = y1 + alturaTelhado;

        faces.add(new HouseFace(new Vector3[]{
                new Vector3(x0, y1, z0), new Vector3(xm, ytop, z0), new Vector3(xm, ytop, z1), new Vector3(x0, y1, z1)
        }, HouseFace.Tipo.TELHADO, 90, telhadoCor));

        faces.add(new HouseFace(new Vector3[]{
                new Vector3(xm, ytop, z0), new Vector3(x1, y1, z0), new Vector3(x1, y1, z1), new Vector3(xm, ytop, z1)
        }, HouseFace.Tipo.TELHADO, 90, telhadoCor));

        faces.add(new HouseFace(new Vector3[]{
                new Vector3(x0, y1, z0), new Vector3(x1, y1, z0), new Vector3(xm, ytop, z0)
        }, HouseFace.Tipo.TELHADO, 60, paredeCor));

        faces.add(new HouseFace(new Vector3[]{
                new Vector3(x1, y1, z1), new Vector3(x0, y1, z1), new Vector3(xm, ytop, z1)
        }, HouseFace.Tipo.TELHADO, 60, paredeCor));

        double eps = 0.05;
        double jw = largura * 0.18, jh = alturaParede * 0.35;
        double jy0 = y0 + alturaParede * 0.35;

        double jx0 = x0 + largura * 0.15;
        faces.add(new HouseFace(new Vector3[]{
                new Vector3(jx0, jy0, z0 - eps), new Vector3(jx0 + jw, jy0, z0 - eps),
                new Vector3(jx0 + jw, jy0 + jh, z0 - eps), new Vector3(jx0, jy0 + jh, z0 - eps)
        }, HouseFace.Tipo.JANELA, 35, janelaCor));

        double jx1 = x0 + largura * 0.62;
        faces.add(new HouseFace(new Vector3[]{
                new Vector3(jx1, jy0, z0 - eps), new Vector3(jx1 + jw, jy0, z0 - eps),
                new Vector3(jx1 + jw, jy0 + jh, z0 - eps), new Vector3(jx1, jy0 + jh, z0 - eps)
        }, HouseFace.Tipo.JANELA, 35, janelaCor));

        double dw = largura * 0.14, dh = alturaParede * 0.55;
        double dx = x0 + largura * 0.4;
        faces.add(new HouseFace(new Vector3[]{
                new Vector3(dx, y0, z0 - eps), new Vector3(dx + dw, y0, z0 - eps),
                new Vector3(dx + dw, y0 + dh, z0 - eps), new Vector3(dx, y0 + dh, z0 - eps)
        }, HouseFace.Tipo.PORTA, 60, portaCor));
    }

    /** Aplica o vento do tornado sobre cada face (usa apenas o plano X/Z, igual ao modelo 2D original). */
    public void aplicarVento(Tornado tornado, double dt) {
        for (HouseFace f : faces) {
            if (f.destruida) continue;

            Vector3 c = f.centroidBase().add(f.offset);

            if (f.arrancada) {
                Vector2 wind = tornado.windAt(new Vector2(c.x, c.z));
                f.vel = f.vel.add(new Vector3(wind.x, 4, wind.y).scale(dt * 0.8)).scale(0.985);
                f.offset = f.offset.add(f.vel.scale(dt));
                continue;
            }

            double windSpeed = tornado.windSpeedAt(new Vector2(c.x, c.z));
            double limiar = 18 * f.resistencia();
            if (windSpeed > limiar) {
                double excedente = windSpeed - limiar;
                f.hp -= excedente * excedente * dt * 0.35;
            }

            if (f.hp <= 0) {
                f.hp = 0;
                if (f.tipo == HouseFace.Tipo.PAREDE) {
                    f.destruida = true;
                } else {
                    f.arrancada = true;
                }
            }
        }
    }

    public double percentualDestruido() {
        int total = faces.size();
        int afetados = 0;
        for (HouseFace f : faces) {
            if (f.destruida || f.arrancada) afetados++;
        }
        return 100.0 * afetados / total;
    }

    public List<Face3D> render() {
        List<Face3D> out = new ArrayList<>();
        for (HouseFace f : faces) {
            if (f.destruida) continue;
            out.add(f.toFace3D());
        }
        return out;
    }
}
