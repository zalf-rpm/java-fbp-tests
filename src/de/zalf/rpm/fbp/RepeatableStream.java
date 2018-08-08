package de.zalf.rpm.fbp;

import com.jpaulmorrison.fbp.core.engine.*;

import java.util.LinkedList;
import java.util.List;

@ComponentDescription("Simply forwards IPs and may repeat these, but beware of IP's content should be copyable")
@InPorts({
        @InPort(value = "IN", description = "Objects to forward and possibly repeat"),
        @InPort(value = "REPEAT", description = "SIGNAL IP to repeat received IPs so far")
})
@OutPort(value = "OUT", description = "send (repeated) IPs")
public class RepeatableStream extends Component {
    InputPort inPort;
    InputPort repeatPort;
    OutputPort outPort;

    List<Object> receivedIPs = new LinkedList<>();

    @Override
    protected void execute() {
        if(!inPort.isClosed()) {
            Packet ip;
            while ((ip = inPort.receive()) != null) {
                receivedIPs.add(ip.getContent());
                drop(ip);
            }
            inPort.close();
        }

        Packet rp = repeatPort.receive();
        if(rp != null) {
            drop(rp);
            outPort.send(create(Packet.OPEN, ""));
            for (Object o : receivedIPs)
                outPort.send(create(o));
            outPort.send(create(Packet.CLOSE, ""));
        }
    }

    @Override
    protected void openPorts() {
        inPort = openInput("IN");
        repeatPort = openInput("REPEAT");
        outPort = openOutput("OUT");
    }
}
