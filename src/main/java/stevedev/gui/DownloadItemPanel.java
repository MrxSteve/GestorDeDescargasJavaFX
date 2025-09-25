package stevedev.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import stevedev.model.DownloadItem;

/**
 * Panel que representa un elemento de descarga individual en la GUI
 */
public class DownloadItemPanel extends VBox {
    private final DownloadItem downloadItem;
    private final Runnable onCancelCallback;
    
    // Componentes de la UI
    private Label fileNameLabel;
    private Label urlLabel;
    private Label statusLabel;
    private Label sizeLabel;
    private Label speedLabel;
    private ProgressBar progressBar;
    private Label progressLabel;
    private Button cancelButton;
    private Button retryButton;
    
    // Para calcular velocidad
    private long lastBytesDownloaded = 0;
    private long lastUpdateTime = System.currentTimeMillis();

    public DownloadItemPanel(DownloadItem downloadItem, Runnable onCancelCallback) {
        this.downloadItem = downloadItem;
        this.onCancelCallback = onCancelCallback;
        
        initializeComponents();
        layoutComponents();
        updateDisplay();
        
        // Configurar estilo del panel
        this.setSpacing(8);
        this.setPadding(new Insets(10));
        this.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-color: #fafafa;");
    }

    private void initializeComponents() {
        // Nombre del archivo
        fileNameLabel = new Label(downloadItem.getFileName());
        fileNameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        // URL (truncada si es muy larga)
        String displayUrl = downloadItem.getUrl();
        if (displayUrl.length() > 60) {
            displayUrl = displayUrl.substring(0, 57) + "...";
        }
        urlLabel = new Label(displayUrl);
        urlLabel.setFont(Font.font("System", 11));
        urlLabel.setTextFill(Color.GRAY);
        
        // Estado
        statusLabel = new Label();
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        
        // Tamaño
        sizeLabel = new Label("Preparando...");
        sizeLabel.setFont(Font.font("System", 11));
        
        // Velocidad
        speedLabel = new Label("");
        speedLabel.setFont(Font.font("System", 11));
        speedLabel.setTextFill(Color.BLUE);
        
        // Barra de progreso
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(200);
        progressBar.setPrefHeight(20);
        
        // Etiqueta de progreso
        progressLabel = new Label("0%");
        progressLabel.setFont(Font.font("System", FontWeight.BOLD, 11));
        
        // Botones
        cancelButton = new Button("Cancelar");
        cancelButton.setOnAction(e -> {
            if (onCancelCallback != null) {
                onCancelCallback.run();
            }
        });
        
        retryButton = new Button("Reintentar");
        retryButton.setOnAction(e -> {
            // TODO: Implementar lógica de reintento
        });
        retryButton.setVisible(false);
    }

    private void layoutComponents() {
        // Header con nombre y estado
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        headerBox.getChildren().addAll(fileNameLabel, spacer1, statusLabel);
        
        // Información de la URL
        HBox urlBox = new HBox();
        urlBox.getChildren().add(urlLabel);
        
        // Información de tamaño y velocidad
        HBox infoBox = new HBox(20);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);
        infoBox.getChildren().addAll(sizeLabel, spacer2, speedLabel);
        
        // Progreso
        HBox progressBox = new HBox(10);
        progressBox.setAlignment(Pos.CENTER_LEFT);
        progressBox.getChildren().addAll(progressBar, progressLabel);
        
        // Botones
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.getChildren().addAll(retryButton, cancelButton);
        
