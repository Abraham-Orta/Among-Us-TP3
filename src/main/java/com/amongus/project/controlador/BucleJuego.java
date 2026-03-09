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
            String victima = mensaje.substring(6);
            for (Jugador j : estado.getJugadores()) {
                if (j.getNombre().equals(victima)) { j.setVivo(false); break; }
            }
        } else if (mensaje.startsWith("REPORTAR:")) {
            estado.setFaseActual(EstadoJuego.Fase.VOTACION);
            if (panelJuego.getPantallaVotacion() != null)
                panelJuego.getPantallaVotacion().reiniciarVotacion();
            for (Jugador j : estado.getJugadores()) j.resetVoto();
            if (estado.getJugadorLocal() != null) estado.getJugadorLocal().resetVoto();
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
        estado.setFaseActual(EstadoJuego.Fase.FINALIZADO);
        corriendo = false;

        String equipo = mensajeGanador.contains("Impostores") ? "Impostores" : "Tripulantes";
        GestorDatos.guardarPartida(equipo, estado.getJugadores().size());

        SwingUtilities.invokeLater(() -> {
            PantallaFinJuego dialog = new PantallaFinJuego(null, mensajeGanador);
            dialog.setVisible(true);
            System.exit(0);
        });
    }

    private void renderizar() {
        if (panelJuego != null) SwingUtilities.invokeLater(panelJuego::repaint);
    }
}