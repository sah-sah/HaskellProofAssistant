package hpa;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

/*
* TODO: we need a common way to show error messages
*
 */

public class HPAController {
    private final String assume = "Assume...";
    private final String instantiateSchema = "Instantiate Schema";
    private final String modusPonens = "Modus Ponens";

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
    private Label infoLabel;

    @FXML
    public void initialize() {
        // Setup interface
        // TODO: this list should be the possible proof steps/other actions
        ObservableList<String> proofActions = FXCollections.observableArrayList(assume, instantiateSchema, modusPonens);
        actionCB.setItems(proofActions);
        actionCB.setValue("Assume...");
        actionCB.setOnAction(e -> Platform.runLater(this::updateActionControls));

        infoLabel.setStyle("-fx-background-color: darkslateblue; -fx-text-fill: white;");

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
        // this needs to go after initializeContent
        updateActionControls();

        // get list of axioms
        sendCommand(HPACommand.listAxioms());
    }

    private void initializeContent() {
        // set up Axiom web view
        axiomDoc = new AxiomHtmlDoc(this.displayAxioms.getEngine());
        // set up input web view
        inputDoc = new InputHtmlDoc(this, this.displayInput.getEngine());
        // set up proof display
        proofDoc = new ProofHtmlDoc(this, this.displayProof.getEngine());

    }

