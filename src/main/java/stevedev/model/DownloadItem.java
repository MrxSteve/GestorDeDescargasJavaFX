package stevedev.model;

import java.time.LocalDateTime;

/**
 * Representa un elemento de descarga con toda su información
 */
public class DownloadItem {
    private String url;
    private String fileName;
    private String destinationPath;
    private DownloadStatus status;
    private long totalSize;
    private long downloadedSize;
    private double progress;
    private String hash;
    private String expectedHash;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String errorMessage;
    private boolean cancelled;

    public enum DownloadStatus {
        PENDING,
        DOWNLOADING,
        PAUSED,
        COMPLETED,
        FAILED,
        CANCELLED,
        VERIFYING,
        HASH_MISMATCH
    }

    public DownloadItem(String url, String fileName, String destinationPath) {
        this.url = url;
        this.fileName = fileName;
        this.destinationPath = destinationPath;
        this.status = DownloadStatus.PENDING;
        this.totalSize = 0;
        this.downloadedSize = 0;
        this.progress = 0.0;
        this.cancelled = false;
    }

    // Getters y Setters
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public void setDestinationPath(String destinationPath) {
        this.destinationPath = destinationPath;
    }

    public DownloadStatus getStatus() {
        return status;
    }

    public void setStatus(DownloadStatus status) {
        this.status = status;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public long getDownloadedSize() {
        return downloadedSize;
    }

    public void setDownloadedSize(long downloadedSize) {
        this.downloadedSize = downloadedSize;
        if (totalSize > 0) {
            this.progress = (double) downloadedSize / totalSize * 100.0;
        }
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getExpectedHash() {
        return expectedHash;
    }

    public void setExpectedHash(String expectedHash) {
        this.expectedHash = expectedHash;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
        if (cancelled) {
            this.status = DownloadStatus.CANCELLED;
        }
    }

    public String getFormattedSize() {
        return formatBytes(downloadedSize) + " / " + formatBytes(totalSize);
    }

    public String getFormattedProgress() {
        return String.format("%.1f%%", progress);
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    @Override
    public String toString() {
        return "DownloadItem{" +
                "fileName='" + fileName + '\'' +
                ", status=" + status +
                ", progress=" + progress +
                '}';
    }
}