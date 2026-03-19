package com.doceditor.ui.controller;

import com.doceditor.export.PandocRunner;
import com.doceditor.ui.PreviewServer;
import com.doceditor.ui.viewmodel.DocumentViewModel;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class EditorController implements Initializable {
    private static final Logger log = LoggerFactory.getLogger(EditorController.class);

    @FXML private StackPane editorPane;
    @FXML private VBox previewPane;
    @FXML private WebView previewView;
    @FXML private SplitPane editorSplit;
    @FXML private TextField titleField;
    @FXML private ToggleButton previewToggle;
    @FXML private ToggleButton numberToggle;
    @FXML private ToggleButton tocToggle;

    private DocumentViewModel viewModel;
    private CodeArea codeArea;
    private PandocRunner pandoc;
    private PreviewServer previewServer;
    private ScheduledExecutorService debounceExecutor;
    private ScheduledFuture<?> pendingRefresh;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        codeArea = new CodeArea();
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.getStyleClass().add("code-area");
        codeArea.setStyle("-fx-font-family: 'JetBrains Mono',monospace; -fx-font-size: 14px;");
        editorPane.getChildren().add(codeArea);

        previewPane.setVisible(false);
        previewPane.setManaged(false);
        editorSplit.setDividerPositions(1.0);

        debounceExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "preview-debounce");
            t.setDaemon(true);
            return t;
        });

        codeArea.textProperty().addListener((obs, old, text) -> schedulePreviewRefresh(text));

        try {
            previewServer = new PreviewServer();
            previewServer.start();
        } catch (Exception e) {
            log.error("Failed to start preview server", e);
        }
    }

    public void setViewModel(DocumentViewModel vm) {
        this.viewModel = vm;
        titleField.textProperty().bindBidirectional(vm.documentTitleProperty());
        vm.currentDocumentProperty().addListener((obs, old, doc) -> {
            if (doc != null) {
                codeArea.replaceText(doc.getMarkdownContent());
                codeArea.moveTo(0);
            } else {
                codeArea.clear();
            }
        });
        codeArea.textProperty().addListener((obs, old, text) -> vm.markdownContentProperty().set(text));
        numberToggle.selectedProperty().bindBidirectional(vm.numberSectionsProperty());
        tocToggle.selectedProperty().bindBidirectional(vm.tableOfContentsProperty());
        vm.numberSectionsProperty().addListener((obs, old, val) -> { if (previewPane.isVisible()) refreshPreview(codeArea.getText()); });
        vm.tableOfContentsProperty().addListener((obs, old, val) -> { if (previewPane.isVisible()) refreshPreview(codeArea.getText()); });
    }

    public void setPandoc(PandocRunner pandoc) { this.pandoc = pandoc; }


    @FXML private void onTogglePreview() {
        boolean show = previewToggle.isSelected();
        previewPane.setVisible(show);
        previewPane.setManaged(show);
        editorSplit.setDividerPositions(show ? 0.5 : 1.0);
        if (show) refreshPreview(codeArea.getText());
    }


    private void schedulePreviewRefresh(String text) {
        if (pendingRefresh != null) pendingRefresh.cancel(false);
        pendingRefresh = debounceExecutor.schedule(() -> Platform.runLater(() -> {
            if (previewPane.isVisible()) refreshPreview(text);
        }), 300, TimeUnit.MILLISECONDS);
    }

    private void refreshPreview(String markdown) {
        if (pandoc == null || viewModel.getCurrentDocument() == null) return;
        try {
            String formatted = com.doceditor.export.MarkdownFormatter.formatMath(markdown);
            String html = pandoc.toHtml(formatted, true, viewModel.numberSectionsProperty().get(), viewModel.tableOfContentsProperty().get())
                    .replace("</head>", "<link rel=\"stylesheet\" href=\"/css/preview.css\"></head>");
            previewServer.setCurrentHtml(html);
            Platform.runLater(() -> previewView.getEngine().load("http://localhost:" + previewServer.getPort() + "/"));
        } catch (Exception e) {
            log.error("Failed to refresh preview", e);
        }
    }
}
