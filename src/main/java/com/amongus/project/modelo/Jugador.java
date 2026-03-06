package com.amongus.project.modelo; // Paquete lógico del modelo

import java.awt.Color; // Para manejar el color del tripulante
import java.awt.Rectangle; // Para áreas matemáticas de colisión
import java.util.List; // Para manejar las listas de jugadores y alcantarillas
import com.amongus.project.controlador.ManejadorEntrada; // Para leer teclas presionadas

/**
 * Clase Jugador
 * =============
 * Esta clase representa a los participantes del juego. 
 * Hereda de Personaje (que le da coordenadas X, Y, velocidad y un rectángulo de colisión).
 * Ahora incluye las mecánicas clave del documento: PARALIZAR (matar) y VÍAS DE ACCESO (alcantarillas).
 */
public class Jugador extends Personaje {
    
    private String nombre;     // Nombre de usuario en pantalla
    private Color color;       // Color del traje del personaje
    private boolean impostor;  // Define si su rol es Impostor (true) o Tripulante normal (false)
    private boolean vivo = true; // Estado actual. Si está inhabilitado pasa a false.
    
    // Variables para control de red
    private int ultimoXEnviado = -1;
    private int ultimoYEnviado = -1;
    private com.amongus.project.red.Cliente clienteRed; // Para poder enviar mensajes al servidor
    
    // Variables de Votación (Dinámica del juego)
    private boolean haVotado = false;      // Indica si ya emitió un voto en esta reunión
    private boolean votoSkip = false;      // Indica si votó por omitir/saltar (Skip)
    private Jugador votoJugador = null;    // Almacena a qué jugador específico votó
    
    // Variables para tiempos de espera (Cooldowns) para que las acciones no se repitan infinitamente
    private int cooldownAsesinato = 0; // Temporizador para volver a paralizar
    private int cooldownVentilacion = 0; // Temporizador para volver a meterse en una alcantarilla
    private int cooldownReporte = 0; // Temporizador para no spamear el reporte

    /**
     * Constructor del Jugador.
     * Configura los parámetros iniciales al crearse en la partida.
     */
    public Jugador(String nombre, int x, int y, Color color, boolean impostor) {
        super(x, y, 4); // Llama al constructor de "Personaje" dándole x, y, y una velocidad de 4
        this.nombre = nombre;
        this.color = color;
        this.impostor = impostor;
    }
    
    /**
     * Establece el canal de comunicación (Cliente) para que este jugador
     * pueda decirle a los demás en internet dónde está parado.
     */
    public void setClienteRed(com.amongus.project.red.Cliente cliente) {
        this.clienteRed = cliente;
    }

    // ==========================================
    // MÉTODOS DE LA FASE DE VOTACIÓN
    // ==========================================
    
    // Limpia los datos de voto al terminar o iniciar una reunión
    public void resetVoto() {
        this.haVotado = false;
        this.votoSkip = false;
        this.votoJugador = null;
    }
    
    // El jugador decide no votar a nadie
    public void votarSkip() {
        this.haVotado = true;
        this.votoSkip = true;
        this.votoJugador = null;
    }
    
    // El jugador selecciona a otro como sospechoso
    public void votarJugador(Jugador objetivo) {
        this.haVotado = true;
        this.votoSkip = false;
        this.votoJugador = objetivo;
    }
    
    // Getters de votación
    public boolean yaVoto() { return haVotado; }
    public boolean isVotoSkip() { return votoSkip; }
    public Jugador getVotoJugador() { return votoJugador; }


    // ==========================================
    // LÓGICA PRINCIPAL DEL JUGADOR (EL BUCLE)
    // ==========================================
    
