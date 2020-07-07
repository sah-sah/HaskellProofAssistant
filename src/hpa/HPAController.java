package hpa;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

import netscape.javascript.JSObject;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;

/*
* TODO: we need a common way to show error messages
*
 */

public class HPAController {
    private final String assume = "Assume...";
    private final String instantiateSchema = "Instantiate Schema";

    // model variables (we will combine model and controller)
    HPAListenTask hpaListener;
    Process hpaProcess;
    BufferedWriter hpaWriter;

    private AxiomHtmlDoc axiomDoc;
    private InputHtmlDoc inputDoc;
    private ProofHtmlDoc proofDoc;

    // view variables
    @FXML
    private WebView displayProof;

    @FXML
    private WebView displayAxioms;

    @FXML
    private WebView displayInput;

    @FXML
    private ComboBox<String> actionCB;

    @FXML
    private Group actionGroup;

    @FXML
    private Button actionBtn;

    @FXML
    public void initialize() {
        // Setup interface
        // TODO: this list should be the possible proof steps/other actions
        ObservableList<String> proofActions = FXCollections.observableArrayList(assume, instantiateSchema);
        actionCB.setItems(proofActions);
        actionCB.setValue("Assume...");
        actionCB.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        updateActionControls();
                    }
                });
            }
        });
        updateActionControls();

        // setup model
        try {
            ProcessBuilder pb = new ProcessBuilder("C:\\Users\\Maryam\\IdeaProjects\\Stack Projects\\HML\\.stack-work\\install\\bbcfd75f\\bin\\HML-exe.exe");
            this.hpaProcess = pb.start();

            // reading PropProver output
            BufferedReader br=new BufferedReader(new InputStreamReader(this.hpaProcess.getInputStream()));
            this.hpaListener = new HPAListenTask(br, this);
            // start the listener
            Thread th = new Thread(this.hpaListener);
            th.setDaemon(true);
            th.start();

            // writing to PropProver
            this.hpaWriter =new BufferedWriter(new OutputStreamWriter(this.hpaProcess.getOutputStream()));
        } catch (Exception ex) {
            System.out.println("Error: unable to start PropProver");
            System.out.println(ex);
        }

        // display starting point of proof

