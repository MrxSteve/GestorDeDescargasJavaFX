package stevedev.core;

import okhttp3.*;
import stevedev.model.DownloadItem;
import stevedev.util.ProgressListener;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tarea de descarga que maneja la descarga de un archivo individual
 */
public class DownloadTask {
    private final DownloadItem item;
    private final ProgressListener progressListener;
    private final OkHttpClient httpClient;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private Call currentCall;

    public DownloadTask(DownloadItem item, ProgressListener progressListener, OkHttpClient httpClient) {
        this.item = item;
        this.progressListener = progressListener;
        this.httpClient = httpClient;
    }

    /**
     * Ejecuta la descarga de forma asíncrona
     * @return CompletableFuture que se completa cuando la descarga termina
     */
    public CompletableFuture<DownloadItem> executeAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return download();
            } catch (Exception e) {
                item.setStatus(DownloadItem.DownloadStatus.FAILED);
                item.setErrorMessage(e.getMessage());
                item.setEndTime(LocalDateTime.now());
                notifyProgress();
                return item;
            }
        });
    }

    /**
     * Cancela la descarga
     */
    public void cancel() {
        cancelled.set(true);
        item.setCancelled(true);
        item.setStatus(DownloadItem.DownloadStatus.CANCELLED);
        
        if (currentCall != null) {
            currentCall.cancel();
        }
        
        // Eliminar archivo parcial si existe
        try {
            Files.deleteIfExists(Paths.get(item.getDestinationPath()));
        } catch (IOException e) {
            System.err.println("Error eliminando archivo parcial: " + e.getMessage());
        }
        
        item.setEndTime(LocalDateTime.now());
        notifyProgress();
    }

    /**
     * Verifica si la descarga fue cancelada
     */
    public boolean isCancelled() {
        return cancelled.get();
    }

    /**
     * Ejecuta la descarga
     */
    private DownloadItem download() throws IOException {
        if (cancelled.get()) {
            return item;
        }

        item.setStartTime(LocalDateTime.now());
        item.setStatus(DownloadItem.DownloadStatus.DOWNLOADING);
        notifyProgress();

        Request request = new Request.Builder()
                .url(item.getUrl())
                .build();

        currentCall = httpClient.newCall(request);
        
        try (Response response = currentCall.execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP Error: " + response.code() + " - " + response.message());
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("Response body es null");
            }

            // Verificar Content-Type para detectar el tipo de archivo real
            String contentType = response.header("Content-Type", "");
            detectAndUpdateFileExtension(contentType);

            // Obtener tamaño total del archivo
            long contentLength = body.contentLength();
            if (contentLength > 0) {
                item.setTotalSize(contentLength);
                notifyProgress();
            }

            // Descargar archivo
            downloadWithProgress(body.byteStream(), item.getDestinationPath());

            if (cancelled.get()) {
                return item;
            }

            // Verificar hash si está disponible
            if (item.getExpectedHash() != null && !item.getExpectedHash().isEmpty()) {
                verifyIntegrity();
            } else {
                // Calcular hash del archivo descargado para registro
                calculateFileHash();
            }

            if (!cancelled.get() && item.getStatus() != DownloadItem.DownloadStatus.HASH_MISMATCH) {
                item.setStatus(DownloadItem.DownloadStatus.COMPLETED);
            }

        } catch (IOException e) {
            if (cancelled.get()) {
                item.setStatus(DownloadItem.DownloadStatus.CANCELLED);
            } else {
                item.setStatus(DownloadItem.DownloadStatus.FAILED);
                item.setErrorMessage(e.getMessage());
            }
            throw e;
        } finally {
            item.setEndTime(LocalDateTime.now());
            notifyProgress();
        }

        return item;
    }

    /**
     * Descarga el archivo con reporte de progreso
     */
    private void downloadWithProgress(InputStream inputStream, String destinationPath) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(inputStream);
             BufferedOutputStream bos = new BufferedOutputStream(
                     Files.newOutputStream(Paths.get(destinationPath), StandardOpenOption.CREATE, StandardOpenOption.WRITE))) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytesRead = 0;

            while ((bytesRead = bis.read(buffer)) != -1 && !cancelled.get()) {
                bos.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
                
                item.setDownloadedSize(totalBytesRead);
                
                // Actualizar progreso cada cierta cantidad de bytes para evitar spam
                if (totalBytesRead % (64 * 1024) == 0 || 
                    (item.getTotalSize() > 0 && totalBytesRead == item.getTotalSize())) {
                    notifyProgress();
                }
            }
            
            bos.flush();
            
            // Si no teníamos el tamaño total, actualizarlo ahora
            if (item.getTotalSize() == 0) {
                item.setTotalSize(totalBytesRead);
            }
            
            item.setDownloadedSize(totalBytesRead);
            notifyProgress();
        }
    }

    /**
     * Verifica la integridad del archivo descargado
     */
    private void verifyIntegrity() {
        if (cancelled.get()) return;
        
        item.setStatus(DownloadItem.DownloadStatus.VERIFYING);
        notifyProgress();
        
        try {
            HashVerifier.HashType hashType = HashVerifier.detectHashType(item.getExpectedHash());
            if (hashType == null) {
                hashType = HashVerifier.HashType.SHA256; // Por defecto
            }
            
            String calculatedHash = HashVerifier.calculateHash(item.getDestinationPath(), hashType);
            item.setHash(calculatedHash);
            
            if (!calculatedHash.equalsIgnoreCase(item.getExpectedHash())) {
                item.setStatus(DownloadItem.DownloadStatus.HASH_MISMATCH);
                item.setErrorMessage("Hash mismatch. Esperado: " + item.getExpectedHash() + 
                                   ", Calculado: " + calculatedHash);
            }
            
        } catch (Exception e) {
            item.setErrorMessage("Error verificando hash: " + e.getMessage());
            // Continuar sin verificación de hash
            calculateFileHash();
        }
    }

    /**
     * Calcula el hash del archivo para registro
     */
    private void calculateFileHash() {
        try {
            String hash = HashVerifier.calculateSHA256(item.getDestinationPath());
            item.setHash(hash);
        } catch (Exception e) {
            System.err.println("Error calculando hash: " + e.getMessage());
        }
    }

    /**
     * Notifica cambios de progreso
     */
    private void notifyProgress() {
        if (progressListener != null) {
            // Usar Platform.runLater para actualizaciones de UI si es necesario
            try {
                javafx.application.Platform.runLater(() -> progressListener.onProgressUpdate(item));
            } catch (Exception e) {
                // Si no estamos en contexto JavaFX, llamar directamente
                progressListener.onProgressUpdate(item);
            }
        }
    }
    
    /**
     * Detecta y actualiza la extensión del archivo basado en el Content-Type
     */
    private void detectAndUpdateFileExtension(String contentType) {
        if (contentType == null || contentType.isEmpty()) {
            return;
        }
        
        String currentPath = item.getDestinationPath();
        String fileName = item.getFileName();
        
        // Si el archivo ya tiene una extensión apropiada, no cambiar
        if (fileName.contains(".") && !fileName.endsWith(".bin") && !fileName.endsWith(".tmp")) {
            return;
        }
        
        String extension = getExtensionFromContentType(contentType.toLowerCase());
        if (extension != null) {
            // Remover extensión actual si es genérica
            String baseName = fileName;
            if (baseName.endsWith(".bin") || baseName.endsWith(".tmp")) {
                baseName = baseName.substring(0, baseName.lastIndexOf('.'));
            }
            
            // Crear nuevo nombre y ruta
            String newFileName = baseName + extension;
            String newPath = currentPath.substring(0, currentPath.lastIndexOf('\\')) + "\\" + newFileName;
            
            // Actualizar el item
            item.setFileName(newFileName);
            item.setDestinationPath(newPath);
        }
    }
    
    /**
     * Obtiene la extensión apropiada basada en el Content-Type
     */
    private String getExtensionFromContentType(String contentType) {
        if (contentType.contains("image/jpeg") || contentType.contains("image/jpg")) {
            return ".jpg";
        } else if (contentType.contains("image/png")) {
            return ".png";
        } else if (contentType.contains("image/gif")) {
            return ".gif";
        } else if (contentType.contains("image/webp")) {
            return ".webp";
        } else if (contentType.contains("image/svg")) {
            return ".svg";
        } else if (contentType.contains("application/pdf")) {
            return ".pdf";
        } else if (contentType.contains("text/plain")) {
            return ".txt";
        } else if (contentType.contains("text/html")) {
            return ".html";
        } else if (contentType.contains("text/css")) {
            return ".css";
        } else if (contentType.contains("application/javascript") || contentType.contains("text/javascript")) {
            return ".js";
        } else if (contentType.contains("application/json")) {
            return ".json";
        } else if (contentType.contains("application/xml") || contentType.contains("text/xml")) {
            return ".xml";
        } else if (contentType.contains("video/mp4")) {
            return ".mp4";
        } else if (contentType.contains("video/webm")) {
            return ".webm";
        } else if (contentType.contains("video/quicktime")) {
            return ".mov";
        } else if (contentType.contains("audio/mpeg")) {
            return ".mp3";
        } else if (contentType.contains("audio/wav")) {
            return ".wav";
        } else if (contentType.contains("application/zip")) {
            return ".zip";
        } else if (contentType.contains("application/x-rar")) {
            return ".rar";
        } else if (contentType.contains("application/octet-stream")) {
            return ".bin";
        }
        
        return null; // No se pudo determinar
    }
}