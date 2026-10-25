
package com.stockdesk.stockdesk;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.Objects;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        try {
            DatabaseManager.initialize();

        } catch (SQLException e) {

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("StockDesk Database Error");
            alert.setHeaderText("Unable to initialize database");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
            return;
        }

        FXMLLoader loader = new FXMLLoader(
                HelloApplication.class.getResource("hello-view.fxml")
        );

        Scene scene = new Scene(loader.load(), 1100, 720);

        scene.getStylesheets().add(
                Objects.requireNonNull(
                        HelloApplication.class.getResource(
                                "stockdesk.css")
                ).toExternalForm()
        );

        stage.setTitle("StockDesk | Inventory Management");
        stage.setScene(scene);

        stage.setMinWidth(900);
        stage.setMinHeight(600);

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