        // Agregar todo al panel principal
        this.getChildren().addAll(headerBox, urlBox, infoBox, progressBox, buttonBox);
    }

    /**
     * Actualiza la visualización con los datos actuales del DownloadItem
     */
    public void updateDisplay() {
        // Actualizar estado
        updateStatus();
        
        // Actualizar progreso
        updateProgress();
        
        // Actualizar información de tamaño
        updateSizeInfo();
        
        // Actualizar velocidad
        updateSpeed();
        
        // Actualizar botones
        updateButtons();
    }

    private void updateStatus() {
        DownloadItem.DownloadStatus status = downloadItem.getStatus();
        statusLabel.setText(getStatusText(status));
        statusLabel.setTextFill(getStatusColor(status));
    }

    private void updateProgress() {
        double progress = downloadItem.getProgress() / 100.0;
        progressBar.setProgress(progress);
        progressLabel.setText(downloadItem.getFormattedProgress());
        
        // Cambiar color de la barra según el estado
        DownloadItem.DownloadStatus status = downloadItem.getStatus();
        String barStyle = getProgressBarStyle(status);
        progressBar.setStyle(barStyle);
    }

    private void updateSizeInfo() {
        if (downloadItem.getTotalSize() > 0) {
            sizeLabel.setText(downloadItem.getFormattedSize());
        } else if (downloadItem.getDownloadedSize() > 0) {
            sizeLabel.setText(formatBytes(downloadItem.getDownloadedSize()) + " descargados");
        } else {
            sizeLabel.setText("Preparando...");
        }
    }

    private void updateSpeed() {
        if (downloadItem.getStatus() == DownloadItem.DownloadStatus.DOWNLOADING) {
            long currentBytes = downloadItem.getDownloadedSize();
            long currentTime = System.currentTimeMillis();
            
            if (lastUpdateTime > 0 && currentTime > lastUpdateTime) {
                long bytesPerSecond = ((currentBytes - lastBytesDownloaded) * 1000) / (currentTime - lastUpdateTime);
                speedLabel.setText(formatBytes(bytesPerSecond) + "/s");
            }
            
            lastBytesDownloaded = currentBytes;
            lastUpdateTime = currentTime;
        } else {
            speedLabel.setText("");
        }
    }

    private void updateButtons() {
        DownloadItem.DownloadStatus status = downloadItem.getStatus();
        
        switch (status) {
            case PENDING:
            case DOWNLOADING:
            case VERIFYING:
                cancelButton.setVisible(true);
                cancelButton.setText("Cancelar");
                retryButton.setVisible(false);
                break;
                
            case COMPLETED:
                cancelButton.setVisible(false);
                retryButton.setVisible(false);
                break;
                
            case FAILED:
            case HASH_MISMATCH:
                cancelButton.setVisible(true);
                cancelButton.setText("Eliminar");
                retryButton.setVisible(true);
                break;
                
            case CANCELLED:
                cancelButton.setVisible(true);
                cancelButton.setText("Eliminar");
                retryButton.setVisible(true);
                break;
                
            case PAUSED:
                cancelButton.setVisible(true);
                cancelButton.setText("Cancelar");
                retryButton.setVisible(false);
                // TODO: Agregar botón de reanudar
                break;
        }
    }

    private String getStatusText(DownloadItem.DownloadStatus status) {
        switch (status) {
            case PENDING: return "Pendiente";
            case DOWNLOADING: return "Descargando";
            case PAUSED: return "Pausado";
            case COMPLETED: return "Completado";
            case FAILED: return "Falló";
            case CANCELLED: return "Cancelado";
            case VERIFYING: return "Verificando";
            case HASH_MISMATCH: return "Error de integridad";
            default: return status.toString();
        }
    }

    private Color getStatusColor(DownloadItem.DownloadStatus status) {
        switch (status) {
            case PENDING: return Color.ORANGE;
            case DOWNLOADING: return Color.BLUE;
            case PAUSED: return Color.GOLDENROD;
            case COMPLETED: return Color.GREEN;
            case FAILED: return Color.RED;
            case CANCELLED: return Color.GRAY;
            case VERIFYING: return Color.PURPLE;
            case HASH_MISMATCH: return Color.DARKRED;
            default: return Color.BLACK;
        }
    }

    private String getProgressBarStyle(DownloadItem.DownloadStatus status) {
        switch (status) {
            case DOWNLOADING:
                return "-fx-accent: #2196F3;"; // Azul
            case COMPLETED:
                return "-fx-accent: #4CAF50;"; // Verde
            case FAILED:
            case HASH_MISMATCH:
                return "-fx-accent: #F44336;"; // Rojo
            case CANCELLED:
                return "-fx-accent: #9E9E9E;"; // Gris
            case VERIFYING:
                return "-fx-accent: #9C27B0;"; // Púrpura
            default:
                return "-fx-accent: #FF9800;"; // Naranja
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * Obtiene el DownloadItem asociado a este panel
     */
    public DownloadItem getDownloadItem() {
        return downloadItem;
    }

    /**
     * Muestra mensaje de error en un tooltip
     */
    public void showError(String errorMessage) {
        if (errorMessage != null && !errorMessage.isEmpty()) {
            Tooltip tooltip = new Tooltip(errorMessage);
            Tooltip.install(statusLabel, tooltip);
        }
    }
}