    public void sendCommand(String cmd) {
        System.out.println("Sending command... " + cmd);
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
        // parse into JSON object
        jo = new JSONObject(new JSONTokener(str));

        // check status (TODO: maybe drop this, check status in other methods?)
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

        switch (cmd) {
            case "axioms" -> processAxiomList(jo);
            case "print" -> processPrint(jo);
            case "read" -> processRead(jo);
            case "assume" -> processAssume(jo);
            case "details" -> processDetails(jo);
            case "instantiateSchema" -> processIS(jo);
            case "modusPonens" -> processMP(jo);
            default -> {
                System.out.println("Error(HPAController.processOutput): unrecognised command");
                System.out.println(str);
            }
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
        JSONArray resArray = jo.getJSONArray("result");
        if(resArray == null) {
            System.out.println("Error: no result field");
            System.out.println(jo.toString());
            return;
        }
        // iterate through the list
        for (Object axiom : resArray) {
            if (axiom != null) {
                // get the latex for the axiom
                sendCommand(HPACommand.printAxiom(axiom.toString()));
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
            return;
        }
        String name = (String) jo.get("name");
        String latex = (String) jo.get("result");
        if(name == null || latex == null) {
            System.out.println("Error(HPAController.processPrint): no name field or no result field");
            System.out.println(jo.toString());
            return;
        }
        // process by type
        switch (type) {
            case "axiom" -> axiomDoc.updateAxiom(name, latex);
            case "proofstep" -> proofDoc.updateResult(name, latex);
            default -> {
                System.out.println("Error(HPAController.processPrint): unrecognised type");
                System.out.println(jo.toString());
            }
        }

    }

    private void processRead(JSONObject jo) {
        //System.out.println("HPAController.processRead");
        //System.out.println(jo.toString());
        // check source
        String source = jo.getString("source");
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

    private void processAssume(JSONObject jo) {
        // check status
        String status = (String)jo.get("status");
        if(status.equals("FAIL")) {
            displayMessage("Unable to assume predicate...");
            System.out.println("Error(HPAController.processAssume): unable to assume predicate");
            System.out.println(jo.toString());
            return;
        }
        // if status OK, send to proofDoc
        proofDoc.processAssume(jo);
    }

    private void processIS(JSONObject jo) {
        // check status
        String status = (String)jo.get("status");
        if(status.equals("FAIL")) {
            displayMessage("Unable to instantiate schema...");
            System.out.println("Error(HPAController.processAssume): unable to instantiate schema");
            System.out.println(jo.toString());
            return;
        }
        // status is OK
        proofDoc.processIS(jo);
    }

    private void processMP(JSONObject jo) {
        // check status
        String status = (String)jo.get("status");
        if(status.equals("FAIL")) {
            displayMessage("Unable to apply modus ponens...");
            System.out.println("Error(HPAController.processModusPonens): unable to apply");
            System.out.println(jo.toString());
            return;
        }
        // status is OK
        proofDoc.processMP(jo);
    }

    private void processDetails(JSONObject jo) {
        // check status
        String status = (String)jo.get("status");
        if(status.equals("FAIL")) {
            displayMessage("Unable to get result details...");
            System.out.println("Error(HPAController.processAssume): unable to get details of result");
            System.out.println(jo.toString());
            return;
        }
        // if status OK, send to proofDoc
        proofDoc.processDetails(jo);
    }

    @FXML
    private void updateActionControls() {
        // remove current children
        actionGroup.getChildren().clear();
        // get type of action
        //System.out.println(actionCB.getValue());
        Node controls = null;
        String action = actionCB.getValue();
        // TODO: maybe put these into fxml files?
        // TODO: we can put all these in same fxml then use setVisible/setManaged
        switch (action) {
            case assume -> { // start a new scope so we can reuse variable names
                VBox box = new VBox(10);
                // input for name for assumption
                Label assumptionNameLabel = new Label("Assume with name:");
                TextField assumptionNameText = new TextField(proofDoc.getNextResultName("A"));
                HBox assumptionNameHBox = new HBox(5);
                assumptionNameHBox.getChildren().addAll(assumptionNameLabel, assumptionNameText);
                // label for name of predicate
                Label predicateNameLabel = new Label("Using predicate named:");
                ArrayList<String> names = inputDoc.getNamesOfPredicates();
                ObservableList<String> predicateNames = FXCollections.observableArrayList(names);
                ComboBox<String> predicatesCB = new ComboBox<>(predicateNames);
                // add to box
                HBox predicateNameHBox = new HBox(5);
                predicateNameHBox.getChildren().addAll(predicateNameLabel, predicatesCB);
                // add them to the main box
                box.getChildren().addAll(assumptionNameHBox, predicateNameHBox);
                // reset the event handler of actionBtn
                actionBtn.setOnAction(e -> assume(assumptionNameText.getText().trim(), predicatesCB.getValue()));
                // update action button name
                actionBtn.setText("Assume...");
                controls = box;
            }
            case modusPonens -> {
                VBox box = new VBox(10);
                // input for name for new result
                Label resultNameLabel = new Label("Name for result:");
                TextField resultNameText = new TextField(proofDoc.getNextResultName("R"));
                HBox resultNameHBox = new HBox(5);
                resultNameHBox.getChildren().addAll(resultNameLabel, resultNameText);
                // get names of predicates
                ArrayList<String> names = axiomDoc.getAxiomNames();
                names.addAll(proofDoc.getResultNames());
                ObservableList<String> predicateNames = FXCollections.observableArrayList(names);
                // label for name of predicate P -> Q
                Label pimpqNameLabel = new Label("Where P -> Q is:");
                ComboBox<String> pimpqCB = new ComboBox<>(predicateNames);
                HBox pimpqNameHBox = new HBox(5);
                pimpqNameHBox.getChildren().addAll(pimpqNameLabel, pimpqCB);
                // label for name of predicate P
                Label pNameLabel = new Label("Where P is:");
                ComboBox<String> pCB = new ComboBox<>(predicateNames);
                HBox pNameHBox = new HBox(5);
                pNameHBox.getChildren().addAll(pNameLabel, pCB);
                // add them to the main box
                box.getChildren().addAll(resultNameHBox, pimpqNameHBox, pNameHBox);
                // reset the event handler of actionBtn
                actionBtn.setOnAction(e -> deduce(resultNameText.getText().trim(), pimpqCB.getValue(), pCB.getValue()));
                // update action button name
                actionBtn.setText("Deduce...");
                controls = box;
            }
            case instantiateSchema -> {
                VBox box = new VBox(10);
                // input for name of instantiation
                Label instantiationNameLabel = new Label("Instantiate with name:");
                TextField instantiationNameText = new TextField(proofDoc.getNextResultName("R"));
                HBox instantiationNameHBox = new HBox(5);
                instantiationNameHBox.getChildren().addAll(instantiationNameLabel, instantiationNameText);
                // input for name of schema (TODO: is there a way to bind contents of ComboBox to axiomDoc name list?)
                Label schemaNameLabel = new Label("Using schema:");
                ArrayList<String> names = axiomDoc.getAxiomNames();
                ObservableList<String> axioms = FXCollections.observableArrayList(names);
                ComboBox<String> axiomsCB = new ComboBox<>(axioms);
                HBox schemaNameHBox = new HBox(5);
                schemaNameHBox.getChildren().addAll(schemaNameLabel, axiomsCB);
                // table for matching
                TableView<PredicateMatching> patvarTable = new TableView<>();
                patvarTable.setEditable(true);
                patvarTable.setFixedCellSize(25);
                patvarTable.prefHeightProperty().bind(Bindings.size(patvarTable.getItems()).multiply(patvarTable.getFixedCellSize()).add(50));
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
                axiomsCB.setOnAction(e -> Platform.runLater(() -> {
                    // get axiom name
                    String axiom = axiomsCB.getValue();
                    // get predicate for axiom
                    if (axiom.length() > 0) {
                        // get predicate for name
                        String latex = axiomDoc.getNamedAxiom(axiom);
                        if (latex != null && latex.length() > 0) {
                            // need to escape twice, once in the string and once for the pattern
                            // so when pattern requires a \, it needs an escape to \\
                            // then string needs both \\ escaped to \\\\
                            // when pattern requires a }, it needs an escape to \}
                            // then string needs \} escaped to \\}
                            Pattern pattern = Pattern.compile("\\\\textrm\\{[NPE]\\}\\\\\\{[^\\\\]+\\\\\\}");
                            Matcher matcher = pattern.matcher(latex);
                            ArrayList<String> patvars = new ArrayList<>();
                            while (matcher.find()) {
                                String pv = latex.substring(matcher.start(), matcher.end());
                                // add to list if new
                                boolean found = false;
                                for (String s : patvars) {
                                    if (s.equals(pv)) {
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found) patvars.add(pv);
                            }
                            // go through unique patvars
                            for (String pv : patvars) {
                                String type = pv.substring(8, 9);
                                String name = pv.substring(12, pv.length() - 2);
                                switch (type) {
                                    case "N" -> patvarTable.getItems().add(new PredicateMatching("N{" + name + "}", "Name", ""));
                                    case "P" -> patvarTable.getItems().add(new PredicateMatching("P{" + name + "}", "Predicate", ""));
                                    case "E" -> patvarTable.getItems().add(new PredicateMatching("E{" + name + "}", "Expression", ""));
                                }
                            }
                        }
                    }
                }));
                // add them to the box
                box.getChildren().addAll(instantiationNameHBox, schemaNameHBox, patvarTable);
                // update button action
                actionBtn.setOnAction(e -> {
                    // we need to get the predicate matching from the table
                    instantiateSchema(instantiationNameText.getText(), axiomsCB.getValue(), patvarTable.getItems());
                });
                // update action button name
                actionBtn.setText("Instantiate...");
                controls = box;
            }
        }
        if(controls != null) actionGroup.getChildren().add(controls);
    }

    private void instantiateSchema(String name, String schemaName, ObservableList<PredicateMatching> matching) {
        // check we have a name
        if(name == null || name.length() == 0) {
            displayMessage("Please provide name for instantiated schema");
            return;
        }
        // check we have a predicate
        if(schemaName == null || schemaName.length() == 0) {
            displayMessage("Please provide a schema to instantiate");
            return;
        }
        // check the matching
        String[] patvars = new String[matching.size()];
        String[] predicates = new String[matching.size()];
        int ix = 0;
        for (PredicateMatching pm : matching) {
            patvars[ix] = pm.getPatternVariable();
            predicates[ix] = inputDoc.getPredicateByName(pm.getMatchedName());
            if (patvars[ix] == null || predicates[ix] == null) {
                displayMessage("Please provide matching for schema pattern variables");
                return;
            }
            ix++;
        }
        // send command
        sendCommand(HPACommand.instantiateSchema(proofDoc.getNextResultNum(),name,schemaName,patvars,predicates));
    }

    private void deduce(String name, String pimpq, String p) {
        if(name == null || name.length() == 0) {
            displayMessage("Please provide a name for the result");
            return;
        }
        if(pimpq == null || pimpq.length() == 0) {
            displayMessage("Please provide P->Q predicate");
            return;
        }
        if(p == null || p.length() == 0) {
            displayMessage("Please provide P predicate");
            return;
        }
        sendCommand(HPACommand.modusPonens(name, pimpq, p));
    }

    private void assume(String name, String predicateName) {
        // check we have a name
        if(name == null || name.length() == 0) {
            displayMessage("Please provide name for assumed predicate");
            return;
        }
        // check we have a predicate
        String predicate = inputDoc.getPredicateByName(predicateName);
        if(predicate == null || predicate.length() == 0) {
            displayMessage("Please provide a predicate to assume");
            return;
        }
        // send message
        sendCommand(HPACommand.assume(name,predicate,proofDoc.getNextResultNum()));
    }

    public void displayMessage(String msg) {
        Platform.runLater(() -> infoLabel.setText(msg));
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

