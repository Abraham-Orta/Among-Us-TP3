package com.amongus.project.modelo;

import java.awt.Rectangle;

public class Personaje {
    protected int x, y;
    protected double drawX, drawY; // Posición suavizada para renderizado
    protected int velocidad;
    protected Rectangle hitbox;
    // Dirección: -1 izquierda, 1 derecha, 0 quieto
    protected int direccion = 1; // Default derecha

    // Referencia al estado de juego de ESTE cliente (la asigna Jugador)
    protected EstadoJuego estadoJuego;

    public Personaje(int x, int y, int velocidad) {
        this.x = x;
        this.y = y;
        this.drawX = x;
        this.drawY = y;
        this.velocidad = velocidad;
        this.hitbox = new Rectangle(x, y, 30, 40); // Hitbox un poco más pequeña
    }

    /**
     * Actualiza la interpolación suave. Debe ser llamado desde el bucle de juego o panel.
     */
    public void actualizarInterpolacion() {
        if (Math.abs(drawX - x) > 0.5) drawX += (x - drawX) * 0.75;
        else drawX = x;

        if (Math.abs(drawY - y) > 0.5) drawY += (y - drawY) * 0.75;
        else drawY = y;
    }

    public int getDrawX() { return (int) Math.round(drawX); }
    public int getDrawY() { return (int) Math.round(drawY); }

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
        Mapa mapa = (estadoJuego != null) ? estadoJuego.getMapa() : null;
        
        if (mapa != null) {
            // --- LIMITES DEL MAPA ---
            if (nuevoX < 0) nuevoX = 0;
            if (nuevoY < 0) nuevoY = 0;
            if (nuevoX > mapa.getAncho() - hitbox.width) nuevoX = mapa.getAncho() - hitbox.width;
            if (nuevoY > mapa.getAlto() - hitbox.height) nuevoY = mapa.getAlto() - hitbox.height;

            Rectangle hitboxFutura = new Rectangle(nuevoX, nuevoY, hitbox.width, hitbox.height);
            
            // Los fantasmas (jugadores muertos) ignoran las colisiones
            boolean esFantasma = (this instanceof Jugador && !((Jugador)this).isVivo());
            
            if (esFantasma || !mapa.hayColision(hitboxFutura)) {
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

    // Registro del último movimiento para las animaciones en red
    private long ultimoTiempoMovimiento = 0;

    public boolean isMoviendose() {
        // Si ha pasado menos de 150ms desde la última vez que cambió X o Y, se considera en movimiento
        return (System.currentTimeMillis() - ultimoTiempoMovimiento) < 150;
    }

    /**
     * Se llama desde la red para indicar a dónde debe ir el personaje de forma remota.
     */
    public void recibirPosicionRed(int nx, int ny) {
        if (nx > this.x) direccion = 1;
        if (nx < this.x) direccion = -1;
        
        // Si la distancia es muy grande (teletransporte/lag severo), no interpolar
        if (Math.abs(nx - this.x) > 150 || Math.abs(ny - this.y) > 150) {
            this.x = nx;
            this.y = ny;
            this.drawX = nx;
            this.drawY = ny;
        } else {
            this.x = nx;
            this.y = ny;
        }
        this.ultimoTiempoMovimiento = System.currentTimeMillis();
    }

    // Getters y Setters
    public int getX() { return x; }
    public void setX(int nuevoX) { 
        if (this.x != nuevoX) {
            if (nuevoX > this.x) direccion = 1;
            if (nuevoX < this.x) direccion = -1;
            this.x = nuevoX; 
            ultimoTiempoMovimiento = System.currentTimeMillis();
            actualizarHitbox(); 
        }
    }
    
    public int getY() { return y; }
    public void setY(int nuevoY) { 
        if (this.y != nuevoY) {
            this.y = nuevoY; 
            ultimoTiempoMovimiento = System.currentTimeMillis();
            actualizarHitbox(); 
        }
    }
    
    public int getVelocidad() { return velocidad; }
    public void setVelocidad(int velocidad) { this.velocidad = velocidad; }
    
    public Rectangle getHitbox() { return hitbox; }
    
    public int getDireccion() { return direccion; }
}