package com.doceditor.ui.controller;

import com.doceditor.export.ExportService;
import com.doceditor.ui.viewmodel.DocumentViewModel;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    @FXML private Label statusLabel;
    @FXML private Label dirtyIndicator;
    private DocumentViewModel viewModel;
    private ExportService exportService;

    @Override
    public void initialize(URL url, ResourceBundle rb) {}

    public void setViewModel(DocumentViewModel vm, ExportService exportService) {
        this.viewModel = vm;
        this.exportService = exportService;
        statusLabel.textProperty().bind(vm.statusMessageProperty());
        dirtyIndicator.textProperty().bind(javafx.beans.binding.Bindings.when(vm.isDirtyProperty()).then("● unsaved").otherwise(""));
    }

    public void onNewDocument() {
        TextInputDialog dlg = new TextInputDialog("Untitled");
        dlg.setTitle("New Document");
        dlg.setHeaderText("Enter document title");
        dlg.setContentText("Title:");
        styleDialog(dlg.getDialogPane());
        dlg.showAndWait().ifPresent(title -> { if (!title.isBlank()) viewModel.newDocument(title.strip()); });
    }

    @FXML private void onSave() { viewModel.saveCurrentDocument(); }

    @FXML private void onDelete() {
        if (viewModel.getCurrentDocument() == null) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete \"" + viewModel.getCurrentDocument().getTitle() + "\"? This cannot be undone.", ButtonType.OK, ButtonType.CANCEL);
        styleDialog(alert.getDialogPane());
        alert.showAndWait().filter(b -> b == ButtonType.OK).ifPresent(b -> viewModel.deleteDocument(viewModel.getCurrentDocument().getId()));
    }

    @FXML private void onQuit() {
        if (viewModel.isDirty()) {
            Alert a = new Alert(Alert.AlertType.CONFIRMATION, "You have unsaved changes. Quit anyway?", ButtonType.YES, ButtonType.CANCEL);
            styleDialog(a.getDialogPane());
            if (a.showAndWait().filter(b -> b == ButtonType.YES).isEmpty()) return;
        }
        Platform.exit();
    }

    @FXML private void onExportMarkdown() {
        if (!validate()) return;
        Path dir = pickDir(); if (dir == null) return;
        exportService.exportMarkdown(viewModel.getCurrentDocument(), dir);
    }

    @FXML private void onExportDocx() {
        if (!validate()) return;
        Path dir = pickDir(); if (dir == null) return;
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("DOCX", "*.docx"));
        File tpl = fc.showOpenDialog(getStage());
        exportService.exportDocx(viewModel.getCurrentDocument(), dir, tpl != null ? tpl.toPath() : null, viewModel.numberSectionsProperty().get(), viewModel.tableOfContentsProperty().get());
    }

    @FXML private void onExportPdf() {
        if (!validate()) return;
        Path dir = pickDir(); if (dir == null) return;
        exportService.exportPdf(viewModel.getCurrentDocument(), dir, viewModel.numberSectionsProperty().get(), viewModel.tableOfContentsProperty().get());
    }

    @FXML private void onAbout() {
        Alert a = new Alert(Alert.AlertType.INFORMATION, "Document Editor v1.0\nJavaFX + Pandoc + RichTextFX");
        styleDialog(a.getDialogPane());
        a.showAndWait();
    }

    private Path pickDir() {
        DirectoryChooser dc = new DirectoryChooser();
        File dir = dc.showDialog(getStage());
        return dir != null ? dir.toPath() : null;
    }

    private Stage getStage() { return (Stage) statusLabel.getScene().getWindow(); }

    private void styleDialog(DialogPane pane) {
        pane.getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());
    }

    private boolean validate() {
        if (viewModel.getCurrentDocument() == null) {
            Alert a = new Alert(Alert.AlertType.WARNING, "Please select a document.");
            styleDialog(a.getDialogPane());
            a.showAndWait();
            return false;
        }
        return true;
    }
}
