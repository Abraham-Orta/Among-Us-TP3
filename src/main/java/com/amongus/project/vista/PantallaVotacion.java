package com.amongus.project.vista; // Paquete de la interfaz de usuario

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amongus.project.controlador.ManejadorEntrada;
import com.amongus.project.modelo.EstadoJuego;
import com.amongus.project.modelo.Jugador;

/**
 * PantallaVotacion
 * ================
 * Muestra la interfaz para que los jugadores voten por quién creen que es el impostor.
 * Cuenta los votos y elimina al jugador con más votos al terminar el tiempo.
 */
public class PantallaVotacion {

    private Rectangle botonSkip; // Botón para omitir el voto
    
    // --- NUEVAS VARIABLES PARA EL REQUERIMIENTO 4 ---
    private int temporizadorVotacion = 900; // 900 frames = 15 segundos (a 60 FPS)
    private boolean mostrandoResultados = false; // Bandera para saber si ya estamos mostrando quién perdió
    private int temporizadorResultados = 300; // 300 frames = 5 segundos mostrando el mensaje final
    private String mensajeResultado = ""; // El texto que dirá quién fue expulsado

    public PantallaVotacion() {
        botonSkip = new Rectangle(50, 500, 150, 50); // Posición y tamaño del botón Skip
    }
    
    /**
     * Resetea los temporizadores para que la votación empiece de cero la próxima vez.
     */
    public void reiniciarVotacion() {
        temporizadorVotacion = 900;
        mostrandoResultados = false;
        temporizadorResultados = 300;
        mensajeResultado = "";
    }

    /**
     * Se llama 60 veces por segundo mientras el estado del juego sea VOTACION.
     */
    public void actualizar() {
        // 1. SI ESTAMOS MOSTRANDO RESULTADOS (El veredicto final)
        if (mostrandoResultados) {
            temporizadorResultados--; // Restamos tiempo
            
            // Si pasaron los 5 segundos de mostrar resultados
            if (temporizadorResultados <= 0) {
                EstadoJuego.getInstancia().setFaseActual(EstadoJuego.Fase.JUGANDO); // Volvemos al mapa
                reiniciarVotacion(); // Limpiamos todo para el próximo reporte
            }
            return; // No hacemos nada más
        }

        // 2. SI ESTAMOS EN FASE DE VOTAR
        temporizadorVotacion--; // El tiempo se acaba
        
        List<Jugador> jugadores = EstadoJuego.getInstancia().getJugadores();
        
        // --- SIMULACIÓN DE BOTS PARA QUE NO TENGAS QUE ESPERAR SOLO ---
        // (Como estás probando localmente, haremos que los otros jugadores voten solos)
        for (Jugador j : jugadores) {
            if (j.isVivo() && !j.yaVoto() && j != EstadoJuego.getInstancia().getJugadorLocal()) {
                // Hay un 1% de probabilidad en cada frame de que un bot vote
                if (Math.random() < 0.01) { 
                    if (Math.random() < 0.5) j.votarSkip(); // A veces salta
                    else j.votarJugador(jugadores.get((int)(Math.random() * jugadores.size()))); // A veces vota a alguien al azar
                }
            }
        }

        // Verificar si TODOS los jugadores vivos ya votaron
        boolean todosVotaron = true;
        for (Jugador j : jugadores) {
            if (j.isVivo() && !j.yaVoto()) {
                todosVotaron = false;
                break;
            }
        }

        // 3. FIN DE VOTACIÓN: Si el tiempo llega a cero o todos ya votaron
        if (temporizadorVotacion <= 0 || todosVotaron) {
            contarVotos(jugadores); // Procesamos quién se va de la nave
            return;
        }

        // 4. LEER EL VOTO DEL JUGADOR REAL (Tú)
        Jugador jugadorLocal = EstadoJuego.getInstancia().getJugadorLocal();
        if (jugadorLocal == null || !jugadorLocal.isVivo() || jugadorLocal.yaVoto()) {
            return; // Si estás muerto o ya votaste, no puedes hacer clic
        }

        // Verificar clic izquierdo del ratón
        if (ManejadorEntrada.clickIzquierdo) {
            
            // Si hizo clic en el botón de SKIP
            if (botonSkip.contains(ManejadorEntrada.mouseX, ManejadorEntrada.mouseY)) {
                jugadorLocal.votarSkip();
                ManejadorEntrada.clickIzquierdo = false; // Consumimos el clic para que no se presione dos veces
                return;
            }
            
            // Si hizo clic en la tarjeta de algún jugador
            int x = 50;
            int y = 50;
            
            for (Jugador j : jugadores) {
                if (!j.isVivo()) continue; // No se puede votar a los muertos
                
                Rectangle areaJugador = new Rectangle(x, y, 200, 60);
                if (areaJugador.contains(ManejadorEntrada.mouseX, ManejadorEntrada.mouseY)) {
                    jugadorLocal.votarJugador(j); // Enviamos el voto hacia esa persona
                    ManejadorEntrada.clickIzquierdo = false;
                    return;
                }
                
                y += 70; // Espaciado entre tarjetas
                if (y > 400) { // Si pasamos el límite de abajo, creamos una nueva columna a la derecha
                    y = 50;
                    x += 250;
                }
            }
        }
    }
    
