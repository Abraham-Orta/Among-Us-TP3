package com.amongus.project.vista;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.RadialGradientPaint;
import java.awt.Paint;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
    private Rectangle rectKill, rectReport, rectVent, rectSabotage;

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
                if (rectKill != null && rectKill.contains(e.getPoint())) { manejadorEntrada.accionMatar = true; clickHUD = true; }
                if (rectReport != null && rectReport.contains(e.getPoint())) { manejadorEntrada.accionReportar = true; clickHUD = true; }
                if (rectVent != null && rectVent.contains(e.getPoint())) { manejadorEntrada.accionVentilar = true; clickHUD = true; }
                if (rectSabotage != null && rectSabotage.contains(e.getPoint())) { manejadorEntrada.accionSabotaje = true; clickHUD = true; }
                
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

        Graphics2D g2d = (Graphics2D) g;
        g2d.translate(-camX, -camY);

        // Capa 1: Mapa — le pasamos el flag de hitboxes de nuestra instancia del manejador
        if (mapa != null) mapa.render(g, manejadorEntrada.modoDesarrollador);

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
            g.setColor(Color.WHITE);
            g.drawString(j.getNombre(), j.getX(), j.getY() - 10);
        }
        if (local != null && !estadoJuego.getJugadores().contains(local)) {
            dibujarTripulante(g, local);
            g.setColor(Color.WHITE);
            g.drawString(local.getNombre(), local.getX(), local.getY() - 10);
        }

        // Capa 3: Niebla de guerra
        if (local != null && mapa != null) {
            dibujarCampoVisual(g2d, local, mapa.getAncho(), mapa.getAlto());
        }

        g2d.translate(camX, camY);

        if (estadoJuego.getJugadores().isEmpty() && local == null) {
            g.setColor(Color.WHITE);
            g.drawString("No hay jugadores. Inicia partida.", 300, 300);
        }
        
        dibujarHUD(g);
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

        if (local.isVivo()) {
            // Botón Reportar (Siempre visible para tripulantes y impostores vivos)
            dibujarBotonAccion(g, "Reportar_boton.png", rectReport, manejadorEntrada.accionReportar);

            if (local.isImpostor()) {
                // Botones exclusivos de Impostor
                dibujarBotonAccion(g, "botonkill.png", rectKill, manejadorEntrada.accionMatar);
                dibujarBotonAccion(g, "Ventana_boton.png", rectVent, manejadorEntrada.accionVentilar);
                dibujarBotonAccion(g, "Sabotaje_boton.png", rectSabotage, manejadorEntrada.accionSabotaje);
            }
        }
    }

    /**
     * Dibuja un botón individual con animación de pulsación
     */
    private void dibujarBotonAccion(Graphics g, String imgName, Rectangle rect, boolean presionado) {
        Image img = obtenerImagenFija(imgName);
        if (img == null) return;

        if (presionado) {
            // Animación: reducir un poco el tamaño al presionar
            int offset = 8;
            g.drawImage(img, rect.x + offset, rect.y + offset, rect.width - offset * 2, rect.height - offset * 2, null);
        } else {
            g.drawImage(img, rect.x, rect.y, rect.width, rect.height, null);
        }
    }

    // Caché para la niebla de guerra — evita recrear el gradiente 60 veces/segundo
    private RadialGradientPaint gradCache;
    private float gradCacheX, gradCacheY, gradCacheRadio;

    private void dibujarCampoVisual(Graphics2D g2d, Jugador local, int anchoMapa, int altoMapa) {
        float radio;
        if (local.isImpostor()) {
            radio = 350.0f;
        } else {
            radio = estadoJuego.areLucesSaboteadas() ? 50.0f : 180.0f;
        }
        if (radio <= 0) radio = 1.0f;

        float cx = local.getX() + 15;
        float cy = local.getY() + 20;

        // Solo recrear el gradiente si cambió la posición o el radio
        if (gradCache == null || cx != gradCacheX || cy != gradCacheY || radio != gradCacheRadio) {
            gradCache = new RadialGradientPaint(
                new Point2D.Float(cx, cy), radio,
                new float[]{0.0f, 0.7f, 1.0f},
                new Color[]{
                    new Color(0, 0, 0,   0),
                    new Color(0, 0, 0, 120),
                    new Color(0, 0, 0, 255)
                }
            );
            gradCacheX = cx;
            gradCacheY = cy;
            gradCacheRadio = radio;
        }

        Paint original = g2d.getPaint();
        g2d.setPaint(gradCache);
        // Pintar un área lo suficientemente grande para cubrir la ventana
        // sin importar si el jugador está en el borde del mapa
        int rectX = (int)(cx - getWidth());
        int rectY = (int)(cy - getHeight());
        g2d.fillRect(rectX, rectY, getWidth() * 2, getHeight() * 2);
        g2d.setPaint(original);
    }

    private void dibujarTripulante(Graphics g, Jugador j) {
        Jugador local = estadoJuego.getJugadorLocal();
        int x, y;
        if (j == local) {
            x = j.getX();
            y = j.getY();
        } else {
            x = j.getDrawX();
            y = j.getDrawY();
        }
        
        int w = 40, h = 50; 
        int dir = j.getDireccion();
        if (dir == 0) dir = 1;

        // Lógica de Animación
        boolean enMovimiento = false;
        if (j == estadoJuego.getJugadorLocal()) {
            enMovimiento = (manejadorEntrada.arriba || manejadorEntrada.abajo || 
                            manejadorEntrada.izquierda || manejadorEntrada.derecha);
        } else {
            enMovimiento = j.isMoviendose();
        }
        
        // Si es otro jugador de la red, evaluamos si cambió de posición en el último frame
        // (Por simplicidad lo trataremos como movimiento genérico si es necesario)

        String rutaMolde = "";
        String claveCache = "";
        long ahora = System.currentTimeMillis();

        if (!j.isVivo()) {
            long tiempoMuerto = ahora - j.getTiempoInicioMuerte();
            if (tiempoMuerto < 2880) { // 48 frames a 60ms cada uno
                int frameActual = (int) (tiempoMuerto / 60) + 1;
                if (frameActual > 48) frameActual = 48;
                String nombreFrame = String.format("killalien_victim%04d.png", frameActual);
                rutaMolde = "sprites/Muerte/" + nombreFrame;
                claveCache = "muerte_" + frameActual;
            } else {
                // Se queda en el último frame (cuerpo en el suelo)
                rutaMolde = "sprites/Muerte/killalien_victim0048.png";
                claveCache = "muerte_final";
            }
        } else if (j.isAtacando()) {
            long tiempoAtaque = ahora - j.getTiempoInicioAsesinato();
            int frameActual = (int) (tiempoAtaque / 60) + 1;
            if (frameActual > 48) frameActual = 48;
            String nombreFrame = String.format("killalien_imposter%04d.png", frameActual);
            rutaMolde = "sprites/Ataque del impostor/" + nombreFrame;
            claveCache = "ataque_" + frameActual;
        } else if (enMovimiento) {
            // Alternar frames basándose en el reloj del sistema (cada 80ms cambia de paso)
            int[] framesDisponibles = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
            int indexFrame = (int) ((System.currentTimeMillis() / 80) % framesDisponibles.length);
            int frameActual = framesDisponibles[indexFrame];
            String nombreFrame = String.format("Walk%04d.png", frameActual);
            
            rutaMolde = "sprites/caminando/" + nombreFrame;
            claveCache = "walk" + frameActual;
        } else {
            rutaMolde = "sprites/sin moverse/idle.png";
            claveCache = "idle";
        }

        // llamamos al motor de palette swapping para colorear y obtener la imagen actual
        BufferedImage spriteActual = obtenerSpriteColoreado(rutaMolde, j.getColor(), claveCache);

        if (spriteActual != null) {
            // renderizar imagen procesada
            if (dir == 1) {
                // mirando derecha
                g.drawImage(spriteActual, x, y, w, h, null);
            } else {
                // mirando izquierda (modo espejo horizontal)
                g.drawImage(spriteActual, x + w, y, -w, h, null);
            }

            if (!j.isVivo()) {
                g.setColor(Color.RED);
                g.drawString("REPORTAR", x - 10, y - 5);
            }
        } else {
            // fallback: si no existen los png, dibuja los cuadrados como antes
            if (!j.isVivo()) {
                g.setColor(j.getColor().darker().darker());
                g.fillRoundRect(x, y + 25, 45, 20, 10, 10);
                g.setColor(Color.WHITE);
                g.fillOval(x + 15, y + 20, 10, 10);
                g.setColor(Color.RED);
                g.drawString("PARALIZADO", x - 10, y - 5);
            } else {
                g.setColor(j.getColor());
                if (dir == 1) g.fillRect(x - 5, y + 10, 10, 25);
                else          g.fillRect(x + 30 - 5, y + 10, 10, 25);
                g.fillRoundRect(x, y, 30, 40, 15, 15);
                g.fillRect(x, y + 40 - 5, 10, 15);
                g.fillRect(x + 30 - 10, y + 40 - 5, 10, 15);

                g.setColor(new Color(150, 200, 220));
                if (dir == 1) g.fillRoundRect(x + 15, y + 10, 18, 12, 5, 5);
                else          g.fillRoundRect(x - 3,  y + 10, 18, 12, 5, 5);
            }
        }

        // --- HUD Y MODO DESARROLLADOR ---
        if (manejadorEntrada.modoDesarrollador) {
            g.setColor(Color.GREEN);
            g.drawRect(j.getHitbox().x, j.getHitbox().y, j.getHitbox().width, j.getHitbox().height);
        }

        // HUD - Textos de nombre y rol
        g.setColor(Color.WHITE);
        g.drawString(j.getNombre(), x, y - 10);

        if (j == local && j.isImpostor()) {
            g.setColor(Color.RED);
            g.drawString(j.getNombre() + " (Impostor)", x, y - 10);
            
            // Solo mostrar las teclas de acción si estamos en modo desarrollador (F3)
            if (manejadorEntrada.modoDesarrollador) {
                g.setColor(Color.ORANGE);
                g.drawString("[Q] Matar | [H] Luces", x - 30, y + 65);
            }
        }
    }
}