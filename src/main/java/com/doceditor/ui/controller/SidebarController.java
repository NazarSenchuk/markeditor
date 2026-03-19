package com.doceditor.ui.controller;

import com.doceditor.document.Document;
import com.doceditor.ui.viewmodel.DocumentViewModel;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.net.URL;
import java.util.ResourceBundle;

public class SidebarController implements Initializable {
    @FXML private ListView<Document> documentList;
    @FXML private TextField searchField;
    private DocumentViewModel viewModel;
    private MainController mainController;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        documentList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Document doc, boolean empty) {
                super.updateItem(doc, empty);
                if (empty || doc == null) { setGraphic(null); setText(null); }
                else {
                    VBox box = new VBox(2);
                    Label title = new Label(doc.getTitle());
                    Label summary = new Label(doc.getSummary());
                    title.setStyle("-fx-font-weight: bold; -fx-text-fill: #cdd6f4;");
                    summary.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c7086;");
                    summary.setMaxWidth(180);
                    box.getChildren().addAll(title, summary);
                    setGraphic(box);
                }
            }
        });
    }

    public void setViewModel(DocumentViewModel vm) {
        this.viewModel = vm;
        documentList.setItems(vm.documentsProperty());
        documentList.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> { if (sel != null) vm.openDocument(sel); });
        vm.currentDocumentProperty().addListener((obs, old, doc) -> {
            if (doc != null && !doc.equals(documentList.getSelectionModel().getSelectedItem())) documentList.getSelectionModel().select(doc);
        });
        vm.loadAllDocuments();
    }

    public void setMainController(MainController mc) { this.mainController = mc; }

    @FXML private void onNewDocument() { if (mainController != null) mainController.onNewDocument(); }

    @FXML private void onFilter() {
        String q = searchField.getText().toLowerCase().trim();
        if (q.isEmpty()) documentList.setItems(viewModel.documentsProperty());
        else documentList.setItems(viewModel.documentsProperty().filtered(d -> d.getTitle().toLowerCase().contains(q) || d.getSummary().toLowerCase().contains(q)));
    }
}
