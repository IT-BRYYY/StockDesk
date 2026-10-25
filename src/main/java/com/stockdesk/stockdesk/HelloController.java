
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

    @FXML private TableView<Product> productTable;
    @FXML private TableView<StockMovement> movementTable;
    @FXML private TextField searchField;

    private final ObservableList<Product> products =
            FXCollections.observableArrayList();

    private final ObservableList<StockMovement> movements =
            FXCollections.observableArrayList();

    private int nextProductId = 1;
    private int nextMovementId = 1;

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
    }

    private <T> void setupColumns(
            TableView<T> table, String... properties) {

        for (int i = 0; i < properties.length; i++) {

            TableColumn<T, ?> column = table.getColumns().get(i);

            column.setCellValueFactory(
                    new PropertyValueFactory<>(properties[i]));
        }
    }

    @FXML
    private void handleAddProduct() {

        Product product = showProductDialog(null);

        if (product != null) {
            products.add(product);
            nextProductId++;
        }
    }

    @FXML
    private void handleEditProduct() {

        Product selected =
                productTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Select a product first.");
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
            showAlert("Select a product first.");
            return;
        }

        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Delete " + selected.getName() + "?",
                ButtonType.OK, ButtonType.CANCEL);

        confirmation.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                products.remove(selected);
            }
        });
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
            } catch (IllegalArgumentException ex) {
                event.consume();
                showAlert(ex.getMessage());
            }
        });

        dialog.setResultConverter(button -> {
            if (button != saveType) {
                return null;
            }

            return new Product(
                    editing ? existing.getId() : nextProductId,
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
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "Enter a valid quantity and price.");
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
            showAlert("Add a product before recording movements.");
            return;
        }

        Dialog<StockMovement> dialog = new Dialog<>();

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
                setText(empty || product == null ? null
                        : product.getName());
            }
        });

        TextField quantityField = new TextField();
        quantityField.setPromptText("Enter quantity");

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
                showAlert("Please select a product.");
                return;
            }

            try {
                int quantity = Integer.parseInt(
                        quantityField.getText().trim());

                if (quantity <= 0) {
                    throw new NumberFormatException();
                }

                if ("OUT".equals(type)
                        && quantity > selected.getQuantity()) {
                    event.consume();
                    showAlert("Insufficient stock. Available: "
                            + selected.getQuantity());
                    return;
                }

                if ("IN".equals(type)
                        && (long) selected.getQuantity() + quantity
                        > Integer.MAX_VALUE) {
                    event.consume();
                    showAlert("Quantity exceeds the supported limit.");
                }

            } catch (NumberFormatException ex) {
                event.consume();
                showAlert("Enter a positive whole-number quantity.");
            }
        });

        dialog.setResultConverter(button -> {

            if (button != saveType) {
                return null;
            }

            Product selected = productBox.getValue();
            int quantity = Integer.parseInt(
                    quantityField.getText().trim());

            if ("IN".equals(type)) {
                selected.setQuantity(
                        selected.getQuantity() + quantity);
            } else {
                selected.setQuantity(
                        selected.getQuantity() - quantity);
            }

            productTable.refresh();

            return new StockMovement(
                    nextMovementId++,
                    selected.getId(),
                    selected.getName(),
                    type,
                    quantity);
        });

        StockMovement movement =
                dialog.showAndWait().orElse(null);

        if (movement != null) {
            movements.add(0, movement);
        }
    }

    private void showAlert(String message) {

        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("StockDesk");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
