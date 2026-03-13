package com.amongus.project.red;

import com.amongus.project.data.GestorDatos;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.Map;
public class Servidor {

    private static final int PUERTO        = 1234;
    private static final int MAX_JUGADORES = 10;

    // Mínimo de jugadores para iniciar.
    // En producción poner 5 (requisito del PDF).
    // En pruebas locales con PruebaDirecta usamos 3.
    private static final int MIN_JUGADORES = 3;

    public static CopyOnWriteArrayList<AtencionJugador> listaJugadores = new CopyOnWriteArrayList<>();
    public static boolean partidaIniciada = false;

    // Fix #2: Mapa de posiciones pendientes para el batch de MOVER.
    // Clave: nombre del jugador. Valor: "nombre,x,y" (la parte después de "MOVER:")
    // ConcurrentHashMap garantiza acceso thread-safe desde los hilos de AtencionJugador.
    public static final ConcurrentHashMap<String, String> pendingMoves = new ConcurrentHashMap<>();

    // Scheduler que cada 50ms agrupa pending moves y los envía en un solo mensaje BATCH_MOVER
    private static final ScheduledExecutorService batchScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "BatchMoverScheduler");
        t.setDaemon(true); // No bloquea el cierre de la JVM
        return t;
    });

    // Estado de votación
    public static boolean enVotacion = false;
    private static Map<String, String> votosActuales = new HashMap<>();

    public static void main(String[] args) {
        System.out.println("Iniciando el servidor de Among Us...");

        // Fix #2: Iniciar el scheduler de batch de posiciones (50ms = 20 actualizaciones/seg)
        batchScheduler.scheduleAtFixedRate(Servidor::enviarBatchMover, 50, 50, TimeUnit.MILLISECONDS);

        try {
            ServerSocket servidorSocket = new ServerSocket(PUERTO);
            System.out.println("Servidor listo en el puerto: " + PUERTO);

            while (true) {
                if (listaJugadores.size() >= MAX_JUGADORES) {
                    System.out.println("Sala llena, rechazando conexión...");
                    servidorSocket.accept().close();
                    continue;
                }

                System.out.println("Esperando conexión... (" + listaJugadores.size() + "/" + MAX_JUGADORES + ")");
                Socket socketDelCliente = servidorSocket.accept();
                System.out.println("¡Conexión recibida! IP: " + socketDelCliente.getInetAddress());

                AtencionJugador nuevoJugador = new AtencionJugador(socketDelCliente);
                listaJugadores.add(nuevoJugador);
                nuevoJugador.start();
            }

        } catch (IOException error) {
            System.out.println("Error en el servidor: " + error.getMessage());
            error.printStackTrace();
        }
    }

    /**
     * Fix #2: Agrupa todas las posiciones acumuladas en un único mensaje BATCH_MOVER
     * y lo envía a todos los jugadores. Se llama cada 50ms desde el batchScheduler.
     * Formato: BATCH_MOVER:nombre1,x1,y1|nombre2,x2,y2|...
     */
    private static void enviarBatchMover() {
        if (pendingMoves.isEmpty() || listaJugadores.isEmpty()) return;

        // Drenamos el mapa atómicamente: tomamos los valores y limpiamos
        StringBuilder sb = new StringBuilder("BATCH_MOVER:");
        boolean primero = true;
        for (Map.Entry<String, String> entry : pendingMoves.entrySet()) {
            if (!primero) sb.append('|');
            sb.append(entry.getValue()); // "nombre,x,y"
            primero = false;
        }
        pendingMoves.clear();

        String batch = sb.toString();
        for (AtencionJugador jugador : listaJugadores) {
            try { jugador.enviarMensaje(batch); }
            catch (Exception e) { /* jugador desconectado, ignorar */ }
        }
    }

    public static void enviarATodos(String mensaje) {
        for (AtencionJugador jugador : listaJugadores) {
            try { jugador.enviarMensaje(mensaje); }
            catch (Exception e) { System.out.println("No se pudo enviar a un jugador."); }
        }
    }

    public static void iniciarPartida(String mapaElegido) {
        System.out.println("Intentando iniciar partida con mapa: " + mapaElegido);

        // Verificar mínimo de jugadores
        if (listaJugadores.size() < MIN_JUGADORES) {
            System.out.println("Faltan jugadores (Mínimo " + MIN_JUGADORES + ")");
            enviarATodos("CHAT:SISTEMA: Faltan jugadores para iniciar (Mínimo " + MIN_JUGADORES + ").");
            return;
        }

        // --- Asignar roles al azar ---
        // Siempre hay al menos 1 impostor
        int impostor1 = (int) (Math.random() * listaJugadores.size());
        int impostor2 = -1;

        // Segundo impostor solo si hay más de 3 jugadores
        if (listaJugadores.size() > 3) {
            impostor2 = (int) (Math.random() * listaJugadores.size());
            while (impostor2 == impostor1) {
                impostor2 = (int) (Math.random() * listaJugadores.size());
            }
        }

        System.out.println("Impostores: índice " + impostor1
                + (impostor2 != -1 ? " y " + impostor2 : " (solo 1 con <= 3 jugadores)"));

        // Marcar rol en cada AtencionJugador
        for (int i = 0; i < listaJugadores.size(); i++) {
            listaJugadores.get(i).esImpostor = (i == impostor1 || i == impostor2);
            listaJugadores.get(i).estaVivo = true;
        }
        partidaIniciada = true;

        // Construir la lista de nombres de impostores separados por comas
        StringBuilder listaImpostores = new StringBuilder();
        for (AtencionJugador j : listaJugadores) {
            if (j.esImpostor && j.getNombreJugador() != null) {
                if (listaImpostores.length() > 0) listaImpostores.append(",");
                listaImpostores.append(j.getNombreJugador());
            }
        }

        // Susurrar el rol individualmente → si es impostor, incluir a sus compañeros
        for (AtencionJugador j : listaJugadores) {
            if (j.esImpostor) {
                j.enviarMensaje("ROL:IMPOSTOR:" + listaImpostores.toString());
            } else {
                j.enviarMensaje("ROL:TRIPULANTE");
            }
        }

        // Avisar a todos que la partida arranca con el mapa elegido
        enviarATodos("JUEGO_INICIADO:" + mapaElegido);
    }

    public static void finalizarPartida(String equipoGanador) {
        partidaIniciada = false;
        enVotacion = false;
        System.out.println("La partida terminó. Ganaron: " + equipoGanador);

        // Revelar impostores a todos los clientes para la pantalla de victoria
        StringBuilder listaImpostores = new StringBuilder();
        for (AtencionJugador j : listaJugadores) {
            if (j.esImpostor && j.getNombreJugador() != null) {
                if (listaImpostores.length() > 0) listaImpostores.append(",");
                listaImpostores.append(j.getNombreJugador());
            }
        }
        enviarATodos("REVELAR_IMPOSTORES:" + listaImpostores.toString());
        enviarATodos("FIN:" + equipoGanador);
        
        GestorDatos.guardarPartida(equipoGanador, listaJugadores.size());
        System.out.println("Datos guardados en el XML.");
    }

    public static void iniciarVotacion() {
        enVotacion = true;
        votosActuales.clear();
        System.out.println("Servidor: Votación iniciada.");
    }

    public static synchronized void registrarVoto(String votante, String votado) {
        if (!enVotacion) return;
        
        votosActuales.put(votante, votado);
        System.out.println("Servidor: Voto registrado de " + votante + " -> " + votado);
        
        verificarVotacionCompleta();
    }

    public static synchronized void verificarVotacionCompleta() {
        if (!enVotacion) return;
        
        int jugadoresVivos = 0;
        for (AtencionJugador j : listaJugadores) {
            if (j.estaVivo) jugadoresVivos++;
        }
        
        if (votosActuales.size() >= jugadoresVivos && jugadoresVivos > 0) {
            System.out.println("Servidor: Todos los vivos han votado. Procesando resultados...");
            procesarResultadosVotacion();
        }
    }

    private static void procesarResultadosVotacion() {
        enVotacion = false;
        
        Map<String, Integer> conteo = new HashMap<>();
        int votosSkip = 0;
        
        for (String votado : votosActuales.values()) {
            if (votado.equals("SKIP")) {
                votosSkip++;
            } else {
                conteo.put(votado, conteo.getOrDefault(votado, 0) + 1);
            }
        }
        
        String masVotado = null;
        int maxVotos = 0;
        boolean empate = false;
        
        for (Map.Entry<String, Integer> entry : conteo.entrySet()) {
            if (entry.getValue() > maxVotos) {
                maxVotos = entry.getValue();
                masVotado = entry.getKey();
                empate = false;
            } else if (entry.getValue() == maxVotos) {
                empate = true;
            }
        }
        
        String mensajeResultado = "";
        String nombreExpulsado = "NADIE";
        
        if (votosSkip >= maxVotos || empate || masVotado == null) {
            mensajeResultado = "NADIE FUE EXPULSADO";
            nombreExpulsado = "NADIE";
        } else {
            boolean eraImpostor = false;
            for (AtencionJugador j : listaJugadores) {
                if (j.getNombreJugador() != null && j.getNombreJugador().equals(masVotado)) {
                    j.estaVivo = false;
                    eraImpostor = j.esImpostor;
                    break;
                }
            }
            nombreExpulsado = masVotado;
            
            int impostoresRestantes = 0;
            for (AtencionJugador j : listaJugadores) {
                if (j.estaVivo && j.esImpostor) impostoresRestantes++;
            }
            
            if (eraImpostor) {
                mensajeResultado = masVotado + " ERA UN IMPOSTOR. QUEDAN " + impostoresRestantes + " IMPOSTORES";
            } else {
                mensajeResultado = masVotado + " NO ERA UN IMPOSTOR. QUEDAN " + impostoresRestantes + " IMPOSTORES";
            }
        }
        
        System.out.println("Servidor: " + mensajeResultado);
        enviarATodos("RESULTADO_VOTACION:" + nombreExpulsado + ":" + mensajeResultado);
        
        verificarVictoria();
    }

    public static synchronized void verificarVictoria() {
        if (!partidaIniciada) return;

        int impostoresVivos = 0;
        int tripulantesVivos = 0;

        for (AtencionJugador j : listaJugadores) {
            // Asegurarnos de que el jugador sí definió un rol y está jugando (tiene nombre)
            if (j.estaVivo && j.getNombreJugador() != null) {
                if (j.esImpostor) impostoresVivos++;
                else tripulantesVivos++;
            }
        }

        // Si ya no queda nadie o no se ha evaluado correctamente a todos
        if (tripulantesVivos == 0 && impostoresVivos == 0) return;

        if (impostoresVivos >= tripulantesVivos && tripulantesVivos > 0) {
            finalizarPartida("Impostores");
        } else if (impostoresVivos == 0 && tripulantesVivos > 0) {
            finalizarPartida("Tripulantes");
        }
    }

    public static void enviarListaJugadores() {
        StringBuilder lista = new StringBuilder("LISTA_JUGADORES:");
        for (int i = 0; i < listaJugadores.size(); i++) {
            AtencionJugador aj = listaJugadores.get(i);
            String nombre = aj.getNombreJugador();
            if (nombre != null) {
                // Formato: Nombre:IdSombrero
                lista.append(nombre).append(":").append(aj.getIdSombrero());
                if (i < listaJugadores.size() - 1) lista.append(",");
            }
        }
        System.out.println("Enviando lista sincronizada: " + lista);
        enviarATodos(lista.toString());
    }
}