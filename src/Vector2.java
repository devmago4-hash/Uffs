public class Vector2 {
    public double x, y;

    public Vector2(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vector2 add(Vector2 o) {
        return new Vector2(x + o.x, y + o.y);
    }

    public Vector2 sub(Vector2 o) {
        return new Vector2(x - o.x, y - o.y);
    }

    public Vector2 scale(double s) {
        return new Vector2(x * s, y * s);
    }

    public double length() {
        return Math.sqrt(x * x + y * y);
    }

    public Vector2 normalized() {
        double len = length();
        if (len < 1e-9) return new Vector2(0, 0);
        return new Vector2(x / len, y / len);
    }

    // Vetor perpendicular (rotacionado 90 graus) - usado para gerar componente tangencial do vortice
    public Vector2 perpendicular() {
        return new Vector2(-y, x);
    }
}
