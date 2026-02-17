package com.amongus.project.modelo;

import java.awt.Rectangle;
import com.amongus.project.modelo.EstadoJuego;

public abstract class Personaje {
    protected int x, y;
    protected int velocidad;
    protected Rectangle hitbox;
    // Dirección: -1 izquierda, 1 derecha, 0 quieto (opcional para futuras animaciones)
    protected int direccion;

    public Personaje(int x, int y, int velocidad) {
        this.x = x;
        this.y = y;
        this.velocidad = velocidad;
        this.hitbox = new Rectangle(x, y, 30, 50); // Tamaño por defecto, ajustable
    }

    public void mover(int dx, int dy) {
        if (dx != 0) {
            this.direccion = dx > 0 ? 1 : -1;
        }

        // Calcular nueva posición propuesta
        int nuevoX = this.x + (dx * velocidad);
        int nuevoY = this.y + (dy * velocidad);
        
        // Crear rectángulo de la nueva posición
        Rectangle hitboxFutura = new Rectangle(nuevoX, nuevoY, hitbox.width, hitbox.height);

        // Verificar colisión con el mapa
        Mapa mapa = EstadoJuego.getInstancia().getMapa();
        if (mapa != null && !mapa.hayColision(hitboxFutura)) {
            // Si no hay colisión, mover
            this.x = nuevoX;
            this.y = nuevoY;
            actualizarHitbox();
        }
    }

    protected void actualizarHitbox() {
        hitbox.setLocation(x, y);
    }

    // Getters y Setters
    public int getX() { return x; }
    public void setX(int x) { this.x = x; actualizarHitbox(); }
    
    public int getY() { return y; }
    public void setY(int y) { this.y = y; actualizarHitbox(); }
    
    public int getVelocidad() { return velocidad; }
    public void setVelocidad(int velocidad) { this.velocidad = velocidad; }
    
    public Rectangle getHitbox() { return hitbox; }
    
    public int getDireccion() { return direccion; }
}
