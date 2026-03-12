package com.amongus.project.vista;

import com.amongus.project.data.GestorDatos;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * PantallaRegistro
 * Muestra el historial de partidas guardadas en el archivo XML.
 * Usa la imagen registro.png como fondo y la fuente del juego.
 */
public class PantallaRegistro extends JFrame {

    private Image imagenFondo;
    private Font fuenteJuego;

    public PantallaRegistro() {
        setTitle("Registro de Partidas");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        cargarRecursos();
        cargarIconoVentana();

        JPanel panelPrincipal = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                
                // 1. Primero pintamos el fondo negro sólido
                g2d.setColor(Color.BLACK);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                
                if (imagenFondo != null) {
                    // 2. Dibujamos la imagen encima con transparencia (0.5f = 50%)
                    // Esto hace que el negro de abajo se mezcle y oscurezca la imagen "blanca"
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
                    g2d.drawImage(imagenFondo, 0, 0, getWidth(), getHeight(), this);
                }
                
                g2d.dispose();
            }
        };

        // Título de la pantalla
        JLabel lblTitulo = new JLabel("HISTORIAL DE PARTIDAS", SwingConstants.CENTER);
        lblTitulo.setFont(fuenteJuego.deriveFont(40f));
        lblTitulo.setForeground(Color.WHITE);
        lblTitulo.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        panelPrincipal.add(lblTitulo, BorderLayout.NORTH);

        // Tabla de datos
        String[] columnas = {"FECHA", "GANADOR", "JUGADORES"};
        List<String[]> datos = GestorDatos.leerHistorial();
        Object[][] data = new Object[datos.size()][3];
        for (int i = 0; i < datos.size(); i++) {
            data[i] = datos.get(i);
        }

        JTable tabla = new JTable(data, columnas) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        // CENTRAR DATOS DE LA TABLA
        javax.swing.table.DefaultTableCellRenderer centerRenderer = new javax.swing.table.DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < tabla.getColumnCount(); i++) {
            tabla.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        tabla.setFont(fuenteJuego.deriveFont(18f));
        tabla.setRowHeight(30);
        tabla.setForeground(Color.WHITE);
        tabla.setBackground(new Color(0, 0, 0, 0)); // Fondo transparente para que se vea el panel de atrás
        tabla.setOpaque(false);
        tabla.getTableHeader().setFont(fuenteJuego.deriveFont(20f));
        tabla.getTableHeader().setBackground(Color.BLACK);
        tabla.getTableHeader().setForeground(Color.CYAN);
        tabla.setShowGrid(false);
        tabla.setSelectionBackground(new Color(255, 255, 255, 50));

        JScrollPane scrollPane = new JScrollPane(tabla);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 50, 50, 50));
        
        // RECUADRO DE CONTRASTE TRAS LA TABLA
        scrollPane.setViewportBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        scrollPane.getViewport().setBackground(new Color(0, 0, 0, 160)); // 60% transparencia aprox
        
        // PERSONALIZACIÓN DE BARRA DE DESPLAZAMIENTO (SCROLLBAR) - ESTILO NEGRO
        scrollPane.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(60, 60, 60)); // Gris muy oscuro
                g2.fillRoundRect(thumbBounds.x + 4, thumbBounds.y + 2, thumbBounds.width - 8, thumbBounds.height - 4, 10, 10);
                g2.dispose();
            }
            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
                g.setColor(new Color(15, 15, 15)); // Negro casi puro
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
        
        panelPrincipal.add(scrollPane, BorderLayout.CENTER);

        // Botón Volver
        JButton btnVolver = crearBotonMenu("VOLVER");
        btnVolver.addActionListener(e -> dispose());
        JPanel panelSur = new JPanel();
        panelSur.setOpaque(false);
        panelSur.add(btnVolver);
        panelPrincipal.add(panelSur, BorderLayout.SOUTH);

        setContentPane(panelPrincipal);
    }

    private void cargarRecursos() {
        // Cargar Fondo
        try {
            String nombreFondo = "registro.png";
            java.net.URL urlFondo = getClass().getClassLoader().getResource(nombreFondo);
            if (urlFondo != null) {
                imagenFondo = ImageIO.read(urlFondo);
            } else {
                File f = new File("src/main/resources/" + nombreFondo);
                if (f.exists()) imagenFondo = ImageIO.read(f);
            }
        } catch (Exception e) {
            System.err.println("Error cargando fondo registro: " + e.getMessage());
        }

        // Cargar Fuente
        try {
            String rutaFuente = "in_your_face_joffrey/InYourFaceJoffrey.ttf";
            InputStream is = getClass().getClassLoader().getResourceAsStream(rutaFuente);
            if (is != null) {
                fuenteJuego = Font.createFont(Font.TRUETYPE_FONT, is);
            } else {
                File f = new File("src/main/resources/" + rutaFuente);
                if (f.exists()) fuenteJuego = Font.createFont(Font.TRUETYPE_FONT, f);
                else fuenteJuego = new Font("Arial", Font.BOLD, 12);
            }
        } catch (Exception e) {
            fuenteJuego = new Font("Arial", Font.BOLD, 12);
        }
    }

    private JButton crearBotonMenu(String texto) {
        JButton boton = new JButton(texto) {
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

        boton.setPreferredSize(new Dimension(200, 50));
        boton.setFont(fuenteJuego.deriveFont(22f));
        boton.setForeground(Color.WHITE);
        boton.setContentAreaFilled(false);
        boton.setBorderPainted(false);
        boton.setFocusPainted(false);
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return boton;
    }

    private void cargarIconoVentana() {
        try {
            String nombre = "icono.jpg";
            java.net.URL url = getClass().getClassLoader().getResource(nombre);
            Image imagenIcono = null;
            if (url != null) {
                imagenIcono = ImageIO.read(url);
            } else {
                File f = new File("src/main/resources/" + nombre);
                if (f.exists()) imagenIcono = ImageIO.read(f);
            }
            if (imagenIcono != null) setIconImage(imagenIcono);
        } catch (Exception e) {
            System.err.println("Icono no encontrado en registro: " + e.getMessage());
        }
    }
}
