package com.amongus.project.modelo; // Paquete que define la estructura lógica del juego

import java.awt.Color; // Para manejar colores en caso de no tener imagen
import java.awt.Graphics; // Para poder dibujar elementos en el panel
import java.awt.Image; // Para manejar la imagen de fondo del mapa
import java.awt.Rectangle; // Para crear cajas de colisión (hitboxes)
import java.io.File; // Para buscar archivos físicos en el disco
import java.io.IOException; // Para manejar errores de lectura de archivos
import java.util.ArrayList; // Para usar listas dinámicas
import java.util.List; // Interfaz para las listas
import javax.imageio.ImageIO; // Para leer la imagen del archivo

import com.amongus.project.controlador.ManejadorEntrada; // Importamos el manejador para saber si el modo desarrollador está activo

/**
 * Clase Mapa
 * ==========
 * Representa el entorno físico donde se mueven los jugadores.
 * Se encarga de dibujar el fondo, manejar los límites del mundo, 
 * los obstáculos con los que se choca y ahora las VÍAS DE ACCESO RÁPIDO (Alcantarillas).
 */
public class Mapa {
    
    private int ancho, alto; // Dimensiones totales del mapa en píxeles
    private List<Rectangle> obstaculos; // Lista de paredes y objetos sólidos que bloquean el paso
    private List<Rectangle> alcantarillas; // NUEVO: Lista de vías de acceso rápido para impostores
    private Image imagenFondo; // La imagen gráfica que se muestra de fondo

    /**
     * Constructor del Mapa.
     * Inicializa las listas, establece el tamaño y carga los elementos.
     */
    public Mapa(String nombreArchivoFondo) {
        this.ancho = 2880; // Ancho total del mundo virtual (más grande que la ventana)
        this.alto = 1920;  // Alto total del mundo virtual
        
        this.obstaculos = new ArrayList<>(); // Inicializamos la lista de obstáculos vacía
        this.alcantarillas = new ArrayList<>(); // Inicializamos la lista de alcantarillas vacía
        
        cargarImagenFondo(nombreArchivoFondo); // Intentamos cargar la imagen elegida por el host
        crearMapaPrueba();   // Generamos los obstáculos y alcantarillas físicas
    }

    /**
     * Intenta cargar el archivo de imagen del mapa.
     */
    private void cargarImagenFondo(String nombreArchivoFondo) {
        try {
            // Creamos una referencia al archivo de imagen
            File f = new File("mapa/" + nombreArchivoFondo);
            
            // Verificamos si el archivo existe realmente en la carpeta
            if (f.exists()) {
                imagenFondo = ImageIO.read(f); // Leemos y guardamos la imagen en memoria
            } else {
                // Si no está, intentamos cargarlo desde el classpath
                java.net.URL u = getClass().getClassLoader().getResource(nombreArchivoFondo);
                if (u != null) {
                    imagenFondo = ImageIO.read(u);
                } else {
                    System.err.println("No se encontró el archivo del mapa: " + nombreArchivoFondo);
                }
            }
        } catch (IOException e) {
            // Si hubo un error leyendo el archivo (archivo corrupto, etc.)
            System.err.println("Error al cargar la imagen del mapa: " + e.getMessage());
        }
    }

    /**
     * Define dónde están los obstáculos (paredes) y las alcantarillas.
     * Todo esto usa coordenadas X e Y relativas al tamaño total del mapa (2880x1920).
     */
    private void crearMapaPrueba() {
        // --- 1. LÍMITES DEL MAPA (Bordes invisibles para no salirse del universo) ---
        obstaculos.add(new Rectangle(0, 0, ancho, 20)); // Borde superior (techo)
        obstaculos.add(new Rectangle(0, alto - 20, ancho, 20)); // Borde inferior (suelo límite)
        obstaculos.add(new Rectangle(0, 0, 20, alto)); // Borde izquierdo (pared izquierda)
        obstaculos.add(new Rectangle(ancho - 20, 0, 20, alto)); // Borde derecho (pared derecha)

        // --- 2. OBSTÁCULOS INTERNOS DE PRUEBA ---
        // (Se asume que luego se adaptarán a las paredes reales de "mapa1.png")
        obstaculos.add(new Rectangle(200, 200, 100, 100)); // Caja cuadrada en medio
        obstaculos.add(new Rectangle(500, 100, 50, 300)); // Pared vertical larga
        obstaculos.add(new Rectangle(100, 450, 200, 50)); // Pared horizontal

        // --- 3. VÍAS DE ACCESO RÁPIDO (ALCANTARILLAS / VENTS) ---
        // Se piden vías de acceso rápido que conecten un espacio con otro.
        // Creamos rectángulos de 60x60 píxeles que servirán como zonas de teletransporte.
        alcantarillas.add(new Rectangle(300, 300, 60, 60));   // Alcantarilla 0 (Sala A)
        alcantarillas.add(new Rectangle(800, 300, 60, 60));   // Alcantarilla 1 (Sala B)
        alcantarillas.add(new Rectangle(500, 800, 60, 60));   // Alcantarilla 2 (Sala C)
        alcantarillas.add(new Rectangle(1200, 800, 60, 60));  // Alcantarilla 3 (Sala D)
    }

