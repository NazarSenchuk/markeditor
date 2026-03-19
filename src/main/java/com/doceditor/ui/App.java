package com.doceditor.ui;

import com.doceditor.app.DocumentService;
import com.doceditor.export.ExportService;
import com.doceditor.export.PandocRunner;
import com.doceditor.storage.LocalMarkdownStorage;
import com.doceditor.ui.controller.EditorController;
import com.doceditor.ui.controller.MainController;
import com.doceditor.ui.controller.SidebarController;
import com.doceditor.ui.viewmodel.DocumentViewModel;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        LocalMarkdownStorage storage = new LocalMarkdownStorage();
        DocumentService docService = new DocumentService(storage);
        DocumentViewModel viewModel = new DocumentViewModel(docService);
        PandocRunner pandoc = new PandocRunner();
        ExportService exportService = new ExportService(pandoc);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        Parent root = loader.load();
        MainController mainController = loader.getController();

        FXMLLoader sideLoader = new FXMLLoader(getClass().getResource("/fxml/sidebar.fxml"));
        Parent sidebar = sideLoader.load();
        SidebarController sidebarController = sideLoader.getController();

        FXMLLoader editLoader = new FXMLLoader(getClass().getResource("/fxml/editor.fxml"));
        Parent editor = editLoader.load();
        EditorController editorController = editLoader.getController();

        mainController.setViewModel(viewModel, exportService);
        sidebarController.setViewModel(viewModel);
        sidebarController.setMainController(mainController);
        editorController.setViewModel(viewModel);
        editorController.setPandoc(pandoc);

        ((javafx.scene.layout.BorderPane) root).setLeft(sidebar);
        ((javafx.scene.layout.BorderPane) root).setCenter(editor);

        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());

        stage.setTitle("Document Editor");
        try { stage.getIcons().add(new Image(getClass().getResourceAsStream("/icon.png"))); } catch (Exception ignored) {}
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) { launch(args); }
}
