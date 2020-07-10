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
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
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
    private final String instantiateAt = "Let...";
    private final String splitAnd = "Split..";
    private final String setFocus = "Focus on...";
    private final String useFocus = "Modify Focus...";
    private final String generalise = "Generalise...";
    private final String liftResult = "Lift result...";
    private final ObservableList<String> proofActions = FXCollections.observableArrayList(assume, instantiateSchema, modusPonens, instantiateAt, splitAnd, setFocus, useFocus, generalise, liftResult);

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
        //System.out.println("Sending command... " + cmd);
        if(cmd == null) return;
        try {
            //System.out.println(cmd);
            this.hpaWriter.write(cmd);
            this.hpaWriter.newLine();
            this.hpaWriter.flush();
        } catch (Exception ex) {
            System.out.println("Error(HPAController.sendCommand): error sending command");
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
            case "instantiateAt" -> processIA(jo);
            case "splitAnd" -> processSA(jo);
            case "setFocus" -> processSF(jo);
            case "moveFocus" -> processMF(jo);
            case "transformFocus" -> processTF(jo);
            case "recordFocus" -> processRF(jo);
            case "clearFocus" -> processCF(jo);
            case "generalise" -> processGW(jo);
            case "liftResult" -> processLR(jo);
            default -> {
                System.out.println("Error(HPAController.processOutput): unrecognised command");
                System.out.println(str);
            }
        }

    }

    // TODO: these could be inlined
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

    private void processGW(JSONObject jo) {
        // check status
        String status = (String)jo.get("status");
        if(status.equals("FAIL")) {
            displayMessage("Unable to generalise result...");
            System.out.println("Error(HPAController.processGW): unable to generalise result");
            System.out.println(jo.toString());
            return;
        }
        // status is OK
        proofDoc.processGW(jo);
    }

    private void processMP(JSONObject jo)  {
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

    private void processIA(JSONObject jo) {
        // check status
        String status = (String)jo.get("status");
        if(status.equals("FAIL")) {
            displayMessage("Unable to instantiate at...");
            System.out.println("Error(HPAController.processIA): unable to instantiate at");
            System.out.println(jo.toString());
            return;
        }
        // status is OK
        proofDoc.processIA(jo);
    }

    private void processSA(JSONObject jo) {
        // check status
        String status = (String)jo.get("status");
        if(status.equals("FAIL")) {
            displayMessage("Unable to split and...");
            System.out.println("Error(HPAController.processSA): unable to split and");
            System.out.println(jo.toString());
            return;
        }
        // status is OK
        proofDoc.processSA(jo);
    }

    private void processSF(JSONObject jo) {
        // check status
        String status = (String)jo.get("status");
        if(status.equals("FAIL")) {
            displayMessage("Unable to set focus...");
            System.out.println("Error(HPAController.processSF): unable to set focus");
            System.out.println(jo.toString());
            return;
        }
        // status is OK
        proofDoc.processSF(jo);
    }

    private void processMF(JSONObject jo) {
        // check status
        String status = (String)jo.get("status");
        if(status.equals("FAIL")) {
            displayMessage("Unable to move focus...");
            System.out.println("Error(HPAController.processMF): unable to move focus");
            System.out.println(jo.toString());
            return;
        }
        // status is OK
        proofDoc.processMF(jo);
    }

    private void processTF(JSONObject jo) {
        // check status
        String status = (String)jo.get("status");
        if(status.equals("FAIL")) {
            displayMessage("Unable to transform focus...");
            System.out.println("Error(HPAController.processTF): unable to transform focus");
            System.out.println(jo.toString());
            return;
        }
        // status is OK
        proofDoc.processTF(jo);
    }

    private void processRF(JSONObject jo) {
        // check status
        String status = (String)jo.get("status");
        if(status.equals("FAIL")) {
            displayMessage("Unable to record focus...");
            System.out.println("Error(HPAController.processRF): unable to record focus");
            System.out.println(jo.toString());
            return;
        }
        // status is OK
        proofDoc.processRF(jo);
    }

    private void processCF(JSONObject jo) {
        // check status
        String status = (String)jo.get("status");
        if(status.equals("FAIL")) {
            displayMessage("Unable to clear focus...");
            System.out.println("Error(HPAController.processCF): unable to clear focus");
            System.out.println(jo.toString());
            return;
        }
        // status is OK
        proofDoc.processCF(jo);
    }

    private void processLR(JSONObject jo) {
        // check status
        String status = (String)jo.get("status");
        if(status.equals("FAIL")) {
            displayMessage("Unable to lift result...");
            System.out.println("Error(HPAController.processLR): unable to lift result");
            System.out.println(jo.toString());
            return;
        }
        // status is OK
        proofDoc.processLR(jo);
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
                    instantiateSchema(instantiationNameText.getText().trim(), axiomsCB.getValue(), patvarTable.getItems());
                });
                // update action button name
                actionBtn.setText("Instantiate...");
                controls = box;
            }
            case instantiateAt -> {
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
                // label for name of predicate to instantiate (forall x. P(x))
                Label faNameLabel = new Label("Where forall x s.t. P(x). Q(x) is:");
                ComboBox<String> faCB = new ComboBox<>(predicateNames);
                HBox faNameHBox = new HBox(5);
                faNameHBox.getChildren().addAll(faNameLabel, faCB);
                // label for name of variable
                Label xNameLabel = new Label("Where x is:");
                ComboBox<String> xCB = new ComboBox<>(FXCollections.observableArrayList(inputDoc.getNamesOfPredicates()));
                HBox xNameHBox = new HBox(5);
                xNameHBox.getChildren().addAll(xNameLabel, xCB);
                // add them to the main box
                box.getChildren().addAll(resultNameHBox, faNameHBox, xNameHBox);
                // reset the event handler of actionBtn
                actionBtn.setOnAction(e -> instantiateAt(resultNameText.getText().trim(), faCB.getValue(), xCB.getValue()));
                // update action button name
                actionBtn.setText("Let...");
                controls = box;
            }
            case splitAnd -> {
                VBox box = new VBox(10);
                // input for name for new results
                // P
                Label pResultNameLabel = new Label("Name for P in P & Q:");
                TextField pResultNameText = new TextField(proofDoc.getNextResultName("R") + ".1");
                HBox pResultNameHBox = new HBox(5);
                pResultNameHBox.getChildren().addAll(pResultNameLabel, pResultNameText);
                // Q
                Label qResultNameLabel = new Label("Name for Q in P & Q:");
                TextField qResultNameText = new TextField(proofDoc.getNextResultName("R") + ".2");
                HBox qResultNameHBox = new HBox(5);
                qResultNameHBox.getChildren().addAll(qResultNameLabel, qResultNameText);
                // get names of predicate to split
                ArrayList<String> names = axiomDoc.getAxiomNames();
                names.addAll(proofDoc.getResultNames());
                ObservableList<String> predicateNames = FXCollections.observableArrayList(names);
                // label for name of predicate P & Q
                Label pandqNameLabel = new Label("Where P & Q is:");
                ComboBox<String> pandqCB = new ComboBox<>(predicateNames);
                HBox pandqNameHBox = new HBox(5);
                pandqNameHBox.getChildren().addAll(pandqNameLabel, pandqCB);
                // add them to the main box
                box.getChildren().addAll(pResultNameHBox, qResultNameHBox, pandqNameHBox);
                // reset the event handler of actionBtn
                actionBtn.setOnAction(e -> split(pResultNameText.getText().trim(), qResultNameText.getText().trim(), pandqCB.getValue()));
                // update action button name
                actionBtn.setText("Split...");
                controls = box;
            }
            case setFocus -> {
                VBox box = new VBox(10);
                // label for name of focus
                Label focusNameLabel = new Label("Focus on:");
                // get names of predicates
                ArrayList<String> names = axiomDoc.getAxiomNames();
                names.addAll(proofDoc.getResultNames());
                ObservableList<String> predicateNames = FXCollections.observableArrayList(names);
                // combo box
                ComboBox<String> focusCB = new ComboBox<>(predicateNames);
                // add to box
                HBox focusNameHBox = new HBox(5);
                focusNameHBox.getChildren().addAll(focusNameLabel, focusCB);
                // add them to the main box
                box.getChildren().addAll(focusNameHBox);
                // reset the event handler of actionBtn
                actionBtn.setOnAction(e -> setFocus(focusCB.getValue()));
                // update action button name
                actionBtn.setText("Focus...");
                controls = box;
            }
            case useFocus -> {
                HBox box = new HBox(10);
                GridPane movePane = new GridPane();
                // move buttons
                Button upBtn = new Button("Up");
                Button rightBtn = new Button("Right");
                Button downBtn = new Button("Down");
                Button leftBtn = new Button("Left");
                Button branchBtn = new Button("Take Branch:");
                // set the size
                movePane.getColumnConstraints().add(new ColumnConstraints(50));
                movePane.getColumnConstraints().add(new ColumnConstraints(50));
                movePane.getColumnConstraints().add(new ColumnConstraints(50));
                upBtn.setPrefWidth(50);
                rightBtn.setPrefWidth(50);
                downBtn.setPrefWidth(50);
                leftBtn.setPrefWidth(50);
                branchBtn.setPrefWidth(100);
                // set actions
                upBtn.setOnAction(e -> moveFocus("up", null));
                rightBtn.setOnAction(e -> moveFocus("right",null));
                downBtn.setOnAction(e -> moveFocus("down",null));
                leftBtn.setOnAction(e -> moveFocus("left",null));
                // TODO: is there are way to validate (force only numbers to be entered)
                TextField branchTextField = new TextField("1");
                branchBtn.setOnAction(e -> moveFocus("branch",branchTextField.getText().trim()));
                movePane.add(upBtn, 1, 0);
                movePane.add(rightBtn, 2, 1);
                movePane.add(downBtn, 1, 2);
                movePane.add(leftBtn, 0, 1);
                movePane.add(branchBtn, 0, 3, 2, 1);
                movePane.add(branchTextField, 2, 3);
                VBox actionBox = new VBox(10);
                // input for name when recording
                Label recordNameLabel = new Label("Record as:");
                TextField recordNameText = new TextField(proofDoc.getNextResultName("R"));
                HBox recordNameHBox = new HBox(5);
                recordNameHBox.getChildren().addAll(recordNameLabel, recordNameText);
                // transform using axioms
                Button transformBtn = new Button("Transform using:");
                ArrayList<String> names = axiomDoc.getAxiomNames();
                names.addAll(proofDoc.getResultNames());
                ObservableList<String> predicateNames = FXCollections.observableArrayList(names);
                ComboBox<String> transformCB = new ComboBox<>(predicateNames);
                HBox transformHBox = new HBox(5);
                transformHBox.getChildren().addAll(transformBtn, transformCB);
                transformBtn.setOnAction(e -> transformFocus(transformCB.getValue()));
                // clear focus
                Button clearBtn = new Button("Clear focus");
                clearBtn.setOnAction(e -> clearFocus());
                // add all the elements
                actionBox.getChildren().addAll(recordNameHBox, transformHBox, clearBtn);
                // add gridpane and vbox
                box.getChildren().addAll(movePane, actionBox);
                // reset the event handler of actionBtn
                actionBtn.setOnAction(e -> recordFocus(recordNameText.getText().trim()));
                // update action button name
                actionBtn.setText("Record...");
                controls = box;
            }
            case generalise -> {
                VBox box = new VBox(10);
                // input for name of instantiation
                Label generaliseNameLabel = new Label("Generalise with name:");
                TextField generaliseNameText = new TextField(proofDoc.getNextResultName("R"));
                HBox generaliseNameHBox = new HBox(5);
                generaliseNameHBox.getChildren().addAll(generaliseNameLabel, generaliseNameText);
                // input for name of schema (TODO: is there a way to bind contents of ComboBox to axiomDoc name list?)
                Label fromNameLabel = new Label("From result:");
                ArrayList<String> names = proofDoc.getResultNames();
                ObservableList<String> results = FXCollections.observableArrayList(names);
                ComboBox<String> resultsCB = new ComboBox<>(results);
                HBox fromNameHBox = new HBox(5);
                fromNameHBox.getChildren().addAll(fromNameLabel, resultsCB);
                // label for variable name
                Label variableNameLabel = new Label("With variable:");
                ObservableList<String> inputNames = FXCollections.observableArrayList(inputDoc.getNamesOfPredicates());
                ComboBox<String> variableCB = new ComboBox<>(inputNames);
                HBox variableNameHBox = new HBox(5);
                variableNameHBox.getChildren().addAll(variableNameLabel, variableCB);
                // add them to the box
                box.getChildren().addAll(generaliseNameHBox, fromNameHBox, variableNameHBox);
                // update button action
                actionBtn.setOnAction(e -> {
                    // we need to get the predicate matching from the table
                    generaliseWith(generaliseNameText.getText().trim(), resultsCB.getValue(), variableCB.getValue());
                });
                // update action button name
                actionBtn.setText("Generalise...");
                controls = box;
            }
            case liftResult -> {
                VBox box = new VBox(10);
                // input for name of lifted result
                Label liftNameLabel = new Label("Lift with name:");
                TextField liftNameText = new TextField(proofDoc.getNextResultName("R"));
                HBox liftNameHBox = new HBox(5);
                liftNameHBox.getChildren().addAll(liftNameLabel, liftNameText);
                // input for name of result to lift
                Label fromNameLabel = new Label("From result:");
                ArrayList<String> names = proofDoc.getResultNames();
                ObservableList<String> results = FXCollections.observableArrayList(names);
                ComboBox<String> resultsCB = new ComboBox<>(results);
                HBox fromNameHBox = new HBox(5);
                fromNameHBox.getChildren().addAll(fromNameLabel, resultsCB);
                // assumption to lift out of
                Label assumptionNameLabel = new Label("Over assumption:");
                ComboBox<String> assumptionsCB = new ComboBox<>();
                resultsCB.setOnAction(e -> {
                    ObservableList<String> assumptions = FXCollections.observableArrayList(proofDoc.getAssumptionsOfResult(resultsCB.getValue()));
                    assumptionsCB.getItems().clear();
                    assumptionsCB.getItems().addAll(assumptions);
                });
                HBox assumptionNameHBox = new HBox(5);
                assumptionNameHBox.getChildren().addAll(assumptionNameLabel, assumptionsCB);
                // add them to the box
                box.getChildren().addAll(liftNameHBox, fromNameHBox, assumptionNameHBox);
                // update button action
                actionBtn.setOnAction(e -> {
                    // we need to get the predicate matching from the table
                    liftResult(liftNameText.getText().trim(), resultsCB.getValue(), assumptionsCB.getValue());
                });
                // update action button name
                actionBtn.setText("Lift result...");
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

    private void instantiateAt(String name, String fan, String xn) {
        if(name == null || name.length() == 0) {
            displayMessage("Please provide a name for the result");
            return;
        }
        if(fan == null || fan.length() == 0) {
            displayMessage("Please provide forall x s.t. P(x). Q(x) predicate");
            return;
        }
        // check we have a predicate for the new variable name
        String xvar = inputDoc.getPredicateByName(xn);
        if(xvar == null || xvar.length() == 0) {
            displayMessage("Please provide variable name");
            return;
        }
        sendCommand(HPACommand.instantiateAt(name, fan, xvar));
    }

    private void liftResult(String name, String result, String assumption) {
        // check we have a name
        if(name == null || name.length() == 0) {
            displayMessage("Please provide name for lifted result");
            return;
        }
        // check we have a result to lift
        if(result == null || result.length() == 0) {
            displayMessage("Please provide a result to lift");
            return;
        }
        // check we have a variable
        if(assumption == null || assumption.length() == 0) {
            displayMessage("Please provide an assumption to lift over");
            return;
        }
        // send message
        sendCommand(HPACommand.liftResult(name,result,assumption));
    }

    private void generaliseWith(String name, String resultName, String variableName) {
        // check we have a name
        if(name == null || name.length() == 0) {
            displayMessage("Please provide name for generalised predicate");
            return;
        }
        // check we have a result to generalise
        if(resultName == null || resultName.length() == 0) {
            displayMessage("Please provide a result to generalise");
            return;
        }
        // check we have a variable
        String variable = inputDoc.getPredicateByName(variableName);
        if(variable == null || variable.length() == 0) {
            displayMessage("Please provide a variable name to use");
            return;
        }
        // send message
        sendCommand(HPACommand.generalise(name,resultName,variable));
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

    private void split(String pname, String qname, String pandq) {
        // check we have a name for P
        if(pname == null || pname.length() == 0) {
            displayMessage("Please provide name for P in P & Q");
            return;
        }
        // check we have a name for Q
        if(qname == null || qname.length() == 0) {
            displayMessage("Please provide name for Q in P & Q");
            return;
        }
        // check the names are not the same
        if(pname.equals(qname)) {
            displayMessage("Names must be different");
            return;
        }
        // check we have a predicate name
        if(pandq == null || pandq.length() == 0) {
            displayMessage("Please provide name P & Q predicate");
            return;
        }
        // send message
        sendCommand(HPACommand.splitAnd(pname,qname,pandq));
    }

    private void setFocus(String name) {
        // check we have a name
        if(name == null || name.length() == 0) {
            displayMessage("Please provide name for predicate to focus on");
            return;
        }
        sendCommand(HPACommand.setFocus(name));
    }

    private void recordFocus(String name) {
        // check we have a name
        if(name == null || name.length() == 0) {
            displayMessage("Please provide name for recording focus");
            return;
        }
        sendCommand(HPACommand.recordFocus(name));
    }

    private void moveFocus(String direction, String branch) {
        // check direction is not null
        if(direction == null || direction.length() == 0) {
            displayMessage("Please specify direction to move focus");
            return;
        }
        // we have a direction
        boolean validDirection = true;
        switch(direction) {
            case "up" -> sendCommand(HPACommand.moveFocus(HPACommand.MoveUp));
            case "right" -> sendCommand(HPACommand.moveFocus(HPACommand.MoveRight));
            case "down" -> sendCommand(HPACommand.moveFocus(HPACommand.MoveDown));
            case "left" -> sendCommand(HPACommand.moveFocus(HPACommand.MoveLeft));
            case "branch" -> {
                // try to read which branch
                try {
                    int b = Integer.parseInt(branch);
                    if(b > 0) {
                        sendCommand(HPACommand.moveFocus(b,true));
                    }
                    else {
                        validDirection = false;
                    }

                } catch(NumberFormatException e) {
                    validDirection = false;
                }
            }
            default -> validDirection = false;
        }
        if(!validDirection) {
            displayMessage("Invalid direction specified");
        }
    }

    private void transformFocus(String logiclaw) {
        // check we have a logic law
        if(logiclaw == null || logiclaw.length() == 0) {
            displayMessage("Please provide a transformation law");
            return;
        }
        sendCommand(HPACommand.transformFocus(logiclaw));
    }

    private void clearFocus() {
        sendCommand(HPACommand.clearFocus());
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

