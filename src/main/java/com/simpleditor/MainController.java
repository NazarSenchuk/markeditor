package com.simpleditor;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MainController implements Initializable {
    private static final Path DOCS_DIR = Path.of(System.getProperty("user.home"), ".simpleditor", "documents");
    private static final Path TEMPLATES_DIR = Path.of("templates");

    @FXML
    private ListView<String> documentListView;
    @FXML
    private TextArea editorTextArea;
    @FXML
    private WebView previewWebView;
    @FXML
    private TextField titleField;
    @FXML
    private CheckBox numberSectionsCheck;
    @FXML
    private CheckBox tocCheck;
    @FXML
    private ComboBox<String> templateComboBox;
    @FXML
    private Label statusLabel;
    @FXML
    private ToggleButton previewToggle;

    private File currentFile;
    private final BooleanProperty isDirty = new SimpleBooleanProperty(false);
    private ScheduledExecutorService debouncer = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> pendingPreview;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            Files.createDirectories(DOCS_DIR);
            Files.createDirectories(TEMPLATES_DIR);
        } catch (IOException e) {
            statusLabel.setText("Помилка створення папок: " + e.getMessage());
        }

        refreshDocumentList();

        documentListView.getSelectionModel().selectedItemProperty().addListener((obs, old, fileName) -> {
            if (fileName != null)
                openDocument(fileName);
        });

        editorTextArea.textProperty().addListener((obs, old, text) -> {
            isDirty.set(true);
            statusLabel.setText("● Не збережено");
            schedulePreviewRefresh(text);
        });

        refreshTemplates();
        statusLabel.setText("Готово");
    }

    private void refreshDocumentList() {
        ObservableList<String> items = FXCollections.observableArrayList();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(DOCS_DIR, "*.md")) {
            for (Path p : stream)
                items.add(p.getFileName().toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        documentListView.setItems(items);
    }

    private void openDocument(String fileName) {
        currentFile = DOCS_DIR.resolve(fileName).toFile();
        try {
            String content = Files.readString(currentFile.toPath(), StandardCharsets.UTF_8);
            editorTextArea.setText(content);
            titleField.setText(fileName.replace(".md", ""));
            isDirty.set(false);
            statusLabel.setText("Відкрито: " + fileName);
            refreshPreview(content);
        } catch (IOException e) {
            showAlert("Помилка", "Не вдалося відкрити файл: " + e.getMessage());
        }
    }

    private void saveCurrentDocument() {
        if (currentFile == null) {
            onNewDocument();
            return;
        }
        try {
            Files.writeString(currentFile.toPath(), editorTextArea.getText(), StandardCharsets.UTF_8);
            isDirty.set(false);
            statusLabel.setText("Збережено: " + currentFile.getName());
            refreshDocumentList();
        } catch (IOException e) {
            showAlert("Помилка", "Не вдалося зберегти: " + e.getMessage());
        }
    }

    @FXML
    private void onNewDocument() {
        TextInputDialog dialog = new TextInputDialog("Без назви");
        dialog.setTitle("Новий документ");
        dialog.setHeaderText("Введіть назву документа");
        dialog.setContentText("Назва:");
        dialog.showAndWait().ifPresent(title -> {
            if (title.isBlank())
                title = "untitled";
            String safeName = title.replaceAll("[^a-zA-Z0-9_\\-]", "_") + ".md";
            File newFile = DOCS_DIR.resolve(safeName).toFile();
            if (newFile.exists()) {
                showAlert("Помилка", "Документ з такою назвою вже існує.");
                return;
            }
            String initialContent = "# " + title + "\n\n";
            try {
                Files.writeString(newFile.toPath(), initialContent, StandardCharsets.UTF_8);
                currentFile = newFile;
                editorTextArea.setText(initialContent);
                titleField.setText(title);
                isDirty.set(false);
                refreshDocumentList();
                documentListView.getSelectionModel().select(safeName);
                statusLabel.setText("Створено: " + title);
            } catch (IOException e) {
                showAlert("Помилка", "Не вдалося створити файл: " + e.getMessage());
            }
        });
    }

    @FXML
    private void onSave() {
        saveCurrentDocument();
    }

    @FXML
    private void onDelete() {
        if (currentFile == null)
            return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Видалити \"" + currentFile.getName() + "\"?",
                ButtonType.YES, ButtonType.CANCEL);
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    Files.deleteIfExists(currentFile.toPath());
                    currentFile = null;
                    editorTextArea.clear();
                    titleField.clear();
                    isDirty.set(false);
                    refreshDocumentList();
                    statusLabel.setText("Документ видалено");
                } catch (IOException e) {
                    showAlert("Помилка", "Не вдалося видалити: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void onQuit() {
        if (isDirty.get()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Є незбережені зміни. Вийти без збереження?",
                    ButtonType.YES, ButtonType.CANCEL);
            if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.YES)
                return;
        }
        Platform.exit();
    }

    @FXML
    private void onExportMarkdown() {
        if (currentFile == null) {
            showAlert("Помилка", "Немає відкритого документа");
            return;
        }
        DirectoryChooser chooser = new DirectoryChooser();
        File dir = chooser.showDialog(editorTextArea.getScene().getWindow());
        if (dir == null)
            return;
        File outFile = dir.toPath().resolve(currentFile.getName()).toFile();
        try {
            Files.copy(currentFile.toPath(), outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            showAlert("Готово", "Експортовано в " + outFile.getAbsolutePath());
        } catch (IOException e) {
            showAlert("Помилка", "Не вдалося експортувати: " + e.getMessage());
        }
    }

    @FXML
    private void onExportDocx() {
        if (currentFile == null) {
            showAlert("Помилка", "Немає відкритого документа");
            return;
        }
        DirectoryChooser chooser = new DirectoryChooser();
        File dir = chooser.showDialog(editorTextArea.getScene().getWindow());
        if (dir == null)
            return;
        String baseName = currentFile.getName().replace(".md", "");
        File outFile = dir.toPath().resolve(baseName + ".docx").toFile();
        String templatePath = templateComboBox.getValue();
        File templateFile = templatePath != null ? TEMPLATES_DIR.resolve(templatePath).toFile() : null;
        try {
            PandocHelper.toDocx(currentFile, outFile, templateFile, numberSectionsCheck.isSelected(),
                    tocCheck.isSelected());
            showAlert("Готово", "DOCX створено: " + outFile.getAbsolutePath());
        } catch (Exception e) {
            showAlert("Помилка", "Помилка Pandoc: " + e.getMessage());
        }
    }

    @FXML
    private void onExportPdf() {
        if (currentFile == null) {
            showAlert("Помилка", "Немає відкритого документа");
            return;
        }
        DirectoryChooser chooser = new DirectoryChooser();
        File dir = chooser.showDialog(editorTextArea.getScene().getWindow());
        if (dir == null)
            return;
        String baseName = currentFile.getName().replace(".md", "");
        File outFile = dir.toPath().resolve(baseName + ".pdf").toFile();
        try {
            PandocHelper.toPdf(currentFile, outFile, numberSectionsCheck.isSelected(), tocCheck.isSelected());
            showAlert("Готово", "PDF створено: " + outFile.getAbsolutePath());
        } catch (Exception e) {
            showAlert("Помилка", "Помилка Pandoc: " + e.getMessage());
        }
    }

    @FXML
    private void onTogglePreview() {
        boolean visible = previewToggle.isSelected();
        previewWebView.setVisible(visible);
        previewWebView.setManaged(visible);
        if (visible)
            refreshPreview(editorTextArea.getText());
    }

    private void schedulePreviewRefresh(String text) {
        if (pendingPreview != null)
            pendingPreview.cancel(false);
        pendingPreview = debouncer.schedule(() -> Platform.runLater(() -> {
            if (previewToggle.isSelected())
                refreshPreview(text);
        }), 300, TimeUnit.MILLISECONDS);
    }

    private void refreshPreview(String markdown) {
        try {
            String html = PandocHelper.toHtml(markdown, numberSectionsCheck.isSelected(), tocCheck.isSelected());
            previewWebView.getEngine().loadContent(html);
        } catch (Exception e) {
            previewWebView.getEngine().loadContent(
                    "<html><body><pre>Помилка генерації прев'ю: " + e.getMessage() + "</pre></body></html>");
        }
    }

    private void refreshTemplates() {
        ObservableList<String> items = FXCollections.observableArrayList();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(TEMPLATES_DIR, "*.docx")) {
            for (Path p : stream)
                items.add(p.getFileName().toString());
        } catch (IOException ignored) {
        }
        templateComboBox.setItems(items);
        if (!items.isEmpty())
            templateComboBox.getSelectionModel().selectFirst();
    }

    @FXML
    private void onAddTemplate() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Виберіть DOCX шаблон");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Word Documents", "*.docx"));
        File file = fc.showOpenDialog(editorTextArea.getScene().getWindow());
        if (file != null) {
            try {
                Files.copy(file.toPath(), TEMPLATES_DIR.resolve(file.getName()), StandardCopyOption.REPLACE_EXISTING);
                refreshTemplates();
                statusLabel.setText("Шаблон додано: " + file.getName());
            } catch (IOException e) {
                showAlert("Помилка", "Не вдалося скопіювати шаблон: " + e.getMessage());
            }
        }
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}