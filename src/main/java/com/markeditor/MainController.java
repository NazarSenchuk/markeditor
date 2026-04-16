package com.markeditor;

import com.markeditor.model.Document;
import com.markeditor.util.DocumentStatistics;
import com.markeditor.util.SearchHelper;
import com.markeditor.util.TextFormattingHelper;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MainController implements Initializable {
    private static final Path DOCS_DIR = Path.of(System.getProperty("user.home"), ".markeditor", "documents");
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

    private Document currentDocument;
    private final BooleanProperty isDirty = new SimpleBooleanProperty(false);
    private final ScheduledExecutorService debouncer = Executors.newSingleThreadScheduledExecutor();
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
        File file = DOCS_DIR.resolve(fileName).toFile();
        Document doc = new Document(file);
        try {
            doc.load();
            currentDocument = doc;
            editorTextArea.setText(doc.getContent());
            titleField.setText(doc.getTitle());
            isDirty.set(false);
            statusLabel.setText("Відкрито: " + doc.getFileName());
            refreshPreview(doc.getContent());
        } catch (IOException e) {
            showAlert("Помилка", "Не вдалося відкрити файл: " + e.getMessage());
        }
    }

    private void saveCurrentDocument() {
        if (currentDocument == null) {
            onNewDocument();
            return;
        }
        try {
            currentDocument.setContent(editorTextArea.getText());
            currentDocument.save();
            isDirty.set(false);
            statusLabel.setText("Збережено: " + currentDocument.getFileName());
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
                Document doc = new Document(newFile);
                doc.setContent(initialContent);
                doc.save();
                currentDocument = doc;
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
        if (currentDocument == null)
            return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Видалити \"" + currentDocument.getFileName() + "\"?",
                ButtonType.YES, ButtonType.CANCEL);
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    currentDocument.delete();
                    currentDocument = null;
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
        if (currentDocument == null) {
            showAlert("Помилка", "Немає відкритого документа");
            return;
        }
        DirectoryChooser chooser = new DirectoryChooser();
        File dir = chooser.showDialog(editorTextArea.getScene().getWindow());
        if (dir == null)
            return;
        File outFile = dir.toPath().resolve(currentDocument.getFileName()).toFile();
        try {
            Files.copy(currentDocument.getFile().toPath(), outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            showAlert("Готово", "Експортовано в " + outFile.getAbsolutePath());
        } catch (IOException e) {
            showAlert("Помилка", "Не вдалося експортувати: " + e.getMessage());
        }
    }

    @FXML
    private void onExportDocx() {
        if (currentDocument == null) {
            showAlert("Помилка", "Немає відкритого документа");
            return;
        }
        DirectoryChooser chooser = new DirectoryChooser();
        File dir = chooser.showDialog(editorTextArea.getScene().getWindow());
        if (dir == null)
            return;
        String baseName = currentDocument.getTitle();
        File outFile = dir.toPath().resolve(baseName + ".docx").toFile();
        String templatePath = templateComboBox.getValue();
        File templateFile = templatePath != null ? TEMPLATES_DIR.resolve(templatePath).toFile() : null;
        try {
            PandocHelper.toDocx(currentDocument.getFile(), outFile, templateFile, numberSectionsCheck.isSelected(),
                    tocCheck.isSelected());
            showAlert("Готово", "DOCX створено: " + outFile.getAbsolutePath());
        } catch (Exception e) {
            showAlert("Помилка", "Помилка Pandoc: " + e.getMessage());
        }
    }

    @FXML
    private void onExportPdf() {
        if (currentDocument == null) {
            showAlert("Помилка", "Немає відкритого документа");
            return;
        }
        DirectoryChooser chooser = new DirectoryChooser();
        File dir = chooser.showDialog(editorTextArea.getScene().getWindow());
        if (dir == null)
            return;
        String baseName = currentDocument.getTitle();
        File outFile = dir.toPath().resolve(baseName + ".pdf").toFile();
        try {
            PandocHelper.toPdf(currentDocument.getFile(), outFile, numberSectionsCheck.isSelected(), tocCheck.isSelected());
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

    @FXML
    private void onFormatH1() { applyEdit(TextFormattingHelper.wrapSelection(editorTextArea.getText(),
            editorTextArea.getSelection().getStart(), editorTextArea.getSelection().getEnd(), "# ", "")); }
    @FXML
    private void onFormatH2() { applyEdit(TextFormattingHelper.wrapSelection(editorTextArea.getText(),
            editorTextArea.getSelection().getStart(), editorTextArea.getSelection().getEnd(), "## ", "")); }
    @FXML
    private void onFormatBold() { applyEdit(TextFormattingHelper.wrapSelection(editorTextArea.getText(),
            editorTextArea.getSelection().getStart(), editorTextArea.getSelection().getEnd(), "**", "**")); }
    @FXML
    private void onFormatItalic() { applyEdit(TextFormattingHelper.wrapSelection(editorTextArea.getText(),
            editorTextArea.getSelection().getStart(), editorTextArea.getSelection().getEnd(), "*", "*")); }
    @FXML
    private void onFormatBulletList() { applyLinePrefix("- "); }
    @FXML
    private void onFormatNumberedList() { applyLinePrefix("1. "); }
    @FXML
    private void onFormatCheckbox() { applyLinePrefix("- [ ] "); }
    @FXML
    private void onFormatQuote() { applyLinePrefix("> "); }
    @FXML
    private void onFormatCode() { applyEdit(TextFormattingHelper.wrapSelection(editorTextArea.getText(),
            editorTextArea.getSelection().getStart(), editorTextArea.getSelection().getEnd(), "`", "`")); }

    @FXML
    private void onShowStatistics() {
        DocumentStatistics.Stats stats = DocumentStatistics.compute(editorTextArea.getText());
        String message = String.format(
                "Символів: %d%nСимволів без пробілів: %d%nСлів: %d%nАбзаців: %d%nЗаголовків: %d%nОрієнтовно сторінок: %d",
                stats.chars(), stats.charsNoSpaces(), stats.words(), stats.paragraphs(), stats.headings(), stats.pages());
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Статистика документа");
        alert.setHeaderText("Параметри поточного документа");
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void onFind() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Пошук");
        dialog.setHeaderText("Пошук по документу");
        dialog.initOwner(editorTextArea.getScene().getWindow());
        dialog.initModality(Modality.NONE);

        ButtonType closeButtonType = new ButtonType("Закрити", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeButtonType);

        TextField searchField = new TextField();
        searchField.setPromptText("Введіть фрагмент тексту");
        CheckBox matchCaseBox = new CheckBox("Враховувати регістр");
        Label resultLabel = new Label("Введіть текст для пошуку.");

        Button nextButton = new Button("Знайти далі");
        Button previousButton = new Button("Знайти попередній");
        Button highlightAllButton = new Button("Порахувати входження");

        nextButton.setOnAction(event -> performSearch(searchField.getText(), matchCaseBox.isSelected(), true, resultLabel));
        previousButton.setOnAction(event -> performSearch(searchField.getText(), matchCaseBox.isSelected(), false, resultLabel));
        highlightAllButton.setOnAction(event -> highlightMatches(searchField.getText(), matchCaseBox.isSelected(), resultLabel));

        HBox buttonBox = new HBox(8, nextButton, previousButton, highlightAllButton);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        grid.addRow(0, new Label("Текст:"), searchField);
        grid.add(matchCaseBox, 1, 1);
        grid.add(buttonBox, 1, 2);
        grid.add(resultLabel, 1, 3);

        dialog.getDialogPane().setContent(grid);
        Node closeButton = dialog.getDialogPane().lookupButton(closeButtonType);
        if (closeButton != null) {
            closeButton.setManaged(true);
        }

        Platform.runLater(searchField::requestFocus);
        dialog.show();
    }

    private void performSearch(String query, boolean matchCase, boolean forward, Label resultLabel) {
        String text = editorTextArea.getText();
        int caretPosition = forward ? editorTextArea.getSelection().getEnd() : editorTextArea.getSelection().getStart();
        Optional<SearchHelper.SearchMatch> match = forward
                ? SearchHelper.findNext(text, query, caretPosition, matchCase)
                : SearchHelper.findPrevious(text, query, caretPosition, matchCase);

        if (match.isEmpty()) {
            resultLabel.setText("Нічого не знайдено.");
            statusLabel.setText("Пошук: збігів немає");
            return;
        }

        SearchHelper.SearchMatch found = match.get();
        editorTextArea.requestFocus();
        editorTextArea.positionCaret(found.end());
        editorTextArea.selectRange(found.start(), found.end());
        List<SearchHelper.SearchMatch> allMatches = SearchHelper.findAll(text, query, matchCase);
        resultLabel.setText(String.format("Знайдено %d входжень. Поточне: %d-%d",
                allMatches.size(), found.start(), found.end()));
        statusLabel.setText("Пошук: знайдено " + allMatches.size() + " входжень");
    }

    private void highlightMatches(String query, boolean matchCase, Label resultLabel) {
        List<SearchHelper.SearchMatch> matches = SearchHelper.findAll(editorTextArea.getText(), query, matchCase);
        if (matches.isEmpty()) {
            resultLabel.setText("Нічого не знайдено.");
            statusLabel.setText("Пошук: збігів немає");
            return;
        }

        SearchHelper.SearchMatch firstMatch = matches.get(0);
        editorTextArea.requestFocus();
        editorTextArea.positionCaret(firstMatch.end());
        editorTextArea.selectRange(firstMatch.start(), firstMatch.end());
        resultLabel.setText("Знайдено входжень: " + matches.size() + ". Підсвічено перший збіг.");
        statusLabel.setText("Пошук: знайдено " + matches.size() + " входжень");
    }

    private void applyLinePrefix(String marker) {
        applyEdit(TextFormattingHelper.insertAtLineStart(editorTextArea.getText(),
                editorTextArea.getSelection().getStart(), editorTextArea.getSelection().getEnd(), marker));
    }

    private void applyEdit(TextFormattingHelper.TextEdit edit) {
        editorTextArea.setText(edit.text());
        editorTextArea.requestFocus();
        editorTextArea.selectRange(edit.selectionStart(), edit.selectionEnd());
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
