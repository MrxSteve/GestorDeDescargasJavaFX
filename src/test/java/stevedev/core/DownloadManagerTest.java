package stevedev.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import stevedev.model.DownloadItem;
import stevedev.util.ProgressListener;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para DownloadManager
 */
class DownloadManagerTest {
    
    private DownloadManager downloadManager;
    
    @BeforeEach
    void setUp() {
        downloadManager = new DownloadManager(2); // 2 descargas concurrentes para pruebas
    }
    
    @AfterEach
    void tearDown() {
        if (downloadManager != null) {
            downloadManager.shutdown();
        }
    }
    
    @Test
    void testStartDownload() {
        // URL de prueba (archivo pequeño)
        String testUrl = "https://httpbin.org/bytes/1024"; // 1KB de datos aleatorios
        
        DownloadItem item = downloadManager.startDownload(testUrl, "test_file.bin", null);
        
        assertNotNull(item);
        assertEquals(testUrl, item.getUrl());
        assertEquals("test_file.bin", item.getFileName());
        assertEquals(DownloadItem.DownloadStatus.PENDING, item.getStatus());
    }
    
    @Test
    void testDownloadProgress() throws InterruptedException {
        String testUrl = "https://httpbin.org/bytes/10240"; // 10KB
        CountDownLatch latch = new CountDownLatch(1);
        
        // Listener para capturar actualizaciones de progreso
        ProgressListener progressListener = item -> {
            if (item.getStatus() == DownloadItem.DownloadStatus.COMPLETED ||
                item.getStatus() == DownloadItem.DownloadStatus.FAILED) {
                latch.countDown();
            }
        };
        
        downloadManager.setGlobalProgressListener(progressListener);
        DownloadItem item = downloadManager.startDownload(testUrl, "progress_test.bin", null);
        
        // Esperar a que la descarga termine (máximo 30 segundos)
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        assertTrue(completed, "La descarga debería completarse en 30 segundos");
        
        // Verificar estado final
        assertTrue(item.getStatus() == DownloadItem.DownloadStatus.COMPLETED ||
                   item.getStatus() == DownloadItem.DownloadStatus.FAILED);
    }
    
    @Test
    void testCancelDownload() throws InterruptedException {
        String testUrl = "https://httpbin.org/bytes/1048576"; // 1MB para dar tiempo a cancelar
        
        DownloadItem item = downloadManager.startDownload(testUrl, "cancel_test.bin", null);
        
        // Esperar un poco para que inicie la descarga
        Thread.sleep(100);
        
        // Cancelar la descarga
        downloadManager.cancelDownload(item);
        
        // Esperar un poco más para que procese la cancelación
        Thread.sleep(500);
        
        assertEquals(DownloadItem.DownloadStatus.CANCELLED, item.getStatus());
        assertTrue(item.isCancelled());
    }
    
    @Test
    void testMultipleDownloads() {
        String[] urls = {
            "https://httpbin.org/bytes/1024",
            "https://httpbin.org/bytes/2048",
            "https://httpbin.org/bytes/4096"
        };
        
        for (int i = 0; i < urls.length; i++) {
            DownloadItem item = downloadManager.startDownload(urls[i], "multi_test_" + i + ".bin", null);
            assertNotNull(item);
        }
        
        // Verificar que el gestor está manejando múltiples descargas
        assertTrue(downloadManager.getActiveDownloadsCount() <= 2, 
                  "No debe exceder el límite de descargas concurrentes");
    }
    
    @Test
    void testDownloadStats() {
        DownloadManager.DownloadStats stats = downloadManager.getStats();
        
        assertNotNull(stats);
        assertTrue(stats.getActiveDownloads() >= 0);
        assertTrue(stats.getCompletedDownloads() >= 0);
        assertTrue(stats.getFailedDownloads() >= 0);
        assertTrue(stats.getTotalBytesDownloaded() >= 0);
    }
    
    @Test
    void testInvalidUrl() {
        assertThrows(Exception.class, () -> {
            downloadManager.startDownload("invalid-url", "test.bin", null);
        });
    }
    
    @Test
    void testShutdown() {
        // Iniciar una descarga
        downloadManager.startDownload("https://httpbin.org/bytes/1024", "shutdown_test.bin", null);
        
        // Hacer shutdown
        assertDoesNotThrow(() -> {
            downloadManager.shutdown();
        });
        
        // Verificar que no hay descargas activas
        assertEquals(0, downloadManager.getActiveDownloadsCount());
    }
}