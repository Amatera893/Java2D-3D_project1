import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

public class CurvePanel extends JPanel {
    // 曲率流で更新するのでコピーして保持
    private final List<List<Vec2>> components;

    // 表示切替
    private boolean showTangents = true;
    private boolean showNormals = true;
    private boolean showCurvature = true;
    private boolean showPoints = true; // ★追加：点表示

    // 曲率流
    private Timer timer;
    private boolean running = false;
    private double dt = 0.0005;       // 安定寄り
    private double flowScale = 1.0;

    public CurvePanel(List<List<Vec2>> inputComponents) {
        // 深いコピー
        this.components = new ArrayList<>();
        for (var comp : inputComponents) {
            var copy = new ArrayList<Vec2>(comp.size());
            for (var p : comp) copy.add(new Vec2(p.x, p.y));
            this.components.add(copy);
        }

        setBackground(Color.WHITE);
        setFocusable(true);

        // キー操作
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();

                if (code == KeyEvent.VK_SPACE) {
                    toggleFlow();
                } else if (code == KeyEvent.VK_T) {
                    showTangents = !showTangents;
                    repaint();
                } else if (code == KeyEvent.VK_N) {
                    showNormals = !showNormals;
                    repaint();
                } else if (code == KeyEvent.VK_K) {
                    showCurvature = !showCurvature;
                    repaint();
                } else if (code == KeyEvent.VK_P) { // ★追加：点表示切替
                    showPoints = !showPoints;
                    repaint();
                } else if (code == KeyEvent.VK_A) {
                    boolean anyOn = showTangents || showNormals || showCurvature || showPoints;
                    showTangents = !anyOn;
                    showNormals  = !anyOn;
                    showCurvature= !anyOn;
                    showPoints   = !anyOn;
                    repaint();
                }
            }
        });

        timer = new Timer(33, e -> { // 約30fps
            stepCurvatureFlow();
            repaint();
        });
    }

    @Override
    public void addNotify() {
        super.addNotify();
        requestFocusInWindow(); // ウィンドウ表示後にフォーカス
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // --- バウンディングボックス ---
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        for (var comp : components) {
            for (var p : comp) {
                minX = Math.min(minX, p.x); maxX = Math.max(maxX, p.x);
                minY = Math.min(minY, p.y); maxY = Math.max(maxY, p.y);
            }
        }

        double w = Math.max(1e-9, maxX - minX);
        double h = Math.max(1e-9, maxY - minY);

        // データ座標系の余白
        double margin = 0.05 * Math.max(w, h);
        minX -= margin; maxX += margin;
        minY -= margin; maxY += margin;
        w = Math.max(1e-9, maxX - minX);
        h = Math.max(1e-9, maxY - minY);

        int padPx = 40;
        double sx = (getWidth()  - 2.0 * padPx) / w;
        double sy = (getHeight() - 2.0 * padPx) / h;
        double s = Math.min(sx, sy);

        // --- 座標変換（y反転） ---
        AffineTransform at = new AffineTransform();
        at.translate(padPx, getHeight() - padPx);
        at.scale(s, -s);
        at.translate(-minX, -minY);
        g2.transform(at);

        // ピクセル基準（スケール補正）
        float lineWidthData = (float) (2.0 / s); // 2px相当
        double pointRadiusData = 4.0 / s;        // 半径4px相当
        double arrowLenData = 20.0 / s;          // 20px相当
        g2.setStroke(new BasicStroke(lineWidthData));

        // 曲率矢印のスケール（見た目用）
        double kArrowScale = 30.0 / s;

        // --- 描画 ---
        for (var comp : components) {
            int n = comp.size();

            // 曲線（エッジ）
            g2.setColor(Color.BLACK);
            for (int i = 0; i < n; i++) {
                Vec2 a = comp.get(i);
                Vec2 b = comp.get((i + 1) % n);
                g2.draw(new Line2D.Double(a.x, a.y, b.x, b.y));
            }

            // 点（切替可能）
            if (showPoints) {
                for (var p : comp) {
                    double r = pointRadiusData;
                    g2.fill(new Ellipse2D.Double(p.x - r, p.y - r, 2*r, 2*r));
                }
            }

            // 微分量
            List<Vec2> T = DifferentialGeometry.unitTangents(comp);
            List<Vec2> N = DifferentialGeometry.unitNormalsFromTangents(T);
            double[] kappa = DifferentialGeometry.curvature(comp);

            // 接線（赤）
            if (showTangents) {
                g2.setColor(Color.RED);
                for (int i = 0; i < n; i++) {
                    Vec2 p = comp.get(i);
                    Vec2 t = T.get(i).mul(arrowLenData);
                    drawArrow(g2, p, p.add(t), (float) (1.5 / s));
                }
            }

            // 法線（青）
            if (showNormals) {
                g2.setColor(Color.BLUE);
                for (int i = 0; i < n; i++) {
                    Vec2 p = comp.get(i);
                    Vec2 nn = N.get(i).mul(arrowLenData);
                    drawArrow(g2, p, p.add(nn), (float) (1.5 / s));
                }
            }

            // 曲率（緑）：表示は法線方向に κ 比例（見た目用）
            if (showCurvature) {
                g2.setColor(new Color(0, 140, 0));
                for (int i = 0; i < n; i++) {
                    Vec2 p = comp.get(i);
                    Vec2 nn = N.get(i);
                    double kk = kappa[i];
                    Vec2 v = nn.mul(kArrowScale * kk);
                    drawArrow(g2, p, p.add(v), (float) (1.5 / s));
                }
            }
        }
    }

    // 矢印描画（データ座標系）
    private void drawArrow(Graphics2D g2, Vec2 from, Vec2 to, float headWidthData) {
        g2.draw(new Line2D.Double(from.x, from.y, to.x, to.y));

        Vec2 d = to.sub(from);
        double len = d.norm();
        if (len < 1e-12) return;

        Vec2 u = d.mul(1.0 / len);
        Vec2 left = new Vec2(-u.y, u.x);
        double headLen = 6.0 * headWidthData;

        Vec2 p1 = to.sub(u.mul(headLen)).add(left.mul(3.0 * headWidthData));
        Vec2 p2 = to.sub(u.mul(headLen)).sub(left.mul(3.0 * headWidthData));

        Path2D.Double tri = new Path2D.Double();
        tri.moveTo(to.x, to.y);
        tri.lineTo(p1.x, p1.y);
        tri.lineTo(p2.x, p2.y);
        tri.closePath();
        g2.fill(tri);
    }

    // Spaceで開始/停止
    private void toggleFlow() {
        running = !running;
        if (running) timer.start();
        else timer.stop();
    }

    // ★曲率流（課題どおり κ と n を使う）＋ h^2 スケール補正で「1点だけ動く」見え方を改善
    private void stepCurvatureFlow() {
        int subSteps = 5;          // なめらかさ＆安定性
        double subDt = dt / subSteps;

        for (int step = 0; step < subSteps; step++) {
            for (int c = 0; c < components.size(); c++) {
                List<Vec2> comp = components.get(c);
                int n = comp.size();

                List<Vec2> T = DifferentialGeometry.unitTangents(comp);
                List<Vec2> N = DifferentialGeometry.unitNormalsFromTangents(T);
                double[] kappa = DifferentialGeometry.curvature(comp);

                // ★スケール補正：平均エッジ長 h による正規化（key.vert 対策）
                double h = DifferentialGeometry.meanEdgeLength(comp);
                double scale = h * h;

                Vec2[] newPts = new Vec2[n];
                for (int i = 0; i < n; i++) {
                    Vec2 p = comp.get(i);
                    Vec2 nn = N.get(i);
                    double kk = kappa[i];

                    Vec2 delta = nn.mul(flowScale * subDt * scale * kk);

                    // 暴れ対策（必要なら調整）
                    double maxMove = 0.2;
                    double dlen = delta.norm();
                    if (dlen > maxMove) delta = delta.mul(maxMove / dlen);

                    newPts[i] = p.add(delta);
                }

                for (int i = 0; i < n; i++) comp.set(i, newPts[i]);
            }
        }
    }
}
