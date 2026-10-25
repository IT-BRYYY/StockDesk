
module com.stockdesk.stockdesk {

    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    requires org.xerial.sqlitejdbc;
    requires org.slf4j;
    requires java.prefs;

    opens com.stockdesk.stockdesk to javafx.fxml, javafx.base;

    exports com.stockdesk.stockdesk;
}
