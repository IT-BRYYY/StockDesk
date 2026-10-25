
module com.stockdesk.stockdesk {

    requires javafx.controls;
    requires javafx.fxml;

    opens com.stockdesk.stockdesk to javafx.fxml, javafx.base;

    exports com.stockdesk.stockdesk;
}
