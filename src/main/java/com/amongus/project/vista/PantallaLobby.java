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
import java.io.IOException;

public class PantallaLobby extends JFrame implements Cliente.MensajeListener {

    // Caché estático: la fuente se carga del disco UNA sola vez
    private static Font fuenteBase;

    private String nombreJugador;
    private boolean esHost;
    private Cliente cliente;
    private JPanel panelJugadores;
    private List<String> jugadoresConectados;
    private String codigoSala;

    private JComboBox<String> selectorMapa;
   private final String[] MAPAS_DISPONIBLES = {"mapa1.tmj", "mapa2.tmj"};

    // --- SISTEMA DE SOMBREROS ---
    private List<String> listaSombreros = new ArrayList<>();
    private int indiceSombreroActual = 0;
    private JLabel lblVistaPreviaSombrero;
    private java.util.Map<String, String> sombrerosJugadores = new java.util.HashMap<>();
    // ----------------------------

    private boolean soyImpostor = false; // Rol asignado por el servidor
    private List<String> companerosImpostores = new ArrayList<>(); // Lista de compañeros impostores

    // Cada cliente tiene su propio EstadoJuego — NO compartido entre ventanas
    private EstadoJuego estadoJuego;

    // --- CACHÉ DE ICONOS PARA OPTIMIZACIÓN ---
    private ImageIcon iconoJugadorCache;
    private java.util.Map<String, ImageIcon> cacheSombrerosLobby = new java.util.HashMap<>();

