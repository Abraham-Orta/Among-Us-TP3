package com.amongus.project.vista; // Paquete de Interfaz de Usuario (UI)

// Importamos librerias de sonido nativas de Java.
// "javax.sound.sampled" es la caja de herramientas estandar para audio.
import javax.sound.sampled.*;// Clases para manejo de audio: Clip, AudioSystem, etc.
import java.io.BufferedInputStream; // Para leer archivos de forma eficiente (con buffer)
import java.io.InputStream; // Para leer flujos de datos genericos

/**
 * ReproductorMusica
 * Esta clase es como  el "DJ"
 * Se encarga de cargar un archivo de audio (.wav) y reproducirlo una y otra vez.
 */
public class ReproductorMusica {

    // "Clip" es el objeto de Java que controla un sonido cargado en memoria.
    // Es como tener un archivo de musica en el cell.
    private Clip clipDeAudio;// Variable que almacena el clip de audio activo

    /**
     * Carga el archivo y lo pone en loop infinito.
     * @param nombreArchivo El nombre exacto del archivo dentro de la carpeta resources.
     */
    public void reproducirEnBucle(String nombreArchivo) {
        // Primero, si ya habia música sonando, la apagamos para no mezclar canciones.
        detenerMusica(); // Llama al método para detener música previa

        try {
            //  Buscamos el archivo de audio.
            InputStream flujoAudioCrudo = cargarArchivo(nombreArchivo);// Obtiene InputStream del archivo
            
            // Si no lo encontramos, avisamos y nos vamos.
            if (flujoAudioCrudo == null) {// Verifica si se encontró el archivo
                System.err.println("DJ: No encuentro el disco: " + nombreArchivo);// Mensaje de error
                return;// Sale del método si no encuentra el archivo
            }

            // Envolvemos el flujo en un Buffer.
            // Esto es TECNICAMENTE IMPORTANTE: Java a veces falla al leer audios si no puede
            // "marcar" (mark/reset) la posición en el archivo. El BufferedInputStream permite eso.
            InputStream flujoConBuffer = new BufferedInputStream(flujoAudioCrudo);// Buffer para mejor rendimiento
            
            //  Obtenemos el flujo de audio decodificado para el sistema.
            AudioInputStream flujoAudioSistema = AudioSystem.getAudioInputStream(flujoConBuffer);// Convierte a formato de audio

            // Conseguimos un "Clip" vacío del sistema de sonido de la computadora.
            clipDeAudio = AudioSystem.getClip(); // Crea un nuevo clip de audio
            
            // Abrimos el clip y le metemos el flujo de audio
            // Aquí es donde se carga el sonido en la memoria RAM
            clipDeAudio.open(flujoAudioSistema); // Carga el audio en el clip
            
            // 6. Configuración clave: LOOP_CONTINUOUSLY hace que nunca pare.
            clipDeAudio.loop(Clip.LOOP_CONTINUOUSLY);// Configura repetición infinita

            clipDeAudio.start();// Inicia la reproducción
            
        } catch (Exception error) {
            // Si el archivo esta corrupto o no es WAV, atrapamos el error aqui.
            System.err.println("DJ: Se rompió el equipo de sonido: " + error.getMessage());// Mensaje de error
            error.printStackTrace();// Imprime traza completa del error
        }
    }

    /**
     * Es importante "cerrar" (close) el clip para no dejar basura en la memoria.
     */
    public void detenerMusica() {
        // Verificamos si existe un clip cargado.
        if (clipDeAudio != null) {// Comprueba si hay un clip activo
            // Si está sonando, lo paramos.
            if (clipDeAudio.isRunning()) { // Verifica si el clip está reproduciéndose
                clipDeAudio.stop();// Detiene la reproducción
            }
            // Cerramos el canal de audio.
            clipDeAudio.close();// Libera recursos del clip
            // Lo ponemos en null para indicar que ya no hay nada cargado.
            clipDeAudio = null;// Elimina referencia al clip
        }
    }
    
    /**
     * Método privado (solo para uso interno) para buscar el archivo.
     * @param nombre El nombre del archivo.
     * @return El flujo de datos (InputStream) o null si no lo encuentra.
     */
    private InputStream cargarArchivo(String nombre) {
        try {
            //  Buscar en el Classpath (Lo normal cuando compilamos).
            InputStream is = getClass().getClassLoader().getResourceAsStream(nombre);// Busca en recursos del JAR
            if (is != null) return is; // Retorna si lo encuentra
            
            // Buscar como archivo físico en "src/main/resources" (Típico de Maven).
            java.io.File archivoFisico = new java.io.File("src/main/resources/" + nombre); // Ruta Maven/Gradle
            if (archivoFisico.exists()) return new java.io.FileInputStream(archivoFisico);// Abre como FileInputStream
            
            //  Buscar en una carpeta "resources" en la raíz (A veces pasa).
            archivoFisico = new java.io.File("resources/" + nombre);// Ruta alternativa
            if (archivoFisico.exists()) return new java.io.FileInputStream(archivoFisico);// Abre archivo

            return null;// Retorna null si no encuentra en ninguna ruta
        } catch (Exception e) {
            return null;// Retorna null si hay error de lectura
        }
    }
}
