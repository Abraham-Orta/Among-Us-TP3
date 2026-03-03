package com.amongus.project.vista;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.InputStream;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.List;

import com.amongus.project.controlador.BucleJuego;
import com.amongus.project.modelo.EstadoJuego;
import com.amongus.project.modelo.Jugador;
import com.amongus.project.red.Cliente;

public class PantallaLobby extends JFrame implements Cliente.MensajeListener {
    
    private String nombreJugador;
    private boolean esHost;
    private Cliente cliente;
    private JPanel panelJugadores;
    private List<String> jugadoresConectados;
    private String codigoSala;
    
    public PantallaLobby(String nombreJugador, boolean esHost, Cliente cliente) {
        this.nombreJugador = nombreJugador;
        this.esHost = esHost;
        this.cliente = cliente;
        this.jugadoresConectados = new ArrayList<>();
        
        // Generar código de sala si es host
        if (esHost) {
            this.codigoSala = generarCodigoSala();
        }
        
        // Agregar el jugador local a la lista
        jugadoresConectados.add(nombreJugador);
        
        // Configuración ventana
        setTitle("Among Us - Sala de Espera");
        setSize(700, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Cargar icono
        cargarIconoVentana();
        
        // Panel con fondo
        JPanel panelFondo = crearPanelConFondo();
        panelFondo.setLayout(new BorderLayout(20, 20));
        panelFondo.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        
        // Título
        JPanel panelTitulo = new JPanel();
        panelTitulo.setLayout(new BoxLayout(panelTitulo, BoxLayout.Y_AXIS));
        panelTitulo.setOpaque(false);
        
        JLabel titulo = new JLabel("SALA DE ESPERA", SwingConstants.CENTER);
        titulo.setFont(cargarFuente(40f));
        titulo.setForeground(Color.WHITE);
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        panelTitulo.add(titulo);
        
        // Mostrar código si es host
        if (esHost && codigoSala != null) {
            JLabel lblCodigo = new JLabel("Código: " + codigoSala, SwingConstants.CENTER);
            lblCodigo.setFont(cargarFuente(28f));
            lblCodigo.setForeground(new Color(100, 255, 100)); // Verde claro
            lblCodigo.setAlignmentX(Component.CENTER_ALIGNMENT);
            panelTitulo.add(Box.createRigidArea(new Dimension(0, 10)));
            panelTitulo.add(lblCodigo);
        }
        
        panelFondo.add(panelTitulo, BorderLayout.NORTH);
        
        // Panel central con lista de jugadores
        JPanel panelCentral = new JPanel(new BorderLayout(10, 10));
        panelCentral.setOpaque(false);
        
        JLabel subtitulo = new JLabel("Jugadores Conectados:", SwingConstants.CENTER);
        subtitulo.setFont(cargarFuente(24f));
        subtitulo.setForeground(Color.WHITE);
        panelCentral.add(subtitulo, BorderLayout.NORTH);
        
        // Panel de jugadores (scrollable)
        panelJugadores = new JPanel();
        panelJugadores.setLayout(new BoxLayout(panelJugadores, BoxLayout.Y_AXIS));
        panelJugadores.setOpaque(false);
        actualizarListaJugadores();
        
        JScrollPane scroll = new JScrollPane(panelJugadores);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        panelCentral.add(scroll, BorderLayout.CENTER);
        
        panelFondo.add(panelCentral, BorderLayout.CENTER);
        
        // Panel inferior con botones
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        panelBotones.setOpaque(false);
        
        if (esHost) {
            JButton btnIniciar = crearBotonEstiloAmongUs("Iniciar Partida");
            btnIniciar.addActionListener(e -> iniciarPartida());
            panelBotones.add(btnIniciar);
        }
        
        JButton btnSalir = crearBotonEstiloAmongUs("Salir");
        btnSalir.addActionListener(e -> {
            dispose();
            System.exit(0);
        });
        panelBotones.add(btnSalir);
        
        panelFondo.add(panelBotones, BorderLayout.SOUTH);
        
        setContentPane(panelFondo);
        
        // Registrar este lobby como listener del cliente
        if (cliente != null) {
            cliente.setMensajeListener(this);
        }
    }
    
    private void actualizarListaJugadores() {
        panelJugadores.removeAll();
        
        for (String jugador : jugadoresConectados) {
            JLabel lblJugador = new JLabel("• " + jugador);
            lblJugador.setFont(cargarFuente(20f));
            lblJugador.setForeground(Color.WHITE);
            lblJugador.setAlignmentX(Component.CENTER_ALIGNMENT);
            panelJugadores.add(lblJugador);
            panelJugadores.add(Box.createRigidArea(new Dimension(0, 10)));
        }
        
        panelJugadores.revalidate();
        panelJugadores.repaint();
    }
    
    public void agregarJugador(String nombre) {
        if (!jugadoresConectados.contains(nombre)) {
            jugadoresConectados.add(nombre);
            actualizarListaJugadores();
        }
    }
    
    private void iniciarPartida() {
        // Enviar mensaje al servidor para iniciar
        if (cliente != null) {
            cliente.enviarMensaje("COMANDO:INICIAR");
        }
        
        // No abrimos el juego directamente. Esperamos a que el servidor nos confirme con "JUEGO_INICIADO"
        // para que todos entremos al mismo tiempo.
    }
    
    public void abrirJuego(Jugador jugadorLocal) {
        dispose();
        
        SwingUtilities.invokeLater(() -> {
            JFrame frameJuego = new JFrame("Among Us - En Juego");
            frameJuego.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            PanelJuego panelJuego = new PanelJuego(cliente, jugadorLocal);
            frameJuego.add(panelJuego);
            frameJuego.pack();
            frameJuego.setLocationRelativeTo(null);
            frameJuego.setVisible(true);
            
            BucleJuego bucle = new BucleJuego(panelJuego);
            bucle.iniciar();
            
            // requestFocusInWindow es más confiable que requestFocus para paneles dentro de un JFrame
            panelJuego.requestFocusInWindow();
        });
    }
    
    private void cargarIconoVentana() {
        try {
            String n = "icono.jpg";
            Image icono = null;
            java.net.URL u = getClass().getClassLoader().getResource(n);
            if (u != null) icono = ImageIO.read(u);
            else {
                String[] rutas = {"src/main/resources/" + n, "resources/" + n, n};
                for (String r : rutas) {
                    File f = new File(r);
                    if (f.exists()) {
                        icono = ImageIO.read(f);
                        break;
                    }
                }
            }
            if (icono != null) setIconImage(icono);
        } catch (Exception e) { 
            System.err.println("Error al cargar icono: " + e.getMessage()); 
        }
    }
    
    private JPanel crearPanelConFondo() {
        return new JPanel() {
            private Image fondo;
            {
                try {
                    String n = "fondo.jpg";
                    java.net.URL u = getClass().getClassLoader().getResource(n);
                    if (u != null) fondo = ImageIO.read(u);
                    else {
                        String[] rutas = {"src/main/resources/" + n, "resources/" + n, n};
                        for (String r : rutas) {
                            File f = new File(r);
                            if (f.exists()) {
                                fondo = ImageIO.read(f);
                                break;
                            }
                        }
                    }
                } catch (Exception e) { 
                    System.err.println("Error fondo: " + e.getMessage()); 
                }
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (fondo != null) g.drawImage(fondo, 0, 0, getWidth(), getHeight(), this);
                else {
                    g.setColor(Color.DARK_GRAY);
                    g.fillRect(0,0,getWidth(),getHeight());
                }
                g.setColor(new Color(0,0,0,150));
                g.fillRect(0,0,getWidth(),getHeight());
            }
        };
    }
    
    private Font cargarFuente(float tamano) {
        try {
            String nombre = "InYourFaceJoffrey.ttf";
            String ruta = "in_your_face_joffrey/" + nombre;
            InputStream is = getClass().getClassLoader().getResourceAsStream(ruta);
            if (is == null) {
                String[] rutas = {"src/main/resources/"+ruta, "resources/"+ruta};
                for (String r : rutas) {
                    File f = new File(r);
                    if (f.exists()) return Font.createFont(Font.TRUETYPE_FONT, f).deriveFont(tamano);
                }
                return new Font("Arial", Font.BOLD, (int)tamano);
            }
            return Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(tamano);
        } catch (Exception e) { 
            return new Font("Arial", Font.BOLD, (int)tamano); 
        }
    }
    
    private JButton crearBotonEstiloAmongUs(String txt) {
        JButton b = new JButton(txt) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isPressed()) g2.setColor(new Color(30,30,30));
                else if (getModel().isRollover()) g2.setColor(new Color(60,60,60));
                else g2.setColor(Color.BLACK);

                g2.fillRoundRect(0,0,getWidth(),getHeight(),30,30);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(3));
                g2.drawRoundRect(1,1,getWidth()-3,getHeight()-3,30,30);