    public PantallaLobby(String nombreJugador, boolean esHost, Cliente cliente) throws IOException {
        this.nombreJugador       = nombreJugador;
        this.esHost              = esHost;
        this.cliente             = cliente;
        this.jugadoresConectados = new ArrayList<>();
        this.estadoJuego         = new EstadoJuego();

        if (esHost) this.codigoSala = generarCodigoSala();

        jugadoresConectados.add(nombreJugador);

        setTitle("Among Us - Sala de Espera");
        setSize(850, 650); // AUMENTADO para dar más espacio a los elementos
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                dispose();
                new PantallaSeleccionModo().setVisible(true);
            }
        });

        cargarIconoVentana();

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

        if (esHost && codigoSala != null) {
            JLabel lblCodigo = new JLabel("Codigo: " + codigoSala, SwingConstants.CENTER);
            lblCodigo.setFont(cargarFuente(28f));
            lblCodigo.setForeground(new Color(100, 255, 100));
            lblCodigo.setAlignmentX(Component.CENTER_ALIGNMENT);
            panelTitulo.add(Box.createRigidArea(new Dimension(0, 10)));
            panelTitulo.add(lblCodigo);

            // Selector de mapa
            JPanel panelSelector = new JPanel(new FlowLayout(FlowLayout.CENTER));
            panelSelector.setOpaque(false);

            selectorMapa = new JComboBox<>(new String[]{"Villa Asia - Sector A", "Villa Asia - Sector B"});
            selectorMapa.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
                @Override
                public void paint(Graphics g, JComponent c) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(Color.BLACK);
                    g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 30, 30);
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(3));
                    g2.drawRoundRect(1, 1, c.getWidth() - 3, c.getHeight() - 3, 30, 30);
                    g2.dispose();
                    paintCurrentValue(g, rectangleForCurrentValue(), hasFocus);
                }
                @Override
                public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
                    ListCellRenderer renderer = comboBox.getRenderer();
                    Component c = renderer.getListCellRendererComponent(listBox, comboBox.getSelectedItem(), -1, false, false);
                    c.setBackground(Color.BLACK);
                    c.setForeground(Color.WHITE);
                    if (c instanceof JComponent) ((JComponent) c).setOpaque(false);
                    currentValuePane.paintComponent(g, c, comboBox, bounds.x, bounds.y, bounds.width, bounds.height, false);
                }
                @Override
                protected JButton createArrowButton() {
                    JButton arrow = new JButton("▼");
                    arrow.setForeground(Color.WHITE);
                    arrow.setFocusPainted(false);
                    arrow.setContentAreaFilled(false);
                    arrow.setOpaque(false);
                    arrow.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 15));
                    return arrow;
                }
            });

            selectorMapa.setFont(cargarFuente(19f));
            selectorMapa.setForeground(Color.WHITE);
            selectorMapa.setBackground(new Color(0, 0, 0, 0));
            selectorMapa.setOpaque(false);
            selectorMapa.setPreferredSize(new Dimension(280, 45));
            selectorMapa.setCursor(new Cursor(Cursor.HAND_CURSOR));
            selectorMapa.setBorder(BorderFactory.createEmptyBorder());
            selectorMapa.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    JLabel c = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    c.setOpaque(true);
                    c.setBackground(isSelected ? new Color(60, 60, 60) : Color.BLACK);
                    c.setForeground(Color.WHITE);
                    c.setHorizontalAlignment(SwingConstants.CENTER);
                    c.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
                    return c;
                }
            });

            panelSelector.add(selectorMapa);
            panelTitulo.add(Box.createRigidArea(new Dimension(0, 15)));
            panelTitulo.add(panelSelector);
        }

        panelFondo.add(panelTitulo, BorderLayout.NORTH);

        JPanel panelCentral = new JPanel(new BorderLayout(10, 10));
        panelCentral.setOpaque(false);

        JLabel subtitulo = new JLabel("Jugadores Conectados:", SwingConstants.CENTER);
        subtitulo.setFont(cargarFuente(24f));
        subtitulo.setForeground(Color.WHITE);
        panelCentral.add(subtitulo, BorderLayout.NORTH);

        panelJugadores = new JPanel();
        panelJugadores.setLayout(new BoxLayout(panelJugadores, BoxLayout.Y_AXIS));
        panelJugadores.setOpaque(false);
        actualizarListaJugadores();

        JScrollPane scroll = new JScrollPane(panelJugadores);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        
        // PERSONALIZACIÓN DE BARRA DE DESPLAZAMIENTO (SCROLLBAR) - ESTILO NEGRO
        scroll.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(60, 60, 60)); // Gris oscuro
                g2.fillRoundRect(thumbBounds.x + 4, thumbBounds.y + 2, thumbBounds.width - 8, thumbBounds.height - 4, 10, 10);
                g2.dispose();
            }
            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
                g.setColor(new Color(15, 15, 15)); // Negro
                g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
            }
            @Override
            protected JButton createDecreaseButton(int orientation) { return crearBotonInvisible(); }
            @Override
            protected JButton createIncreaseButton(int orientation) { return crearBotonInvisible(); }
            private JButton crearBotonInvisible() {
                JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); return b;
            }
        });
        
        panelCentral.add(scroll, BorderLayout.CENTER);

        // --- PANEL DE SELECCIÓN DE SOMBRERO ---
        JPanel panelSombrero = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        panelSombrero.setOpaque(false);

        // Inicializar lista de sombreros (IDs hats0002 a hats0110 - Se saltó el 0001 por eliminación)
        listaSombreros.add("ninguno");
        for (int i = 2; i <= 110; i++) {
            listaSombreros.add(String.format("hats%04d", i));
        }

        JButton btnAnterior = crearBotonFlecha(true);
        JButton btnSiguiente = crearBotonFlecha(false);

        lblVistaPreviaSombrero = new JLabel();
        lblVistaPreviaSombrero.setPreferredSize(new Dimension(80, 80));
        lblVistaPreviaSombrero.setHorizontalAlignment(SwingConstants.CENTER);
        actualizarVistaPreviaSombrero();

        btnAnterior.addActionListener(e -> {
            indiceSombreroActual = (indiceSombreroActual - 1 + listaSombreros.size()) % listaSombreros.size();
            actualizarVistaPreviaSombrero();
            enviarSombreroRed();
        });

        btnSiguiente.addActionListener(e -> {
            indiceSombreroActual = (indiceSombreroActual + 1) % listaSombreros.size();
            actualizarVistaPreviaSombrero();
            enviarSombreroRed();
        });

        panelSombrero.add(btnAnterior);
        panelSombrero.add(lblVistaPreviaSombrero);
        panelSombrero.add(btnSiguiente);

        panelCentral.add(panelSombrero, BorderLayout.SOUTH);
        // ---------------------------------------

        panelFondo.add(panelCentral, BorderLayout.CENTER);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        panelBotones.setOpaque(false);

        if (esHost) {
            JButton btnIniciar = crearBotonEstiloAmongUs("Iniciar Partida");
            btnIniciar.addActionListener(e -> iniciarPartida());
            panelBotones.add(btnIniciar);
        }

        JButton btnSalir = crearBotonEstiloAmongUs("Salir");
        btnSalir.addActionListener(e -> { dispose(); new PantallaSeleccionModo().setVisible(true); });
        panelBotones.add(btnSalir);

        panelFondo.add(panelBotones, BorderLayout.SOUTH);
        setContentPane(panelFondo);

        if (cliente != null) cliente.setMensajeListener(this);
    }

    private void actualizarListaJugadores() {
        if (panelJugadores == null) return;
        
        panelJugadores.removeAll();

        // 1. CARGA ROBUSTA DEL ICONO
        if (iconoJugadorCache == null) {
            try {
                String n = "icono_espera.jpg"; // Nombre corregido
                Image img = null;
                
                // Intento 1: Classloader
                java.net.URL u = getClass().getClassLoader().getResource(n);
                if (u != null) img = ImageIO.read(u);
                
                // Intento 2: Rutas físicas
                if (img == null) {
                    String[] rutas = {"src/main/resources/" + n, "resources/" + n, n};
                    for (String r : rutas) {
                        File f = new File(r);
                        if (f.exists()) { img = ImageIO.read(f); break; }
                    }
                }
                
                if (img != null) {
                    iconoJugadorCache = new ImageIcon(img.getScaledInstance(48, 32, Image.SCALE_SMOOTH));
                }
            } catch (Exception e) {
                System.err.println("No se pudo cargar el icono del jugador en el Lobby");
            }
        }

        // 2. Construir la lista visual
        for (String nombre : jugadoresConectados) {
            JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
            fila.setOpaque(false);
            fila.setMaximumSize(new Dimension(600, 60));

            // Capa de Iconos (Cuerpo + Sombrero encima)
            JPanel capaIconos = new JPanel(null);
            capaIconos.setPreferredSize(new Dimension(60, 50));
            capaIconos.setOpaque(false);

            // Icono del Tripulante (Abajo)
            JLabel lblJugador = new JLabel(iconoJugadorCache);
            lblJugador.setBounds(0, 15, 48, 32);
            capaIconos.add(lblJugador);

            // Icono del Sombrero (Arriba)
            String idSom = sombrerosJugadores.getOrDefault(nombre.trim(), "ninguno");
            if (idSom != null && !idSom.equals("ninguno")) {
                Image imgS = PanelJuego.obtenerImagenFija("sprites/sombreros/" + idSom + ".png");
                if (imgS != null) {
                    JLabel lblSom = new JLabel(new ImageIcon(imgS.getScaledInstance(45, 35, Image.SCALE_SMOOTH)));
                    lblSom.setBounds(2, 0, 45, 35);
                    capaIconos.add(lblSom);
                    capaIconos.setComponentZOrder(lblSom, 0); // Forzar sombrero al frente
                }
            }

            // Nombre
            JLabel lblNombre = new JLabel(nombre.trim());
            lblNombre.setFont(cargarFuente(24f));
            lblNombre.setForeground(Color.WHITE);

            fila.add(capaIconos);
            fila.add(lblNombre);
            
            panelJugadores.add(fila);
            panelJugadores.add(Box.createRigidArea(new Dimension(0, 15)));
        }

        // 3. REFRESO MAESTRO: Forzamos a toda la cadena de componentes a actualizarse
        panelJugadores.revalidate();
        panelJugadores.repaint();
        
        // Buscamos el ScrollPane que contiene al panel y lo refrescamos también
        Container parent = panelJugadores.getParent();
        if (parent instanceof JViewport) {
            Container scroll = parent.getParent();
            if (scroll instanceof JScrollPane) {
                scroll.revalidate();
                scroll.repaint();
            }
        }
    }

    public void agregarJugador(String nombre) {
        if (!jugadoresConectados.contains(nombre)) {
            jugadoresConectados.add(nombre);
            actualizarListaJugadores();
        }
    }

    private void iniciarPartida() {
        if (cliente != null) {
            // Validación de rúbrica: mínimo 5 jugadores para iniciar
            if (jugadoresConectados.size() < 5) {
                JOptionPane.showMessageDialog(this, 
                    "Se necesitan al menos 5 jugadores para iniciar (Regla del proyecto).", 
                    "Jugadores insuficientes", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }

            String mapaSeleccionado = "mapa1.png";
            if (selectorMapa != null) {
                mapaSeleccionado = MAPAS_DISPONIBLES[selectorMapa.getSelectedIndex()];
            }
            cliente.enviarMensaje("COMANDO:INICIAR:" + mapaSeleccionado);
        }
    }

    /** Abre la ventana del juego — usa el EstadoJuego propio de este cliente. */
    public void abrirJuego() {
        dispose();

        SwingUtilities.invokeLater(() -> {
            JFrame frameJuego = new JFrame("Among Us - En Juego (" + nombreJugador + ")");
            frameJuego.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            try {
                String n = "icono.jpg";
                Image icono = null;
                java.net.URL u = getClass().getClassLoader().getResource(n);
                if (u != null) icono = ImageIO.read(u);
                else {
                    String[] rutas = {"src/main/resources/" + n, "resources/" + n, n};
                    for (String r : rutas) {
                        File f = new File(r);
                        if (f.exists()) { icono = ImageIO.read(f); break; }
                    }
                }
                if (icono != null) frameJuego.setIconImage(icono);
            } catch (Exception e) { /* icono opcional */ }

            PanelJuego panelJuego = new PanelJuego(estadoJuego);
            frameJuego.add(panelJuego);
            frameJuego.pack();
            frameJuego.setLocationRelativeTo(null);
            frameJuego.setVisible(true);

            BucleJuego bucle = new BucleJuego(panelJuego, cliente);
            bucle.iniciar();

            panelJuego.requestFocusInWindow();
        });
    }

    @Override
    public void onMensajeRecibido(String mensaje) {
        if (mensaje.startsWith("LISTA_JUGADORES:")) {
            String listaStr = mensaje.substring(16);
            if (listaStr.isEmpty()) return;
            String[] entradas = listaStr.split(",");
            SwingUtilities.invokeLater(() -> {
                jugadoresConectados.clear();
                for (String entrada : entradas) {
                    if (entrada.contains(":")) {
                        String[] partes = entrada.split(":");
                        String nom = partes[0].trim();
                        String som = partes[1].trim();
                        jugadoresConectados.add(nom);
                        sombrerosJugadores.put(nom, som);
                    } else {
                        String nom = entrada.trim();
                        if (!nom.isEmpty()) jugadoresConectados.add(nom);
                    }
                }
                actualizarListaJugadores();
            });

        } else if (mensaje.startsWith("TAREAS:") || mensaje.startsWith("TAREAS_FALSAS:")) {
            System.out.println("[LOBBY] Recibidas tareas anticipadas: " + mensaje);
            // Guardar para que cuando se cree el jugador local, ya las tenga
            String lista = mensaje.substring(mensaje.indexOf(":") + 1);
            String[] tareas = lista.split(",");
            Jugador local = estadoJuego.getJugadorLocal();
            if (local != null) {
                local.getTareasPendientes().clear();
                for (String t : tareas) local.getTareasPendientes().add(t);
                local.setTotalTareas(tareas.length);
            } else {
                // Si el jugador aún no existe (porque JUEGO_INICIADO viene después), 
                // podemos guardarlas en el EstadoJuego o una variable temporal.
                // Pero usualmente ROL y TAREAS vienen justo después de JUEGO_INICIADO.
            }

        } else if (mensaje.startsWith("SOMBRERO:")) {
            try {
                String[] p = mensaje.split(":");
                String nom = p[1].trim();
                String som = p[2].trim();
                sombrerosJugadores.put(nom, som);
                actualizarListaJugadores();
            } catch (Exception e) {}

        } else if (mensaje.startsWith("ROL:")) {
            // El servidor nos susurra el rol
            String[] partes = mensaje.split(":");
            soyImpostor = partes[1].equals("IMPOSTOR");
            
            if (soyImpostor && partes.length >= 3) {
                String[] compas = partes[2].split(",");
                for (String c : compas) {
                    companerosImpostores.add(c.trim());
                }
            }
            
            System.out.println("Rol asignado: " + (soyImpostor ? "Impostor" : "Tripulante"));

        } else if (mensaje.startsWith("JUEGO_INICIADO")) {
            // Extraer el mapa elegido (ej. JUEGO_INICIADO:mapa2.png)
            String mapaElegido = "mapa1.png";
            if (mensaje.contains(":")) mapaElegido = mensaje.split(":")[1];

            // Inicializar el mapa en el EstadoJuego propio de este cliente
            com.amongus.project.modelo.Mapa mapa = null;
            try {
                mapa = new com.amongus.project.modelo.Mapa(mapaElegido);
            } catch (IOException ex) {
                System.getLogger(PantallaLobby.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }
            estadoJuego.setMapa(mapa);

            // Paleta de colores para todos los jugadores
            Color[] todosColores = {Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.CYAN,
                                    Color.MAGENTA, Color.ORANGE, Color.PINK, Color.WHITE};

            // Determinar el índice de ESTE jugador (Comparación robusta)
            int miIndice = -1;
            for (int i = 0; i < jugadoresConectados.size(); i++) {
                if (jugadoresConectados.get(i).trim().equalsIgnoreCase(nombreJugador.trim())) {
                    miIndice = i;
                    break;
                }
            }
            if (miIndice < 0) miIndice = 0;
            Color miColor = todosColores[miIndice % todosColores.length];

            // Crear nuestro propio jugador local
            Jugador jugadorLocal = new Jugador(nombreJugador.trim(), 100, 100, miColor, soyImpostor);
            jugadorLocal.setSombrero(listaSombreros.get(indiceSombreroActual));
            jugadorLocal.setClienteRed(this.cliente);
            jugadorLocal.setEstadoJuego(estadoJuego);
            estadoJuego.setJugadorLocal(jugadorLocal);

            // Crear jugadores remotos SINCRONIZADOS
            for (int idx = 0; idx < jugadoresConectados.size(); idx++) {
                String nombreRemoto = jugadoresConectados.get(idx).trim();
                if (!nombreRemoto.equalsIgnoreCase(nombreJugador.trim())) {
                    Color colorRemoto = todosColores[idx % todosColores.length];
                    
                    // RECONOCIMIENTO DE IMPOSTORES: Comparación manual robusta
                    boolean esRemotoImpostor = false;
                    if (soyImpostor) {
                        for (String compa : companerosImpostores) {
                            if (compa.trim().equalsIgnoreCase(nombreRemoto)) {
                                esRemotoImpostor = true;
                                break;
                            }
                        }
                    }

                    Jugador otro = new Jugador(nombreRemoto, 150 + (idx * 50), 100, colorRemoto, esRemotoImpostor);
                    
                    // ¡CLAVE! Pasamos el sombrero que el servidor nos dijo en el Lobby
                    String sombreroRemoto = sombrerosJugadores.getOrDefault(nombreRemoto, "ninguno");
                    otro.setSombrero(sombreroRemoto);
                    
                    otro.setEstadoJuego(estadoJuego);
                    estadoJuego.agregarJugador(otro);
                }
            }

            estadoJuego.setFaseActual(EstadoJuego.Fase.REVELACION);
            enviarSombreroRed(); // Notificamos una última vez al entrar

            SwingUtilities.invokeLater(() -> abrirJuego());
        }
    }

    private String generarCodigoSala() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        java.util.Random rnd = new java.util.Random();
        for (int i = 0; i < 6; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    private Color getColorPorNombre(String nombreColor) {
        if (nombreColor == null) return Color.WHITE;
        switch (nombreColor.toUpperCase()) {
            case "RED":    return Color.RED;
            case "BLUE":   return Color.BLUE;
            case "GREEN":  return Color.GREEN;
            case "YELLOW": return Color.YELLOW;
            case "ORANGE": return Color.ORANGE;
            case "PINK":   return Color.PINK;
            case "PURPLE": return new Color(128, 0, 128);
            case "CYAN":   return Color.CYAN;
            case "WHITE":  return Color.WHITE;
            case "BROWN":  return new Color(139, 69, 19);
            default:       return Color.GRAY;
        }
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
                    if (f.exists()) { icono = ImageIO.read(f); break; }
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
                            if (f.exists()) { fondo = ImageIO.read(f); break; }
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
                else { g.setColor(Color.DARK_GRAY); g.fillRect(0, 0, getWidth(), getHeight()); }
                g.setColor(new Color(0, 0, 0, 150));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
    }

    private JButton crearBotonFlecha(boolean izquierda) {
        JButton b = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                Image imgFlecha = PanelJuego.obtenerImagenFija("flecha.png");
                if (imgFlecha != null) {
                    if (izquierda) {
                        g2.drawImage(imgFlecha, 0, 0, getWidth(), getHeight(), null);
                    } else {
                        // Espejo horizontal para la flecha derecha
                        g2.drawImage(imgFlecha, getWidth(), 0, -getWidth(), getHeight(), null);
                    }
                } else {
                    g2.setColor(Color.WHITE);
                    g2.fillPolygon(izquierda ? new int[]{getWidth(), 0, getWidth()} : new int[]{0, getWidth(), 0},
                                   new int[]{0, getHeight()/2, getHeight()}, 3);
                }
                g2.dispose();
            }
        };
        b.setPreferredSize(new Dimension(40, 40));
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void actualizarVistaPreviaSombrero() {
        String idSombrero = listaSombreros.get(indiceSombreroActual);
        if (idSombrero.equals("ninguno")) {
            lblVistaPreviaSombrero.setIcon(null);
            lblVistaPreviaSombrero.setText("SIN SKIN");
            lblVistaPreviaSombrero.setForeground(Color.GRAY);
            lblVistaPreviaSombrero.setFont(cargarFuente(18f));
        } else {
            lblVistaPreviaSombrero.setText("");
            Image img = PanelJuego.obtenerImagenFija("sprites/sombreros/" + idSombrero + ".png");
            if (img != null) {
                lblVistaPreviaSombrero.setIcon(new ImageIcon(img.getScaledInstance(60, 50, Image.SCALE_SMOOTH)));
            }
        }
    }

    private void enviarSombreroRed() {
        if (cliente != null) {
            String idSombrero = listaSombreros.get(indiceSombreroActual);
            cliente.enviarMensaje("SOMBRERO:" + nombreJugador + ":" + idSombrero);
        }
    }

    private Font cargarFuente(float tamano) {
        // Cachear la fuente base para no releer del disco en cada llamada
        if (fuenteBase == null) {
            try {
                String ruta = "in_your_face_joffrey/InYourFaceJoffrey.ttf";
                InputStream is = getClass().getClassLoader().getResourceAsStream(ruta);
                if (is == null) {
                    String[] rutas = {"src/main/resources/" + ruta, "resources/" + ruta};
                    for (String r : rutas) {
                        File f = new File(r);
                        if (f.exists()) { fuenteBase = Font.createFont(Font.TRUETYPE_FONT, f); break; }
                    }
                } else {
                    fuenteBase = Font.createFont(Font.TRUETYPE_FONT, is);
                }
            } catch (Exception e) {
                // fallback silencioso
            }
            if (fuenteBase == null) fuenteBase = new Font("Arial", Font.BOLD, 12);
        }
        return fuenteBase.deriveFont(tamano);
    }

    private JButton crearBotonEstiloAmongUs(String txt) {
        JButton b = new JButton(txt) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed())       g2.setColor(new Color(30, 30, 30));
                else if (getModel().isRollover()) g2.setColor(new Color(60, 60, 60));
                else                              g2.setColor(Color.BLACK);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(3));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 30, 30);
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
}