package de.zalf.rpm.fbp;

import com.jpaulmorrison.fbp.core.engine.*;

import java.util.ArrayList;
import java.util.List;

@ComponentDescription("Collect all IPs into a single list")
@InPort("IN")
@OutPort(value = "OUT", type = List.class, optional = true)
public class ToList extends Component {
    private InputPort inPort;
    private OutputPort outPort;

    List ips = new ArrayList<>();

    @Override
    protected void execute() {
        Packet ip;
        while((ip = inPort.receive()) != null){
            ips.add(ip.getContent());
            drop(ip);
        }
        outPort.send(create(ips));
    }

    @Override
    protected void openPorts() {
        inPort = openInput("IN");
        outPort = openOutput("OUT");
    }
}
