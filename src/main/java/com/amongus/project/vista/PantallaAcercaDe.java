package com.amongus.project.vista;  // Define el paquete donde se ubica la clase

import javax.swing.*;            // Importa clases de Swing para interfaz gráfica
import javax.imageio.ImageIO;    // Para leer imágenes (JPEG, PNG, etc.)
import java.awt.*;               // Para componentes AWT (ventanas, colores, fuentes)


/**
 * PantallaAcercaDe
 * Muestra los créditos y tecnologías usadas.
 * Hereda el diseño visual de la Pantalla de Ayuda.
 */
public class PantallaAcercaDe extends JFrame {// Clase que representa una ventana (JFrame)

    public PantallaAcercaDe() {  // Constructor de la ventana
        setTitle("Acerca de");   // Establece el título de la ventana
        setSize(450, 450); // Define tamaño: 450x450 píxeles, un poco más alto para que quepa todo.
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);// Cierra solo esta ventana, no toda la app
        setLocationRelativeTo(null);// Centra la ventana en la pantalla
        
        // Carga del Icono
        try {
            Image imagenIcono = null;           // Variable para almacenar el icono
            String nombreIcono = "icono.jpg";   // Nombre del archivo del icono

            // Intenta cargar el icono desde el classpath (dentro del JAR)
            java.net.URL urlIcono = getClass().getClassLoader().getResource(nombreIcono);

            if (urlIcono != null) {  // Si lo encuentra en el classpath
                imagenIcono = ImageIO.read(urlIcono);  // Lee la imagen
            } else {
                // Si no está en classpath, busca en rutas alternativas
                String[] rutas = {"src/main/resources/" + nombreIcono, "resources/" + nombreIcono, nombreIcono};
                for (String r : rutas) {
                    java.io.File f = new java.io.File(r);  // Crea objeto File
                    if (f.exists()) {  // Si el archivo existe
                        imagenIcono = ImageIO.read(f);  // Lee la imagen
                        break;  // Sale del bucle
                    }
                }
            }
            if (imagenIcono != null) setIconImage(imagenIcono);  // Establece el icono de ventana
        } catch (Exception e) {
            System.out.println("Error cargando icono: " + e.getMessage());  // Mensaje para manejo de error
        }

