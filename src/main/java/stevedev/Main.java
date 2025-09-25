package stevedev;

import javafx.application.Application;
import javafx.stage.Stage;
import stevedev.gui.MainWindow;

/**
 * Clase principal de la aplicación Gestor de Descargas
 * 
 * Este proyecto implementa un gestor de descargas con verificación de integridad
 * que permite descargas múltiples en paralelo sin bloquear la GUI.
 * 
 * Características principales:
 * - Descargas paralelas con control de concurrencia
 * - Verificación de integridad mediante hashes SHA-256
 * - Interfaz gráfica intuitiva con barras de progreso
 * - Registro de descargas en formato JSON y CSV
 * - Cancelación de descargas en progreso
 * - Gestión de archivos parciales
 * 
 * @author MrxSteve
 * @version 1.0
 */
public class Main extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        try {
            // Crear y mostrar la ventana principal
            MainWindow mainWindow = new MainWindow(primaryStage);
            mainWindow.show();
            
        } catch (Exception e) {
            System.err.println("Error iniciando la aplicación: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Punto de entrada principal de la aplicación
     */
    public static void main(String[] args) {
        // Configurar propiedades del sistema para JavaFX
        System.setProperty("javafx.preloader", "");
        
        // Información de la aplicación
        System.out.println("=================================================");
        System.out.println("    Gestor de Descargas v1.0");
        System.out.println("=================================================");
        System.out.println("Iniciando aplicación...");
        
        try {
            // Lanzar aplicación JavaFX
            launch(args);
            
        } catch (Exception e) {
            System.err.println("Error crítico en la aplicación: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        
        System.out.println("Aplicación finalizada correctamente.");
    }
}