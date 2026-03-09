package com.amongus.project.vista; // paquete de interfaz de usuario (ui)

// importamos librerias de sonido nativas de java.
// "javax.sound.sampled" es la caja de herramientas estandar para audio.
import javax.sound.sampled.*;// clases para manejo de audio: clip, audiosystem, etc.
import java.io.BufferedInputStream; // para leer archivos de forma eficiente (con buffer)
import java.io.InputStream; // para leer flujos de datos genericos
import java.io.File; // para acceder a archivos

/**
 * ReproductorMusica
 * esta clase es como el "dj"
 * se encarga de cargar un archivo de audio (.wav) y reproducirlo una y otra vez.
 */
public class ReproductorMusica {

    // "clip" es el objeto de java que controla un sonido cargado en memoria.
    private Clip clipDeAudio;// variable que almacena el clip de audio activo
    
    // mapa de clips para manejar múltiples alarmas o loops simultáneos
    private static java.util.Map<String, Clip> clipsBucle = new java.util.HashMap<>();

    /**
     * carga el archivo y lo pone en loop infinito.
     * @param nombreArchivo el nombre exacto del archivo dentro de la carpeta resources.
     */
    public void reproducirEnBucle(String nombreArchivo) {
        // primero, si ya habia música sonando, la apagamos para no mezclar canciones.
        detenerMusica(); // llama al método para detener música previa

        try {
            // buscamos el archivo de audio.
            InputStream flujoAudioCrudo = cargarArchivo(nombreArchivo);// obtiene inputstream del archivo
            
            // si no lo encontramos, avisamos y nos vamos.
            if (flujoAudioCrudo == null) {// verifica si se encontró el archivo
                System.err.println("dj: no encuentro el disco: " + nombreArchivo);// mensaje de error
                return;// sale del método si no encuentra el archivo
            }

            // envolvemos el flujo en un buffer.
            InputStream flujoConBuffer = new BufferedInputStream(flujoAudioCrudo);// buffer para mejor rendimiento
            
            // obtenemos el flujo de audio decodificado para el sistema.
            AudioInputStream flujoAudioSistema = AudioSystem.getAudioInputStream(flujoConBuffer);// convierte a formato de audio

            // conseguimos un "clip" vacío del sistema de sonido de la computadora.
            clipDeAudio = AudioSystem.getClip(); // crea un nuevo clip de audio
            
            // abrimos el clip y le metemos el flujo de audio
            clipDeAudio.open(flujoAudioSistema); // carga el audio en el clip
            
            // configuración clave: loop_continuously hace que nunca pare.
            clipDeAudio.loop(Clip.LOOP_CONTINUOUSLY);// configura repetición infinita

            clipDeAudio.start();// inicia la reproducción
            
        } catch (Exception error) {
            // si el archivo esta corrupto o no es wav, atrapamos el error aqui.
            System.err.println("dj: se rompió el equipo de sonido: " + error.getMessage());// mensaje de error
            error.printStackTrace();// imprime traza completa del error
        }
    }

    /**
     * es importante "cerrar" (close) el clip para no dejar basura en la memoria.
     */
    public void detenerMusica() {
        // verificamos si existe un clip cargado.
        if (clipDeAudio != null) {// comprueba si hay un clip activo
            // si está sonando, lo paramos.
            if (clipDeAudio.isRunning()) { // verifica si el clip está reproduciéndose
                clipDeAudio.stop();// detiene la reproducción
            }
            // cerramos el canal de audio.
            clipDeAudio.close();// libera recursos del clip
            // lo ponemos en null para indicar que ya no hay nada cargado.
            clipDeAudio = null;// elimina referencia al clip
        }
    }

    // mapa para evitar que el mismo efecto de sonido se reproduzca varias veces al mismo tiempo (ej. 3 clientes locales)
    private static java.util.Map<String, Long> ultimoTiempoEfecto = new java.util.HashMap<>();

    /**
     * reproduce un efecto de sonido una sola vez (como clics de ui o alertas).
     * crea un hilo nuevo para no bloquear el juego.
     */
    public static void reproducirEfecto(String nombreArchivo) {
        reproducirEfectoConVolumen(nombreArchivo, 0.0f); // Volumen normal
    }

