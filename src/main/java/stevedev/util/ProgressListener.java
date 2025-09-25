package stevedev.util;

import stevedev.model.DownloadItem;

/**
 * Interface para escuchar cambios en el progreso de descarga
 */
@FunctionalInterface
public interface ProgressListener {
    /**
     * Se llama cuando hay una actualización del progreso
     * @param item El elemento de descarga actualizado
     */
    void onProgressUpdate(DownloadItem item);
}