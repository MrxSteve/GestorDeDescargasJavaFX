package stevedev.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * Representa una entrada de log para el registro de descargas
 */
public class DownloadLog {
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    private String url;
    private String fileName;
    private long fileSize;
    private String hash;
    private String expectedHash;
    private long durationSeconds;
    private String result; // SUCCESS, FAILED, CANCELLED, HASH_MISMATCH
    private String errorMessage;

    public DownloadLog() {
        this.timestamp = LocalDateTime.now();
    }

    public DownloadLog(DownloadItem item) {
        this();
        this.url = item.getUrl();
        this.fileName = item.getFileName();
        this.fileSize = item.getTotalSize();
        this.hash = item.getHash();
        this.expectedHash = item.getExpectedHash();
        
        if (item.getStartTime() != null && item.getEndTime() != null) {
            this.durationSeconds = java.time.Duration.between(item.getStartTime(), item.getEndTime()).getSeconds();
        }
        
        switch (item.getStatus()) {
            case COMPLETED:
                this.result = "SUCCESS";
                break;
            case FAILED:
                this.result = "FAILED";
                break;
            case CANCELLED:
                this.result = "CANCELLED";
                break;
            case HASH_MISMATCH:
                this.result = "HASH_MISMATCH";
                break;
            default:
                this.result = "UNKNOWN";
        }
        
        this.errorMessage = item.getErrorMessage();
    }

    // Getters y Setters
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

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

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
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

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getFormattedDuration() {
        if (durationSeconds < 60) {
            return durationSeconds + "s";
        } else {
            long minutes = durationSeconds / 60;
            long seconds = durationSeconds % 60;
            return String.format("%dm %ds", minutes, seconds);
        }
    }

    public String getFormattedSize() {
        if (fileSize < 1024) return fileSize + " B";
        int exp = (int) (Math.log(fileSize) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", fileSize / Math.pow(1024, exp), pre);
    }

    @Override
    public String toString() {
        return "DownloadLog{" +
                "timestamp=" + timestamp +
                ", fileName='" + fileName + '\'' +
                ", result='" + result + '\'' +
                '}';
    }
}