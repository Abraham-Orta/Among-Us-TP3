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
        cargarImagenFondo(nombreArchivoTmj);
        crearMapaPrueba();
        
        cargarImagenFondo(nombreArchivoTmj);
        
        crearMapaPrueba();
        
       CargadorNivel cargador = new CargadorNivel("src/main/resources/" + nombreArchivoTmj);
      
        for (java.awt.geom.Rectangle2D.Double rectDouble : cargador.getColisiones()) {
            Rectangle rectNormal = new Rectangle(
                (int) rectDouble.x, 
                (int) rectDouble.y, 
                (int) rectDouble.width, 
                (int) rectDouble.height
            );
            this.obstaculos.add(rectNormal);
        }
        for (java.awt.geom.Rectangle2D.Double rectDouble : cargador.getAlcantarillas()) {
            Rectangle rectNormal = new Rectangle(
                (int) rectDouble.x, 
                (int) rectDouble.y, 
                (int) rectDouble.width, 
                (int) rectDouble.height
            );
            this.alcantarillas.add(rectNormal);
        }
      
    }
    
      
    private void cargarImagenFondo(String nombreArchivoTmj) throws IOException {
        
        String nombreImagen = nombreArchivoTmj.replace(".tmj", ".png");
        
        if (cacheImagenes.containsKey(nombreImagen)) {
            imagenFondo = cacheImagenes.get(nombreImagen);
            return;
        }
            
       
        try {
            File archivoImagen = new File("mapa/" + nombreImagen);
        
            if (archivoImagen.exists()) {
                imagenFondo = javax.imageio.ImageIO.read(archivoImagen);
                cacheImagenes.put(nombreImagen, imagenFondo);
            } else {
                System.out.println("¡Aviso! No se encontró la imagen en: " + archivoImagen.getAbsolutePath());
            }
        } catch (java.io.IOException e) {
            System.out.println("Error al cargar la imagen del mapa: " + e.getMessage());
        }
        
            File f = new File("mapa/" + nombreArchivoTmj);
            if (f.exists()) {
                imagenFondo = ImageIO.read(f);
            } else {
                java.net.URL u = getClass().getClassLoader().getResource(nombreArchivoTmj);
                if (u != null) {
                    imagenFondo = ImageIO.read(u);
                } else {
                    System.err.println("No se encontró el archivo del mapa: " + nombreArchivoTmj);
                }
            }
            // Guardar en caché para reutilización
            if (imagenFondo != null) {
                cacheImagenes.put(nombreArchivoTmj, imagenFondo);
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
     *                          PanelJuego lo obtiene de su instancia de ManejadorEntrada.
     */
    public void render(Graphics g, boolean modoDesarrollador) {
        // Fondo: imagen real o gris de respaldo
        if (imagenFondo != null) {
            g.drawImage(imagenFondo, 0, 0, null);
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

    /** Retorna true si rectFuturo choca con alguna pared. */
    public boolean hayColision(Rectangle rectFuturo) {
        for (Rectangle obs : obstaculos) {
            if (obs.intersects(rectFuturo)) return true;
        }
        return false;
    }

    public List<Rectangle> getObstaculos()    { return obstaculos; }
    public List<Rectangle> getAlcantarillas() { return alcantarillas; }
    public int getAncho()                      { return ancho; }
    public int getAlto()                       { return alto; }
}