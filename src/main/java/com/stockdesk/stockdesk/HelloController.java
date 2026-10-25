
package com.stockdesk.stockdesk;

import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class HelloController {

    @FXML
    private TextField searchField;

    @FXML
    private TableView<?> productTable;

    @FXML
    public void initialize() {
        System.out.println("StockDesk initialized successfully!");
    }
}
