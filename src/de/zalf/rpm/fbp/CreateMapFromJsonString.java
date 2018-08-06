package de.zalf.rpm.fbp;

import com.fasterxml.jackson.datatype.pcollections.PCollectionsModule;
import com.jpaulmorrison.fbp.core.engine.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;
import org.pcollections.HashPMap;

@InPort(value = "IN", description = "String of JSON object", type = String.class)
@OutPort(value = "OUT", description = "Map representation of JSON object", type = HashPMap.class)
public class CreateMapFromJsonString extends Component {
    InputPort inPort;
    OutputPort outPort;

    @Override
    protected void execute() {
        //ObjectMapper om = JsonFactory.create(); //Boon
        ObjectMapper om = new ObjectMapper().registerModule(new PCollectionsModule()); //Jackson

        Packet p = inPort.receive();
        while (p != null) {
            String s = (String)p.getContent();

            //Map m = om.fromJson(s, Map.class); //Boon
            //drop(p);
            //Packet out = create(m);
            //outPort.send(out);

            //Jackson
            try {
                Map m = om.readValue(s, HashPMap.class);
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
