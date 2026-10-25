
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

import java.sql.SQLException;

public class HelloController {

    @FXML private TableView<Product> productTable;
    @FXML private TableView<StockMovement> movementTable;
    @FXML private TextField searchField;

    private final StockDeskRepository repository =
            new StockDeskRepository();

    private final ObservableList<Product> products =
            FXCollections.observableArrayList();

    private final ObservableList<StockMovement> movements =
            FXCollections.observableArrayList();

    @FXML
    public void initialize() {

        setupColumns(productTable,
                "id", "name", "category", "quantity", "price");

        setupColumns(movementTable,
                "id", "product", "type", "quantity", "date");

        FilteredList<Product> filtered =
                new FilteredList<>(products, p -> true);

        productTable.setItems(filtered);
        movementTable.setItems(movements);

        searchField.textProperty().addListener(
                (obs, oldValue, newValue) -> {

                    String keyword = newValue == null
                            ? ""
                            : newValue.trim().toLowerCase();

                    filtered.setPredicate(product ->
                            keyword.isEmpty()
                                    || product.getName().toLowerCase()
                                    .contains(keyword)
                                    || product.getCategory().toLowerCase()
                                    .contains(keyword));
                });

        reloadData();
    }

    private <T> void setupColumns(
            TableView<T> table, String... properties) {

        for (int i = 0; i < properties.length; i++) {
            TableColumn<T, ?> column =
                    table.getColumns().get(i);

            column.setCellValueFactory(
                    new PropertyValueFactory<>(properties[i]));
        }
    }

    private void reloadData() {
        try {
            products.setAll(repository.findAllProducts());
            movements.setAll(repository.findAllMovements());
        } catch (SQLException e) {
            showAlert("Database Error", e.getMessage());
        }
    }

    @FXML
    private void handleAddProduct() {

        Product product = showProductDialog(null);

        if (product == null) return;

        try {
            repository.addProduct(
                    product.getName(),
                    product.getCategory(),
                    product.getQuantity(),
                    product.getPrice());

            reloadData();

        } catch (SQLException e) {
            showAlert("Add Product Failed", e.getMessage());
        }
    }

