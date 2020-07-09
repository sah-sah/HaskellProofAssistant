package hpa;

import org.json.JSONObject;

public abstract class HPACommand {
    public static String listAxioms() {
        final JSONObject cmdObj = new JSONObject();
        // put values into object
        cmdObj.put("cmd", "axioms");
        //cmdObj.put("other", "stuff");
        //System.out.println(cmdObj.toString());
        return cmdObj.toString();
    }

    private static String printPredicate(String name, boolean isAxiom) {
        final JSONObject cmdObj = new JSONObject();
        cmdObj.put("cmd", "print");
        cmdObj.put("name", name);
        cmdObj.put("type", isAxiom ? "axiom" : "proofstep");
        return cmdObj.toString();
    }

    public static String printAxiom(String name) {
        return printPredicate(name, true);
    }

    /*
    public static String printResult(String name) {
        return printPredicate(name, false);
    }
    */

    public static String printDetails(String name) {
        final JSONObject cmdObj = new JSONObject();
        cmdObj.put("cmd","details");
        cmdObj.put("name", name);
        return cmdObj.toString();
    }

    public static String readPredicate(String predicate, String source) {
        final JSONObject cmdObj = new JSONObject();
        cmdObj.put("cmd","read");
        cmdObj.put("predicate",predicate);
        cmdObj.put("source", source);
        return cmdObj.toString();
    }

    public static String assume(String name, String predicate, int resultNum) {
        final JSONObject cmdObj =  new JSONObject();
        cmdObj.put("cmd","assume");
        cmdObj.put("name", name);
        cmdObj.put("predicate", predicate);
        cmdObj.put("index", String.valueOf(resultNum));
        return cmdObj.toString();
    }

    public static String instantiateSchema(int resultNum, String name, String schemaName, String[] patvars, String[] predicates) {
        // check lists have same length
        if(patvars.length != predicates.length) {
            System.out.println("Error(HPACommand.instantiateSchema): pattern variables and predicates do not have same size");
            return null;
        }
        // build JSON object
        final JSONObject cmdObj = new JSONObject();
        cmdObj.put("cmd", "instantiateSchema");
        cmdObj.put("name", name);
        cmdObj.put("schema", schemaName);
        cmdObj.put("index", String.valueOf(resultNum));
        // add matching
        for(int i = 0; i < patvars.length; i++) {
            cmdObj.put(patvars[i], predicates[i]);
        }
        // return JSON Object as String
        return cmdObj.toString();
    }

    public static String modusPonens(String name, String pimpq, String p) {
        final JSONObject cmdObj = new JSONObject();
        cmdObj.put("cmd", "modusPonens");
        cmdObj.put("name", name);
        cmdObj.put("pimpqn", pimpq);
        cmdObj.put("pn", p);
        return cmdObj.toString();
    }

    public static String instantiateAt(String name, String fan, String xvar) {
        final JSONObject cmdObj = new JSONObject();
        cmdObj.put("cmd","instantiateAt");
        cmdObj.put("name", name);
        cmdObj.put("fan", fan);
        cmdObj.put("xvarp", xvar);
        return cmdObj.toString();
    }

    public static String splitAnd(String pname, String qname, String pandq) {
        final JSONObject cmdObj = new JSONObject();
        cmdObj.put("cmd","splitAnd");
        cmdObj.put("pname", pname);
        cmdObj.put("qname", qname);
        cmdObj.put("pandq", pandq);
        return cmdObj.toString();
    }
    // TODO: commands for proof steps should return the completed proof step for display
    // TODO: should return all the proofstep information
}
