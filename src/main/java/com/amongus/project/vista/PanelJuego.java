package com.amongus.project.vista;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.GradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.BasicStroke;
import java.awt.AlphaComposite;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import com.amongus.project.modelo.EstadoJuego;
import com.amongus.project.modelo.Jugador;
import com.amongus.project.modelo.Mapa;
import com.amongus.project.controlador.ManejadorEntrada;

/**
 * PanelJuego
 * ==========
 * Lienzo principal durante la partida.
 *
 * Cada instancia tiene su propio ManejadorEntrada y EstadoJuego,
 * por lo que múltiples ventanas abiertas simultáneamente (PruebaDirecta)
 * controlan a jugadores distintos de forma independiente.
 */
public class PanelJuego extends JPanel {

    private PantallaVotacion pantallaVotacion;

    // Instancia propia de ManejadorEntrada — NO estática, NO compartida
    private ManejadorEntrada manejadorEntrada;

    // Instancia propia de EstadoJuego — NO compartida entre ventanas
    private EstadoJuego estadoJuego;

    // Rectángulos para los botones del HUD
    private Rectangle rectKill, rectReport, rectVent, rectSabotage, rectContinuar, rectEmergencia, rectUse;
    private boolean cercaDeBoton = false;

    // ==============================================================
    // SISTEMA DE PALETTE SWAPPING Y ANIMACIÓN (LOS 5 PASOS)
    // ==============================================================
    
    // CACHÉ: Guarda las imágenes ya coloreadas para no repetir el proceso matemático (Paso 5)
    // La clave será un String como: "idle_-65536" o "walk1_-16776961"
    private static final Map<String, BufferedImage> cacheSpritesColorizados = new HashMap<>();
    
    // Almacena las imágenes molde (plantillas rojo/azul/verde) para no leer el disco a cada rato
    private static final Map<String, BufferedImage> cacheMoldes = new HashMap<>();

    /**
     * Obtiene y colorea un sprite dinámicamente.
     * @param rutaMolde Ejemplo: "sprites/sin moverse/idle.png"
     * @param colorPrimario El color del jugador
     * @param claveCache Un nombre único para guardar en caché, ej: "idle"
     */
    public static BufferedImage obtenerSpriteColoreado(String rutaMolde, Color colorPrimario, String claveCache) {
        // Generamos un ID único combinando la acción y el código numérico del color
        String idUnico = claveCache + "_" + colorPrimario.getRGB();

        // PASO 5: ¿Ya calculamos esta imagen antes? Si es así, la devolvemos inmediatamente
        if (cacheSpritesColorizados.containsKey(idUnico)) {
            return cacheSpritesColorizados.get(idUnico);
        }

        // PASO 1: Cargar la imagen base (El molde)
        BufferedImage molde = cacheMoldes.get(rutaMolde);
        if (molde == null) {
            try {
                URL url = PanelJuego.class.getClassLoader().getResource(rutaMolde);
                if (url != null) {
                    molde = ImageIO.read(url);
                    cacheMoldes.put(rutaMolde, molde);
                } else {
                    return null; // No encontró la imagen base
                }
            } catch (Exception e) {
                System.err.println("Error leyendo molde: " + e.getMessage());
                return null;
            }
        }

        // PASO 2: Crear un lienzo vacío (El destino) con soporte ARGB (Transparencia)
        int ancho = molde.getWidth();
        int alto = molde.getHeight();
        BufferedImage lienzoDestino = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);

        // Pre-calculamos los colores que vamos a inyectar
        Color colorSombra = colorPrimario.darker();
        Color colorVisor = new Color(150, 200, 220); // Un celeste suave para el visor

        // PASO 3: Recorrer la matriz de píxeles (El escaneo)
        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                int pixelOriginal = molde.getRGB(x, y);

                // Extraemos los canales
                int alpha = (pixelOriginal >> 24) & 0xff;
                int red   = (pixelOriginal >> 16) & 0xff;
                int green = (pixelOriginal >> 8) & 0xff;
                int blue  = pixelOriginal & 0xff;

                // Si es totalmente transparente, lo copiamos y pasamos al siguiente
                if (alpha == 0) {
                    lienzoDestino.setRGB(x, y, pixelOriginal);
                    continue;
                }

