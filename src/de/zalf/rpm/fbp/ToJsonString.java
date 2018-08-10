package de.zalf.rpm.fbp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jpaulmorrison.fbp.core.engine.*;

import com.fasterxml.jackson.databind.ObjectMapper;

@ComponentDescription("Serialize value into JSON string")
@InPort(value = "IN", description = "Value")
@OutPort(value = "OUT", description = "String of JSON object", type = String.class)
public class ToJsonString extends Component {
    InputPort inPort;
    OutputPort outPort;

    ObjectMapper om = new ObjectMapper();

    @Override
    protected void execute() {
        Packet p;
        while ((p  = inPort.receive()) != null) {
            Object o = p.getContent();
            drop(p);

            try {
                String s = om.writerWithDefaultPrettyPrinter().writeValueAsString(o);
                Packet out = create(s);
                outPort.send(out);
            } catch (JsonProcessingException e) {
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