    /**
     * Método actualizar()
     * Se ejecuta continuamente (ej. 60 veces por segundo) desde BucleJuego.
     * Aquí se procesa el movimiento y las acciones especiales (Paralizar, Alcantarillas).
     */
    public void actualizar() {
        // Reducimos los temporizadores frame a frame
        if (cooldownAsesinato > 0) cooldownAsesinato--;
        if (cooldownVentilacion > 0) cooldownVentilacion--;
        if (cooldownReporte > 0) cooldownReporte--;

        // REQUISITO: "Los jugadores inhabilitados se quedan en el lugar... No pueden continuar el juego."
        if (!vivo) return; // Si está muerto/paralizado, la función termina aquí. No se mueve ni hace acciones.

        // --- 1. PROCESAR ACCIONES DEL IMPOSTOR ---
        if (this.impostor) {
            
            // REQUISITO: "Los impostores al tener el contacto con el oponente lo paralizan (inhabilitan)"
            // Si el impostor presiona la tecla de matar (Q) y no está en cooldown
            if (ManejadorEntrada.accionMatar && cooldownAsesinato <= 0) {
                intentarParalizar();
            }
            
            // REQUISITO: "Vías de acceso rápido que conecten un espacio con otro"
            // Si el impostor presiona la tecla de ventilar (E) y no está en cooldown
            if (ManejadorEntrada.accionVentilar && cooldownVentilacion <= 0) {
                intentarUsarAlcantarilla();
            }
        }
        
        // --- 2. PROCESAR REPORTE (Todos pueden reportar) ---
        if (ManejadorEntrada.accionReportar && cooldownReporte <= 0) {
            intentarReportar();
        }

        // --- 3. PROCESAR MOVIMIENTO ---
        int dx = 0; // Dirección horizontal deseada
        int dy = 0; // Dirección vertical deseada

        // Verificamos qué teclas están presionadas (desde ManejadorEntrada)
        if (ManejadorEntrada.arriba) dy -= 1;    // Hacia arriba (Y disminuye)
        if (ManejadorEntrada.abajo) dy += 1;     // Hacia abajo (Y aumenta)
        if (ManejadorEntrada.izquierda) dx -= 1; // Hacia la izquierda (X disminuye)
        if (ManejadorEntrada.derecha) dx += 1;   // Hacia la derecha (X aumenta)

        // Si realmente estamos intentando movernos hacia algún lado
        if (dx != 0 || dy != 0) {
            
            // NORMALIZACIÓN DE VELOCIDAD
            // Matemáticas: Si vas en diagonal (ej. Arriba y Derecha a la vez), formas un triángulo rectángulo.
            // La hipotenusa es más larga que los lados, por lo que te moverías más rápido en diagonal.
            // Para evitar esto, dividimos por la magnitud (la longitud total).
            double magnitud = Math.sqrt(dx * dx + dy * dy); 
            
            // Calculamos cuánto avanzar en X y en Y para mantener una velocidad constante (4 píxeles)
            double velocidadX = (dx / magnitud) * velocidad;
            double velocidadY = (dy / magnitud) * velocidad;

            // Llamamos a mover() de la clase padre Personaje, que se encarga de las colisiones con las paredes
            super.mover((int)Math.round(velocidadX), (int)Math.round(velocidadY));
            
            // Si nos movimos, quizás necesitemos enviarlo por red a los demás
            enviarPosicionSiCambio();
        }
    }
    
    // ==========================================
    // MÉTODOS DE MECÁNICAS (PASO 2 Y 3)
    // ==========================================

    /**
     * PASO 10 (Dinámica): Reportar un cuerpo.
     * Revisa si hay algún tripulante muerto cerca para iniciar una votación.
     */
    private void intentarReportar() {
        // Obtenemos la lista de todos los jugadores
        List<Jugador> todosLosJugadores = EstadoJuego.getInstancia().getJugadores();
        
        // Rango para reportar (un poco más grande que el de matar)
        int rangoReporte = 80; 
        
        for (Jugador victima : todosLosJugadores) {
            // Buscamos a alguien que esté MUERTO (!isVivo())
            if (victima != this && !victima.isVivo()) {
                
                // Calculamos distancia
                int distanciaX = this.x - victima.getX();
                int distanciaY = this.y - victima.getY();
                double distanciaReal = Math.sqrt((distanciaX * distanciaX) + (distanciaY * distanciaY));
                
                // Si estamos cerca del cadáver...
                if (distanciaReal <= rangoReporte) {
                    System.out.println(this.nombre + " ha reportado el cuerpo de " + victima.getNombre() + "!");
                    
                    // Iniciamos la fase de votación para todos
                    EstadoJuego.getInstancia().setFaseActual(EstadoJuego.Fase.VOTACION);
                    
                    // Reseteamos los votos de todos para la nueva reunión
                    for (Jugador j : todosLosJugadores) {
                        j.resetVoto();
                    }
                    if (EstadoJuego.getInstancia().getJugadorLocal() != null) {
                        EstadoJuego.getInstancia().getJugadorLocal().resetVoto();
                    }
                    
                    // Evitamos que se reporte mil veces seguidas
                    this.cooldownReporte = 300; // 5 segundos
                    break;
                }
            }
        }
    }

