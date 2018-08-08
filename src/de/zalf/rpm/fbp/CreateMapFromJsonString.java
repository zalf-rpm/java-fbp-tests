package de.zalf.rpm.fbp;

import com.fasterxml.jackson.datatype.pcollections.PCollectionsModule;
import com.jpaulmorrison.fbp.core.engine.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import org.pcollections.HashPMap;
import org.pcollections.PCollection;
import org.pcollections.TreePVector;

@ComponentDescription("Create a MAP from a JSON string")
@InPort(value = "IN", description = "String of JSON object", type = String.class)
@OutPort(value = "OUT", description = "Map representation of JSON object", type = PCollection.class)
public class CreateMapFromJsonString extends Component {
    InputPort inPort;
    OutputPort outPort;

    @Override
    protected void execute() {
        ObjectMapper om = new ObjectMapper().registerModule(new PCollectionsModule());

        Packet p = inPort.receive();
        while (p != null) {
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

            p = inPort.receive();
        }
    }

    @Override
    protected void openPorts() {
        inPort = openInput("IN");
        outPort = openOutput("OUT");
    }
}
