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
    
    // --- NUEVO: Selector de mapa ---
    private JComboBox<String> selectorMapa;
    private final String[] MAPAS_DISPONIBLES = {"mapa1.png", "mapa2.png"}; // Los dos mapas requeridos por el PDF
    
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
            JLabel lblCodigo = new JLabel("Codigo: " + codigoSala, SwingConstants.CENTER);
            lblCodigo.setFont(cargarFuente(28f));
            lblCodigo.setForeground(new Color(100, 255, 100)); // Verde claro
            lblCodigo.setAlignmentX(Component.CENTER_ALIGNMENT);
            panelTitulo.add(Box.createRigidArea(new Dimension(0, 10)));
            panelTitulo.add(lblCodigo);
            
            // Selector de mapa (Solo visible para el anfitrión)
            JPanel panelSelector = new JPanel(new FlowLayout(FlowLayout.CENTER));
            panelSelector.setOpaque(false);
            
            selectorMapa = new JComboBox<>(new String[]{"Villa Asia - Sector A", "Villa Asia - Sector B"});
            
            // Sobrescribimos TODO el proceso de pintado para hacerlo idéntico a los botones del juego
            selectorMapa.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
                @Override
                public void paint(Graphics g, JComponent c) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    // Fondo de cápsula negra
                    g2.setColor(Color.BLACK);
                    g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 30, 30);
                    
                    // Borde de cápsula blanca
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(3));
                    g2.drawRoundRect(1, 1, c.getWidth() - 3, c.getHeight() - 3, 30, 30);
                    g2.dispose();
                    
                    // Dibujamos el texto encima
                    Rectangle bounds = rectangleForCurrentValue();
                    paintCurrentValue(g, bounds, hasFocus);
                }
                
                @Override
                public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
                    ListCellRenderer renderer = comboBox.getRenderer();
                    Component c = renderer.getListCellRendererComponent(listBox, comboBox.getSelectedItem(), -1, false, false);
                    c.setBackground(Color.BLACK);
                    c.setForeground(Color.WHITE);
                    if (c instanceof JComponent) {
                        ((JComponent) c).setOpaque(false); // Transparente para que se vea la cápsula
                    }
                    currentValuePane.paintComponent(g, c, comboBox, bounds.x, bounds.y, bounds.width, bounds.height, false);
                }
                
                @Override
                protected JButton createArrowButton() {
                    // Flecha completamente transparente
                    JButton arrow = new JButton("▼");
                    arrow.setForeground(Color.WHITE);
                    arrow.setFocusPainted(false);
                    arrow.setContentAreaFilled(false);
                    arrow.setOpaque(false);
                    arrow.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 15)); // Un margen derecho
                    return arrow;
                }
            });
            
            selectorMapa.setFont(cargarFuente(19f)); 
            selectorMapa.setForeground(Color.WHITE);
            selectorMapa.setBackground(new Color(0, 0, 0, 0)); // Transparente para que se vea la cápsula pintada a mano
            selectorMapa.setOpaque(false);
            selectorMapa.setPreferredSize(new Dimension(280, 45)); // Más alto para la cápsula
            selectorMapa.setCursor(new Cursor(Cursor.HAND_CURSOR));
            selectorMapa.setBorder(BorderFactory.createEmptyBorder()); // Quitamos el borde viejo
            
            // Renderizador de la lista emergente para que sea oscurísima
            selectorMapa.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    JLabel c = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    c.setOpaque(true);
                    c.setBackground(isSelected ? new Color(60, 60, 60) : Color.BLACK);
                    c.setForeground(Color.WHITE);
                    c.setHorizontalAlignment(SwingConstants.CENTER);
                    c.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0)); // Espaciado entre opciones
                    return c;
                }
            });
            
            // Forzar el color de fondo y borde del menú desplegable (Popup)
            Object comp = selectorMapa.getAccessibleContext().getAccessibleChild(0);
            if (comp instanceof javax.swing.plaf.basic.ComboPopup) {
                JComponent popup = (JComponent) comp;
                JList<?> list = ((javax.swing.plaf.basic.ComboPopup) popup).getList();
                list.setBackground(Color.BLACK);
                list.setForeground(Color.WHITE);
                list.setSelectionBackground(new Color(60, 60, 60));
                list.setSelectionForeground(Color.WHITE);
                popup.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
            }
            
            panelSelector.add(selectorMapa);
            panelTitulo.add(Box.createRigidArea(new Dimension(0, 15)));
            panelTitulo.add(panelSelector);
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
        
        // Intentamos cargar la imagen pequeña para los jugadores de forma síncrona
        ImageIcon iconoJugador = null;
        try {
            // NOTA: Java no soporta archivos .webp de forma nativa. 
            // Buscamos la version .png o .jpg
            String[] posiblesNombres = {"Imagen_Espera.png", "Imagen_Espera.jpg", "icono_jugador.png"};
            Image img = null;
            
            for (String n : posiblesNombres) {
                java.net.URL u = getClass().getClassLoader().getResource(n);
                if (u != null) {
                    img = ImageIO.read(u);
                } else {
                    String[] rutas = {"src/main/resources/" + n, "resources/" + n, n};
                    for (String r : rutas) {
                        File f = new File(r);
                        if (f.exists()) {
                            img = ImageIO.read(f);
                            break;
                        }
                    }
                }
                if (img != null) break;
            }
            
            if (img != null) {
                // Redimensionar a 48x32 píxeles
                Image imgRedimensionada = img.getScaledInstance(48, 32, Image.SCALE_SMOOTH);
                iconoJugador = new ImageIcon(imgRedimensionada);
            }
        } catch (Exception e) {
            System.err.println("No se pudo cargar el icono del jugador para la lista: " + e.getMessage());
        }
        
        for (String jugador : jugadoresConectados) {
            JLabel lblJugador = new JLabel();
            
            // Limpiamos la variable jugador por si trae espacios vacíos raros del servidor
            String nombreLimpio = jugador.trim();
            
            // Si logramos cargar la imagen, se la ponemos al JLabel junto con el nombre
            if (iconoJugador != null) {
                lblJugador.setIcon(iconoJugador);
                lblJugador.setText(nombreLimpio); // Sin ningún espacio manual
                lblJugador.setIconTextGap(15); // Espacio limpio manejado por Swing
            } else {
                lblJugador.setText("- " + nombreLimpio); // Guion clásico si falla
            }
            
            lblJugador.setFont(cargarFuente(24f)); // Fuente un poco más grande para que haga juego con la imagen
            lblJugador.setForeground(Color.WHITE);
            lblJugador.setAlignmentX(Component.CENTER_ALIGNMENT);
            panelJugadores.add(lblJugador);
            panelJugadores.add(Box.createRigidArea(new Dimension(0, 15))); // Más espacio entre jugadores
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
            String mapaSeleccionado = "mapa1.png"; // Mapa por defecto
            if (selectorMapa != null) {
                // Obtenemos el archivo de la constante según la posición elegida en el combo box
                int index = selectorMapa.getSelectedIndex();
                mapaSeleccionado = MAPAS_DISPONIBLES[index];
            }
            
            // Enviamos la orden de iniciar y le pegamos el nombre del mapa elegido
            cliente.enviarMensaje("COMANDO:INICIAR:" + mapaSeleccionado);
        }
    }
    
    public void abrirJuego() {
        dispose();
        
        SwingUtilities.invokeLater(() -> {
            JFrame frameJuego = new JFrame("Among Us - En Juego");
            frameJuego.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            // Reutilizamos el método para ponerle el icono también a la ventana del juego
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
            } catch (Exception e) {}
            
            PanelJuego panelJuego = new PanelJuego();
            frameJuego.add(panelJuego);
            frameJuego.pack();
            frameJuego.setLocationRelativeTo(null);
            frameJuego.setVisible(true);
            
            // Pasamos el cliente al BucleJuego para que maneje los mensajes en vivo
            BucleJuego bucle = new BucleJuego(panelJuego, cliente);
            bucle.iniciar();
            
            panelJuego.requestFocus();
        });
    }
    
    private void cargarIconoVentana() {
        try {
            String n = "icono.jpg"; // Nombre del archivo de icono
            Image icono = null; // Variable para almacenar icono
            //  Buscar en classpath
            java.net.URL u = getClass().getClassLoader().getResource(n); // Busca recurso
            if (u != null) icono = ImageIO.read(u); // Lee imagen desde URL
            else {
                //  Buscar en disco
                String[] rutas = {"src/main/resources/" + n, "resources/" + n, n}; // Rutas posibles
                for (String r : rutas) {
                    File f = new File(r); // Crea objeto File
                    if (f.exists()) { // Si el archivo existe
                        icono = ImageIO.read(f); // Lee imagen desde archivo
                        break; // Sale del bucle
                    }
                }
            }
            // Importante: Si encontramos la imagen, la aplicamos
            if (icono != null) {
                setIconImage(icono); // Establece icono de la ventana
                System.out.println("Icono de sala de espera cargado correctamente.");
            } else {
                System.err.println("No se encontró el archivo del icono en la sala de espera.");
            }
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
    
    private boolean soyImpostor = false; // Guardará el rol asignado por el servidor

    @Override
    public void onMensajeRecibido(String mensaje) {
        // Procesar mensajes del servidor
        if (mensaje.startsWith("LISTA_JUGADORES:")) {
            // Extraer lista de jugadores
            String listaStr = mensaje.substring(16); // Después de "LISTA_JUGADORES:"
            
            if (listaStr.isEmpty()) {
                return; // No hay jugadores
            }
            
            String[] nombres = listaStr.split(",");
            
            // Actualizar lista en el hilo de Swing (thread-safe)
            SwingUtilities.invokeLater(() -> {
                jugadoresConectados.clear();
                for (String nombre : nombres) {
                    if (nombre != null && !nombre.trim().isEmpty()) {
                        jugadoresConectados.add(nombre.trim());
                    }
                }
                actualizarListaJugadores();
            });
        } else if (mensaje.startsWith("ROL:")) {
            // El servidor nos susurra al oído qué rol nos tocó
            soyImpostor = mensaje.substring(4).equals("IMPOSTOR");
            System.out.println("El servidor me asignó el rol de: " + (soyImpostor ? "Impostor" : "Tripulante"));
            
        } else if (mensaje.startsWith("JUEGO_INICIADO")) {
            // ¡Arranca la partida! 
            
            // Extraer el mapa elegido (ej. JUEGO_INICIADO:mapa2.png)
            String mapaElegido = "mapa1.png";
            if (mensaje.contains(":")) {
                mapaElegido = mensaje.split(":")[1];
            }
            
            // 0. Creamos el mapa con la imagen seleccionada
            com.amongus.project.modelo.Mapa mapa = new com.amongus.project.modelo.Mapa(mapaElegido);
            EstadoJuego.getInstancia().setMapa(mapa);
            
            // 1. Configuramos a nuestro propio personaje con el rol recibido
            Jugador jugadorLocal = new Jugador(this.nombreJugador, 100, 100, Color.RED, soyImpostor);
            jugadorLocal.setClienteRed(this.cliente); // Le damos el cable de internet
            EstadoJuego.getInstancia().setJugadorLocal(jugadorLocal);
            
            // 2. Creamos a los demás jugadores que vimos en la lista de espera
            Color[] coloresDisponibles = {Color.BLUE, Color.GREEN, Color.YELLOW, Color.CYAN, Color.MAGENTA, Color.ORANGE, Color.PINK, Color.WHITE};
            int indexColor = 0;
            
            for (String nombre : jugadoresConectados) {
                // No nos volvemos a crear a nosotros mismos
                if (!nombre.equals(this.nombreJugador)) {
                    // Ponemos a los demás como Tripulantes (porque los roles son secretos)
                    Jugador otro = new Jugador(nombre, 150 + (indexColor * 50), 100, coloresDisponibles[indexColor % coloresDisponibles.length], false);
                    EstadoJuego.getInstancia().agregarJugador(otro);
                    indexColor++;
                }
            }
            
            // 3. CAMBIAMOS LA FASE DEL JUEGO PARA QUE SE PUEDAN MOVER (ESTO CORRIGE EL ERROR DE MOVIMIENTO)
            EstadoJuego.getInstancia().setFaseActual(EstadoJuego.Fase.JUGANDO);
            
            // El host inició la partida, cambiamos de ventana
            SwingUtilities.invokeLater(() -> abrirJuego());
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
