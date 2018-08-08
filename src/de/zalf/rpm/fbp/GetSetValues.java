package de.zalf.rpm.fbp;

import com.jpaulmorrison.fbp.core.engine.*;
import org.pcollections.PMap;

@ComponentDescription("Attaches IP received on port ATTACH to IP received on port IN.")
@InPorts({
        @InPort(value = "GET", description = "IP to attach", type = PMap.class),
        @InPort(value = "KEY", description = "Key used for attached GET MAP", type = String.class),
        @InPort(value = "SET", description = "MAP to set values in"),
})
@OutPort(value = "OUT", description = "The updated SET MAP")
public class GetSetValues extends Component {
    InputPort getPort;
    InputPort keyPort;
    InputPort setPort;
    OutputPort outPort;

    String key;

    @Override
    protected void execute() {
        if (!keyPort.isClosed()) {
            Packet kp = keyPort.receive();
            if (kp == null)
                return;
            key = (String)kp.getContent();
            drop(kp);
            keyPort.close();
        }

        Packet sp = setPort.receive();
        if(sp == null)
            return;
        PMap<String, Object> setMap = (PMap<String, Object>)sp.getContent();
        drop(sp);

        Packet gp;
        while((gp = getPort.receive()) != null) {

            drop(gp);
        }

        outPort.send(create(setMap));
    }

    @Override
    protected void openPorts() {
        getPort = openInput("GET");
        keyPort = openInput("KEY");
        setPort = openInput("SET");
        outPort = openOutput("OUT");
    }
}
