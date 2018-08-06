package de.zalf.rpm.fbp;

import com.jpaulmorrison.fbp.core.engine.*;
import org.zeromq.ZMQ;

@ComponentDescription("")
@InPorts({
        @InPort(value = "IN", description = "IPs to be sent via ZeroMQ", type = String.class),
        @InPort(value = "ZMQ-ADDRESS", description = "TCP address of ZMQ socket", type = String.class)})
@OutPort(value = "OUT", description = "passthrough", type = String.class, optional = true)
public class ZeroMQOutProxy extends Component {

    InputPort inport;
    InputPort addressPort;
    OutputPort outport;

    ZMQ.Context ctx;
    ZMQ.Socket pushSocket;

    public ZeroMQOutProxy()
    {
        ctx = ZMQ.context(1);
        pushSocket = ctx.socket(ZMQ.PUSH);
    }

    @Override
    protected void openPorts() {
        inport = openInput("IN");
        addressPort = openInput("ZMQ-ADDRESS");
        outport = openOutput("OUT");
    }

    @Override
    protected void execute() {

        if (!addressPort.isClosed()) {
            Packet ap = addressPort.receive();
            if(ap == null)
                return;
            addressPort.close();

            String address = (String)ap.getContent();
            address = address.trim();

            pushSocket.connect(address);
            drop(ap);
        }

        Packet msgp = inport.receive();
        if(msgp == null)
            return;
        //inPort.close()

        String msg = (String)msgp.getContent();
        pushSocket.send(msg);
        System.out.println("sent msg");

        if(outport.isConnected())
            outport.send(msgp);
        else
            drop(msgp);

        //Thread.sleep(50);
    }
}


