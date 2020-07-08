package hpa;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
import javafx.scene.web.WebEngine;

import netscape.javascript.JSObject;

import java.util.ArrayList;

import org.json.JSONObject;

import org.w3c.dom.html.HTMLInputElement;

import javax.naming.Name;


/*
 * TODO: get the buttons to call back to this object
 *  then make the buttons work
 * - get a list of known functions, special symbols from server (hard code it for the moment)
 * - present them in a table with information about # of arguments etc
 */

public class InputHtmlDoc {

    /* We need functions for the buttons, and the table */

    /* base html code */
    private final String htmlTop =
            "<!DOCTYPE html>\n" +
                    "<html lang=\"en\">\n" + 
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\">\n" +
                    "    <title>Predicate Input</title>\n" +
                    "    <script src=\"https://ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js\"></script>\n" +
                    "    <script src=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js\"></script>\n" +
                    "    <script type=\"text/javascript\" async=\"async\" src=\"https://cdnjs.cloudflare.com/ajax/libs/mathjax/2.7.7/MathJax.js?config=TeX-MML-AM_CHTML\"></script>\n" +
                    "    <link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css\">\n" +
                    "    <link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/font-awesome/4.6.3/css/font-awesome.min.css\">\n" +
                    "    <link rel=\"stylesheet\" href=\"axioms.css\">\n" +
                    "</head>\n" +
                    "\n"+
                    "<body>\n" +
                    "<div class=\"container\">\n" +
                    "    <section class=\"predicate-input\">\n" +
                    "        <div class=\"row\">\n" +
                    "            <div class=\"col-md-12\">\n" +
                    "    <h4>Parsed predicate</h4>\n" +
                    "               <div class=\"readPredicate well\">\n" +
                    "                    <p id=\"predicate-display\">\n" +
                    "                    <small style=\"color:grey\">Press check to read predicate </small>\n" +
                    "                    </p>\n" +
                    "                </div>\n" +
                    "            </div>\n" +
                    "        </div>\n" +
                    "        <div class=\"row\">\n" +
                    "            <div class=\"col-md-12\">\n" +
                    "                <form class=\"form\"> <!-- add action= -->\n" +
                    "                    <label for=\"predicate\">Input predicate:</label>\n" +
                    "                    <div class=\"input-group\">\n" +
                    "                        <input type=\"text\" class=\"form-control\" id=\"predicate\">\n" +
                    "                        <div class=\"input-group-btn\">\n" +
                    "                            <button class=\"btn btn-default\" type=\"submit\" onclick=\"controller.checkPredicate();\">\n" +
                    "                                <i class=\"glyphicon glyphicon-export\"></i>\n" +
                    "                            </button>\n" +
                    "                        </div>\n" +
                    "                    </div>\n" +
                    "                </form>\n" +
                    "            </div>\n" +
                    "        </div>\n" +
                    "        <div class=\"row\">\n" +
                    "            <div class=\"col-md-12\">\n" +
                    "                <ul class=\"nav nav-tabs\">\n" +
                    "                    <li class=\"active\"><a data-toggle=\"tab\" href=\"#predicate-list\">Predicates</a> </li>\n" +
                    "                    <li><a data-toggle=\"tab\" href=\"#symbols\">Symbols</a></li>\n" +
                    "                    <li><a data-toggle=\"tab\" href=\"#greek\">Greek</a></li>\n" +
                    "                </ul>\n" +
                    "                <div class=\"tab-content\">\n" +
                    "                    <div id=\"predicate-list\" class=\"tab-pane active\">\n" +
                    "                        <br>\n" +
                    "                        <form class=\"form-inline\"> <!-- add action= -->\n" +
                    "                            <label for=\"predicate-name\">Add predicate with name:</label>\n" +
                    "                            <div class=\"input-group\">\n" +
                    "                                <input type=\"text\" class=\"form-control\" id=\"predicate-name\">\n" +
                    "                                <div class=\"input-group-btn\">\n" +
                    "                                    <button class=\"btn btn-default\" type=\"submit\" onclick=\"controller.addNamedPredicate();\">\n" +
                    "                                        <i class=\"glyphicon glyphicon-plus\"></i>\n" +
                    "                                    </button>\n" +
                    "                                </div>\n" +
                    "                            </div>\n" +
                    "                        </form>\n";

