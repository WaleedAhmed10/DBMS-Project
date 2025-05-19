module com.example.oopfrontproject {
    requires java.sql;
    requires javafx.controls;
    requires javafx.fxml;

    opens com.example.oopfrontproject to javafx.fxml;
    exports com.example.oopfrontproject;
}
