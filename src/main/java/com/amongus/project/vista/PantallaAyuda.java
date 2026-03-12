package com.amongus.project.vista;  // Paquete que organiza clases relacionadas con la interfaz

import javax.swing.*;            // Componentes de interfaz gráfica (JFrame, JPanel, etc.)
import javax.imageio.ImageIO;    // Para leer archivos de imagen
import java.awt.*;               // Componentes base de AWT (Graphics, Color, Font)

/**
 * PantallaAyuda
 * Muestra las reglas del juego.
 * Tiene un fondo espacial con los personajes y un cuadro de texto  encima.
 */
public class PantallaAyuda extends JFrame {

    /**
     * Constructor: Prepara toda la interfaz gráfica.
     */
    public PantallaAyuda() { // Constructor que se ejecuta al crear instancia
        // Título de la ventana en la barra superior.
        setTitle("Ayuda del Juego");// Establece texto en la barra de título
        // Tamaño fijo (600 ancho, 400 alto).
        setSize(600, 400);// Define dimensiones de ventana: ancho x alto
        // Al cerrar, solo cerramos esta ventana, no el programa entero.
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);// Cierra solo esta ventana
        // Aparecer en el centro de la pantalla.
        setLocationRelativeTo(null);// Centra ventana respecto a pantalla
        
        //Carga del Icono de la ventana (La carita en la barra superior)
        try {
            Image imagenIcono = null;            // Variable para almacenar icono
            String nombreIcono = "icono.jpg";    // Nombre del archivo de icono
            
            // Buscar en recursos del JAR.
            java.net.URL urlIcono = getClass().getClassLoader().getResource(nombreIcono);
            if (urlIcono != null) { // Si encuentra recurso en classpath
                imagenIcono = ImageIO.read(urlIcono);// Lee imagen desde URL
            } else {
                // Buscar en carpetas físicas (modo desarrollo).
                String[] rutas = {"src/main/resources/" + nombreIcono, "resources/" + nombreIcono, nombreIcono};
                for (String r : rutas) {  // Itera por cada ruta posible
                    java.io.File f = new java.io.File(r);  // Crea objeto File
                    if (f.exists()) {  // Si el archivo existe físicamente
                        imagenIcono = ImageIO.read(f);  // Lee imagen desde archivo
                        break;  // Sale del bucle al encontrar
                    }
                }
            }
            // Si encontramos icono, lo seteamos.
            if (imagenIcono != null) setIconImage(imagenIcono);// Asigna icono a ventana
        }  catch (Exception e) {
            System.out.println("No se pudo cargar el icono: " + e.getMessage());  // Error
            }

