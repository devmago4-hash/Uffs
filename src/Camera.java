/**
 * Camera livre em 3D. yaw = rotacao horizontal (em torno de Y), pitch = olhar para
 * cima/baixo (em torno de X local). Convencao: yaw = 0 olha em direcao a +Z.
 */
public class Camera {
    public Vector3 pos;
    public double yaw;
    public double pitch;

    private static final double LIMITE_PITCH = Math.toRadians(85);

    public Camera(Vector3 pos, double yaw, double pitch) {
        this.pos = pos;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public void moveForward(double d) {
        Vector3 dir = new Vector3(Math.sin(yaw), 0, Math.cos(yaw));
        pos = pos.add(dir.scale(d));
    }

    public void moveRight(double d) {
        Vector3 dir = new Vector3(Math.cos(yaw), 0, -Math.sin(yaw));
        pos = pos.add(dir.scale(d));
    }

    public void moveUp(double d) {
        pos = pos.add(new Vector3(0, d, 0));
    }

    public void rotate(double dyaw, double dpitch) {
        yaw += dyaw;
        pitch += dpitch;
        if (pitch > LIMITE_PITCH) pitch = LIMITE_PITCH;
        if (pitch < -LIMITE_PITCH) pitch = -LIMITE_PITCH;
    }

    /** Transforma um ponto do mundo para o espaco da camera (x=direita, y=cima, z=profundidade/frente). */
    public Vector3 toCameraSpace(Vector3 p) {
        Vector3 t = p.sub(pos);

        double cosY = Math.cos(-yaw), sinY = Math.sin(-yaw);
        double x1 = t.x * cosY + t.z * sinY;
        double z1 = -t.x * sinY + t.z * cosY;
        double y1 = t.y;

        double cosP = Math.cos(-pitch), sinP = Math.sin(-pitch);
        double y2 = y1 * cosP - z1 * sinP;
        double z2 = y1 * sinP + z1 * cosP;

        return new Vector3(x1, y2, z2);
    }
}
