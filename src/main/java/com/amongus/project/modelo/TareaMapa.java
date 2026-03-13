package com.amongus.project.modelo;

import java.awt.Rectangle;

/**
 * TareaMapa
 * =========
 * Representa una zona física en el mapa donde se puede realizar una tarea.
 */
public class TareaMapa {
    private String nombre; // "simon", "energia", "numeros"
    private Rectangle zona;

    public TareaMapa(String nombre, Rectangle zona) {
        this.nombre = nombre;
        this.zona = zona;
    }

    public String getNombre() {
        return nombre;
    }

    public Rectangle getZona() {
        return zona;
    }
}
