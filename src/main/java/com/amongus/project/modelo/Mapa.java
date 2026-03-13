package com.amongus.project.modelo;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;



/**
 * Clase Mapa
 * ==========
 * Entorno físico donde se mueven los jugadores.
 * Maneja el fondo, los obstáculos de colisión y las vías de acceso rápido (alcantarillas).
 */
public class Mapa {

    // Caché estático: la imagen se carga del disco UNA sola vez y se reutiliza
    private static final Map<String, Image> cacheImagenes = new HashMap<>();

    private int ancho, alto;
    private List<Rectangle> obstaculos;
    private List<Rectangle> alcantarillas; // Vías de acceso rápido (vents)
    private List<Rectangle> botones;
    private Image imagenFondo;

    /**
     * @param nombreArchivoTmj nombre del archivo de imagen dentro de la carpeta "mapa/",
     *                            p.ej. "mapa1.png"
     */
    public Mapa(String nombreArchivoTmj) throws IOException {
        this.ancho         = 2880;
        this.alto          = 1920;
        this.obstaculos    = new ArrayList<>();
        this.alcantarillas = new ArrayList<>();
        this.botones       = new ArrayList<>();
        // Fix #2: Eliminar llamadas duplicadas — antes se llamaba 2 veces a cada uno
        cargarImagenFondo(nombreArchivoTmj);
        crearMapaPrueba();

        CargadorNivel cargador = new CargadorNivel("src/main/resources/" + nombreArchivoTmj);

        for (java.awt.geom.Rectangle2D.Double rectDouble : cargador.getColisiones()) {
            obstaculos.add(new Rectangle(
                (int) rectDouble.x, (int) rectDouble.y,
                (int) rectDouble.width, (int) rectDouble.height));
        }
        for (java.awt.geom.Rectangle2D.Double rectDouble : cargador.getAlcantarillas()) {
            alcantarillas.add(new Rectangle(
                (int) rectDouble.x, (int) rectDouble.y,
                (int) rectDouble.width, (int) rectDouble.height));
        }
        for (java.awt.geom.Rectangle2D.Double rectDouble : cargador.getBotonesEmergencia()) {
            botones.add(new Rectangle(
                (int) rectDouble.x, (int) rectDouble.y,
                (int) rectDouble.width, (int) rectDouble.height));
        }
    }
    
      
    private void cargarImagenFondo(String nombreArchivoTmj) throws IOException {
        // Fix #2: Normalizar la clave de caché a siempre usar la extensión .png
        String nombreImagen = nombreArchivoTmj.replace(".tmj", ".png");

        // Revisamos caché con la clave normalizada
        if (cacheImagenes.containsKey(nombreImagen)) {
            imagenFondo = cacheImagenes.get(nombreImagen);
            return;
        }

        // Intentar cargar desde el sistema de archivos o classpath
        File archivoImagen = new File("mapa/" + nombreImagen);
        if (archivoImagen.exists()) {
            imagenFondo = ImageIO.read(archivoImagen);
        } else {
            java.net.URL u = getClass().getClassLoader().getResource(nombreImagen);
            if (u != null) {
                imagenFondo = ImageIO.read(u);
            } else {
                System.err.println("No se encontró el archivo del mapa: " + nombreImagen);
            }
        }

        // Guardar en caché con la clave normalizada
        if (imagenFondo != null) {
            cacheImagenes.put(nombreImagen, imagenFondo);
        }
    }
    private void crearMapaPrueba() {
        // --- Bordes ---
        obstaculos.add(new Rectangle(0,          0,         ancho, 20));
        obstaculos.add(new Rectangle(0,          alto - 20, ancho, 20));
        obstaculos.add(new Rectangle(0,          0,         20,    alto));
        obstaculos.add(new Rectangle(ancho - 20, 0,         20,    alto));

       

        // --- Vías de acceso rápido (alcantarillas) ---
       // alcantarillas.add(new Rectangle( 300, 300, 60, 60));  // Sala A
        //alcantarillas.add(new Rectangle( 800, 300, 60, 60));  // Sala B
        //alcantarillas.add(new Rectangle( 500, 800, 60, 60));  // Sala C
        //alcantarillas.add(new Rectangle(1200, 800, 60, 60));  // Sala D
    }

