package com.amongus.project.controlador;

import com.amongus.project.modelo.EstadoJuego;
import com.amongus.project.modelo.Jugador;
import com.amongus.project.vista.PanelJuego;
import com.amongus.project.vista.PantallaFinJuego;
import com.amongus.project.data.GestorDatos;
import com.amongus.project.red.Cliente; // Importamos el cliente

import javax.swing.SwingUtilities;
import java.util.List;

public class BucleJuego implements Runnable, Cliente.MensajeListener {
    
    private PanelJuego panelJuego;
    private EstadoJuego estado;
    private boolean corriendo = false;
    private Thread hiloJuego;
    private Cliente clienteRed;
    
    // Tareas
    private int tripulantesConTareasListas = 0;
    
    // FPS objetivo
    private final int FPS = 60;
    // Tiempo objetivo por frame en nanosegundos
    private final long TIEMPO_OBJETIVO = 1000000000 / FPS;

    // Constructor original (Para pruebas directas sin red)
    public BucleJuego(PanelJuego panelJuego) {
        this.panelJuego = panelJuego;
        this.estado = EstadoJuego.getInstancia();
    }
    
    // Constructor con red (Para cuando jugamos Online o Host)
    public BucleJuego(PanelJuego panelJuego, Cliente cliente) {
        this.panelJuego = panelJuego;
        this.estado = EstadoJuego.getInstancia();
        this.clienteRed = cliente;
        
        // Nos suscribimos para escuchar los mensajes en vivo durante la partida
        if (this.clienteRed != null) {
            this.clienteRed.setMensajeListener(this);
        }
    }
    
    // =====================================
    // MANEJO DE RED EN TIEMPO REAL
    // =====================================
    @Override
    public void onMensajeRecibido(String mensaje) {
        // 1. SINCRONIZAR MOVIMIENTO: Si alguien caminó, actualizamos su posición
        if (mensaje.startsWith("MOVER:")) {
            // El formato es MOVER:Nombre,x,y
            try {
                String[] partes = mensaje.substring(6).split(",");
                String nombre = partes[0];
                int nuevoX = Integer.parseInt(partes[1]);
                int nuevoY = Integer.parseInt(partes[2]);
                
                // Buscamos a ese jugador en nuestro mundo y le actualizamos la posición
                for (Jugador j : estado.getJugadores()) {
                    if (j.getNombre().equals(nombre)) {
                        j.setX(nuevoX);
                        j.setY(nuevoY);
                        break;
                    }
                }
            } catch (Exception e) {
                System.err.println("Error procesando paquete de movimiento: " + mensaje);
            }
        } 
        // 2. SINCRONIZAR ASESINATOS: Alguien mató a otro jugador
        else if (mensaje.startsWith("MATAR:")) {
            String victima = mensaje.substring(6);
            for (Jugador j : estado.getJugadores()) {
                if (j.getNombre().equals(victima)) {
                    j.setVivo(false);
                    System.out.println("Sincronizando: " + victima + " ha muerto.");
                    break;
                }
            }
        }
        // 3. SINCRONIZAR REPORTES: Alguien reportó un cadáver
        else if (mensaje.equals("REPORTAR:")) {
            System.out.println("Sincronizando: ¡Reporte de cuerpo!");
            estado.setFaseActual(EstadoJuego.Fase.VOTACION);
            
            // Reseteamos la pantalla de votación para todos
            if (panelJuego != null && panelJuego.getPantallaVotacion() != null) {
                panelJuego.getPantallaVotacion().reiniciarVotacion();
            }
            
            for (Jugador j : estado.getJugadores()) {
                j.resetVoto();
            }
            if (estado.getJugadorLocal() != null) {
                estado.getJugadorLocal().resetVoto();
            }
        }
        // 4. SINCRONIZAR TAREAS: Alguien terminó sus tareas
        else if (mensaje.startsWith("TAREA_LISTA:")) {
            String quienTermino = mensaje.substring(12);
            System.out.println("Sincronizando: " + quienTermino + " completó todas sus tareas.");
            tripulantesConTareasListas++;
            
            // Contamos cuántos tripulantes (no impostores) hay en total en la partida
            int totalTripulantes = 0;
            for (Jugador j : estado.getJugadores()) {
                if (!j.isImpostor()) totalTripulantes++;
            }
            
            // Si todos los tripulantes terminaron sus tareas, ¡Ganan los buenos!
            if (tripulantesConTareasListas >= totalTripulantes && totalTripulantes > 0) {
                if (clienteRed != null && estado.getJugadorLocal() != null && !estado.getJugadorLocal().isImpostor()) {
                    clienteRed.enviarMensaje("COMANDO:GANAR_TRIPULANTES");
                }
            }
        }
        // 5. SINCRONIZAR FIN DE JUEGO: Si el servidor decreta el fin
        else if (mensaje.startsWith("FIN:")) {
            String ganador = mensaje.substring(4);
            finalizarJuego("¡Ganan los " + ganador + "!");
        }
        // 6. SINCRONIZAR SABOTAJE DE LUCES
        else if (mensaje.equals("SABOTAJE:LUCES:ON")) {
            EstadoJuego.getInstancia().setLucesSaboteadas(true);
            System.out.println("¡LUCES SABOTEADAS!");
        }
        else if (mensaje.equals("SABOTAJE:LUCES:OFF")) {
            EstadoJuego.getInstancia().setLucesSaboteadas(false);
            System.out.println("Luces reparadas.");
        }
    }
    
