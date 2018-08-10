package de.zalf.rpm.fbp;

import com.fasterxml.jackson.datatype.pcollections.PCollectionsModule;
import com.jpaulmorrison.fbp.core.engine.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pcollections.HashPMap;
import org.pcollections.TreePVector;

@ComponentDescription("Create a PMap or PVector depending on the contents of the JSON file")
@InPort(value = "IN", description = "Path to JSON file", type = String.class)
@OutPort(value = "OUT", description = "PMap<String, Object> or PVector<Object>")
public class JsonFileToPColl extends Component {
    InputPort inPort;
    OutputPort outPort;

    ObjectMapper om = new ObjectMapper().registerModule(new PCollectionsModule());

    @Override
    protected void execute() {
        Packet p;
        while ((p = inPort.receive()) != null) {
            String s = (String)p.getContent();
            drop(p);

            try {
                BufferedReader br = new BufferedReader(new FileReader(s));
                br.mark(200);
                char c = ' ';
                for(int i = 0; i < 200; i++) {
                    if(!Character.isWhitespace(c = (char)br.read()))
                        break;
                }
                br.reset();

                Object o;
                if(c == '{')
                    o = om.readValue(new File(s), HashPMap.class);
                else if(c == '[')
                    o = om.readValue(new File(s), TreePVector.class);
                else
                    continue;

                Packet out = create(o);
                outPort.send(out);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void openPorts() {
        inPort = openInput("IN");
        outPort = openOutput("OUT");
    }
}

