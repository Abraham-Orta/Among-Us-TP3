package com.amongus.project.vista;  // Define el paquete de la interfaz de usuario

import javax.swing.*; // Importa componentes de Swing (ventanas, botones, diálogos)
import java.awt.*;   // Importa componentes AWT (colores, fuentes, layouts)
import java.io.File;
import java.io.InputStream;

/**
 * PantallaFinJuego
 * Es un cuadro de diálogo (JDialog) que salta al terminar la partida
 * REDISEÑO: Estilo oscuro y personalizado.
 */
public class PantallaFinJuego extends JDialog {

    public PantallaFinJuego(JFrame ventanaPadre, String mensajeGanador) {
        super(ventanaPadre, "Fin de la Partida", true);
        
        // Configuración de ventana redondeada
        setUndecorated(true);
        setSize(500, 300);
        setLocationRelativeTo(ventanaPadre);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        
        // APLICAR FORMA REDONDEADA NATIVA
        setShape(new java.awt.geom.RoundRectangle2D.Double(0, 0, 500, 300, 40, 40));

        // Determinar colores según ganador
        boolean ganaronImpostores = mensajeGanador.toLowerCase().contains("impostor");
        Color colorTema = ganaronImpostores ? Color.RED : new Color(0, 150, 255);

        // Panel Principal con gráficos personalizados
        JPanel panelPrincipal = new JPanel(new GridBagLayout()) {
            private Image imagenVentana;
            {
                try {
                    java.net.URL url = getClass().getClassLoader().getResource("ventanas.jpg");
                    if (url != null) imagenVentana = javax.imageio.ImageIO.read(url);
                } catch (Exception e) {}
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 1. Imagen de fondo o color sólido
                if (imagenVentana != null) {
                    g2.drawImage(imagenVentana, 0, 0, getWidth(), getHeight(), this);
                } else {
                    g2.setColor(Color.BLACK);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }

                // 2. Velo oscuro
                g2.setColor(new Color(0, 0, 0, 150));
                g2.fillRect(0, 0, getWidth(), getHeight());

                // 3. Borde del color del ganador
                g2.setColor(colorTema);
                g2.setStroke(new BasicStroke(6));
                g2.drawRoundRect(3, 3, getWidth() - 6, getHeight() - 6, 40, 40);
                
                g2.dispose();
            }
        };
        
        panelPrincipal.setOpaque(true); // Panel sólido para evitar parpadeos
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0; 
        gbc.gridy = 0;
        
        // Título "VICTORIA" o "DERROTA" (Simplificado como el mensaje)
        JLabel etiquetaGanador = new JLabel("<html><div style='text-align: center;'>" + mensajeGanador.toUpperCase() + "</div></html>");
        etiquetaGanador.setFont(cargarFuente(35f));
        etiquetaGanador.setForeground(colorTema);
        panelPrincipal.add(etiquetaGanador, gbc);

        // Botón Aceptar Personalizado
        gbc.gridy = 1;
        gbc.insets = new Insets(30, 10, 10, 10);
        
        JButton botonAceptar = new JButton("Salir") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isPressed()) g2.setColor(new Color(30, 30, 30));
                else if (getModel().isRollover()) g2.setColor(new Color(60, 60, 60));
                else g2.setColor(Color.BLACK);

                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(3));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 30, 30);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        
        botonAceptar.setPreferredSize(new Dimension(180, 50));
        botonAceptar.setFont(cargarFuente(24f));
        botonAceptar.setForeground(Color.WHITE);
        botonAceptar.setContentAreaFilled(false);
        botonAceptar.setBorderPainted(false);
        botonAceptar.setFocusPainted(false);
        botonAceptar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        botonAceptar.addActionListener(evento -> dispose());
        panelPrincipal.add(botonAceptar, gbc);

        add(panelPrincipal);
    }
    
    // Método auxiliar para la fuente (Copiado para no depender de otra clase)
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
        } catch (Exception e) { return new Font("Arial", Font.BOLD, (int)tamano); }
    }
}
