package hpa;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    // the controller object (we need this to clean up properly on exit)
    private HPAController hpaController;

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("sample.fxml"));
        Parent root = loader.load();
        hpaController = loader.getController();

        //Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setTitle("HaskellProofAssistant");
        primaryStage.setScene(new Scene(root, 300, 275));
        primaryStage.show();
    }

    @Override
    public void stop() {
        this.hpaController.cleanUp();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
/*
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        TableView<Contact> contactTable = new TableView<>();
        contactTable.setEditable(true);

        TableColumn<Contact, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        contactTable.getColumns().add(nameCol);


        TableColumn<Contact, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(cellData -> cellData.getValue().categoryProperty());

        categoryCol.setCellFactory(ComboBoxTableCell.forTableColumn("Friends", "Family", "Work Contacts"));

        contactTable.getColumns().add(categoryCol);

        contactTable.getItems().addAll(
            new Contact("Bill Gates", "Work Contacts"),
            new Contact("Barack Obama", "Friends"),
            new Contact("Tim Cook", "Work Contacts")
        );

        Scene scene = new Scene(new BorderPane(contactTable), 600, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static class Contact {
        private final StringProperty name = new SimpleStringProperty();
        private final StringProperty category = new SimpleStringProperty();

        public Contact(String name, String category) {
            setName(name);
            setCategory(category);
        }

        public final StringProperty nameProperty() {
            return this.name;
        }


        public final String getName() {
            return this.nameProperty().get();
        }


        public final void setName(final String name) {
            this.nameProperty().set(name);
        }


        public final StringProperty categoryProperty() {
            return this.category;
        }


        public final String getCategory() {
            return this.categoryProperty().get();
        }


        public final void setCategory(final String category) {
            this.categoryProperty().set(category);
        }

    }

    public static void main(String[] args) {
        launch(args);
    }
}
*/