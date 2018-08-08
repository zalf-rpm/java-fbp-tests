package de.zalf.rpm.fbp;

import com.jpaulmorrison.fbp.core.engine.*;

@ComponentDescription("Pass through IPs but if connected send a trigger IP for every pass through on TRIGGER port")
@InPort(value = "IN", description = "IPs to pass through")
@OutPorts({
        @OutPort(value = "OUT", description = "Pass through from IN"),
        @OutPort(value = "TRIGGER", description = "Trigger IP for every IP passed through",
                type = String.class, optional = true)
})
public class PassThroughWithTrigger extends Component {
    InputPort inPort;
    OutputPort outPort;
    OutputPort triggerPort;

    @Override
    protected void execute() {
        boolean tc = triggerPort.isConnected();
        Packet p;
        while((p = inPort.receive()) != null) {
            outPort.send(p);
            if (tc)
                triggerPort.send(create(""));
        }
    }

    @Override
    protected void openPorts() {
        inPort = openInput("IN");
        outPort = openOutput("OUT");
        triggerPort = openOutput("TRIGGER");
    }
}