    /**
     * Cuenta los votos de todos los jugadores y decide quién es expulsado.
     */
    private void contarVotos(List<Jugador> jugadores) {
        Map<Jugador, Integer> conteo = new HashMap<>(); // Diccionario para guardar [Jugador -> Cantidad Votos]
        int votosSkip = 0; // Votos nulos
        
        // Recorrer los votos de cada jugador
        for (Jugador j : jugadores) {
            if (j.yaVoto()) {
                if (j.isVotoSkip()) {
                    votosSkip++; // Suma a los saltos
                } else if (j.getVotoJugador() != null) {
                    Jugador votado = j.getVotoJugador();
                    // Le sumamos 1 voto al que eligió
                    conteo.put(votado, conteo.getOrDefault(votado, 0) + 1); 
                }
            }
        }
        
        // Encontrar al jugador que más votos recibió
        Jugador masVotado = null;
        int maxVotos = 0;
        boolean empate = false; // Para evitar expulsar a alguien si hay un empate
        
        for (Map.Entry<Jugador, Integer> entry : conteo.entrySet()) {
            if (entry.getValue() > maxVotos) {
                maxVotos = entry.getValue();
                masVotado = entry.getKey();
                empate = false; // Rompimos el empate anterior
            } else if (entry.getValue() == maxVotos) {
                empate = true; // Alguien tiene los mismos votos que el líder
            }
        }
        
        mostrandoResultados = true; // Cambiamos la pantalla para mostrar la cinemática de expulsión
        
        // --- LÓGICA DE EXPULSIÓN Y REPORTE DE IMPOSTORES RESTANTES ---
        // Si los votos por "Skip" ganan, hay empate, o nadie votó
        if (votosSkip >= maxVotos || empate || masVotado == null) {
            mensajeResultado = "Nadie fue expulsado (Empate o Skip).";
        } else {
            // Expulsamos al más votado (lo inhabilitamos/matamos)
            masVotado.setVivo(false);
            
            // REQUERIMIENTO: Contar cuántos impostores quedan vivos
            int impostoresRestantes = 0;
            for (Jugador j : jugadores) {
                if (j.isVivo() && j.isImpostor()) {
                    impostoresRestantes++;
                }
            }
            
            // Construimos el mensaje detallado
            if (masVotado.isImpostor()) {
                mensajeResultado = masVotado.getNombre() + " era un Impostor. Quedan " + impostoresRestantes + " impostores.";
            } else {
                mensajeResultado = masVotado.getNombre() + " no era un Impostor. Quedan " + impostoresRestantes + " impostores.";
            }
        }
    }

