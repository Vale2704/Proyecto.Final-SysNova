
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Lectura de cargas de procesos en CSV y en un JSON sencillo, sin librerías externas. */
public final class CargadorProcesos {
    private CargadorProcesos() {}

    public static List<DatosProceso> leer(Path ruta) throws IOException {
        String texto = Files.readString(ruta, StandardCharsets.UTF_8);
        String nombre = ruta.getFileName().toString().toLowerCase(Locale.ROOT);
        if (nombre.endsWith(".json")) {
            return desdeJson(texto);
        }
        if (nombre.endsWith(".csv")) {
            return desdeCsv(texto);
        }
        throw new IllegalArgumentException("El archivo debe ser .csv o .json.");
    }

    public static List<DatosProceso> desdeCsv(String texto) {
        texto = quitarBom(texto).replace("\r\n", "\n").replace('\r', '\n');
        String[] lineas = texto.split("\n");
        List<String> utiles = new ArrayList<>();
        for (String linea : lineas) {
            if (!linea.isBlank() && !linea.strip().startsWith("#")) {
                utiles.add(linea.strip());
            }
        }
        if (utiles.size() < 2) {
            throw new IllegalArgumentException("El CSV necesita un encabezado y al menos un proceso.");
        }
        String[] encabezado = utiles.get(0).split(",");
        Map<String, Integer> columnas = new LinkedHashMap<>();
        for (int i = 0; i < encabezado.length; i++) {
            columnas.put(normalizar(encabezado[i]), i);
        }
        exigir(columnas, "nombre");
        exigir(columnas, "prioridad");
        exigir(columnas, "llegada");
        exigir(columnas, "rafaga");
        exigir(columnas, "paginas");
        List<DatosProceso> lista = new ArrayList<>();
        for (int fila = 1; fila < utiles.size(); fila++) {
            String[] celdas = utiles.get(fila).split(",", -1);
            try {
                int[] recursos = new int[]{
                        entero(celdas, columnas, "r1", 0),
                        entero(celdas, columnas, "r2", 0),
                        entero(celdas, columnas, "r3", 0)
                };
                lista.add(new DatosProceso(
                        texto(celdas, columnas, "nombre"),
                        entero(celdas, columnas, "prioridad", -1),
                        entero(celdas, columnas, "llegada", -1),
                        entero(celdas, columnas, "rafaga", -1),
                        entero(celdas, columnas, "paginas", -1),
                        entero(celdas, columnas, "es", 0),
                        recursos));
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException("CSV, línea " + (fila + 1) + ": " + ex.getMessage(), ex);
            }
        }
        return lista;
    }

    public static List<DatosProceso> desdeJson(String texto) {
        texto = quitarBom(texto);
        List<DatosProceso> lista = new ArrayList<>();
        int i = 0;
        int nObjeto = 0;
        while (i < texto.length()) {
            int abre = texto.indexOf('{', i);
            if (abre < 0) {
                break;
            }
            int cierra = indiceCierre(texto, abre);
            String cuerpo = texto.substring(abre + 1, cierra);
            if (cuerpo.indexOf('{') >= 0) {
                i = abre + 1;
                continue;
            }
            nObjeto++;
            try {
                lista.add(objeto(cuerpo));
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException("JSON, objeto " + nObjeto + ": " + ex.getMessage(), ex);
            }
            i = cierra + 1;
        }
        if (lista.isEmpty()) {
            throw new IllegalArgumentException("El JSON no contiene procesos.");
        }
        return lista;
    }

