package com.amongus.project.modelo;

import java.awt.Color;
import java.awt.Rectangle;

public class Jugador {
    private String nombre;
    private int x, y;
    private Color color;
    private int velocidad = 5;
    private boolean impostor;
    private boolean vivo = true;
    // Hitbox para colisiones
    private Rectangle hitbox;

    public Jugador(String nombre, int x, int y, Color color, boolean impostor) {
        this.nombre = nombre;
        this.x = x;
        this.y = y;
        this.color = color;
        this.impostor = impostor;
        this.hitbox = new Rectangle(x, y, 30, 50); // Tamaño aproximado
    }

    public void mover(int dx, int dy) {
        this.x += dx * velocidad;
        this.y += dy * velocidad;
        actualizarHitbox();
    }

    private void actualizarHitbox() {
        hitbox.setLocation(x, y);
    }
    
    // Getters y Setters
    public int getX() { return x; }
    public int getY() { return y; }
    public Color getColor() { return color; }
    public String getNombre() { return nombre; }
    public boolean isImpostor() { return impostor; }
    public boolean isVivo() { return vivo; }
    public void setVivo(boolean vivo) { this.vivo = vivo; }
}
