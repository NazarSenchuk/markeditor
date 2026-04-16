module com.markeditor {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.graphics;

    opens com.markeditor to javafx.fxml;
    exports com.markeditor;
}