    /**
     * Dibuja toda la interfaz de votación en la pantalla
     */
    public void render(Graphics g) {
        // Fondo oscuro semitransparente sobre el mapa
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, 800, 600);
        
        // --- PANTALLA DE RESULTADOS (CUANDO YA SE VOTÓ) ---
        if (mostrandoResultados) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 26));
            FontMetrics fm = g.getFontMetrics();
            // Matemática para centrar el texto en el medio de la pantalla
            int xCentro = (800 - fm.stringWidth(mensajeResultado)) / 2;
            g.drawString(mensajeResultado, xCentro, 300);
            return; // Salimos, no dibujamos botones
        }
        
        // --- PANTALLA NORMAL DE VOTAR ---
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        // Mostramos el tiempo restante convirtiendo los frames a segundos (/ 60)
        g.drawString("¿Quién es el Impostor? Tiempo: " + (temporizadorVotacion / 60), 220, 30);
        
        List<Jugador> jugadores = EstadoJuego.getInstancia().getJugadores();
        int x = 50;
        int y = 50;

        // Dibuja la tarjeta de cada jugador
        for (Jugador j : jugadores) {
            
            // Fondo de la tarjeta (Gris si está vivo, Rojo si está muerto)
            if (j.isVivo()) {
                g.setColor(new Color(70, 70, 80)); 
            } else {
                g.setColor(new Color(150, 0, 0)); // Rojo muerto
            }
            g.fillRect(x, y, 200, 60);
            
            // Borde blanco si pasas el ratón por encima (Efecto Hover)
            Rectangle rect = new Rectangle(x, y, 200, 60);
            if (rect.contains(ManejadorEntrada.mouseX, ManejadorEntrada.mouseY) && j.isVivo()) {
                g.setColor(Color.WHITE);
                g.drawRect(x, y, 200, 60);
            }
            
            // Icono del jugador (cuadrado de su color)
            g.setColor(j.getColor());
            g.fillRect(x + 10, y + 10, 40, 40);
            
            // Nombre del jugador
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 16));
            g.drawString(j.getNombre(), x + 60, y + 35);
            
            // Marcar si ya votó (Punto verde)
            if (j.yaVoto()) {
                g.setColor(Color.GREEN);
                g.fillOval(x + 180, y + 20, 10, 10);
                g.setFont(new Font("Arial", Font.PLAIN, 10));
                g.drawString("Votó", x + 160, y + 15);
            } else if (!j.isVivo()) {
                // Etiqueta para muertos
                g.setColor(Color.BLACK);
                g.drawString("Muerto", x + 140, y + 35);
            }
            
            y += 70; // Siguiente fila
            if (y > 400) { // Siguiente columna
                y = 50;
                x += 250;
            }
        }
        
        // --- BOTÓN DE SKIP VOTE ---
        // Color diferente si pasamos el mouse
        if (botonSkip.contains(ManejadorEntrada.mouseX, ManejadorEntrada.mouseY)) {
            g.setColor(Color.WHITE);
        } else {
            g.setColor(Color.LIGHT_GRAY);
        }
        g.fillRect(botonSkip.x, botonSkip.y, botonSkip.width, botonSkip.height);
        
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("SKIP VOTE", botonSkip.x + 35, botonSkip.y + 30);
        
        // --- MENSAJE INFERIOR (ESTADO LOCAL) ---
        Jugador local = EstadoJuego.getInstancia().getJugadorLocal();
        if (local != null) {
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.ITALIC, 16));
            if (!local.isVivo()) {
                g.drawString("Estás muerto. No puedes votar.", 300, 550);
            } else if (local.yaVoto()) {
                g.drawString("Has votado. Esperando a los demás...", 280, 550);
            } else {
                g.drawString("Haz clic en un jugador o Skip para votar.", 250, 550);
            }
        }
    }
}