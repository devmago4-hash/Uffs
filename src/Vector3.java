public class Vector3 {
    public double x, y, z;

    public Vector3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3 add(Vector3 o) {
        return new Vector3(x + o.x, y + o.y, z + o.z);
    }

    public Vector3 sub(Vector3 o) {
        return new Vector3(x - o.x, y - o.y, z - o.z);
    }

    public Vector3 scale(double s) {
        return new Vector3(x * s, y * s, z * s);
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public Vector3 normalized() {
        double l = length();
        if (l < 1e-9) return new Vector3(0, 0, 0);
        return new Vector3(x / l, y / l, z / l);
    }

    /** Rotaciona o vetor em torno do eixo Y (usado para girar destroços/carro no ar). */
    public Vector3 rotateY(double ang) {
        double c = Math.cos(ang), s = Math.sin(ang);
        return new Vector3(x * c + z * s, y, -x * s + z * c);
    }

    public Vector3 withY(double ny) {
        return new Vector3(x, ny, z);
    }

    public Vector3 cross(Vector3 o) {
        return new Vector3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x);
    }

    public double dot(Vector3 o) {
        return x * o.x + y * o.y + z * o.z;
    }

    /**
     * Rotaciona este vetor em torno de um eixo arbitrario (formula de rotacao de Rodrigues).
     * Usado para tombar o poste em torno de um eixo horizontal na base, por exemplo.
     */
    public Vector3 rotateAroundAxis(Vector3 axis, double angle) {
        Vector3 k = axis.normalized();
        double cos = Math.cos(angle), sin = Math.sin(angle);
        Vector3 termo1 = this.scale(cos);
        Vector3 termo2 = k.cross(this).scale(sin);
        Vector3 termo3 = k.scale(k.dot(this) * (1 - cos));
        return termo1.add(termo2).add(termo3);
    }
}
