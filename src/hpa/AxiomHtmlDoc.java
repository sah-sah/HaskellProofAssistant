package hpa;

import javafx.application.Platform;
import javafx.scene.web.WebEngine;

import java.util.ArrayList;

/*
 * Maintains the html document that displays the axioms
 * We will use bootstrap package
 * https://www.w3schools.com/bootstrap/default.asp
 */
public class AxiomHtmlDoc {

    private final String css =
            "<style>\n" +
    "section.axiom-list .axiom-item {\n" +
    "    text-align: left;\n" +
    "    margin-top: 5px;\n" +
    "    margin-bottom: 5px;\n" +
    "    height: 100px;\n" +
    "}\n" +
    "\n" +
    "section.axiom-list .display-math {\n" +
    "    text-align: center;\n" +
    "}\n" +
    "\n" +
    "section.axiom-list .axiom-item:hover {\n" +
    "    background-color: #a5d8f6;\n" +
    "    cursor: pointer;\n" +
    "}\n" +
                    "</style>\n";

    private final String htmlHeader =
            "<!DOCTYPE html>\n" +
                    "<html lang=\"en\">\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\">\n" +
                    "    <title>Axioms</title>\n" +
                    "\n" +
                    "    <script src=\"https://ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js\"></script>\n" +
                    "    <script src=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js\"></script>\n" +
                    "    <script type=\"text/javascript\" async=\"async\" src=\"https://cdnjs.cloudflare.com/ajax/libs/mathjax/2.7.7/MathJax.js?config=TeX-MML-AM_CHTML\"></script>\n" +
                    "\n" +
                    "    <link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css\">\n" +
                    "    <link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/font-awesome/4.6.3/css/font-awesome.min.css\">\n" +
                    css +
                    "</head>";

    private final String bodyStart =
            "<body>\n" +
                    "\n" +
                    "<div class=\"container-fluid\">\n" +
                    "    <section class=\"axiom-list\">";

    private String bodyMiddle = "";

    private final String bodyEnd =
            "    </section>\n" +
                    "</div>\n" +
                    "\n" +
                    "</body>\n" +
                    "</html>";

    private ArrayList<AxiomItem> axiomList;

    private class AxiomItem {
        public String name, latex;

        public AxiomItem(String name, String latex) {
            this.name = name;
            this.latex = latex;
        }
    }

    // the web view
    private WebEngine engine;

    public AxiomHtmlDoc(WebEngine engine) {
        // set engine
        this.engine = engine;
        // initialise axiom list
        axiomList = new ArrayList<AxiomItem>();
        // load
        load();
    }

    private void load() {
        // update the middle
        updateMiddle();
        // load the webpage
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                engine.loadContent(htmlHeader + bodyStart + bodyMiddle + bodyEnd);
            }
        });
    }

    private void updateMiddle() {
        StringBuilder middle = new StringBuilder();
        for (AxiomItem ax : axiomList) {
            middle.append("<div class=\"row\">\n");
            middle.append("    <div class=\"col-md-12\">\n");
            middle.append("        <div class=\"axiom-item well\">\n");
            middle.append("            <p>");
            middle.append("                <h4>").append(ax.name).append("</h4>\n");
            middle.append("                <div class=\"display-math\">\n");
            middle.append("                    \\(").append(ax.latex).append("\\)\n");
            middle.append("                </div>\n");
            middle.append("            </p>\n");
            middle.append("        </div>\n");
            middle.append("    </div>\n");
            middle.append("</div>\n");
        }
        bodyMiddle = middle.toString();
    }

    public void updateAxiom(String name, String latex) {
        // update an existing axiom
        for (AxiomItem ax : axiomList) {
            if (ax.name.equals(name)) {
                ax.latex = latex;
                return;
            }
        }
        // or, add one to the list
        axiomList.add(new AxiomItem(name, latex));
        // reload the webpage
        load();
    }

    public ArrayList<String> getAxiomNames() {
        ArrayList<String> names = new ArrayList<String>();
        for(AxiomItem a : axiomList) {
            names.add(a.name);
        }
        return names;
    }
}