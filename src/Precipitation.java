import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Precipitation {

    public static class RainDrop {
        public Vector3 pos;
        public RainDrop(Vector3 pos) { this.pos = pos; }
    }

    public static class HailStone {
        public Vector3 pos;
        public double raio;
        public HailStone(Vector3 pos, double raio) {
            this.pos = pos;
            this.raio = raio;
        }
    }

    public List<RainDrop> chuva = new ArrayList<>();
    public List<HailStone> granizo = new ArrayList<>();
    private final Random rng = new Random();
    private Vector3 area;
    private final double raioArea;

    public Precipitation(Vector3 area, double raioArea) {
        this.area = area;
        this.raioArea = raioArea;
    }

    public void update(Tornado tornado, double dt, TornadoPhase phase, Tornado.EFScale escala) {
        area = new Vector3(tornado.center.x, 0, tornado.center.y);

        int taxaChuva = phase == TornadoPhase.CALMARIA ? 0 : 8;
        for (int i = 0; i < taxaChuva; i++) {
            chuva.add(new RainDrop(novoPontoAleatorioBase(35)));
        }

        boolean comGranizo = escala.ordinal() >= Tornado.EFScale.EF1.ordinal() && phase != TornadoPhase.CALMARIA;
        if (comGranizo) {
            for (int i = 0; i < 2; i++) {
                granizo.add(new HailStone(novoPontoAleatorioBase(30), 0.15 + rng.nextDouble() * 0.25));
            }
        }

        for (RainDrop d : chuva) {
            Vector2 wind = tornado.windAt(new Vector2(d.pos.x, d.pos.z));
            d.pos = d.pos.add(new Vector3(wind.x * 0.3, -18, wind.y * 0.3).scale(dt));
        }
        chuva.removeIf(d -> d.pos.y < 0);
        if (chuva.size() > 500) chuva.subList(0, chuva.size() - 500).clear();

        for (HailStone h : granizo) {
            Vector2 wind = tornado.windAt(new Vector2(h.pos.x, h.pos.z));
            h.pos = h.pos.add(new Vector3(wind.x * 0.4, -9, wind.y * 0.4).scale(dt));
        }
        granizo.removeIf(h -> h.pos.y < 0);
        if (granizo.size() > 150) granizo.subList(0, granizo.size() - 150).clear();
    }

    private Vector3 novoPontoAleatorioBase(double altura) {
        double ang = rng.nextDouble() * 2 * Math.PI;
        double r = rng.nextDouble() * raioArea;
        double x = area.x + Math.cos(ang) * r;
        double z = area.z + Math.sin(ang) * r;
        return new Vector3(x, altura, z);
    }
}
