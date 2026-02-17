package com.amongus.project.modelo;

import java.awt.Color;
import java.awt.Rectangle;

public class Jugador extends Personaje {
    private String nombre;
    private Color color;
    private boolean impostor;
    private boolean vivo = true;

    public Jugador(String nombre, int x, int y, Color color, boolean impostor) {
        super(x, y, 5); // Velocidad por defecto 5
        this.nombre = nombre;
        this.color = color;
        this.impostor = impostor;
    }

    // El método mover y actualizarHitbox ahora se heredan de Personaje
    
    // Getters y Setters específicos de Jugador
    public Color getColor() { return color; }
    public String getNombre() { return nombre; }
    public boolean isImpostor() { return impostor; }
    public boolean isVivo() { return vivo; }
    public void setVivo(boolean vivo) { this.vivo = vivo; }
}
