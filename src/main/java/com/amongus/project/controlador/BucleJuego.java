package com.amongus.project.controlador;

import com.amongus.project.modelo.EstadoJuego;
import com.amongus.project.modelo.Jugador;
import com.amongus.project.vista.PanelJuego;
import com.amongus.project.vista.PantallaFinJuego;
import com.amongus.project.data.GestorDatos;
import com.amongus.project.red.Cliente;

import javax.swing.SwingUtilities;
import java.util.List;

public class BucleJuego implements Runnable, Cliente.MensajeListener {

    private PanelJuego  panelJuego;
    private EstadoJuego estado;
    private boolean     corriendo = false;
    private Thread      hiloJuego;
    private Cliente     clienteRed;

    private int tripulantesConTareasListas = 0;

    private final int  FPS            = 60;
    private final long TIEMPO_OBJETIVO = 1_000_000_000L / FPS;

    /** Constructor para pruebas locales sin red */
    public BucleJuego(PanelJuego panelJuego) {
        this.panelJuego = panelJuego;
        this.estado     = panelJuego.getEstadoJuego();
    }

    /** Constructor para juego en red */
    public BucleJuego(PanelJuego panelJuego, Cliente cliente) {
        this.panelJuego = panelJuego;
        this.estado     = panelJuego.getEstadoJuego();
        this.clienteRed = cliente;
        if (this.clienteRed != null)
            this.clienteRed.setMensajeListener(this);
    }

