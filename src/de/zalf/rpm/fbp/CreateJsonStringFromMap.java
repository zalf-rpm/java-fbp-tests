package de.zalf.rpm.fbp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jpaulmorrison.fbp.core.engine.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import org.pcollections.PMap;

@InPort(value = "IN", description = "Map representation of JSON object", type = PMap.class)
@OutPort(value = "OUT", description = "String of JSON object", type = String.class)
public class CreateJsonStringFromMap extends Component {
    InputPort inPort;
    OutputPort outPort;

    ObjectMapper om = new ObjectMapper();

    @Override
    protected void execute() {


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
