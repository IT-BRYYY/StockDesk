
package com.stockdesk.stockdesk;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

public class HelloController {

    @FXML
    private TableView<Product> productTable;

    @FXML
    private TextField searchField;

    private final ObservableList<Product> products =
            FXCollections.observableArrayList();

    private FilteredList<Product> filteredProducts;

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

        filteredProducts = new FilteredList<>(products, p -> true);

        productTable.setItems(filteredProducts);

        searchField.textProperty().addListener(
                (observable, oldValue, newValue) -> {

                    String keyword = newValue == null
                            ? ""
                            : newValue.toLowerCase().trim();

                    filteredProducts.setPredicate(product -> {

                        if (keyword.isEmpty()) {
                            return true;
                        }

                        return product.getName()
                                .toLowerCase()
                                .contains(keyword)
                                || product.getCategory()
                                .toLowerCase()
                                .contains(keyword);
                    });
                });
    }

    @FXML
    private void handleAddProduct() {

        Product product = showProductDialog(null);

        if (product != null) {
            products.add(product);
            nextId++;
        }
    }

    @FXML
    private void handleEditProduct() {

        Product selected =
                productTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("No Product Selected",
                    "Please select a product to edit.");
            return;
        }

        Product updated = showProductDialog(selected);

        if (updated != null) {
            selected.setName(updated.getName());
            selected.setCategory(updated.getCategory());
            selected.setQuantity(updated.getQuantity());
            selected.setPrice(updated.getPrice());

            productTable.refresh();
        }
    }

    @FXML
    private void handleDeleteProduct() {

        Product selected =
                productTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("No Product Selected",
                    "Please select a product to delete.");
            return;
        }

        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION);

        confirmation.setTitle("Delete Product");
        confirmation.setHeaderText("Delete " + selected.getName() + "?");
        confirmation.setContentText(
                "This action cannot be undone.");

        confirmation.showAndWait().ifPresent(response -> {

            if (response == ButtonType.OK) {
                products.remove(selected);
            }
        });
    }

    private Product showProductDialog(Product existing) {

        boolean editing = existing != null;

        Dialog<Product> dialog = new Dialog<>();

        dialog.setTitle(editing ? "Edit Product" : "Add Product");
        dialog.setHeaderText(editing
                ? "Update Product Information"
                : "Enter Product Information");

        ButtonType saveButton = new ButtonType(
                "Save", ButtonBar.ButtonData.OK_DONE);

        dialog.getDialogPane().getButtonTypes().addAll(
                saveButton, ButtonType.CANCEL);

        TextField nameField = new TextField();
        TextField categoryField = new TextField();
        TextField quantityField = new TextField();
        TextField priceField = new TextField();

        if (editing) {
            nameField.setText(existing.getName());
            categoryField.setText(existing.getCategory());
            quantityField.setText(
                    String.valueOf(existing.getQuantity()));
            priceField.setText(
                    String.valueOf(existing.getPrice()));
        }

        GridPane grid = new GridPane();

        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        grid.add(new Label("Product Name:"), 0, 0);
        grid.add(nameField, 1, 0);

        grid.add(new Label("Category:"), 0, 1);
        grid.add(categoryField, 1, 1);

        grid.add(new Label("Quantity:"), 0, 2);
        grid.add(quantityField, 1, 2);

        grid.add(new Label("Price:"), 0, 3);
        grid.add(priceField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        Button save = (Button)
                dialog.getDialogPane().lookupButton(saveButton);

        save.addEventFilter(ActionEvent.ACTION, event -> {

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

            } catch (IllegalArgumentException e) {

                event.consume();

                showAlert("Invalid Input",
                        "Enter a valid product name, category, "
                                + "non-negative whole-number quantity, "
                                + "and non-negative price.");
            }
        });

        dialog.setResultConverter(button -> {

            if (button == saveButton) {

                return new Product(
                        editing ? existing.getId() : nextId,
                        nameField.getText().trim(),
                        categoryField.getText().trim(),
                        Integer.parseInt(
                                quantityField.getText().trim()),
                        Double.parseDouble(
                                priceField.getText().trim()));
            }

            return null;
        });

        return dialog.showAndWait().orElse(null);
    }

    private void showAlert(String title, String message) {

        Alert alert = new Alert(Alert.AlertType.WARNING);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }
}
