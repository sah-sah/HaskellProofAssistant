package hpa;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Control;
import javafx.scene.control.TextArea;

import java.io.BufferedReader;
import java.io.IOException;

public class HPAListenTask extends Task<Void> {
    private BufferedReader br;
    private HPAController owner;

    public HPAListenTask(BufferedReader br, HPAController owner) {
        this.br = br;
        this.owner = owner;
    }

    @Override
    protected Void call() {
        StringBuilder output = new StringBuilder();
        // make sure we haven't been interrupted/cancelled
        while(!isCancelled()) {
            try {
                // read until a new line
                while (this.br.ready()) {
                    char next = (char) this.br.read();
                    if(next == '\n') {
                        // if we have some output, send it for processing
                        if(output.length() > 0) owner.processOutput(output.toString());
                        // start reading again
                        output = new StringBuilder();
                    } else {
                        output.append(next);
                    }
                }
            } catch (IOException e) {
                System.out.println("ListenTask: reading from BufferedReader");
                System.out.println(e);
                System.out.println("ListenTask: no longer listening");
                return null;
            }
            // sleep until there is some input
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                System.out.println("ListenTask: cancelled");
//					System.out.println(e);
                if(isCancelled()) return null;
            }

        }
        return null;
    }
}

