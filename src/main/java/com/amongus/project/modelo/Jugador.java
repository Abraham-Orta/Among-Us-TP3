package com.amongus.project.modelo;

import java.awt.Color;
import com.amongus.project.controlador.ManejadorEntrada;

public class Jugador extends Personaje {
    private String nombre;
    private Color color;
    private boolean impostor;
    private boolean vivo = true;
    
    // Variables para control de red (evitar enviar spam de paquetes si no me muevo)
    private int ultimoXEnviado = -1;
    private int ultimoYEnviado = -1;
    
    // Variables de Votación
    private boolean haVotado = false;
    private boolean votoSkip = false;
    private Jugador votoJugador = null; // A quién votó este jugador

    public Jugador(String nombre, int x, int y, Color color, boolean impostor) {
        super(x, y, 4); // Velocidad base 4 (ajustable)
        this.nombre = nombre;
        this.color = color;
        this.impostor = impostor;
    }

    // Métodos de votación
    public void resetVoto() {
        this.haVotado = false;
        this.votoSkip = false;
        this.votoJugador = null;
    }
    
    public void votarSkip() {
        this.haVotado = true;
        this.votoSkip = true;
        this.votoJugador = null;
    }
    
    public void votarJugador(Jugador objetivo) {
        this.haVotado = true;
        this.votoSkip = false;
        this.votoJugador = objetivo;
    }
    
    public boolean yaVoto() { return haVotado; }
    public boolean isVotoSkip() { return votoSkip; }
    public Jugador getVotoJugador() { return votoJugador; }

    // Este método se llama en cada frame del juego (60 veces por segundo)
    public void actualizar() {
        if (!vivo) return; // Si está muerto, no se mueve (o se mueve como fantasma, lógica futura)

        int dx = 0;
        int dy = 0;

        // Leemos el estado del teclado
        if (ManejadorEntrada.arriba) dy -= 1;
        if (ManejadorEntrada.abajo) dy += 1;
        if (ManejadorEntrada.izquierda) dx -= 1;
        if (ManejadorEntrada.derecha) dx += 1;

        // Si nos estamos moviendo en alguna dirección
        if (dx != 0 || dy != 0) {
            
            // Normalización para diagonales:
            // Si te mueves en X e Y a la vez, la distancia es raíz(2) = 1.41
            // Para mantener la velocidad constante, dividimos por 1.41 (aprox multiplicar por 0.71)
            double magnitud = Math.sqrt(dx * dx + dy * dy);
            
            // Calculamos la velocidad real
            double velocidadX = (dx / magnitud) * velocidad;
            double velocidadY = (dy / magnitud) * velocidad;

            // Movemos usando la lógica de colisiones de la clase padre
            // Nota: Al pasar a int perdemos precisión decimal, pero para pixel art está bien.
            // Una mejora futura sería guardar posición en double.
            super.mover((int)Math.round(velocidadX), (int)Math.round(velocidadY));
            
            // Lógica de Red: Enviar posición al servidor
            enviarPosicionSiCambio();
        }
    }
    
    private void enviarPosicionSiCambio() {
        // Solo enviamos si nos hemos movido una distancia considerable o ha pasado tiempo
        // Para simplificar, si la posición entera cambió, enviamos.
        if (this.x != ultimoXEnviado || this.y != ultimoYEnviado) {
            
            // Obtenemos la instancia del cliente para enviar mensaje
            // Ojo: Esto es una dependencia circular un poco fea, pero funcional por ahora.
            // Idealmente usaríamos un evento, pero vamos directo al grano.
            /* 
               Aquí necesitaríamos acceso al Cliente. 
               Como Jugador es parte del Modelo, no debería conocer al Cliente (Red).
               
               Por ahora, dejaremos que la clase Cliente o BucleJuego maneje el envío,
               o asumimos que el movimiento local ya se replica visualmente.
               
               Para que funcione en red, el BucleJuego debería chequear cambios y notificar.
               Voy a dejar este método preparado pero vacío por ahora para no romper arquitectura,
               y lo implementaré correctamente en el controlador.
            */
            ultimoXEnviado = this.x;
            ultimoYEnviado = this.y;
        }
    }

    public Color getColor() { return color; }
    public String getNombre() { return nombre; }
    public boolean isImpostor() { return impostor; }
    public boolean isVivo() { return vivo; }
    public void setVivo(boolean vivo) { this.vivo = vivo; }
}