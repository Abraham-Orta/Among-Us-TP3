package com.amongus.project.modelo;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public class Mapa {
    private int ancho, alto;
    private List<Rectangle> obstaculos;
    private Image imagenFondo;

    public Mapa() {
        this.ancho = 2880; // Tamaño del mapa real
        this.alto = 1920;
        this.obstaculos = new ArrayList<>();
        cargarImagenFondo();
        crearMapaPrueba();
    }

    private void cargarImagenFondo() {
        try {
            File f = new File("mapa/mapa1.png");
            if (f.exists()) {
                imagenFondo = ImageIO.read(f);
            } else {
                System.err.println("No se encontró el archivo del mapa: " + f.getAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Error al cargar la imagen del mapa: " + e.getMessage());
        }
    }

    private void crearMapaPrueba() {
        // Bordes
        obstaculos.add(new Rectangle(0, 0, ancho, 20)); // Arriba
        obstaculos.add(new Rectangle(0, alto - 20, ancho, 20)); // Abajo
        obstaculos.add(new Rectangle(0, 0, 20, alto)); // Izquierda
        obstaculos.add(new Rectangle(ancho - 20, 0, 20, alto)); // Derecha

        // Obstáculos internos (ej: mesas, cajas) - Estos son solo de prueba
        obstaculos.add(new Rectangle(200, 200, 100, 100)); // Una caja grande en medio
        obstaculos.add(new Rectangle(500, 100, 50, 300)); // Una pared vertical
        obstaculos.add(new Rectangle(100, 450, 200, 50)); // Una pared horizontal abajo
    }

    public void render(Graphics g) {
        if (imagenFondo != null) {
            g.drawImage(imagenFondo, 0, 0, null);
        } else {
            g.setColor(Color.GRAY);
            g.fillRect(0, 0, ancho, alto);
        }
        
        // Dibujar obstáculos (opcional, para depuración)
        /*
        g.setColor(new Color(255, 0, 0, 100)); // Rojo semitransparente
        for (Rectangle obs : obstaculos) {
            g.fillRect(obs.x, obs.y, obs.width, obs.height);
        }
        */
    }

    public boolean hayColision(Rectangle rectFuturo) {
        for (Rectangle obs : obstaculos) {
            if (obs.intersects(rectFuturo)) {
                return true;
            }
        }
        return false;
    }
    
    public List<Rectangle> getObstaculos() { return obstaculos; }
    public int getAncho() { return ancho; }
    public int getAlto() { return alto; }
}
