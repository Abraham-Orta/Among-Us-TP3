package com.amongus.project.modelo;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public class Mapa {
    private int ancho, alto;
    private List<Rectangle> obstaculos;
    private List<Conducto> conductos;
    private Image imagenFondo;

    public Mapa() {
        this.ancho = 2880; // Tamaño del mapa real
        this.alto = 1920;
        this.obstaculos = new ArrayList<>();
        this.conductos = new ArrayList<>();
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
        obstaculos.add(new Rectangle(0, 0, ancho, 20));          // Arriba
        obstaculos.add(new Rectangle(0, alto - 20, ancho, 20));  // Abajo
        obstaculos.add(new Rectangle(0, 0, 20, alto));           // Izquierda
        obstaculos.add(new Rectangle(ancho - 20, 0, 20, alto));  // Derecha

        // Obstáculos internos
        obstaculos.add(new Rectangle(200, 200, 100, 100)); // Una caja grande en medio
        obstaculos.add(new Rectangle(500, 100, 50, 300));  // Una pared vertical
        obstaculos.add(new Rectangle(100, 450, 200, 50));  // Una pared horizontal abajo

        // Conductos de ventilación (vías rápidas entre zonas del mapa)
        conductos.add(new Conducto(new Rectangle(100, 100, 40, 40), new Point(600, 100)));  // Izquierda → Derecha
        conductos.add(new Conducto(new Rectangle(600, 100, 40, 40), new Point(100, 100)));  // Derecha → Izquierda
    }

    public void render(Graphics g) {
        // Dibujar fondo (imagen real del mapa o gris de respaldo)
        if (imagenFondo != null) {
            g.drawImage(imagenFondo, 0, 0, null);
        } else {
            g.setColor(Color.GRAY);
            g.fillRect(0, 0, ancho, alto);
        }

        // Dibujar obstáculos
        g.setColor(Color.DARK_GRAY);
        for (Rectangle obs : obstaculos) {
            g.fillRect(obs.x, obs.y, obs.width, obs.height);
        }

        // Dibujar conductos de ventilación
        for (Conducto c : conductos) {
            g.setColor(new Color(50, 50, 50));
            g.fillRect(c.area.x, c.area.y, c.area.width, c.area.height);
            g.setColor(Color.LIGHT_GRAY);
            g.drawRect(c.area.x, c.area.y, c.area.width, c.area.height);
            // Rejilla decorativa
            g.drawLine(c.area.x, c.area.y + 10, c.area.x + c.area.width, c.area.y + 10);
            g.drawLine(c.area.x, c.area.y + 20, c.area.x + c.area.width, c.area.y + 20);
            g.drawLine(c.area.x, c.area.y + 30, c.area.x + c.area.width, c.area.y + 30);
        }
    }

    public boolean hayColision(Rectangle rectFuturo) {
        for (Rectangle obs : obstaculos) {
            if (obs.intersects(rectFuturo)) {
                return true;
            }
        }
        return false;
    }

    public Point verificarVentilacion(Rectangle hitboxJugador) {
        for (Conducto c : conductos) {
            if (c.area.intersects(hitboxJugador)) {
                return c.destino;
            }
        }
        return null;
    }

    public List<Rectangle> getObstaculos() { return obstaculos; }
    public int getAncho() { return ancho; }
    public int getAlto() { return alto; }

    // Clase interna que representa un conducto de ventilación
    public static class Conducto {
        public Rectangle area;    // Zona de entrada (hitbox en el suelo)
        public Point destino;     // Coordenadas de salida

        public Conducto(Rectangle area, Point destino) {
            this.area = area;
            this.destino = destino;
        }
    }
}