    /**
     * Se encarga de dibujar el mapa en la pantalla cada vez que se refresca el juego (60 FPS).
     * @param g El objeto Graphics usado para pintar en el lienzo.
     */
    public void render(Graphics g) {
        // Si logramos cargar la imagen del mapa, la dibujamos en la coordenada (0,0)
        if (imagenFondo != null) {
            g.drawImage(imagenFondo, 0, 0, null);
        } else {
            // Si no hay imagen, pintamos un rectángulo gris gigante como fondo provisional
            g.setColor(Color.GRAY);
            g.fillRect(0, 0, ancho, alto);
        }
        
        // --- DIBUJADO DE ALCANTARILLAS (Para que el jugador sepa dónde están) ---
        // Iteramos por la lista de alcantarillas y dibujamos un cuadro visual
        for (Rectangle vent : alcantarillas) {
            // Color gris oscuro para el fondo de la alcantarilla
            g.setColor(new Color(50, 50, 50)); 
            g.fillRect(vent.x, vent.y, vent.width, vent.height);
            
            // Dibujamos un borde blanco con rayas simulando la rejilla
            g.setColor(Color.WHITE);
            g.drawRect(vent.x, vent.y, vent.width, vent.height);
            g.drawLine(vent.x + 10, vent.y, vent.x + 10, vent.y + vent.height); // Rejilla 1
            g.drawLine(vent.x + 30, vent.y, vent.x + 30, vent.y + vent.height); // Rejilla 2
            g.drawLine(vent.x + 50, vent.y, vent.x + 50, vent.y + vent.height); // Rejilla 3
        }
        
        // --- MODO DESARROLLADOR: DIBUJAR HITBOXES ---
        // Si el usuario presionó F3, dibujamos las cajas de colisión para entender qué pasa
        if (ManejadorEntrada.modoDesarrollador) {
            g.setColor(new Color(255, 0, 0, 150)); // Rojo semitransparente para ver las hitboxes
            for (Rectangle obs : obstaculos) {
                g.fillRect(obs.x, obs.y, obs.width, obs.height);
            }
            
            g.setColor(new Color(0, 0, 255, 150)); // Azul para las alcantarillas
            for (Rectangle vent : alcantarillas) {
                g.fillRect(vent.x, vent.y, vent.width, vent.height);
            }
        }
    }

    /**
     * Comprueba si un rectángulo propuesto (el jugador en su próximo paso) 
     * choca contra alguna pared de la lista de obstáculos.
     * @param rectFuturo La posición a la que el jugador quiere moverse.
     * @return true si choca, false si el camino está libre.
     */
    public boolean hayColision(Rectangle rectFuturo) {
        // Recorremos todos los obstáculos registrados
        for (Rectangle obs : obstaculos) {
            // "intersects" es un método de Java que verifica si dos rectángulos se solapan
            if (obs.intersects(rectFuturo)) {
                return true; // ¡Choque detectado!
            }
        }
        return false; // El camino está libre
    }
    
    // Getters para que otras clases puedan acceder a la información del mapa
    public List<Rectangle> getObstaculos() { return obstaculos; }
    public List<Rectangle> getAlcantarillas() { return alcantarillas; } // Nuevo getter para las vías de acceso
    public int getAncho() { return ancho; }
    public int getAlto() { return alto; }
}
