
package com.stockdesk.stockdesk;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;

public class HelloController {

    @FXML
    private TableView<Product> productTable;

    private final ObservableList<Product> products =
            FXCollections.observableArrayList();

    private int nextId = 1;

    @FXML
    public void initialize() {

        TableColumn<Product, Integer> idColumn =
                (TableColumn<Product, Integer>)
                        productTable.getColumns().get(0);

        TableColumn<Product, String> nameColumn =
                (TableColumn<Product, String>)
                        productTable.getColumns().get(1);

        TableColumn<Product, String> categoryColumn =
                (TableColumn<Product, String>)
                        productTable.getColumns().get(2);

        TableColumn<Product, Integer> quantityColumn =
                (TableColumn<Product, Integer>)
                        productTable.getColumns().get(3);

        TableColumn<Product, Double> priceColumn =
                (TableColumn<Product, Double>)
                        productTable.getColumns().get(4);

        idColumn.setCellValueFactory(
                new PropertyValueFactory<>("id"));

        nameColumn.setCellValueFactory(
                new PropertyValueFactory<>("name"));

        categoryColumn.setCellValueFactory(
                new PropertyValueFactory<>("category"));

        quantityColumn.setCellValueFactory(
                new PropertyValueFactory<>("quantity"));

        priceColumn.setCellValueFactory(
                new PropertyValueFactory<>("price"));

        productTable.setItems(products);
    }

    @FXML
    private void handleAddProduct() {

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add Product");
        dialog.setHeaderText("Enter Product Information");

        ButtonType saveButton =
                new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);

        dialog.getDialogPane().getButtonTypes().addAll(
                saveButton, ButtonType.CANCEL);

        TextField nameField = new TextField();
        nameField.setPromptText("Product name");

        TextField categoryField = new TextField();
        categoryField.setPromptText("Category");

        TextField quantityField = new TextField();
        quantityField.setPromptText("Quantity");

        TextField priceField = new TextField();
        priceField.setPromptText("Price");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);

        grid.add(new Label("Category:"), 0, 1);
        grid.add(categoryField, 1, 1);

        grid.add(new Label("Quantity:"), 0, 2);
        grid.add(quantityField, 1, 2);

        grid.add(new Label("Price:"), 0, 3);
        grid.add(priceField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        Button save = (Button) dialog.getDialogPane()
                .lookupButton(saveButton);

        save.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                String name = nameField.getText().trim();
                String category = categoryField.getText().trim();

                int quantity = Integer.parseInt(
                        quantityField.getText().trim());

                double price = Double.parseDouble(
                        priceField.getText().trim());

                if (name.isEmpty() || category.isEmpty()
                        || quantity < 0 || !Double.isFinite(price)
                        || price < 0) {
                    throw new IllegalArgumentException();
                }

                products.add(new Product(
                        nextId++, name, category, quantity, price));

            } catch (IllegalArgumentException e) {
                event.consume();

                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Invalid Input");
                alert.setHeaderText("Unable to add product");
                alert.setContentText(
                        "Enter a product name and category, " +
                                "a non-negative whole-number quantity, " +
                                "and a valid non-negative price.");
                alert.showAndWait();
            }
        });

        dialog.showAndWait();
    }
}
