
import java.awt.Color;

public final class Colores {
    private static final Color[] PALETA = {
            new Color(37, 99, 168),
            new Color(15, 118, 110),
            new Color(180, 83, 9),
            new Color(153, 27, 27),
            new Color(91, 33, 182),
            new Color(3, 105, 161),
            new Color(21, 128, 61),
            new Color(161, 98, 7),
            new Color(157, 23, 77),
            new Color(30, 64, 175),
            new Color(17, 94, 89),
            new Color(124, 45, 18)
    };
    public static final Color OCIOSO = new Color(203, 213, 225);
    public static final Color LIBRE = new Color(241, 245, 249);

    private Colores() {}

    public static Color dePid(int pid) {
        if (pid <= 0) return OCIOSO;
        return PALETA[(pid - 1) % PALETA.length];
    }
}