    public void iniciar() {
        if (corriendo) return;
        corriendo = true;
        hiloJuego = new Thread(this);
        hiloJuego.start();
    }
    
    public void detener() {
        corriendo = false;
        try {
            if (hiloJuego != null) hiloJuego.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        long tiempoInicial;
        long tiempoTranscurrido;
        long tiempoEspera;

        while (corriendo) {
            tiempoInicial = System.nanoTime();

            // 1. ACTUALIZAR (Lógica)
            actualizar();

            // 2. RENDERIZAR (Dibujo)
            renderizar();

            // 3. CONTROL DE TIEMPO (Sleep para mantener 60 FPS estables)
            tiempoTranscurrido = System.nanoTime() - tiempoInicial;
            tiempoEspera = TIEMPO_OBJETIVO - tiempoTranscurrido;

            if (tiempoEspera > 0) {
                try {
                    // Convertir nanosegundos a milisegundos
                    Thread.sleep(tiempoEspera / 1000000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    private void actualizar() {
        EstadoJuego.Fase fase = estado.getFaseActual();
        
        if (fase == EstadoJuego.Fase.VOTACION) {
            panelJuego.getPantallaVotacion().actualizar();
        } else if (fase == EstadoJuego.Fase.JUGANDO) {
             // Obtener el jugador local y actualizar su movimiento basado en teclas
            Jugador jugadorLocal = estado.getJugadorLocal();
            if (jugadorLocal != null) {
                jugadorLocal.actualizar();
            }
            
            // PASO 3: Verificar si la partida ya terminó
            verificarCondicionesVictoria();
        }
    }
    
    /**
     * PASO 3: Revisa constantemente si algún bando cumplió su objetivo para ganar.
     */
    private void verificarCondicionesVictoria() {
        List<Jugador> jugadores = estado.getJugadores();
        if (jugadores.isEmpty()) return; // Previene errores si no hay nadie
        
        int impostoresVivos = 0;
        int tripulantesVivos = 0;
        
        // Contamos cuántos quedan de cada bando
        for (Jugador j : jugadores) {
            if (j.isVivo()) {
                if (j.isImpostor()) impostoresVivos++;
                else tripulantesVivos++;
            }
        }
        
        // CONDICIÓN ESPECIAL PARA PRUEBAS: Si solo hay 2 o menos jugadores, no terminamos 
        // el juego automáticamente para que el desarrollador pueda probar el movimiento.
        if (jugadores.size() <= 2) {
            return; 
        }

        // CONDICIÓN 1: Ganan los Impostores si igualan o superan en número a los tripulantes
        if (impostoresVivos >= tripulantesVivos && tripulantesVivos > 0) {
            finalizarJuego("¡Ganan los Impostores!");
        } 
        // CONDICIÓN 2: Ganan los Tripulantes si expulsan a todos los impostores
        else if (impostoresVivos == 0 && tripulantesVivos > 0) {
            finalizarJuego("¡Ganan los Tripulantes!");
        }
        // (La condición de ganar por completar todas las misiones irá en el Paso 1)
    }

    /**
     * Maneja el fin del juego, guardando datos y mostrando la pantalla final.
     */
    private void finalizarJuego(String mensajeGanador) {
        // Detenemos el juego cambiando la fase
        estado.setFaseActual(EstadoJuego.Fase.FINALIZADO);
        
        // ¡IMPORTANTE! No usamos this.detener() aquí porque causaría un Deadlock.
        // Como este método es llamado por el propio hilo del juego, si le pedimos 
        // al hilo que se espere a sí mismo (hiloJuego.join()), se congela para siempre.
        // Simplemente ponemos la bandera en false para que el bucle while(corriendo) termine natural.
        corriendo = false; 
        
        // REQUISITO: Guardar en XML
        // Determinamos quién ganó basados en el mensaje
        String equipo = mensajeGanador.contains("Impostores") ? "Impostores" : "Tripulantes";
        GestorDatos.guardarPartida(equipo, estado.getJugadores().size()); // Se guarda en el historial
        
        // Mostramos la pantalla final. 
        // Se usa invokeLater porque Swing exige que las ventanas se abran en su propio hilo (EDT).
        SwingUtilities.invokeLater(() -> {
            PantallaFinJuego dialog = new PantallaFinJuego(null, mensajeGanador);
            dialog.setVisible(true);
            
            // Al darle "Aceptar", cerramos todo (o se podría volver al menú)
            System.exit(0);
        });
    }
    
    private void renderizar() {
        if (panelJuego != null) {
            panelJuego.repaint();
            // Toolkit.getDefaultToolkit().sync(); // A veces ayuda en Linux con el tearing
        }
    }
}