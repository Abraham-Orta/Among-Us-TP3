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
    private int framesRevelacion = 0; // contador para la pantalla inicial
    private boolean finalizando = false; // Flag para evitar múltiples llamadas al terminar el juego

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

                // ¡SOLUCIÓN! Si el mensaje de movimiento es sobre MÍ MISMO, lo ignoro.
                // Yo ya sé dónde estoy porque manejo mi propio teclado.
                if (estado.getJugadorLocal() != null && nombre.equals(estado.getJugadorLocal().getNombre())) {
                    return; 
                }

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
                
                // SINCRONIZACIÓN DE CADÁVER: Extraemos coordenadas X,Y si vienen en el mensaje
                int bodyX = p.length >= 4 ? Integer.parseInt(p[2]) : -1;
                int bodyY = p.length >= 4 ? Integer.parseInt(p[3]) : -1;
                
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
                    if (j.getNombre().equals(victima)) { 
                        j.setVivo(false); 
                        // FIJAMOS EL CUERPO: Si el mensaje traía posición, la aplicamos para que todos lo vean igual
                        if (bodyX != -1) {
                            j.setXMuerte(bodyX);
                            j.setYMuerte(bodyY);
                        }
                    }
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
                                j.setVivo(false, true); // true = fue expulsado
                                break;
                            }
                        }
                        // También verificar si el local fue el expulsado
                        Jugador loc = estado.getJugadorLocal();
                        if (loc != null && loc.getNombre().equals(expulsado)) {
                            jExpulsado = loc;
                            loc.setVivo(false, true);
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
        } else if (mensaje.startsWith("REVELAR_IMPOSTORES:")) {
            String listaStr = mensaje.substring(19);
            if (!listaStr.isEmpty()) {
                String[] impostores = listaStr.split(",");
                for (String imp : impostores) {
                    for (Jugador j : estado.getJugadores()) {
                        if (j.getNombre().equals(imp.trim())) {
                            j.setImpostor(true);
                        }
                    }
                    if (estado.getJugadorLocal() != null && estado.getJugadorLocal().getNombre().equals(imp.trim())) {
                        estado.getJugadorLocal().setImpostor(true);
                    }
                }
            }
        } else if (mensaje.startsWith("FIN:")) {
            finalizarJuego("Ganan los " + mensaje.substring(4));
        } else if (mensaje.equals("SABOTAJE:LUCES:ON")) {
            estado.setLucesSaboteadas(true);
        } else if (mensaje.startsWith("SABOTAJE:LUCES:OFF")) {
            estado.setLucesSaboteadas(false);
        } else if (mensaje.startsWith("SOMBRERO:")) {
            try {
                String[] p = mensaje.split(":");
                String nombre = p[1];
                String idSom = p[2];
                for (Jugador j : estado.getJugadores()) {
                    if (j.getNombre().equals(nombre)) {
                        j.setSombrero(idSom);
                        break;
                    }
                }
            } catch (Exception e) {}
        }
        }

    public void iniciar() {
        if (corriendo) return;
        this.framesRevelacion = 0; // resetear al iniciar
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
        long ultimoTiempo = System.nanoTime();
        while (corriendo) {
            long ahora = System.nanoTime();
            // Delta Time: segundos que han pasado desde el frame anterior
            double delta = (ahora - ultimoTiempo) / 1_000_000_000.0;
            ultimoTiempo = ahora;

            actualizar(delta);
            renderizar();

            long tiempoProcesado = System.nanoTime() - ahora;
            long espera = TIEMPO_OBJETIVO - tiempoProcesado;
            if (espera > 0) {
                try { Thread.sleep(espera / 1_000_000); }
                catch (InterruptedException e) { e.printStackTrace(); }
            }
        }
    }

    private void actualizar(double delta) {
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

        } else if (fase == EstadoJuego.Fase.REVELACION) {
            // sumamos frames hasta llegar a 5 segundos (60 fps * 5 seg = 300)
            framesRevelacion++;
            if (framesRevelacion >= 300) {
                estado.setFaseActual(EstadoJuego.Fase.JUGANDO);
                framesRevelacion = 0;
            }

        } else if (fase == EstadoJuego.Fase.JUGANDO) {

            // Actualizar físicas y teclado del jugador local con Delta Time
            Jugador jugadorLocal = estado.getJugadorLocal();
            if (jugadorLocal != null) {
                jugadorLocal.actualizar(panelJuego.getManejadorEntrada(), delta);
            }

            // Actualizar interpolación visual de TODOS los jugadores
            for (Jugador j : estado.getJugadores()) {
                j.actualizarInterpolacion();
            }

            // Solo verificar victoria localmente si NO hay red.
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
            finalizarJuego("Ganan los Impostores");
        else if (impostoresVivos == 0 && tripulantesVivos > 0)
            finalizarJuego("Ganan los Tripulantes");
    }

    private void finalizarJuego(String mensajeGanador) {
        if (finalizando) return; // evitar múltiples llamadas
        finalizando = true;
        
        boolean estabaEnVotacion = (estado.getFaseActual() == EstadoJuego.Fase.VOTACION);

        // Hilo paralelo para manejar la transición
        new Thread(() -> {
            // Solo esperamos 3 segundos si estábamos en la fase de JUGANDO (para ver la cinemática).
            // Si estábamos en VOTACION, no hay cinemática, así que saltamos directo.
            if (!estabaEnVotacion) {
                try {
                    Thread.sleep(3000); // 3 segundos de espera (la cinemática dura ~2.8s)
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            
            // NO ponemos "corriendo = false" para que el ciclo de renderizado siga pintando la victoria
            estado.setMensajeGanador(mensajeGanador);
            estado.setFaseActual(EstadoJuego.Fase.FINALIZADO);
            
            // Nota: Se removió la duplicación de guardado de XML aquí, 
            // ya que el Servidor.java ya lo está guardando para todos.
        }).start();
    }

    private void renderizar() {
        if (panelJuego != null) panelJuego.repaint();
    }
}