    private static DatosProceso objeto(String cuerpo) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (String parte : partirComas(cuerpo)) {
            int sep = parte.indexOf(':');
            if (sep < 0) {
                continue;
            }
            String clave = normalizar(parte.substring(0, sep).replace("\"", ""));
            String valor = parte.substring(sep + 1).trim();
            campos.put(clave, valor);
        }
        String nombre = cadena(campos, "nombre");
        int[] recursos = recursos(campos.get("recursos"));
        return new DatosProceso(
                nombre,
                enteroCampo(campos, "prioridad"),
                enteroCampo(campos, "llegada"),
                enteroCampo(campos, "rafaga"),
                enteroCampo(campos, "paginas"),
                campos.containsKey("es") ? enteroSuelto(campos.get("es"), "es") : 0,
                recursos);
    }

    private static int[] recursos(String texto) {
        int[] valores = new int[]{0, 0, 0};
        if (texto == null || texto.isBlank()) {
            return valores;
        }
        String limpio = texto.replace("[", "").replace("]", "").trim();
        if (limpio.isEmpty()) {
            return valores;
        }
        String[] partes = limpio.split(",");
        if (partes.length != 3) {
            throw new IllegalArgumentException("«recursos» debe tener tres enteros [impresora, disco, puerto].");
        }
        for (int i = 0; i < 3; i++) {
            valores[i] = enteroSuelto(partes[i], "recursos");
        }
        return valores;
    }

    private static List<String> partirComas(String cuerpo) {
        List<String> partes = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        int corchetes = 0;
        boolean comillas = false;
        for (int i = 0; i < cuerpo.length(); i++) {
            char c = cuerpo.charAt(i);
            if (c == '"' && (i == 0 || cuerpo.charAt(i - 1) != '\\')) {
                comillas = !comillas;
            }
            if (!comillas && c == '[') corchetes++;
            if (!comillas && c == ']') corchetes--;
            if (!comillas && corchetes == 0 && c == ',') {
                partes.add(actual.toString());
                actual.setLength(0);
            } else {
                actual.append(c);
            }
        }
        if (!actual.toString().isBlank()) {
            partes.add(actual.toString());
        }
        return partes;
    }

    private static int indiceCierre(String texto, int abre) {
        int profundidad = 0;
        for (int i = abre; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c == '{') profundidad++;
            else if (c == '}') {
                profundidad--;
                if (profundidad == 0) return i;
            }
        }
        throw new IllegalArgumentException("JSON incompleto: falta '}'.");
    }

    private static void exigir(Map<String, Integer> columnas, String nombre) {
        if (!columnas.containsKey(nombre)) {
            throw new IllegalArgumentException("Falta la columna «" + nombre + "» en el CSV.");
        }
    }

    private static String texto(String[] celdas, Map<String, Integer> columnas, String clave) {
        int indice = columnas.get(clave);
        if (indice >= celdas.length) {
            throw new IllegalArgumentException("Falta el valor de «" + clave + "».");
        }
        String valor = celdas[indice].trim();
        if (valor.isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío.");
        }
        return valor;
    }

    private static int entero(String[] celdas, Map<String, Integer> columnas, String clave, int omision) {
        if (!columnas.containsKey(clave)) {
            return omision;
        }
        int indice = columnas.get(clave);
        if (indice >= celdas.length || celdas[indice].isBlank()) {
            return omision;
        }
        return enteroSuelto(celdas[indice], clave);
    }

    private static String cadena(Map<String, String> campos, String clave) {
        String valor = campos.get(clave);
        if (valor == null) {
            throw new IllegalArgumentException("Falta el campo «" + clave + "».");
        }
        valor = valor.trim();
        if (valor.startsWith("\"") && valor.endsWith("\"") && valor.length() >= 2) {
            valor = valor.substring(1, valor.length() - 1);
        }
        if (valor.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío.");
        }
        return valor;
    }

    private static int enteroCampo(Map<String, String> campos, String clave) {
        if (!campos.containsKey(clave)) {
            throw new IllegalArgumentException("Falta el campo «" + clave + "».");
        }
        return enteroSuelto(campos.get(clave), clave);
    }

    private static int enteroSuelto(String texto, String campo) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("«" + campo + "» debe ser un entero.");
        }
    }

    private static String normalizar(String texto) {
        return texto.trim().toLowerCase(Locale.ROOT)
                .replace("á", "a").replace("é", "e").replace("í", "i")
                .replace("ó", "o").replace("ú", "u").replace(" ", "");
    }

    private static String quitarBom(String texto) {
        if (texto.startsWith("\uFEFF")) {
            return texto.substring(1);
        }
        return texto;
    }
}
