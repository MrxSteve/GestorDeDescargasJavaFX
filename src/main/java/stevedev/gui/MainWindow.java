package stevedev.gui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import stevedev.core.DownloadManager;
import stevedev.model.DownloadItem;
import stevedev.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ventana principal de la aplicación de gestión de descargas
 */
public class MainWindow {
    private Stage primaryStage;
    private DownloadManager downloadManager;
    
    // Componentes principales de la UI
    private TextField urlField;
    private TextField hashField;
    private TextField fileNameField;
    private Button addDownloadButton;
    private Button clearAllButton;
    private Button openDownloadsFolderButton;
    private ScrollPane scrollPane;
    private VBox downloadsContainer;
    private Label statusLabel;
    private ProgressBar globalProgressBar;
    
    // Control de paneles de descarga
    private Map<DownloadItem, DownloadItemPanel> downloadPanels;
    private List<DownloadItem> downloadItems;

    public MainWindow(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.downloadManager = new DownloadManager(3); // Máximo 3 descargas simultáneas
        this.downloadPanels = new HashMap<>();
        this.downloadItems = new ArrayList<>();
        
        // Configurar listener global
        downloadManager.setGlobalProgressListener(this::onDownloadProgress);
        
        initializeComponents();
        layoutComponents();
        setupEventHandlers();
        
        // Configurar ventana
        Scene scene = new Scene(createMainLayout(), 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Gestor de Descargas - SteveDesignTech");
        primaryStage.setMinWidth(700);
        primaryStage.setMinHeight(500);
        
        // Manejar cierre de la aplicación
        primaryStage.setOnCloseRequest(e -> {
            downloadManager.shutdown();
            Platform.exit();
        });
    }

    private void initializeComponents() {
        // Campo de URL
        urlField = new TextField();
        urlField.setPromptText("Ingrese la URL del archivo a descargar...");
        urlField.setPrefWidth(400);
        
        // Campo de hash opcional
        hashField = new TextField();
        hashField.setPromptText("Hash esperado (SHA-256, opcional)");
        hashField.setPrefWidth(300);
        
        // Campo de nombre de archivo opcional
        fileNameField = new TextField();
        fileNameField.setPromptText("Nombre del archivo (opcional)");
        fileNameField.setPrefWidth(200);
        
        // Botones
        addDownloadButton = new Button("Agregar Descarga");
        addDownloadButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
        addDownloadButton.setPrefWidth(150);
        
        clearAllButton = new Button("Limpiar Todo");
        clearAllButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        
        openDownloadsFolderButton = new Button("Abrir Carpeta");
        openDownloadsFolderButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        
        // Container para las descargas
        downloadsContainer = new VBox(10);
        downloadsContainer.setPadding(new Insets(10));
        
        // ScrollPane para las descargas
        scrollPane = new ScrollPane(downloadsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        // Barra de progreso global
        globalProgressBar = new ProgressBar(0);
        globalProgressBar.setPrefWidth(200);
        globalProgressBar.setVisible(false);
        
        // Etiqueta de estado
        statusLabel = new Label("Listo para descargar");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");
    }

    private VBox createMainLayout() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        
        // Header
        Label titleLabel = new Label("Gestor de Descargas");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #333333;");
        
        // Formulario de nueva descarga
        VBox formBox = createFormBox();
        
        // Separador
        Separator separator = new Separator();
        separator.setPrefWidth(200);
        
        // Área de descargas
        VBox downloadsArea = createDownloadsArea();
        
        // Barra de estado
        HBox statusBox = createStatusBox();
        
        root.getChildren().addAll(titleLabel, formBox, separator, downloadsArea, statusBox);
        
        // El área de descargas debe expandirse
        VBox.setVgrow(downloadsArea, Priority.ALWAYS);
        
        return root;
    }

    private VBox createFormBox() {
        VBox formBox = new VBox(10);
        formBox.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 15; -fx-border-radius: 5;");
        
        // Primera fila: URL
        HBox urlRow = new HBox(10);
        urlRow.setAlignment(Pos.CENTER_LEFT);
        Label urlLabel = new Label("URL:");
        urlLabel.setPrefWidth(80);
        urlRow.getChildren().addAll(urlLabel, urlField);
        HBox.setHgrow(urlField, Priority.ALWAYS);
        
        // Segunda fila: Nombre de archivo y Hash
        HBox detailsRow = new HBox(10);
        detailsRow.setAlignment(Pos.CENTER_LEFT);
        
        Label nameLabel = new Label("Archivo:");
        nameLabel.setPrefWidth(80);
        
        Label hashLabel = new Label("Hash:");
        hashLabel.setPrefWidth(50);
        
        detailsRow.getChildren().addAll(nameLabel, fileNameField, hashLabel, hashField);
        
        // Tercera fila: Botones
        HBox buttonRow = new HBox(10);
        buttonRow.setAlignment(Pos.CENTER_RIGHT);
        buttonRow.getChildren().addAll(openDownloadsFolderButton, clearAllButton, addDownloadButton);
        
        formBox.getChildren().addAll(urlRow, detailsRow, buttonRow);
        return formBox;
    }

    private VBox createDownloadsArea() {
        VBox downloadsArea = new VBox(5);
        
        Label downloadsLabel = new Label("Descargas Activas");
        downloadsLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333333;");
        
        downloadsArea.getChildren().addAll(downloadsLabel, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        return downloadsArea;
    }

    private HBox createStatusBox() {
        HBox statusBox = new HBox(15);
        statusBox.setAlignment(Pos.CENTER_LEFT);
        statusBox.setPadding(new Insets(10, 0, 0, 0));
        statusBox.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1 0 0 0;");
        
        statusBox.getChildren().addAll(statusLabel, globalProgressBar);
        
        return statusBox;
    }

    private void layoutComponents() {
        // Ya implementado en createMainLayout()
    }

    private void setupEventHandlers() {
        // Botón agregar descarga
        addDownloadButton.setOnAction(e -> addDownload());
        
        // Enter en el campo de URL
        urlField.setOnAction(e -> addDownload());
        
        // Botón limpiar todo
        clearAllButton.setOnAction(e -> clearCompletedDownloads());
        
        // Botón abrir carpeta de descargas
        openDownloadsFolderButton.setOnAction(e -> openDownloadsFolder());
    }

    private void addDownload() {
        String url = urlField.getText().trim();
        
        if (url.isEmpty()) {
            showAlert("Error", "Por favor ingrese una URL válida.");
            return;
        }
        
        if (!isValidUrl(url)) {
            showAlert("Error", "La URL ingresada no es válida.");
            return;
        }
        
        String fileName = fileNameField.getText().trim();
        String hash = hashField.getText().trim();
        
        // Si no hay nombre, se extraerá de la URL
        if (fileName.isEmpty()) {
            fileName = null;
        }
        
        // Si no hay hash, será null
        if (hash.isEmpty()) {
            hash = null;
        }
        
        try {
            DownloadItem item = downloadManager.startDownload(url, fileName, hash);
            downloadItems.add(item);
            
            // Crear panel para la descarga
            DownloadItemPanel panel = new DownloadItemPanel(item, () -> cancelDownload(item));
            downloadPanels.put(item, panel);
            
            // Agregar a la UI
            Platform.runLater(() -> {
                downloadsContainer.getChildren().add(panel);
                clearForm();
                updateGlobalStatus();
            });
            
        } catch (Exception ex) {
            showAlert("Error", "Error iniciando descarga: " + ex.getMessage());
        }
    }

    private void cancelDownload(DownloadItem item) {
        downloadManager.cancelDownload(item);
        
        Platform.runLater(() -> {
            DownloadItemPanel panel = downloadPanels.get(item);
            if (panel != null) {
                // Si está completado o cancelado, remover de la UI
                if (item.getStatus() == DownloadItem.DownloadStatus.CANCELLED ||
                    item.getStatus() == DownloadItem.DownloadStatus.COMPLETED) {
                    downloadsContainer.getChildren().remove(panel);
                    downloadPanels.remove(item);
                    downloadItems.remove(item);
                }
            }
            updateGlobalStatus();
        });
    }

    private void clearCompletedDownloads() {
        List<DownloadItem> toRemove = new ArrayList<>();
        
        for (DownloadItem item : downloadItems) {
            if (item.getStatus() == DownloadItem.DownloadStatus.COMPLETED ||
                item.getStatus() == DownloadItem.DownloadStatus.CANCELLED ||
                item.getStatus() == DownloadItem.DownloadStatus.FAILED) {
                toRemove.add(item);
            }
        }
        
        Platform.runLater(() -> {
            for (DownloadItem item : toRemove) {
                DownloadItemPanel panel = downloadPanels.get(item);
                if (panel != null) {
                    downloadsContainer.getChildren().remove(panel);
                    downloadPanels.remove(item);
                }
                downloadItems.remove(item);
            }
            updateGlobalStatus();
        });
    }

    private void openDownloadsFolder() {
        try {
            File downloadsDir = new File(FileUtils.getDownloadsDirectory());
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs();
            }
            
            // Abrir carpeta en el explorador del sistema
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(downloadsDir);
            } else {
                showAlert("Información", "Carpeta de descargas: " + downloadsDir.getAbsolutePath());
            }
        } catch (Exception e) {
            showAlert("Error", "No se pudo abrir la carpeta de descargas: " + e.getMessage());
        }
    }