    /**
     * PASO 2: Mecánica de Paralizar/Inhabilitar al oponente.
     * Revisa si hay algún tripulante vivo y muy cerca ("al tener contacto") para inhabilitarlo.
     */
    private void intentarParalizar() {
        // Obtenemos la lista global de jugadores de esta partida
        List<Jugador> todosLosJugadores = EstadoJuego.getInstancia().getJugadores();
        
        // Rango de "contacto". Es la distancia máxima en píxeles para lograr paralizar.
        int rangoContacto = 50; 
        
        // Recorremos a todos los jugadores para buscar una víctima
        for (Jugador victima : todosLosJugadores) {
            
            // No nos podemos matar a nosotros mismos, ni a otro impostor, ni a alguien ya muerto
            if (victima != this && !victima.isImpostor() && victima.isVivo()) {
                
                // Calculamos la distancia usando el Teorema de Pitágoras
                int distanciaX = this.x - victima.getX();
                int distanciaY = this.y - victima.getY();
                double distanciaReal = Math.sqrt((distanciaX * distanciaX) + (distanciaY * distanciaY));
                
                // Si la distancia es menor al rango de contacto...
                if (distanciaReal <= rangoContacto) {
                    
                    // ¡ZAZ! Lo paralizamos. Cambiamos su estado a inhabilitado.
                    victima.setVivo(false); 
                    
                    System.out.println("El impostor " + this.nombre + " ha paralizado a " + victima.getNombre());
                    
                    // Iniciamos el cooldown para que no pueda matar a todos de golpe (ej. 10 segundos a 60 fps = 600 frames)
                    this.cooldownAsesinato = 600; 
                    
                    // Solo matamos a uno por vez, rompemos el ciclo
                    break;
                }
            }
        }
    }
    
    /**
     * PASO 3: Mecánica de Vías de Acceso Rápido (Alcantarillas/Vents).
     * Revisa si el impostor está parado sobre una alcantarilla, y si es así, 
     * lo teletransporta instantáneamente a la siguiente alcantarilla.
     */
    private void intentarUsarAlcantarilla() {
        // Obtenemos el mapa actual
        Mapa mapaActual = EstadoJuego.getInstancia().getMapa();
        if (mapaActual == null) return; // Seguridad
        
        // Obtenemos la lista de vías de acceso creadas en el Mapa
        List<Rectangle> vias = mapaActual.getAlcantarillas();
        
        // Recorremos las alcantarillas para ver en cuál estamos parados
        for (int i = 0; i < vias.size(); i++) {
            Rectangle alcantarilla = vias.get(i);
            
            // Si la Hitbox del jugador intersecta (toca) el rectángulo de la alcantarilla
            if (this.hitbox.intersects(alcantarilla)) {
                
                // Calculamos el índice de la SIGUIENTE alcantarilla en la lista
                // El operador '%' (módulo) hace que si estamos en la última, vuelva a la primera (ciclo)
                int indiceSiguiente = (i + 1) % vias.size();
                Rectangle destino = vias.get(indiceSiguiente);
                
                // Teletransportamos al jugador directamente a las coordenadas del destino
                // Le sumamos 10 píxeles para que quede justo en el centro del cuadro
                this.setX(destino.x + 10);
                this.setY(destino.y + 10);
                
                System.out.println(this.nombre + " se ha trasladado rápidamente por los conductos.");
                
                // Ponemos un cooldown para evitar que pase por todas a la velocidad de la luz si deja apretado
                this.cooldownVentilacion = 60; // 1 segundo (a 60 fps)
                
                // Terminamos el método, ya se movió
                break;
            }
        }
    }

    // ==========================================
    // MÉTODOS DE RED Y GETTERS/SETTERS
    // ==========================================

    /**
     * Se encarga de controlar cuándo la posición cambia lo suficiente para ser notificada al servidor de red.
     */
    private void enviarPosicionSiCambio() {
        // Si la posición ha cambiado respecto a la última vez que avisamos...
        if (this.x != ultimoXEnviado || this.y != ultimoYEnviado) {
            // Si tenemos un cliente de red conectado, le mandamos nuestras nuevas coordenadas
            if (clienteRed != null) {
                clienteRed.enviarMensaje("MOVER:" + this.x + "," + this.y);
            }
            ultimoXEnviado = this.x;
            ultimoYEnviado = this.y;
        }
    }

    // Getters y Setters básicos necesarios para la Vista (dibujar) y Lógica externa
    public Color getColor() { return color; }
    public String getNombre() { return nombre; }
    public boolean isImpostor() { return impostor; }
    
    // Métodos para verificar y modificar el estado del jugador (Vivo o Paralizado)
    public boolean isVivo() { return vivo; }
    public void setVivo(boolean vivo) { this.vivo = vivo; }
}