//        sendCommand(HPACommand.printPredicate("unionAxiom"));
        //sendCommand("cp");

        // setup HTML file for axioms
        initializeContent();

        // get list of axioms
        sendCommand(HPACommand.listAxioms());
    }

    private void initializeContent() {
        // set up Axiom web view
        axiomDoc = new AxiomHtmlDoc(this.displayAxioms.getEngine());
        // set up input web view
        inputDoc = new InputHtmlDoc(this, this.displayInput.getEngine());
        // set up proof display
        proofDoc = new ProofHtmlDoc(this.displayProof.getEngine());

    }

    public void sendCommand(String cmd) {
        //System.out.println("Sending command... " + cmd);
        try {
            //System.out.println(cmd);
            this.hpaWriter.write(cmd);
            this.hpaWriter.newLine();
            this.hpaWriter.flush();
        } catch (Exception ex) {
            System.out.println("PropProver.sendCommand");
            System.out.println(ex);
        }
    }

    public void processOutput(String str) {
        //System.out.println("Processing response... " + str);
        JSONObject jo;
        //System.out.println(str);
        // try and parse the line
        try {
            // parse into JSON object
            Object obj = new JSONParser().parse(str);
            jo = (JSONObject) obj;
        } catch (ParseException pe) {
            System.out.println("Error: unrecognized message");
            System.out.println(str);
            return;
        }

        // check status
        String status = (String)jo.get("status");
        if(status == null) {
            System.out.println("Error: status field expected");
            System.out.println(str);
            return;
        }

        // switch based on cmd
        String cmd = (String)jo.get("cmd");
        if(cmd == null) {
            System.out.println("Error: no command field");
            System.out.println(str);
            return;
        }

        switch(cmd) {
            case "axioms":
                processAxiomList(jo);
                break;
            case "print":
                processPrint(jo);
                break;
            case "read":
                processRead(jo);
                break;
            default:
                System.out.println("Error: unrecognised command");
                System.out.println(str);
        }

    }

    private void processAxiomList(JSONObject jo) {
        //System.out.println("ProcessAxiomList");
        // check status
        String status = (String)jo.get("status");
        if(status.equals("FAIL")) {
            System.out.println("Error: failed to retrieve axioms");
            System.out.println(jo.toString());
            return;
        }
        // get the list of axioms
        JSONArray resArray = (JSONArray)jo.get("result");
        if(resArray == null) {
            System.out.println("Error: no result field");
            System.out.println(jo.toString());
            return;
        }
        // iterate through the list
        Iterator<String> strItr = resArray.iterator();
        while(strItr.hasNext()) {
            String axiom = strItr.next();
            if(axiom != null) {
                // get the latex for the axiom
                sendCommand(HPACommand.printAxiom(axiom));
            }
        }
    }

    private void processPrint(JSONObject jo) {
        // check status
        String status = (String)jo.get("status");
        if(status.equals("FAIL")) {
            System.out.println("Error: failed to print");
            System.out.println(jo.toString());
            return;
        }
        // check type
        String type = (String)jo.get("type");
        if(type == null) {
            System.out.println("Error: no type field");
            System.out.println(jo.toString());
        }
        // process by type
        switch(type) {
            case "axiom":
                String name = (String)jo.get("name");
                String latex = (String)jo.get("result");
                if(name != null && latex != null) axiomDoc.updateAxiom(name, latex);
                else {
                    System.out.println("Error: no name field or no result field");
                    System.out.println(jo.toString());
                }
                break;
            case "proofstep":
                break;
            default:
                System.out.println("Error: unrecognised type");
                System.out.println(jo.toString());
                break;
        }

    }

    private void processRead(JSONObject jo) {
        //System.out.println("HPAController.processRead");
        //System.out.println(jo.toString());
        // check source
        String source = (String)jo.get("source");
        if(source == null) {
            System.out.println("Error: no source field");
            System.out.println(jo.toString());
            return;
        }
        // switch on source
        switch(source) {
            case "InputHtmlDoc":
                inputDoc.processRead(jo);
                break;
            default:
                System.out.println("Error: unknown source");
                System.out.println(jo.toString());
                break;
        }
    }

    private void updateActionControls() {
        // remove current children
        actionGroup.getChildren().clear();
        // get type of action
        //System.out.println(actionCB.getValue());
        Node controls = null;
        String action = actionCB.getValue();
        switch(action) {
            // TODO: maybe put these into fxml files?
            case assume: { // start a new scope so we can reuse variable names
                VBox box = new VBox(10);
                // input for name for assumption
                Label assumptionNameLabel = new Label("Assume with name:");
                TextField assumptionNameText = new TextField();
                HBox assumptionNameHBox = new HBox(5);
                assumptionNameHBox.getChildren().addAll(assumptionNameLabel, assumptionNameText);
                // label for name of predicate (TODO: this should be a combobox - how to update it, add a refresh button?)
                Label predicateNameLabel = new Label("Using predicate named:");
                TextField predicateNameText = new TextField();
                HBox predicateNameHBox = new HBox(5);
                predicateNameHBox.getChildren().addAll(predicateNameLabel, predicateNameText);
                // add them to the main box
                box.getChildren().addAll(assumptionNameHBox, predicateNameHBox);
                // reset the event handler of actionBtn
                actionBtn.setOnAction(new EventHandler<ActionEvent>() {
                    @Override public void handle(ActionEvent e) {
                        assume(assumptionNameText.getText(), predicateNameText.getText());
                    }
                });
                controls = (Node) box;
            }
                break;
            case instantiateSchema: {
                VBox box = new VBox(10);
                // input for name of instantiation
                Label instantiationNameLabel = new Label("Instantiate with name:");
                TextField instantiationNameText = new TextField();
                HBox instantiationNameHBox = new HBox(5);
                instantiationNameHBox.getChildren().addAll(instantiationNameLabel, instantiationNameText);
                // input for name of schema (TODO: this should be a combobox)
                Label schemaNameLabel = new Label("Using schema:");
                ArrayList<String> names = axiomDoc.getAxiomNames();
                ObservableList<String> axioms = FXCollections.observableArrayList(names);
                ComboBox axiomsCB = new ComboBox(axioms);
                HBox schemaNameHBox = new HBox(5);
                schemaNameHBox.getChildren().addAll(schemaNameLabel, axiomsCB);
                // table for matching
                TableView<PredicateMatching> patvarTable = new TableView<>();
                patvarTable.setEditable(true);
                // pattern variable column
                TableColumn<PredicateMatching, String> patvarCol = new TableColumn<>("Pattern Variable");
                patvarCol.setCellValueFactory(cellData -> cellData.getValue().patternVariableProperty());
                patvarTable.getColumns().add(patvarCol);
                // type column
                TableColumn<PredicateMatching, String> typeCol = new TableColumn<>("Type");
                typeCol.setCellValueFactory(cellData -> cellData.getValue().typeProperty());
                patvarTable.getColumns().add(typeCol);
                // matched name column (TODO: use a combo box)
                TableColumn<PredicateMatching, String> nameCol = new TableColumn<>("Matched Name");
                nameCol.setCellValueFactory(cellData -> cellData.getValue().matchedNameProperty());
                nameCol.setCellFactory(ComboBoxTableCell.forTableColumn(FXCollections.observableArrayList(inputDoc.getNamesOfPredicates())));
                nameCol.setEditable(true);
                patvarTable.getColumns().add(nameCol);
                // TODO: populate the table
                patvarTable.getItems().add(new PredicateMatching("P{P}", "Predicate", "P"));


                // add them to the box
                box.getChildren().addAll(instantiationNameHBox, schemaNameHBox, patvarTable);
                // update button action
                actionBtn.setOnAction(new EventHandler<ActionEvent>() {
                    @Override public void handle(ActionEvent e) {
                        instantiateSchema(instantiationNameText.getText(), (String)axiomsCB.getValue());
                    }
                });
                controls = (Node) box;
            }
                break;
        }
        if(controls != null) actionGroup.getChildren().add(controls);
    }

    private void instantiateSchema(String name, String schemaName) {
        System.out.println("Instantiate schema " + schemaName + " as " + name);
    }

    private void assume(String name, String predicateName) {
        System.out.println("Assume " + predicateName + " as " + name);
    }

    public void cleanUp() {
        //System.out.println("Cleaning up...");
        this.hpaProcess.destroy();
        this.hpaListener.cancel();
    }

    public static class PredicateMatching {
        private final StringProperty patternVariable = new SimpleStringProperty();
        private final StringProperty type = new SimpleStringProperty();
        private final StringProperty matchedName = new SimpleStringProperty();

        public PredicateMatching(String patvar, String t, String name) {
            setPatternVariable(patvar);
            setType(t);
            setMatchedName(name);
        }

        public final StringProperty patternVariableProperty() {
            return this.patternVariable;
        }
        public final String getPatternVariable() {
            return this.patternVariableProperty().get();
        }
        public final void setPatternVariable(final String patvar) {
            this.patternVariableProperty().set(patvar);
        }

        public final StringProperty typeProperty() {
            return this.type;
        }
        public final String getType() {
            return this.typeProperty().get();
        }
        public final void setType(final String type) {
            this.typeProperty().set(type);
        }

        public final StringProperty matchedNameProperty() {
            return this.matchedName;
        }
        public final String getMatchedName() {
            return this.matchedNameProperty().get();
        }
        public final void setMatchedName(final String name) {
            this.matchedNameProperty().set(name);
        }

    }
}

