package de.zalf.rpm.fbp;

import com.jpaulmorrison.fbp.core.engine.*;

@ComponentDescription("Make stream into substream, by wrapping IPs into brackets")
@InPort("IN")
@OutPort("OUT")
public class ToSubstream extends Component {
    InputPort inPort;
    OutputPort outPort;

    @Override
    protected void execute() {

        Packet ip = inPort.receive();
        if(ip != null)
            outPort.send(create(Packet.OPEN, ""));
        while(ip != null){
            outPort.send(ip);
            ip = inPort.receive();
        }
        outPort.send(create(Packet.CLOSE, ""));
    }

    @Override
    protected void openPorts() {
        inPort = openInput("IN");
        outPort = openOutput("OUT");
    }
}
