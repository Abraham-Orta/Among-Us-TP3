package com.amongus.project.vista; // Paquete de la interfaz visual

// aquí importamos nuestro Gestor de Datos (Persistencia)
import com.amongus.project.data.GestorDatos; 

// aquí importamos librerías gráficas de Java
import javax.swing.*; 
import java.awt.*; 
import java.io.File; 
import javax.imageio.ImageIO; 

/**
 * esta es la clase MenuPrincipal
 * La puerta de entrada al juego.
 * Muestra el logo, el fondo espacial y los botones para navegar
 * También controla la música.
 */
public class MenuPrincipal extends JFrame {

    // Este es por asi decirlo el "DJ" de nuestra proyecto
    private ReproductorMusica reproductorMusica;


     //Constructor: Aquí construimos toda la ventana pieza por pieza

    public MenuPrincipal() {
        // aqui empieza la parte de la musica
        // Instanciamos el reproductor y le decimos que toque la canción del menú
        reproductorMusica = new ReproductorMusica();
        // Usamos WAV porque es el formato que mejor se lleva con Java, aparte la libreria no puede leer otro formato
        reproductorMusica.reproducirEnBucle("menu_music.wav");

        //  Configuracion basica de la ventana
        setTitle("Among Us - Menú Principal"); // Lo que dice arriba a la izquierda, en la pestaña que se abre
        setSize(800, 600); // Tamaño inicial (Ancho x Alto)
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);//esta orden dice que si cierras la ventana, se acaba el programa
        setLocationRelativeTo(null); // se usa para centrar en la pantalla.

        //configuaracion que se usa para cargar el icono que está en la esquina de la ventana (La carita de Among Us)
        cargarIconoVentana();

        //en esta parte se configura el fondo (El espacio exterior con los muñequitos)
        // Creamos un panel especial que tiene la foto de fondo pintada.
        JPanel panelFondo = crearPanelConFondo();

        // Estructura de contenido
        // Con esto vamos a organizar los botones en una columna vertical
        JPanel panelBotones = new JPanel();
        panelBotones.setLayout(new BoxLayout(panelBotones, BoxLayout.Y_AXIS));
        panelBotones.setOpaque(false); // Transparente para ver las estrellas de fondo

        // Parte del logo
        // aqui se usa un panel inteligente que escala la imagen del logo
        // sin deformarla y sin perder calidad
        JPanel panelLogo = crearPanelLogo();
        panelBotones.add(panelLogo);

        // Un pequeño espacio de 20px antes de los botones
        panelBotones.add(Box.createRigidArea(new Dimension(0, 20)));

        // Parte de los botones
        // Agregamos cada botón con su acción específica.

        // Botón de Jugar
        panelBotones.add(crearBotonMenu("Jugar"));
        panelBotones.add(Box.createRigidArea(new Dimension(0, 15))); // Espacio

        // Botón de Ayuda
        panelBotones.add(crearBotonMenu("Ayuda"));
        panelBotones.add(Box.createRigidArea(new Dimension(0, 15)));

        // Botón de Acerca De
        panelBotones.add(crearBotonMenu("Acerca de"));
        panelBotones.add(Box.createRigidArea(new Dimension(0, 15)));

        // Botón de Salir
        panelBotones.add(crearBotonMenu("Salir"));

        // aqui empujamos todo hacia arriba con un resorte invisible al final
        panelBotones.add(Box.createVerticalGlue());

        // aqui se puede observar el centrado final
        // Usamos GridBagLayout para centrar el panel de botones en toda la ventana
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH; // esto para llenar todo el espacio

        panelFondo.add(panelBotones, gbc);