        // Panel Principal con Imagen de Fondo
        // Creamos un JPanel "especial" (anónimo) que sabe dibujarse a sí mismo.
        JPanel panelPrincipal = new JPanel(new BorderLayout(10, 10)) { // BorderLayout con gaps
            private Image imagenFondo;  // Variable para imagen de fondo
            
            // Este bloque {} se ejecuta al crear el panel. Carga la imagen de fondo.
            {
                try {
                    String nombreArchivo = "imagenAyuda.jpg";// Nombre imagen ayuda
                    // Buscamos la imagen igual que el icono.
                    java.net.URL urlFondo = getClass().getClassLoader().getResource(nombreArchivo);
                    
                    if (urlFondo != null) { // Si está en classpath
                        imagenFondo = ImageIO.read(urlFondo);
                    } else {
                        // Rutas alternativas para desarrollo
                        String[] rutasPosibles = {
                            "src/main/resources/" + nombreArchivo,
                            "src/main/java/resources/" + nombreArchivo,
                            "resources/" + nombreArchivo,
                            nombreArchivo
                        };
                        for (String ruta : rutasPosibles) {  // Recorre rutas
                            java.io.File archivo = new java.io.File(ruta); // Archivo físico
                            if (archivo.exists()) {  // Si existe
                                imagenFondo = ImageIO.read(archivo);  // Lee imagen
                                break;  // Termina búsqueda
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error cargando fondo de ayuda: " + e.getMessage());
                }
            }

            // este es el centro de todo lo del dibujo.
            @Override
            protected void paintComponent(Graphics g) { // Método que dibuja el componente

                super.paintComponent(g);// Llama pintado base de JPanel
                // se Dibuja la foto del espacio.
                if (imagenFondo != null) {// Si hay imagen cargada
                    // Dibuja imagen escalada a tamaño del panel
                    g.drawImage(imagenFondo, 0, 0, getWidth(), getHeight(), this);
                } else {
                    // Si no hay foto, fondo azul oscuro.
                    g.setColor(new Color(30, 30, 50));  // Color RGB personalizado
                    g.fillRect(0, 0, getWidth(), getHeight());  // Rellena todo el panel
                }

                //  se Dibuja un velo negro semitransparente encima.
                // Esto es para que las letras blancas se lean bien sobre la foto.
                // Alpha 180 = Bastante oscuro.
                g.setColor(new Color(0, 0, 0, 171));  // Negro con transparencia (180/255)
                g.fillRect(0, 0, getWidth(), getHeight());  // Rellena con capa semitransparente
            }
        };
        
        // Borde invisible para separar contenido de los bordes.
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));// Márgenes

        // Titulo
        JLabel etiquetaTitulo = new JLabel("Reglas de Among Us", SwingConstants.CENTER);
        etiquetaTitulo.setFont(new Font("Arial", Font.BOLD, 28));  // Fuente grande y negrita
        etiquetaTitulo.setForeground(Color.WHITE);  // Texto blanco
        panelPrincipal.add(etiquetaTitulo, BorderLayout.NORTH);  // Añade en zona norte

        // area de texto con las reglas
        JTextArea areaReglas = new JTextArea();//area de texto para reglas
        areaReglas.setEditable(false); // No se puede escribir encima.
        areaReglas.setOpaque(false);   // Transparente para ver el fondo.
        areaReglas.setBackground(new Color(0, 0, 0, 0));  // Color completamente transparente
        areaReglas.setFont(new Font("Arial", Font.BOLD, 16)); // Fuente para reglas
        areaReglas.setForeground(Color.WHITE); // Texto blanco
        areaReglas.setLineWrap(true);   // Ajustar lineas al ancho.
        areaReglas.setWrapStyleWord(true);  // No cortar palabras a la mitad.
        areaReglas.setText(obtenerReglasDelJuego()); // Texto largo.
        
        // ScrollPane para poder bajar si el texto es largo.
        JScrollPane panelConScroll = new JScrollPane(areaReglas);// Envuelve en scroll
        panelConScroll.setOpaque(false);// ScrollPane transparente
        panelConScroll.getViewport().setOpaque(false); // Fondo del visor transparente.
        panelConScroll.setBorder(null); // Sin borde feo.
        
        // PERSONALIZACIÓN DE BARRA DE DESPLAZAMIENTO (SCROLLBAR) - ESTILO NEGRO
        panelConScroll.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
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
        
        panelPrincipal.add(panelConScroll, BorderLayout.CENTER);// Añade en centro

        //  Botón Cerrar Personalizado
        // Panel transparente para contener el botón.
        JPanel panelBoton = new JPanel();  // Panel para botón
        panelBoton.setOpaque(false);  // Transparente
        
        // Se Crea el botón con estilo "Among Us" (Negro con borde blanco).
        JButton botonCerrar = new JButton("Cerrar") { // Clase anónima para botón personalizado
            @Override
            protected void paintComponent(Graphics g) { // Sobrescribe dibujo del botón
                Graphics2D g2 = (Graphics2D) g.create();// Crea copia de Graphics
                // Suavizado de bordes (Antialiasing).
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Color al presionar o pasar el mouse.
                if (getModel().isPressed())
                    g2.setColor(new Color(30, 30, 30));  // Oscuro al presionar
                else if (getModel().isRollover())
                    g2.setColor(new Color(60, 60, 60));  // Gris al pasar mouse
                else
                    g2.setColor(Color.BLACK);  // Negro normal
                
                // Fondo redondeado.
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
                
                // Borde blanco.
                g2.setColor(Color.WHITE);// Color del borde
                g2.setStroke(new BasicStroke(3));// Grosor de línea
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 30, 30);
                g2.dispose();// Libera recursos de Graphics2D
                super.paintComponent(g);// Dibuja texto del botón
            }
        };
        
