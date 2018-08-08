package de.zalf.rpm.fbp;

import com.fasterxml.jackson.datatype.pcollections.PCollectionsModule;
import com.jpaulmorrison.fbp.core.engine.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.pcollections.HashPMap;
import org.pcollections.PMap;

@ComponentDescription("Create a MAP from a JSON file")
@InPort(value = "IN", description = "Path to JSON file", type = String.class)
@OutPort(value = "OUT", description = "Map representation of JSON object", type = PMap.class)
public class CreateMapFromJsonFile extends Component {
    InputPort inPort;
    OutputPort outPort;

    @Override
    protected void execute() {
        ObjectMapper om = new ObjectMapper().registerModule(new PCollectionsModule());

        Packet p = inPort.receive();
        while (p != null) {
            String s = (String)p.getContent();

            try {
                Map m = om.readValue(new File(s), HashPMap.class);
                drop(p);
                Packet out = create(m);
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

