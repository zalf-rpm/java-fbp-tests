package de.zalf.rpm.fbp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jpaulmorrison.fbp.core.engine.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import org.pcollections.HashPMap;

@InPort(value = "IN", description = "Map representation of JSON object", type = HashPMap.class)
@OutPort(value = "OUT", description = "String of JSON object", type = String.class)
public class CreateJsonStringFromMap extends Component {
    InputPort inPort;
    OutputPort outPort;

    @Override
    protected void execute() {
        //ObjectMapper om = JsonFactory.create(); //Boon
        ObjectMapper om = new ObjectMapper();

        Packet p = inPort.receive();
        while (p != null) {
            Map m = (Map)p.getContent();

            try {
                String s = om.writerWithDefaultPrettyPrinter().writeValueAsString(m);
                drop(p);
                Packet out = create(s);
                outPort.send(out);
            } catch (JsonProcessingException e) {
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