        // se Configura la fuente y cursor del boton.
        botonCerrar.setFont(cargarFuentePersonalizada(20f));  // Fuente personalizada
        botonCerrar.setForeground(Color.WHITE);  // Texto blanco
        botonCerrar.setPreferredSize(new Dimension(150, 50));  // Tamaño preferido
        botonCerrar.setContentAreaFilled(false);  // Sin fondo por defecto de Swing
        botonCerrar.setBorderPainted(false);  // Sin borde por defecto
        botonCerrar.setFocusPainted(false);  // Sin efecto de foco
        botonCerrar.setCursor(new Cursor(Cursor.HAND_CURSOR));  // Cursor de mano
        
        botonCerrar.addActionListener(e -> dispose()); // Acción de cerrar.

        panelBoton.add(botonCerrar);  // Añade botón al panel
        panelPrincipal.add(panelBoton, BorderLayout.SOUTH);  // Añade panel en zona sur

        add(panelPrincipal);// Añade panel principal a la ventana
    }


    //Devuelve el texto largo de las reglas.
    private String obtenerReglasDelJuego() { // Método que retorna texto de reglas
        return "OBJETIVO DEL JUEGO:\n" +
               "Tripulantes: Completar todas las tareas asignadas o descubrir y expulsar a todos los impostores.\n" +
               "Impostores: Anular a suficientes tripulantes para que su número sea igual al de los impostores, o sabotear con éxito un sistema crítico de la nave.\n\n" +
               "DINÁMICA:\n" +
               "1. Inicio de Partida: Se juega en un mapa con varias salas y misiones. A cada jugador se le asigna un rol (Tripulante o Impostor) de forma secreta.\n\n" +
               "2. Tareas: Los tripulantes deben moverse por el mapa para completar sus misiones. Los impostores pueden fingir hacer tareas.\n\n" +
               "3. Anulación y Reporte: Los impostores pueden anular a los tripulantes. Si un jugador encuentra un cuerpo, puede reportarlo, lo que inicia una fase de votación.\n\n" +
               "4. Votación: Todos los jugadores discuten y votan por quién creen que es el impostor. El jugador con más votos es expulsado de la nave. Si no hay consenso, nadie es expulsado.\n\n" +
               "5. Reuniones de Emergencia: Cualquier jugador puede convocar una reunión de emergencia desde una sala específica para iniciar una votación sin que haya un cuerpo.\n\n" +
               "6. Sabotaje y Vías de Acceso: Los impostores pueden sabotear sistemas de la nave para crear caos (apagar luces, etc.) y usar conductos de ventilación para moverse rápidamente y en secreto.";
    }


     //Carga la fuente que esta en el archivo.
    private Font cargarFuentePersonalizada(float tamaño) {
        try {
            String nombreArchivo = "InYourFaceJoffrey.ttf";  // Nombre archivo fuente
            String rutaRelativa = "in_your_face_joffrey/" + nombreArchivo;  // Ruta dentro de proyecto

            // Intenta cargar desde classpath (dentro del JAR)
            java.io.InputStream is = getClass().getClassLoader().getResourceAsStream(rutaRelativa);

            if (is == null) {  // Si no está en classpath
                // Si falla classpath, buscamos rutas físicas.
                String[] rutas = {"src/main/resources/" + rutaRelativa, "resources/" + rutaRelativa};
                for (String r : rutas) {
                    java.io.File f = new java.io.File(r);  // Archivo físico
                    if (f.exists())
                        return Font.createFont(Font.TRUETYPE_FONT, f).deriveFont(tamaño);
                }
                return new Font("Arial", Font.BOLD, (int)tamaño);  // Fuente por defecto
            }
            return Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(tamaño);  // Crea desde InputStream
        } catch (Exception e) {
            return new Font("Arial", Font.BOLD, (int)tamaño);  // Fuente por defecto si hay error
        }
    }
}