    private final String htmlBottom = "                    </div>\n" +
                    "                    <div id=\"symbols\" class=\"tab-pane\">\n" +
                    "                        <div class=\"btn-grp btn-matrix\">\n" +
                    "                            <button class=\"btn btn-default\" type=\"submit\">1</button>\n" +
                    "                            <button class=\"btn btn-default\" type=\"submit\">2</button>\n" +
                    "                            <button class=\"btn btn-default\" type=\"submit\">3</button>\n" +
                    "                            <button class=\"btn btn-default\" type=\"submit\">4</button>\n" +
                    "                            <button class=\"btn btn-default\" type=\"submit\">5</button>\n" +
                    "                            <button class=\"btn btn-default\" type=\"submit\">6</button>\n" +
                    "                            <button class=\"btn btn-default\" type=\"submit\">7</button>\n" +
                    "                            <button class=\"btn btn-default\" type=\"submit\">8</button>\n" +
                    "                            <button class=\"btn btn-default\" type=\"submit\">9</button>\n" +
                    "                            <button class=\"btn btn-default\" type=\"submit\">10</button>\n" +
                    "                            <button class=\"btn btn-default\" type=\"submit\">11</button>\n" +
                    "                            <button class=\"btn btn-default\" type=\"submit\">12</button>\n" +
                    "                        </div>\n" +
                    "                    </div>\n" +
                    "                    <div id=\"greek\" class=\"tab-pane\">\n" +
                    "                        <div class=\"btn-grp btn-matrix\">\n" +
                    "                            <button class=\"btn btn-default\" type=\"submit\">a</button>\n" +
                    "                            <button class=\"btn btn-default\" type=\"submit\">b</button>\n" +
                    "                            <button class=\"btn btn-default\" type=\"submit\">c</button>\n" +
                    "                            <button class=\"btn btn-default\" type=\"submit\">d</button>\n" +
                    "                            <button class=\"btn btn-default\" type=\"submit\">e</button>\n" +
                    "                            <button class=\"btn btn-default\" type=\"submit\">f</button>\n" +
                    "                            <button class=\"btn btn-default\" type=\"submit\">g</button>\n" +
                    "                            <button class=\"btn btn-default\" type=\"submit\">h</button>\n" +
                    "                            <button class=\"btn btn-default\" type=\"submit\">i</button>\n" +
                    "                            <button class=\"btn btn-default\" type=\"submit\">j</button>\n" +
                    "                            <button class=\"btn btn-default\" type=\"submit\">k</button>\n" +
                    "                            <button class=\"btn btn-default\" type=\"submit\">l</button>\n" +
                    "                        </div>\n" +
                    "                    </div>\n" +
                    "                </div>\n" +
                    "            </div>\n" +
                    "        </div>\n" +
                    "    </section>\n" +
                    "</div>\n" +
                    "</body>\n" +
                    "</html>";

    private String namedPredicateTable;

    // owner of this object
    private HPAController owner;
    // the web view
    private WebEngine engine;

    // the list of named predicates
    private class NamedPredicate {
        public String name;
        public String predicateRaw;
        public String predicateLaTeX;

        public NamedPredicate() {}
    }
    private ArrayList<NamedPredicate> namedPredicates;
    private ArrayList<NamedPredicate> checkedPredicates;

    public InputHtmlDoc(HPAController owner, WebEngine engine) {
        // set owner
        this.owner = owner;
        // set engine
        this.engine = engine;
        // set a listener for page loading
        Worker<Void> worker = this.engine.getLoadWorker();
        InputHtmlDoc docController = this;

        // Listening to the status of worker
        worker.stateProperty().addListener(new ChangeListener<State>() {

            @Override
            public void changed(ObservableValue<? extends State> observable, //
                                State oldValue, State newValue) {

                // When load successed.
                if (newValue == Worker.State.SUCCEEDED) {
                    //System.out.println("Loaded webpage");
                    // Get window object of page.
                    JSObject jsobj = (JSObject) engine.executeScript("window");
                    //System.out.println(jsobj);
                    // Set member for 'window' object.
                    // I think this is not working -- try creating a private class
                    jsobj.setMember("controller", docController);
                }
            }
        });
        // set up list of named predicates
        namedPredicates = new ArrayList<NamedPredicate>();
        checkedPredicates = new ArrayList<NamedPredicate>();
        // load
        load();
    }