    private void onDownloadProgress(DownloadItem item) {
        Platform.runLater(() -> {
            DownloadItemPanel panel = downloadPanels.get(item);
            if (panel != null) {
                panel.updateDisplay();
                
                // Mostrar error si es necesario
                if (item.getErrorMessage() != null) {
                    panel.showError(item.getErrorMessage());
                }
            }
            updateGlobalStatus();
        });
    }

    private void updateGlobalStatus() {
        int activeDownloads = (int) downloadItems.stream()
                .mapToInt(item -> downloadManager.isDownloadActive(item) ? 1 : 0)
                .sum();
        
        int completedDownloads = (int) downloadItems.stream()
                .mapToInt(item -> item.getStatus() == DownloadItem.DownloadStatus.COMPLETED ? 1 : 0)
                .sum();
        
        if (activeDownloads > 0) {
            statusLabel.setText(String.format("Descargando %d archivo(s)... | %d completados", 
                    activeDownloads, completedDownloads));
            globalProgressBar.setVisible(true);
            
            // Calcular progreso global aproximado
            double totalProgress = downloadItems.stream()
                    .filter(item -> downloadManager.isDownloadActive(item))
                    .mapToDouble(item -> item.getProgress() / 100.0)
                    .average()
                    .orElse(0.0);
            
            globalProgressBar.setProgress(totalProgress);
        } else {
            statusLabel.setText(String.format("Listo | %d descargas completadas", completedDownloads));
            globalProgressBar.setVisible(false);
        }
    }

    private void clearForm() {
        urlField.clear();
        hashField.clear();
        fileNameField.clear();
        urlField.requestFocus();
    }

    private boolean isValidUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://") || url.startsWith("ftp://");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void show() {
        primaryStage.show();
    }
}