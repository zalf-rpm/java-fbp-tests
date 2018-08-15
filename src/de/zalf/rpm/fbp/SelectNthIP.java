package de.zalf.rpm.fbp;

import com.jpaulmorrison.fbp.core.engine.*;

import java.util.ArrayList;
import java.util.List;

@ComponentDescription("Route the first NUMBER IPs to ACC the rest to REJ")
@InPorts({
        @InPort(value = "NTH", description = "1-indexed IP to select (Integer or String), defaults to 1", optional = true),
        @InPort(value = "COUNT", description = "How many IPs to select for ACC (Integer or String), defaults to 1", optional = true),
        @InPort(value = "IN")
})
@OutPorts({
        @OutPort(value = "ACC", description = "The stream of accepted IPs", optional = true),
        @OutPort(value = "REJ", description = "The stream of rejected IPs", optional = true)
})

public class SelectNthIP extends Component {
    private InputPort inPort;
    private InputPort nthPort;
    private InputPort countPort;
    private OutputPort accPort;
    private OutputPort rejPort;

    private int nth = -1;
    private int count = -1;

    private Integer receiveIntegerFromPort(InputPort port){
        Integer i = null;

        Packet p = port.receive();
        if (p != null) {

            Object o = p.getContent();
            drop(p);
            System.out.println(Thread.currentThread().getName() + ": closing port " + port.getName());
            port.close();

            if (o instanceof String) {
                try {
                    i = Integer.parseInt((String) o);
                } catch (NumberFormatException nfe) {
                    nfe.printStackTrace();
                }
            } else if (Integer.class.isAssignableFrom(o.getClass()))
                i = (Integer) o;

            System.out.println(Thread.currentThread().getName() + ": received on " + port.getName() + ": " + i);
        }

        return i;
    }

    int packetCount = 0;

    @Override
    protected void execute() {

        if(Thread.currentThread().getName() == "select_header_lines")
            push(create("x"));

        if(nthPort.isClosed())
            nth = 1;
        else {
            Integer i = receiveIntegerFromPort(nthPort);
            if (i != null && i > 0)
                nth = i;
        }
        System.out.println(Thread.currentThread().getName() + ": countPort.isClosed(): " + countPort.isClosed());
        if(countPort.isClosed()) {
            System.out.println(Thread.currentThread().getName() + ": setting count = 1 and countPort.isClosed(): " + countPort.isClosed());
            count = 1;
        } else {
            System.out.println(Thread.currentThread().getName() + ": trying to receive count on COUNT");
            Integer i = receiveIntegerFromPort(countPort);
            if (i != null && i > -1)
                count = i;
        }
        if(nth < 0 || count < 0)
            return;

        boolean accConnected = accPort.isConnected();
        boolean rejConnected = rejPort.isConnected();

        Packet ip;
        //int packetCount = 0;
        //while((ip = inPort.receive()) != null){
        if((ip = inPort.receive()) != null){
            packetCount++;

            if(accConnected && packetCount >= nth && count > (packetCount - nth)) {
                accPort.send(ip);
                System.out.println(Thread.currentThread().getName() + ": IP sent on ACC");
            } else if(rejConnected) {
                rejPort.send(ip);
                System.out.println(Thread.currentThread().getName() + ": IP sent on REJ");
            } else {
                drop(ip);
                System.out.println(Thread.currentThread().getName() + ": IP dropped");
            }
        }
    }

    @Override
    protected void openPorts() {
        inPort = openInput("IN");
        nthPort = openInput("NTH");
        countPort = openInput("COUNT");
        accPort = openOutput("ACC");
        rejPort = openOutput("REJ");
    }
}
