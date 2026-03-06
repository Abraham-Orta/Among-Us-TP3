package com.amongus.project.vista; // Paquete de la interfaz gráfica


import javax.swing.*; // Componentes Swing (JFrame, JPanel, JButton, etc.)
import java.awt.*; // Componentes AWT (Color, Font, Graphics, etc.)
import java.io.File; // Para manejo de archivos del sistema
import java.io.InputStream; // Para leer flujos de datos
import javax.imageio.ImageIO; // Para leer imágenes (JPG, PNG, etc.)
import com.amongus.project.controlador.BucleJuego; // Logica del bucle de juego
import com.amongus.project.vista.PanelJuego; // Panel donde se dibuja el juego
import com.amongus.project.red.Cliente; // Cliente de red

/**
 * PantallaSeleccionModo
 * Pantalla para elegir entre modo Local o En Línea.
 */
public class PantallaSeleccionModo extends JFrame { // Hereda de JFrame = ventana principal

    /**
     * Constructor: Configura la ventana al iniciar.
     */
    private static boolean servidorIniciado = false; // Control para no abrir el servidor dos veces

    /**
     * Constructor: Configura la ventana al iniciar.
     */
    public PantallaSeleccionModo() {
        // configuaracion de ventana
        setTitle("Seleccionar Modo de Juego"); // Titulo superior de la ventana
        setSize(900, 700); // Tamaño de la ventana (ancho x alto en píxeles)
        setLocationRelativeTo(null); // Centrar en pantalla
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Cierra solo esta ventana al salir

        // icono
        // Cargamos el icono antes de mostrar nada más
        cargarIconoVentana(); // Llama al metodo para cargar el icono

        // fondo
        // Creamos el panel que contiene la imagen de fondo
        JPanel panelFondo = crearPanelConFondo(); // Crea panel con fondo personalizado
        panelFondo.setLayout(new BorderLayout()); // Diseño con 5 zonas (Norte/Centro/Sur/Este/Oeste)

        // titulo para la seleccion
        // Etiqueta de texto superior
        JLabel etiquetaTitulo = new JLabel("Selecciona el Modo", SwingConstants.CENTER); // Texto centrado
        etiquetaTitulo.setFont(cargarFuente(50f)); // Fuente personalizada tamaño 50
        etiquetaTitulo.setForeground(Color.WHITE); // Color blanco para el texto
        etiquetaTitulo.setBorder(BorderFactory.createEmptyBorder(30, 0, 10, 0)); // Margen (arriba, izq, abajo, der)

        // Agrega el titulo al panel fondo
        panelFondo.add(etiquetaTitulo, BorderLayout.NORTH); // Añade en zona norte del BorderLayout

        // opciones en el centro
        // Panel transparente para agrupar las tres opciones
        JPanel panelCentral = new JPanel(); // Panel para contener las opciones
        panelCentral.setOpaque(false); // Hace el panel transparente
        panelCentral.setLayout(new GridBagLayout()); // Layout para centrado flexible

        // parte de la izquierda (local)
        JPanel panelLocal = new JPanel(); // Panel para opción Local
        panelLocal.setOpaque(false); // Transparente
        panelLocal.setLayout(new BoxLayout(panelLocal, BoxLayout.Y_AXIS)); // Disposición vertical

        // Imagen Local (Reducida para que quepan las 3 opciones)
        JLabel imgLocal = crearEtiquetaImagen("imagenLocal.png", 240, 160); // Crea etiqueta con imagen
        imgLocal.setAlignmentX(Component.CENTER_ALIGNMENT); // Centra horizontalmente

        // Botón Local
        JButton btnLocal = crearBotonEstiloAmongUs("Local"); // Crea botón con texto "Local"
        btnLocal.setAlignmentX(Component.CENTER_ALIGNMENT); // Centra horizontalmente

        // Agregar elementos Local
        panelLocal.add(imgLocal); // Añade imagen
        panelLocal.add(Box.createVerticalStrut(20)); // Espacio vertical de 20 píxeles
        panelLocal.add(btnLocal); // Añade botón

        // parte de la derecha (en linea)
        JPanel panelOnline = new JPanel(); // Panel para opción En Línea
        panelOnline.setOpaque(false); // Transparente
        panelOnline.setLayout(new BoxLayout(panelOnline, BoxLayout.Y_AXIS)); // Disposición vertical

        // Imagen En Linea (Reducida)
        JLabel imgOnline = crearEtiquetaImagen("imagenEnLinea.png", 240, 160); // Crea etiqueta con imagen
        imgOnline.setAlignmentX(Component.CENTER_ALIGNMENT); // Centra horizontalmente

        // Botón En Linea
        JButton btnOnline = crearBotonEstiloAmongUs("En Linea"); // Crea botón con texto "En Linea"
        btnOnline.setAlignmentX(Component.CENTER_ALIGNMENT); // Centra horizontalmente

        // Agregar elementos Online
        panelOnline.add(imgOnline); // Añade imagen
        panelOnline.add(Box.createVerticalStrut(20)); // Espacio vertical de 20 píxeles
        panelOnline.add(btnOnline); // Añade botón

        // parte de "Unirse a Partida"
        JPanel panelUnirse = new JPanel(); // Panel para opción Unirse
        panelUnirse.setOpaque(false); // Transparente
        panelUnirse.setLayout(new BoxLayout(panelUnirse, BoxLayout.Y_AXIS)); // Disposición vertical

        // Imagen Unirse (Reducida)
        JLabel imgUnirse = crearEtiquetaImagen("imagenEnLinea.png", 240, 160);
        imgUnirse.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Botón Unirse
        JButton btnUnirse = crearBotonEstiloAmongUs("Unirse a Partida");
        btnUnirse.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Agregar elementos Unirse
        panelUnirse.add(imgUnirse);
        panelUnirse.add(Box.createVerticalStrut(20));
        panelUnirse.add(btnUnirse);

        // aqui para unir los paneles de manera responsiva
        GridBagConstraints gbc = new GridBagConstraints(); // Objeto para configurar posición en GridBagLayout
        gbc.weightx = 1.0; // Distribuye el espacio horizontal equitativamente
        gbc.anchor = GridBagConstraints.CENTER; // Centra los elementos
        gbc.fill = GridBagConstraints.NONE; // No estira los paneles, solo los centra

        // Configuración Izquierda
        gbc.gridx = 0; // Columna 0 (primera columna)
        gbc.gridy = 0; // Fila 0
        gbc.insets = new Insets(0, 10, 0, 10); // Márgenes más pequeños para que quepan bien
        panelCentral.add(panelLocal, gbc); // Añade panel Local

        // Configuración Centro
        gbc.gridx = 1; // Columna 1 (segunda columna)
        panelCentral.add(panelOnline, gbc); // Añade panel Online

        // Configuración Derecha
        gbc.gridx = 2; // Columna 2 (tercera columna)
        panelCentral.add(panelUnirse, gbc); // Añade panel Unirse

        // Agregar al fondo
        panelFondo.add(panelCentral, BorderLayout.CENTER); // Añade panel central en zona centro

        // boton atras abajo
        JPanel panelSur = new JPanel(); // Panel para el botón Atrás
        panelSur.setOpaque(false); // Transparente
        panelSur.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0)); // Margen inferior de 30 píxeles

        JButton botonAtras = crearBotonEstiloAmongUs("Atras"); // Crea boton "Atras"
        // Limpiamos acciones previas y agregamos cerrar
        for(var al : botonAtras.getActionListeners()) botonAtras.removeActionListener(al); // Elimina listeners anteriores
        botonAtras.addActionListener(e -> dispose()); // Cierra la ventana al hacer clic

        panelSur.add(botonAtras); // Añade botón al panel sur
        panelFondo.add(panelSur, BorderLayout.SOUTH); // Añade panel sur en zona sur

        // finalizar
        setContentPane(panelFondo); // Establece panelFondo como contenido principal de la ventana
    }

    // Metodos auxiliares

    /**
     * Carga imagen desde recursos
     */
    private JLabel crearEtiquetaImagen(String nombre, int w, int h) {
        JLabel lbl = new JLabel(); // Crea etiqueta vacía
        try {
            Image img = null; // Variable para almacenar imagen
            // Classpath (recursos dentro del JAR)
            java.net.URL url = getClass().getClassLoader().getResource(nombre); // Busca recurso
            if (url != null) img = ImageIO.read(url); // Lee imagen desde URL
            else {
                //  Rutas físicas (para desarrollo)
                String[] rutas = {"src/main/resources/" + nombre, "resources/" + nombre, nombre}; // Posibles rutas
                for (String r : rutas) {
                    File f = new File(r); // Crea objeto File
                    if (f.exists()) { // Si el archivo existe
                        img = ImageIO.read(f); // Lee imagen desde archivo
                        break; // Sale del bucle
                    }
                }
            }
            // Si cargó, redimensionar y asignar
            if (img != null) {
                img = img.getScaledInstance(w, h, Image.SCALE_SMOOTH); // Redimensiona imagen
                lbl.setIcon(new ImageIcon(img)); // Establece imagen como icono de la etiqueta
            } else {
                lbl.setText("NO IMAGEN"); // Texto alternativo
                lbl.setForeground(Color.RED); // Color rojo para el texto
            }
        } catch (Exception e) { System.err.println("Error img: " + e.getMessage()); } // Manejo de error
        return lbl; // Retorna la etiqueta
    }

    /**
     * Carga y establece el icono de la ventana.
     */
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
                System.out.println("Icono cargado y aplicado correctamente."); // Mensaje de éxito
            } else {
                System.err.println("No se encontró el archivo del icono."); // Mensaje de error
            }
        } catch (Exception e) { System.err.println("Error al poner icono: " + e.getMessage()); } // Manejo de error
    }

    /**
     * Panel con fondo pintado.
     */
    private JPanel crearPanelConFondo() {
        return new JPanel() { // Retorna un JPanel anónimo personalizado
            private Image fondo; // Variable para imagen de fondo
            // Bloque inicializador (se ejecuta al crear el panel)
            {
                try {
                    String n = "fondo.jpg"; // Nombre archivo fondo
                    java.net.URL u = getClass().getClassLoader().getResource(n); // Busca recurso
                    if (u != null) fondo = ImageIO.read(u); // Lee imagen desde URL
                    else {
                        String[] rutas = {"src/main/resources/" + n, "resources/" + n, n}; // Rutas posibles
                        for (String r : rutas) {
                            File f = new File(r); // Crea objeto File
                            if (f.exists()) { // Si el archivo existe
                                fondo = ImageIO.read(f); // Lee imagen desde archivo
                                break; // Sale del bucle
                            }
                        }
                    }
                } catch (Exception e) { System.err.println("Error fondo: " + e.getMessage()); } // Manejo de error
            }

            @Override
            protected void paintComponent(Graphics g) { // Sobrescribe metodo de pintado
                super.paintComponent(g); // Llama pintado original
                if (fondo != null) g.drawImage(fondo, 0, 0, getWidth(), getHeight(), this); // Dibuja fondo escalado
                else {
                    g.setColor(Color.DARK_GRAY); // Color de respaldo
                    g.fillRect(0,0,getWidth(),getHeight()); // Rellena con color
                }
                // Velo oscuro semitransparente
                g.setColor(new Color(0,0,0,100)); // Negro con transparencia (100/255)
                g.fillRect(0,0,getWidth(),getHeight()); // Rellena panel con velo
            }
        };
    }

    /**
     * Carga fuente personalizada o usa Arial como respaldo.
     */
    private Font cargarFuente(float tamano) {
        try {
            String nombre = "InYourFaceJoffrey.ttf"; // Nombre archivo fuente
            String ruta = "in_your_face_joffrey/" + nombre; // Ruta relativa en recursos
            // Intentar cargar flujo desde classpath
            InputStream is = getClass().getClassLoader().getResourceAsStream(ruta); // Obtiene InputStream
            if (is == null) {
                // Intentar cargar archivo físico
                String[] rutas = {"src/main/resources/"+ruta, "resources/"+ruta}; // Rutas posibles
                for (String r : rutas) {
                    File f = new File(r); // Crea objeto File
                    if (f.exists()) return Font.createFont(Font.TRUETYPE_FONT, f).deriveFont(tamano); // Crea fuente desde archivo
                }
                return new Font("Arial", Font.BOLD, (int)tamano); // Fuente por defecto
            }
            return Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(tamano); // Crea fuente desde InputStream
        } catch (Exception e) { return new Font("Arial", Font.BOLD, (int)tamano); } // Fuente por defecto en caso de error
    }

    /**
     * Crea botón estilizado (Negro con borde blanco).
     */
    private JButton crearBotonEstiloAmongUs(String txt) {
        JButton b = new JButton(txt) { // Crea JButton anónimo personalizado
            @Override
            protected void paintComponent(Graphics g) { // Sobrescribe pintado del botón
                Graphics2D g2 = (Graphics2D) g.create(); // Crea copia de Graphics
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // Suaviza bordes

                // Colores según estado del botón
                if (getModel().isPressed()) g2.setColor(new Color(30,30,30)); // Oscuro al presionar
                else if (getModel().isRollover()) g2.setColor(new Color(60,60,60)); // Gris al pasar mouse
                else g2.setColor(Color.BLACK); // Negro normal

                g2.fillRoundRect(0,0,getWidth(),getHeight(),30,30); // Fondo redondeado
                g2.setColor(Color.WHITE); // Color del borde
                g2.setStroke(new BasicStroke(3)); // Grosor del borde
                g2.drawRoundRect(1,1,getWidth()-3,getHeight()-3,30,30); // Dibuja borde redondeado

                g2.dispose(); // Libera recursos
                super.paintComponent(g); // Llama pintado original (para texto)
            }
        };

        // Configuración del botón
        b.setPreferredSize(new Dimension(280, 60)); // Tamaño preferido
        b.setFont(cargarFuente(28f)); // Fuente personalizada tamaño 28
        b.setForeground(Color.WHITE); // Color del texto
        b.setContentAreaFilled(false); // Sin fondo por defecto de Swing
        b.setBorderPainted(false); // Sin borde por defecto
        b.setFocusPainted(false); // Sin efecto de foco
        b.setCursor(new Cursor(Cursor.HAND_CURSOR)); // Cursor de mano al pasar sobre el botón

        // Accion personalizada segun el boton
        if (txt.equals("En Linea")) {
            // Si el boton es el de jugar online, hacemos esto:
            b.addActionListener(e -> {
                //   pregunta el nombre al usuario con una ventanita
                String nombre = JOptionPane.showInputDialog(this, "Ingresa tu nombre de tripulante:");
                
                // Verifica que no haya dado a Cancelar y que no haya dejado el nombre vacio
                if (nombre != null && !nombre.trim().isEmpty()) { 
                    
                    // Crea el cliente de red (el telefono para hablar con el server)
                    com.amongus.project.red.Cliente cliente = new com.amongus.project.red.Cliente();
                    
                    // Esperar un momento para que el cliente se conecte
                    try { Thread.sleep(500); } catch (InterruptedException ex) {}
                    
                    // Abrir lobby y enviar mensaje de conexión
                    abrirLobby(nombre, false, cliente, "CONECTAR:" + nombre);
                }
            });
        } else if (txt.equals("Local")) {
            // modo local
            b.addActionListener(e -> {
                String nombre = JOptionPane.showInputDialog(this, "Nombre del Anfitrion:");
                if (nombre != null && !nombre.trim().isEmpty()) {
                    
                    // En Local, nosotros somos el servidor.
                    // Creamos un hilo para arrancar el servidor en segundo plano
                    if (!servidorIniciado) {
                        new Thread(() -> {
                            try {
                                // Llamamos al main del Servidor
                                com.amongus.project.red.Servidor.main(new String[]{});
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }).start();
                        servidorIniciado = true;
                        
                        // Esperamos a que el servidor arranque bien
                        try { Thread.sleep(1000); } catch (InterruptedException ex) {}
                    }
                    
                    // Esperamos  a que el servidor arranque bien
                    try { Thread.sleep(1000); } catch (InterruptedException ex) {}
                    
                    // nos conectamos a nosotros mismos (localhost)
                    com.amongus.project.red.Cliente cliente = new com.amongus.project.red.Cliente();
                    
                    // Esperar conexión
                    try { Thread.sleep(500); } catch (InterruptedException ex) {}
                    
                    // Abrir lobby y enviar mensaje
                    abrirLobby(nombre + " (Host)", true, cliente, "CONECTAR:" + nombre + " (Host)");
                }
            });
        } else if (txt.equals("Unirse a Partida")) {
            // Unirse a partida existente
            b.addActionListener(e -> {
                // Pedir código de sala
                String codigo = JOptionPane.showInputDialog(this, "Ingresa el código de la sala:");
                
                if (codigo != null && !codigo.trim().isEmpty()) {
                    // Validar formato (6 caracteres alfanuméricos)
                    codigo = codigo.trim().toUpperCase();
                    if (!codigo.matches("[A-Z0-9]{6}")) {
                        JOptionPane.showMessageDialog(this, 
                            "Código inválido. Debe tener 6 caracteres (letras y números).", 
                            "Error", 
                            JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    
                    // Pedir nombre
                    String nombre = JOptionPane.showInputDialog(this, "Ingresa tu nombre de tripulante:");
                    
                    if (nombre != null && !nombre.trim().isEmpty()) {
                        // Conectar al servidor
                        com.amongus.project.red.Cliente cliente = new com.amongus.project.red.Cliente();
                        
                        // Esperar conexión
                        try { Thread.sleep(500); } catch (InterruptedException ex) {}
                        
                        // Abrir lobby y enviar mensaje
                        abrirLobby(nombre, false, cliente, "CONECTAR:" + nombre + " (Código: " + codigo + ")");
                    }
                }
            });
        } else {
            // Accion por defecto para los otros botones (Atras) que aun no tienen logica PARA ABRAHAM
            b.addActionListener(e -> JOptionPane.showMessageDialog(this, "Modo: " + txt)); 
        }
        
        return b; // Retorna el botón creado
    }

    private void abrirLobby(String nombre, boolean esHost, Cliente cliente, String mensajeInicial) {
        // Cierra la ventana de seleccion
        dispose();
        
        // Abre la ventana de lobby
        SwingUtilities.invokeLater(() -> {
            PantallaLobby lobby = new PantallaLobby(nombre, esHost, cliente);
            lobby.setVisible(true);
            
            if (cliente != null && mensajeInicial != null) {
                cliente.enviarMensaje(mensajeInicial);
            }
        });
    }
}