        //Panel Principal con Fondo
        // Crea un panel personalizado con BorderLayout y fondo de imagen
        JPanel panelPrincipal = new JPanel(new BorderLayout()) {
            private Image imagenFondo;// Variable para la imagen de fondo

            // Bloque de inicialización (se ejecuta al crear el panel)
            {
                try {
                    String nombreImagen = "AcercaDeImagen.jpeg";  // Nombre de imagen de fondo

                    // Intenta cargar desde classpath
                    java.net.URL urlFondo = getClass().getClassLoader().getResource(nombreImagen);
                    if (urlFondo != null) {
                        imagenFondo = ImageIO.read(urlFondo);
                    } else {
                        // Rutas alternativas
                        String[] rutasPosibles = {
                                "src/main/resources/" + nombreImagen, "resources/" + nombreImagen, nombreImagen
                        };
                        for (String ruta : rutasPosibles) {
                            java.io.File archivo = new java.io.File(ruta);
                            if (archivo.exists()) {
                                imagenFondo = ImageIO.read(archivo);
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error fondo acerca de: " + e.getMessage());// Error más detallado
                }
            }

            @Override
            protected void paintComponent(Graphics g) {
                // Llama al metodo original

                // Dibuja la imagen de fondo si existe
                if (imagenFondo != null) {
                    g.drawImage(imagenFondo, 0, 0, getWidth(), getHeight(), this);
                } else {
                    //Fondo negro si no hay imagen
                    g.setColor(Color.BLACK);
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
                // Capa oscura semitransparente (180/255 de opacidad)
                g.setColor(new Color(0, 0, 0, 180));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        // Cambia el layout del panel a BoxLayout vertical
        panelPrincipal.setLayout(new BoxLayout(panelPrincipal, BoxLayout.Y_AXIS));
        // Añade márgenes internos de 20 píxeles en todos los lados
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // area de Texto unificada
        JTextArea areaInfo = new JTextArea();  // Área de texto no editable
        areaInfo.setEditable(false);           // No se puede editar
        areaInfo.setOpaque(false);             // Fondo transparente
        areaInfo.setBackground(new Color(0,0,0,0));  // Color completamente transparente
        areaInfo.setForeground(Color.WHITE);   // Texto blanco
        areaInfo.setFont(new Font("Arial", Font.BOLD, 14));  // Fuente Arial negrita 14px
        areaInfo.setLineWrap(true);            // Ajuste de líneas automático
        areaInfo.setWrapStyleWord(true);       // Ajuste por palabras completas

        // Texto con información del proyecto
        String textoContenido = 
            "PROYECTO SIMULACIÓN AMONG US\n\n" +
            "Asignatura: Técnicas de Programación III\n" +
            "Profesora: Jannelly Bello\n" +
            "Versión: 1.0.0\n\n" +
            "Tecnologías Utilizadas:\n" +
            "• Lenguaje: Java\n" +
            "• IDE: Apache NetBeans\n" +
            "• UI: Java Swing\n" +
            "• Persistencia: XML\n\n" +
            "DESARROLLADORES (EQUIPO):\n" +
            "• Abraham Orta\n" +
            "• Samuel Silva\n" +
            "• Cristobal Requena\n" +
            "• Jorge Bravo\n";
            
        areaInfo.setText(textoContenido);  // Establece el texto
        panelPrincipal.add(areaInfo);      // Añade el área de texto al panel
        
        panelPrincipal.add(Box.createVerticalStrut(20));  //Espacio vertical de 20 píxeles

        //Boton de cerrar pero personalizado
        JPanel panelBoton = new JPanel();  // Panel para contener el botón
        panelBoton.setOpaque(false);       // Panel transparente

        // Crea el boton personalizado con graficos dibujados
        JButton botonCerrar = new JButton("Cerrar") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();  // Crea copia de Graphics
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // Suaviza bordes
                // Colores según estado del botón
                if (getModel().isPressed())
                    g2.setColor(new Color(30, 30, 30));     // Al presionar
                else if (getModel().isRollover())
                    g2.setColor(new Color(60, 60, 60));     // Al pasar mouse
                else
                    g2.setColor(Color.BLACK);// Normal
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30); // Fondo redondeado
                g2.setColor(Color.WHITE);// Borde blanco
                g2.setStroke(new BasicStroke(3));  // Grosor 3 píxeles
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 30, 30);
                g2.dispose();  // Libera recursos
                super.paintComponent(g);  // Dibuja el texto "Cerrar
            }
        };

        // Configuración del botón
        botonCerrar.setFont(cargarFuentePersonalizada(20f));  // Fuente personalizada tamaño 20
        botonCerrar.setForeground(Color.WHITE);// Texto blanco
        botonCerrar.setPreferredSize(new Dimension(150, 50)); // Tamaño preferido
        botonCerrar.setContentAreaFilled(false);  // Sin fondo por defecto
        botonCerrar.setBorderPainted(false); // Sin borde por defecto
        botonCerrar.setFocusPainted(false); // Sin efecto de foco
        botonCerrar.setCursor(new Cursor(Cursor.HAND_CURSOR)); // Cursor de mano

        botonCerrar.addActionListener(e -> dispose());// Cierra la ventana al hacer clic
        panelBoton.add(botonCerrar);// Añade botón al panel

        panelPrincipal.add(panelBoton);  // Añade panel del botón al panel principal

        // Scroll Pane (para contenido desplazable)
        JScrollPane scroll = new JScrollPane(panelPrincipal);// Envuelve el panel en scroll
        scroll.setBorder(null);// Sin borde
        scroll.getViewport().setOpaque(false); // Viewport transparente
        scroll.setOpaque(false);// ScrollPane transparente

        add(scroll);// Añade el ScrollPane a la ventana (JFrame)
    }

    // Metodo para cargar fuentes personalizadas desde archivos TTF
    private Font cargarFuentePersonalizada(float tamaño) {
        try {
            String nombreArchivo = "InYourFaceJoffrey.ttf"; // Nombre del archivo de fuente
            String rutaRelativa = "in_your_face_joffrey/" + nombreArchivo;// Ruta relativa

            // Intenta cargar desde classpath (dentro del JAR)
            java.io.InputStream is = getClass().getClassLoader().getResourceAsStream(rutaRelativa);

            if (is == null) { // Si no está en classpath
                String[] rutas = {"src/main/resources/" + rutaRelativa, "resources/" + rutaRelativa};
                for (String r : rutas) {
                    java.io.File f = new java.io.File(r);
                    if (f.exists())
                        // Crea la fuente desde el archivo
                        return Font.createFont(Font.TRUETYPE_FONT, f).deriveFont(tamaño);
                }
                // Fuente por defecto si no se encuentra
                return new Font("Arial", Font.BOLD, (int)tamaño);
            }
            // Crea fuente desde InputStream (cuando está en JAR)
            return Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(tamaño);
        } catch (Exception e) {
            // Fuente por defecto en caso de error
            return new Font("Arial", Font.BOLD, (int)tamaño);
        }
    }
}