    @FXML
    private void handleEditProduct() {

        Product selected =
                productTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Selection Required",
                    "Select a product to edit.");
            return;
        }

        Product updated = showProductDialog(selected);

        if (updated == null) return;

        try {
            repository.updateProduct(updated);
            reloadData();

        } catch (SQLException e) {
            showAlert("Edit Failed", e.getMessage());
        }
    }

    @FXML
    private void handleDeleteProduct() {

        Product selected =
                productTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Selection Required",
                    "Select a product to delete.");
            return;
        }

        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Delete " + selected.getName() + "?\n"
                        + "Existing movement history will be preserved.",
                ButtonType.OK, ButtonType.CANCEL);

        confirmation.setTitle("Delete Product");

        if (confirmation.showAndWait().orElse(
                ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            repository.deleteProduct(selected.getId());
            reloadData();

        } catch (SQLException e) {
            showAlert("Delete Failed", e.getMessage());
        }
    }

    private Product showProductDialog(Product existing) {

        boolean editing = existing != null;

        Dialog<Product> dialog = new Dialog<>();

        dialog.setTitle(editing ? "Edit Product" : "Add Product");
        dialog.setHeaderText("Product Information");

        ButtonType saveType = new ButtonType(
                "Save", ButtonBar.ButtonData.OK_DONE);

        dialog.getDialogPane().getButtonTypes().addAll(
                saveType, ButtonType.CANCEL);

        TextField name = new TextField();
        TextField category = new TextField();
        TextField quantity = new TextField();
        TextField price = new TextField();

        if (editing) {
            name.setText(existing.getName());
            category.setText(existing.getCategory());
            quantity.setText(
                    String.valueOf(existing.getQuantity()));
            price.setText(
                    String.valueOf(existing.getPrice()));

            // Quantity must only change through stock movements.
            quantity.setDisable(true);
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        grid.addRow(0, new Label("Name:"), name);
        grid.addRow(1, new Label("Category:"), category);
        grid.addRow(2, new Label("Quantity:"), quantity);
        grid.addRow(3, new Label("Price:"), price);

        dialog.getDialogPane().setContent(grid);

        Button save = (Button)
                dialog.getDialogPane().lookupButton(saveType);

        save.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                validateProduct(name, category, quantity, price);
            } catch (IllegalArgumentException e) {
                event.consume();
                showAlert("Invalid Product", e.getMessage());
            }
        });

        dialog.setResultConverter(button -> {

            if (button != saveType) return null;

            return new Product(
                    editing ? existing.getId() : 0,
                    name.getText().trim(),
                    category.getText().trim(),
                    Integer.parseInt(quantity.getText().trim()),
                    Double.parseDouble(price.getText().trim()));
        });

        return dialog.showAndWait().orElse(null);
    }

    private void validateProduct(
            TextField name, TextField category,
            TextField quantity, TextField price) {

        if (name.getText().isBlank()
                || category.getText().isBlank()) {
            throw new IllegalArgumentException(
                    "Name and category are required.");
        }

        try {
            int qty = Integer.parseInt(quantity.getText().trim());
            double amount = Double.parseDouble(price.getText().trim());

            if (qty < 0 || !Double.isFinite(amount) || amount < 0) {
                throw new NumberFormatException();
            }

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Quantity must be a non-negative whole number, "
                            + "and price must be a valid non-negative number.");
        }
    }

    @FXML
    private void handleStockIn() {
        handleMovement("IN");
    }

    @FXML
    private void handleStockOut() {
        handleMovement("OUT");
    }

    private void handleMovement(String type) {

        if (products.isEmpty()) {
            showAlert("No Products",
                    "Add a product before recording stock movements.");
            return;
        }

        Dialog<MovementInput> dialog = new Dialog<>();
        dialog.setTitle("Stock " + type);
        dialog.setHeaderText("Record Stock " + type);

        ButtonType saveType = new ButtonType(
                "Save", ButtonBar.ButtonData.OK_DONE);

        dialog.getDialogPane().getButtonTypes().addAll(
                saveType, ButtonType.CANCEL);

        ComboBox<Product> productBox = new ComboBox<>(products);
        productBox.setPromptText("Select product");
        productBox.setMaxWidth(Double.MAX_VALUE);

        productBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Product product, boolean empty) {
                super.updateItem(product, empty);
                setText(empty || product == null ? null
                        : product.getName() + " ("
                          + product.getQuantity() + " available)");
            }
        });

        productBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Product product, boolean empty) {
                super.updateItem(product, empty);
                setText(empty || product == null
                        ? null : product.getName());
            }
        });

        TextField quantityField = new TextField();
        quantityField.setPromptText("Positive whole number");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        grid.addRow(0, new Label("Product:"), productBox);
        grid.addRow(1, new Label("Quantity:"), quantityField);

        dialog.getDialogPane().setContent(grid);

        Button save = (Button)
                dialog.getDialogPane().lookupButton(saveType);

        save.addEventFilter(ActionEvent.ACTION, event -> {

            Product selected = productBox.getValue();

            if (selected == null) {
                event.consume();
                showAlert("Invalid Selection", "Select a product.");
                return;
            }

            try {
                int amount = Integer.parseInt(
                        quantityField.getText().trim());

                if (amount <= 0) {
                    throw new NumberFormatException();
                }

                if (type.equals("OUT")
                        && amount > selected.getQuantity()) {
                    event.consume();
                    showAlert("Insufficient Stock",
                            "Available: " + selected.getQuantity());
                    return;
                }

                if (type.equals("IN")
                        && (long) selected.getQuantity() + amount
                        > Integer.MAX_VALUE) {
                    event.consume();
                    showAlert("Stock Limit",
                            "The quantity exceeds the supported limit.");
                }

            } catch (NumberFormatException e) {
                event.consume();
                showAlert("Invalid Quantity",
                        "Enter a positive whole number.");
            }
        });

        dialog.setResultConverter(button -> {
            if (button != saveType) return null;

            return new MovementInput(
                    productBox.getValue().getId(),
                    Integer.parseInt(quantityField.getText().trim()));
        });

        MovementInput input = dialog.showAndWait().orElse(null);

        if (input == null) return;

        try {
            repository.recordMovement(
                    input.productId(), type, input.quantity());

            reloadData();

        } catch (SQLException e) {
            showAlert("Stock Movement Failed", e.getMessage());
        }
    }

    private record MovementInput(int productId, int quantity) {
    }

    private void showAlert(String title, String message) {

        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
