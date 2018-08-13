package de.zalf.rpm.fbp;

import com.jpaulmorrison.fbp.core.engine.*;
import org.zeromq.ZMQ;

@ComponentDescription("Sends message via ZeroMQ")
@InPorts({
        @InPort(value = "IN", description = "IPs to be sent via ZeroMQ", type = String.class),
        @InPort(value = "ADDRESS", description = "TCP address of ZMQ socket", type = String.class)})
@OutPort(value = "OUT", description = "pass through", type = String.class, optional = true)
public class ZeroMQOutProxy extends Component {
    private InputPort inPort;
    private InputPort addressPort;
    private OutputPort outPort;

    private ZMQ.Context ctx;
    private ZMQ.Socket pushSocket;

    public ZeroMQOutProxy() {
        ctx = ZMQ.context(1);
        pushSocket = ctx.socket(ZMQ.PUSH);
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

        Packet ip = inPort.receive();
        if(ip == null)
            return;

        String msg = (String)ip.getContent();
        pushSocket.send(msg);
        //System.out.println("sent msg");

        if(outPort.isConnected())
            outPort.send(ip);
        else
            drop(ip);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void openPorts() {
        inPort = openInput("IN");
        addressPort = openInput("ADDRESS");
        outPort = openOutput("OUT");
    }
}


