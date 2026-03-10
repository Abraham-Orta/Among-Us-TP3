/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.amongus.project.modelo;

import java.nio.file.Files;
import java.nio.file.Paths;
import org.json.JSONArray;
import org.json.JSONObject;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

public class CargadorNivel {
    private int mapWidth;
    private int mapHeight;
    private int tileWidth;
    private int tileHeight;

    // Matriz 2D para guardar el mapa de tiles (ej. la capa "suelo")
    private int[][] capaSuelo;
    
    // Lista para guardar las áreas donde el jugador chocará
    private List<Rectangle2D.Double> colisiones;
    private List<Rectangle2D.Double> alcantarillas;

    public CargadorNivel(String rutaArchivo) {
        colisiones = new ArrayList<>();
        alcantarillas = new ArrayList<>();
        cargarArchivo(rutaArchivo);
    }

    private void cargarArchivo(String ruta) {
        try {
            // 1. Leer todo el archivo .tmj como una cadena de texto
            String contenido = new String(Files.readAllBytes(Paths.get(ruta)));
            JSONObject mapaJson = new JSONObject(contenido);
            
            // 2. Extraer las dimensiones del mapa
            this.mapWidth = mapaJson.getInt("width");   // 90 en tu mapa
            this.mapHeight = mapaJson.getInt("height"); // 60 en tu mapa
            
            // Extraer el tamaño de cada "cuadrito" (suele ser 16 o 32 píxeles)
            this.tileWidth = mapaJson.has("tilewidth") ? mapaJson.getInt("tilewidth") : 32; 
            this.tileHeight = mapaJson.has("tileheight") ? mapaJson.getInt("tileheight") : 32;

            // 3. Obtener todas las capas
            JSONArray layers = mapaJson.getJSONArray("layers");

            for (int i = 0; i < layers.length(); i++) {
                JSONObject layer = layers.getJSONObject(i);
                String tipoCapa = layer.getString("type");
                String nombreCapa = layer.getString("name");

                // 4. CARGAR CAPA GRÁFICA (Los tiles)
                if (tipoCapa.equals("tilelayer") && nombreCapa.equals("suelo")) {
                    JSONArray data = layer.getJSONArray("data");
                    capaSuelo = new int[mapHeight][mapWidth];
                    
                    int index = 0;
                    for (int y = 0; y < mapHeight; y++) {
                        for (int x = 0; x < mapWidth; x++) {
                            // Guardamos el ID del gráfico en su coordenada exacta
                            capaSuelo[y][x] = data.getInt(index);
                            index++;
                        }
                    }
                    System.out.println("Capa 'suelo' cargada para ser dibujada.");
                }

                // 5. CARGAR CAPA DE COLISIONES
                
               else if (tipoCapa.equals("objectgroup") && nombreCapa.equals("zonas_alcantarillas")) {
                    JSONArray objects = layer.getJSONArray("objects");
                    
                    for (int j = 0; j < objects.length(); j++) {
                        JSONObject obj = objects.getJSONObject(j);
                        
                        // Obtenemos la caja exacta que dibujaste en Tiled
                        double objX = obj.getDouble("x");
                        double objY = obj.getDouble("y");
                        double objWidth = obj.getDouble("width");
                        double objHeight = obj.getDouble("height");
                        
                        alcantarillas.add(new Rectangle2D.Double(objX, objY, objWidth, objHeight));
                    }
                    System.out.println("Cargadas " + alcantarillas.size() + " alcantarillas reales.");
                }
                
                
                else if (tipoCapa.equals("objectgroup") && nombreCapa.equals("colision")) {
                    JSONArray objects = layer.getJSONArray("objects");
                    
                    for (int j = 0; j < objects.length(); j++) {
                        JSONObject obj = objects.getJSONObject(j);
                        
                        // Obtener coordenadas y tamaño del obstáculo
                        double objX = obj.getDouble("x");
                        double objY = obj.getDouble("y");
                        double objWidth = obj.getDouble("width");
                        double objHeight = obj.getDouble("height");
                        
                        colisiones.add(new Rectangle2D.Double(objX, objY, objWidth, objHeight));
                    }
                    System.out.println("Cargadas " + colisiones.size() + " zonas de colisión.");
                }
            }
        } catch (Exception e) {
            System.out.println("Error procesando el JSON: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Getters para que el resto de tu juego acceda a los datos
    public int[][] getCapaSuelo() { return capaSuelo; }
    public List<Rectangle2D.Double> getColisiones() { return colisiones; }
    public int getTileWidth() { return tileWidth; }
    public int getTileHeight() { return tileHeight; }
    public List<Rectangle2D.Double> getAlcantarillas() { return alcantarillas; }
}