package com.amongus.project.data; // Estamos en la carpeta de datos del proyecto

// Importamos las herramientas de Java para manejar XML (que es como una hoja de excel pero en texto)
import org.w3c.dom.Document; // Representa todo el documento XML en memoria
import org.w3c.dom.Element;   // Representa una etiqueta individual <Etiqueta>
import org.w3c.dom.Node;      // Representa cualquier nodo (elemento o texto)
import org.w3c.dom.NodeList;  // Una lista de nodos

// Importamos herramientas para "construir" y "transformar" esos XML
import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory; 
import javax.xml.transform.OutputKeys; 
import javax.xml.transform.Transformer; 
import javax.xml.transform.TransformerFactory; 
import javax.xml.transform.dom.DOMSource; 
import javax.xml.transform.stream.StreamResult; 

// Herramientas basicas de archivos y fechas
import java.io.File; 
import java.text.SimpleDateFormat; 
import java.util.Date; 

/**
 * GestorDatos
 * ===========
 * Esta clase es el "bibliotecario" del juego.
 * Su único trabajo es guardar en una libreta (archivo XML) quién ganó cada partida.
 * No sabe jugar, solo sabe escribir y leer el historial.
 */
public class GestorDatos {

    // Definimos el nombre del archivo como una constante para no equivocarnos al escribirlo luego.
    // "final" significa que no se puede cambiar mientras corre el programa.
    private static final String NOMBRE_ARCHIVO_HISTORIAL = "historial_partidas.xml";

    /**
     * Este método es el que "escribe" en la libreta.
     * Recibe quién ganó y cuántos jugaron, y lo guarda para la posteridad.
     */
    public static void guardarPartida(String equipoGanador, int cantidadJugadores) {
        // Usamos try-catch porque trabajar con archivos es peligroso (el disco puede estar lleno, etc.)
        try {
            // 1. Buscamos el archivo físico en la carpeta del proyecto.
            File archivoFisico = new File(NOMBRE_ARCHIVO_HISTORIAL);
            
            // 2. Preparamos la fábrica de constructores de documentos XML.
            // Es como contratar a un arquitecto para que nos deje hacer planos.
            DocumentBuilderFactory fabricaConstructores = DocumentBuilderFactory.newInstance();
            DocumentBuilder constructorDocumento = fabricaConstructores.newDocumentBuilder();
            
            // Variables para manejar el documento en la memoria RAM.
            Document documentoEnMemoria;
            Element etiquetaRaiz;

            // 3. Preguntamos: ¿Ya existe el archivo o es la primera vez?
            if (archivoFisico.exists()) {
                // Si YA existe, le decimos al constructor que lo lea y lo cargue en memoria.
                documentoEnMemoria = constructorDocumento.parse(archivoFisico);
                // Recuperamos la etiqueta principal que engloba todo (<GameHistory>).
                etiquetaRaiz = documentoEnMemoria.getDocumentElement();
            } else {
                // Si NO existe, creamos una hoja en blanco (documento nuevo).
                documentoEnMemoria = constructorDocumento.newDocument();
                // Creamos la etiqueta principal "GameHistory" (HistorialJuego).
                etiquetaRaiz = documentoEnMemoria.createElement("GameHistory");
                // Pegamos esa etiqueta en el documento vacio.
                documentoEnMemoria.appendChild(etiquetaRaiz);
            }

            // 4. Ahora vamos a crear la ficha de la partida actual.
            // Creamos una etiqueta <Partida>.
            Element nuevaPartida = documentoEnMemoria.createElement("Partida");
            // La metemos dentro de la etiqueta raiz (como meter una hoja en una carpeta).
            etiquetaRaiz.appendChild(nuevaPartida);

            // 5. Agregamos el dato de la FECHA.
            Element etiquetaFecha = documentoEnMemoria.createElement("Fecha");
            // Obtenemos la fecha y hora exacta de este instante.
            Date fechaReloj = new Date();
            // Le damos formato bonito: "Año-Mes-Dia Hora:Minuto:Segundo".
            SimpleDateFormat formatoFecha = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            // Creamos el texto y lo metemos dentro de la etiqueta <Fecha>.
            etiquetaFecha.appendChild(documentoEnMemoria.createTextNode(formatoFecha.format(fechaReloj)));
            // Metemos la etiqueta fecha dentro de la partida.
            nuevaPartida.appendChild(etiquetaFecha);
            
            // 6. Agregamos el dato del GANADOR.
            Element etiquetaGanador = documentoEnMemoria.createElement("Ganador");
            // Metemos el texto (ej: "Tripulantes") dentro de la etiqueta.
            etiquetaGanador.appendChild(documentoEnMemoria.createTextNode(equipoGanador));
            // Lo pegamos a la partida.
            nuevaPartida.appendChild(etiquetaGanador);

            // 7. Agregamos el dato de JUGADORES.
            Element etiquetaJugadores = documentoEnMemoria.createElement("Jugadores");
            // Convertimos el numero entero a texto (String) para poder escribirlo.
            etiquetaJugadores.appendChild(documentoEnMemoria.createTextNode(String.valueOf(cantidadJugadores)));
            // Lo pegamos a la partida.
            nuevaPartida.appendChild(etiquetaJugadores);

            // 8. MOMENTO DE GUARDAR: Volcar la memoria al disco duro.
            // Necesitamos un "Transformer" (Transformador) para convertir los objetos de Java a texto XML.
            TransformerFactory fabricaTransformadores = TransformerFactory.newInstance();
            Transformer transformador = fabricaTransformadores.newTransformer();
            
            // Le decimos que queremos que el XML quede bonito (indentado), con espacios.
            // Si no hacemos esto, queda todo en una sola linea y es ilegible.
            transformador.setOutputProperty(OutputKeys.INDENT, "yes");
            transformador.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            
            // Preparamos la fuente (lo que tenemos en memoria).
            DOMSource fuenteDatos = new DOMSource(documentoEnMemoria);
            // Preparamos el destino (el archivo en el disco).
            StreamResult resultadoArchivo = new StreamResult(archivoFisico);
            
            // ¡PUM! Escribimos los cambios.
            transformador.transform(fuenteDatos, resultadoArchivo);

            // Aviso para nosotros los programadores en la consola.
            System.out.println("Se guardó la partida correctamente en: " + archivoFisico.getAbsolutePath());

        } catch (Exception error) {
            // Si algo explota, que nos diga qué pasó.
            System.err.println("¡Ups! Error guardando el XML: " + error.getMessage());
            error.printStackTrace();
        }
    }
}
