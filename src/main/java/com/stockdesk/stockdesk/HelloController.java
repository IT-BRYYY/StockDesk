
package com.stockdesk.stockdesk;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

import java.sql.SQLException;
import java.util.Locale;
import java.util.Objects;
import java.util.prefs.Preferences;

public class HelloController {

    private static final int LOW_STOCK_THRESHOLD = 5;

    private final StockDeskRepository repository =
            new StockDeskRepository();

    private final Preferences preferences =
            Preferences.userNodeForPackage(HelloController.class);

    private final ObservableList<Product> products =
            FXCollections.observableArrayList();

    private final ObservableList<StockMovement> movements =
            FXCollections.observableArrayList();

    @FXML private BorderPane appRoot;
    @FXML private TabPane mainTabs;
    @FXML private Tab productsTab;
    @FXML private Button themeButton;

    @FXML private TableView<Product> productTable;
    @FXML private TableView<StockMovement> movementTable;
    @FXML private TextField searchField;

    @FXML private Label totalProductsLabel;
    @FXML private Label totalStockLabel;
    @FXML private Label inventoryValueLabel;
    @FXML private Label lowStockLabel;
    @FXML private Label inventoryHealthLabel;

    private boolean darkMode;

    @FXML
    public void initialize() {

        setupColumns(productTable,
                "id", "name", "category", "quantity", "price");

        setupColumns(movementTable,
                "id", "product", "type", "quantity", "date");

        setupProductFormatting();
        setupMovementFormatting();

        FilteredList<Product> filtered =
                new FilteredList<>(products, p -> true);

        productTable.setItems(filtered);
        movementTable.setItems(movements);

        searchField.textProperty().addListener(
                (obs, oldValue, newValue) -> {

                    String keyword = newValue == null
                            ? ""
                            : newValue.trim().toLowerCase(Locale.ROOT);

                    filtered.setPredicate(product ->
                            keyword.isEmpty()
                                    || product.getName()
                                    .toLowerCase(Locale.ROOT)
                                    .contains(keyword)
                                    || product.getCategory()
                                    .toLowerCase(Locale.ROOT)
                                    .contains(keyword));
                });

        darkMode = preferences.getBoolean("darkMode", false);
        applyTheme();

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

        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY);
    }

    @SuppressWarnings("unchecked")
    private void setupProductFormatting() {

        TableColumn<Product, Integer> quantityColumn =
                (TableColumn<Product, Integer>)
                        productTable.getColumns().get(3);

        quantityColumn.setCellFactory(column ->
                new TableCell<>() {

                    @Override
                    protected void updateItem(
                            Integer quantity, boolean empty) {

                        super.updateItem(quantity, empty);

                        getStyleClass().remove("low-stock-text");

                        if (empty || quantity == null) {
                            setText(null);
                            return;
                        }

                        setText(quantity.toString());

                        if (quantity <= LOW_STOCK_THRESHOLD) {
                            getStyleClass().add("low-stock-text");
                        }
                    }
                });

        TableColumn<Product, Double> priceColumn =
                (TableColumn<Product, Double>)
                        productTable.getColumns().get(4);

        priceColumn.setCellFactory(column ->
                new TableCell<>() {

                    @Override
                    protected void updateItem(
                            Double price, boolean empty) {

                        super.updateItem(price, empty);

                        setText(empty || price == null
                                ? null
                                : formatMoney(price));
                    }
                });
    }

    @SuppressWarnings("unchecked")
    private void setupMovementFormatting() {

        TableColumn<StockMovement, String> typeColumn =
                (TableColumn<StockMovement, String>)
                        movementTable.getColumns().get(2);

        typeColumn.setCellFactory(column ->
                new TableCell<>() {

                    @Override
                    protected void updateItem(
                            String type, boolean empty) {

                        super.updateItem(type, empty);

                        if (empty || type == null) {
                            setText(null);
                            setStyle("");
                            return;
                        }

                        setText(type.equals("IN")
                                ? "▲ IN" : "▼ OUT");

                        setStyle(type.equals("IN")
                                ? "-fx-text-fill: #16a34a; -fx-font-weight: bold;"
                                : "-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                    }
                });
    }

    private String formatMoney(double value) {
        return String.format(Locale.US, "₱%,.2f", value);
    }

    private void updateDashboard() {

        long totalUnits = 0;
        int lowStock = 0;
        double inventoryValue = 0;

        for (Product product : products) {

            totalUnits += product.getQuantity();

            inventoryValue +=
                    product.getQuantity() * product.getPrice();

            if (product.getQuantity() <= LOW_STOCK_THRESHOLD) {
                lowStock++;
            }
        }

        totalProductsLabel.setText(
                String.valueOf(products.size()));

        totalStockLabel.setText(
                String.format(Locale.US, "%,d", totalUnits));

        inventoryValueLabel.setText(
                formatMoney(inventoryValue));

        lowStockLabel.setText(
                String.valueOf(lowStock));

        if (products.isEmpty()) {

            inventoryHealthLabel.setText(
                    "No products yet. Add your first product to get started.");

        } else if (lowStock == 0) {

            inventoryHealthLabel.setText(
                    "All products have healthy stock levels.");

        } else {

            inventoryHealthLabel.setText(
                    lowStock + " product(s) have 5 units or fewer. "
                            + "Consider restocking soon.");
        }
    }

    // ==========================================
    // THEME MANAGEMENT
    // ==========================================

    @FXML
    private void handleToggleTheme() {

        darkMode = !darkMode;
        applyTheme();

        preferences.putBoolean("darkMode", darkMode);
    }

    private void applyTheme() {

        appRoot.getStyleClass().removeAll(
                "light-theme", "dark-theme");

        appRoot.getStyleClass().add(
                darkMode ? "dark-theme" : "light-theme");

        themeButton.setText(
                darkMode ? "Light Mode" : "Dark Mode");
    }

    // ==========================================
    // MODERN DIALOG STYLING
    // ==========================================

    private void styleDialog(Dialog<?> dialog) {

        DialogPane pane = dialog.getDialogPane();

        pane.getStylesheets().add(
                Objects.requireNonNull(
                        HelloApplication.class.getResource(
                                "stockdesk.css")
                ).toExternalForm()
        );

        pane.getStyleClass().removeAll(
                "light-theme", "dark-theme");

        pane.getStyleClass().add(
                darkMode ? "dark-theme" : "light-theme");

        dialog.initOwner(appRoot.getScene().getWindow());

        pane.setPrefWidth(460);
    }

    @FXML
    private void handleViewProducts() {
        mainTabs.getSelectionModel().select(productsTab);
    }

    // ==========================================
    // DATABASE
    // ==========================================

    private void reloadData() {

        try {

            products.setAll(repository.findAllProducts());
            movements.setAll(repository.findAllMovements());

            updateDashboard();

        } catch (SQLException e) {

            showAlert("Database Error", e.getMessage());
        }
    }

    // ==========================================
    // PRODUCT CRUD
    // ==========================================

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
        confirmation.setHeaderText("Confirm Product Deletion");

        styleDialog(confirmation);

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

    // ==========================================
    // ADD / EDIT PRODUCT DIALOG
    // ==========================================

    private Product showProductDialog(Product existing) {

        boolean editing = existing != null;

        Dialog<Product> dialog = new Dialog<>();

        styleDialog(dialog);

        dialog.setTitle(editing ? "Edit Product" : "Add Product");

        dialog.setHeaderText(editing
                ? "Update Product Information"
                : "Add New Product");

        ButtonType saveType = new ButtonType(
                editing ? "Update Product" : "Save Product",
                ButtonBar.ButtonData.OK_DONE);

        dialog.getDialogPane().getButtonTypes().addAll(
                saveType, ButtonType.CANCEL);

        TextField name = new TextField();
        TextField category = new TextField();
        TextField quantity = new TextField();
        TextField price = new TextField();

        name.setPromptText("Enter product name");
        category.setPromptText("Enter category");
        quantity.setPromptText("Enter quantity");
        price.setPromptText("Enter price in PHP");

        if (editing) {

            name.setText(existing.getName());
            category.setText(existing.getCategory());

            quantity.setText(
                    String.valueOf(existing.getQuantity()));

            price.setText(
                    String.valueOf(existing.getPrice()));

            quantity.setDisable(true);
        }

        GridPane grid = new GridPane();

        grid.setHgap(18);
        grid.setVgap(16);
        grid.setPadding(new Insets(18, 22, 18, 22));

        grid.addRow(0, new Label("Product Name"), name);
        grid.addRow(1, new Label("Category"), category);
        grid.addRow(2, new Label("Quantity"), quantity);
        grid.addRow(3, new Label("Price (₱)"), price);

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
            TextField name,
            TextField category,
            TextField quantity,
            TextField price) {

        if (name.getText().isBlank()
                || category.getText().isBlank()) {

            throw new IllegalArgumentException(
                    "Product name and category are required.");
        }

        try {

            int qty = Integer.parseInt(
                    quantity.getText().trim());

            double amount = Double.parseDouble(
                    price.getText().trim());

            if (qty < 0
                    || !Double.isFinite(amount)
                    || amount < 0) {

                throw new NumberFormatException();
            }

        } catch (NumberFormatException e) {

            throw new IllegalArgumentException(
                    "Enter a valid non-negative quantity and price.");
        }
    }

    // ==========================================
    // STOCK MOVEMENTS
    // ==========================================

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
                    "Add a product before recording movements.");
            return;
        }

        Dialog<MovementInput> dialog = new Dialog<>();

        styleDialog(dialog);

        dialog.setTitle("Stock " + type);

        dialog.setHeaderText(type.equals("IN")
                ? "Receive Stock"
                : "Release Stock");

        ButtonType saveType = new ButtonType(
                type.equals("IN") ? "Record Stock IN" : "Record Stock OUT",
                ButtonBar.ButtonData.OK_DONE);

        dialog.getDialogPane().getButtonTypes().addAll(
                saveType, ButtonType.CANCEL);

        ComboBox<Product> productBox = new ComboBox<>(products);

        productBox.setPromptText("Select product");
        productBox.setMaxWidth(Double.MAX_VALUE);

        productBox.setCellFactory(list -> new ListCell<>() {

            @Override
            protected void updateItem(
                    Product product, boolean empty) {

                super.updateItem(product, empty);

                setText(empty || product == null
                        ? null
                        : product.getName() + " ("
                          + product.getQuantity() + " available)");
            }
        });

        productBox.setButtonCell(new ListCell<>() {

            @Override
            protected void updateItem(
                    Product product, boolean empty) {

                super.updateItem(product, empty);

                setText(empty || product == null
                        ? null
                        : product.getName());
            }
        });

        TextField quantityField = new TextField();
        quantityField.setPromptText("Enter quantity");

        GridPane grid = new GridPane();

        grid.setHgap(18);
        grid.setVgap(16);
        grid.setPadding(new Insets(18, 22, 18, 22));

        grid.addRow(0, new Label("Product"), productBox);
        grid.addRow(1, new Label("Quantity"), quantityField);

        dialog.getDialogPane().setContent(grid);

        Button save = (Button)
                dialog.getDialogPane().lookupButton(saveType);

        save.addEventFilter(ActionEvent.ACTION, event -> {

            Product selected = productBox.getValue();

            if (selected == null) {

                event.consume();

                showAlert("Invalid Selection",
                        "Please select a product.");
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
                            "Available quantity: "
                                    + selected.getQuantity());
                    return;
                }

                if (type.equals("IN")
                        && (long) selected.getQuantity() + amount
                        > Integer.MAX_VALUE) {

                    event.consume();

                    showAlert("Stock Limit",
                            "Quantity exceeds the supported limit.");
                }

            } catch (NumberFormatException e) {

                event.consume();

                showAlert("Invalid Quantity",
                        "Enter a positive whole-number quantity.");
            }
        });

        dialog.setResultConverter(button -> {

            if (button != saveType) return null;

            return new MovementInput(
                    productBox.getValue().getId(),
                    Integer.parseInt(quantityField.getText().trim()));
        });

        MovementInput input =
                dialog.showAndWait().orElse(null);

        if (input == null) return;

        try {

            repository.recordMovement(
                    input.productId(),
                    type,
                    input.quantity());

            reloadData();

        } catch (SQLException e) {

            showAlert("Stock Movement Failed", e.getMessage());
        }
    }

    private record MovementInput(
            int productId,
            int quantity) {
    }

    // ==========================================
    // MODERN VALIDATION ALERTS
    // ==========================================

    private void showAlert(String title, String message) {

        Alert alert = new Alert(Alert.AlertType.WARNING);

        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);

        styleDialog(alert);

        alert.showAndWait();
    }
}
