import java.awt.Color;

public class HouseFace {
    public enum Tipo { PAREDE, TELHADO, JANELA, PORTA }

    public Vector3[] baseVertices;
    public Vector3 offset = new Vector3(0, 0, 0);
    public Vector3 vel = new Vector3(0, 0, 0);
    public Tipo tipo;
    public double hp, hpMax;
    public boolean destruida = false;
    public boolean arrancada = false;
    public Color baseColor;

    public HouseFace(Vector3[] baseVertices, Tipo tipo, double hpMax, Color baseColor) {
        this.baseVertices = baseVertices;
        this.tipo = tipo;
        this.hpMax = hpMax;
        this.hp = hpMax;
        this.baseColor = baseColor;
    }

    public double resistencia() {
        switch (tipo) {
            case JANELA: return 0.5;
            case PORTA: return 0.8;
            case TELHADO: return 1.0;
            case PAREDE: return 1.4;
            default: return 1.0;
        }
    }

    public Vector3 centroidBase() {
        double x = 0, y = 0, z = 0;
        for (Vector3 v : baseVertices) {
            x += v.x;
            y += v.y;
            z += v.z;
        }
        int n = baseVertices.length;
        return new Vector3(x / n, y / n, z / n);
    }

    public Face3D toFace3D() {
        Vector3[] verts = new Vector3[baseVertices.length];
        for (int i = 0; i < verts.length; i++) {
            verts[i] = baseVertices[i].add(offset);
        }
        return new Face3D(verts, corAtual());
    }

    private Color corAtual() {
        double frac = Math.max(0, hp / hpMax);
        int r = (int) (90 * (1 - frac) + baseColor.getRed() * frac);
        int g = (int) (70 * (1 - frac) + baseColor.getGreen() * frac);
        int b = (int) (60 * (1 - frac) + baseColor.getBlue() * frac);
        return new Color(clamp(r), clamp(g), clamp(b));
    }

    private int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
