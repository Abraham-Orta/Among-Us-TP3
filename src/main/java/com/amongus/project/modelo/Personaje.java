package com.amongus.project.modelo;

import java.awt.Rectangle;

public class Personaje {
    protected int x, y;
    protected int velocidad;
    protected Rectangle hitbox;
    // Dirección: -1 izquierda, 1 derecha, 0 quieto
    protected int direccion = 1; // Default derecha

    public Personaje(int x, int y, int velocidad) {
        this.x = x;
        this.y = y;
        this.velocidad = velocidad;
        this.hitbox = new Rectangle(x, y, 30, 40); // Hitbox un poco más pequeña
    }

    /**
     * Mueve al personaje en la dirección especificada por dx y dy.
     * @param vx Velocidad en X (puede ser negativo)
     * @param vy Velocidad en Y (puede ser negativo)
     */
    public void mover(int vx, int vy) {
        // Actualizar dirección visual (Sprite flip)
        if (vx > 0) direccion = 1;
        if (vx < 0) direccion = -1;

        // Calcular nueva posición propuesta
        int nuevoX = this.x + vx;
        int nuevoY = this.y + vy;
        
        // Validar colisión con Mapa si existe
        Mapa mapa = EstadoJuego.getInstancia().getMapa();
        
        if (mapa != null) {
            // --- LIMITES DEL MAPA ---
            if (nuevoX < 0) nuevoX = 0;
            if (nuevoY < 0) nuevoY = 0;
            if (nuevoX > mapa.getAncho() - hitbox.width) nuevoX = mapa.getAncho() - hitbox.width;
            if (nuevoY > mapa.getAlto() - hitbox.height) nuevoY = mapa.getAlto() - hitbox.height;

            Rectangle hitboxFutura = new Rectangle(nuevoX, nuevoY, hitbox.width, hitbox.height);
            if (!mapa.hayColision(hitboxFutura)) {
                this.x = nuevoX;
                this.y = nuevoY;
            }
        } else {
            // Si no hay mapa cargado, mover libremente
            this.x = nuevoX;
            this.y = nuevoY;
        }

        actualizarHitbox();
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