    /**
     * @param modoDesarrollador si es true dibuja las hitboxes de obstáculos y alcantarillas.
     *
     * @param camX coordenada X de la cámara (esquina superior izquierda del viewport en el mundo)
     * @param camY coordenada Y de la cámara
     * @param viewportW ancho del viewport visible (ej. 800)
     * @param viewportH alto del viewport visible (ej. 600)
     */
    public void render(Graphics g, boolean modoDesarrollador, int camX, int camY, int viewportW, int viewportH) {
        if (imagenFondo != null) {
            // Fix #6: Viewport clipping — solo dibujamos la porción visible del mapa.
            // drawImage(img, dx1,dy1,dx2,dy2, sx1,sy1,sx2,sy2, observer)
            // Destino: esquina (0,0) → (viewportW, viewportH) en pantalla
            // Fuente: región (camX, camY) → (camX+viewportW, camY+viewportH) en el mapa
            g.drawImage(imagenFondo,
                0, 0, viewportW, viewportH,          // destino (pantalla)
                camX, camY, camX + viewportW, camY + viewportH, // fuente (mapa)
                null);
        } else {
            g.setColor(Color.GRAY);
            g.fillRect(0, 0, ancho, alto);
        }

        // Dibujar alcantarillas con rejilla decorativa
        for (Rectangle vent : alcantarillas) {
            g.setColor(new Color(50, 50, 50));
            g.fillRect(vent.x, vent.y, vent.width, vent.height);
            g.setColor(Color.WHITE);
            g.drawRect(vent.x, vent.y, vent.width, vent.height);
            g.drawLine(vent.x + 10, vent.y, vent.x + 10, vent.y + vent.height);
            g.drawLine(vent.x + 30, vent.y, vent.x + 30, vent.y + vent.height);
            g.drawLine(vent.x + 50, vent.y, vent.x + 50, vent.y + vent.height);
        }

        // Modo desarrollador (F3): hitboxes visibles
        if (modoDesarrollador) {
            g.setColor(new Color(255, 0, 0, 150));
            for (Rectangle obs : obstaculos) {
                g.fillRect(obs.x, obs.y, obs.width, obs.height);
            }
            g.setColor(new Color(0, 0, 255, 150));
            for (Rectangle vent : alcantarillas) {
                g.fillRect(vent.x, vent.y, vent.width, vent.height);
            }
        }
    }

    /** Compatibilidad: render sin viewport (dibuja el mapa completo, como antes) */
    public void render(Graphics g, boolean modoDesarrollador) {
        if (imagenFondo != null) {
            g.drawImage(imagenFondo, 0, 0, null);
        } else {
            g.setColor(Color.GRAY);
            g.fillRect(0, 0, ancho, alto);
        }
        for (Rectangle vent : alcantarillas) {
            g.setColor(new Color(50, 50, 50));
            g.fillRect(vent.x, vent.y, vent.width, vent.height);
            g.setColor(Color.WHITE);
            g.drawRect(vent.x, vent.y, vent.width, vent.height);
            g.drawLine(vent.x + 10, vent.y, vent.x + 10, vent.y + vent.height);
            g.drawLine(vent.x + 30, vent.y, vent.x + 30, vent.y + vent.height);
            g.drawLine(vent.x + 50, vent.y, vent.x + 50, vent.y + vent.height);
        }
        if (modoDesarrollador) {
            g.setColor(new Color(255, 0, 0, 150));
            for (Rectangle obs : obstaculos) g.fillRect(obs.x, obs.y, obs.width, obs.height);
            g.setColor(new Color(0, 0, 255, 150));
            for (Rectangle vent : alcantarillas) g.fillRect(vent.x, vent.y, vent.width, vent.height);
        }
    }

    /**
     * Fix #7: Colisiones con filtrado espacial.
     * Solo evalúa obstáculos dentro de un radio de 200px del centro del rectángulo futuro,
     * evitando iterar cientos de obstáculos lejanos en cada frame.
     */
    public boolean hayColision(Rectangle rectFuturo) {
        int cx = rectFuturo.x + rectFuturo.width  / 2;
        int cy = rectFuturo.y + rectFuturo.height / 2;
        final int RADIO_FILTRADO = 200;
        for (Rectangle obs : obstaculos) {
            // Filtrado espacial: solo procesar obstáculos cercanos
            int ox = obs.x + obs.width  / 2;
            int oy = obs.y + obs.height / 2;
            int dx = cx - ox;
            int dy = cy - oy;
            if (dx * dx + dy * dy > RADIO_FILTRADO * RADIO_FILTRADO) continue;
            if (obs.intersects(rectFuturo)) return true;
        }
        return false;
    }

    public List<Rectangle> getObstaculos()    { return obstaculos; }
    public List<Rectangle> getAlcantarillas() { return alcantarillas; }
    public List<Rectangle> getBotones()       { return botones; }
    public int getAncho()                      { return ancho; }
    public int getAlto()                       { return alto; }
}