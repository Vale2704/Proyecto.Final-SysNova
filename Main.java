import javax.swing.SwingUtilities;

/** Punto de entrada. Al ejecutarse abre el gestor, sin consola. */
public final class Main {
    private Main() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Estilos.aplicar();
            VentanaPrincipal ventana = new VentanaPrincipal();
            ventana.setVisible(true);
        });
    }
}