                g2.dispose();
                super.paintComponent(g);
            }
        };

        b.setPreferredSize(new Dimension(220, 50));
        b.setFont(cargarFuente(22f));
        b.setForeground(Color.WHITE);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        return b;
    }

    private Color getColorPorNombre(String nombreColor) {
        if (nombreColor == null) return Color.WHITE;
        switch (nombreColor.toUpperCase()) {
            case "RED": return Color.RED;
            case "BLUE": return Color.BLUE;
            case "GREEN": return Color.GREEN;
            case "YELLOW": return Color.YELLOW;
            case "ORANGE": return Color.ORANGE;
            case "PINK": return Color.PINK;
            case "PURPLE": return new Color(128, 0, 128);
            case "CYAN": return Color.CYAN;
            case "WHITE": return Color.WHITE;
            case "BROWN": return new Color(139, 69, 19);
            default: return Color.GRAY;
        }
    }
    
    @Override
    public void onMensajeRecibido(String mensaje) {
        if (mensaje.startsWith("LISTA_JUGADORES:")) {
            String listaStr = mensaje.substring(16);
            if (listaStr.isEmpty()) return;
            
            String[] nombres = listaStr.split(",");
            SwingUtilities.invokeLater(() -> {
                jugadoresConectados.clear();
                for (String nombre : nombres) {
                    if (nombre != null && !nombre.trim().isEmpty()) {
                        jugadoresConectados.add(nombre.trim());
                    }
                }
                actualizarListaJugadores();
            });

        } else if (mensaje.startsWith("DATOS_PARTIDA:")) {
            String datos = mensaje.substring(14);
            String[] jugadoresData = datos.split(";");

            Jugador miJugador = null;
            java.util.List<Jugador> todosJugadores = new java.util.ArrayList<>();

            for (String dataJugador : jugadoresData) {
                String[] partes = dataJugador.split(",");
                String nombre = partes[0];
                boolean esImpostor = partes[1].equals("IMPOSTOR");
                Color color = getColorPorNombre(partes[2]);
                
                Jugador nuevoJugador = new Jugador(nombre, 100, 100, color, esImpostor);
                todosJugadores.add(nuevoJugador);
                
                if (nombre.equals(this.nombreJugador)) {
                    miJugador = nuevoJugador;
                }
            }

            // Poblar el EstadoJuego con la lista limpia (limpiar antes para evitar duplicados)
            EstadoJuego estado = EstadoJuego.getInstancia();
            estado.getJugadores().clear();
            for (Jugador j : todosJugadores) {
                estado.agregarJugador(j);
            }
            estado.setFaseActual(EstadoJuego.Fase.JUGANDO);

            final Jugador jugadorLocalFinal = miJugador;
            SwingUtilities.invokeLater(() -> abrirJuego(jugadorLocalFinal));
        }
    }
    
    private String generarCodigoSala() {
        String caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder codigo = new StringBuilder();
        java.util.Random random = new java.util.Random();
        
        for (int i = 0; i < 6; i++) {
            int index = random.nextInt(caracteres.length());
            codigo.append(caracteres.charAt(index));
        }
        
        return codigo.toString();
    }
}