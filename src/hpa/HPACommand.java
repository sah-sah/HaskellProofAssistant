package hpa;

import org.json.JSONObject;

public abstract class HPACommand {
    public static final int MoveUp = 0;
    public static final int MoveRight = 1;
    public static final int MoveDown = 2;
    public static final int MoveLeft = 3;

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

    public static String generalise(String name, String resultName, String variable) {
        final JSONObject cmdObj = new JSONObject();
        cmdObj.put("cmd", "generalise");
        cmdObj.put("name", name);
        cmdObj.put("result", resultName);
        cmdObj.put("var", variable);
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

    public static String setFocus(String name) {
        final JSONObject cmdObj = new JSONObject();
        cmdObj.put("cmd","setFocus");
        cmdObj.put("name", name);
        return cmdObj.toString();
    }

    public static String transformFocus(String logiclaw) {
        final JSONObject cmdObj = new JSONObject();
        cmdObj.put("cmd","transformFocus");
        cmdObj.put("name", logiclaw);
        return cmdObj.toString();
    }

    public static String recordFocus(String name) {
        final JSONObject cmdObj = new JSONObject();
        cmdObj.put("cmd","recordFocus");
        cmdObj.put("name", name);
        return cmdObj.toString();
    }

    public static String clearFocus() {
        final JSONObject cmdObj = new JSONObject();
        cmdObj.put("cmd","clearFocus");
        return cmdObj.toString();
    }

    public static String moveFocus(int direction) {
        return HPACommand.moveFocus(direction, false);
    }

    public static String moveFocus(int direction, boolean isBranch) {
        // check input
        if((isBranch && direction > 0) || (!isBranch && direction >= 0 && direction <= 3)) {
            final JSONObject cmdObj = new JSONObject();
            cmdObj.put("cmd", "moveFocus");
            if(isBranch) {
                cmdObj.put("direction", "branch");
                cmdObj.put("branch", String.valueOf(direction));
            }
            else {
                switch(direction) {
                    case MoveUp -> cmdObj.put("direction", "up");
                    case MoveRight -> cmdObj.put("direction", "right");
                    case MoveDown -> cmdObj.put("direction", "down");
                    case MoveLeft -> cmdObj.put("direction", "left");
                }
            }
            return cmdObj.toString();
        }
        else {
            System.out.println("Error(HPACommand.moveFocus): invalid directions given");
            return null;
        }
    }
}
