package stevedev.core;

import okhttp3.OkHttpClient;
import stevedev.model.DownloadItem;
import stevedev.model.DownloadLog;
import stevedev.util.FileUtils;
import stevedev.util.ProgressListener;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Gestor principal que coordina todas las descargas
 */
public class DownloadManager {
    private final ExecutorService executorService;
    private final OkHttpClient httpClient;
    private final Map<DownloadItem, DownloadTask> activeTasks;
    private final Map<DownloadItem, CompletableFuture<DownloadItem>> futures;
    private ProgressListener globalProgressListener;

    /**
     * Constructor
     * @param maxConcurrentDownloads Número máximo de descargas simultáneas
     */
    public DownloadManager(int maxConcurrentDownloads) {
        this.executorService = Executors.newFixedThreadPool(maxConcurrentDownloads);
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(30))
                .readTimeout(Duration.ofMinutes(5))
                .writeTimeout(Duration.ofMinutes(5))
                .build();
        
        this.activeTasks = new ConcurrentHashMap<>();
        this.futures = new ConcurrentHashMap<>();
        
        // Crear directorios necesarios
        FileUtils.createDirectories();
    }

    /**
     * Constructor con configuración por defecto (4 descargas simultáneas)
     */
    public DownloadManager() {
        this(4);
    }

    /**
     * Establece el listener global para todas las descargas
     */
    public void setGlobalProgressListener(ProgressListener listener) {
        this.globalProgressListener = listener;
    }

    /**
     * Inicia una nueva descarga
     * @param url URL del archivo a descargar
     * @param fileName Nombre del archivo (opcional, se extraerá de la URL si es null)
     * @param expectedHash Hash esperado para verificación (opcional)
     * @return DownloadItem que representa la descarga
     */
    public DownloadItem startDownload(String url, String fileName, String expectedHash) {
        if (fileName == null || fileName.isEmpty()) {
            fileName = FileUtils.extractFileNameFromUrl(url);
        }
        
        String destinationPath = Paths.get(FileUtils.getDownloadsDirectory(), fileName).toString();
        
        DownloadItem item = new DownloadItem(url, fileName, destinationPath);
        item.setExpectedHash(expectedHash);
        
        return startDownload(item);
    }

    /**
     * Inicia una descarga con un DownloadItem existente
     */
    public DownloadItem startDownload(DownloadItem item) {
        DownloadTask task = new DownloadTask(item, this::onProgressUpdate, httpClient);
        activeTasks.put(item, task);
        
        CompletableFuture<DownloadItem> future = task.executeAsync()
                .thenApply(completedItem -> {
                    // Remover de tareas activas
                    activeTasks.remove(completedItem);
                    futures.remove(completedItem);
                    
                    // Guardar log
                    saveDownloadLog(completedItem);
                    
                    return completedItem;
                })
                .exceptionally(throwable -> {
                    item.setStatus(DownloadItem.DownloadStatus.FAILED);
                    item.setErrorMessage(throwable.getMessage());
                    
                    activeTasks.remove(item);
                    futures.remove(item);
                    
                    saveDownloadLog(item);
                    
                    return item;
                });
        
        futures.put(item, future);
        return item;
    }

    /**
     * Cancela una descarga
     */
    public void cancelDownload(DownloadItem item) {
        DownloadTask task = activeTasks.get(item);
        if (task != null) {
            task.cancel();
        }
        
        CompletableFuture<DownloadItem> future = futures.get(item);
        if (future != null) {
            future.cancel(true);
        }
    }

    /**
     * Cancela todas las descargas activas
     */
    public void cancelAllDownloads() {
        activeTasks.values().forEach(DownloadTask::cancel);
        futures.values().forEach(future -> future.cancel(true));
    }

    /**
     * Pausa una descarga (implementación futura)
     */
    public void pauseDownload(DownloadItem item) {
        // Para implementar pausar/reanudar necesitaríamos soporte de HTTP Range requests
        // Por ahora, simplemente cancelamos
        cancelDownload(item);
        item.setStatus(DownloadItem.DownloadStatus.PAUSED);
    }

    /**
     * Verifica si una descarga está activa
     */
    public boolean isDownloadActive(DownloadItem item) {
        return activeTasks.containsKey(item);
    }

    /**
     * Obtiene el número de descargas activas
     */
    public int getActiveDownloadsCount() {
        return activeTasks.size();
    }

    /**
     * Espera a que todas las descargas activas terminen
     */
    public void waitForAllDownloads() {
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.values().toArray(new CompletableFuture[0])
        );
        
        try {
            allFutures.get(10, TimeUnit.MINUTES); // Timeout de 10 minutos
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            System.err.println("Error esperando descargas: " + e.getMessage());
        }
    }

    /**
     * Obtiene estadísticas de descargas
     */
    public DownloadStats getStats() {
        return new DownloadStats(
            activeTasks.size(),
            getCompletedDownloadsCount(),
            getFailedDownloadsCount(),
            getTotalDownloadedBytes()
        );
    }

    /**
     * Cierra el gestor y libera recursos
     */
    public void shutdown() {
        cancelAllDownloads();
        executorService.shutdown();
        
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
        
        // Cerrar cliente HTTP
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }

    /**
     * Callback para actualizaciones de progreso
     */
    private void onProgressUpdate(DownloadItem item) {
        if (globalProgressListener != null) {
            globalProgressListener.onProgressUpdate(item);
        }
    }

    /**
     * Guarda el log de una descarga
     */
    private void saveDownloadLog(DownloadItem item) {
        try {
            DownloadLog log = new DownloadLog(item);
            
            // Guardar en ambos formatos
            FileUtils.saveLogAsJson(log);
            FileUtils.saveLogAsCsv(log);
            
        } catch (Exception e) {
            System.err.println("Error guardando log: " + e.getMessage());
        }
    }

    // Métodos auxiliares para estadísticas
    private int getCompletedDownloadsCount() {
        // En una implementación real, mantendríamos contadores
        return 0;
    }

    private int getFailedDownloadsCount() {
        return 0;
    }

    private long getTotalDownloadedBytes() {
        return activeTasks.keySet().stream()
                .mapToLong(DownloadItem::getDownloadedSize)
                .sum();
    }

    /**
     * Clase para estadísticas de descargas
     */
    public static class DownloadStats {
        private final int activeDownloads;
        private final int completedDownloads;
        private final int failedDownloads;
        private final long totalBytesDownloaded;

        public DownloadStats(int activeDownloads, int completedDownloads, 
                           int failedDownloads, long totalBytesDownloaded) {
            this.activeDownloads = activeDownloads;
            this.completedDownloads = completedDownloads;
            this.failedDownloads = failedDownloads;
            this.totalBytesDownloaded = totalBytesDownloaded;
        }

        // Getters
        public int getActiveDownloads() { return activeDownloads; }
        public int getCompletedDownloads() { return completedDownloads; }
        public int getFailedDownloads() { return failedDownloads; }
        public long getTotalBytesDownloaded() { return totalBytesDownloaded; }

        @Override
        public String toString() {
            return String.format("DownloadStats{active=%d, completed=%d, failed=%d, totalBytes=%s}",
                    activeDownloads, completedDownloads, failedDownloads, 
                    FileUtils.formatBytes(totalBytesDownloaded));
        }
    }
}