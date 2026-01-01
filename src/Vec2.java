public class Vec2 {
    public final double x, y;
    public Vec2(double x, double y) { this.x = x; this.y = y; }

    public Vec2 add(Vec2 o) { return new Vec2(x + o.x, y + o.y); }
    public Vec2 sub(Vec2 o) { return new Vec2(x - o.x, y - o.y); }
    public Vec2 mul(double s) { return new Vec2(x * s, y * s); }
    public double norm() { return Math.hypot(x, y); }
}
