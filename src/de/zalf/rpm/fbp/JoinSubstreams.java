package de.zalf.rpm.fbp;

import com.jpaulmorrison.fbp.core.engine.*;

@ComponentDescription("Make successive substream into one substream")
@InPort("IN")
@OutPort("OUT")
public class JoinSubstreams extends Component {
    InputPort inPort;
    OutputPort outPort;

    @Override
    protected void execute() {

        Packet ip = inPort.receive();
        if(ip != null)
            outPort.send(create(Packet.OPEN, ""));
        while(ip != null){
            if(ip.getType() == Packet.NORMAL)
                outPort.send(ip);
            else
                drop(ip);
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
