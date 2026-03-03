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
    public void actualizar(ManejadorEntrada entrada) {
        if (!vivo) return; // Si está muerto, no se mueve (o se mueve como fantasma, lógica futura)

        int dx = 0;
        int dy = 0;

        // Leemos el estado del teclado
        if (entrada.arriba) dy -= 1;
        if (entrada.abajo) dy += 1;
        if (entrada.izquierda) dx -= 1;
        if (entrada.derecha) dx += 1;

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

        // Logica para paralizar (matar) por contacto
        if (this.impostor && entrada.accionMatar) { // Si el jugador es impostor y presiona la tecla de accion (E)
            intentarParalizarTripulante(); // Llama al metodo para paralizar
            entrada.accionMatar = false; // Resetea la tecla para evitar hacer spam de la accion
        }

        // Logica para vias de acceso rapido (alcantarillas / conductos)
        if (this.impostor && entrada.accionVentilar) { // Si el jugador es impostor y presiona la tecla de ventilacion (F)
            intentarUsarVentilacion(); // Llama al metodo para viajar por el conducto
            entrada.accionVentilar = false; // Resetea la tecla de ventilacion
        }
    }

    private void intentarUsarVentilacion() { // Metodo para usar vias de acceso rapido
        Mapa mapa = EstadoJuego.getInstancia().getMapa(); // Obtenemos el mapa actual de la partida
        if (mapa != null) { // Si hay un mapa cargado correctamente
            java.awt.Point destino = mapa.verificarVentilacion(this.hitbox); // Verifica en el mapa si el jugador esta pisando un conducto
            if (destino != null) { // Si el destino devuelto no es nulo (es decir, piso una alcantarilla valida)
                this.x = destino.x; // Teletransporta la coordenada X del jugador a la del destino de la alcantarilla
                this.y = destino.y; // Teletransporta la coordenada Y del jugador a la del destino de la alcantarilla
                actualizarHitbox(); // Actualiza la hitbox para que viaje instantaneamente con el sprite a la nueva posicion
                System.out.println("¡" + this.nombre + " se ha transportado por un conducto de ventilacion!"); // Aviso de accion en consola
            }
        }
    }

    private void intentarParalizarTripulante() { // Metodo para inhabilitar a un oponente al contacto
        java.util.List<Jugador> todosLosJugadores = EstadoJuego.getInstancia().getJugadores(); // Obtiene la lista de todos los jugadores
        
        for (Jugador otroJugador : todosLosJugadores) { // Recorre cada jugador de la partida
            // Verifica que el oponente no sea el mismo, que este vivo y que sea un tripulante normal
            if (otroJugador != this && otroJugador.isVivo() && !otroJugador.isImpostor()) { 
                
                // Verifica si hay contacto fisico (colision de las cajas delimitadoras / hitboxes)
                if (this.hitbox.intersects(otroJugador.getHitbox())) { // Si las hitboxes chocan
                    otroJugador.setVivo(false); // Paraliza o inhabilita al jugador oponente
                    System.out.println("¡" + this.nombre + " ha paralizado a " + otroJugador.getNombre() + "!"); // Aviso de accion en consola
                    break; // Termina el bucle para paralizar solo a un jugador a la vez
                }
            }
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
