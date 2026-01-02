import java.util.ArrayList;
import java.util.List;

public class DifferentialGeometry {

    private static int idx(int i, int n) {
        int r = i % n;
        return (r < 0) ? r + n : r;
    }

    // t_i = normalize(p_{i+1} - p_{i-1})
    public static List<Vec2> unitTangents(List<Vec2> comp) {
        int n = comp.size();
        List<Vec2> T = new ArrayList<>(n);

        for (int i = 0; i < n; i++) {
            Vec2 pPrev = comp.get(idx(i - 1, n));
            Vec2 pNext = comp.get(idx(i + 1, n));
            Vec2 d = pNext.sub(pPrev);
            double len = d.norm();
            if (len < 1e-12) T.add(new Vec2(1, 0));
            else T.add(d.mul(1.0 / len));
        }
        return T;
    }

    // n_i = (-t_y, t_x)
    public static List<Vec2> unitNormalsFromTangents(List<Vec2> tangents) {
        int n = tangents.size();
        List<Vec2> N = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            Vec2 t = tangents.get(i);
            N.add(new Vec2(-t.y, t.x));
        }
        return N;
    }

    // Curvature : |p_{i+1} - 2p_i + p_{i-1}| / |p_{i+1}-p_i|^2
    public static double[] curvature(List<Vec2> comp) {
        int n = comp.size();
        double[] k = new double[n];

        for (int i = 0; i < n; i++) {
            Vec2 pPrev = comp.get(idx(i - 1, n));
            Vec2 p = comp.get(i);
            Vec2 pNext = comp.get(idx(i + 1, n));

            Vec2 second = pNext.sub(p.mul(2.0)).add(pPrev);
            double denom = pNext.sub(p).norm();
            denom = denom * denom;

            if (denom < 1e-12) k[i] = 0.0;
            else k[i] = second.norm() / denom;
        }
        return k;
    }

    public static Vec2[] curvatureVectorLaplacian(List<Vec2> comp) {
    int n = comp.size();
    Vec2[] cv = new Vec2[n];

    // average edge length h 
    double sum = 0.0;
    for (int i = 0; i < n; i++) {
        Vec2 a = comp.get(i);
        Vec2 b = comp.get((i + 1) % n);
        sum += b.sub(a).norm();
    }
    double h = Math.max(1e-9, sum / n);
    double invH2 = 1.0 / (h * h);

    for (int i = 0; i < n; i++) {
        Vec2 pPrev = comp.get(idx(i - 1, n));
        Vec2 p = comp.get(i);
        Vec2 pNext = comp.get(idx(i + 1, n));

        // Δp ≈ p_{i+1} - 2p_i + p_{i-1}
        Vec2 lap = pNext.sub(p.mul(2.0)).add(pPrev);
        cv[i] = lap.mul(invH2);
        }
    return cv;
    }

    public static double meanEdgeLength(List<Vec2> comp) {
    int n = comp.size();
    double sum = 0.0;
    for (int i = 0; i < n; i++) {
        Vec2 a = comp.get(i);
        Vec2 b = comp.get((i + 1) % n);
        sum += b.sub(a).norm();
    }
    return Math.max(1e-9, sum / n);
    }


}