    private void load() {
        // build the webpage
        buildNamedPredicateTable();
        // load the webpage
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                engine.loadContent(htmlTop + namedPredicateTable + htmlBottom);
            }
        });
    }

    public void checkPredicate() {
        //System.out.println("Checking predicate...");
        // NOTE: I don't think this needs to be in a runLater call
        // because we are directly responding to a user event
        // But if there is strange behaviour then try putting it in a runLater call
        // get the predicate to read
        HTMLInputElement input = (HTMLInputElement)engine.getDocument().getElementById("predicate");
        String predicate = input.getValue().trim();
        //System.out.println("Predicate to check is .. " + predicate);
        if(predicate.length() > 0) {
            // check this predicate
            owner.sendCommand(HPACommand.readPredicate(predicate, "InputHtmlDoc"));
            engine.getDocument().getElementById("predicate-display").setTextContent("Checking predicate...");
        }
    }

    public void addNamedPredicate() {
        //System.out.println("addNamedPredicate");
        // NOTE: I don't think this needs to be in a runLater call
        // because we are directly responding to a user event
        // But if there is strange behaviour then try putting it in a runLater call
        // get name from html doc
        HTMLInputElement nameElement = (HTMLInputElement) engine.getDocument().getElementById("predicate-name");
        HTMLInputElement predicateElement = (HTMLInputElement) engine.getDocument().getElementById("predicate");
        String name = nameElement.getValue().trim();
        String predicate = predicateElement.getValue().trim();
        //System.out.println("Trying to add... |" + predicate + "| as " + name);
        // check we have a valid name and predicate
        if (name.length() > 0 && predicate.length() > 0) {
            NamedPredicate np = new NamedPredicate();
            np.name = name;
            np.predicateRaw = predicate;
            // try to find it in the array of checked predicates
            for (NamedPredicate i : checkedPredicates) {
                //System.out.println("Checking against...|" + i.predicateRaw + "|");
                if (i.predicateRaw.equals(predicate)) {
                    System.out.println("Found checked predicate...");
                    np.predicateLaTeX = i.predicateLaTeX;
                    namedPredicates.add(np);
                    load();
                    return;
                }
            }
            // we did not find a checked predicate
            System.out.println("Error: predicate to add has not been checked");
        } else {
            System.out.println("Error: name or predicate is missing");
        }
    }

    public void processRead(JSONObject jo) {
        //System.out.println("InputHtmlDoc.processRead");
        //System.out.println(jo.toString());

        // already checked existence of this field
        String status = (String)jo.get("status");

        if(status.equals("OK")) {
            // check we have a result
            String result = (String) jo.get("result");
            String predicate = (String) jo.get("predicate");
            if (result == null || predicate == null) { // throw error (we need a better way to do this)
                System.out.println("Error: no result field or no predicate field");
                System.out.println(jo.toString());
                return;
            }
            // add predicate to the list of checked predicates
            NamedPredicate np = new NamedPredicate();
            np.predicateRaw = predicate;
            np.predicateLaTeX = result;
            checkedPredicates.add(np);
            //System.out.println("Updating display with" + result);
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    HTMLInputElement input = (HTMLInputElement) engine.getDocument().getElementById("predicate");
                    String inputPredicate = input.getValue();
                    if (inputPredicate.equals(predicate)) {
                        // update display
                        engine.getDocument().getElementById("predicate-display").setTextContent("\\(" + result + "\\)");
                        // NOTE: we can also limit the updating to specific DOM elements (which might
                        // be useful if there is a lot of LaTeX on the page
                        engine.executeScript("MathJax.Hub.Queue([\"Typeset\",MathJax.Hub]);");
                    }
                }
            });
        } else {
            // failed to parse predicate
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    engine.getDocument().getElementById("predicate-display").setTextContent("Not a valid predicate");
                }
            });
        }
    }

    private void buildNamedPredicateTable() {
        StringBuilder table = new StringBuilder();
        // add header to table
        table.append("<table class=\"table\">\n");
        table.append("    <thead>\n");
        table.append("        <tr>\n");
        table.append("        <th>Name</th>\n");
        table.append("        <th>Predicate</th>\n");
        table.append("        </tr>\n");
        table.append("    </thead>\n");
        table.append("    <tbody>\n");
        // add a row for each named predicate
        for(NamedPredicate np : namedPredicates) {
            table.append("    <tr>\n");
            table.append("    <td>").append(np.name).append("</td>\n");
            table.append("    <td>\\(").append(np.predicateLaTeX).append("\\)</td>\n");
            table.append("    </tr>\n");
        }
        // close table
        table.append("</table>\n");
        // save table
        namedPredicateTable = table.toString();
    }

    public boolean namedPredicateExists(String name) {
        for(NamedPredicate np : namedPredicates) {
            if(np.name.equals(name)) return true;
        }
        return false;
    }

    public String getPredicateByName(String name) {
        for(NamedPredicate np : namedPredicates) {
            if(np.name.equals(name)) return np.predicateRaw;
        }
        return null;
    }

    public ArrayList<String> getNamesOfPredicates() {
        ArrayList<String> names = new ArrayList<>();
        for(NamedPredicate np : namedPredicates) {
            names.add(np.name);
        }
        return names;
    }
}
