package de.zalf.rpm.fbp;

import com.fasterxml.jackson.datatype.pcollections.PCollectionsModule;
import com.jpaulmorrison.fbp.core.engine.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import org.pcollections.HashPMap;
import org.pcollections.TreePVector;

@ComponentDescription("Create a PMap or PVector depending on the content of the JSON string")
@InPort(value = "IN", description = "String of JSON object", type = String.class)
@OutPort(value = "OUT", description = "PMap or PVector representation of JSON file content")
public class JsonStringToPMapOrVec extends Component {
    InputPort inPort;
    OutputPort outPort;

    ObjectMapper om = new ObjectMapper().registerModule(new PCollectionsModule());

    @Override
    protected void execute() {
        Packet p;
        while ((p  = inPort.receive()) != null) {
            String s = (String)p.getContent();
            drop(p);

            try {
                Object o;
                if(s.startsWith("{"))
                    o = om.readValue(s, HashPMap.class);
                else if(s.startsWith("["))
                    o = om.readValue(s, TreePVector.class);
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
