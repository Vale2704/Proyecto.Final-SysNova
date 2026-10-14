

/**
 * Ejecuta operaciones que pueden fallar y las convierte en un registro
 * controlado, en lugar de dejar caer el sistema operativo simulado.
 */
public final class ManejadorExcepciones {

    public Resultado dividir(Proceso proceso, int dividendo, int divisor, int tiempo, boolean recuperar) {
        try {
            int valor = dividendo / divisor;
            return Resultado.exito("Resultado de " + dividendo + " / " + divisor + " = " + valor + ".", valor);
        } catch (ArithmeticException ex) {
            String manejo = recuperar
                    ? "División por cero capturada. Se omite la instrucción y el proceso continúa."
                    : "División por cero capturada. El proceso se termina y se liberan su memoria y sus recursos.";
            RegistroExcepcion registro = new RegistroExcepcion(
                    tiempo, proceso.getPid(), proceso.getNombre(),
                    "ArithmeticException (división por cero)",
                    ex.toString(), manejo);
            return Resultado.fallo(manejo, registro, !recuperar);
        }
    }

    public Resultado accederMemoria(GestorMemoria memoria, Proceso proceso, int pagina, int tiempo, boolean recuperar) {
        try {
            int fisica = memoria.acceder(proceso, pagina);
            return Resultado.exito(
                    "Acceso válido: página lógica " + pagina + " → página física " + fisica + ".",
                    fisica);
        } catch (AccesoMemoriaInvalidoException ex) {
            String manejo = recuperar
                    ? "Acceso inválido capturado. Se omite la instrucción y el proceso continúa."
                    : "Acceso inválido capturado. El proceso se termina y se liberan su memoria y sus recursos.";
            RegistroExcepcion registro = new RegistroExcepcion(
                    tiempo, proceso.getPid(), proceso.getNombre(),
                    "Acceso a memoria no válida",
                    ex.getMessage(), manejo);
            return Resultado.fallo(manejo, registro, !recuperar);
        }
    }

    public static final class Resultado {
        public final boolean exito;
        public final boolean terminarProceso;
        public final String detalle;
        public final RegistroExcepcion registro;
        public final int valor;

        private Resultado(boolean exito, boolean terminarProceso, String detalle,
                          RegistroExcepcion registro, int valor) {
            this.exito = exito;
            this.terminarProceso = terminarProceso;
            this.detalle = detalle;
            this.registro = registro;
            this.valor = valor;
        }

        static Resultado exito(String detalle, int valor) {
            return new Resultado(true, false, detalle, null, valor);
        }

        static Resultado fallo(String detalle, RegistroExcepcion registro, boolean terminar) {
            return new Resultado(false, terminar, detalle, registro, 0);
        }
    }
}
