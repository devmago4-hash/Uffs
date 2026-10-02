import java.util.ArrayList;
import java.util.List;
import java.awt.Color;

public class CloudSystem {
    private final List<Vector3> centros = new ArrayList<>();
    private double time = 0;
    private static final int N = 7;

    public CloudSystem(Vector3 baseCenter) {
        for (int i = 0; i < N; i++) {
            double ang = i * (2 * Math.PI / N);
            double r = 15 + Math.random() * 10;
            centros.add(new Vector3(baseCenter.x + Math.cos(ang) * r, baseCenter.y, baseCenter.z + Math.sin(ang) * r));
        }
    }

    public void update(double dt, Vector3 targetCenter) {
        time += dt;
        for (int i = 0; i < centros.size(); i++) {
            Vector3 c = centros.get(i);
            Vector3 toTarget = new Vector3(targetCenter.x - c.x, 0, targetCenter.z - c.z);
            centros.set(i, c.add(toTarget.scale(dt * 0.05)));
        }
    }

    public List<Face3D> render() {
        List<Face3D> out = new ArrayList<>();
        Color cor = new Color(70, 70, 80, 140);
        for (int i = 0; i < centros.size(); i++) {
            Vector3 c = centros.get(i);
            double s = 10 + 4 * Math.sin(time * 0.5 + i);
            Vector3[] verts = new Vector3[]{
                    c.add(new Vector3(-s, 0, -s)), c.add(new Vector3(s, 0, -s)),
                    c.add(new Vector3(s, 0, s)), c.add(new Vector3(-s, 0, s))
            };
            out.add(new Face3D(verts, cor));
        }
        return out;
    }
}