    /**
     * reproduce un efecto de sonido con un ajuste de volumen.
     * @param nombrearchivo el nombre del archivo
     * @param volumendb ajuste en decibelios (ej: -10.0f para bajarlo, 0.0f normal)
     */
    public static void reproducirEfectoConVolumen(String nombreArchivo, float volumenDb) {
        // evitar solapamiento si múltiples clientes intentan reproducir lo mismo al mismo tiempo
        long tiempoActual = System.currentTimeMillis();
        if (ultimoTiempoEfecto.containsKey(nombreArchivo)) {
            if (tiempoActual - ultimoTiempoEfecto.get(nombreArchivo) < 500) {
                return; 
            }
        }
        ultimoTiempoEfecto.put(nombreArchivo, tiempoActual);

        new Thread(() -> {
            try {
                InputStream is = ReproductorMusica.class.getClassLoader().getResourceAsStream(nombreArchivo);
                if (is == null) {
                    File f = new File("src/main/resources/" + nombreArchivo);
                    if (!f.exists()) f = new File("resources/" + nombreArchivo);
                    if (f.exists()) is = new java.io.FileInputStream(f);
                }
                if (is != null) {
                    InputStream bufferedIn = new BufferedInputStream(is);
                    AudioInputStream audioIn = AudioSystem.getAudioInputStream(bufferedIn);
                    Clip clip = AudioSystem.getClip();
                    clip.open(audioIn);
                    
                    // ajustar el volumen si el sistema lo permite usando master_gain
                    if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                        FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                        gainControl.setValue(volumenDb); // aplicar los decibelios indicados
                    }

                    clip.start();
                    
                    // liberar memoria al terminar de sonar
                    clip.addLineListener(event -> {
                        if (event.getType() == LineEvent.Type.STOP) {
                            clip.close();
                        }
                    });
                }
            } catch (Exception e) {
                System.err.println("dj: error al reproducir efecto " + nombreArchivo + " - " + e.getMessage());
            }
        }).start();
    }

    /**
     * reproduce un efecto de sonido una cantidad específica de veces adicionales.
     * @param repeticiones número de veces a REPETIR después de la primera reproducción (ej: 2 repeticiones = suena 3 veces en total)
     */
    public static void reproducirEfectoRepetido(String nombreArchivo, int repeticiones) {
        // Evitar solapamiento si múltiples clientes intentan reproducir lo mismo al mismo tiempo (PruebaDirecta)
        long tiempoActual = System.currentTimeMillis();
        if (ultimoTiempoEfecto.containsKey(nombreArchivo)) {
            if (tiempoActual - ultimoTiempoEfecto.get(nombreArchivo) < 500) {
                return; // Si pasó menos de medio segundo desde la última vez, ignorar
            }
        }
        ultimoTiempoEfecto.put(nombreArchivo, tiempoActual);

        new Thread(() -> {
            try {
                InputStream is = ReproductorMusica.class.getClassLoader().getResourceAsStream(nombreArchivo);
                if (is == null) {
                    File f = new File("src/main/resources/" + nombreArchivo);
                    if (!f.exists()) f = new File("resources/" + nombreArchivo);
                    if (f.exists()) is = new java.io.FileInputStream(f);
                }
                if (is != null) {
                    InputStream bufferedIn = new BufferedInputStream(is);
                    AudioInputStream audioIn = AudioSystem.getAudioInputStream(bufferedIn);
                    Clip clip = AudioSystem.getClip();
                    clip.open(audioIn);
                    if (repeticiones > 0) {
                        clip.loop(repeticiones);
                    } else {
                        clip.start();
                    }
                    // cierra el clip automáticamente cuando termina de sonar para liberar memoria
                    clip.addLineListener(event -> {
                        if (event.getType() == LineEvent.Type.STOP) {
                            clip.close();
                        }
                    });
                }
            } catch (Exception e) {
                System.err.println("dj: error al reproducir efecto " + nombreArchivo + " - " + e.getMessage());
            }
        }).start();
    }

    /**
     * activa o desactiva un sonido en bucle (alarma de sabotaje, música de expulsión, etc.).
     * @param nombreArchivo el nombre del archivo wav
     * @param activar true para iniciar el loop, false para detenerlo
     */
    public static synchronized void manejarAlarma(String nombreArchivo, boolean activar) {
        try {
            Clip clip = clipsBucle.get(nombreArchivo);
            
            if (activar) {
                // si ya existe el clip
                if (clip != null) {
                    if (clip.isRunning()) return; // si ya está sonando, no hacemos nada
                    // si estaba pausado, lo reiniciamos
                    clip.setFramePosition(0);
                    clip.loop(Clip.LOOP_CONTINUOUSLY);
                    clip.start();
                    return;
                }
                
                // si no existe, lo cargamos desde cero
                InputStream is = ReproductorMusica.class.getClassLoader().getResourceAsStream(nombreArchivo);
                if (is == null) {
                    File f = new File("src/main/resources/" + nombreArchivo);
                    if (!f.exists()) f = new File("resources/" + nombreArchivo);
                    if (f.exists()) is = new java.io.FileInputStream(f);
                }
                if (is != null) {
                    InputStream bufferedIn = new BufferedInputStream(is);
                    AudioInputStream audioIn = AudioSystem.getAudioInputStream(bufferedIn);
                    clip = AudioSystem.getClip();
                    clip.open(audioIn);
                    clip.loop(Clip.LOOP_CONTINUOUSLY); // sonido en bucle
                    clip.start();
                    clipsBucle.put(nombreArchivo, clip);
                }
            } else {
                // detener y limpiar el clip
                if (clip != null) {
                    clip.stop();
                    clip.close();
                    clipsBucle.remove(nombreArchivo);
                }
            }
        } catch (Exception e) {
            System.err.println("dj: error con la alarma - " + e.getMessage());
        }
    }
    
    /**
     * método privado (solo para uso interno) para buscar el archivo.
     * @param nombre el nombre del archivo.
     * @return el flujo de datos (inputstream) o null si no lo encuentra.
     */
    private InputStream cargarArchivo(String nombre) {
        try {
            // buscar en el classpath (lo normal cuando compilamos).
            InputStream is = getClass().getClassLoader().getResourceAsStream(nombre);// busca en recursos del jar
            if (is != null) return is; // retorna si lo encuentra
            
            // buscar como archivo físico en "src/main/resources" (típico de maven).
            java.io.File archivoFisico = new java.io.File("src/main/resources/" + nombre); // ruta maven/gradle
            if (archivoFisico.exists()) return new java.io.FileInputStream(archivoFisico);// abre como fileinputstream
            
            // buscar en una carpeta "resources" en la raíz (a veces pasa).
            archivoFisico = new java.io.File("resources/" + nombre);// ruta alternativa
            if (archivoFisico.exists()) return new java.io.FileInputStream(archivoFisico);// abre archivo

            return null;// retorna null si no encuentra en ninguna ruta
        } catch (Exception e) {
            return null;// retorna null si hay error de lectura
        }
    }
}
