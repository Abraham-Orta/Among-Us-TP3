package com.amongus.project.modelo;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

public class Mapa {
    private int ancho, alto;
    private List<Rectangle> obstaculos;

    public Mapa() {
        this.ancho = 800; // Tamaño del panel por defecto
        this.alto = 600;
        this.obstaculos = new ArrayList<>();
        crearMapaPrueba();
    }

    private void crearMapaPrueba() {
        // Bordes
        obstaculos.add(new Rectangle(0, 0, ancho, 20)); // Arriba
        obstaculos.add(new Rectangle(0, alto - 20, ancho, 20)); // Abajo
        obstaculos.add(new Rectangle(0, 0, 20, alto)); // Izquierda
        obstaculos.add(new Rectangle(ancho - 20, 0, 20, alto)); // Derecha

        // Obstáculos internos (ej: mesas, cajas)
        obstaculos.add(new Rectangle(200, 200, 100, 100)); // Una caja grande en medio
        obstaculos.add(new Rectangle(500, 100, 50, 300)); // Una pared vertical
        obstaculos.add(new Rectangle(100, 450, 200, 50)); // Una pared horizontal abajo
    }

    public void render(Graphics g) {
        g.setColor(Color.GRAY);
        // Dibujar suelo (opcional, por ahora fondo negro del panel)
        
        g.setColor(Color.DARK_GRAY);
        for (Rectangle obs : obstaculos) {
            g.fillRect(obs.x, obs.y, obs.width, obs.height);
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
    
    public List<Rectangle> getObstaculos() { return obstaculos; }
}
