package de.zalf.rpm.fbp;

import com.jpaulmorrison.fbp.core.engine.*;

@ComponentDescription("Apply toString on all IPs in stream")
@InPort("IN")
@OutPort(value = "OUT", description = "toString representation of IP from IN", type = String.class)
public class ToString extends Component {
    private InputPort inPort;
    private OutputPort outPort;

    @Override
    protected void execute() {
        Packet ip;
        while((ip = inPort.receive()) != null){
            outPort.send(create(ip.getContent().toString()));
            drop(ip);
        }
    }

    @Override
    protected void openPorts() {
        inPort = openInput("IN");
        outPort = openOutput("OUT");
    }
}