        // Le decimos a la ventana que su contenido principal es nuestro panel con fondo
        setContentPane(panelFondo);
    }

    // aqui unos metodos auxiliares (Para no ensuciar el constructor)

    private void cargarIconoVentana() {
        try {
            Image imagenIcono = null; // Variable para almacenar la imagen del icono
            String nombre = "icono.jpg";// este es el nombre del archivo del icono

            // log de depuracion
            System.out.println("Busco icono: " + nombre);// Mensaje para depuración

            // Intenta cargar el icono desde el classpath (dentro del JAR)
            java.net.URL url = getClass().getClassLoader().getResource(nombre);

            if (url != null) {
                System.out.println("Icono encontrado en Classpath: " + url);
                imagenIcono = ImageIO.read(url);  // Lee la imagen desde la URL
            } else {
                String[] rutas = {
                        // Si no está en classpath, busca en rutas alternativas
                    "src/main/resources/"+nombre, // Ruta comun
                    "resources/"+nombre,  // Ruta alternativa
                    nombre // Ruta actual
                };
                // Recorre cada ruta posible
                for (String r : rutas) {
                    File f = new File(r);// Crea objeto File
                    System.out.println("Probando ruta fisica: " + f.getAbsolutePath() + " -> Existe: " + f.exists());
                    if (f.exists()) { // Si el archivo existe
                        imagenIcono = ImageIO.read(f); // Lee la imagen del archivo
                        break;  // Sale del bucle al encontrar la imagen
                    }
                }
            }
            if (imagenIcono != null) setIconImage(imagenIcono);// Establece el icono de la ventana
        } catch (Exception e) {
            System.out.println("Icono no encontrado: " + e.getMessage());// Mensaje usado para menejar un errores
        }
    }

    private JPanel crearPanelConFondo() {
        return new JPanel(new GridBagLayout()) { // Crea panel con layout GridBagLayout
            private Image imagenFondo;// Variable para la imagen de fondo
            // Inicializador del bloque, se ejecuta al crear el panel
            {
                try {
                    String nombre = "fondo.jpg";
                    System.out.println("Busco fondo: " + nombre); // LOG

                    // Similar al método anterior: busca en classpath primero
                    java.net.URL url = getClass().getClassLoader().getResource(nombre);
                    if (url != null) {
                        System.out.println("Fondo encontrado en Classpath: " + url);
                        imagenFondo = ImageIO.read(url);
                    } else {
                        // Rutas alternativas para desarrollo
                        String[] rutas = {
                            "src/main/resources/" + nombre,
                            "resources/" + nombre
                        };
                        for (String r : rutas) {
                             File f = new File(r);
                             System.out.println("Probando ruta fondo: " + f.getAbsolutePath() + " -> " + f.exists());
                             if (f.exists()) {
                                 imagenFondo = ImageIO.read(f);
                                 break;
                             }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error cargando fondo: " + e.getMessage());
                }
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);// Llama al metodo original
                // Pinta un fondo oscuro como respaldo
                g.setColor(new Color(20, 20, 40)); // Color azul oscuro
                g.fillRect(0, 0, getWidth(), getHeight());// Rellena todo el panel

                // Si hay imagen de fondo, la dibuja ajustada al tamaño del panel
                if (imagenFondo != null) {
                    g.drawImage(imagenFondo, 0, 0, getWidth(), getHeight(), this);
                }
            }
        };
    }

    private JPanel crearPanelLogo() {
        Image imagenRaw = null;// Variable temporal para el logo
        try {
            String nombre = "logo.png";
            System.out.println("Busco logo: " + nombre); // LOG

            // Busca el logo similar a los metodos anteriores
            java.net.URL url = getClass().getClassLoader().getResource(nombre);
            if (url != null) {
                System.out.println("Logo encontrado en Classpath");
                imagenRaw = ImageIO.read(url);
            } else {
                // Rutas alternativas
                 String[] rutas = {
                    "src/main/resources/" + nombre,
                    "resources/" + nombre
                };
                 for (String r : rutas) {
                     File f = new File(r);
                     System.out.println("Probando ruta logo: " + f.getAbsolutePath() + " -> " + f.exists());
                     if (f.exists()) {
                         imagenRaw = ImageIO.read(f);
                         break;
                     }
                 }
            }
        } catch (Exception e) {
            e.printStackTrace(); // Imprime error completo
        }

        final Image logoFinal = imagenRaw;// Variable final para usar en clase anonima

        JPanel panel = new JPanel() { // Crea panel personalizado
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (logoFinal != null) {
                    // Matemáticas para mantener la proporción, basicamente se calcula las dimensiones manteniendo proporción (Aspect Ratio)
                    int wPanel = getWidth(); // Ancho del panel
                    int hPanel = getHeight();// Alto del panel
                    int wImg = logoFinal.getWidth(this);// Ancho original imagen
                    int hImg = logoFinal.getHeight(this);// Ancho original imagen


                    if (wImg > 0 && hImg > 0) {
                        double ratioPanel = (double) wPanel / hPanel;// Proporción panel
                        double ratioImg = (double) wImg / hImg; // Proporción imagen

                        int drawW, drawH; // Dimensiones finales de dibujo

                        // Si el panel es más "ancho" que la imagen, limitamos por altura, basicamente ajusta según qué lado limita
                        if (ratioPanel > ratioImg) {
                            // Panel más ancho: limita por altura
                            drawH = hPanel;
                            drawW = (int) (hPanel * ratioImg);
                        } else {
                            // Si el panel es más "alto", limitamos por ancho
                            drawW = wPanel;
                            drawH = (int) (wPanel / ratioImg);
                        }

                        // Coordenadas para centrar, calcula las coordenadas para centrar
                        int x = (wPanel - drawW) / 2;
                        int y = (hPanel - drawH) / 2;

                        // Dibujamos con calidad alta, se usa Graphics2D para mejor calidad
                        Graphics2D g2d = (Graphics2D) g;
                        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        g2d.drawImage(logoFinal, x, y, drawW, drawH, this);
                    }
                } else {
                    // Texto de respaldo si no hay imagen
                    g.setColor(Color.WHITE);
                    g.setFont(new Font("Arial", Font.BOLD, 40));
                    String txt = "AMONG US";
                    FontMetrics fm = g.getFontMetrics();  // Para centrar texto
                    g.drawString(txt, (getWidth()-fm.stringWidth(txt))/2, getHeight()/2);
                }
            }
        };

        // Configura propiedades del panel
        panel.setOpaque(false); // Hace panel transparente
        panel.setPreferredSize(new Dimension(800, 300));// Tamaño preferido
        panel.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));// Tamaño maximo
        return panel;
    }

    // aqui se Fabrican los botones personalizados.

    private JButton crearBotonMenu(String texto) {
        // Crea botón personalizado con gráficos personalizados
        JButton boton = new JButton(texto) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();// Copia de Graphics para no afectar original
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // Suaviza bordes

                // Colores interactivos segun estado del boton
                if (getModel().isPressed()) g2.setColor(new Color(30, 30, 30)); // Color al hacer click
                else if (getModel().isRollover()) g2.setColor(new Color(60, 60, 60)); //  Color al pasar mouse
                else g2.setColor(Color.BLACK); //  Color normal

                //Dibuja fondo redondeado (capsula)
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);

                // Dibuja borde blanco
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(3)); // Grosor del borde
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 30, 30);

                g2.dispose();// Libera recursos
                super.paintComponent(g);// Pinta el texto del botón
            }
        };

        // Estilos del boton
        boton.setMaximumSize(new Dimension(450, 70));     // Tamaño maximo
        boton.setPreferredSize(new Dimension(350, 60));   // Tamaño preferido
        boton.setFont(cargarFuente(28f));                 // Fuente personalizada
        boton.setForeground(Color.WHITE);                 // Color del texto
        boton.setAlignmentX(Component.CENTER_ALIGNMENT);  // Centrado horizontal

        // Limpieza de estilos Swing,basicamente elimina estilos por defecto de Swing
        boton.setContentAreaFilled(false);  // Sin fondo por defecto
        boton.setBorderPainted(false);      // Sin borde por defecto
        boton.setFocusPainted(false);       // Sin efecto de foco
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR));  // Cursor de mano

        // Acciones (Eventos), asigna acciones según el texto del botón
        switch (texto) {
            case "Jugar":
                // Al presionar Jugar, abrimos la pantalla intermedia de selección (Local / Online)
                boton.addActionListener(e -> new PantallaSeleccionModo().setVisible(true));
                break;
            case "Ayuda":
                // Aquí abrimos la nueva clase PantallaAyuda
                boton.addActionListener(e -> new PantallaAyuda().setVisible(true));
                break;
            case "Acerca de":
                // Aquí abrimos la nueva clase PantallaAcercaDe
                boton.addActionListener(e -> new PantallaAcercaDe().setVisible(true));
                break;
            case "Salir":
                boton.addActionListener(e -> System.exit(0));// Cierra aplicación
                break;
        }
        return boton;
    }

    private Font cargarFuente(float tamano) {
        try {
            String nombre = "InYourFaceJoffrey.ttf";// Nombre archivo fuente
            String rutaRelativa = "in_your_face_joffrey/" + nombre;//Ruta dentro del proyecto
            java.io.InputStream is = getClass().getClassLoader().getResourceAsStream(rutaRelativa);//Intenta cargar desde classpath

            if (is == null) {
                // Busca en rutas alternativas
                String[] rutas = {"src/main/resources/"+rutaRelativa, "resources/"+rutaRelativa};
                for (String r : rutas) {
                    File f = new File(r);
                    if (f.exists()) return Font.createFont(Font.TRUETYPE_FONT, f).deriveFont(tamano);
                }
                // Fuente por defecto si no se encuentra
                return new Font("Arial", Font.BOLD, (int)tamano);
            }
            // Crea fuente desde InputStream
            return Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(tamano);
        } catch (Exception e) {
            // Fuente por defecto en caso de error
            return new Font("Arial", Font.BOLD, (int)tamano);
        }
    }

}
