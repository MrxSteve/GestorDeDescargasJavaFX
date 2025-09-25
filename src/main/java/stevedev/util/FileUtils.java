package stevedev.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import stevedev.model.DownloadLog;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilidades para manejo de archivos y logs
 */
public class FileUtils {
    private static final String LOG_DIR = "logs";
    private static final String DOWNLOADS_DIR = "downloads";
    private static final ObjectMapper objectMapper;
    
    static {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Crea los directorios necesarios para la aplicación
     */
    public static void createDirectories() {
        try {
            Files.createDirectories(Paths.get(LOG_DIR));
            Files.createDirectories(Paths.get(DOWNLOADS_DIR));
        } catch (IOException e) {
            System.err.println("Error creando directorios: " + e.getMessage());
        }
    }

    /**
     * Obtiene el directorio de descargas
     */
    public static String getDownloadsDirectory() {
        return DOWNLOADS_DIR;
    }

    /**
     * Guarda un log de descarga en formato JSON
     */
    public static void saveLogAsJson(DownloadLog log) {
        String fileName = "download_log_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".json";
        Path logFile = Paths.get(LOG_DIR, fileName);
        
        try {
            List<DownloadLog> logs = new ArrayList<>();
            
            // Si el archivo existe, cargar logs existentes
            if (Files.exists(logFile)) {
                logs = loadLogsFromJson(logFile.toString());
            }
            
            // Agregar nuevo log
            logs.add(log);
            
            // Guardar todos los logs
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(logFile.toFile(), logs);
            
        } catch (IOException e) {
            System.err.println("Error guardando log JSON: " + e.getMessage());
        }
    }

    /**
     * Guarda un log de descarga en formato CSV
     */
    public static void saveLogAsCsv(DownloadLog log) {
        String fileName = "download_log_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv";
        Path logFile = Paths.get(LOG_DIR, fileName);
        
        try {
            boolean fileExists = Files.exists(logFile);
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(logFile.toFile(), true))) {
                // Escribir header si es un archivo nuevo
                if (!fileExists) {
                    writer.println("Timestamp,URL,FileName,FileSize,Hash,ExpectedHash,Duration,Result,ErrorMessage");
                }
                
                // Escribir datos del log
                writer.printf("%s,%s,%s,%d,%s,%s,%d,%s,%s%n",
                    log.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    escapeCSV(log.getUrl()),
                    escapeCSV(log.getFileName()),
                    log.getFileSize(),
                    escapeCSV(log.getHash()),
                    escapeCSV(log.getExpectedHash()),
                    log.getDurationSeconds(),
                    escapeCSV(log.getResult()),
                    escapeCSV(log.getErrorMessage())
                );
            }
            
        } catch (IOException e) {
            System.err.println("Error guardando log CSV: " + e.getMessage());
        }
    }

    /**
     * Carga logs desde un archivo JSON
     */
    public static List<DownloadLog> loadLogsFromJson(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                return objectMapper.readValue(file, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, DownloadLog.class));
            }
        } catch (IOException e) {
            System.err.println("Error cargando logs JSON: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    /**
     * Verifica si un archivo existe
     */
    public static boolean fileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }

    /**
     * Obtiene el tamaño de un archivo
     */
    public static long getFileSize(String filePath) {
        try {
            return Files.size(Paths.get(filePath));
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * Elimina un archivo
     */
    public static boolean deleteFile(String filePath) {
        try {
            return Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            System.err.println("Error eliminando archivo: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene el nombre de archivo desde una URL con detección mejorada de extensiones
     */
    public static String extractFileNameFromUrl(String url) {
        try {
            // Obtener la parte final de la URL
            String fileName = url.substring(url.lastIndexOf('/') + 1);
            
            // Remover parámetros de query si existen
            int queryIndex = fileName.indexOf('?');
            if (queryIndex != -1) {
                fileName = fileName.substring(0, queryIndex);
            }
            
            // Remover fragmentos (#)
            int fragmentIndex = fileName.indexOf('#');
            if (fragmentIndex != -1) {
                fileName = fileName.substring(0, fragmentIndex);
            }
            
            // Si no hay nombre o no tiene extensión, inferir desde el URL o Content-Type
            if (fileName.isEmpty() || !fileName.contains(".")) {
                fileName = generateFileNameFromUrl(url);
            }
            
            // Limpiar caracteres inválidos para nombres de archivo
            fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
            
            return fileName.isEmpty() ? "downloaded_file.bin" : fileName;
        } catch (Exception e) {
            return "downloaded_file.bin";
        }
    }
    
    /**
     * Genera un nombre de archivo basado en el patrón de la URL
     */
    private static String generateFileNameFromUrl(String url) {
        try {
            // Detectar extensión común por patrones en la URL
            if (url.contains("image/jpeg") || url.contains("jpg")) {
                return "image_" + System.currentTimeMillis() + ".jpg";
            } else if (url.contains("image/png") || url.contains("png")) {
                return "image_" + System.currentTimeMillis() + ".png";
            } else if (url.contains("image/gif") || url.contains("gif")) {
                return "image_" + System.currentTimeMillis() + ".gif";
            } else if (url.contains("pdf")) {
                return "document_" + System.currentTimeMillis() + ".pdf";
            } else if (url.contains("txt") || url.contains("text")) {
                return "text_" + System.currentTimeMillis() + ".txt";
            } else if (url.contains("mp4") || url.contains("video")) {
                return "video_" + System.currentTimeMillis() + ".mp4";
            } else if (url.contains("mp3") || url.contains("audio")) {
                return "audio_" + System.currentTimeMillis() + ".mp3";
            } else if (url.contains("zip")) {
                return "archive_" + System.currentTimeMillis() + ".zip";
            } else {
                // Por defecto, usar el último segmento de la URL o un nombre genérico
                String[] segments = url.split("/");
                String lastSegment = segments[segments.length - 1];
                if (!lastSegment.isEmpty() && lastSegment.length() < 50) {
                    return lastSegment + ".bin";
                } else {
                    return "downloaded_file_" + System.currentTimeMillis() + ".bin";
                }
            }
        } catch (Exception e) {
            return "downloaded_file_" + System.currentTimeMillis() + ".bin";
        }
    }

    /**
     * Escapa strings para CSV
     */
    private static String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * Formatea bytes a string legible
     */
    public static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}