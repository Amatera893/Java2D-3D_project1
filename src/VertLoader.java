import java.io.*;
import java.util.*;

public class VertLoader {

    // 返り値：components -> each component is List<Vec2>
    public static List<List<Vec2>> load(String path) throws IOException {
        try (Scanner sc = new Scanner(new File(path))) {
            int numComponents = sc.nextInt();
            List<List<Vec2>> comps = new ArrayList<>();

            for (int c = 0; c < numComponents; c++) {
                int n = sc.nextInt();
                List<Vec2> pts = new ArrayList<>(n);
                for (int i = 0; i < n; i++) {
                    double x = sc.nextDouble();
                    double y = sc.nextDouble();
                    pts.add(new Vec2(x, y));
                }
                comps.add(pts);
            }
            return comps;
        }
    }
}