                // PASO 4: Aplicar la máscara de color (El reemplazo)
                if (red > 150 && green < 100 && blue < 100) { 
                    // Domina Rojo -> Color Primario
                    lienzoDestino.setRGB(x, y, mezclarConTransparencia(colorPrimario, alpha));
                } 
                else if (blue > 150 && red < 100 && green < 100) { 
                    // Domina Azul -> Color de Sombra
                    lienzoDestino.setRGB(x, y, mezclarConTransparencia(colorSombra, alpha));
                } 
                else if (green > 150 && red < 100 && blue < 100) { 
                    // Domina Verde -> Color del Visor
                    lienzoDestino.setRGB(x, y, mezclarConTransparencia(colorVisor, alpha));
                } 
                else {
                    // Contornos negros, brillos blancos, etc. Se copian igual
                    lienzoDestino.setRGB(x, y, pixelOriginal);
                }
            }
        }

        // Guardamos el resultado en el Caché (Paso 5)
        cacheSpritesColorizados.put(idUnico, lienzoDestino);
        
        return lienzoDestino;
    }

    // auxiliar matemático para inyectar el color respetando el anti-aliasing (bordes suaves) originales
    private static int mezclarConTransparencia(Color color, int alphaOriginal) {
        return (alphaOriginal << 24) | (color.getRGB() & 0x00FFFFFF);
    }

    // método para obtener imágenes estáticas sin alterar (como el fondo del asesinato)
    public static Image obtenerImagenFija(String ruta) {
        if (cacheMoldes.containsKey(ruta)) return cacheMoldes.get(ruta);
        try {
            URL url = PanelJuego.class.getClassLoader().getResource(ruta);
            if (url != null) {
                BufferedImage img = ImageIO.read(url);
                cacheMoldes.put(ruta, img);
                return img;
            }
        } catch (Exception e) {}
        return null;
    }

    // Caché estático: la fuente se carga del disco UNA sola vez
    private static Font fuenteBase;

    private Font cargarFuente(float tamano) {
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

    // Flag para controlar que la música de victoria suene una sola vez
    private boolean musicaVictoriaReproducida = false;

    public PanelJuego(EstadoJuego estadoJuego) {
        this.estadoJuego = estadoJuego;

        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.BLACK);
        setFocusable(true);

        // Cada ventana tiene su propio manejador → teclas independientes
        manejadorEntrada = new ManejadorEntrada(estadoJuego);
        addKeyListener(manejadorEntrada);

        // Ratón
        ManejadorEntrada.MouseHandler mouseHandler = manejadorEntrada.new MouseHandler();
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);

        // listener adicional para los botones del hud
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                boolean clickHUD = false;
                
                // Manejo de clic en pantalla FINALIZADO
                if (estadoJuego.getFaseActual() == EstadoJuego.Fase.FINALIZADO) {
                    if (rectContinuar != null && rectContinuar.contains(e.getPoint())) {
                        java.awt.Window win = javax.swing.SwingUtilities.getWindowAncestor(PanelJuego.this);
                        if (win != null) win.dispose();
                        new MenuPrincipal().setVisible(true); // Volvemos al menú principal
                    }
                    return; // Si estamos en finalizado, no procesar el resto de botones
                }

                if (rectKill != null && rectKill.contains(e.getPoint())) { manejadorEntrada.accionMatar = true; clickHUD = true; }
                if (rectReport != null && rectReport.contains(e.getPoint())) { manejadorEntrada.accionReportar = true; clickHUD = true; }
                if (rectVent != null && rectVent.contains(e.getPoint())) { manejadorEntrada.accionVentilar = true; clickHUD = true; }
                if (rectSabotage != null && rectSabotage.contains(e.getPoint())) { manejadorEntrada.accionSabotaje = true; clickHUD = true; }
                if (rectUse != null && rectUse.contains(e.getPoint())) { manejadorEntrada.accionUsar = true; clickHUD = true; }
                if (rectEmergencia != null && rectEmergencia.contains(e.getPoint()) && cercaDeBoton) {
                    manejadorEntrada.accionEmergencia = true; // Feedback de estado
                    Jugador local = estadoJuego.getJugadorLocal();
                    if (local != null && local.isVivo()) {
                        local.presionarBotonEmergencia();
                        clickHUD = true;
                    }
                }
                
                if (clickHUD) {
                    com.amongus.project.vista.ReproductorMusica.reproducirEfecto("UI_boton.wav");
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                manejadorEntrada.accionMatar = false;
                manejadorEntrada.accionReportar = false;
                manejadorEntrada.accionVentilar = false;
                manejadorEntrada.accionSabotaje = false;
                manejadorEntrada.accionUsar     = false;
                manejadorEntrada.accionEmergencia = false;
            }
        });

        this.pantallaVotacion = new PantallaVotacion(estadoJuego, manejadorEntrada);
    }

    /** BucleJuego lo usa para pasarle el manejador correcto a jugador.actualizar() */
    public ManejadorEntrada getManejadorEntrada() { return manejadorEntrada; }

    public PantallaVotacion getPantallaVotacion() { return pantallaVotacion; }

    public EstadoJuego getEstadoJuego() { return estadoJuego; }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        EstadoJuego.Fase fase = estadoJuego.getFaseActual();

        if (fase == EstadoJuego.Fase.VOTACION) {
            pantallaVotacion.render(g, getWidth(), getHeight());
            return;
        }

        Graphics2D g2d = (Graphics2D) g;
        // Fix #3: Usar velocidad en contexto global (60fps). Solo antialiasing para texto y contornos.
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,       RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,  RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,          RenderingHints.VALUE_RENDER_SPEED);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,      RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,    RenderingHints.VALUE_COLOR_RENDER_SPEED);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,     RenderingHints.VALUE_STROKE_DEFAULT);

        // --- PANTALLA DE VICTORIA (FINALIZADO) ---
        if (fase == EstadoJuego.Fase.FINALIZADO) {
            
            // LÓGICA DE SONIDO: Solo reproducimos la música de victoria una vez al entrar en este estado.
            // Si el flag 'musicaVictoriaReproducida' es falso, significa que acabamos de terminar.
            if (!musicaVictoriaReproducida) {
                String msgGana = estadoJuego.getMensajeGanador();
                // Verificamos quién ganó para poner la canción adecuada.
                if (msgGana != null && msgGana.toLowerCase().contains("impostor")) {
                    // Si ganaron los impostores, suena su tema triunfal.
                    ReproductorMusica.reproducirEfecto("victoria_impostor.wav");
                } else {
                    // Si ganaron los tripulantes, suena su melodía característica.
                    ReproductorMusica.reproducirEfecto("victoria_tripulantes.wav");
                }
                // Marcamos el flag como verdadero para que en el siguiente frame (60 veces por seg) no vuelva a sonar.
                musicaVictoriaReproducida = true;
            }

            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, getWidth(), getHeight());

            String msgGanador = estadoJuego.getMensajeGanador();
            boolean gananImpostores = msgGanador != null && msgGanador.toLowerCase().contains("impostor");

            // Tema visual
            Color colorTexto = gananImpostores ? Color.RED : Color.CYAN;
            Color colorBrillo = gananImpostores ? new Color(255, 0, 0, 150) : new Color(0, 255, 255, 90);

            // Resplandor de fondo
            int centroX = getWidth() / 2;
            int centroY = getHeight() / 2;
            float radio = Math.max(getWidth(), getHeight()) * 0.6f;
            RadialGradientPaint resplandor = new RadialGradientPaint(centroX, centroY, radio, new float[]{0.0f, 1.0f}, new Color[]{colorBrillo, new Color(0, 0, 0, 0)});
            g2d.setPaint(resplandor);
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.setPaint(null);

            // Filtrar ganadores
            List<Jugador> equipo = new ArrayList<>();
            // Asegurarnos de tener la lista completa sin duplicados (incluyendo al jugador local si no está en la red principal)
            List<Jugador> todosLosJugadores = new ArrayList<>(estadoJuego.getJugadores());
            Jugador elLocal = estadoJuego.getJugadorLocal();
            if (elLocal != null && !todosLosJugadores.contains(elLocal)) {
                todosLosJugadores.add(elLocal);
            }

            for (Jugador j : todosLosJugadores) {
                if (gananImpostores && j.isImpostor()) {
                    if (!equipo.contains(j)) equipo.add(j);
                } else if (!gananImpostores && !j.isImpostor()) {
                    if (!equipo.contains(j)) equipo.add(j);
                }
            }

            int anchoVentana = getWidth();
            int altoVentana = getHeight();
            
            // 1. Dibujar Título (Fijo arriba, se adapta a la ventana)
            String titulo = msgGanador != null ? msgGanador.toUpperCase() : "FIN DEL JUEGO";
            int tamTitulo = Math.max(30, (int)(altoVentana * 0.08));
            g2d.setFont(cargarFuente(tamTitulo));
            g2d.setColor(colorTexto);
            FontMetrics fmTitulo = g2d.getFontMetrics();
            int yTitulo = (int)(altoVentana * 0.15) + fmTitulo.getAscent();
            g2d.drawString(titulo, (anchoVentana - fmTitulo.stringWidth(titulo)) / 2, yTitulo);

            // 2. Dibujar Botón CONTINUAR (Fijo abajo)
            int wBtn = Math.max(220, (int)(anchoVentana * 0.2));
            int hBtn = Math.max(55, (int)(altoVentana * 0.08));
            int xBtn = (anchoVentana - wBtn) / 2;
            int yBtn = altoVentana - (int)(altoVentana * 0.1) - hBtn;
            rectContinuar = new Rectangle(xBtn, yBtn, wBtn, hBtn);

            g2d.setColor(Color.WHITE);
            g2d.drawRoundRect(xBtn, yBtn, wBtn, hBtn, 20, 20);
            
            int tamBtn = Math.max(20, (int)(hBtn * 0.45));
            g2d.setFont(cargarFuente(tamBtn)); 
            String txtBtn = "CONTINUAR";
            FontMetrics fmBtn = g2d.getFontMetrics();
            int tx = xBtn + (wBtn - fmBtn.stringWidth(txtBtn)) / 2;
            int ty = yBtn + ((hBtn - fmBtn.getHeight()) / 2) + fmBtn.getAscent();
            g2d.drawString(txtBtn, tx, ty);

            // 3. Área dinámica para personajes (Calcula exactamente el espacio restante)
            int yAreaPersonajesInicio = yTitulo + (int)(altoVentana * 0.05);
            int yAreaPersonajesFin = yBtn - (int)(altoVentana * 0.05);
            int altoAreaPersonajes = yAreaPersonajesFin - yAreaPersonajesInicio;

            if (altoAreaPersonajes > 0 && !equipo.isEmpty()) {
                double anchoBase = 110.0;
                double altoBase = 140.0;
                double relacionAspecto = altoBase / anchoBase;

                // Parámetros de formación
                double overlapX = 0.75; // Separación del 75%
                double overlapY = 0.15; // Elevación para la V
                
                double anchoDisponible = anchoVentana * 0.85;
                double factorAnchoTotal = 1.0 + (equipo.size() - 1) * overlapX;
                double maxWPorAncho = anchoDisponible / factorAnchoTotal;
                
                double factorAltoTotal = 1.0 + (equipo.size() / 2) * overlapY;
                double maxWPorAlto = (altoAreaPersonajes / factorAltoTotal) / relacionAspecto;

                // El personaje no debe ser mayor a 1/3 de la pantalla (por estética si hay pocos ganadores)
                double limiteMaxAbsoluto = anchoVentana * 0.35;
                
                // Tomamos el limitante más estricto
                double anchoFinal = Math.min(Math.min(maxWPorAncho, maxWPorAlto), limiteMaxAbsoluto);
                double altoFinal = anchoFinal * relacionAspecto;

                double stepX = anchoFinal * overlapX; 
                double stepY = altoFinal * overlapY;
                int maxYOffset = (equipo.size() / 2) * (int)stepY;

                int xBaseCentro = anchoVentana / 2;
                int centroAreaY = yAreaPersonajesInicio + (altoAreaPersonajes / 2);
                int yBaseCentro = centroAreaY - (int)(altoFinal / 2) + (maxYOffset / 2);

                String rutaImg = "sprites/revelacion/revelacion.png";

                for (int i = equipo.size() - 1; i >= 0; i--) {
                    Jugador j = equipo.get(i);
                    int pos = (i == 0) ? 0 : (i % 2 != 0) ? -(i + 1) / 2 : i / 2;
                    int distancia = Math.abs(pos);

                    double reduccion = 1.0 - (distancia * 0.08);
                    if (reduccion < 0.5) reduccion = 0.5;

                    int drawW = (int) (anchoFinal * reduccion);
                    int drawH = (int) (altoFinal * reduccion);

                    int anchorX = xBaseCentro + (int)(pos * stepX);
                    int curX = anchorX - (drawW / 2);
                    int curY = yBaseCentro - (int)(distancia * stepY) + ((int)altoFinal - drawH);

                    BufferedImage imgColoreada = obtenerSpriteColoreado(rutaImg, j.getColor(), "vic_" + j.getNombre());
                    if (imgColoreada != null) {
                        g2d.drawImage(imgColoreada, curX, curY, drawW, drawH, null);
                    }
                }
            }

            return;
        }

        // --- CÁMARA ---
        int camX = 0, camY = 0;
        Jugador local = estadoJuego.getJugadorLocal();
        Mapa mapa     = estadoJuego.getMapa();

        if (local != null && mapa != null) {
            camX = local.getX() - (getWidth()  / 2);
            camY = local.getY() - (getHeight() / 2);
            if (camX < 0) camX = 0;
            if (camY < 0) camY = 0;
            if (camX > mapa.getAncho() - getWidth())  camX = mapa.getAncho() - getWidth();
            if (camY > mapa.getAlto()  - getHeight()) camY = mapa.getAlto()  - getHeight();
        }

        // --- PANTALLA DE REVELACION DE ROL ---
        if (fase == EstadoJuego.Fase.REVELACION) {
            // sonido de intro
            if (local != null) {
                if (local.isImpostor()) ReproductorMusica.reproducirEfecto("victoria_impostor.wav");
                else                   ReproductorMusica.reproducirEfecto("victoria_tripulantes.wav");
            }

            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, getWidth(), getHeight());

            if (local != null) {
                // --- EFECTO DE LINTERNA / RESPLANDOR ---
                int centroX = getWidth() / 2;
                int centroY = getHeight() / 2;
                float radio = Math.max(getWidth(), getHeight()) * 0.6f; 
                
                // Color rojo semi-transparente si es impostor, cyan si es tripulante
                Color colorBrillo = local.isImpostor() ? new Color(255, 0, 0, 150) : new Color(0, 255, 255, 90);
                Color colorBorde = new Color(0, 0, 0, 0);

                float[] fracciones = {0.0f, 1.0f};
                RadialGradientPaint resplandor = new RadialGradientPaint(centroX, centroY, radio, fracciones, new Color[]{colorBrillo, colorBorde});

                g2d.setPaint(resplandor);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.setPaint(null); // Limpiamos la brocha para los siguientes dibujos
                // ---------------------------------------

                // 1. armar el equipo
                List<Jugador> equipo = new ArrayList<>();
                equipo.add(local);
                for (Jugador j : estadoJuego.getJugadores()) {
                    if (j != local && j.isImpostor() == local.isImpostor()) {
                        equipo.add(j);
                    }
                }

                // 2. MATEMÁTICA DE ESCALADO DINÁMICO
                int anchoVentana = getWidth();
                int altoVentana = getHeight();
                
                // Definimos medidas base del sprite original (revelacion.png suele ser alto)
                double anchoBase = 110.0;
                double altoBase = 140.0;
                double relacionAspecto = altoBase / anchoBase; // Proporción original
                
                // Calculamos cuánto espacio máximo puede tener cada uno (usamos el 85% del ancho total)
                double margenLateral = anchoVentana * 0.15;
                double anchoDisponible = anchoVentana - margenLateral;
                
                // 1. Superposición (Overlap en X)
                // Al superponerse (ej. 60% de encime), ocupan menos espacio horizontal total.
                // Ancho total = Ancho de 1 + (N - 1) * (Ancho * 0.4)
                double factorAnchoTotal = 1.0 + (equipo.size() - 1) * 0.4;
                double anchoMaximoPorMuñeco = anchoDisponible / factorAnchoTotal;
                
                // No queremos que sean GIGANTES si hay solo 1 jugador, así que limitamos a 250px
                double anchoFinal = Math.min(anchoMaximoPorMuñeco, 250.0);
                
                // Aplicamos el Aspect Ratio para calcular la altura perfecta sin deformar
                double altoFinal = anchoFinal * relacionAspecto;
                
                // Si el alto es demasiado para la pantalla, encogemos un poco más
                if (altoFinal > altoVentana * 0.5) {
                    altoFinal = altoVentana * 0.5;
                    anchoFinal = altoFinal / relacionAspecto;
                }

                // 2. Desplazamiento de Altura (Desfase en Y)
                double stepX = anchoFinal * 0.4; // Separación en X (60% de overlap)
                double stepY = altoFinal * 0.15; // Elevación para los de atrás
                
                // Cuánto sube como máximo el muñeco más lejano
                int maxYOffset = (equipo.size() / 2) * (int)stepY;

                int xBaseCentro = anchoVentana / 2;
                // Ajustamos el centro Y para compensar la altura de los de atrás
                int yBaseCentro = (altoVentana / 2) - (int)(altoFinal / 2) + (maxYOffset / 2);

                // 3. El Orden de Dibujado (Z-Index / Capas)
                String rutaImg = "sprites/revelacion/revelacion.png";
                
                // Iteramos DE ATRÁS HACIA ADELANTE para que el jugador principal se dibuje al final encima de todos
                for (int i = equipo.size() - 1; i >= 0; i--) {
                    Jugador j = equipo.get(i);
                    
                    // Cálculo de posición en la V
                    // Si i=0 (local) -> pos = 0
                    // Si i es impar -> lado izquierdo (negativo)
                    // Si i es par -> lado derecho (positivo)
                    int pos = (i == 0) ? 0 : (i % 2 != 0) ? -(i + 1) / 2 : i / 2;
                    int distancia = Math.abs(pos);
                    
                    // Escala dinámica según distancia al centro (8% menos por paso)
                    double reduccion = 1.0 - (distancia * 0.08);
                    if (reduccion < 0.5) reduccion = 0.5; // Límite de pequeñez suavizado
                    
                    int drawW = (int) (anchoFinal * reduccion);
                    int drawH = (int) (altoFinal * reduccion);
                    
                    // Centro de la posición ideal para este personaje en X
                    int anchorX = xBaseCentro + (int)(pos * stepX);
                    int curX = anchorX - (drawW / 2);
                    
                    // Los personajes de atrás suben en Y, pero alineamos sus bases ("suelo" imaginario)
                    int curY = yBaseCentro - (int)(distancia * stepY) + ((int)altoFinal - drawH);
                    
                    BufferedImage imgColoreada = obtenerSpriteColoreado(rutaImg, j.getColor(), "rev_" + j.getNombre());
                    
                    if (imgColoreada != null) {
                        g2d.drawImage(imgColoreada, curX, curY, drawW, drawH, null);
                        
                        // --- DIBUJAR SOMBRERO EN REVELACIÓN ---
                        if (!j.getSombrero().equals("ninguno")) {
                            String rutaSombrero = "sprites/sombreros/" + j.getSombrero() + ".png";
                            Image imgSom = obtenerImagenFija(rutaSombrero);
                            if (imgSom != null) {
                                // Escalado proporcional para la pantalla de revelación
                                int sw = (int)(drawW * 1.2);
                                int sh = (int)(drawH * 0.8);
                                int sx = curX - (sw - drawW) / 2;
                                int sy = curY - (int)(sh * 0.6);
                                g2d.drawImage(imgSom, sx, sy, sw, sh, null);
                            }
                        }
                    } else {
                        // monigote de emergencia dinámico escalado
                        g2d.setColor(j.getColor());
                        g2d.fillRoundRect(curX + (int)(drawW*0.2), curY + (int)(drawH*0.1), (int)(drawW*0.6), (int)(drawH*0.7), 20, 20);
                    }

                    // Mostrar solo al protagonista: dibujamos el nombre debajo únicamente del jugador local
                    if (j == local) {
                        // FUENTE ADAPTATIVA: Tamaño basado en el ancho del muñeco
                        int tamFuente = Math.max(16, (int)(drawW * 0.22));
                        g2d.setFont(cargarFuente(tamFuente));
                        g2d.setColor(Color.WHITE);
                        int txtX = curX + (drawW - g2d.getFontMetrics().stringWidth(j.getNombre())) / 2;
                        // POSICIÓN ADAPTATIVA: Se baja el nombre un 5% de la altura de la ventana respecto al pie del muñeco
                        g2d.drawString(j.getNombre(), txtX, curY + drawH + (int)(altoVentana * 0.05));
                    }
                }

                // 4. Encabezados dinámicos con AJUSTE ESTRICTO DE LÍMITES
                String titulo = local.isImpostor() ? "IMPOSTOR" : "TRIPULANTE";
                Color colorT = local.isImpostor() ? Color.RED : Color.CYAN;
                
                // --- AJUSTE DINÁMICO DE TÍTULO ---
                int tamTitulo = Math.max(30, (int)(altoVentana * 0.12));
                g2d.setFont(cargarFuente(tamTitulo));
                // Si el título es muy ancho para la ventana, lo encogemos hasta que quepa
                while (g2d.getFontMetrics().stringWidth(titulo) > anchoVentana * 0.9 && tamTitulo > 15) {
                    tamTitulo -= 2;
                    g2d.setFont(cargarFuente(tamTitulo));
                }
                g2d.setColor(colorT);
                g2d.drawString(titulo, (anchoVentana - g2d.getFontMetrics().stringWidth(titulo)) / 2, yBaseCentro - maxYOffset - (int)(altoFinal * 0.1));

                // --- AJUSTE DINÁMICO DE INFORMACIÓN ---
                int tamSub = Math.max(16, (int)(altoVentana * 0.04));
                int totalJugadores = estadoJuego.getJugadores().size();
                int numImpostores = totalJugadores > 3 ? 2 : 1;
                String info1 = local.isImpostor() ? "Elimina a todos sin que te descubran" : "Hay " + numImpostores + " Impostor" + (numImpostores > 1 ? "es" : "") + " entre nosotros";
                
                g2d.setFont(cargarFuente(tamSub));
                // Aseguramos que info1 quepa horizontalmente
                while (g2d.getFontMetrics().stringWidth(info1) > anchoVentana * 0.9 && tamSub > 12) {
                    tamSub -= 1;
                    g2d.setFont(cargarFuente(tamSub));
                }
                
                int yBaseLetras = yBaseCentro + (int)altoFinal;
                // Ajustamos coordenadas Y para que sean más compactas y seguras (no salirse abajo)
                int yTexto1 = yBaseLetras + (int)(altoVentana * 0.10); 
                if (yTexto1 > altoVentana - 40) yTexto1 = altoVentana - 40; // Límite inferior de seguridad
                
                g2d.setColor(Color.WHITE);
                g2d.drawString(info1, (anchoVentana - g2d.getFontMetrics().stringWidth(info1)) / 2, yTexto1);

                if (!local.isImpostor()) {
                    String info2 = "Completa tareas o encuentra al impostor";
                    // info2 usa el mismo tamSub ajustado o se ajusta más si es necesario
                    while (g2d.getFontMetrics().stringWidth(info2) > anchoVentana * 0.9 && tamSub > 10) {
                        tamSub -= 1;
                        g2d.setFont(cargarFuente(tamSub));
                    }
                    g2d.setColor(Color.LIGHT_GRAY);
                    int yTexto2 = yTexto1 + g2d.getFontMetrics().getHeight() + 5;
                    if (yTexto2 > altoVentana - 15) yTexto2 = altoVentana - 15; // Límite inferior absoluto
                    g2d.drawString(info2, (anchoVentana - g2d.getFontMetrics().stringWidth(info2)) / 2, yTexto2);
                }
            }
            return;
        }

        // Capa 1: Mapa (Solo dibujado dentro del "Lente" visual)
        if (mapa != null) {
            mapa.render(g, manejadorEntrada.modoDesarrollador, camX, camY, getWidth(), getHeight());
            
            // DIBUJAR TAREAS (Zonas de interacción en el mapa)
            for (com.amongus.project.modelo.TareaMapa tarea : mapa.getTareasDisponibles()) {
                Rectangle r = tarea.getZona();
                // Solo dibujamos si el jugador local tiene esta tarea pendiente o es impostor
                if (local != null && (local.getTareasPendientes().contains(tarea.getNombre()) || local.isImpostor())) {
                    g2d.setColor(new Color(255, 255, 0, 80)); // Amarillo suave
                    g2d.fillRect(r.x, r.y, r.width, r.height);
                    g2d.setColor(new Color(255, 255, 100, 180)); // Borde brillante
                    g2d.setStroke(new BasicStroke(3));
                    g2d.drawRect(r.x, r.y, r.width, r.height);
                    
                    // Texto pequeño con el nombre de la tarea
                    g2d.setFont(new Font("Arial", Font.BOLD, 14));
                    g2d.setColor(Color.WHITE);
                    g2d.drawString("TAREA", r.x, r.y - 5);
                }
            }
        }

        g2d.translate(-camX, -camY);

        // Detección de botón de emergencia cercano (en coordenadas de mapa)
        if (mapa != null) {
            cercaDeBoton = false;
            if (local != null) {
                for (Rectangle btnRect : mapa.getBotones()) {
                    double dist = Math.sqrt(Math.pow(local.getX() - btnRect.x, 2) + Math.pow(local.getY() - btnRect.y, 2));
                    if (dist < 100) {
                        cercaDeBoton = true;
                    }
                }
            }
        }

        // Capa 1.5: cinemática de asesinato (solo para el atacante o víctima)
        boolean enCinematica = false;
        Jugador atacante = null;
        Jugador victima = null;
        long ahora = System.currentTimeMillis();

        if (local != null) {
            if (!local.isVivo() && (ahora - local.getTiempoInicioMuerte() < 2880)) {
                enCinematica = true;
                victima = local;
                for (Jugador j : estadoJuego.getJugadores()) {
                    if (j.isAtacando()) { atacante = j; break; }
                }
            } else if (local.isAtacando()) {
                enCinematica = true;
                atacante = local;
                for (Jugador j : estadoJuego.getJugadores()) {
                    if (!j.isVivo() && (ahora - j.getTiempoInicioMuerte() < 1920)) { victima = j; break; }
                }
            }
        }

        if (enCinematica) {
            // fondo completamente negro para resaltar la cinemática
            g.setColor(Color.BLACK);
            g.fillRect(camX, camY, getWidth(), getHeight());
            
            Image bgKill = obtenerImagenFija("sprites/cinematica_kill/killBG.png");
            if (bgKill != null) {
                // abarca toda la pantalla
                g.drawImage(bgKill, camX, camY, getWidth(), getHeight(), null);
            }

            int centerX = camX + getWidth() / 2;
            int centerY = camY + getHeight() / 2;
            int scaleW = 350; // tamaño bien grande para que se vea en el medio
            int scaleH = 400;
            
            long tiempoAnim = (atacante != null) ? (ahora - atacante.getTiempoInicioAsesinato()) : (ahora - victima.getTiempoInicioMuerte());
            int frameActual = (int) (tiempoAnim / 60) + 1;
            if (frameActual > 48) frameActual = 48;

            if (atacante != null) {
                String nombreFrame = String.format("killalien_imposter%04d.png", frameActual);
                String rutaMolde = "sprites/Ataque del impostor/" + nombreFrame;
                BufferedImage spriteA = obtenerSpriteColoreado(rutaMolde, atacante.getColor(), "ataque_" + frameActual);
                if (spriteA != null) {
                    // lo dibujamos más a la izquierda del centro para dar espacio
                    g.drawImage(spriteA, centerX - scaleW - 20, centerY - scaleH / 2, scaleW, scaleH, null);
                }
            }
            
            if (victima != null) {
                String nombreFrame = String.format("killalien_victim%04d.png", frameActual);
                String rutaMolde = "sprites/Muerte/" + nombreFrame;
                BufferedImage spriteV = obtenerSpriteColoreado(rutaMolde, victima.getColor(), "muerte_" + frameActual);
                if (spriteV != null) {
                    // lo dibujamos más a la derecha del centro para dar espacio
                    g.drawImage(spriteV, centerX + 20, centerY - scaleH / 2, scaleW, scaleH, null);
                }
            }

            // restauramos la cámara y salimos para no dibujar el mapa normal
            g2d.translate(camX, camY);
            return;
        }

        // Capa 2: Jugadores
        for (Jugador j : estadoJuego.getJugadores()) {
            dibujarTripulante(g, j);
        }
        if (local != null && !estadoJuego.getJugadores().contains(local)) {
            dibujarTripulante(g, local);
        }

        // Capa 3: Oscuridad externa al rango visual.
        // Se dibuja un Área restando el círculo de luz de la ventana para oscurecer eficientemente 
        // y se elimina la necesidad del costoso RadialGradientPaint de dibujarCampoVisual.
        if (local != null && mapa != null) {
            float radioLuz;
            if (local.isImpostor()) {
                radioLuz = 350.0f;
            } else {
                radioLuz = estadoJuego.areLucesSaboteadas() ? 50.0f : 180.0f;
            }

            g2d.translate(camX, camY); // restaurar a coordenadas de pantalla UI
            
            // Construir la máscara negra
            Area pantallaCompleta = new Area(new Rectangle(0, 0, getWidth(), getHeight()));
            
            int luzRealX = local.getX() - camX + 15;
            int luzRealY = local.getY() - camY + 20;

            Shape circuloLuz = new Ellipse2D.Float(
                luzRealX - radioLuz, 
                luzRealY - radioLuz, 
                radioLuz * 2, 
                radioLuz * 2
            );
            
            pantallaCompleta.subtract(new Area(circuloLuz));

            // Dibujar la oscuridad exterior
            g2d.setColor(Color.BLACK);
            g2d.fill(pantallaCompleta);
            
            // Agregamos un borde suave (penumbra) en el arco de visión
            Graphics2D g2soft = (Graphics2D) g.create();
            java.awt.RadialGradientPaint bordeDifuminado = new java.awt.RadialGradientPaint(
                new Point2D.Float(luzRealX, luzRealY), radioLuz,
                new float[]{0.8f, 1.0f},
                new Color[]{
                    new Color(0, 0, 0, 0),
                    new Color(0, 0, 0, 255)
                }
            );
            g2soft.setPaint(bordeDifuminado);
            g2soft.fill(circuloLuz);
            g2soft.dispose();
            
            g2d.translate(-camX, -camY); // volver al estado normal
        }

        g2d.translate(camX, camY);

        if (estadoJuego.getJugadores().isEmpty() && local == null) {
            g.setColor(Color.WHITE);
            g.drawString("No hay jugadores. Inicia partida.", 300, 300);
        }
        
        dibujarHUD(g);
        dibujarTareas(g);
    }

    private void dibujarTareas(Graphics g) {
        Jugador local = estadoJuego.getJugadorLocal();
        if (local == null) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int x = 20;
        int y = 40;
        
        // --- 1. BARRA DE PROGRESO GLOBAL ---
        g2.setFont(cargarFuente(18f));
        g2.setColor(Color.WHITE);
        g2.drawString("PROGRESO TOTAL", x, y - 20);
        
        int barW = 300;
        int barH = 25;
        // Fondo de la barra
        g2.setColor(new Color(30, 30, 30, 220));
        g2.fillRoundRect(x, y - 15, barW, barH, 12, 12);
        
        String progreso = estadoJuego.getProgresoTareas();
        try {
            String[] partes = progreso.split("/");
            int actuales = Integer.parseInt(partes[0]);
            int totales = Integer.parseInt(partes[1]);
            if (totales > 0) {
                int filledW = (int) (barW * ((double) actuales / totales));
                // Gradiente verde para la barra
                g2.setPaint(new GradientPaint(x, 0, new Color(0, 150, 0), x + filledW, 0, new Color(0, 255, 0)));
                g2.fillRoundRect(x, y - 15, filledW, barH, 12, 12);
            }
        } catch (Exception e) {}
        
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(3));
        g2.drawRoundRect(x, y - 15, barW, barH, 12, 12);
        
        // --- 2. LISTA DE TAREAS LOCALES ---
        y += 50;
        
        // Dibujar un fondo oscuro para la lista de tareas
        int numTareas = local.getTareasPendientes().size() + local.getTareasCompletadas().size();
        // System.out.println("Dibujando HUD: Tareas en Jugador = " + numTareas); // Log de depuración
        if (numTareas > 0) {
            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillRoundRect(x - 5, y - 25, 260, 40 + (numTareas * 25), 15, 15);
        }

        g2.setFont(cargarFuente(22f));
        g2.setColor(new Color(255, 255, 100)); // Amarillo para el título "Tareas"
        g2.drawString("TAREAS", x, y);
        y += 30;
        
        // Detectar si estamos sobre alguna zona de tarea para resaltarla
        String tareaCercana = "";
        Mapa mapa = estadoJuego.getMapa();
        if (mapa != null) {
            for (com.amongus.project.modelo.TareaMapa tm : mapa.getTareasDisponibles()) {
                if (local.getHitbox().intersects(tm.getZona())) {
                    tareaCercana = tm.getNombre();
                    break;
                }
            }
        }

        // Tareas Completadas (Verde)
        g2.setFont(cargarFuente(18f));
        for (String t : local.getTareasCompletadas()) {
            g2.setColor(new Color(0, 255, 0, 180));
            g2.drawString("✔ " + formatearNombreTarea(t), x + 10, y);
            y += 25;
        }
        
        // Tareas Pendientes
        for (String t : local.getTareasPendientes()) {
            if (t.equals(tareaCercana)) {
                g2.setColor(Color.YELLOW); // Resaltar si estamos encima de la zona
                g2.setFont(cargarFuente(20f)); // Un poco más grande
                g2.drawString("➜ " + formatearNombreTarea(t), x + 5, y);
            } else {
                g2.setColor(Color.WHITE);
                g2.setFont(cargarFuente(18f));
                g2.drawString("☐ " + formatearNombreTarea(t), x + 10, y);
            }
            y += 25;
        }
        
        g2.dispose();
    }

    private String formatearNombreTarea(String id) {
        switch(id.toLowerCase()) {
            case "simon": return "Reactor: Simon Dice";
            case "energia": return "Distribucion Energia";
            case "numeros": return "Pulsar Números";
            case "cables": return "Arreglar Cables";
            case "asteroides": return "Armas: Asteroides";
            case "tarjeta": return "Admin: Pasar Tarjeta";
            case "calibrar": return "Calibrar Distribuidor";
            case "download": return "Descargar Datos";
            default: return id;
        }
    }

    /**
     * Dibuja los botones de acción en la pantalla (HUD)
     */
    private void dibujarHUD(Graphics g) {
        Jugador local = estadoJuego.getJugadorLocal();
        if (local == null) return;

        int w = getWidth();
        int h = getHeight();
        int btnSize = 100;
        int gap = 20;

        // Posiciones dinámicas basadas en el tamaño actual de la ventana
        rectKill = new Rectangle(w - btnSize - gap, h - btnSize - gap, btnSize, btnSize);
        rectReport = new Rectangle(w - (btnSize + gap) * 2, h - btnSize - gap, btnSize, btnSize);
        rectVent = new Rectangle(w - btnSize - gap, h - (btnSize + gap) * 2, btnSize, btnSize);
        rectSabotage = new Rectangle(w - (btnSize + gap) * 2, h - (btnSize + gap) * 2, btnSize, btnSize);
        rectUse = new Rectangle(w - btnSize - gap, h - (btnSize + gap) * 2, btnSize, btnSize); // Mismo lugar que Vent
        
        int emergencyX = w - (btnSize + gap) * 2 + (local.isImpostor() ? 40 : 20);
        int emergencyY = h - (btnSize + gap) * 2 - gap - (btnSize / 2) - (local.isImpostor() ? 20 : 10);
        rectEmergencia = new Rectangle(emergencyX, emergencyY, btnSize, btnSize);

        if (local.isVivo()) {
            // Un solo contexto Graphics2D compartido para todos los botones del HUD
            Graphics2D g2hud = (Graphics2D) g.create();
            try {
                // Botón Emergencia (Solo si está cerca)
                if (cercaDeBoton) {
                    dibujarBotonAccion(g2hud, "boton/botnhud/boton-hud.png", rectEmergencia, manejadorEntrada.accionEmergencia, true, 0);
                }
                
                // Botón Reportar (Siempre visible para tripulantes y impostores vivos)
                boolean puedeReportar = local.hayCuerpoCerca();
                int cdReporte = local.getCooldownReporte();
                dibujarBotonAccion(g2hud, "Reportar_boton.png", rectReport, manejadorEntrada.accionReportar, puedeReportar && cdReporte <= 0, cdReporte);

                if (local.isImpostor()) {
                    // Botones exclusivos de Impostor
                    boolean puedeMatar = local.hayVictimaCerca();
                    int cdMatar = local.getCooldownAsesinato();
                    dibujarBotonAccion(g2hud, "botonkill.png", rectKill, manejadorEntrada.accionMatar, puedeMatar && cdMatar <= 0, cdMatar);
                    
                    int cdVent = local.getCooldownVentilacion();
                    dibujarBotonAccion(g2hud, "Ventana_boton.png", rectVent, manejadorEntrada.accionVentilar, cdVent <= 0, cdVent);
                    
                    int cdSabotaje = local.getCooldownSabotaje();
                    dibujarBotonAccion(g2hud, "Sabotaje_boton.png", rectSabotage, manejadorEntrada.accionSabotaje, cdSabotaje <= 0, cdSabotaje);
                } else {
                    // Botón USAR para tripulantes
                    dibujarBotonAccion(g2hud, "Ventana_boton.png", rectUse, manejadorEntrada.accionUsar, true, 0);
                }
            } finally {
                g2hud.dispose(); // Un solo dispose para todos los botones
            }
        }
    }

    /**
     * Dibuja un botón individual con animación de pulsación, filtro gris si no está habilitado y contador de cooldown
     */
    //  Recibe Graphics2D directamente (el contexto compartido del HUD). No hace create()/dispose().
    private void dibujarBotonAccion(Graphics2D g2, String imgName, Rectangle rect, boolean presionado, boolean habilitado, int cooldown) {
        Image img = obtenerImagenFija(imgName);
        if (img == null) return;

        // Guardar el composite original para restaurarlo al final
        java.awt.Composite compositeOriginal = g2.getComposite();

        if (!habilitado) {
            g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.4f));
        }

        if (presionado && habilitado) {
            int newW = (int)(rect.width * 0.9);
            int newH = (int)(rect.height * 0.9);
            int offsetX = (rect.width - newW) / 2;
            int offsetY = (rect.height - newH) / 2;
            g2.drawImage(img, rect.x + offsetX, rect.y + offsetY, newW, newH, null);
        } else {
            g2.drawImage(img, rect.x, rect.y, rect.width, rect.height, null);
        }

        g2.setComposite(compositeOriginal); // restaurar, no crear instancia extra

        if (cooldown > 0) {
            String textoCd = String.valueOf((int) Math.ceil(cooldown / 60.0));
            g2.setFont(cargarFuente(35f));
            FontMetrics fm = g2.getFontMetrics();
            int tx = rect.x + (rect.width - fm.stringWidth(textoCd)) / 2;
            int ty = rect.y + ((rect.height - fm.getHeight()) / 2) + fm.getAscent();
            g2.setColor(Color.BLACK);
            g2.drawString(textoCd, tx + 2, ty + 2);
            g2.setColor(Color.WHITE);
            g2.drawString(textoCd, tx, ty);
        }
    }

    // Método dibujarCampoVisual() eliminado por la optimización de Clipping Circular

    private void dibujarTripulante(Graphics g, Jugador j) {
        Jugador local = estadoJuego.getJugadorLocal();
        long ahora = System.currentTimeMillis();
        int dir = j.getDireccion();
        if (dir == 0) dir = 1;
        int w = 40, h = 50; 

        // 1. DIBUJAR EL CUERPO (solo si está muerto y NO fue expulsado)
        if (!j.isVivo() && !j.isFueExpulsado()) {
            int bx = j.getXMuerte();
            int by = j.getYMuerte();
            
            // Si las coordenadas son 0,0 (porque murió justo al iniciar o error), usamos su actual
            if (bx == 0 && by == 0) {
                bx = j.getX(); by = j.getY();
            }

            long tiempoMuerto = ahora - j.getTiempoInicioMuerte();
            String rutaMoldeCuerpo;
            String claveCacheCuerpo;
            if (tiempoMuerto < 2880) { // 48 frames a 60ms cada uno
                int frameActual = (int) (tiempoMuerto / 60) + 1;
                if (frameActual > 48) frameActual = 48;
                rutaMoldeCuerpo = String.format("sprites/Muerte/killalien_victim%04d.png", frameActual);
                claveCacheCuerpo = "muerte_" + frameActual;
            } else {
                rutaMoldeCuerpo = "sprites/Muerte/killalien_victim0048.png";
                claveCacheCuerpo = "muerte_final";
            }
            
            // AJUSTE: Usamos la dirección grabada al morir, no la del fantasma actual
            int dirMuerte = j.getDireccionMuerte();
            if (dirMuerte == 0) dirMuerte = 1;

            BufferedImage spriteCuerpo = obtenerSpriteColoreado(rutaMoldeCuerpo, j.getColor(), claveCacheCuerpo);
            if (spriteCuerpo != null) {
                if (dirMuerte == 1) {
                    g.drawImage(spriteCuerpo, bx, by, w, h, null);
                } else {
                    g.drawImage(spriteCuerpo, bx + w, by, -w, h, null);
                }
            } else {
                g.setColor(j.getColor().darker().darker());
                g.fillRoundRect(bx, by + 25, 45, 20, 10, 10);
                g.setColor(Color.WHITE);
                g.fillOval(bx + 15, by + 20, 10, 10);
            }
            g.setColor(Color.RED);
            g.drawString("REPORTAR", bx - 10, by - 5);
        }

        // 2. ¿ES VISIBLE EL PERSONAJE O FANTASMA PARA MÍ?
        boolean esFantasma = !j.isVivo();
        boolean yoSoyFantasma = (local != null && !local.isVivo());
        
        // Si el objetivo es un fantasma, SOLO lo puedo ver si yo también soy fantasma (o soy yo mismo)
        if (esFantasma && !yoSoyFantasma && j != local) {
            return; // No dibujamos al fantasma
        }

        // Determinar coordenadas de dibujo del personaje/fantasma activo
        int x, y;
        if (j == local) {
            x = j.getX();
            y = j.getY();
        } else {
            x = j.getDrawX();
            y = j.getDrawY();
        }

        String rutaMolde = "";
        String claveCache = "";

        if (esFantasma) {
            // FANTASMA (flota continuamente)
            int frameFantasma = (int) ((ahora / 60) % 48) + 1;
            rutaMolde = String.format("sprites/Fantasma/ghost%04d.png", frameFantasma);
            claveCache = "fantasma_" + frameFantasma;
        } else if (j.isAtacando()) {
            long tiempoAtaque = ahora - j.getTiempoInicioAsesinato();
            int frameActual = (int) (tiempoAtaque / 60) + 1;
            if (frameActual > 48) frameActual = 48;
            rutaMolde = String.format("sprites/Ataque del impostor/killalien_imposter%04d.png", frameActual);
            claveCache = "ataque_" + frameActual;
        } else {
            boolean enMovimiento = false;
            if (j == local) {
                enMovimiento = (manejadorEntrada.arriba || manejadorEntrada.abajo || 
                                manejadorEntrada.izquierda || manejadorEntrada.derecha);
            } else {
                enMovimiento = j.isMoviendose();
            }
            
            if (enMovimiento) {
                int[] framesDisponibles = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
                int indexFrame = (int) ((ahora / 80) % framesDisponibles.length);
                int frameActual = framesDisponibles[indexFrame];
                rutaMolde = String.format("sprites/caminando/Walk%04d.png", frameActual);
                claveCache = "walk" + frameActual;
            } else {
                rutaMolde = "sprites/sin moverse/idle.png";
                claveCache = "idle";
            }
        }

        // llamamos al motor de palette swapping
        BufferedImage spriteActual = obtenerSpriteColoreado(rutaMolde, j.getColor(), claveCache);

        Graphics2D g2d = (Graphics2D) g.create();
        // BILINEAR es suficiente para sprites de 40x50px escalados — BICUBIC era innecesariamente caro
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        if (spriteActual != null) {
            if (dir == 1) {
                g2d.drawImage(spriteActual, x, y, w, h, null);
            } else {
                g2d.drawImage(spriteActual, x + w, y, -w, h, null);
            }
            
            // --- DIBUJAR SOMBRERO (Solo para tripulantes vivos) ---
            if (j.isVivo() && !j.getSombrero().equals("ninguno")) {
                String rutaSombrero = "sprites/sombreros/" + j.getSombrero() + ".png";
                Image imgSombrero = obtenerImagenFija(rutaSombrero);
                if (imgSombrero != null) {
                    // Posicionamiento del sombrero (ajustable según el arte)
                    int sw = (int)(w * 1.2); // Un poco más ancho que el cuerpo
                    int sh = (int)(h * 0.8); // Altura proporcional
                    int sx = x - (sw - w) / 2;
                    int sy = y - (int)(sh * 0.6); // Lo subimos a la cabeza
                    
                    if (dir == 1) {
                        g2d.drawImage(imgSombrero, sx, sy, sw, sh, null);
                    } else {
                        g2d.drawImage(imgSombrero, sx + sw, sy, -sw, sh, null);
                    }
                }
            }
        } else {
            // fallback (cuadros)
            g2d.setColor(j.getColor());
            if (dir == 1) g2d.fillRect(x - 5, y + 10, 10, 25);
            else          g2d.fillRect(x + 30 - 5, y + 10, 10, 25);
            g2d.fillRoundRect(x, y, 30, 40, 15, 15);
            g2d.fillRect(x, y + 40 - 5, 10, 15);
            g2d.fillRect(x + 30 - 10, y + 40 - 5, 10, 15);
            g2d.setColor(new Color(150, 200, 220));
            if (dir == 1) g2d.fillRoundRect(x + 15, y + 10, 18, 12, 5, 5);
            else          g2d.fillRoundRect(x - 3,  y + 10, 18, 12, 5, 5);
        }

        // HUD Textos
        if (manejadorEntrada.modoDesarrollador) {
            g2d.setColor(Color.GREEN);
            g2d.drawRect(j.getHitbox().x, j.getHitbox().y, j.getHitbox().width, j.getHitbox().height);
        }

        // --- DIBUJAR NOMBRE ---
        if (j.isImpostor() && local != null && local.isImpostor()) {
            g2d.setColor(Color.RED);
        } else {
            g2d.setColor(Color.WHITE);
        }
        
        // Si es fantasma, aplicamos transparencia al nombre (60% de opacidad)
        if (esFantasma) {
            g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.6f));
        }
        
        String lblNombre = j.getNombre();
        if (esFantasma && manejadorEntrada.modoDesarrollador) lblNombre += " (Fantasma)";
        else if (j == local && j.isImpostor()) lblNombre += " (Impostor)";
        
        g2d.drawString(lblNombre, x, y - 10);
        
        // Resetear transparencia
        g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 1.0f));

        if (j == local && j.isImpostor() && manejadorEntrada.modoDesarrollador) {
            g2d.setColor(Color.ORANGE);
            g2d.drawString("[Q] Matar | [H] Luces", x - 30, y + 65);
        }
        
        g2d.dispose();
    }
}