    // =====================================
    //  MENSAJES DE RED
    // =====================================
    @Override
    public void onMensajeRecibido(String mensaje) {
        if (mensaje.startsWith("MOVER:")) {
            try {
                String[] p = mensaje.substring(6).split(",");
                String nombre = p[0];
                int nx = Integer.parseInt(p[1]);
                int ny = Integer.parseInt(p[2]);
                for (Jugador j : estado.getJugadores()) {
                    if (j.getNombre().equals(nombre)) { j.recibirPosicionRed(nx, ny); break; }
                }
            } catch (Exception e) {
                System.err.println("Error procesando MOVER: " + mensaje);
            }
        } else if (mensaje.startsWith("MATAR:")) {
            try {
                String[] p = mensaje.substring(6).split(",");
                String atacante = p[0];
                String victima = p[1];
                
                // verificamos si el jugador de esta ventana es el atacante o la victima
                Jugador local = estado.getJugadorLocal();
                boolean involucrado = (local != null && (local.getNombre().equals(atacante) || local.getNombre().equals(victima)));
                
                if (involucrado) {
                    // musica de fondo para la cinematica con el volumen bajito (-10 decibelios)
                    com.amongus.project.vista.ReproductorMusica.reproducirEfectoConVolumen("impostor_killMusic.wav", -10.0f);
                    
                    // sonido de ataque "kill.wav" sincronizado con los frames (esperamos 480 milisegundos)
                    new Thread(() -> {
                        try {
                            Thread.sleep(480);
                            com.amongus.project.vista.ReproductorMusica.reproducirEfecto("Kill.wav");
                        } catch (InterruptedException e) {}
                    }).start();
                }

                for (Jugador j : estado.getJugadores()) {
                    if (j.getNombre().equals(victima)) { j.setVivo(false); }
                    if (j.getNombre().equals(atacante)) { j.iniciarAnimacionAtaque(); }
                }
            } catch (Exception e) {
                System.err.println("Error procesando MATAR: " + mensaje);
            }
        } else if (mensaje.startsWith("ANIMACION_MATAR:")) {
            // ya no se usa, la animación se procesa con matar
        } else if (mensaje.startsWith("CHAT:")) {
            // Reenviamos el chat a la pantalla de votación (si existe)
            if (estado.getFaseActual() == EstadoJuego.Fase.VOTACION && panelJuego.getPantallaVotacion() != null) {
                panelJuego.getPantallaVotacion().recibirMensajeChat(mensaje.substring(5));
            }
        } else if (mensaje.startsWith("REPORTAR:")) {
            estado.setFaseActual(EstadoJuego.Fase.VOTACION);
            if (panelJuego.getPantallaVotacion() != null)
                panelJuego.getPantallaVotacion().reiniciarVotacion();
            for (Jugador j : estado.getJugadores()) j.resetVoto();
            if (estado.getJugadorLocal() != null) estado.getJugadorLocal().resetVoto();
        } else if (mensaje.startsWith("VOTO:")) {
            // procesar votos remotos
            try {
                String[] partes = mensaje.substring(5).split(",");
                String votante = partes[0];
                String votado = partes[1];
                for (Jugador j : estado.getJugadores()) {
                    if (j.getNombre().equals(votante)) {
                        if (votado.equals("SKIP")) {
                            j.recibirVotoRemotoSkip();
                        } else {
                            for (Jugador obj : estado.getJugadores()) {
                                if (obj.getNombre().equals(votado)) {
                                    j.recibirVotoRemotoJugador(obj);
                                    break;
                                }
                            }
                        }
                        break;
                    }
                }
            } catch (Exception e) {}
        } else if (mensaje.startsWith("RESULTADO_VOTACION:")) {
            try {
                String[] partes = mensaje.split(":", 3);
                String expulsado = partes[1];
                String msjResultado = partes[2];
                if (panelJuego.getPantallaVotacion() != null) {
                    Jugador jExpulsado = null;
                    if (!expulsado.equals("NADIE")) {
                        for (Jugador j : estado.getJugadores()) {
                            if (j.getNombre().equals(expulsado)) {
                                jExpulsado = j;
                                j.setVivo(false);
                                break;
                            }
                        }
                    }
                    panelJuego.getPantallaVotacion().mostrarResultadosVotacion(msjResultado, jExpulsado);
                }
            } catch (Exception e) {
                System.err.println("Error procesando RESULTADO_VOTACION: " + mensaje);
            }
        } else if (mensaje.startsWith("TAREA_LISTA:")) {
            tripulantesConTareasListas++;
            int total = 0;
            for (Jugador j : estado.getJugadores()) { if (!j.isImpostor()) total++; }
            if (tripulantesConTareasListas >= total && total > 0) {
                if (clienteRed != null && estado.getJugadorLocal() != null
                        && !estado.getJugadorLocal().isImpostor())
                    clienteRed.enviarMensaje("COMANDO:GANAR_TRIPULANTES");
            }
        } else if (mensaje.startsWith("FIN:")) {
            finalizarJuego("¡Ganan los " + mensaje.substring(4) + "!");
        } else if (mensaje.equals("SABOTAJE:LUCES:ON")) {
            estado.setLucesSaboteadas(true);
        } else if (mensaje.equals("SABOTAJE:LUCES:OFF")) {
            estado.setLucesSaboteadas(false);
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
        try { if (hiloJuego != null) hiloJuego.join(); }
        catch (InterruptedException e) { e.printStackTrace(); }
    }

    @Override
    public void run() {
        while (corriendo) {
            long inicio = System.nanoTime();
            actualizar();
            renderizar();
            long espera = TIEMPO_OBJETIVO - (System.nanoTime() - inicio);
            if (espera > 0) {
                try { Thread.sleep(espera / 1_000_000); }
                catch (InterruptedException e) { e.printStackTrace(); }
            }
        }
    }

    private void actualizar() {
        EstadoJuego.Fase fase = estado.getFaseActual();

        if (fase == EstadoJuego.Fase.VOTACION) {
            panelJuego.getPantallaVotacion().actualizar();

            // Disparar mensaje de chat si aplico enter
            ManejadorEntrada m = panelJuego.getManejadorEntrada();
            if (m.presionoEnterChat) {
                m.presionoEnterChat = false;
                if (clienteRed != null) {
                    clienteRed.enviarMensaje("CHAT:" + m.entradaChat.toString());
                }
                m.entradaChat.setLength(0); // limpiar
            }

        } else if (fase == EstadoJuego.Fase.JUGANDO) {

            // Actualizar físicas y teclado del jugador local
            Jugador jugadorLocal = estado.getJugadorLocal();
            if (jugadorLocal != null) {
                // ← Pasamos el ManejadorEntrada de ESTA ventana, no uno estático
                jugadorLocal.actualizar(panelJuego.getManejadorEntrada());
            }

            // Actualizar interpolación visual de TODOS los jugadores (para evitar lag visual en red)
            for (Jugador j : estado.getJugadores()) {
                j.actualizarInterpolacion();
            }

            // Solo verificar victoria localmente si NO hay red.
            // En modo red, el servidor decide quién gana (vía FIN:).
            // Localmente no sabemos qué jugadores remotos son impostores.
            if (clienteRed == null) {
                verificarCondicionesVictoria();
            }
        }
    }

    private void verificarCondicionesVictoria() {
        List<Jugador> jugadores = estado.getJugadores();
        if (jugadores.isEmpty()) return;
        if (jugadores.size() <= 2) return; // No terminar en pruebas pequeñas

        int impostoresVivos = 0, tripulantesVivos = 0;
        for (Jugador j : jugadores) {
            if (j.isVivo()) {
                if (j.isImpostor()) impostoresVivos++;
                else                tripulantesVivos++;
            }
        }

        if (impostoresVivos >= tripulantesVivos && tripulantesVivos > 0)
            finalizarJuego("¡Ganan los Impostores!");
        else if (impostoresVivos == 0 && tripulantesVivos > 0)
            finalizarJuego("¡Ganan los Tripulantes!");
    }

    private void finalizarJuego(String mensajeGanador) {
        if (estado.getFaseActual() == EstadoJuego.Fase.FINALIZADO) return; // evitar múltiples llamadas
        estado.setFaseActual(EstadoJuego.Fase.FINALIZADO);
        
        // Hilo paralelo para esperar 3 segundos y dejar que la cinemática de muerte termine
        new Thread(() -> {
            try {
                Thread.sleep(3000); // 3 segundos de espera (la cinemática dura ~2.8s)
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            corriendo = false;
            String equipo = mensajeGanador.contains("Impostores") ? "Impostores" : "Tripulantes";
            GestorDatos.guardarPartida(equipo, estado.getJugadores().size());

            SwingUtilities.invokeLater(() -> {
                PantallaFinJuego dialog = new PantallaFinJuego(null, mensajeGanador);
                dialog.setVisible(true);
                // System.exit(0) se debe manejar desde PantallaFinJuego idealmente, pero lo mantenemos para compatibilidad
                // System.exit(0); // <-- comentado si quieres que cierre gracefully, lo dejamos por ahora si es como estaba
            });
        }).start();
    }

    private void renderizar() {
        if (panelJuego != null) SwingUtilities.invokeLater(panelJuego::repaint);
    }
}